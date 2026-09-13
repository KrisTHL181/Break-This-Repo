package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
   private final BlockRenderDispatcher blockRenderer;

   public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> p_234850_, BlockRenderDispatcher p_234851_) {
      super(p_234850_);
      this.blockRenderer = p_234851_;
   }

   public void submit(PoseStack p_429754_, SubmitNodeCollector p_427787_, int p_422588_, MushroomCowRenderState p_431757_, float p_424255_, float p_423023_) {
      if (!p_431757_.isBaby) {
         boolean flag = p_431757_.appearsGlowing() && p_431757_.isInvisible;
         if (!p_431757_.isInvisible || flag) {
            BlockState blockstate = p_431757_.variant.getBlockState();
            int i = LivingEntityRenderer.getOverlayCoords(p_431757_, 0.0F);
            BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
            p_429754_.pushPose();
            p_429754_.translate(0.2F, -0.35F, 0.5F);
            p_429754_.mulPose(Axis.YP.rotationDegrees(-48.0F));
            p_429754_.scale(-1.0F, -1.0F, 1.0F);
            p_429754_.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(p_429754_, p_427787_, p_422588_, flag, p_431757_.outlineColor, blockstate, i, blockstatemodel);
            p_429754_.popPose();
            p_429754_.pushPose();
            p_429754_.translate(0.2F, -0.35F, 0.5F);
            p_429754_.mulPose(Axis.YP.rotationDegrees(42.0F));
            p_429754_.translate(0.1F, 0.0F, -0.6F);
            p_429754_.mulPose(Axis.YP.rotationDegrees(-48.0F));
            p_429754_.scale(-1.0F, -1.0F, 1.0F);
            p_429754_.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(p_429754_, p_427787_, p_422588_, flag, p_431757_.outlineColor, blockstate, i, blockstatemodel);
            p_429754_.popPose();
            p_429754_.pushPose();
            this.getParentModel().getHead().translateAndRotate(p_429754_);
            p_429754_.translate(0.0F, -0.7F, -0.2F);
            p_429754_.mulPose(Axis.YP.rotationDegrees(-78.0F));
            p_429754_.scale(-1.0F, -1.0F, 1.0F);
            p_429754_.translate(-0.5F, -0.5F, -0.5F);
            this.submitMushroomBlock(p_429754_, p_427787_, p_422588_, flag, p_431757_.outlineColor, blockstate, i, blockstatemodel);
            p_429754_.popPose();
         }
      }
   }

   private void submitMushroomBlock(
      PoseStack p_428055_,
      SubmitNodeCollector p_429562_,
      int p_423863_,
      boolean p_426386_,
      int p_431708_,
      BlockState p_424434_,
      int p_423497_,
      BlockStateModel p_428545_
   ) {
      if (p_426386_) {
         p_429562_.submitBlockModel(p_428055_, RenderTypes.outline(TextureAtlas.LOCATION_BLOCKS), p_428545_, 0.0F, 0.0F, 0.0F, p_423863_, p_423497_, p_431708_);
      } else {
         p_429562_.submitBlock(p_428055_, p_424434_, p_423863_, p_423497_, p_431708_);
      }
   }
}
