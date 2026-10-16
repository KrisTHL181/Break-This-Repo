# libs/changelog_handle.py

import os
import json
import re
from datetime import datetime
from typing import Optional, List, Dict, Any, Tuple
from dataclasses import dataclass, asdict
from libs.gui.lazy_loader import LazyLoader

@dataclass
class ChangelogEntry:
    """Represents a single changelog entry"""
    version: str
    date: str
    sections: Dict[str, List[str]]  # {'Added': [...], 'Fixed': [...], etc.}
    file_path: Optional[str] = None


class ChangelogHandle:
    """Handle changelog operations for projects"""
    
    def __init__(self, project_root: Optional[str] = None):
        self.project_root = project_root or os.getcwd()
        self.changelog_path = os.path.join(self.project_root, "CHANGELOG.md")
        self.entries: List[ChangelogEntry] = []
    
    def parse_changelog(self, content: str) -> List[ChangelogEntry]:
        """Parse CHANGELOG.md content into structured entries"""
        entries = []
        lines = content.splitlines()
        
        i = 0
        while i < len(lines):
            line = lines[i].strip()
            
            # Detect version header: ## [1.0.0] - 2024-01-01 or ## [Unreleased]
            # Date part is optional for compatibility with undated Unreleased sections
            version_match = re.match(r'^##\s*\[([^\]]+)\](?:\s*[-–]\s*(.+))?$', line)
            if version_match:
                version = version_match.group(1)
                date = version_match.group(2).strip() if version_match.group(2) else ""
                
                # Skip empty lines
                i += 1
                while i < len(lines) and not lines[i].strip():
                    i += 1
                
                # Collect sections (### Added, ### Fixed, etc.)
                sections = {}
                current_section = None
                current_items = []
                
                while i < len(lines):
                    line = lines[i].strip()
                    
                    if not line:
                        i += 1
                        continue
                    
                    # Check for section header: ### Added
                    section_match = re.match(r'^###\s+(.+)$', line)
                    if section_match:
                        # Save previous section
                        if current_section and current_items:
                            sections[current_section] = current_items
                        
                        current_section = section_match.group(1).strip()
                        current_items = []
                        i += 1
                        continue
                    
                    # Check for version end (## next version)
                    if line.startswith('##'):
                        break
                    
                    # Collect list items
                    if line.startswith('-') or line.startswith('*'):
                        item = line[1:].strip()
                        if item:
                            current_items.append(item)
                    elif current_section and line and not line.startswith('#'):
                        # Plain text items (not in list format)
                        current_items.append(line)
                    
                    i += 1
                
                # Save last section
                if current_section and current_items:
                    sections[current_section] = current_items
                
                # Create entry even if no sections (for Unreleased)
                if version or date:
                    entries.append(ChangelogEntry(
                        version=version,
                        date=date,
                        sections=sections
                    ))
            else:
                i += 1
        
        return entries
    
    def load_changelog(self) -> bool:
        """Load changelog from file"""
        if not os.path.exists(self.changelog_path):
            self.entries = []
            return False
        
        try:
            with open(self.changelog_path, 'r', encoding='utf-8') as f:
                content = f.read()
            self.entries = self.parse_changelog(content)
            return True
        except Exception:
            self.entries = []
            return False
    
    def save_changelog(self) -> bool:
        """Save changelog to file"""
        try:
            content = self.generate_markdown()
            with open(self.changelog_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        except Exception:
            return False
    
    def generate_markdown(self) -> str:
        """Generate markdown from entries"""
        if not self.entries:
            return "# Changelog\n\nNo entries yet."
        
        lines = ["# Changelog", ""]
        
        for entry in self.entries:
            if entry.date:
                lines.append(f"## [{entry.version}] - {entry.date}")
            else:
                lines.append(f"## [{entry.version}]")
            lines.append("")
            
            for section_name, items in entry.sections.items():
                lines.append(f"### {section_name}")
                for item in items:
                    lines.append(f"- {item}")
                lines.append("")
            
            lines.append("")
        
        return "\n".join(lines)
    
    def add_entry(self, version: str, date: str, sections: Dict[str, List[str]] = None) -> bool:
        """Add a new changelog entry with sections"""
        if sections is None:
            sections = {}
        
        # Check if version already exists
        for i, entry in enumerate(self.entries):
            if entry.version == version:
                # Update existing
                self.entries[i] = ChangelogEntry(
                    version=version,
                    date=date,
                    sections=sections
                )
                return self.save_changelog()
        
        # Add new entry at the top (latest first), unless it's Unreleased
        new_entry = ChangelogEntry(
            version=version,
            date=date,
            sections=sections
        )
        
        # Insert Unreleased at the top, or regular entries after
        if version == "Unreleased":
            self.entries.insert(0, new_entry)
        else:
            # Find position after Unreleased if it exists
            insert_pos = 0
            for i, entry in enumerate(self.entries):
                if entry.version == "Unreleased":
                    insert_pos = i + 1
                    break
            self.entries.insert(insert_pos, new_entry)
        
        return self.save_changelog()
    
    def remove_entry(self, version: str) -> bool:
        """Remove a changelog entry by version"""
        for i, entry in enumerate(self.entries):
            if entry.version == version:
                del self.entries[i]
                return self.save_changelog()
        return False
    
    def get_entry(self, version: str) -> Optional[ChangelogEntry]:
        """Get a specific entry by version"""
        for entry in self.entries:
            if entry.version == version:
                return entry
        return None
    
    def get_versions(self) -> List[str]:
        """Get all version strings"""
        return [entry.version for entry in self.entries]
    
    def get_latest_version(self) -> Optional[str]:
        """Get the latest version (excluding Unreleased)"""
        for entry in self.entries:
            if entry.version != "Unreleased":
                return entry.version
        return None
    
    def export_to_json(self, file_path: Optional[str] = None) -> bool:
        """Export changelog to JSON"""
        if file_path is None:
            file_path = os.path.join(self.project_root, "changelog.json")
        
        try:
            data = [asdict(entry) for entry in self.entries]
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False
    
    def import_from_json(self, file_path: str) -> bool:
        """Import changelog from JSON"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            self.entries = [ChangelogEntry(**item) for item in data]
            return self.save_changelog()
        except Exception:
            return False
    
    def get_template(self) -> str:
        """Get a template for a new changelog entry"""
        today = datetime.now().strftime("%Y-%m-%d")
        return f"""## [Unreleased]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [1.0.0] - {today}
### Added
- Initial release
"""
    
    def create_empty_changelog(self) -> bool:
        """Create an empty changelog file with template"""
        content = self.get_template()
        try:
            with open(self.changelog_path, 'w', encoding='utf-8') as f:
                f.write(content)
            self.entries = self.parse_changelog(content)
            return True
        except Exception:
            return False
    
    def generate_release_notes(self, version: str) -> Optional[str]:
        """Generate release notes for a specific version"""
        entry = self.get_entry(version)
        if not entry:
            return None
        
        lines = [
            f"# Release Notes - {entry.version}",
        ]
        if entry.date:
            lines.append(f"**Date:** {entry.date}")
        lines.append("")
        
        for section_name, items in entry.sections.items():
            if items:
                lines.append(f"## {section_name}")
                for item in items:
                    lines.append(f"- {item}")
                lines.append("")
        
        return "\n".join(lines)
    
    def get_changelog_preview(self, max_entries: int = 3) -> str:
        """Get a preview of recent changelog entries"""
        if not self.entries:
            return "No changelog entries found."
        
        lines = ["## Recent Changes", ""]
        for entry in self.entries[:max_entries]:
            if entry.date:
                lines.append(f"### [{entry.version}] - {entry.date}")
            else:
                lines.append(f"### [{entry.version}]")
            for section_name, items in list(entry.sections.items())[:2]:
                if items:
                    lines.append(f"**{section_name}:**")
                    for item in items[:3]:
                        lines.append(f"  - {item}")
                    if len(items) > 3:
                        lines.append(f"  - ... and {len(items) - 3} more")
            lines.append("")
        
        return "\n".join(lines)