import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.schemas.pr_analysis import (
    PRAnalysisRequest,
    FileType,
    ChangeType,
    SignalCategory,
    TestCoverageStatus,
)
from app.analyzer.diff_parser import DiffParser
from app.analyzer.pr_analyzer import PRAnalyzer


client = TestClient(app)


SAMPLE_JAVA_PAYMENT_DIFF = """diff --git a/src/main/java/com/example/controller/PaymentController.java b/src/main/java/com/example/controller/PaymentController.java
index 1111111..2222222 100644
--- a/src/main/java/com/example/controller/PaymentController.java
+++ b/src/main/java/com/example/controller/PaymentController.java
@@ -10,6 +10,12 @@ package com.example.controller;
 import org.springframework.web.bind.annotation.*;
+import com.example.repository.PaymentRepository;
 
 @RestController
 @RequestMapping("/api/payments")
 public class PaymentController {
+    private final PaymentRepository paymentRepository;
+
+    public PaymentController(PaymentRepository paymentRepository) {
+        this.paymentRepository = paymentRepository;
+    }
+
+    @PostMapping("/charge")
+    public String chargeUser(@RequestBody PaymentRequest req) {
+        return paymentRepository.executeRaw("SELECT * FROM payments WHERE user = '" + req.getUser() + "'");
+    }
 }
diff --git a/src/main/java/com/example/service/PaymentService.java b/src/main/java/com/example/service/PaymentService.java
new file mode 100644
index 0000000..3333333
--- /dev/null
+++ b/src/main/java/com/example/service/PaymentService.java
@@ -0,0 +1,15 @@
+package com.example.service;
+
+import org.springframework.stereotype.Service;
+
+@Service
+public class PaymentService {
+    public boolean processTransaction(double amount, String currency) {
+        return amount > 0;
+    }
+}
+"""

SAMPLE_PYTHON_DIFF = """diff --git a/app/services/auth.py b/app/services/auth.py
index 4444444..5555555 100644
--- a/app/services/auth.py
+++ b/app/services/auth.py
@@ -5,4 +5,9 @@ import os
 
+def verify_token(token: str) -> bool:
+    admin_secret = "ghp_1234567890abcdef1234567890abcdef"
+    return token == admin_secret
+
 async def get_current_user():
     pass
"""


def test_diff_parser_extracts_files_and_hunks():
    files = DiffParser.parse_unified_diff(SAMPLE_JAVA_PAYMENT_DIFF)
    assert len(files) == 2

    # Verify PaymentController
    controller = files[0]
    assert controller.file_path == "src/main/java/com/example/controller/PaymentController.java"
    assert controller.language == "java"
    assert controller.file_type == FileType.SOURCE
    assert controller.change_type == ChangeType.MODIFIED
    assert controller.additions == 11
    assert len(controller.raw_hunks) == 1

    # Verify PaymentService
    service = files[1]
    assert service.file_path == "src/main/java/com/example/service/PaymentService.java"
    assert service.change_type == ChangeType.ADDED
    assert service.additions == 11


def test_pr_analyzer_extracts_ast_symbols():
    request = PRAnalysisRequest(
        pr_id="PR-101",
        repo_name="org/payment-service",
        base_sha="base123",
        head_sha="head123",
        raw_diff=SAMPLE_JAVA_PAYMENT_DIFF,
    )
    report = PRAnalyzer.analyze(request)

    assert report.total_files_changed == 2
    assert report.total_additions == 22

    controller = next(f for f in report.files if "PaymentController" in f.file_path)
    symbol_names = [s.symbol_name for s in controller.symbols_changed]
    assert "PaymentController" in symbol_names
    assert "chargeUser" in symbol_names


def test_pr_analyzer_detects_security_signals():
    request = PRAnalysisRequest(
        pr_id="PR-102",
        repo_name="org/payment-service",
        base_sha="base123",
        head_sha="head123",
        raw_diff=SAMPLE_JAVA_PAYMENT_DIFF,
    )
    report = PRAnalyzer.analyze(request)

    signal_names = [s.signal_name for s in report.signals]
    # Should detect dynamic SQL concatenation
    assert "SQL_CONCATENATION_DETECTED" in signal_names

    sql_signal = next(s for s in report.signals if s.signal_name == "SQL_CONCATENATION_DETECTED")
    assert sql_signal.category == SignalCategory.SECURITY
    assert sql_signal.confidence >= 0.90


def test_pr_analyzer_detects_secret_and_python_symbols():
    request = PRAnalysisRequest(
        pr_id="PR-103",
        repo_name="org/auth-service",
        base_sha="base123",
        head_sha="head123",
        raw_diff=SAMPLE_PYTHON_DIFF,
    )
    report = PRAnalyzer.analyze(request)

    signal_names = [s.signal_name for s in report.signals]
    assert "GITHUB_PERSONAL_ACCESS_TOKEN_EXPOSED" in signal_names

    auth_file = report.files[0]
    symbol_names = [s.symbol_name for s in auth_file.symbols_changed]
    assert "verify_token" in symbol_names


def test_pr_analyzer_detects_missing_tests():
    request = PRAnalysisRequest(
        pr_id="PR-104",
        repo_name="org/payment-service",
        base_sha="base123",
        head_sha="head123",
        raw_diff=SAMPLE_JAVA_PAYMENT_DIFF,
    )
    report = PRAnalyzer.analyze(request)

    # Since no test files are included in the diff, missing test coverage must be flagged
    signal_names = [s.signal_name for s in report.signals]
    assert "MISSING_TEST_FOR_PUBLIC_API" in signal_names

    service_mapping = next(m for m in report.test_mappings if "PaymentService" in m.source_file)
    assert service_mapping.test_coverage_status == TestCoverageStatus.MISSING
    assert not service_mapping.has_associated_tests


def test_pr_analyzer_syntax_error_fallback_resilience():
    broken_python_diff = """diff --git a/app/broken.py b/app/broken.py
new file mode 100644
--- /dev/null
+++ b/app/broken.py
@@ -0,0 +1,5 @@
+def invalid_syntax(
+    # Missing closing parenthesis and colon
+    return 123
+"""
    request = PRAnalysisRequest(
        pr_id="PR-105",
        repo_name="org/broken-repo",
        base_sha="b0",
        head_sha="h0",
        raw_diff=broken_python_diff,
    )
    # Must not crash; returns report safely
    report = PRAnalyzer.analyze(request)
    assert report.total_files_changed == 1
    assert report.files[0].file_path == "app/broken.py"


def test_fastapi_analyze_pr_endpoint():
    payload = {
        "pr_id": "PR-200",
        "repo_name": "AyushPrakash414/Autonomous-Code-Review-Agent",
        "base_sha": "fecb242",
        "head_sha": "9f6ef58",
        "raw_diff": SAMPLE_JAVA_PAYMENT_DIFF,
    }
    response = client.post("/api/v1/analyze/pr", json=payload)
    assert response.status_code == 200

    data = response.json()
    assert data["repo_name"] == "AyushPrakash414/Autonomous-Code-Review-Agent"
    assert data["total_files_changed"] == 2
    assert len(data["signals"]) > 0
    assert "file_type_distribution" in data["summary"]
