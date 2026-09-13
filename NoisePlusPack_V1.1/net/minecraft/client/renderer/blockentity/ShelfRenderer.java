package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfRenderState> {
   private static final float ITEM_SIZE = 0.25F;
   private static final float ALIGN_ITEMS_TO_BOTTOM = -0.25F;
   private final ItemModelResolver itemModelResolver;

   public ShelfRenderer(BlockEntityRendererProvider.Context p_423895_) {
      this.itemModelResolver = p_423895_.itemModelResolver();
   }

   public ShelfRenderState createRenderState() {
      return new ShelfRenderState();
   }

   public void extractRenderState(
      ShelfBlockEntity p_430069_, ShelfRenderState p_430965_, float p_422712_, Vec3 p_427950_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_424228_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_430069_, p_430965_, p_422712_, p_427950_, p_424228_);
      p_430965_.alignToBottom = p_430069_.getAlignItemsToBottom();
      NonNullList<ItemStack> nonnulllist = p_430069_.getItems();
      int i = HashCommon.long2int(p_430069_.getBlockPos().asLong());

      for (int j = 0; j < nonnulllist.size(); j++) {
         ItemStack itemstack = nonnulllist.get(j);
         if (!itemstack.isEmpty()) {
            ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(itemstackrenderstate, itemstack, ItemDisplayContext.ON_SHELF, p_430069_.level(), p_430069_, i + j);
            p_430965_.items[j] = itemstackrenderstate;
         }
      }
   }

   public void submit(ShelfRenderState p_426683_, PoseStack p_425033_, SubmitNodeCollector p_426446_, CameraRenderState p_423073_) {
      Direction direction = p_426683_.blockState.getValue(ShelfBlock.FACING);
      float f = direction.getAxis().isHorizontal() ? -direction.toYRot() : 180.0F;

      for (int i = 0; i < p_426683_.items.length; i++) {
         ItemStackRenderState itemstackrenderstate = p_426683_.items[i];
         if (itemstackrenderstate != null) {
            this.submitItem(p_426683_, itemstackrenderstate, p_425033_, p_426446_, i, f);
         }
      }
   }

   private void submitItem(
      ShelfRenderState p_427871_, ItemStackRenderState p_426817_, PoseStack p_427926_, SubmitNodeCollector p_430029_, int p_425448_, float p_424010_
   ) {
      float f = (p_425448_ - 1) * 0.3125F;
      Vec3 vec3 = new Vec3(f, p_427871_.alignToBottom ? -0.25 : 0.0, -0.25);
      p_427926_.pushPose();
      p_427926_.translate(0.5F, 0.5F, 0.5F);
      p_427926_.mulPose(Axis.YP.rotationDegrees(p_424010_));
      p_427926_.translate(vec3);
      p_427926_.scale(0.25F, 0.25F, 0.25F);
      AABB aabb = p_426817_.getModelBoundingBox();
      double d0 = -aabb.minY;
      if (!p_427871_.alignToBottom) {
         d0 += -(aabb.maxY - aabb.minY) / 2.0;
      }

      p_427926_.translate(0.0, d0, 0.0);
      p_426817_.submit(p_427926_, p_430029_, p_427871_.lightCoords, OverlayTexture.NO_OVERLAY, 0);
      p_427926_.popPose();
   }
}
