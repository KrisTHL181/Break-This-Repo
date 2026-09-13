package net.minecraft.client.gui.components.debug;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntrySystemSpecs implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("system");

   @Override
   public void display(DebugScreenDisplayer p_423519_, @Nullable Level p_430413_, @Nullable LevelChunk p_427711_, @Nullable LevelChunk p_426200_) {
      GpuDevice gpudevice = RenderSystem.getDevice();
      p_423519_.addToGroup(
         GROUP,
         List.of(
            String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")),
            String.format(Locale.ROOT, "CPU: %s", GLX._getCpuInfo()),
            String.format(
               Locale.ROOT,
               "Display: %dx%d (%s)",
               Minecraft.getInstance().getWindow().getWidth(),
               Minecraft.getInstance().getWindow().getHeight(),
               gpudevice.getVendor()
            ),
            gpudevice.getRenderer(),
            String.format(Locale.ROOT, "%s %s", gpudevice.getBackendName(), gpudevice.getVersion())
         )
      );
   }

   @Override
   public boolean isAllowed(boolean p_423991_) {
      return true;
   }
}
