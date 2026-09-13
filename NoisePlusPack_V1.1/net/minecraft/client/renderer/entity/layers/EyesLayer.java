package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EyesLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   public EyesLayer(RenderLayerParent<S, M> p_116981_) {
      super(p_116981_);
   }

   @Override
   public void submit(PoseStack p_430448_, SubmitNodeCollector p_428922_, int p_422438_, S p_424839_, float p_431631_, float p_428191_) {
      p_428922_.order(1)
         .submitModel(
            this.getParentModel(), p_424839_, p_430448_, this.renderType(), p_422438_, OverlayTexture.NO_OVERLAY, -1, null, p_424839_.outlineColor, null
         );
   }

   public abstract RenderType renderType();
}
