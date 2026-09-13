package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity, VaultRenderState> {
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public VaultRenderer(BlockEntityRendererProvider.Context p_335617_) {
      this.itemModelResolver = p_335617_.itemModelResolver();
   }

   public VaultRenderState createRenderState() {
      return new VaultRenderState();
   }

   public void extractRenderState(
      VaultBlockEntity p_429133_, VaultRenderState p_428366_, float p_426429_, Vec3 p_428835_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_431366_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_429133_, p_428366_, p_426429_, p_428835_, p_431366_);
      ItemStack itemstack = p_429133_.getSharedData().getDisplayItem();
      if (VaultBlockEntity.Client.shouldDisplayActiveEffects(p_429133_.getSharedData()) && !itemstack.isEmpty() && p_429133_.getLevel() != null) {
         p_428366_.displayItem = new ItemClusterRenderState();
         this.itemModelResolver.updateForTopItem(p_428366_.displayItem.item, itemstack, ItemDisplayContext.GROUND, p_429133_.getLevel(), null, 0);
         p_428366_.displayItem.count = ItemClusterRenderState.getRenderedAmount(itemstack.getCount());
         p_428366_.displayItem.seed = ItemClusterRenderState.getSeedForItemStack(itemstack);
         VaultClientData vaultclientdata = p_429133_.getClientData();
         p_428366_.spin = Mth.rotLerp(p_426429_, vaultclientdata.previousSpin(), vaultclientdata.currentSpin());
      }
   }

   public void submit(VaultRenderState p_423435_, PoseStack p_431651_, SubmitNodeCollector p_429833_, CameraRenderState p_428537_) {
      if (p_423435_.displayItem != null) {
         p_431651_.pushPose();
         p_431651_.translate(0.5F, 0.4F, 0.5F);
         p_431651_.mulPose(Axis.YP.rotationDegrees(p_423435_.spin));
         ItemEntityRenderer.renderMultipleFromCount(p_431651_, p_429833_, p_423435_.lightCoords, p_423435_.displayItem, this.random);
         p_431651_.popPose();
      }
   }
}
