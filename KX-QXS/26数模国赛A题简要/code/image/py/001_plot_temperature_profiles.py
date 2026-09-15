"""Plot radial temperature profiles for problems 1 and 2."""
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
    T = data["temperature"]
    x = np.linspace(0, 2, T.shape[1])
    indices = np.linspace(0, len(t) - 1, min(7, len(t)), dtype=int)
    plt.figure(figsize=(7.2, 4.6))
    for i in indices:
        plt.plot(x, T[i], label=f"{t[i] / 3600:.2f} h")
    plt.xlabel("Equivalent distance from center (cm)")
    plt.ylabel("Temperature (deg C)")
    plt.title(title)
    plt.grid(alpha=0.25)
    plt.legend(ncol=2, fontsize=8)
    plt.tight_layout()
    plt.savefig(OUT / output_name, format="svg")
    plt.close()


if __name__ == "__main__":
    plot("result1.npz", "temperature_problem1.svg", "Problem 1 temperature profiles")
    plot("result2.npz", "temperature_problem2.svg", "Problem 2 temperature profiles")
