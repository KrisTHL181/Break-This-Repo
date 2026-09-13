package dev.miru.gui.debug;

import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class TerrainArgumentViewer implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("terrain_arguments");
   private static final String splitter = "=====================";

   @Override
   public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk1) {
      TppSettings tppSettings = ModMain.getOptions();
      Minecraft minecraft = Minecraft.getInstance();
      double[] scaler = tppSettings.getScalerAsArray();
      double[] offset = tppSettings.getOffsetAsArray();
      debugScreenDisplayer.addToGroup(
         GROUP,
         List.of(
            ModMain.getI18N("debugger.title"),
            "=====================",
            ModMain.getI18N("debugger.subtitle.options"),
            "=====================",
            ModMain.getI18N("debugger.options.noise_modify_mode", tppSettings.getWgen_modifyModeValue()),
            ModMain.getI18N("debugger.options.scaler", scaler[0], scaler[1], scaler[2]),
            ModMain.getI18N("debugger.options.offset", offset[0], offset[1], offset[2]),
            ModMain.getI18N("debugger.options.limit_noise_division.title"),
            ModMain.getI18N("debugger.options.limit_noise_division.max", tppSettings.getWgen_maxLimitNoiseCounterDivisionValue()),
            ModMain.getI18N("debugger.options.limit_noise_division.min", tppSettings.getWgen_minLimitNoiseCounterDivisionValue()),
            ModMain.getI18N("debugger.options.amplitude", tppSettings.getWgen_amplitudeValue()),
            ModMain.getI18N("debugger.options.frequency", tppSettings.getWgen_frequencyValue()),
            ModMain.getI18N("debugger.options.wrapper.mode", tppSettings.getWgen_wrapModeValue()),
            ModMain.getI18N("debugger.options.wrapper.division", tppSettings.getWgen_wrapDivisionValue()),
            ModMain.getI18N("debugger.options.lerp.mode", tppSettings.getWgen_lerpModeValue()),
            ModMain.getI18N("debugger.options.lerp.rate", tppSettings.getWgen_lerpRateValue()),
            "=====================",
            ModMain.getI18N("debugger.subtitle.pos"),
            "=====================",
            minecraft.player != null
               ? ModMain.getI18N("debugger.pos_view.player", minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())
               : ModMain.getI18N("debugger.pos_view.player.load_level"),
            minecraft.player != null
               ? ModMain.getI18N(
                  "debugger.pos_view.terrain",
                  tppSettings.getWgen_modifyModeValue()
                     .applyFunction
                     .apply((double)minecraft.player.getBlockX(), tppSettings.getScalerX(), tppSettings.getOffsetX()),
                  tppSettings.getWgen_modifyModeValue()
                     .applyFunction
                     .apply((double)minecraft.player.getBlockY(), tppSettings.getScalerY(), tppSettings.getOffsetY()),
                  tppSettings.getWgen_modifyModeValue()
                     .applyFunction
                     .apply((double)minecraft.player.getBlockZ(), tppSettings.getScalerZ(), tppSettings.getOffsetZ())
               )
               : ModMain.getI18N("debugger.pos_view.terrain.load_level")
         )
      );
   }
}
