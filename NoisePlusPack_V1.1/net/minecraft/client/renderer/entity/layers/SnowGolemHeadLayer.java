package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SnowGolemRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SnowGolemHeadLayer extends RenderLayer<SnowGolemRenderState, SnowGolemModel> {
   private final BlockRenderDispatcher blockRenderer;

   public SnowGolemHeadLayer(RenderLayerParent<SnowGolemRenderState, SnowGolemModel> p_234871_, BlockRenderDispatcher p_234872_) {
      super(p_234871_);
      this.blockRenderer = p_234872_;
   }

   public void submit(PoseStack p_430306_, SubmitNodeCollector p_425009_, int p_430842_, SnowGolemRenderState p_428935_, float p_424807_, float p_426765_) {
      if (p_428935_.hasPumpkin && (!p_428935_.isInvisible || p_428935_.appearsGlowing())) {
         p_430306_.pushPose();
         this.getParentModel().getHead().translateAndRotate(p_430306_);
         float f = 0.625F;
         p_430306_.translate(0.0F, -0.34375F, 0.0F);
         p_430306_.mulPose(Axis.YP.rotationDegrees(180.0F));
         p_430306_.scale(0.625F, -0.625F, -0.625F);
         BlockState blockstate = Blocks.CARVED_PUMPKIN.defaultBlockState();
         BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
         int i = LivingEntityRenderer.getOverlayCoords(p_428935_, 0.0F);
         p_430306_.translate(-0.5F, -0.5F, -0.5F);
         RenderType rendertype = p_428935_.appearsGlowing() && p_428935_.isInvisible
            ? RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS)
            : ItemBlockRenderTypes.getRenderType(blockstate);
         p_425009_.submitBlockModel(p_430306_, rendertype, blockstatemodel, 0.0F, 0.0F, 0.0F, p_430842_, i, p_428935_.outlineColor);
         p_430306_.popPose();
      }
   }
}
