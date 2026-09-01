import re
from typing import List, Tuple, Optional
from pathlib import PurePosixPath

from app.schemas.pr_analysis import FileAnalysis, DiffHunk, FileType, ChangeType


EXTENSION_TO_LANGUAGE = {
    ".java": "java",
    ".py": "python",
    ".ts": "typescript",
    ".tsx": "typescript",
    ".js": "javascript",
    ".jsx": "javascript",
    ".go": "go",
    ".rs": "rust",
    ".cpp": "cpp",
    ".c": "c",
    ".h": "c",
    ".cs": "csharp",
    ".php": "php",
    ".rb": "ruby",
    ".kt": "kotlin",
    ".scala": "scala",
    ".sql": "sql",
    ".sh": "bash",
    ".yml": "yaml",
    ".yaml": "yaml",
    ".json": "json",
    ".xml": "xml",
    ".properties": "properties",
    ".toml": "toml",
    ".md": "markdown",
    ".txt": "text",
}

TEST_PATTERNS = [
    r"test[s]?\/",
    r"__tests__\/",
    r".*test\.py$",
    r".*_test\.py$",
    r".*Test\.java$",
    r".*Tests\.java$",
    r".*TestCase\.java$",
    r".*IT\.java$",
    r".*\.spec\.[tj]sx?$",
    r".*\.test\.[tj]sx?$",
]

CONFIG_EXTENSIONS = {".yml", ".yaml", ".json", ".xml", ".properties", ".toml", ".ini", ".env"}
DOC_EXTENSIONS = {".md", ".rst", ".adoc", ".txt"}
INFRA_PATTERNS = [r"docker-compose", r"Dockerfile", r"k8s\/", r"terraform\/", r"\.github\/workflows\/", r"Makefile"]


def determine_file_type(file_path: str) -> FileType:
    path_lower = file_path.lower()

    # Check test patterns
    for pattern in TEST_PATTERNS:
        if re.search(pattern, file_path, re.IGNORECASE):
            return FileType.TEST

    # Check infra patterns
    for pattern in INFRA_PATTERNS:
        if re.search(pattern, file_path, re.IGNORECASE):
            return FileType.INFRASTRUCTURE

    suffix = PurePosixPath(file_path).suffix.lower()
    if suffix in DOC_EXTENSIONS:
        return FileType.DOCUMENTATION
    if suffix in CONFIG_EXTENSIONS:
        return FileType.CONFIG
    if suffix in EXTENSION_TO_LANGUAGE:
        return FileType.SOURCE

    return FileType.UNKNOWN


def detect_language(file_path: str) -> str:
    suffix = PurePosixPath(file_path).suffix.lower()
    return EXTENSION_TO_LANGUAGE.get(suffix, "unknown")


class DiffParser:

    DIFF_FILE_HEADER_RE = re.compile(r"^diff --git a/(.*?) b/(.*?)$")
    HUNK_HEADER_RE = re.compile(r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)$")

    @classmethod
    def parse_unified_diff(cls, raw_diff: str) -> List[FileAnalysis]:
        if not raw_diff or not raw_diff.strip():
            return []

        file_analyses: List[FileAnalysis] = []
        lines = raw_diff.splitlines()

        current_file_path: Optional[str] = None
        current_old_path: Optional[str] = None
        current_change_type: ChangeType = ChangeType.MODIFIED
        current_hunks: List[DiffHunk] = []
        current_hunk_lines: List[str] = []
        current_hunk_meta: Optional[Tuple[int, int, int, int, str]] = None
        current_additions = 0
        current_deletions = 0
        current_added_lines: List[int] = []
        current_deleted_lines: List[int] = []
        current_new_line_ptr = 0
        current_old_line_ptr = 0

        def flush_current_hunk():
            nonlocal current_hunks, current_hunk_lines, current_hunk_meta, current_added_lines, current_deleted_lines
            if current_hunk_meta is not None and current_hunk_lines:
                old_start, old_lines, new_start, new_lines, header = current_hunk_meta
                hunk = DiffHunk(
                    old_start=old_start,
                    old_lines=old_lines,
                    new_start=new_start,
                    new_lines=new_lines,
                    header=header,
                    content="\n".join(current_hunk_lines),
                    added_lines=list(current_added_lines),
                    deleted_lines=list(current_deleted_lines),
                )
                current_hunks.append(hunk)
                current_hunk_lines = []
                current_hunk_meta = None
                current_added_lines = []
                current_deleted_lines = []

        def flush_current_file():
            nonlocal file_analyses, current_file_path, current_old_path, current_change_type
            nonlocal current_hunks, current_additions, current_deletions
            flush_current_hunk()
            if current_file_path:
                file_analysis = FileAnalysis(
                    file_path=current_file_path,
                    old_path=current_old_path if current_old_path != current_file_path else None,
                    file_type=determine_file_type(current_file_path),
                    language=detect_language(current_file_path),
                    change_type=current_change_type,
                    additions=current_additions,
                    deletions=current_deletions,
                    raw_hunks=current_hunks,
                )
                file_analyses.append(file_analysis)

            current_file_path = None
            current_old_path = None
            current_change_type = ChangeType.MODIFIED
            current_hunks = []
            current_additions = 0
            current_deletions = 0

        i = 0
        while i < len(lines):
            line = lines[i]

            # Detect git diff file header: diff --git a/... b/...
            file_match = cls.DIFF_FILE_HEADER_RE.match(line)
            if file_match:
                flush_current_file()
                old_path = file_match.group(1)
                new_path = file_match.group(2)
                current_old_path = old_path
                current_file_path = new_path
                current_change_type = ChangeType.MODIFIED
                i += 1
                continue

            # Detect new/deleted/rename file headers
            if line.startswith("new file mode"):
                current_change_type = ChangeType.ADDED
            elif line.startswith("deleted file mode"):
                current_change_type = ChangeType.DELETED
            elif line.startswith("similarity index"):
                current_change_type = ChangeType.RENAMED
            elif line.startswith("--- a/"):
                if current_change_type != ChangeType.ADDED:
                    current_old_path = line[6:]
            elif line.startswith("+++ b/"):
                if current_change_type != ChangeType.DELETED:
                    current_file_path = line[6:]

            # Detect hunk header: @@ -10,5 +10,8 @@ ...
            hunk_match = cls.HUNK_HEADER_RE.match(line)
            if hunk_match:
                flush_current_hunk()
                old_start = int(hunk_match.group(1))
                old_lines = int(hunk_match.group(2)) if hunk_match.group(2) else 1
                new_start = int(hunk_match.group(3))
                new_lines = int(hunk_match.group(4)) if hunk_match.group(4) else 1
                header = hunk_match.group(5).strip()

                current_hunk_meta = (old_start, old_lines, new_start, new_lines, header)
                current_new_line_ptr = new_start
                current_old_line_ptr = old_start
                i += 1
                continue

            # Inside hunk content lines
            if current_hunk_meta is not None:
                current_hunk_lines.append(line)
                if line.startswith("+") and not line.startswith("+++"):
                    current_additions += 1
                    current_added_lines.append(current_new_line_ptr)
                    current_new_line_ptr += 1
                elif line.startswith("-") and not line.startswith("---"):
                    current_deletions += 1
                    current_deleted_lines.append(current_old_line_ptr)
                    current_old_line_ptr += 1
                elif not line.startswith("\\"):  # Context line (space)
                    current_new_line_ptr += 1
                    current_old_line_ptr += 1

            i += 1

        flush_current_file()
        return file_analyses
