# Carpet behavior gate

This probe tests the released `fabric-carpet-26.2+v260616.jar` against the staged Minecraft 26.2 merged game and both loader carriers. It uses disposable dedicated-server worlds, Carpet's real fake-player class, real Scarpet event functions, and NeoForge event listeners.

From the repository root, with the shared game artifacts already staged:

```sh
python3 forbric-kernel/run/compat/carpet-gate.py \
  --carpet /path/to/fabric-carpet-26.2+v260616.jar \
  --staged-root /path/to/forbric-loader/run
```

The gate compiles this probe, runs the same 22 behavior checks with `forbric.carpetMixins=off`, then runs them with the repair enabled under **strict** compatibility policy. It requires the negative control to fail the repaired behaviors, the fixed run to pass every check, both servers to save and stop normally, and the fixed compatibility report to contain no confirmed Carpet losses. Logs, test worlds, reports and input hashes are kept under the printed output directory. `--output` can select a new directory explicitly.

Coverage:

- `fillUpdates` on/off: redstone lamp notifications and support-dependent block shape updates.
- `renewableBlackstone` and `renewableDeepslate` on/off, both placement and neighbor updates. Source lava remains obsidian, reactions above zero remain cobblestone, basalt retains precedence, and lava without water does not become deepslate.
- Real `player_swaps_hands` and `player_breaks_block` Scarpet callbacks, including cancellation. Canceled breaks preserve blocks and tool durability; successful events fire once and receive the original block state. Creative and survival modes are both exercised.
- Native hand-swap and block-break vetoes still prevent the action and its Scarpet callback.

The fake player is instantiated locally without a profile/skin lookup. This tests the affected event paths; it does not certify every Carpet rule, Scarpet API, or third-party extension.

The bytecode regression suite is `CarpetMixinAdapterTest`. Its Carpet fixture goes at `forbric-kernel/build/compat-inputs/carpet/mods/fabric-carpet-26.2+v260616.jar`. The suite also reads the staged carrier/game JARs and the local vanilla Minecraft 26.2 JAR. Missing fixtures are reported as skipped tests rather than replaced by mock injector bodies.
