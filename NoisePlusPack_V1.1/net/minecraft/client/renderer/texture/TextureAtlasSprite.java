package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
public class TextureAtlasSprite implements AutoCloseable {
   private final Identifier atlasLocation;
   private final SpriteContents contents;
   private final int x;
   private final int y;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final int padding;

   protected TextureAtlasSprite(Identifier p_460544_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_, int p_452036_) {
      this.atlasLocation = p_460544_;
      this.contents = p_248526_;
      this.padding = p_452036_;
      this.x = p_248672_;
      this.y = p_248637_;
      this.u0 = (float)(p_248672_ + p_452036_) / p_248950_;
      this.u1 = (float)(p_248672_ + p_452036_ + p_248526_.width()) / p_248950_;
      this.v0 = (float)(p_248637_ + p_452036_) / p_249741_;
      this.v1 = (float)(p_248637_ + p_452036_ + p_248526_.height()) / p_249741_;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public float getU0() {
      return this.u0;
   }

   public float getU1() {
      return this.u1;
   }

   public SpriteContents contents() {
      return this.contents;
   }

   public SpriteContents.@Nullable AnimationState createAnimationState(GpuBufferSlice p_455189_, int p_458866_) {
      return this.contents.createAnimationState(p_455189_, p_458866_);
   }

   public float getU(float p_298825_) {
      float f = this.u1 - this.u0;
      return this.u0 + f * p_298825_;
   }

   public float getV0() {
      return this.v0;
   }

   public float getV1() {
      return this.v1;
   }

   public float getV(float p_299087_) {
      float f = this.v1 - this.v0;
      return this.v0 + f * p_299087_;
   }

   public Identifier atlasLocation() {
      return this.atlasLocation;
   }

   @Override
   public String toString() {
      return "TextureAtlasSprite{contents='" + this.contents + "', u0=" + this.u0 + ", u1=" + this.u1 + ", v0=" + this.v0 + ", v1=" + this.v1 + "}";
   }

   public void uploadFirstFrame(GpuTexture p_397186_, int p_460430_) {
      this.contents.uploadFirstFrame(p_397186_, p_460430_);
   }

   public VertexConsumer wrap(VertexConsumer p_118382_) {
      return new SpriteCoordinateExpander(p_118382_, this);
   }

   boolean isAnimated() {
      return this.contents.isAnimated();
   }

   public void uploadSpriteUbo(ByteBuffer p_450246_, int p_456272_, int p_452106_, int p_453297_, int p_452181_, int p_457141_) {
      for (int i = 0; i <= p_452106_; i++) {
         Std140Builder.intoBuffer(MemoryUtil.memSlice(p_450246_, p_456272_ + i * p_457141_, p_457141_))
            .putMat4f(new Matrix4f().ortho2D(0.0F, p_453297_ >> i, 0.0F, p_452181_ >> i))
            .putMat4f(
               new Matrix4f()
                  .translate(this.x >> i, this.y >> i, 0.0F)
                  .scale(this.contents.width() + this.padding * 2 >> i, this.contents.height() + this.padding * 2 >> i, 1.0F)
            )
            .putFloat((float)this.padding / this.contents.width())
            .putFloat((float)this.padding / this.contents.height())
            .putInt(i);
      }
   }

   @Override
   public void close() {
      this.contents.close();
   }
}
