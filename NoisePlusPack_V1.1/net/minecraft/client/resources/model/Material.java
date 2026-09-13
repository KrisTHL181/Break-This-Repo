package net.minecraft.client.resources.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class Material {
   public static final Comparator<Material> COMPARATOR = Comparator.comparing(Material::atlasLocation).thenComparing(Material::texture);
   private final Identifier atlasLocation;
   private final Identifier texture;
   private @Nullable RenderType renderType;

   public Material(Identifier p_451389_, Identifier p_459887_) {
      this.atlasLocation = p_451389_;
      this.texture = p_459887_;
   }

   public Identifier atlasLocation() {
      return this.atlasLocation;
   }

   public Identifier texture() {
      return this.texture;
   }

   public RenderType renderType(Function<Identifier, RenderType> p_119202_) {
      if (this.renderType == null) {
         this.renderType = p_119202_.apply(this.atlasLocation);
      }

      return this.renderType;
   }

   public VertexConsumer buffer(MaterialSet p_428142_, MultiBufferSource p_119198_, Function<Identifier, RenderType> p_119199_) {
      return p_428142_.get(this).wrap(p_119198_.getBuffer(this.renderType(p_119199_)));
   }

   public VertexConsumer buffer(
      MaterialSet p_429946_, MultiBufferSource p_119195_, Function<Identifier, RenderType> p_119196_, boolean p_429743_, boolean p_426800_
   ) {
      return p_429946_.get(this).wrap(ItemRenderer.getFoilBuffer(p_119195_, this.renderType(p_119196_), p_429743_, p_426800_));
   }

   @Override
   public boolean equals(Object p_119206_) {
      if (this == p_119206_) {
         return true;
      } else if (p_119206_ != null && this.getClass() == p_119206_.getClass()) {
         Material material = (Material)p_119206_;
         return this.atlasLocation.equals(material.atlasLocation) && this.texture.equals(material.texture);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.atlasLocation, this.texture);
   }

   @Override
   public String toString() {
      return "Material{atlasLocation=" + this.atlasLocation + ", texture=" + this.texture + "}";
   }
}
