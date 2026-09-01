import os
from typing import List, Set
from pathlib import PurePosixPath

from app.schemas.pr_analysis import FileAnalysis, FileType, TestMapping, TestCoverageStatus


class TestMapper:

    @classmethod
    def analyze_test_mappings(cls, files: List[FileAnalysis]) -> List[TestMapping]:
        test_files: Set[str] = {
            f.file_path for f in files if f.file_type == FileType.TEST
        }

        source_files = [f for f in files if f.file_type == FileType.SOURCE]
        mappings: List[TestMapping] = []

        for source in source_files:
            candidates = cls._generate_test_candidates(source.file_path, source.language)

            # Check if any candidate is present in the PR's changed test files
            matched_tests = [c for c in candidates if any(c.lower() in tf.lower() for tf in test_files)]
            has_tests = len(matched_tests) > 0

            public_symbols = [
                s.symbol_name for s in source.symbols_changed if s.is_public
            ]

            if not public_symbols:
                status = TestCoverageStatus.NOT_APPLICABLE
            elif has_tests:
                status = TestCoverageStatus.COVERED
            else:
                status = TestCoverageStatus.MISSING

            mappings.append(
                TestMapping(
                    source_file=source.file_path,
                    test_file_candidates=candidates,
                    has_associated_tests=has_tests,
                    test_coverage_status=status,
                    untested_public_symbols=public_symbols if status == TestCoverageStatus.MISSING else [],
                )
            )

        return mappings

    @classmethod
    def _generate_test_candidates(cls, file_path: str, language: str) -> List[str]:
        p = PurePosixPath(file_path)
        stem = p.stem
        candidates: List[str] = []

        if language == "java":
            candidates.extend([
                f"{stem}Test.java",
                f"{stem}Tests.java",
                f"{stem}TestCase.java",
                f"{stem}IT.java",
            ])
        elif language == "python":
            candidates.extend([
                f"test_{stem}.py",
                f"{stem}_test.py",
            ])
        elif language in ("typescript", "javascript"):
            ext = p.suffix
            candidates.extend([
                f"{stem}.test{ext}",
                f"{stem}.spec{ext}",
            ])
        else:
            candidates.append(f"test_{stem}")

        return candidates
