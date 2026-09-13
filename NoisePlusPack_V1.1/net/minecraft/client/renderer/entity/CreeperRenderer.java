package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreeperRenderer extends MobRenderer<Creeper, CreeperRenderState, CreeperModel> {
   private static final Identifier CREEPER_LOCATION = Identifier.withDefaultNamespace("textures/entity/creeper/creeper.png");

   public CreeperRenderer(EntityRendererProvider.Context p_173958_) {
      super(p_173958_, new CreeperModel(p_173958_.bakeLayer(ModelLayers.CREEPER)), 0.5F);
      this.addLayer(new CreeperPowerLayer(this, p_173958_.getModelSet()));
   }

   protected void scale(CreeperRenderState p_366533_, PoseStack p_114047_) {
      float f = p_366533_.swelling;
      float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
      f = Mth.clamp(f, 0.0F, 1.0F);
      f *= f;
      f *= f;
      float f2 = (1.0F + f * 0.4F) * f1;
      float f3 = (1.0F + f * 0.1F) / f1;
      p_114047_.scale(f2, f3, f2);
   }

   protected float getWhiteOverlayProgress(CreeperRenderState p_364355_) {
      float f = p_364355_.swelling;
      return (int)(f * 10.0F) % 2 == 0 ? 0.0F : Mth.clamp(f, 0.5F, 1.0F);
   }

   public Identifier getTextureLocation(CreeperRenderState p_368131_) {
      return CREEPER_LOCATION;
   }

   public CreeperRenderState createRenderState() {
      return new CreeperRenderState();
   }

   public void extractRenderState(Creeper p_366316_, CreeperRenderState p_369319_, float p_368319_) {
      super.extractRenderState(p_366316_, p_369319_, p_368319_);
      p_369319_.swelling = p_366316_.getSwelling(p_368319_);
      p_369319_.isPowered = p_366316_.isPowered();
   }
}
