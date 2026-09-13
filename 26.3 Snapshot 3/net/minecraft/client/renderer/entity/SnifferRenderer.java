package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.sniffer.SnifferModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.AABB;

public class SnifferRenderer extends AgeableMobRenderer<Sniffer, SnifferRenderState, SnifferModel> {
   private static final Identifier SNIFFER_LOCATION = Identifier.withDefaultNamespace("textures/entity/sniffer/sniffer.png");
   private static final Identifier SNIFFLET_LOCATION = Identifier.withDefaultNamespace("textures/entity/sniffer/snifflet.png");

   public SnifferRenderer(final EntityRendererProvider.Context context) {
      super(context, new SnifferModel(context.bakeLayer(ModelLayers.SNIFFER)), new SnifferModel(context.bakeLayer(ModelLayers.SNIFFER_BABY)), 1.1F);
   }

   public Identifier getTextureLocation(final SnifferRenderState state) {
      return state.isBaby ? SNIFFLET_LOCATION : SNIFFER_LOCATION;
   }

   public SnifferRenderState createRenderState() {
      return new SnifferRenderState();
   }

   public void extractRenderState(final Sniffer entity, final SnifferRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isSearching = entity.isSearching();
      state.diggingAnimationState.copyFrom(entity.diggingAnimationState);
      state.sniffingAnimationState.copyFrom(entity.sniffingAnimationState);
      state.risingAnimationState.copyFrom(entity.risingAnimationState);
      state.feelingHappyAnimationState.copyFrom(entity.feelingHappyAnimationState);
      state.scentingAnimationState.copyFrom(entity.scentingAnimationState);
   }

   protected AABB getBoundingBoxForCulling(final Sniffer entity) {
      return super.getBoundingBoxForCulling(entity).inflate(0.6F);
   }
}
