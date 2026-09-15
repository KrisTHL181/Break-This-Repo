"""Import shim for the numbered model-core module."""
from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path
import sys

_path = Path(__file__).with_name("001_model_core.py")
_spec = spec_from_file_location("drying_model_core", _path)
_module = module_from_spec(_spec)
sys.modules[_spec.name] = _module
_spec.loader.exec_module(_module)
for _name in dir(_module):
    if not _name.startswith("__"):
        globals()[_name] = getattr(_module, _name)
