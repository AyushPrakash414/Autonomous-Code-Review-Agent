import ast
import re
from typing import List, Optional, Set

from app.schemas.pr_analysis import ChangedSymbol, SymbolType, ChangeType, FileAnalysis


class ASTSymbolExtractor:

    JAVA_CLASS_PATTERN = re.compile(
        r"^(?:\s*(?:@\w+(?:\([^)]*\))?\s*)*)?\s*(?:public|protected|private)?\s*(?:abstract|static|final)?\s*(class|interface|enum|record)\s+(\w+)",
        re.MULTILINE,
    )
    JAVA_METHOD_PATTERN = re.compile(
        r"^\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:public|protected|private)\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+)*(?:<[^>]+>\s+)?([\w<>\[\],\s\?]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\s*[{;]",
        re.MULTILINE,
    )
    JAVA_ANNOTATION_PATTERN = re.compile(r"@(\w+)(?:\([^)]*\))?")

    @classmethod
    def extract_symbols(
        cls,
        file_analysis: FileAnalysis,
        file_content: Optional[str] = None
    ) -> List[ChangedSymbol]:
        if not file_content and not file_analysis.raw_hunks:
            return []

        changed_lines: Set[int] = set()
        for hunk in file_analysis.raw_hunks:
            changed_lines.update(hunk.added_lines)

        if not changed_lines and file_analysis.change_type != ChangeType.DELETED:
            return []

        lang = file_analysis.language.lower()
        if lang == "python":
            return cls._extract_python_symbols(file_analysis.file_path, file_content, changed_lines, file_analysis.change_type)
        elif lang in ("java", "kotlin", "scala"):
            return cls._extract_java_symbols(file_analysis.file_path, file_content, changed_lines, file_analysis.change_type)
        elif lang in ("typescript", "javascript"):
            return cls._extract_js_symbols(file_analysis.file_path, file_content, changed_lines, file_analysis.change_type)

        return []

    @classmethod
    def _extract_python_symbols(
        cls,
        file_path: str,
        content: Optional[str],
        changed_lines: Set[int],
        change_type: ChangeType
    ) -> List[ChangedSymbol]:
        if not content:
            return []

        try:
            tree = ast.parse(content, filename=file_path)
        except SyntaxError:
            # Syntax-error fallback
            return []

        symbols: List[ChangedSymbol] = []

        class PythonSymbolVisitor(ast.NodeVisitor):
            def __init__(self):
                self.current_class: Optional[str] = None

            def visit_ClassDef(self, node: ast.ClassDef):
                end_lineno = getattr(node, "end_lineno", node.lineno)
                node_lines = set(range(node.lineno, end_lineno + 1))
                if node_lines & changed_lines:
                    symbols.append(
                        ChangedSymbol(
                            symbol_name=node.name,
                            symbol_type=SymbolType.CLASS,
                            file_path=file_path,
                            start_line=node.lineno,
                            end_line=end_lineno,
                            change_type=change_type,
                            is_public=not node.name.startswith("_"),
                            parent_symbol=self.current_class,
                            annotations=[ast.unparse(dec) if hasattr(ast, "unparse") else str(dec) for dec in node.decorator_list],
                        )
                    )

                prev_class = self.current_class
                self.current_class = node.name
                self.generic_visit(node)
                self.current_class = prev_class

            def visit_FunctionDef(self, node: ast.FunctionDef):
                self._handle_func(node)

            def visit_AsyncFunctionDef(self, node: ast.AsyncFunctionDef):
                self._handle_func(node)

            def _handle_func(self, node):
                end_lineno = getattr(node, "end_lineno", node.lineno)
                node_lines = set(range(node.lineno, end_lineno + 1))
                if node_lines & changed_lines:
                    sym_type = SymbolType.METHOD if self.current_class else SymbolType.FUNCTION
                    symbols.append(
                        ChangedSymbol(
                            symbol_name=node.name,
                            symbol_type=sym_type,
                            file_path=file_path,
                            start_line=node.lineno,
                            end_line=end_lineno,
                            change_type=change_type,
                            is_public=not node.name.startswith("_"),
                            parent_symbol=self.current_class,
                            annotations=[ast.unparse(dec) if hasattr(ast, "unparse") else str(dec) for dec in node.decorator_list],
                        )
                    )
                self.generic_visit(node)

        visitor = PythonSymbolVisitor()
        visitor.visit(tree)
        return symbols

    @classmethod
    def _extract_java_symbols(
        cls,
        file_path: str,
        content: Optional[str],
        changed_lines: Set[int],
        change_type: ChangeType
    ) -> List[ChangedSymbol]:
        if not content:
            return []

        symbols: List[ChangedSymbol] = []
        lines = content.splitlines()

        current_class: Optional[str] = None
        pending_annotations: List[str] = []

        for line_idx, line in enumerate(lines, start=1):
            stripped = line.strip()

            # Track annotations
            anno_matches = cls.JAVA_ANNOTATION_PATTERN.findall(stripped)
            if anno_matches and stripped.startswith("@"):
                pending_annotations.extend(anno_matches)
                continue

            # Check for class/interface/enum definition
            class_match = re.search(r"\b(class|interface|enum|record)\s+(\w+)", stripped)
            if class_match and not stripped.startswith("//") and not stripped.startswith("*"):
                kind = class_match.group(1)
                name = class_match.group(2)
                current_class = name
                sym_type = SymbolType.INTERFACE if kind == "interface" else SymbolType.ENUM if kind == "enum" else SymbolType.CLASS

                # Approximate end line to end of file or next class
                start_l = line_idx
                end_l = min(len(lines), start_l + 200)

                if any(start_l <= l <= end_l for l in changed_lines):
                    symbols.append(
                        ChangedSymbol(
                            symbol_name=name,
                            symbol_type=sym_type,
                            file_path=file_path,
                            start_line=start_l,
                            end_line=end_l,
                            change_type=change_type,
                            is_public="public" in stripped or "interface" in stripped,
                            annotations=list(pending_annotations),
                        )
                    )
                pending_annotations = []
                continue

            # Check for method definition
            method_match = re.search(
                r"(public|protected|private)?\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+)*(?:<[^>]+>\s+)?([\w<>\[\],\s\?]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?\s*\{?",
                stripped
            )
            if method_match and not stripped.startswith("//") and not stripped.startswith("*") and not stripped.startswith("if") and not stripped.startswith("for") and not stripped.startswith("while"):
                access = method_match.group(1) or "package-private"
                method_name = method_match.group(3)
                if method_name not in ("if", "for", "while", "switch", "catch", "new", "return"):
                    start_l = line_idx
                    end_l = min(len(lines), start_l + 50)
                    if any(start_l <= l <= end_l for l in changed_lines):
                        symbols.append(
                            ChangedSymbol(
                                symbol_name=method_name,
                                symbol_type=SymbolType.METHOD,
                                file_path=file_path,
                                start_line=start_l,
                                end_line=end_l,
                                change_type=change_type,
                                is_public=access == "public",
                                parent_symbol=current_class,
                                annotations=list(pending_annotations),
                            )
                        )
                pending_annotations = []
                continue

            if not stripped.startswith("@"):
                pending_annotations = []

        return symbols

    @classmethod
    def _extract_js_symbols(
        cls,
        file_path: str,
        content: Optional[str],
        changed_lines: Set[int],
        change_type: ChangeType
    ) -> List[ChangedSymbol]:
        if not content:
            return []

        symbols: List[ChangedSymbol] = []
        lines = content.splitlines()

        for line_idx, line in enumerate(lines, start=1):
            stripped = line.strip()

            # Class definition
            class_match = re.search(r"\bclass\s+(\w+)", stripped)
            if class_match:
                name = class_match.group(1)
                start_l = line_idx
                end_l = min(len(lines), start_l + 150)
                if any(start_l <= l <= end_l for l in changed_lines):
                    symbols.append(
                        ChangedSymbol(
                            symbol_name=name,
                            symbol_type=SymbolType.CLASS,
                            file_path=file_path,
                            start_line=start_l,
                            end_line=end_l,
                            change_type=change_type,
                            is_public="export" in stripped,
                        )
                    )

            # Function / Arrow function
            func_match = re.search(r"\b(?:function\s+(\w+)|(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s*)?\([^)]*\)\s*=>)", stripped)
            if func_match:
                name = func_match.group(1) or func_match.group(2)
                if name:
                    start_l = line_idx
                    end_l = min(len(lines), start_l + 40)
                    if any(start_l <= l <= end_l for l in changed_lines):
                        symbols.append(
                            ChangedSymbol(
                                symbol_name=name,
                                symbol_type=SymbolType.FUNCTION,
                                file_path=file_path,
                                start_line=start_l,
                                end_line=end_l,
                                change_type=change_type,
                                is_public="export" in stripped,
                            )
                        )

        return symbols
