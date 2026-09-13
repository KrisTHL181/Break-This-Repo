package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.state.WorldBorderRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldBorderRenderer {
   public static final Identifier FORCEFIELD_LOCATION = Identifier.withDefaultNamespace("textures/misc/forcefield.png");
   private boolean needsRebuild = true;
   private double lastMinX;
   private double lastMinZ;
   private double lastBorderMinX;
   private double lastBorderMaxX;
   private double lastBorderMinZ;
   private double lastBorderMaxZ;
   private final GpuBuffer worldBorderBuffer = RenderSystem.getDevice()
      .createBuffer(() -> "World border vertex buffer", 40, 16L * DefaultVertexFormat.POSITION_TEX.getVertexSize());
   private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

   private void rebuildWorldBorderBuffer(
      WorldBorderRenderState p_431369_, double p_393396_, double p_397561_, double p_391860_, float p_396982_, float p_396911_, float p_392846_
   ) {
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4 * 4)) {
         double d0 = p_431369_.minX;
         double d1 = p_431369_.maxX;
         double d2 = p_431369_.minZ;
         double d3 = p_431369_.maxZ;
         double d4 = Math.max(Mth.floor(p_397561_ - p_393396_), d2);
         double d5 = Math.min(Mth.ceil(p_397561_ + p_393396_), d3);
         float f = (Mth.floor(d4) & 1) * 0.5F;
         float f1 = (float)(d5 - d4) / 2.0F;
         double d6 = Math.max(Mth.floor(p_391860_ - p_393396_), d0);
         double d7 = Math.min(Mth.ceil(p_391860_ + p_393396_), d1);
         float f2 = (Mth.floor(d6) & 1) * 0.5F;
         float f3 = (float)(d7 - d6) / 2.0F;
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         bufferbuilder.addVertex(0.0F, -p_396982_, (float)(d3 - d4)).setUv(f2, p_396911_);
         bufferbuilder.addVertex((float)(d7 - d6), -p_396982_, (float)(d3 - d4)).setUv(f3 + f2, p_396911_);
         bufferbuilder.addVertex((float)(d7 - d6), p_396982_, (float)(d3 - d4)).setUv(f3 + f2, p_392846_);
         bufferbuilder.addVertex(0.0F, p_396982_, (float)(d3 - d4)).setUv(f2, p_392846_);
         bufferbuilder.addVertex(0.0F, -p_396982_, 0.0F).setUv(f, p_396911_);
         bufferbuilder.addVertex(0.0F, -p_396982_, (float)(d5 - d4)).setUv(f1 + f, p_396911_);
         bufferbuilder.addVertex(0.0F, p_396982_, (float)(d5 - d4)).setUv(f1 + f, p_392846_);
         bufferbuilder.addVertex(0.0F, p_396982_, 0.0F).setUv(f, p_392846_);
         bufferbuilder.addVertex((float)(d7 - d6), -p_396982_, 0.0F).setUv(f2, p_396911_);
         bufferbuilder.addVertex(0.0F, -p_396982_, 0.0F).setUv(f3 + f2, p_396911_);
         bufferbuilder.addVertex(0.0F, p_396982_, 0.0F).setUv(f3 + f2, p_392846_);
         bufferbuilder.addVertex((float)(d7 - d6), p_396982_, 0.0F).setUv(f2, p_392846_);
         bufferbuilder.addVertex((float)(d1 - d6), -p_396982_, (float)(d5 - d4)).setUv(f, p_396911_);
         bufferbuilder.addVertex((float)(d1 - d6), -p_396982_, 0.0F).setUv(f1 + f, p_396911_);
         bufferbuilder.addVertex((float)(d1 - d6), p_396982_, 0.0F).setUv(f1 + f, p_392846_);
         bufferbuilder.addVertex((float)(d1 - d6), p_396982_, (float)(d5 - d4)).setUv(f, p_392846_);

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.worldBorderBuffer.slice(), meshdata.vertexBuffer());
         }

         this.lastBorderMinX = d0;
         this.lastBorderMaxX = d1;
         this.lastBorderMinZ = d2;
         this.lastBorderMaxZ = d3;
         this.lastMinX = d6;
         this.lastMinZ = d4;
         this.needsRebuild = false;
      }
   }

   public void extract(WorldBorder p_426855_, float p_460299_, Vec3 p_426240_, double p_429983_, WorldBorderRenderState p_431572_) {
      p_431572_.minX = p_426855_.getMinX(p_460299_);
      p_431572_.maxX = p_426855_.getMaxX(p_460299_);
      p_431572_.minZ = p_426855_.getMinZ(p_460299_);
      p_431572_.maxZ = p_426855_.getMaxZ(p_460299_);
      if ((
            !(p_426240_.x < p_431572_.maxX - p_429983_)
               || !(p_426240_.x > p_431572_.minX + p_429983_)
               || !(p_426240_.z < p_431572_.maxZ - p_429983_)
               || !(p_426240_.z > p_431572_.minZ + p_429983_)
         )
         && !(p_426240_.x < p_431572_.minX - p_429983_)
         && !(p_426240_.x > p_431572_.maxX + p_429983_)
         && !(p_426240_.z < p_431572_.minZ - p_429983_)
         && !(p_426240_.z > p_431572_.maxZ + p_429983_)) {
         p_431572_.alpha = 1.0 - p_426855_.getDistanceToBorder(p_426240_.x, p_426240_.z) / p_429983_;
         p_431572_.alpha = Math.pow(p_431572_.alpha, 4.0);
         p_431572_.alpha = Mth.clamp(p_431572_.alpha, 0.0, 1.0);
         p_431572_.tint = p_426855_.getStatus().getColor();
      } else {
         p_431572_.alpha = 0.0;
      }
   }

   public void render(WorldBorderRenderState p_422499_, Vec3 p_368400_, double p_360813_, double p_369225_) {
   }

   public void invalidate() {
      this.needsRebuild = true;
   }

   private boolean shouldRebuildWorldBorderBuffer(WorldBorderRenderState p_424164_) {
      return this.needsRebuild
         || p_424164_.minX != this.lastBorderMinX
         || p_424164_.minZ != this.lastBorderMinZ
         || p_424164_.maxX != this.lastBorderMaxX
         || p_424164_.maxZ != this.lastBorderMaxZ;
   }
}
