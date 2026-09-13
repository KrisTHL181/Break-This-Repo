package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishRenderer extends MobRenderer<TropicalFish, TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
   private final EntityModel<TropicalFishRenderState> smallModel = this.getModel();
   private final EntityModel<TropicalFishRenderState> largeModel;
   private static final Identifier SMALL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a.png");
   private static final Identifier LARGE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b.png");

   public TropicalFishRenderer(EntityRendererProvider.Context p_174428_) {
      super(p_174428_, new TropicalFishSmallModel(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.largeModel = new TropicalFishLargeModel(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, p_174428_.getModelSet()));
   }

   public Identifier getTextureLocation(TropicalFishRenderState p_455466_) {
      return switch (p_455466_.pattern.base()) {
         case SMALL -> SMALL_TEXTURE;
         case LARGE -> LARGE_TEXTURE;
      };
   }

   public TropicalFishRenderState createRenderState() {
      return new TropicalFishRenderState();
   }

   public void extractRenderState(TropicalFish p_457082_, TropicalFishRenderState p_361016_, float p_366837_) {
      super.extractRenderState(p_457082_, p_361016_, p_366837_);
      p_361016_.pattern = p_457082_.getPattern();
      p_361016_.baseColor = p_457082_.getBaseColor().getTextureDiffuseColor();
      p_361016_.patternColor = p_457082_.getPatternColor().getTextureDiffuseColor();
   }

   public void submit(TropicalFishRenderState p_426672_, PoseStack p_429234_, SubmitNodeCollector p_427557_, CameraRenderState p_430196_) {
      this.model = switch (p_426672_.pattern.base()) {
         case SMALL -> this.smallModel;
         case LARGE -> this.largeModel;
      };
      super.submit(p_426672_, p_429234_, p_427557_, p_430196_);
   }

   protected int getModelTint(TropicalFishRenderState p_363762_) {
      return p_363762_.baseColor;
   }

   protected void setupRotations(TropicalFishRenderState p_364918_, PoseStack p_116205_, float p_116206_, float p_116207_) {
      super.setupRotations(p_364918_, p_116205_, p_116206_, p_116207_);
      float f = 4.3F * Mth.sin(0.6F * p_364918_.ageInTicks);
      p_116205_.mulPose(Axis.YP.rotationDegrees(f));
      if (!p_364918_.isInWater) {
         p_116205_.translate(0.2F, 0.1F, 0.0F);
         p_116205_.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }
   }
}
