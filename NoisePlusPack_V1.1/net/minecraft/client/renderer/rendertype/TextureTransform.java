package net.minecraft.client.renderer.rendertype;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class TextureTransform {
   public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0;
   private final String name;
   private final Supplier<Matrix4f> supplier;
   public static final TextureTransform DEFAULT_TEXTURING = new TextureTransform("default_texturing", Matrix4f::new);
   public static final TextureTransform GLINT_TEXTURING = new TextureTransform("glint_texturing", () -> setupGlintTexturing(8.0F));
   public static final TextureTransform ENTITY_GLINT_TEXTURING = new TextureTransform("entity_glint_texturing", () -> setupGlintTexturing(0.5F));
   public static final TextureTransform ARMOR_ENTITY_GLINT_TEXTURING = new TextureTransform("armor_entity_glint_texturing", () -> setupGlintTexturing(0.16F));

   public TextureTransform(String p_451692_, Supplier<Matrix4f> p_460424_) {
      this.name = p_451692_;
      this.supplier = p_460424_;
   }

   public Matrix4f getMatrix() {
      return this.supplier.get();
   }

   @Override
   public String toString() {
      return "TexturingStateShard[" + this.name + "]";
   }

   private static Matrix4f setupGlintTexturing(float p_454799_) {
      long i = (long)(Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
      float f = (float)(i % 110000L) / 110000.0F;
      float f1 = (float)(i % 30000L) / 30000.0F;
      Matrix4f matrix4f = new Matrix4f().translation(-f, f1, 0.0F);
      matrix4f.rotateZ((float) (Math.PI / 18)).scale(p_454799_);
      return matrix4f;
   }

   @OnlyIn(Dist.CLIENT)
   public static final class OffsetTextureTransform extends TextureTransform {
      public OffsetTextureTransform(float p_454869_, float p_455303_) {
         super("offset_texturing", () -> new Matrix4f().translation(p_454869_, p_455303_, 0.0F));
      }
   }
}
