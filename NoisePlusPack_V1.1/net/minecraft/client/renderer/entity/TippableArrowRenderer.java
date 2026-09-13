package net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TippableArrowRenderer extends ArrowRenderer<Arrow, TippableArrowRenderState> {
   public static final Identifier NORMAL_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
   public static final Identifier TIPPED_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/tipped_arrow.png");

   public TippableArrowRenderer(EntityRendererProvider.Context p_174422_) {
      super(p_174422_);
   }

   protected Identifier getTextureLocation(TippableArrowRenderState p_455982_) {
      return p_455982_.isTipped ? TIPPED_ARROW_LOCATION : NORMAL_ARROW_LOCATION;
   }

   public TippableArrowRenderState createRenderState() {
      return new TippableArrowRenderState();
   }

   public void extractRenderState(Arrow p_457595_, TippableArrowRenderState p_457596_, float p_366585_) {
      super.extractRenderState(p_457595_, p_457596_, p_366585_);
      p_457596_.isTipped = p_457595_.getColor() > 0;
   }
}
