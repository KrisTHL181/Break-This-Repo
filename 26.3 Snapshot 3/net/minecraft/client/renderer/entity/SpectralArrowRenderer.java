package net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;

public class SpectralArrowRenderer extends ArrowRenderer<SpectralArrow, ArrowRenderState> {
   public static final Identifier SPECTRAL_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow_spectral.png");

   public SpectralArrowRenderer(final EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   protected Identifier getTextureLocation(final ArrowRenderState state) {
      return SPECTRAL_ARROW_LOCATION;
   }

   public ArrowRenderState createRenderState() {
      return new ArrowRenderState();
   }
}
