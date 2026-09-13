package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WolfCollarLayer extends RenderLayer<WolfRenderState, WolfModel> {
   private static final Identifier WOLF_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");

   public WolfCollarLayer(RenderLayerParent<WolfRenderState, WolfModel> p_117707_) {
      super(p_117707_);
   }

   public void submit(PoseStack p_430602_, SubmitNodeCollector p_430277_, int p_431658_, WolfRenderState p_430543_, float p_429085_, float p_425979_) {
      DyeColor dyecolor = p_430543_.collarColor;
      if (dyecolor != null && !p_430543_.isInvisible) {
         int i = dyecolor.getTextureDiffuseColor();
         p_430277_.order(1)
            .submitModel(
               this.getParentModel(),
               p_430543_,
               p_430602_,
               RenderTypes.entityCutoutNoCull(WOLF_COLLAR_LOCATION),
               p_431658_,
               OverlayTexture.NO_OVERLAY,
               i,
               null,
               p_430543_.outlineColor,
               null
            );
      }
   }
}
