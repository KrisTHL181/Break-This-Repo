#! /usr/bin/env python3

package: dict = {
    "ID": "thon-code-gui",
    "Name": "Thon Code",
    "Path": ".main.libs.cfg_handle",
    "Entrance": "main.py"
}

import json
import os
import sys
import libs.json_handle as json_handle

DEFAULT_CFG_FILE_PATH: str = "assets/config.json"
DEFAULT_CFG_DATA: dict = {
    "language": "zh_cn",
    "theme": "dark",
    "auto_save": True,
    "auto_save_time_sec": 15,
    "tab_size": 4,
    "font_ligatures": True,
    "plugin_marketplace_url": "",
}

class cfg_handle:
    def __init__(self, 
        cfg_file_path: str = DEFAULT_CFG_FILE_PATH) -> None:

        self.cfg_file_path = cfg_file_path
        return None

    def make_cfg_file(self, default_data: dict = DEFAULT_CFG_DATA) -> dict:
        try:
            with open(self.cfg_file_path, "w", encoding="utf-8") as f:
                f.write(json.dumps(default_data, ensure_ascii=False, indent=4))

                return_msg = {
                    "status": "success",
                    "data": f"Configuration file created successfully at {self.cfg_file_path}.",
                    "code": 200
                }
            return return_msg
        except Exception as e:
            return_msg = {
                "status": "error",
                "data": str(e),
                "code": 405
            }
            return return_msg

    def check_cfg_file(self) -> dict:
        try:
            if os.path.exists(self.cfg_file_path):
                return_msg = {
                    "status": "success",
                    "data": f"Configuration file exists: {self.cfg_file_path}",
                    "code": 201
                }
                return return_msg
            else:
                self.make_cfg_file()
                return_msg = {
                    "status": "success",
                    "data": f"None",
                    "code": 200
                }
                return return_msg
        except Exception as e:
            return_msg = {
                "status": "error",
                "data": str(e),
                "code": 405
            }

    def read_cfg(self) -> dict:
        try:
            with open(self.cfg_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return {"status": "success", "data": data, "code": 200}
        except (json.JSONDecodeError, FileNotFoundError):
            self.make_cfg_file()
            with open(self.cfg_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
            return {"status": "success", "data": data, "code": 200}

    def write_cfg(self, data: dict) -> dict:
        try:
            with open(self.cfg_file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=4)
            return {"status": "success", "data": "配置已保存", "code": 200}
        except Exception as e:
            return {"status": "error", "data": str(e), "code": 405}

    def get_project_config(self, project_path):
        """Read the .thoncode/project.json config from project root; return defaults if not found"""
        config_dir = os.path.join(project_path, ".thoncode")
        config_file = os.path.join(config_dir, "project.json")
        default = {
            "python_path": sys.executable,   # Project-specific interpreter
            "dependencies": []               # Dependency list read from requirements.txt
        }
        if os.path.exists(config_file):
            with open(config_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                # Merge defaults to ensure fields exist
                for key in default:
                    if key not in data:
                        data[key] = default[key]
                return data
        else:
            # Try reading requirements.txt to initialize dependencies
            req_file = os.path.join(project_path, "requirements.txt")
            deps = []
            if os.path.exists(req_file):
                with open(req_file, "r", encoding="utf-8") as f:
                    deps = [line.strip() for line in f if line.strip() and not line.startswith("#")]
            default["dependencies"] = deps
            # Auto-create config file
            os.makedirs(config_dir, exist_ok=True)
            with open(config_file, "w", encoding="utf-8") as f:
                json.dump(default, f, indent=4, ensure_ascii=False)
            return default

    def save_project_config(self, project_path, config):
        """Save project config to .thoncode/project.json"""
        config_dir = os.path.join(project_path, ".thoncode")
        os.makedirs(config_dir, exist_ok=True)
        config_file = os.path.join(config_dir, "project.json")
        with open(config_file, "w", encoding="utf-8") as f:
            json.dump(config, f, indent=4, ensure_ascii=False)

    def get_recent_projects(self, limit=10):
        """Read recent projects list from global config"""
        cfg = self.read_cfg()["data"]
        return cfg.get("recent_projects", [])[:limit]

    def add_recent_project(self, project_path):
        """Add project to recent list (deduplicate, move to top)"""
        cfg = self.read_cfg()["data"]
        recent = cfg.get("recent_projects", [])
        if project_path in recent:
            recent.remove(project_path)
        recent.insert(0, project_path)
        # Maintain count limit (e.g., 10)
        if len(recent) > 10:
            recent = recent[:10]
        cfg["recent_projects"] = recent
        self.write_cfg(cfg)