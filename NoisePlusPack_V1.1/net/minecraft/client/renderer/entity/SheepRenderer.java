package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepRenderer extends AgeableMobRenderer<Sheep, SheepRenderState, SheepModel> {
   private static final Identifier SHEEP_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep.png");

   public SheepRenderer(EntityRendererProvider.Context p_174366_) {
      super(p_174366_, new SheepModel(p_174366_.bakeLayer(ModelLayers.SHEEP)), new SheepModel(p_174366_.bakeLayer(ModelLayers.SHEEP_BABY)), 0.7F);
      this.addLayer(new SheepWoolUndercoatLayer(this, p_174366_.getModelSet()));
      this.addLayer(new SheepWoolLayer(this, p_174366_.getModelSet()));
   }

   public Identifier getTextureLocation(SheepRenderState p_364199_) {
      return SHEEP_LOCATION;
   }

   public SheepRenderState createRenderState() {
      return new SheepRenderState();
   }

   public void extractRenderState(Sheep p_392007_, SheepRenderState p_365680_, float p_363826_) {
      super.extractRenderState(p_392007_, p_365680_, p_363826_);
      p_365680_.headEatAngleScale = p_392007_.getHeadEatAngleScale(p_363826_);
      p_365680_.headEatPositionScale = p_392007_.getHeadEatPositionScale(p_363826_);
      p_365680_.isSheared = p_392007_.isSheared();
      p_365680_.woolColor = p_392007_.getColor();
      p_365680_.isJebSheep = checkMagicName(p_392007_, "jeb_");
   }
}
