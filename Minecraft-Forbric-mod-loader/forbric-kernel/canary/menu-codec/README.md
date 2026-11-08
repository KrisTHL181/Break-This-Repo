# Menu codec gate

This probe opens Farmer's Delight's cooking pot on a dedicated server running the staged Minecraft 26.2 merged game with both loader carriers, using the released Fabric build `FarmersDelight-26.2-3.6.26+refabricated.jar` and Fabric API `0.155.2+26.2`. A player without a client sends the cooking pot the same `ServerboundUseItemOnPacket` a right click does, and the server handles it as it handles a real one.

From the repository root, after `python3 tools/dev.py prepare` and with the kernel built (`./gradlew jar` in `forbric-kernel`):

```sh
python3 forbric-kernel/run/compat/menu-codec-gate.py \
  --farmers-delight /path/to/FarmersDelight-26.2-3.6.26+refabricated.jar
```

The staged game, Minecraft libraries and Fabric API default to what `tools/dev.py prepare` wrote under `forbric-kernel/.dev/`, and otherwise to `FORBRIC_OLD`/`MC_DIR` and the compatibility pack's `run/client-merged-pack/mods/fabric-api-0.155.2+26.2.jar`; `--staged-root` and `--fabric-api` select them explicitly. The gate runs the probe with `forbric.wrapperEntryEvents=off`, then with the repair enabled under **strict** compatibility policy. The negative control has to reproduce issue #4's failure: the menu type is an `ExtendedMenuType`, fabric-menu-api recorded no codec for it, and opening the pot throws `Codec for farmersdelight:cooking_pot is not registered!`. The fixed run has to pass all three checks: the menu type is extended, its codec is recorded, and the pot's menu opens with fabric-menu-api's `open_screen` payload sent to the player. Its compatibility report has to contain no confirmed loss for Farmer's Delight or fabric-menu-api. Both servers have to save and stop normally. Logs, worlds, the probe jar and input hashes are kept under the printed output directory.

The bytecode regression suite is `WrapperEntryAddedInjectorTest`. It reads the staged game and carrier JARs and the compatibility pack's Fabric API. Missing fixtures are reported as skipped tests.
