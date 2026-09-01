import re
from typing import List, Dict, Optional

from app.schemas.pr_analysis import (
    FileAnalysis,
    CapabilitySignal,
    SignalCategory,
    TestMapping,
    TestCoverageStatus,
    FileType,
)


class SignalDetector:

    SQL_CONCAT_PATTERNS = [
        re.compile(r'("SELECT\s+.*"\s*\+|"INSERT\s+INTO\s+.*"\s*\+|"UPDATE\s+.*"\s*\+|"DELETE\s+FROM\s+.*"\s*\+)', re.IGNORECASE),
        re.compile(r'f["\'](?:SELECT|INSERT|UPDATE|DELETE)\s+.*\{.+\}', re.IGNORECASE),
        re.compile(r'createNativeQuery\s*\(\s*["\'].*["\']\s*\+', re.IGNORECASE),
        re.compile(r'execute(?:Query|Update)?\s*\(\s*["\'].*["\']\s*\+', re.IGNORECASE),
        re.compile(r'cursor\.execute\s*\(\s*f["\']', re.IGNORECASE),
    ]

    HARDCODED_SECRET_PATTERNS = [
        (re.compile(r'ghp_[a-zA-Z0-9]{20,}'), "GITHUB_PERSONAL_ACCESS_TOKEN_EXPOSED"),
        (re.compile(r'AKIA[0-9A-Z]{16}'), "AWS_ACCESS_KEY_EXPOSED"),
        (re.compile(r'-----BEGIN\s+(?:RSA\s+)?PRIVATE\s+KEY-----'), "PRIVATE_KEY_EXPOSED"),
        (re.compile(r'(?:api[_-]?key|secret|password|auth[_-]?token)\s*=\s*["\'][a-zA-Z0-9_\-]{16,}["\']', re.IGNORECASE), "HARDCODED_CREDENTIAL_FOUND"),
    ]

    UNSAFE_DESERIALIZE_PATTERNS = [
        re.compile(r'pickle\.loads?\s*\('),
        re.compile(r'yaml\.load\s*\([^,)]+\)(?!\s*,\s*Loader=yaml\.SafeLoader)'),
        re.compile(r'\.readObject\s*\(\s*\)'),
    ]

    CONTROLLER_INJECTS_REPO = re.compile(
        r'(?:@Autowired\s+)?(?:private|protected)?\s+(?:final\s+)?(\w+Repository)\s+\w+;',
        re.MULTILINE
    )

    @classmethod
    def detect_signals(
        cls,
        files: List[FileAnalysis],
        test_mappings: List[TestMapping],
        file_contents: Optional[Dict[str, str]] = None
    ) -> List[CapabilitySignal]:
        signals: List[CapabilitySignal] = []

        for f in files:
            content = (file_contents or {}).get(f.file_path, "")
            cls._detect_file_signals(f, content, signals)

        cls._detect_test_signals(files, test_mappings, signals)
        cls._detect_architecture_signals(files, file_contents or {}, signals)

        return signals

    @classmethod
    def _detect_file_signals(cls, f: FileAnalysis, content: str, signals: List[CapabilitySignal]):
        # Analyze each hunk
        for hunk in f.raw_hunks:
            hunk_lines = hunk.content.splitlines()
            for line_idx, line in enumerate(hunk_lines):
                if not line.startswith("+") or line.startswith("+++"):
                    continue

                clean_line = line[1:].strip()
                line_no = hunk.new_start + line_idx

                # 1. Security: SQL Injection / String Concatenation
                for pat in cls.SQL_CONCAT_PATTERNS:
                    if pat.search(clean_line):
                        signals.append(
                            CapabilitySignal(
                                signal_name="SQL_CONCATENATION_DETECTED",
                                category=SignalCategory.SECURITY,
                                confidence=0.95,
                                description=f"Dynamic string concatenation detected in SQL query: `{clean_line[:60]}...`",
                                file_path=f.file_path,
                                line_number=line_no,
                                metadata={"snippet": clean_line, "vulnerability_type": "CWE-89_SQL_INJECTION"},
                            )
                        )
                        break

                # 2. Security: Hardcoded Secrets
                for pat, sig_name in cls.HARDCODED_SECRET_PATTERNS:
                    if pat.search(clean_line):
                        signals.append(
                            CapabilitySignal(
                                signal_name=sig_name,
                                category=SignalCategory.SECURITY,
                                confidence=0.98,
                                description=f"Potential hardcoded secret or token detected: `{clean_line[:40]}...`",
                                file_path=f.file_path,
                                line_number=line_no,
                                metadata={"snippet": clean_line},
                            )
                        )
                        break

                # 3. Security: Unsafe Deserialization
                for pat in cls.UNSAFE_DESERIALIZE_PATTERNS:
                    if pat.search(clean_line):
                        signals.append(
                            CapabilitySignal(
                                signal_name="UNSAFE_DESERIALIZATION_DETECTED",
                                category=SignalCategory.SECURITY,
                                confidence=0.90,
                                description=f"Unsafe object deserialization call detected: `{clean_line[:60]}`",
                                file_path=f.file_path,
                                line_number=line_no,
                                metadata={"snippet": clean_line, "vulnerability_type": "CWE-502_DESERIALIZATION"},
                            )
                        )
                        break

        # 4. Quality: Cyclomatic Complexity & Large Methods
        for sym in f.symbols_changed:
            span = sym.end_line - sym.start_line
            if span > 60:
                signals.append(
                    CapabilitySignal(
                        signal_name="LARGE_METHOD_SIZE",
                        category=SignalCategory.QUALITY,
                        confidence=0.85,
                        description=f"Method `{sym.symbol_name}` is {span} lines long (threshold: 60 lines)",
                        file_path=f.file_path,
                        line_number=sym.start_line,
                        metadata={"symbol_name": sym.symbol_name, "line_count": span},
                    )
                )

    @classmethod
    def _detect_test_signals(
        cls,
        files: List[FileAnalysis],
        test_mappings: List[TestMapping],
        signals: List[CapabilitySignal]
    ):
        for tm in test_mappings:
            if tm.test_coverage_status == TestCoverageStatus.MISSING:
                signals.append(
                    CapabilitySignal(
                        signal_name="MISSING_TEST_FOR_PUBLIC_API",
                        category=SignalCategory.TEST,
                        confidence=0.92,
                        description=f"Public symbols {tm.untested_public_symbols} in `{tm.source_file}` were modified without test additions or updates.",
                        file_path=tm.source_file,
                        metadata={
                            "untested_symbols": tm.untested_public_symbols,
                            "expected_test_candidates": tm.test_file_candidates,
                        },
                    )
                )

        # Check if all files in PR are test-only
        non_test_files = [f for f in files if f.file_type != FileType.TEST and f.file_type != FileType.DOCUMENTATION]
        if files and not non_test_files:
            signals.append(
                CapabilitySignal(
                    signal_name="TEST_ONLY_PR",
                    category=SignalCategory.TEST,
                    confidence=1.0,
                    description="This Pull Request modifies only test and documentation files.",
                    file_path=files[0].file_path,
                    metadata={"total_test_files": len(files)},
                )
            )

    @classmethod
    def _detect_architecture_signals(
        cls,
        files: List[FileAnalysis],
        file_contents: Dict[str, str],
        signals: List[CapabilitySignal]
    ):
        for f in files:
            content = file_contents.get(f.file_path, "")
            is_controller = "controller" in f.file_path.lower() or any(
                "RestController" in s.annotations or "Controller" in s.annotations for s in f.symbols_changed
            )

            if is_controller and content:
                # Check for direct Repository injection in Controller
                if cls.CONTROLLER_INJECTS_REPO.search(content) or "Repository " in content:
                    signals.append(
                        CapabilitySignal(
                            signal_name="CONTROLLER_CALLS_REPOSITORY_DIRECTLY",
                            category=SignalCategory.ARCHITECTURE,
                            confidence=0.94,
                            description=f"Controller `{f.file_path}` directly accesses a Repository, violating Controller -> Service -> Repository layer boundaries.",
                            file_path=f.file_path,
                            metadata={"violation": "LAYER_BYPASS"},
                        )
                    )
