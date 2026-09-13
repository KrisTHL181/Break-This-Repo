package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.ghast.GhastModel;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ghast;

public class GhastRenderer extends MobRenderer<Ghast, GhastRenderState, GhastModel> {
   private static final Identifier GHAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/ghast.png");
   private static final Identifier GHAST_SHOOTING_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/ghast_shooting.png");

   public GhastRenderer(final EntityRendererProvider.Context context) {
      super(context, new GhastModel(context.bakeLayer(ModelLayers.GHAST)), 1.5F);
   }

   public Identifier getTextureLocation(final GhastRenderState state) {
      return state.isCharging ? GHAST_SHOOTING_LOCATION : GHAST_LOCATION;
   }

   public GhastRenderState createRenderState() {
      return new GhastRenderState();
   }

   public void extractRenderState(final Ghast entity, final GhastRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isCharging = entity.isCharging();
   }
}
