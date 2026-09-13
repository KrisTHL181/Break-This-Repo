package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EnergySwirlLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   public EnergySwirlLayer(RenderLayerParent<S, M> p_116967_) {
      super(p_116967_);
   }

   @Override
   public void submit(PoseStack p_431750_, SubmitNodeCollector p_424351_, int p_425927_, S p_426926_, float p_428987_, float p_424500_) {
      if (this.isPowered(p_426926_)) {
         float f = p_426926_.ageInTicks;
         M m = this.model();
         p_424351_.order(1)
            .submitModel(
               m,
               p_426926_,
               p_431750_,
               RenderTypes.energySwirl(this.getTextureLocation(), this.xOffset(f) % 1.0F, f * 0.01F % 1.0F),
               p_425927_,
               OverlayTexture.NO_OVERLAY,
               -8355712,
               null,
               p_426926_.outlineColor,
               null
            );
      }
   }

   protected abstract boolean isPowered(S var1);

   protected abstract float xOffset(float var1);

   protected abstract Identifier getTextureLocation();

   protected abstract M model();
}
