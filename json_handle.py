#! /usr/bin/env python3

package: dict = {
    "ID": "thon-code-gui",
    "Name": "Thon Code",
    "Path": ".main.libs.json_handle",
    "Entrance": "main.py"
}

import json

class handle:
    def __init__(self, 
        json_file_path: str) -> None:

        self.json_file_path = json_file_path
        return None

    def get_json(self) -> dict:
        try:
            with open(self.json_file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                return_msg = {
                    "status": "success",
                    "data": data,
                    "code": 200
                }
            return return_msg
        except FileNotFoundError:
            return_msg = {
                "status": "error",
                "data": f"File not found: {self.json_file_path}",
                "code": 404
            }
            return return_msg
        except json.JSONDecodeError:
            return_msg = {
                "status": "error",
                "data": f"Invalid JSON format in file: {self.json_file_path}",
                "code": 500
            }
            return return_msg
        except Exception as e:
            return_msg = {
                "status": "error",
                "data": str(e),
                "code": 405
            }
            return return_msg

    def write_json(self, data: dict) -> dict:
        try:
            with open(self.json_file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=4)
                return_msg = {
                    "status": "success",
                    "data": f"Data has been written to {self.json_file_path} successfully.",
                    "code": 200
                }
            return return_msg
        except Exception as e: 
            return_msg = {
                "status": "error",
                "data": str(e),
                "code": 500
            }
            return return_msg