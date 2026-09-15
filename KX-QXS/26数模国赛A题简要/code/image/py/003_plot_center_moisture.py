"""Plot center and surface moisture versus time for problems 3 and 4."""
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "code" / "image" / "output"
OUT.mkdir(parents=True, exist_ok=True)


def main():
    plt.figure(figsize=(7.2, 4.6))
    for name, label, style in (("result3.npz", "Problem 3 center", "-"), ("result4.npz", "Problem 4 center", "--")):
        d = np.load(ROOT / "result" / name)
        plt.plot(d["times"] / 3600, d["moisture"][:, 0], style, label=label)
    plt.axhline(0.15, color="black", linestyle=":", label="Threshold 0.15")
    plt.xlabel("Time (h)")
    plt.ylabel("Center dry-basis moisture (kg/kg)")
    plt.title("Center moisture during drying")
    plt.grid(alpha=0.25)
    plt.legend()
    plt.tight_layout()
    plt.savefig(OUT / "center_moisture_comparison.svg", format="svg")
    plt.close()


if __name__ == "__main__":
    main()
