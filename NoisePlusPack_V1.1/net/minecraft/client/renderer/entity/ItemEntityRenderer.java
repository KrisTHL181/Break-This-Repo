package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625F;
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context p_174198_) {
      super(p_174198_);
      this.itemModelResolver = p_174198_.getItemModelResolver();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   public ItemEntityRenderState createRenderState() {
      return new ItemEntityRenderState();
   }

   public void extractRenderState(ItemEntity p_365788_, ItemEntityRenderState p_361751_, float p_369533_) {
      super.extractRenderState(p_365788_, p_361751_, p_369533_);
      p_361751_.bobOffset = p_365788_.bobOffs;
      p_361751_.extractItemGroupRenderState(p_365788_, p_365788_.getItem(), this.itemModelResolver);
   }

   public void submit(ItemEntityRenderState p_426384_, PoseStack p_430644_, SubmitNodeCollector p_429208_, CameraRenderState p_423141_) {
      if (!p_426384_.item.isEmpty()) {
         p_430644_.pushPose();
         AABB aabb = p_426384_.item.getModelBoundingBox();
         float f = -((float)aabb.minY) + 0.0625F;
         float f1 = Mth.sin(p_426384_.ageInTicks / 10.0F + p_426384_.bobOffset) * 0.1F + 0.1F;
         p_430644_.translate(0.0F, f1 + f, 0.0F);
         float f2 = ItemEntity.getSpin(p_426384_.ageInTicks, p_426384_.bobOffset);
         p_430644_.mulPose(Axis.YP.rotation(f2));
         submitMultipleFromCount(p_430644_, p_429208_, p_426384_.lightCoords, p_426384_, this.random, aabb);
         p_430644_.popPose();
         super.submit(p_426384_, p_430644_, p_429208_, p_423141_);
      }
   }

   public static void submitMultipleFromCount(
      PoseStack p_430176_, SubmitNodeCollector p_426685_, int p_430605_, ItemClusterRenderState p_425809_, RandomSource p_429667_
   ) {
      submitMultipleFromCount(p_430176_, p_426685_, p_430605_, p_425809_, p_429667_, p_425809_.item.getModelBoundingBox());
   }

   public static void submitMultipleFromCount(
      PoseStack p_426862_, SubmitNodeCollector p_430116_, int p_425551_, ItemClusterRenderState p_430657_, RandomSource p_424140_, AABB p_427782_
   ) {
      int i = p_430657_.count;
      if (i != 0) {
         p_424140_.setSeed(p_430657_.seed);
         ItemStackRenderState itemstackrenderstate = p_430657_.item;
         float f = (float)p_427782_.getZsize();
         if (f > 0.0625F) {
            itemstackrenderstate.submit(p_426862_, p_430116_, p_425551_, OverlayTexture.NO_OVERLAY, p_430657_.outlineColor);

            for (int j = 1; j < i; j++) {
               p_426862_.pushPose();
               float f1 = (p_424140_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f2 = (p_424140_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f3 = (p_424140_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               p_426862_.translate(f1, f2, f3);
               itemstackrenderstate.submit(p_426862_, p_430116_, p_425551_, OverlayTexture.NO_OVERLAY, p_430657_.outlineColor);
               p_426862_.popPose();
            }
         } else {
            float f4 = f * 1.5F;
            p_426862_.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
            itemstackrenderstate.submit(p_426862_, p_430116_, p_425551_, OverlayTexture.NO_OVERLAY, p_430657_.outlineColor);
            p_426862_.translate(0.0F, 0.0F, f4);

            for (int k = 1; k < i; k++) {
               p_426862_.pushPose();
               float f5 = (p_424140_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float f6 = (p_424140_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               p_426862_.translate(f5, f6, 0.0F);
               itemstackrenderstate.submit(p_426862_, p_430116_, p_425551_, OverlayTexture.NO_OVERLAY, p_430657_.outlineColor);
               p_426862_.popPose();
               p_426862_.translate(0.0F, 0.0F, f4);
            }
         }
      }
   }

   public static void renderMultipleFromCount(
      PoseStack p_330844_, SubmitNodeCollector p_428133_, int p_334169_, ItemClusterRenderState p_377874_, RandomSource p_331892_
   ) {
      AABB aabb = p_377874_.item.getModelBoundingBox();
      int i = p_377874_.count;
      if (i != 0) {
         p_331892_.setSeed(p_377874_.seed);
         ItemStackRenderState itemstackrenderstate = p_377874_.item;
         float f = (float)aabb.getZsize();
         if (f > 0.0625F) {
            itemstackrenderstate.submit(p_330844_, p_428133_, p_334169_, OverlayTexture.NO_OVERLAY, p_377874_.outlineColor);

            for (int j = 1; j < i; j++) {
               p_330844_.pushPose();
               float f1 = (p_331892_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f2 = (p_331892_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float f3 = (p_331892_.nextFloat() * 2.0F - 1.0F) * 0.15F;
               p_330844_.translate(f1, f2, f3);
               itemstackrenderstate.submit(p_330844_, p_428133_, p_334169_, OverlayTexture.NO_OVERLAY, p_377874_.outlineColor);
               p_330844_.popPose();
            }
         } else {
            float f4 = f * 1.5F;
            p_330844_.translate(0.0F, 0.0F, -(f4 * (i - 1) / 2.0F));
            itemstackrenderstate.submit(p_330844_, p_428133_, p_334169_, OverlayTexture.NO_OVERLAY, p_377874_.outlineColor);
            p_330844_.translate(0.0F, 0.0F, f4);

            for (int k = 1; k < i; k++) {
               p_330844_.pushPose();
               float f5 = (p_331892_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float f6 = (p_331892_.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               p_330844_.translate(f5, f6, 0.0F);
               itemstackrenderstate.submit(p_330844_, p_428133_, p_334169_, OverlayTexture.NO_OVERLAY, p_377874_.outlineColor);
               p_330844_.popPose();
               p_330844_.translate(0.0F, 0.0F, f4);
            }
         }
      }
   }
}
