from typing import Dict, Optional, List
from collections import Counter

from app.schemas.pr_analysis import (
    PRAnalysisRequest,
    PRAnalysisReport,
    FileAnalysis,
    TestMapping,
    CapabilitySignal,
)
from app.analyzer.diff_parser import DiffParser
from app.analyzer.ast_extractor import ASTSymbolExtractor
from app.analyzer.test_mapper import TestMapper
from app.analyzer.signal_detector import SignalDetector


class PRAnalyzer:

    @classmethod
    def analyze(cls, request: PRAnalysisRequest) -> PRAnalysisReport:
        # 1. Parse Unified Git Diff into structured FileAnalysis list
        files: List[FileAnalysis] = DiffParser.parse_unified_diff(request.raw_diff)

        file_contents: Dict[str, str] = request.file_contents or {}

        # 2. Extract AST Symbols for each changed file
        for f in files:
            content = file_contents.get(f.file_path, "")
            # If no full file content was supplied, reconstruct snippet aligned to line numbers
            if not content and f.raw_hunks:
                content = cls._reconstruct_content_from_hunks(f.raw_hunks)
            f.symbols_changed = ASTSymbolExtractor.extract_symbols(f, content)

        # 3. Discover Tests & Map Source to Test Coverage Status
        test_mappings: List[TestMapping] = TestMapper.analyze_test_mappings(files)

        # 4. Detect Capability Signals (Security, Architecture, Test, Quality)
        signals: List[CapabilitySignal] = SignalDetector.detect_signals(
            files, test_mappings, file_contents
        )

        # 5. Compute Quantitative Metrics & Summary
        total_additions = sum(f.additions for f in files)
        total_deletions = sum(f.deletions for f in files)
        file_types = [f.file_type.value for f in files]
        signal_categories = [s.category.value for s in signals]

        summary = {
            "file_type_distribution": dict(Counter(file_types)),
            "signal_category_counts": dict(Counter(signal_categories)),
            "total_signals_detected": len(signals),
            "untested_public_apis_count": sum(
                len(tm.untested_public_symbols) for tm in test_mappings
            ),
        }

        return PRAnalysisReport(
            pr_id=request.pr_id,
            repo_name=request.repo_name,
            base_sha=request.base_sha,
            head_sha=request.head_sha,
            total_files_changed=len(files),
            total_additions=total_additions,
            total_deletions=total_deletions,
            files=files,
            signals=signals,
            test_mappings=test_mappings,
            summary=summary,
        )

    @classmethod
    def _reconstruct_content_from_hunks(cls, hunks) -> str:
        line_map: Dict[int, str] = {}
        for hunk in hunks:
            current_line = hunk.new_start
            for line in hunk.content.splitlines():
                if line.startswith("+") and not line.startswith("+++"):
                    line_map[current_line] = line[1:]
                    current_line += 1
                elif line.startswith(" "):
                    line_map[current_line] = line[1:]
                    current_line += 1
                elif not line.startswith("-") and not line.startswith("\\"):
                    line_map[current_line] = line
                    current_line += 1

        if not line_map:
            return ""

        max_line = max(line_map.keys())
        reconstructed = []
        for l_idx in range(1, max_line + 1):
            reconstructed.append(line_map.get(l_idx, ""))
        return "\n".join(reconstructed)

