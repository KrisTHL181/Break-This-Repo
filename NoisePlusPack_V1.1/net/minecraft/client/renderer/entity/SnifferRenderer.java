package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.sniffer.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnifferRenderer extends AgeableMobRenderer<Sniffer, SnifferRenderState, SnifferModel> {
   private static final Identifier SNIFFER_LOCATION = Identifier.withDefaultNamespace("textures/entity/sniffer/sniffer.png");

   public SnifferRenderer(EntityRendererProvider.Context p_272933_) {
      super(p_272933_, new SnifferModel(p_272933_.bakeLayer(ModelLayers.SNIFFER)), new SnifferModel(p_272933_.bakeLayer(ModelLayers.SNIFFER_BABY)), 1.1F);
   }

   public Identifier getTextureLocation(SnifferRenderState p_455344_) {
      return SNIFFER_LOCATION;
   }

   public SnifferRenderState createRenderState() {
      return new SnifferRenderState();
   }

   public void extractRenderState(Sniffer p_364660_, SnifferRenderState p_364272_, float p_362529_) {
      super.extractRenderState(p_364660_, p_364272_, p_362529_);
      p_364272_.isSearching = p_364660_.isSearching();
      p_364272_.diggingAnimationState.copyFrom(p_364660_.diggingAnimationState);
      p_364272_.sniffingAnimationState.copyFrom(p_364660_.sniffingAnimationState);
      p_364272_.risingAnimationState.copyFrom(p_364660_.risingAnimationState);
      p_364272_.feelingHappyAnimationState.copyFrom(p_364660_.feelingHappyAnimationState);
      p_364272_.scentingAnimationState.copyFrom(p_364660_.scentingAnimationState);
   }

   protected AABB getBoundingBoxForCulling(Sniffer p_367774_) {
      return super.getBoundingBoxForCulling(p_367774_).inflate(0.6F);
   }
}
