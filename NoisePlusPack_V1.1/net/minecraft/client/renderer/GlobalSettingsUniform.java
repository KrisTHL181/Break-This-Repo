package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class GlobalSettingsUniform implements AutoCloseable {
   public static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putVec3().putVec2().putFloat().putFloat().putInt().putInt().get();
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Global Settings UBO", 136, UBO_SIZE);

   public void update(
      int p_406236_, int p_410639_, double p_409435_, long p_409358_, DeltaTracker p_405996_, int p_410662_, Camera p_456490_, boolean p_455075_
   ) {
      Vec3 vec3 = p_456490_.position();
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         int i = Mth.floor(vec3.x);
         int j = Mth.floor(vec3.y);
         int k = Mth.floor(vec3.z);
         ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, UBO_SIZE)
            .putIVec3(i, j, k)
            .putVec3((float)(i - vec3.x), (float)(j - vec3.y), (float)(k - vec3.z))
            .putVec2(p_406236_, p_410639_)
            .putFloat((float)p_409435_)
            .putFloat(((float)(p_409358_ % 24000L) + p_405996_.getGameTimeDeltaPartialTick(false)) / 24000.0F)
            .putInt(p_410662_)
            .putInt(p_455075_ ? 1 : 0)
            .get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytebuffer);
      } catch (Throwable var18) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memorystack != null) {
         memorystack.close();
      }

      RenderSystem.setGlobalSettingsUniform(this.buffer);
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
