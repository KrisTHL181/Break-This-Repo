"""Print key result rows used in the report."""
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
X = [0, 5, 10, 15, 20]


def show(name, query, field=None):
    d = np.load(ROOT / "result" / name)
    times = d["times"]
    arr = d[field] if field else (d["temperature"] if "temperature" in d else d["moisture"])
    print("###", name)
    print("time", *[f"d{x/10:.1f}" for x in X])
    for q in query:
        i = int(np.argmin(np.abs(times - q)))
        print(f"{times[i]:.0f}", *[f"{arr[i,j]:.6f}" for j in X])
    if "moisture" in d and field != "temperature":
        print("shape", arr.shape, "end_h", times[-1] / 3600, "endmax", arr[-1].max())


if __name__ == "__main__":
    show("result1.npz", [100, 300, 600, 900, 1200, 1500, 1800])
    show("result1.npz", [100, 300, 600, 900, 1200, 1500, 1800], "moisture")
    show("result2.npz", [1800, 3600, 5400, 7200, 9000, 10800])
    show("result2.npz", [1800, 3600, 5400, 7200, 9000, 10800], "moisture")
    show("result3.npz", [21600, 43200, 64800, 86400, 108000, 129600, 152400, 305160])
    show("result4.npz", [21600, 43200, 64800, 86400, 108000, 129600, 152400, 260700])
