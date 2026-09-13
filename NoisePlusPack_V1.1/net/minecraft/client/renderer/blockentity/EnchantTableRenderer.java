package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EnchantTableRenderer implements BlockEntityRenderer<EnchantingTableBlockEntity, EnchantTableRenderState> {
   public static final Material BOOK_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("enchanting_table_book");
   private final MaterialSet materials;
   private final BookModel bookModel;

   public EnchantTableRenderer(BlockEntityRendererProvider.Context p_173619_) {
      this.materials = p_173619_.materials();
      this.bookModel = new BookModel(p_173619_.bakeLayer(ModelLayers.BOOK));
   }

   public EnchantTableRenderState createRenderState() {
      return new EnchantTableRenderState();
   }

   public void extractRenderState(
      EnchantingTableBlockEntity p_425544_,
      EnchantTableRenderState p_429180_,
      float p_430108_,
      Vec3 p_428735_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_429040_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_425544_, p_429180_, p_430108_, p_428735_, p_429040_);
      p_429180_.flip = Mth.lerp(p_430108_, p_425544_.oFlip, p_425544_.flip);
      p_429180_.open = Mth.lerp(p_430108_, p_425544_.oOpen, p_425544_.open);
      p_429180_.time = p_425544_.time + p_430108_;
      float f = p_425544_.rot - p_425544_.oRot;

      while (f >= (float) Math.PI) {
         f -= (float) (Math.PI * 2);
      }

      while (f < (float) -Math.PI) {
         f += (float) (Math.PI * 2);
      }

      p_429180_.yRot = p_425544_.oRot + f * p_430108_;
   }

   public void submit(EnchantTableRenderState p_424666_, PoseStack p_427655_, SubmitNodeCollector p_428087_, CameraRenderState p_422286_) {
      p_427655_.pushPose();
      p_427655_.translate(0.5F, 0.75F, 0.5F);
      p_427655_.translate(0.0F, 0.1F + Mth.sin(p_424666_.time * 0.1F) * 0.01F, 0.0F);
      float f = p_424666_.yRot;
      p_427655_.mulPose(Axis.YP.rotation(-f));
      p_427655_.mulPose(Axis.ZP.rotationDegrees(80.0F));
      float f1 = Mth.frac(p_424666_.flip + 0.25F) * 1.6F - 0.3F;
      float f2 = Mth.frac(p_424666_.flip + 0.75F) * 1.6F - 0.3F;
      BookModel.State bookmodel$state = new BookModel.State(p_424666_.time, Mth.clamp(f1, 0.0F, 1.0F), Mth.clamp(f2, 0.0F, 1.0F), p_424666_.open);
      p_428087_.submitModel(
         this.bookModel,
         bookmodel$state,
         p_427655_,
         BOOK_TEXTURE.renderType(RenderTypes::entitySolid),
         p_424666_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         -1,
         this.materials.get(BOOK_TEXTURE),
         0,
         p_424666_.breakProgress
      );
      p_427655_.popPose();
   }
}
