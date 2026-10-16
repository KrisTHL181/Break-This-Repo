# libs/license_handle.py
"""License template handling for the Thon Code License Manager.

Supports:
  * Discovery of license templates bundled with the IDE (source tree and
    PyInstaller frozen builds via sys._MEIPASS).
  * Fall-back "builtin templates" written on first run when the
    on-disk ``assets/license`` directory is empty or missing – this
    avoids the classic symptom "template hierarchy broken so I have to
    close the Git window to do anything".
  * Variable interpolation using ``[key]`` and ``{{key}}`` placeholders
    (``year`` / ``author`` are the two the License Manager UI fills in).
"""

import logging
import os
import shutil
import sys
from typing import Optional, Dict, List, Any

_logger = logging.getLogger("thoncode.license_handle")


# ---------------------------------------------------------------------------
# Built-in fall-back templates
# ---------------------------------------------------------------------------
# If no license files exist on disk, we materialise these into the
# configured license dir on first access so the dropdown always has
# useful options. Keys match {name: content}.
_BUILTIN_LICENSES: Dict[str, str] = {
    "MIT": (
        "MIT License\n"
        "\n"
        "Copyright (c) [year] [author]\n"
        "\n"
        "Permission is hereby granted, free of charge, to any person obtaining a copy\n"
        "of this software and associated documentation files (the \"Software\"), to deal\n"
        "in the Software without restriction, including without limitation the rights\n"
        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n"
        "copies of the Software, and to permit persons to whom the Software is\n"
        "furnished to do so, subject to the following conditions:\n"
        "\n"
        "The above copyright notice and this permission notice shall be included in all\n"
        "copies or substantial portions of the Software.\n"
        "\n"
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n"
        "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n"
        "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n"
        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n"
        "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n"
        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n"
        "SOFTWARE.\n"
    ),
    "Apache-2.0": (
        "                                 Apache License\n"
        "                           Version 2.0, January 2004\n"
        "                        http://www.apache.org/licenses/\n"
        "\n"
        "   Copyright [year] [author]\n"
        "\n"
        "   Licensed under the Apache License, Version 2.0 (the \"License\");\n"
        "   you may not use this file except in compliance with the License.\n"
        "   You may obtain a copy of the License at\n"
        "\n"
        "       http://www.apache.org/licenses/LICENSE-2.0\n"
        "\n"
        "   Unless required by applicable law or agreed to in writing, software\n"
        "   distributed under the License is distributed on an \"AS IS\" BASIS,\n"
        "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
        "   See the License for the specific language governing permissions and\n"
        "   limitations under the License.\n"
    ),
    "BSD-2-Clause": (
        "BSD 2-Clause License\n"
        "\n"
        "Copyright (c) [year], [author]\n"
        "All rights reserved.\n"
        "\n"
        "Redistribution and use in source and binary forms, with or without\n"
        "modification, are permitted provided that the following conditions are met:\n"
        "\n"
        "1. Redistributions of source code must retain the above copyright notice,\n"
        "   this list of conditions and the following disclaimer.\n"
        "\n"
        "2. Redistributions in binary form must reproduce the above copyright notice,\n"
        "   this list of conditions and the following disclaimer in the documentation\n"
        "   and/or other materials provided with the distribution.\n"
        "\n"
        "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n"
        "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n"
        "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n"
        "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE\n"
        "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n"
        "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR\n"
        "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER\n"
        "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,\n"
        "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
        "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
    ),
    "BSD-3-Clause": (
        "BSD 3-Clause License\n"
        "\n"
        "Copyright (c) [year], [author]\n"
        "All rights reserved.\n"
        "\n"
        "Redistribution and use in source and binary forms, with or without\n"
        "modification, are permitted provided that the following conditions are met:\n"
        "\n"
        "1. Redistributions of source code must retain the above copyright notice,\n"
        "   this list of conditions and the following disclaimer.\n"
        "\n"
        "2. Redistributions in binary form must reproduce the above copyright notice,\n"
        "   this list of conditions and the following disclaimer in the documentation\n"
        "   and/or other materials provided with the distribution.\n"
        "\n"
        "3. Neither the name of the copyright holder nor the names of its\n"
        "   contributors may be used to endorse or promote products derived from\n"
        "   this software without specific prior written permission.\n"
        "\n"
        "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"\n"
        "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE\n"
        "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n"
        "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE\n"
        "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n"
        "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR\n"
        "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER\n"
        "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,\n"
        "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
        "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
    ),
    "GPL-3.0": (
        "                    GNU GENERAL PUBLIC LICENSE\n"
        "                       Version 3, 29 June 2007\n"
        "\n"
        " Copyright (C) [year] [author]\n"
        " Everyone is permitted to copy and distribute verbatim copies\n"
        " of this license document, but changing it is not allowed.\n"
        "\n"
        "                            Preamble\n"
        "\n"
        "The GNU General Public License is a free, copyleft license for\n"
        "software and other kinds of works.\n"
        "\n"
        "The licenses for most software and other practical works are designed\n"
        "to take away your freedom to share and change the works.  By contrast,\n"
        "the GNU General Public License is intended to guarantee your freedom to\n"
        "share and change all versions of a program--to make sure it remains free\n"
        "software for all its users.\n"
        "\n"
        "For full terms please refer to https://www.gnu.org/licenses/gpl-3.0.txt\n"
    ),
    "MPL-2.0": (
        "Mozilla Public License Version 2.0\n"
        "==================================\n"
        "\n"
        "1. Definitions\n"
        "--------------\n"
        "1.1. \"Contributor\" means each individual or legal entity that creates or\n"
        "contributes to the creation of Covered Software.\n"
        "1.2. \"Covered Software\" means the Source Code Form of the Work distributed\n"
        "under this License.\n"
        "1.3. \"Source Code Form\" means the preferred form of making modifications,\n"
        "including all modules, source files, plus any associated configuration,\n"
        "interface definition, and documentation.\n"
        "\n"
        "Copyright (c) [year] [author]\n"
        "\n"
        "For the full text of the Mozilla Public License, please see:\n"
        "https://mozilla.org/MPL/2.0/\n"
    ),
    "Unlicense": (
        "This is free and unencumbered software released into the public domain.\n"
        "\n"
        "Anyone is free to copy, modify, publish, use, compile, sell, or\n"
        "distribute this software, either in source code form or as a compiled\n"
        "binary, for any purpose, commercial or non-commercial, and by any\n"
        "means.\n"
        "\n"
        "For jurisdictions that do not recognise a dedication into the public\n"
        "domain: the author grants a perpetual, worldwide, royalty-free license\n"
        "to exercise all rights of any kind associated with the Work, by all\n"
        "means and in any form.\n"
        "\n"
        "Copyright is waived for [year] by [author].\n"
    ),
}


def _resolve_default_license_dir() -> str:
    """Return the absolute directory where license templates are stored.

    Priority:
      1. Frozen (PyInstaller) build -> ``{sys.executable}/assets/license``
         next to the binary so users can add their own templates, fall
         back to ``sys._MEIPASS/assets/license``.
      2. Source tree -> the in-tree ``src/thoncode/assets/license``.

    The directory is created if it does not exist.
    """
    base_dir: Optional[str] = None
    frozen = getattr(sys, "frozen", False)
    if frozen:
        exe_dir = os.path.dirname(sys.executable)
        candidate = os.path.join(exe_dir, "assets", "license")
        try:
            os.makedirs(candidate, exist_ok=True)
            base_dir = candidate
        except Exception:
            base_dir = None
        if base_dir is None:
            meipass = getattr(sys, "_MEIPASS", None)
            if meipass:
                base_dir = os.path.join(meipass, "assets", "license")
    if base_dir is None:
        # Walk from this file up to the ``thoncode`` package, then append
        # assets/license. This keeps things robust when the IDE is
        # launched from an arbitrary working directory.
        here = os.path.dirname(os.path.abspath(__file__))  # libs/
        thoncode = os.path.dirname(here)  # thoncode package root
        base_dir = os.path.join(thoncode, "assets", "license")
    try:
        os.makedirs(base_dir, exist_ok=True)
    except Exception as exc:  # pragma: no cover - defensive
        _logger.warning("Unable to create license dir %s: %s", base_dir, exc)
    return base_dir


class LicenseHandle:
    """Handle license operations for projects"""

    def __init__(self, license_dir: Optional[str] = None):
        self.license_dir = license_dir if license_dir else _resolve_default_license_dir()
        # Populate an empty directory with the built-in fall-back set so
        # users never stare at an empty dropdown.
        self._ensure_builtin_templates()

    # ------------------------------------------------------------------
    # Initialisation helpers
    # ------------------------------------------------------------------
    def _ensure_builtin_templates(self) -> None:
        """Write every built-in template that is not already present.

        Missing files are materialised so the dropdown always has a
        default set. Existing files are never overwritten (users can
        edit them without being reverted by the IDE).
        """
        if not self.license_dir:
            return
        try:
            os.makedirs(self.license_dir, exist_ok=True)
        except Exception:
            return
        for name, content in _BUILTIN_LICENSES.items():
            target = os.path.join(self.license_dir, f"{name}.md")
            if os.path.exists(target):
                continue
            try:
                with open(target, "w", encoding="utf-8") as f:
                    f.write(content)
                _logger.info("Materialised built-in license: %s", name)
            except Exception as exc:
                _logger.warning("Failed to materialise %s: %s", name, exc)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------
    def get_available_licenses(self) -> List[str]:
        """Get list of available license templates"""
        if not self.license_dir or not os.path.exists(self.license_dir):
            return []

        licenses = []
        for file in os.listdir(self.license_dir):
            if file.endswith('.md'):
                licenses.append(file[:-3])
        return sorted(licenses)

    def get_license_content(self, license_name: str) -> Optional[str]:
        """Get content of a license template"""
        license_path = os.path.join(self.license_dir, f"{license_name}.md")
        if not os.path.exists(license_path):
            return None

        try:
            with open(license_path, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception:
            return None

    def get_license_path(self, license_name: str) -> Optional[str]:
        """Get full path of a license template"""
        path = os.path.join(self.license_dir, f"{license_name}.md")
        return path if os.path.exists(path) else None

    def add_license(self, license_name: str, content: str) -> bool:
        """Add a new license template"""
        if not license_name:
            return False

        os.makedirs(self.license_dir, exist_ok=True)
        license_path = os.path.join(self.license_dir, f"{license_name}.md")
        try:
            with open(license_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        except Exception:
            return False

    def delete_license(self, license_name: str) -> bool:
        """Delete a license template"""
        license_path = os.path.join(self.license_dir, f"{license_name}.md")
        if not os.path.exists(license_path):
            return False
        try:
            os.remove(license_path)
            return True
        except Exception:
            return False

    def apply_license_to_project(self, project_root: str, license_name: str,
                                  custom_vars: Optional[Dict[str, str]] = None) -> bool:
        """
        Apply a license to a project

        Args:
            project_root: Root directory of the project
            license_name: Name of the license to apply
            custom_vars: Custom variables to replace in template
                         (e.g., {'year': '2024', 'author': 'Name'})
        """
        content = self.get_license_content(license_name)
        if not content:
            return False

        # Replace template variables
        if custom_vars:
            for key, value in custom_vars.items():
                content = content.replace(f'[{key}]', value)
                content = content.replace(f'{{{{{key}}}}}', value)

        # Write LICENSE file to project root
        license_file = os.path.join(project_root, "LICENSE")
        try:
            with open(license_file, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        except Exception:
            return False

    def read_project_license(self, project_root: str) -> Optional[str]:
        """Read LICENSE file from project root"""
        license_file = os.path.join(project_root, "LICENSE")
        if not os.path.exists(license_file):
            return None
        try:
            with open(license_file, 'r', encoding='utf-8') as f:
                return f.read()
        except Exception:
            return None
