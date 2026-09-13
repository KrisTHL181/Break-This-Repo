package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.goat.GoatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoatRenderer extends AgeableMobRenderer<Goat, GoatRenderState, GoatModel> {
   private static final Identifier GOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/goat/goat.png");

   public GoatRenderer(EntityRendererProvider.Context p_174153_) {
      super(p_174153_, new GoatModel(p_174153_.bakeLayer(ModelLayers.GOAT)), new GoatModel(p_174153_.bakeLayer(ModelLayers.GOAT_BABY)), 0.7F);
   }

   public Identifier getTextureLocation(GoatRenderState p_452751_) {
      return GOAT_LOCATION;
   }

   public GoatRenderState createRenderState() {
      return new GoatRenderState();
   }

   public void extractRenderState(Goat p_364753_, GoatRenderState p_368056_, float p_364169_) {
      super.extractRenderState(p_364753_, p_368056_, p_364169_);
      p_368056_.hasLeftHorn = p_364753_.hasLeftHorn();
      p_368056_.hasRightHorn = p_364753_.hasRightHorn();
      p_368056_.rammingXHeadRot = p_364753_.getRammingXHeadRot();
   }
}
