package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.allay.AllayModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AllayRenderer extends MobRenderer<Allay, AllayRenderState, AllayModel> {
   private static final Identifier ALLAY_TEXTURE = Identifier.withDefaultNamespace("textures/entity/allay/allay.png");

   public AllayRenderer(EntityRendererProvider.Context p_234551_) {
      super(p_234551_, new AllayModel(p_234551_.bakeLayer(ModelLayers.ALLAY)), 0.4F);
      this.addLayer(new ItemInHandLayer<>(this));
   }

   public Identifier getTextureLocation(AllayRenderState p_366664_) {
      return ALLAY_TEXTURE;
   }

   public AllayRenderState createRenderState() {
      return new AllayRenderState();
   }

   public void extractRenderState(Allay p_365148_, AllayRenderState p_368332_, float p_362065_) {
      super.extractRenderState(p_365148_, p_368332_, p_362065_);
      ArmedEntityRenderState.extractArmedEntityRenderState(p_365148_, p_368332_, this.itemModelResolver, p_362065_);
      p_368332_.isDancing = p_365148_.isDancing();
      p_368332_.isSpinning = p_365148_.isSpinning();
      p_368332_.spinningProgress = p_365148_.getSpinningProgress(p_362065_);
      p_368332_.holdingAnimationProgress = p_365148_.getHoldingItemAnimationProgress(p_362065_);
   }

   protected int getBlockLightLevel(Allay p_234560_, BlockPos p_234561_) {
      return 15;
   }
}
