"""Axisymmetric radial finite-volume solver for the drying problem.

The module is intentionally self-contained so the numbered driver scripts can
be run from the repository root with the supplied virtual environment.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import numpy as np
import pandas as pd


ROOT = Path(__file__).resolve().parents[1]
N = 21
XI = np.linspace(0.0, 1.0, N)


def load_environment():
    path = next(ROOT.joinpath("A题", "附件").glob("附件1.xlsx"))
    raw = pd.read_excel(path, header=None).iloc[1:, :3]
    raw.columns = ["time", "temperature", "moisture"]
    return raw.astype(float).to_numpy()


def load_radius():
    path = next(ROOT.joinpath("A题", "附件").glob("附件2.xlsx"))
    raw = pd.read_excel(path, header=None).iloc[1:, :2]
    raw.columns = ["time", "radius_cm"]
    return raw.astype(float).to_numpy()


ENV = load_environment()
RAD = load_radius()


def environment(t: float) -> tuple[float, float]:
    """Return oven temperature (deg C) and moisture at time t (seconds)."""
    te = np.interp(t, ENV[:, 0], ENV[:, 1])
    ce = np.interp(t, ENV[:, 0], ENV[:, 2])
    if t > 1800.0:
        te = float(np.interp(1800.0, ENV[:, 0], ENV[:, 1]))
        ce = float(np.interp(1800.0, ENV[:, 0], ENV[:, 2]))
    return float(te), float(ce)


def radius_m(t: float) -> float:
    return float(np.interp(t, RAD[:, 0], RAD[:, 1])) * 1e-2


@dataclass(frozen=True)
class Material:
    rho: callable
    cp: callable
    k: callable
    diffusivity: callable
    h: float
    hm: float


def constant_material() -> Material:
    return Material(
        rho=lambda c, t: np.full_like(c, 820.0),
        cp=lambda c, t: np.full_like(c, 2600.0),
        k=lambda c, t: np.full_like(c, 0.36),
        diffusivity=lambda c, t: 7e-9 * np.exp(-0.89 / np.maximum(c, 1e-8)),
        h=25.0,
        hm=8e-7,
    )


def variable_material_23() -> Material:
    return Material(
        rho=lambda c, t: 650.0 + 128.0 * c,
        cp=lambda c, t: 1450.0 + 2736.0 * c / (c + 1.0),
        k=lambda c, t: 0.21 + 0.38 * c / (c + 1.0),
        diffusivity=lambda c, t: 2.4e-3 * np.exp(-0.45 / np.maximum(c, 1e-8)) * np.exp(-3850.0 / np.maximum(t + 273.15, 250.0)),
        h=25.0,
        hm=8e-7,
    )


def variable_material_4() -> Material:
    return Material(
        rho=lambda c, t: 760.0 + 90.0 * c,
        cp=lambda c, t: 1850.0 + 2150.0 * c / (c + 1.0),
        k=lambda c, t: 0.12 + 0.20 * c / (c + 1.0),
        diffusivity=lambda c, t: 4.2e-4 * np.exp(-0.30 / np.maximum(c, 1e-8)) * np.exp(-3850.0 / np.maximum(t + 273.15, 250.0)),
        h=25.0,
        hm=8e-7,
    )


def geometry(radius: float):
    """Control-volume geometry, with pi and cylinder length cancelled."""
    dr = radius / (N - 1)
    nodes = XI * radius
    west = np.maximum(nodes - dr / 2.0, 0.0)
    east = np.minimum(nodes + dr / 2.0, radius)
    volume = east * east - west * west
    face_r = nodes[:-1] + dr / 2.0
    face_area = 2.0 * face_r
    return dr, nodes, volume, face_area


def solve_tridiagonal(lower, diagonal, upper, rhs):
    """Thomas algorithm for a real tridiagonal system."""
    n = len(diagonal)
    a = np.asarray(lower, dtype=float).copy()
    b = np.asarray(diagonal, dtype=float).copy()
    c = np.asarray(upper, dtype=float).copy()
    d = np.asarray(rhs, dtype=float).copy()
    for i in range(1, n):
        m = a[i - 1] / b[i - 1]
        b[i] -= m * c[i - 1]
        d[i] -= m * d[i - 1]
    x = np.empty(n, dtype=float)
    x[-1] = d[-1] / b[-1]
    for i in range(n - 2, -1, -1):
        x[i] = (d[i] - c[i] * x[i + 1]) / b[i]
    return x


def _matrix_coeff(state, other, old, dt, radius, material, is_heat, env_value):
    dr, nodes, volume, face_area = geometry(radius)
    if is_heat:
        rho = material.rho(other, state)
        capacity = rho * material.cp(other, state) * volume / dt
        coeff = material.k(other, state)
        boundary_coeff = material.h
    else:
        capacity = volume / dt
        coeff = material.diffusivity(other, state)
        boundary_coeff = material.hm
    face_coeff = 2.0 * coeff[:-1] * coeff[1:] / np.maximum(coeff[:-1] + coeff[1:], 1e-30)
    conductance = face_coeff * face_area / dr
    lower = np.zeros(N - 1)
    upper = np.zeros(N - 1)
    diagonal = capacity.copy()
    rhs = capacity * old
    diagonal[:-1] += conductance
    diagonal[1:] += conductance
    lower[:] = -conductance
    upper[:] = -conductance
    # Half-cell conduction in series with the external Robin resistance.
    surface_k = max(float(coeff[-1]), 1e-15)
    surface_area = 2.0 * radius
    surface_g = surface_area / (dr / (2.0 * surface_k) + 1.0 / boundary_coeff)
    diagonal[-1] += surface_g
    rhs[-1] += surface_g * env_value
    return lower, diagonal, upper, rhs


def advance(T_old, C_old, t_new, dt, radius, material, *, max_iter=80, tol=1e-8, relax=0.7):
    """Advance one fully implicit coupled step and return T, C, iteration count."""
    te, ce = environment(t_new)
    T = T_old.copy()
    C = C_old.copy()
    for iteration in range(1, max_iter + 1):
        tl, td, tu, tr = _matrix_coeff(C, T, T_old, dt, radius, material, True, te)
        cl, cd, cu, cr = _matrix_coeff(T, C, C_old, dt, radius, material, False, ce)
        T_new = solve_tridiagonal(tl, td, tu, tr)
        C_new = solve_tridiagonal(cl, cd, cu, cr)
        T_next = relax * T_new + (1.0 - relax) * T
        C_next = relax * C_new + (1.0 - relax) * C
        err = max(float(np.max(np.abs(T_next - T))), float(np.max(np.abs(C_next - C))))
        T, C = T_next, np.maximum(C_next, 0.0)
        if err < tol:
            return T, C, iteration
    return T, C, max_iter


def integrate(duration, output_step, internal_step, material, *, variable_radius=False, stop_when=None):
    """Integrate from t=0, returning output times, temperatures and moisture."""
    n_out = int(np.floor(duration / output_step + 1e-9)) + 1
    out_times = np.arange(n_out, dtype=float) * output_step
    temps = np.empty((n_out, N), dtype=float)
    moist = np.empty((n_out, N), dtype=float)
    T = np.full(N, 28.0)
    C = np.full(N, 2.55)
    temps[0] = T
    moist[0] = C
    t = 0.0
    out_i = 1
    max_iter_seen = 0
    while out_i < n_out:
        target = out_times[out_i]
        while t < target - 1e-10:
            dt = min(internal_step, target - t)
            t_new = t + dt
            radius = radius_m(t_new) if variable_radius else 0.02
            T, C, iters = advance(T, C, t_new, dt, radius, material)
            max_iter_seen = max(max_iter_seen, iters)
            t = t_new
        temps[out_i] = T
        moist[out_i] = C
        if stop_when is not None and stop_when(C, t):
            out_times = out_times[: out_i + 1]
            temps = temps[: out_i + 1]
            moist = moist[: out_i + 1]
            break
        out_i += 1
    return out_times, temps, moist, max_iter_seen

