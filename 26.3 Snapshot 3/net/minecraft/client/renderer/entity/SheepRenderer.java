package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.sheep.BabySheepModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sheep.Sheep;

public class SheepRenderer extends AgeableMobRenderer<Sheep, SheepRenderState, SheepModel> {
   private static final Identifier SHEEP_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep.png");
   private static final Identifier SHEEP_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_baby.png");

   public SheepRenderer(final EntityRendererProvider.Context context) {
      super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), new BabySheepModel(context.bakeLayer(ModelLayers.SHEEP_BABY)), 0.7F);
      this.addLayer(new SheepWoolUndercoatLayer(this, context.getModelSet()));
      this.addLayer(new SheepWoolLayer(this, context.getModelSet()));
   }

   public Identifier getTextureLocation(final SheepRenderState state) {
      return state.isBaby ? SHEEP_BABY_LOCATION : SHEEP_LOCATION;
   }

   public SheepRenderState createRenderState() {
      return new SheepRenderState();
   }

   public void extractRenderState(final Sheep entity, final SheepRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.headEatAngleScale = entity.getHeadEatAngleScale(partialTicks);
      state.headEatPositionScale = entity.getHeadEatPositionScale(partialTicks);
      state.isSheared = entity.isSheared();
      state.woolColor = entity.getColor();
      state.isJebSheep = checkMagicName(entity, "jeb_");
   }
}
