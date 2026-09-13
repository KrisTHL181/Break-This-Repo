package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.LecternRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity, LecternRenderState> {
   private final MaterialSet materials;
   private final BookModel bookModel;
   private final BookModel.State bookState = new BookModel.State(0.0F, 0.1F, 0.9F, 1.2F);

   public LecternRenderer(BlockEntityRendererProvider.Context p_173621_) {
      this.materials = p_173621_.materials();
      this.bookModel = new BookModel(p_173621_.bakeLayer(ModelLayers.BOOK));
   }

   public LecternRenderState createRenderState() {
      return new LecternRenderState();
   }

   public void extractRenderState(
      LecternBlockEntity p_427987_, LecternRenderState p_425307_, float p_429830_, Vec3 p_423596_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_425366_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_427987_, p_425307_, p_429830_, p_423596_, p_425366_);
      p_425307_.hasBook = p_427987_.getBlockState().getValue(LecternBlock.HAS_BOOK);
      p_425307_.yRot = p_427987_.getBlockState().getValue(LecternBlock.FACING).getClockWise().toYRot();
   }

   public void submit(LecternRenderState p_431401_, PoseStack p_428343_, SubmitNodeCollector p_431222_, CameraRenderState p_425118_) {
      if (p_431401_.hasBook) {
         p_428343_.pushPose();
         p_428343_.translate(0.5F, 1.0625F, 0.5F);
         p_428343_.mulPose(Axis.YP.rotationDegrees(-p_431401_.yRot));
         p_428343_.mulPose(Axis.ZP.rotationDegrees(67.5F));
         p_428343_.translate(0.0F, -0.125F, 0.0F);
         p_431222_.submitModel(
            this.bookModel,
            this.bookState,
            p_428343_,
            EnchantTableRenderer.BOOK_TEXTURE.renderType(RenderTypes::entitySolid),
            p_431401_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(EnchantTableRenderer.BOOK_TEXTURE),
            0,
            p_431401_.breakProgress
         );
         p_428343_.popPose();
      }
   }
}
