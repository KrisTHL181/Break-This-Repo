package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeRenderer extends MobRenderer<Slime, SlimeRenderState, SlimeModel> {
   public static final Identifier SLIME_LOCATION = Identifier.withDefaultNamespace("textures/entity/slime/slime.png");

   public SlimeRenderer(EntityRendererProvider.Context p_174391_) {
      super(p_174391_, new SlimeModel(p_174391_.bakeLayer(ModelLayers.SLIME)), 0.25F);
      this.addLayer(new SlimeOuterLayer(this, p_174391_.getModelSet()));
   }

   protected float getShadowRadius(SlimeRenderState p_377129_) {
      return p_377129_.size * 0.25F;
   }

   protected void scale(SlimeRenderState p_370205_, PoseStack p_115964_) {
      float f = 0.999F;
      p_115964_.scale(0.999F, 0.999F, 0.999F);
      p_115964_.translate(0.0F, 0.001F, 0.0F);
      float f1 = p_370205_.size;
      float f2 = p_370205_.squish / (f1 * 0.5F + 1.0F);
      float f3 = 1.0F / (f2 + 1.0F);
      p_115964_.scale(f3 * f1, 1.0F / f3 * f1, f3 * f1);
   }

   public Identifier getTextureLocation(SlimeRenderState p_368833_) {
      return SLIME_LOCATION;
   }

   public SlimeRenderState createRenderState() {
      return new SlimeRenderState();
   }

   public void extractRenderState(Slime p_366571_, SlimeRenderState p_363359_, float p_369770_) {
      super.extractRenderState(p_366571_, p_363359_, p_369770_);
      p_363359_.squish = Mth.lerp(p_369770_, p_366571_.oSquish, p_366571_.squish);
      p_363359_.size = p_366571_.getSize();
   }
}
