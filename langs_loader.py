package: dict = {
    "ID": "thon-code-gui",
    "Name": "Thon Code",
    "Path": ".main.libs.langs_loader",
    "Entrance": "main.py"
}

import libs.langs_handle as langs_handle

langs_key = langs_handle.langs_handle()

class langs:
    """Dynamic language container; class attributes are populated from the
    current language JSON file. Call reload() after changing the config
    language to refresh all i18n strings at runtime.
    """
    _lang_data = langs_key.load_full_language()

    @classmethod
    def reload(cls) -> None:
        """Reload language data from the current config language.

        Re-reads the language JSON file specified in config and re-populates
        class attributes so all modules using langs see the new translations
        immediately without a restart.
        """
        cls._lang_data = langs_key.load_full_language()
        for key, value in cls._lang_data.items():
            attr_name = key.replace(".", "_")
            setattr(cls, attr_name, value)

for key, value in langs._lang_data.items():
    attr_name = key.replace(".", "_")
    setattr(langs, attr_name, value)