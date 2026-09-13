package net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.Arrow;

public class TippableArrowRenderer extends ArrowRenderer<Arrow, TippableArrowRenderState> {
   public static final Identifier NORMAL_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
   public static final Identifier TIPPED_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow_tipped.png");

   public TippableArrowRenderer(final EntityRendererProvider.Context context) {
      super(context);
   }

   protected Identifier getTextureLocation(final TippableArrowRenderState state) {
      return state.isTipped ? TIPPED_ARROW_LOCATION : NORMAL_ARROW_LOCATION;
   }

   public TippableArrowRenderState createRenderState() {
      return new TippableArrowRenderState();
   }

   public void extractRenderState(final Arrow entity, final TippableArrowRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isTipped = entity.getColor() > 0;
   }
}
