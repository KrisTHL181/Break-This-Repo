#! /usr/bin/env python3

import os
import json
import argparse
import fnmatch
import re
from typing import Union, Optional, Dict, List, Any

package: dict = {
    "ID": "thon-code-gui",
    "Name": "Thon Code",
    "Path": ".main.libs.struct_handle",
    "Entrance": "main.py"
}

# Directory Structure Output Function

def get_dir_structure(
    root_path: str,
    output_format: str = "str",  # "str" | "json"
    output_file: Optional[str] = None,
    max_depth: int = -1,
    include_file_content: bool = False,
    content_file_exts: List[str] = ['.py'],
    exclude_patterns: Optional[List[str]] = None,  # wildcard(*,?) / simple regex filters to skip entries
    use_regex: bool = False  # set True to treat exclude patterns as regex, False for fnmatch wildcard
) -> Union[str, Dict[str, Any]]:
    """
    Get the structure of a specified directory with multiple output formats.

    Parameters:
        root_path (str): The root directory path to scan.
        output_format (str): Output format, "str" for human-readable tree, "json" for structured data.
        output_file (Optional[str]): If specified, write the output to this file.
        max_depth (int): Recursion depth, -1 for unlimited, 0 for root only.
        include_file_content (bool): Whether to include file content (only for specified extensions).
        content_file_exts (List[str]): List of file extensions to include content for (default ['.py']).
        exclude_patterns (Optional[List[str]]): List of filter patterns to skip matched files/dirs; supports wildcard or regex.
        use_regex (bool): If True treat exclude_patterns as regular expressions; if False use fnmatch wildcard matching.

    Returns:
        Union[str, Dict[str, Any]]: Returns string or dictionary based on output_format.
    """
    exclude_patterns = exclude_patterns or []
    compiled_regex = []
    if use_regex:
        compiled_regex = [re.compile(pat) for pat in exclude_patterns]

    def _is_excluded(name: str) -> bool:
        """Check whether entry name matches any exclude filter rule"""
        if use_regex:
            return any(r.match(name) for r in compiled_regex)
        else:
            return any(fnmatch.fnmatch(name, pat) for pat in exclude_patterns)

    def _scan_dir(path: str, current_depth: int = 0) -> Dict[str, Any]:
        """Recursively scan a directory and return structured data."""
        if max_depth != -1 and current_depth > max_depth:
            return {"type": "dir", "name": os.path.basename(path), "children": []}

        basename = os.path.basename(path)
        if _is_excluded(basename):
            return {}  # mark entry as excluded

        result = {
            "type": "dir" if os.path.isdir(path) else "file",
            "name": basename,
            "path": os.path.abspath(path)
        }

        if os.path.isdir(path):
            children = []
            try:
                items = os.listdir(path)
                items.sort()  # Keep sorted for predictable output
                for item in items:
                    item_path = os.path.join(path, item)
                    child_data = _scan_dir(item_path, current_depth + 1)
                    if not child_data:
                        continue  # skip excluded child entry
                    if os.path.isdir(item_path):
                        children.append(child_data)
                    else:
                        # Handle files with optional content extraction
                        file_info = child_data
                        if include_file_content:
                            ext = os.path.splitext(item)[1]
                            if ext in content_file_exts:
                                try:
                                    with open(item_path, 'r', encoding='utf-8') as f:
                                        file_info["content"] = f.read()
                                except Exception as e:
                                    file_info["content"] = f"[Error reading file: {e}]"
                        children.append(file_info)
                result["children"] = children
            except PermissionError:
                result["children"] = []
                result["error"] = "Permission denied"
        else:
            # Handle file content if requested
            if include_file_content:
                ext = os.path.splitext(path)[1]
                if ext in content_file_exts:
                    try:
                        with open(path, 'r', encoding='utf-8') as f:
                            result["content"] = f.read()
                    except Exception as e:
                        result["content"] = f"[Error reading file: {e}]"
            result["children"] = []  # Files have no children

        return result

    def _dict_to_tree(data: Dict[str, Any], prefix: str = "", is_last: bool = True) -> str:
        """Convert structured dictionary to tree string."""
        if not data:
            return ""
        lines = []
        name = data.get("name", "")
        if data.get("type") == "dir":
            connector = "└── " if is_last else "├── "
            lines.append(prefix + connector + name + "/")
            children = data.get("children", [])
            if children:
                child_prefix = prefix + ("    " if is_last else "│   ")
                for i, child in enumerate(children):
                    is_last_child = (i == len(children) - 1)
                    sub_str = _dict_to_tree(child, child_prefix, is_last_child)
                    if sub_str:
                        lines.append(sub_str)
        else:
            connector = "└── " if is_last else "├── "
            file_line = prefix + connector + name
            if "content" in data:
                file_line += " (content included)"
            lines.append(file_line)
        return "\n".join(lines)

    # Verify root path exists
    if not os.path.exists(root_path):
        raise FileNotFoundError(f"Path does not exist: {root_path}")

    # Scan directory
    root_data = _scan_dir(root_path)

    if output_format == "str":
        # Human-readable tree structure
        result_str = _dict_to_tree(root_data)
        if output_file:
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(result_str)
        return result_str
    elif output_format == "json":
        # JSON structured output
        if output_file:
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(root_data, f, indent=2, ensure_ascii=False)
        return root_data
    else:
        raise ValueError(f"Unsupported output format: {output_format}")


def main():
    parser = argparse.ArgumentParser(description="Generate directory structure tree or json output")
    parser.add_argument("root", help="Root directory to scan")
    parser.add_argument("--format", default="str", choices=["str", "json"], help="Output format")
    parser.add_argument("--output", default=None, help="Write result to target file")
    parser.add_argument("--max-depth", type=int, default=-1, help="Max scan depth, -1 unlimited")
    parser.add_argument("--include-content", action="store_true", help="Read content of matched file extensions")
    parser.add_argument("--content-exts", nargs="+", default=[".py"], help="Extensions for content reading")
    parser.add_argument("--exclude", nargs="+", default=[], help="Exclude name patterns (wildcard by default)")
    parser.add_argument("--use-regex", action="store_true", help="Treat exclude patterns as regular expression")
    args = parser.parse_args()

    res = get_dir_structure(
        root_path=args.root,
        output_format=args.format,
        output_file=args.output,
        max_depth=args.max_depth,
        include_file_content=args.include_content,
        content_file_exts=args.content_exts,
        exclude_patterns=args.exclude,
        use_regex=args.use_regex
    )
    if args.format == "str" and not args.output:
        print(res)

if __name__ == "__main__":
    main()
