package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

public class Lighting implements AutoCloseable {
   private static final Vector3fc DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
   private static final Vector3fc DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();
   private static final Vector3fc NETHER_DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
   private static final Vector3fc NETHER_DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.7F).normalize();
   private static final Vector3fc INVENTORY_DIFFUSE_LIGHT_0 = new Vector3f(0.2F, -1.0F, 1.0F).normalize();
   private static final Vector3fc INVENTORY_DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, -1.0F, 0.0F).normalize();
   public static final int UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
   private final GpuBuffer buffer;
   private final long paddedSize;

   public Lighting() {
      GpuDevice device = RenderSystem.getDevice();
      this.paddedSize = Mth.roundToward(UBO_SIZE, device.getDeviceInfo().limits().minUniformOffsetAlignment());
      this.buffer = device.createBuffer(() -> "Lighting UBO", 136, this.paddedSize * Lighting.Entry.values().length);
      Matrix4f flatPose = new Matrix4f().rotationY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
      this.updateBuffer(
         Lighting.Entry.ITEMS_FLAT, flatPose.transformDirection(DIFFUSE_LIGHT_0, new Vector3f()), flatPose.transformDirection(DIFFUSE_LIGHT_1, new Vector3f())
      );
      Matrix4f item3DPose = new Matrix4f()
         .scaling(1.0F, -1.0F, 1.0F)
         .rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
         .rotateYXZ((float) (-Math.PI / 8), (float) (Math.PI * 3.0 / 4.0), 0.0F);
      this.updateBuffer(
         Lighting.Entry.ITEMS_3D,
         item3DPose.transformDirection(DIFFUSE_LIGHT_0, new Vector3f()),
         item3DPose.transformDirection(DIFFUSE_LIGHT_1, new Vector3f())
      );
      this.updateBuffer(Lighting.Entry.ENTITY_IN_UI, INVENTORY_DIFFUSE_LIGHT_0, INVENTORY_DIFFUSE_LIGHT_1);
      Matrix4f playerSkinPose = new Matrix4f();
      this.updateBuffer(
         Lighting.Entry.PLAYER_SKIN,
         playerSkinPose.transformDirection(INVENTORY_DIFFUSE_LIGHT_0, new Vector3f()),
         playerSkinPose.transformDirection(INVENTORY_DIFFUSE_LIGHT_1, new Vector3f())
      );
   }

   public void updateLevel(final CardinalLighting.Type type) {
      switch (type) {
         case DEFAULT:
            this.updateBuffer(Lighting.Entry.LEVEL, DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
            break;
         case NETHER:
            this.updateBuffer(Lighting.Entry.LEVEL, NETHER_DIFFUSE_LIGHT_0, NETHER_DIFFUSE_LIGHT_1);
      }
   }

   private void updateBuffer(final Lighting.Entry entry, final Vector3fc light0, final Vector3fc light1) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = Std140Builder.onStack(stack, UBO_SIZE).putVec3(light0).putVec3(light1).get();
         RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(entry.ordinal() * this.paddedSize, this.paddedSize), byteBuffer);
      } catch (Throwable var8) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public void setupFor(final Lighting.Entry entry) {
      RenderSystem.setShaderLights(this.buffer.slice(entry.ordinal() * this.paddedSize, UBO_SIZE));
   }

   @Override
   public void close() {
      this.buffer.close();
   }

   public enum Entry {
      LEVEL,
      ITEMS_FLAT,
      ITEMS_3D,
      ENTITY_IN_UI,
      PLAYER_SKIN;
   }
}
