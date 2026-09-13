package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.turtle.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurtleRenderer extends AgeableMobRenderer<Turtle, TurtleRenderState, TurtleModel> {
   private static final Identifier TURTLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/turtle/big_sea_turtle.png");

   public TurtleRenderer(EntityRendererProvider.Context p_174430_) {
      super(p_174430_, new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE)), new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE_BABY)), 0.7F);
   }

   protected float getShadowRadius(TurtleRenderState p_363081_) {
      float f = super.getShadowRadius(p_363081_);
      return p_363081_.isBaby ? f * 0.83F : f;
   }

   public TurtleRenderState createRenderState() {
      return new TurtleRenderState();
   }

   public void extractRenderState(Turtle p_451287_, TurtleRenderState p_365033_, float p_360902_) {
      super.extractRenderState(p_451287_, p_365033_, p_360902_);
      p_365033_.isOnLand = !p_451287_.isInWater() && p_451287_.onGround();
      p_365033_.isLayingEgg = p_451287_.isLayingEgg();
      p_365033_.hasEgg = !p_451287_.isBaby() && p_451287_.hasEgg();
   }

   public Identifier getTextureLocation(TurtleRenderState p_368462_) {
      return TURTLE_LOCATION;
   }
}
