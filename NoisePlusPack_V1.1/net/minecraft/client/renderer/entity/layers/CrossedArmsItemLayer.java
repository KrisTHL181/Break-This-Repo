package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerLikeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrossedArmsItemLayer<S extends HoldingEntityRenderState, M extends EntityModel<S> & VillagerLikeModel> extends RenderLayer<S, M> {
   public CrossedArmsItemLayer(RenderLayerParent<S, M> p_234818_) {
      super(p_234818_);
   }

   public void submit(PoseStack p_428438_, SubmitNodeCollector p_428031_, int p_430795_, S p_425971_, float p_427932_, float p_428075_) {
      ItemStackRenderState itemstackrenderstate = p_425971_.heldItem;
      if (!itemstackrenderstate.isEmpty()) {
         p_428438_.pushPose();
         this.applyTranslation(p_425971_, p_428438_);
         itemstackrenderstate.submit(p_428438_, p_428031_, p_430795_, OverlayTexture.NO_OVERLAY, p_425971_.outlineColor);
         p_428438_.popPose();
      }
   }

   protected void applyTranslation(S p_378379_, PoseStack p_378611_) {
      this.getParentModel().translateToArms(p_378379_, p_378611_);
      p_378611_.mulPose(Axis.XP.rotation(0.75F));
      p_378611_.scale(1.07F, 1.07F, 1.07F);
      p_378611_.translate(0.0F, 0.13F, -0.34F);
      p_378611_.mulPose(Axis.XP.rotation((float) Math.PI));
   }
}
