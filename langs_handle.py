#! /usr/bin/env python3

package: dict = {
    "ID": "thon-code-gui",
    "Name": "Thon Code",
    "Path": ".main.libs.langs_handle",
    "Entrance": "main.py"
}

import libs.cfg_handle as cfg_handle

DEFAULT_LANGS_PATH: str = "assets/langs/"

class langs_handle:
    def __init__(self) -> None:
        pass

    def default_language(self) -> dict:
        try:
            cfg_langs: str = cfg_handle.cfg_handle().read_cfg()["data"]["language"]
            return_msg: dict = {
                "status": "success",
                "data": cfg_langs,
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

    def key_languages(self, key: str = "") -> dict:
        try:
            LANGS_JSON:str = self.default_language()["data"]
            LANGS_PATH:str = DEFAULT_LANGS_PATH + LANGS_JSON + ".json"
            JSON_DATA = cfg_handle.cfg_handle(LANGS_PATH).read_cfg()["data"][key]
            return_msg: dict = {
                "status": "success",
                "data": JSON_DATA,
                "code": 200
            }
        except Exception as e:
            return_msg = {
                "status": "error",
                "data": str(e),
                "code": 405
            }
            return return_msg
        return return_msg

    def load_full_language(self) -> dict:
        try:
            lang = self.default_language()["data"]
            path = DEFAULT_LANGS_PATH + lang + ".json"
            full_data = cfg_handle.cfg_handle(path).read_cfg()["data"]
            return full_data
        except Exception as e:
            return {}