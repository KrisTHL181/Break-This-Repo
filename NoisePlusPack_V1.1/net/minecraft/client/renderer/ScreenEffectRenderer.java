package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ScreenEffectRenderer {
   private static final Identifier UNDERWATER_LOCATION = Identifier.withDefaultNamespace("textures/misc/underwater.png");
   private final Minecraft minecraft;
   private final MaterialSet materials;
   private final MultiBufferSource bufferSource;
   public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
   private @Nullable ItemStack itemActivationItem;
   private int itemActivationTicks;
   private float itemActivationOffX;
   private float itemActivationOffY;

   public ScreenEffectRenderer(Minecraft p_408767_, MaterialSet p_428240_, MultiBufferSource p_405885_) {
      this.minecraft = p_408767_;
      this.materials = p_428240_;
      this.bufferSource = p_405885_;
   }

   public void tick() {
      if (this.itemActivationTicks > 0) {
         this.itemActivationTicks--;
         if (this.itemActivationTicks == 0) {
            this.itemActivationItem = null;
         }
      }
   }

   public void renderScreenEffect(boolean p_409640_, float p_408951_, SubmitNodeCollector p_429619_) {
      PoseStack posestack = new PoseStack();
      Player player = this.minecraft.player;
      if (this.minecraft.options.getCameraType().isFirstPerson() && !p_409640_) {
         if (!player.noPhysics) {
            BlockState blockstate = getViewBlockingState(player);
            if (blockstate != null) {
               renderTex(this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(blockstate), posestack, this.bufferSource);
            }
         }

         if (!this.minecraft.player.isSpectator()) {
            if (this.minecraft.player.isEyeInFluid(FluidTags.WATER)) {
               renderWater(this.minecraft, posestack, this.bufferSource);
            }

            if (this.minecraft.player.isOnFire()) {
               TextureAtlasSprite textureatlassprite = this.materials.get(ModelBakery.FIRE_1);
               renderFire(posestack, this.bufferSource, textureatlassprite);
            }
         }
      }

      if (!this.minecraft.options.hideGui) {
         this.renderItemActivationAnimation(posestack, p_408951_, p_429619_);
      }
   }

   private void renderItemActivationAnimation(PoseStack p_408146_, float p_408750_, SubmitNodeCollector p_425832_) {
      if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
         int i = 40 - this.itemActivationTicks;
         float f = (i + p_408750_) / 40.0F;
         float f1 = f * f;
         float f2 = f * f1;
         float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
         float f4 = f3 * (float) Math.PI;
         float f5 = (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight();
         float f6 = this.itemActivationOffX * 0.3F * f5;
         float f7 = this.itemActivationOffY * 0.3F;
         p_408146_.pushPose();
         p_408146_.translate(f6 * Mth.abs(Mth.sin(f4 * 2.0F)), f7 * Mth.abs(Mth.sin(f4 * 2.0F)), -10.0F + 9.0F * Mth.sin(f4));
         float f8 = 0.8F;
         p_408146_.scale(0.8F, 0.8F, 0.8F);
         p_408146_.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
         p_408146_.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
         p_408146_.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
         this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
         ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
         this.minecraft
            .getItemModelResolver()
            .updateForTopItem(itemstackrenderstate, this.itemActivationItem, ItemDisplayContext.FIXED, this.minecraft.level, null, 0);
         itemstackrenderstate.submit(p_408146_, p_425832_, 15728880, OverlayTexture.NO_OVERLAY, 0);
         p_408146_.popPose();
      }
   }

   public void resetItemActivation() {
      this.itemActivationItem = null;
   }

   public void displayItemActivation(ItemStack p_407673_, RandomSource p_406761_) {
      this.itemActivationItem = p_407673_;
      this.itemActivationTicks = 40;
      this.itemActivationOffX = p_406761_.nextFloat() * 2.0F - 1.0F;
      this.itemActivationOffY = p_406761_.nextFloat() * 2.0F - 1.0F;
   }

   private static @Nullable BlockState getViewBlockingState(Player p_110717_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < 8; i++) {
         double d0 = p_110717_.getX() + ((i >> 0) % 2 - 0.5F) * p_110717_.getBbWidth() * 0.8F;
         double d1 = p_110717_.getEyeY() + ((i >> 1) % 2 - 0.5F) * 0.1F * p_110717_.getScale();
         double d2 = p_110717_.getZ() + ((i >> 2) % 2 - 0.5F) * p_110717_.getBbWidth() * 0.8F;
         blockpos$mutableblockpos.set(d0, d1, d2);
         BlockState blockstate = p_110717_.level().getBlockState(blockpos$mutableblockpos);
         if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(p_110717_.level(), blockpos$mutableblockpos)) {
            return blockstate;
         }
      }

      return null;
   }

   private static void renderTex(TextureAtlasSprite p_173297_, PoseStack p_173298_, MultiBufferSource p_376984_) {
      float f = 0.1F;
      int i = ARGB.colorFromFloat(1.0F, 0.1F, 0.1F, 0.1F);
      float f1 = -1.0F;
      float f2 = 1.0F;
      float f3 = -1.0F;
      float f4 = 1.0F;
      float f5 = -0.5F;
      float f6 = p_173297_.getU0();
      float f7 = p_173297_.getU1();
      float f8 = p_173297_.getV0();
      float f9 = p_173297_.getV1();
      Matrix4f matrix4f = p_173298_.last().pose();
      VertexConsumer vertexconsumer = p_376984_.getBuffer(RenderTypes.blockScreenEffect(p_173297_.atlasLocation()));
      vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(f7, f9).setColor(i);
      vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(f6, f9).setColor(i);
      vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(f6, f8).setColor(i);
      vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(f7, f8).setColor(i);
   }

   private static void renderWater(Minecraft p_110726_, PoseStack p_110727_, MultiBufferSource p_376402_) {
      BlockPos blockpos = BlockPos.containing(p_110726_.player.getX(), p_110726_.player.getEyeY(), p_110726_.player.getZ());
      float f = LightTexture.getBrightness(p_110726_.player.level().dimensionType(), p_110726_.player.level().getMaxLocalRawBrightness(blockpos));
      int i = ARGB.colorFromFloat(0.1F, f, f, f);
      float f1 = 4.0F;
      float f2 = -1.0F;
      float f3 = 1.0F;
      float f4 = -1.0F;
      float f5 = 1.0F;
      float f6 = -0.5F;
      float f7 = -p_110726_.player.getYRot() / 64.0F;
      float f8 = p_110726_.player.getXRot() / 64.0F;
      Matrix4f matrix4f = p_110727_.last().pose();
      VertexConsumer vertexconsumer = p_376402_.getBuffer(RenderTypes.blockScreenEffect(UNDERWATER_LOCATION));
      vertexconsumer.addVertex(matrix4f, -1.0F, -1.0F, -0.5F).setUv(4.0F + f7, 4.0F + f8).setColor(i);
      vertexconsumer.addVertex(matrix4f, 1.0F, -1.0F, -0.5F).setUv(0.0F + f7, 4.0F + f8).setColor(i);
      vertexconsumer.addVertex(matrix4f, 1.0F, 1.0F, -0.5F).setUv(0.0F + f7, 0.0F + f8).setColor(i);
      vertexconsumer.addVertex(matrix4f, -1.0F, 1.0F, -0.5F).setUv(4.0F + f7, 0.0F + f8).setColor(i);
   }

   private static void renderFire(PoseStack p_110730_, MultiBufferSource p_376973_, TextureAtlasSprite p_422518_) {
      VertexConsumer vertexconsumer = p_376973_.getBuffer(RenderTypes.fireScreenEffect(p_422518_.atlasLocation()));
      float f = p_422518_.getU0();
      float f1 = p_422518_.getU1();
      float f2 = p_422518_.getV0();
      float f3 = p_422518_.getV1();
      float f4 = 1.0F;

      for (int i = 0; i < 2; i++) {
         p_110730_.pushPose();
         float f5 = -0.5F;
         float f6 = 0.5F;
         float f7 = -0.5F;
         float f8 = 0.5F;
         float f9 = -0.5F;
         p_110730_.translate(-(i * 2 - 1) * 0.24F, -0.3F, 0.0F);
         p_110730_.mulPose(Axis.YP.rotationDegrees((i * 2 - 1) * 10.0F));
         Matrix4f matrix4f = p_110730_.last().pose();
         vertexconsumer.addVertex(matrix4f, -0.5F, -0.5F, -0.5F).setUv(f1, f3).setColor(1.0F, 1.0F, 1.0F, 0.9F);
         vertexconsumer.addVertex(matrix4f, 0.5F, -0.5F, -0.5F).setUv(f, f3).setColor(1.0F, 1.0F, 1.0F, 0.9F);
         vertexconsumer.addVertex(matrix4f, 0.5F, 0.5F, -0.5F).setUv(f, f2).setColor(1.0F, 1.0F, 1.0F, 0.9F);
         vertexconsumer.addVertex(matrix4f, -0.5F, 0.5F, -0.5F).setUv(f1, f2).setColor(1.0F, 1.0F, 1.0F, 0.9F);
         p_110730_.popPose();
      }
   }
}
