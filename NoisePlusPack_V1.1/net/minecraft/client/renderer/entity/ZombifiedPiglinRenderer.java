package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.ZombifiedPiglinModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
   private static final Identifier ZOMBIFIED_PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");

   public ZombifiedPiglinRenderer(
      EntityRendererProvider.Context p_366482_,
      ModelLayerLocation p_361904_,
      ModelLayerLocation p_361115_,
      ArmorModelSet<ModelLayerLocation> p_431220_,
      ArmorModelSet<ModelLayerLocation> p_430127_
   ) {
      super(
         p_366482_,
         new ZombifiedPiglinModel(p_366482_.bakeLayer(p_361904_)),
         new ZombifiedPiglinModel(p_366482_.bakeLayer(p_361115_)),
         0.5F,
         PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
      );
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(p_431220_, p_366482_.getModelSet(), ZombifiedPiglinModel::new),
            ArmorModelSet.bake(p_430127_, p_366482_.getModelSet(), ZombifiedPiglinModel::new),
            p_366482_.getEquipmentRenderer()
         )
      );
   }

   public Identifier getTextureLocation(ZombifiedPiglinRenderState p_458284_) {
      return ZOMBIFIED_PIGLIN_LOCATION;
   }

   public ZombifiedPiglinRenderState createRenderState() {
      return new ZombifiedPiglinRenderState();
   }

   public void extractRenderState(ZombifiedPiglin p_453464_, ZombifiedPiglinRenderState p_360783_, float p_367145_) {
      super.extractRenderState(p_453464_, p_360783_, p_367145_);
      p_360783_.isAggressive = p_453464_.isAggressive();
   }
}
