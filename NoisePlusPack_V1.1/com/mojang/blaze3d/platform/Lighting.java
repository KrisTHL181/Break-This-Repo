package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public class Lighting implements AutoCloseable {
   private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
   private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
   private static final Vector3f NETHER_DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
   private static final Vector3f NETHER_DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();
   private static final Vector3f INVENTORY_DIFFUSE_LIGHT_0 = new Vector3f(0.2F, -1.0F, 1.0F).normalize();
   private static final Vector3f INVENTORY_DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();
   public static final int UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
   private final GpuBuffer buffer;
   private final long paddedSize;

   public Lighting() {
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.paddedSize = Mth.roundToward(UBO_SIZE, gpudevice.getUniformOffsetAlignment());
      this.buffer = gpudevice.createBuffer(() -> "Lighting UBO", 136, this.paddedSize * Lighting.Entry.values().length);
      Matrix4f matrix4f = new Matrix4f().rotationY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
      this.updateBuffer(
         Lighting.Entry.ITEMS_FLAT, matrix4f.transformDirection(DIFFUSE_LIGHT_0, new Vector3f()), matrix4f.transformDirection(DIFFUSE_LIGHT_1, new Vector3f())
      );
      Matrix4f matrix4f1 = new Matrix4f()
         .scaling(1.0F, -1.0F, 1.0F)
         .rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
         .rotateYXZ((float) (-Math.PI / 8), (float) (Math.PI * 3.0 / 4.0), 0.0F);
      this.updateBuffer(
         Lighting.Entry.ITEMS_3D, matrix4f1.transformDirection(DIFFUSE_LIGHT_0, new Vector3f()), matrix4f1.transformDirection(DIFFUSE_LIGHT_1, new Vector3f())
      );
      this.updateBuffer(Lighting.Entry.ENTITY_IN_UI, INVENTORY_DIFFUSE_LIGHT_0, INVENTORY_DIFFUSE_LIGHT_1);
      Matrix4f matrix4f2 = new Matrix4f();
      this.updateBuffer(
         Lighting.Entry.PLAYER_SKIN,
         matrix4f2.transformDirection(INVENTORY_DIFFUSE_LIGHT_0, new Vector3f()),
         matrix4f2.transformDirection(INVENTORY_DIFFUSE_LIGHT_1, new Vector3f())
      );
   }

   public void updateLevel(DimensionType.CardinalLightType p_452972_) {
      switch (p_452972_) {
         case DEFAULT:
            this.updateBuffer(Lighting.Entry.LEVEL, DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
            break;
         case NETHER:
            this.updateBuffer(Lighting.Entry.LEVEL, NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
      }
   }

   private void updateBuffer(Lighting.Entry p_408846_, Vector3f p_409019_, Vector3f p_408652_) {
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         ByteBuffer bytebuffer = Std140Builder.onStack(memorystack, UBO_SIZE).putVec3(p_409019_).putVec3(p_408652_).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(p_408846_.ordinal() * this.paddedSize, this.paddedSize), bytebuffer);
      } catch (Throwable var8) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (memorystack != null) {
         memorystack.close();
      }
   }

   public void setupFor(Lighting.Entry p_406507_) {
      RenderSystem.setShaderLights(this.buffer.slice(p_406507_.ordinal() * this.paddedSize, UBO_SIZE));
   }

   @Override
   public void close() {
      this.buffer.close();
   }

   @OnlyIn(Dist.CLIENT)
   public enum Entry {
      LEVEL,
      ITEMS_FLAT,
      ITEMS_3D,
      ENTITY_IN_UI,
      PLAYER_SKIN;
   }
}
