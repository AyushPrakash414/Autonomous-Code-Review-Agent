from enum import Enum
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class FileType(str, Enum):
    SOURCE = "SOURCE"
    TEST = "TEST"
    CONFIG = "CONFIG"
    DOCUMENTATION = "DOCUMENTATION"
    INFRASTRUCTURE = "INFRASTRUCTURE"
    ASSET = "ASSET"
    UNKNOWN = "UNKNOWN"


class ChangeType(str, Enum):
    ADDED = "ADDED"
    MODIFIED = "MODIFIED"
    DELETED = "DELETED"
    RENAMED = "RENAMED"


class SymbolType(str, Enum):
    CLASS = "CLASS"
    METHOD = "METHOD"
    FUNCTION = "FUNCTION"
    INTERFACE = "INTERFACE"
    ENUM = "ENUM"
    FIELD = "FIELD"
    UNKNOWN = "UNKNOWN"


class SignalCategory(str, Enum):
    SECURITY = "SECURITY"
    ARCHITECTURE = "ARCHITECTURE"
    TEST = "TEST"
    QUALITY = "QUALITY"


class CoverageStatus(str, Enum):
    COVERED = "COVERED"
    MISSING = "MISSING"
    PARTIAL = "PARTIAL"
    NOT_APPLICABLE = "NOT_APPLICABLE"


# Prevent pytest from treating schema as a test class
CoverageStatus.__test__ = False
TestCoverageStatus = CoverageStatus


class DiffHunk(BaseModel):
    old_start: int
    old_lines: int
    new_start: int
    new_lines: int
    header: str
    content: str
    added_lines: List[int] = Field(default_factory=list)
    deleted_lines: List[int] = Field(default_factory=list)


class ChangedSymbol(BaseModel):
    symbol_name: str
    symbol_type: SymbolType
    file_path: str
    start_line: int
    end_line: int
    change_type: ChangeType
    is_public: bool = True
    parent_symbol: Optional[str] = None
    annotations: List[str] = Field(default_factory=list)


class CapabilitySignal(BaseModel):
    signal_name: str
    category: SignalCategory
    confidence: float = Field(ge=0.0, le=1.0)
    description: str
    file_path: str
    line_number: Optional[int] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class FileAnalysis(BaseModel):
    file_path: str
    old_path: Optional[str] = None
    file_type: FileType
    language: str
    change_type: ChangeType
    additions: int = 0
    deletions: int = 0
    symbols_changed: List[ChangedSymbol] = Field(default_factory=list)
    raw_hunks: List[DiffHunk] = Field(default_factory=list)


class TestMapping(BaseModel):
    source_file: str
    test_file_candidates: List[str] = Field(default_factory=list)
    has_associated_tests: bool = False
    test_coverage_status: TestCoverageStatus = TestCoverageStatus.NOT_APPLICABLE
    untested_public_symbols: List[str] = Field(default_factory=list)


class PRAnalysisReport(BaseModel):
    pr_id: Optional[str] = None
    repo_name: str
    base_sha: str
    head_sha: str
    total_files_changed: int
    total_additions: int
    total_deletions: int
    files: List[FileAnalysis] = Field(default_factory=list)
    signals: List[CapabilitySignal] = Field(default_factory=list)
    test_mappings: List[TestMapping] = Field(default_factory=list)
    summary: Dict[str, Any] = Field(default_factory=dict)


class PRAnalysisRequest(BaseModel):
    pr_id: Optional[str] = None
    repo_name: str
    base_sha: str
    head_sha: str
    raw_diff: str
    file_contents: Optional[Dict[str, str]] = None
