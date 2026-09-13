"""List local fixtures with escaped names; never interpret their contents."""

from pathlib import Path


def main():
    directory = Path(__file__).resolve().parent / "fixtures"
    if not directory.is_dir():
        print("fixtures/ is not checked out here. View it in Git, or clone on Linux.")
        return
    for entry in sorted(directory.iterdir(), key=lambda path: path.name):
        print(f"{len(entry.name.encode('utf-8')):3} bytes  {ascii(entry.name)}")


if __name__ == "__main__":
    main()
