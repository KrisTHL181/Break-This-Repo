"""Plot radial moisture profiles and drying threshold."""
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "code" / "image" / "output"
OUT.mkdir(parents=True, exist_ok=True)


def plot(npz_name, output_name, title):
    data = np.load(ROOT / "result" / npz_name)
    t = data["times"]
    C = data["moisture"]
    x = np.linspace(0, 2, C.shape[1])
    indices = np.linspace(0, len(t) - 1, min(7, len(t)), dtype=int)
    plt.figure(figsize=(7.2, 4.6))
    for i in indices:
        plt.plot(x, C[i], label=f"{t[i] / 3600:.2f} h")
    plt.axhline(0.15, color="black", linestyle="--", linewidth=1, label="Threshold 0.15")
    plt.xlabel("Equivalent distance from center (cm)")
    plt.ylabel("Dry-basis moisture (kg/kg)")
    plt.title(title)
    plt.grid(alpha=0.25)
    plt.legend(ncol=2, fontsize=8)
    plt.tight_layout()
    plt.savefig(OUT / output_name, format="svg")
    plt.close()


if __name__ == "__main__":
    plot("result3.npz", "moisture_problem3.svg", "Problem 3 moisture profiles")
    plot("result4.npz", "moisture_problem4.svg", "Problem 4 moisture profiles")
