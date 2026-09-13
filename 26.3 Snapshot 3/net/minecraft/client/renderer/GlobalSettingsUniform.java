package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

public class GlobalSettingsUniform implements AutoCloseable {
   public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

   public void update(
      final int width,
      final int height,
      final double glintAlpha,
      final long gameTime,
      final DeltaTracker deltaTracker,
      final int menuBlurRadius,
      final Vec3 cameraPos,
      final boolean useRgss
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         int cameraX = Mth.floor(cameraPos.x);
         int cameraY = Mth.floor(cameraPos.y);
         int cameraZ = Mth.floor(cameraPos.z);
         ByteBuffer data = Std140Builder.onStack(stack, UBO_SIZE)
            .putIVec3(cameraX, cameraY, cameraZ)
            .putVec3((float)(cameraX - cameraPos.x), (float)(cameraY - cameraPos.y), (float)(cameraZ - cameraPos.z))
            .putVec2(width, height)
            .putFloat((float)glintAlpha)
            .putFloat(((float)(gameTime % 24000L) + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24000.0F)
            .putInt(menuBlurRadius)
            .putInt(useRgss ? 1 : 0)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), data);
      } catch (Throwable var17) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (stack != null) {
         stack.close();
      }

      RenderSystem.setGlobalSettingsUniform(this.buffer);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
