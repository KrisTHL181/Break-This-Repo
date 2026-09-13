package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandRenderer {
   private static final RenderType MAP_BACKGROUND = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background.png"));
   private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderTypes.text(
      Identifier.withDefaultNamespace("textures/map/map_background_checkerboard.png")
   );
   private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
   private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
   private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
   private static final float ITEM_HEIGHT_SCALE = -0.6F;
   private static final float ITEM_POS_X = 0.56F;
   private static final float ITEM_POS_Y = -0.52F;
   private static final float ITEM_POS_Z = -0.72F;
   private static final float ITEM_PRESWING_ROT_Y = 45.0F;
   private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
   private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
   private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
   private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
   private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
   private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
   private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
   private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
   private static final double EAT_JIGGLE_EXPONENT = 27.0;
   private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
   private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
   private static final float ARM_SWING_X_POS_SCALE = -0.3F;
   private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
   private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
   private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
   private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float ARM_HEIGHT_SCALE = -0.6F;
   private static final float ARM_POS_SCALE = 0.8F;
   private static final float ARM_POS_X = 0.8F;
   private static final float ARM_POS_Y = -0.75F;
   private static final float ARM_POS_Z = -0.9F;
   private static final float ARM_PRESWING_ROT_Y = 45.0F;
   private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
   private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
   private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
   private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
   private static final int ARM_ROT_X = 200;
   private static final int ARM_ROT_Y = -135;
   private static final int ARM_ROT_Z = 120;
   private static final float MAP_SWING_X_POS_SCALE = -0.4F;
   private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
   private static final float MAP_HANDS_POS_X = 0.0F;
   private static final float MAP_HANDS_POS_Y = 0.04F;
   private static final float MAP_HANDS_POS_Z = -0.72F;
   private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
   private static final float MAP_HANDS_TILT_SCALE = -0.5F;
   private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
   private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
   private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
   private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
   private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
   private static final float MAP_HAND_X_POS = 0.3F;
   private static final float MAP_HAND_Y_POS = -1.1F;
   private static final float MAP_HAND_Z_POS = 0.45F;
   private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
   private static final float MAP_PRE_ROT_SCALE = 0.38F;
   private static final float MAP_GLOBAL_X_POS = -0.5F;
   private static final float MAP_GLOBAL_Y_POS = -0.5F;
   private static final float MAP_GLOBAL_Z_POS = 0.0F;
   private static final float MAP_FINAL_SCALE = 0.0078125F;
   private static final int MAP_BORDER = 7;
   private static final int MAP_HEIGHT = 128;
   private static final int MAP_WIDTH = 128;
   private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
   private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
   private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
   private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_SCALE = 0.2F;
   private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
   private final Minecraft minecraft;
   private final MapRenderState mapRenderState = new MapRenderState();
   private ItemStack mainHandItem = ItemStack.EMPTY;
   private ItemStack offHandItem = ItemStack.EMPTY;
   private float mainHandHeight;
   private float oMainHandHeight;
   private float offHandHeight;
   private float oOffHandHeight;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemModelResolver itemModelResolver;

   public ItemInHandRenderer(Minecraft p_234241_, EntityRenderDispatcher p_234242_, ItemModelResolver p_376876_) {
      this.minecraft = p_234241_;
      this.entityRenderDispatcher = p_234242_;
      this.itemModelResolver = p_376876_;
   }

   public void renderItem(
      LivingEntity p_270072_, ItemStack p_270793_, ItemDisplayContext p_270837_, PoseStack p_270974_, SubmitNodeCollector p_429847_, int p_270103_
   ) {
      if (!p_270793_.isEmpty()) {
         ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
         this.itemModelResolver
            .updateForTopItem(itemstackrenderstate, p_270793_, p_270837_, p_270072_.level(), p_270072_, p_270072_.getId() + p_270837_.ordinal());
         itemstackrenderstate.submit(p_270974_, p_429847_, p_270103_, OverlayTexture.NO_OVERLAY, 0);
      }
   }

   private float calculateMapTilt(float p_109313_) {
      float f = 1.0F - p_109313_ / 45.0F + 0.1F;
      f = Mth.clamp(f, 0.0F, 1.0F);
      return -Mth.cos(f * (float) Math.PI) * 0.5F + 0.5F;
   }

   private void renderMapHand(PoseStack p_109362_, SubmitNodeCollector p_427410_, int p_109364_, HumanoidArm p_109365_) {
      AvatarRenderer<AbstractClientPlayer> avatarrenderer = this.entityRenderDispatcher.getPlayerRenderer(this.minecraft.player);
      p_109362_.pushPose();
      float f = p_109365_ == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      p_109362_.mulPose(Axis.YP.rotationDegrees(92.0F));
      p_109362_.mulPose(Axis.XP.rotationDegrees(45.0F));
      p_109362_.mulPose(Axis.ZP.rotationDegrees(f * -41.0F));
      p_109362_.translate(f * 0.3F, -1.1F, 0.45F);
      Identifier identifier = this.minecraft.player.getSkin().body().texturePath();
      if (p_109365_ == HumanoidArm.RIGHT) {
         avatarrenderer.renderRightHand(p_109362_, p_427410_, p_109364_, identifier, this.minecraft.player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
      } else {
         avatarrenderer.renderLeftHand(p_109362_, p_427410_, p_109364_, identifier, this.minecraft.player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
      }

      p_109362_.popPose();
   }

   private void renderOneHandedMap(
      PoseStack p_109354_, SubmitNodeCollector p_428648_, int p_109356_, float p_109357_, HumanoidArm p_109358_, float p_109359_, ItemStack p_109360_
   ) {
      float f = p_109358_ == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      p_109354_.translate(f * 0.125F, -0.125F, 0.0F);
      if (!this.minecraft.player.isInvisible()) {
         p_109354_.pushPose();
         p_109354_.mulPose(Axis.ZP.rotationDegrees(f * 10.0F));
         this.renderPlayerArm(p_109354_, p_428648_, p_109356_, p_109357_, p_109359_, p_109358_);
         p_109354_.popPose();
      }

      p_109354_.pushPose();
      p_109354_.translate(f * 0.51F, -0.08F + p_109357_ * -1.2F, -0.75F);
      float f1 = Mth.sqrt(p_109359_);
      float f2 = Mth.sin(f1 * (float) Math.PI);
      float f3 = -0.5F * f2;
      float f4 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
      float f5 = -0.3F * Mth.sin(p_109359_ * (float) Math.PI);
      p_109354_.translate(f * f3, f4 - 0.3F * f2, f5);
      p_109354_.mulPose(Axis.XP.rotationDegrees(f2 * -45.0F));
      p_109354_.mulPose(Axis.YP.rotationDegrees(f * f2 * -30.0F));
      this.renderMap(p_109354_, p_428648_, p_109356_, p_109360_);
      p_109354_.popPose();
   }

   private void renderTwoHandedMap(PoseStack p_109340_, SubmitNodeCollector p_425898_, int p_109342_, float p_109343_, float p_109344_, float p_109345_) {
      float f = Mth.sqrt(p_109345_);
      float f1 = -0.2F * Mth.sin(p_109345_ * (float) Math.PI);
      float f2 = -0.4F * Mth.sin(f * (float) Math.PI);
      p_109340_.translate(0.0F, -f1 / 2.0F, f2);
      float f3 = this.calculateMapTilt(p_109343_);
      p_109340_.translate(0.0F, 0.04F + p_109344_ * -1.2F + f3 * -0.5F, -0.72F);
      p_109340_.mulPose(Axis.XP.rotationDegrees(f3 * -85.0F));
      if (!this.minecraft.player.isInvisible()) {
         p_109340_.pushPose();
         p_109340_.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.renderMapHand(p_109340_, p_425898_, p_109342_, HumanoidArm.RIGHT);
         this.renderMapHand(p_109340_, p_425898_, p_109342_, HumanoidArm.LEFT);
         p_109340_.popPose();
      }

      float f4 = Mth.sin(f * (float) Math.PI);
      p_109340_.mulPose(Axis.XP.rotationDegrees(f4 * 20.0F));
      p_109340_.scale(2.0F, 2.0F, 2.0F);
      this.renderMap(p_109340_, p_425898_, p_109342_, this.mainHandItem);
   }

   private void renderMap(PoseStack p_109367_, SubmitNodeCollector p_425019_, int p_109369_, ItemStack p_109370_) {
      p_109367_.mulPose(Axis.YP.rotationDegrees(180.0F));
      p_109367_.mulPose(Axis.ZP.rotationDegrees(180.0F));
      p_109367_.scale(0.38F, 0.38F, 0.38F);
      p_109367_.translate(-0.5F, -0.5F, 0.0F);
      p_109367_.scale(0.0078125F, 0.0078125F, 0.0078125F);
      MapId mapid = p_109370_.get(DataComponents.MAP_ID);
      MapItemSavedData mapitemsaveddata = MapItem.getSavedData(mapid, this.minecraft.level);
      RenderType rendertype = mapitemsaveddata == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD;
      p_425019_.submitCustomGeometry(p_109367_, rendertype, (p_431196_, p_422769_) -> {
         p_422769_.addVertex(p_431196_, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(p_109369_);
         p_422769_.addVertex(p_431196_, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(p_109369_);
         p_422769_.addVertex(p_431196_, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(p_109369_);
         p_422769_.addVertex(p_431196_, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(p_109369_);
      });
      if (mapitemsaveddata != null) {
         MapRenderer maprenderer = this.minecraft.getMapRenderer();
         maprenderer.extractRenderState(mapid, mapitemsaveddata, this.mapRenderState);
         maprenderer.render(this.mapRenderState, p_109367_, p_425019_, false, p_109369_);
      }
   }

   private void renderPlayerArm(PoseStack p_109347_, SubmitNodeCollector p_428322_, int p_109349_, float p_109350_, float p_109351_, HumanoidArm p_109352_) {
      boolean flag = p_109352_ != HumanoidArm.LEFT;
      float f = flag ? 1.0F : -1.0F;
      float f1 = Mth.sqrt(p_109351_);
      float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
      float f3 = 0.4F * Mth.sin(f1 * (float) (Math.PI * 2));
      float f4 = -0.4F * Mth.sin(p_109351_ * (float) Math.PI);
      p_109347_.translate(f * (f2 + 0.64000005F), f3 + -0.6F + p_109350_ * -0.6F, f4 + -0.71999997F);
      p_109347_.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
      float f5 = Mth.sin(p_109351_ * p_109351_ * (float) Math.PI);
      float f6 = Mth.sin(f1 * (float) Math.PI);
      p_109347_.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
      p_109347_.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
      AbstractClientPlayer abstractclientplayer = this.minecraft.player;
      p_109347_.translate(f * -1.0F, 3.6F, 3.5F);
      p_109347_.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
      p_109347_.mulPose(Axis.XP.rotationDegrees(200.0F));
      p_109347_.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
      p_109347_.translate(f * 5.6F, 0.0F, 0.0F);
      AvatarRenderer<AbstractClientPlayer> avatarrenderer = this.entityRenderDispatcher.getPlayerRenderer(abstractclientplayer);
      Identifier identifier = abstractclientplayer.getSkin().body().texturePath();
      if (flag) {
         avatarrenderer.renderRightHand(p_109347_, p_428322_, p_109349_, identifier, abstractclientplayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
      } else {
         avatarrenderer.renderLeftHand(p_109347_, p_428322_, p_109349_, identifier, abstractclientplayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
      }
   }

   private void applyEatTransform(PoseStack p_109331_, float p_109332_, HumanoidArm p_109333_, ItemStack p_109334_, Player p_343800_) {
      float f = p_343800_.getUseItemRemainingTicks() - p_109332_ + 1.0F;
      float f1 = f / p_109334_.getUseDuration(p_343800_);
      if (f1 < 0.8F) {
         float f2 = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
         p_109331_.translate(0.0F, f2, 0.0F);
      }

      float f3 = 1.0F - (float)Math.pow(f1, 27.0);
      int i = p_109333_ == HumanoidArm.RIGHT ? 1 : -1;
      p_109331_.translate(f3 * 0.6F * i, f3 * -0.5F, f3 * 0.0F);
      p_109331_.mulPose(Axis.YP.rotationDegrees(i * f3 * 90.0F));
      p_109331_.mulPose(Axis.XP.rotationDegrees(f3 * 10.0F));
      p_109331_.mulPose(Axis.ZP.rotationDegrees(i * f3 * 30.0F));
   }

   private void applyBrushTransform(PoseStack p_273513_, float p_273245_, HumanoidArm p_273726_, Player p_344712_) {
      float f = p_344712_.getUseItemRemainingTicks() % 10;
      float f1 = f - p_273245_ + 1.0F;
      float f2 = 1.0F - f1 / 10.0F;
      float f3 = -90.0F;
      float f4 = 60.0F;
      float f5 = 150.0F;
      float f6 = -15.0F;
      int i = 2;
      float f7 = -15.0F + 75.0F * Mth.cos(f2 * 2.0F * (float) Math.PI);
      if (p_273726_ != HumanoidArm.RIGHT) {
         p_273513_.translate(0.1, 0.83, 0.35);
         p_273513_.mulPose(Axis.XP.rotationDegrees(-80.0F));
         p_273513_.mulPose(Axis.YP.rotationDegrees(-90.0F));
         p_273513_.mulPose(Axis.XP.rotationDegrees(f7));
         p_273513_.translate(-0.3, 0.22, 0.35);
      } else {
         p_273513_.translate(-0.25, 0.22, 0.35);
         p_273513_.mulPose(Axis.XP.rotationDegrees(-80.0F));
         p_273513_.mulPose(Axis.YP.rotationDegrees(90.0F));
         p_273513_.mulPose(Axis.ZP.rotationDegrees(0.0F));
         p_273513_.mulPose(Axis.XP.rotationDegrees(f7));
      }
   }

   private void applyItemArmAttackTransform(PoseStack p_109336_, HumanoidArm p_109337_, float p_109338_) {
      int i = p_109337_ == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(p_109338_ * p_109338_ * (float) Math.PI);
      p_109336_.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
      float f1 = Mth.sin(Mth.sqrt(p_109338_) * (float) Math.PI);
      p_109336_.mulPose(Axis.ZP.rotationDegrees(i * f1 * -20.0F));
      p_109336_.mulPose(Axis.XP.rotationDegrees(f1 * -80.0F));
      p_109336_.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
   }

   private void applyItemArmTransform(PoseStack p_109383_, HumanoidArm p_109384_, float p_109385_) {
      int i = p_109384_ == HumanoidArm.RIGHT ? 1 : -1;
      p_109383_.translate(i * 0.56F, -0.52F + p_109385_ * -0.6F, -0.72F);
   }

   public void renderHandsWithItems(float p_109315_, PoseStack p_109316_, SubmitNodeCollector p_424174_, LocalPlayer p_109318_, int p_109319_) {
      float f = p_109318_.getAttackAnim(p_109315_);
      InteractionHand interactionhand = (InteractionHand)MoreObjects.firstNonNull(p_109318_.swingingArm, InteractionHand.MAIN_HAND);
      float f1 = p_109318_.getXRot(p_109315_);
      ItemInHandRenderer.HandRenderSelection iteminhandrenderer$handrenderselection = evaluateWhichHandsToRender(p_109318_);
      float f2 = Mth.lerp(p_109315_, p_109318_.xBobO, p_109318_.xBob);
      float f3 = Mth.lerp(p_109315_, p_109318_.yBobO, p_109318_.yBob);
      p_109316_.mulPose(Axis.XP.rotationDegrees((p_109318_.getViewXRot(p_109315_) - f2) * 0.1F));
      p_109316_.mulPose(Axis.YP.rotationDegrees((p_109318_.getViewYRot(p_109315_) - f3) * 0.1F));
      if (iteminhandrenderer$handrenderselection.renderMainHand) {
         float f4 = interactionhand == InteractionHand.MAIN_HAND ? f : 0.0F;
         float f5 = this.itemModelResolver.swapAnimationScale(this.mainHandItem) * (1.0F - Mth.lerp(p_109315_, this.oMainHandHeight, this.mainHandHeight));
         this.renderArmWithItem(p_109318_, p_109315_, f1, InteractionHand.MAIN_HAND, f4, this.mainHandItem, f5, p_109316_, p_424174_, p_109319_);
      }

      if (iteminhandrenderer$handrenderselection.renderOffHand) {
         float f6 = interactionhand == InteractionHand.OFF_HAND ? f : 0.0F;
         float f7 = this.itemModelResolver.swapAnimationScale(this.offHandItem) * (1.0F - Mth.lerp(p_109315_, this.oOffHandHeight, this.offHandHeight));
         this.renderArmWithItem(p_109318_, p_109315_, f1, InteractionHand.OFF_HAND, f6, this.offHandItem, f7, p_109316_, p_424174_, p_109319_);
      }

      this.minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
      this.minecraft.renderBuffers().bufferSource().endBatch();
   }

   @VisibleForTesting
   static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer p_172915_) {
      ItemStack itemstack = p_172915_.getMainHandItem();
      ItemStack itemstack1 = p_172915_.getOffhandItem();
      boolean flag = itemstack.is(Items.BOW) || itemstack1.is(Items.BOW);
      boolean flag1 = itemstack.is(Items.CROSSBOW) || itemstack1.is(Items.CROSSBOW);
      if (!flag && !flag1) {
         return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else if (p_172915_.isUsingItem()) {
         return selectionUsingItemWhileHoldingBowLike(p_172915_);
      } else {
         return isChargedCrossbow(itemstack)
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      }
   }

   private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer p_172917_) {
      ItemStack itemstack = p_172917_.getUseItem();
      InteractionHand interactionhand = p_172917_.getUsedItemHand();
      if (!itemstack.is(Items.BOW) && !itemstack.is(Items.CROSSBOW)) {
         return interactionhand == InteractionHand.MAIN_HAND && isChargedCrossbow(p_172917_.getOffhandItem())
            ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY
            : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else {
         return ItemInHandRenderer.HandRenderSelection.onlyForHand(interactionhand);
      }
   }

   private static boolean isChargedCrossbow(ItemStack p_172913_) {
      return p_172913_.is(Items.CROSSBOW) && CrossbowItem.isCharged(p_172913_);
   }

   private void renderArmWithItem(
      AbstractClientPlayer p_109372_,
      float p_109373_,
      float p_109374_,
      InteractionHand p_109375_,
      float p_109376_,
      ItemStack p_109377_,
      float p_109378_,
      PoseStack p_109379_,
      SubmitNodeCollector p_425151_,
      int p_109381_
   ) {
      if (!p_109372_.isScoping()) {
         boolean flag = p_109375_ == InteractionHand.MAIN_HAND;
         HumanoidArm humanoidarm = flag ? p_109372_.getMainArm() : p_109372_.getMainArm().getOpposite();
         p_109379_.pushPose();
         if (p_109377_.isEmpty()) {
            if (flag && !p_109372_.isInvisible()) {
               this.renderPlayerArm(p_109379_, p_425151_, p_109381_, p_109378_, p_109376_, humanoidarm);
            }
         } else if (p_109377_.has(DataComponents.MAP_ID)) {
            if (flag && this.offHandItem.isEmpty()) {
               this.renderTwoHandedMap(p_109379_, p_425151_, p_109381_, p_109374_, p_109378_, p_109376_);
            } else {
               this.renderOneHandedMap(p_109379_, p_425151_, p_109381_, p_109378_, humanoidarm, p_109376_, p_109377_);
            }
         } else if (p_109377_.is(Items.CROSSBOW)) {
            this.applyItemArmTransform(p_109379_, humanoidarm, p_109378_);
            boolean flag1 = CrossbowItem.isCharged(p_109377_);
            boolean flag2 = humanoidarm == HumanoidArm.RIGHT;
            int i = flag2 ? 1 : -1;
            if (p_109372_.isUsingItem() && p_109372_.getUseItemRemainingTicks() > 0 && p_109372_.getUsedItemHand() == p_109375_ && !flag1) {
               p_109379_.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
               p_109379_.mulPose(Axis.XP.rotationDegrees(-11.935F));
               p_109379_.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
               p_109379_.mulPose(Axis.ZP.rotationDegrees(i * -9.785F));
               float f = p_109377_.getUseDuration(p_109372_) - (p_109372_.getUseItemRemainingTicks() - p_109373_ + 1.0F);
               float f1 = f / CrossbowItem.getChargeDuration(p_109377_, p_109372_);
               if (f1 > 1.0F) {
                  f1 = 1.0F;
               }

               if (f1 > 0.1F) {
                  float f2 = Mth.sin((f - 0.1F) * 1.3F);
                  float f3 = f1 - 0.1F;
                  float f4 = f2 * f3;
                  p_109379_.translate(f4 * 0.0F, f4 * 0.004F, f4 * 0.0F);
               }

               p_109379_.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.04F);
               p_109379_.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
               p_109379_.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
            } else {
               this.swingArm(p_109376_, p_109379_, i, humanoidarm);
               if (flag1 && p_109376_ < 0.001F && flag) {
                  p_109379_.translate(i * -0.641864F, 0.0F, 0.0F);
                  p_109379_.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
               }
            }

            this.renderItem(
               p_109372_,
               p_109377_,
               flag2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               p_109379_,
               p_425151_,
               p_109381_
            );
         } else {
            boolean flag3 = humanoidarm == HumanoidArm.RIGHT;
            int j = flag3 ? 1 : -1;
            if (p_109372_.isUsingItem() && p_109372_.getUseItemRemainingTicks() > 0 && p_109372_.getUsedItemHand() == p_109375_) {
               ItemUseAnimation itemuseanimation = p_109377_.getUseAnimation();
               if (!itemuseanimation.hasCustomArmTransform()) {
                  this.applyItemArmTransform(p_109379_, humanoidarm, p_109378_);
               }

               switch (itemuseanimation) {
                  case NONE:
                  default:
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatTransform(p_109379_, p_109373_, humanoidarm, p_109377_, p_109372_);
                     this.applyItemArmTransform(p_109379_, humanoidarm, p_109378_);
                     break;
                  case BLOCK:
                     if (!(p_109377_.getItem() instanceof ShieldItem)) {
                        p_109379_.translate(j * -0.14142136F, 0.08F, 0.14142136F);
                        p_109379_.mulPose(Axis.XP.rotationDegrees(-102.25F));
                        p_109379_.mulPose(Axis.YP.rotationDegrees(j * 13.365F));
                        p_109379_.mulPose(Axis.ZP.rotationDegrees(j * 78.05F));
                     }
                     break;
                  case BOW:
                     p_109379_.translate(j * -0.2785682F, 0.18344387F, 0.15731531F);
                     p_109379_.mulPose(Axis.XP.rotationDegrees(-13.935F));
                     p_109379_.mulPose(Axis.YP.rotationDegrees(j * 35.3F));
                     p_109379_.mulPose(Axis.ZP.rotationDegrees(j * -9.785F));
                     float f7 = p_109377_.getUseDuration(p_109372_) - (p_109372_.getUseItemRemainingTicks() - p_109373_ + 1.0F);
                     float f9 = f7 / 20.0F;
                     f9 = (f9 * f9 + f9 * 2.0F) / 3.0F;
                     if (f9 > 1.0F) {
                        f9 = 1.0F;
                     }

                     if (f9 > 0.1F) {
                        float f11 = Mth.sin((f7 - 0.1F) * 1.3F);
                        float f13 = f9 - 0.1F;
                        float f15 = f11 * f13;
                        p_109379_.translate(f15 * 0.0F, f15 * 0.004F, f15 * 0.0F);
                     }

                     p_109379_.translate(f9 * 0.0F, f9 * 0.0F, f9 * 0.04F);
                     p_109379_.scale(1.0F, 1.0F, 1.0F + f9 * 0.2F);
                     p_109379_.mulPose(Axis.YN.rotationDegrees(j * 45.0F));
                     break;
                  case TRIDENT:
                     p_109379_.translate(j * -0.5F, 0.7F, 0.1F);
                     p_109379_.mulPose(Axis.XP.rotationDegrees(-55.0F));
                     p_109379_.mulPose(Axis.YP.rotationDegrees(j * 35.3F));
                     p_109379_.mulPose(Axis.ZP.rotationDegrees(j * -9.785F));
                     float f6 = p_109377_.getUseDuration(p_109372_) - (p_109372_.getUseItemRemainingTicks() - p_109373_ + 1.0F);
                     float f8 = f6 / 10.0F;
                     if (f8 > 1.0F) {
                        f8 = 1.0F;
                     }

                     if (f8 > 0.1F) {
                        float f10 = Mth.sin((f6 - 0.1F) * 1.3F);
                        float f12 = f8 - 0.1F;
                        float f14 = f10 * f12;
                        p_109379_.translate(f14 * 0.0F, f14 * 0.004F, f14 * 0.0F);
                     }

                     p_109379_.translate(0.0F, 0.0F, f8 * 0.2F);
                     p_109379_.scale(1.0F, 1.0F, 1.0F + f8 * 0.2F);
                     p_109379_.mulPose(Axis.YN.rotationDegrees(j * 45.0F));
                     break;
                  case BRUSH:
                     this.applyBrushTransform(p_109379_, p_109373_, humanoidarm, p_109372_);
                     break;
                  case BUNDLE:
                     this.swingArm(p_109376_, p_109379_, j, humanoidarm);
                     break;
                  case SPEAR:
                     p_109379_.translate(j * 0.56F, -0.52F, -0.72F);
                     float f5 = p_109377_.getUseDuration(p_109372_) - (p_109372_.getUseItemRemainingTicks() - p_109373_ + 1.0F);
                     SpearAnimations.firstPersonUse(p_109372_.getTicksSinceLastKineticHitFeedback(p_109373_), p_109379_, f5, humanoidarm, p_109377_);
               }
            } else if (p_109372_.isAutoSpinAttack()) {
               this.applyItemArmTransform(p_109379_, humanoidarm, p_109378_);
               p_109379_.translate(j * -0.4F, 0.8F, 0.3F);
               p_109379_.mulPose(Axis.YP.rotationDegrees(j * 65.0F));
               p_109379_.mulPose(Axis.ZP.rotationDegrees(j * -85.0F));
            } else {
               this.applyItemArmTransform(p_109379_, humanoidarm, p_109378_);
               switch (p_109377_.getSwingAnimation().type()) {
                  case NONE:
                  default:
                     break;
                  case WHACK:
                     this.swingArm(p_109376_, p_109379_, j, humanoidarm);
                     break;
                  case STAB:
                     SpearAnimations.firstPersonAttack(p_109376_, p_109379_, j, humanoidarm);
               }
            }

            this.renderItem(
               p_109372_,
               p_109377_,
               flag3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
               p_109379_,
               p_425151_,
               p_109381_
            );
         }

         p_109379_.popPose();
      }
   }

   private void swingArm(float p_376030_, PoseStack p_378498_, int p_375719_, HumanoidArm p_377471_) {
      float f = -0.4F * Mth.sin(Mth.sqrt(p_376030_) * (float) Math.PI);
      float f1 = 0.2F * Mth.sin(Mth.sqrt(p_376030_) * (float) (Math.PI * 2));
      float f2 = -0.2F * Mth.sin(p_376030_ * (float) Math.PI);
      p_378498_.translate(p_375719_ * f, f1, f2);
      this.applyItemArmAttackTransform(p_378498_, p_377471_, p_376030_);
   }

   private boolean shouldInstantlyReplaceVisibleItem(ItemStack p_376473_, ItemStack p_378754_) {
      return ItemStack.matchesIgnoringComponents(p_376473_, p_378754_, DataComponentType::ignoreSwapAnimation)
         ? true
         : !this.itemModelResolver.shouldPlaySwapAnimation(p_378754_);
   }

   public void tick() {
      this.oMainHandHeight = this.mainHandHeight;
      this.oOffHandHeight = this.offHandHeight;
      LocalPlayer localplayer = this.minecraft.player;
      ItemStack itemstack = localplayer.getMainHandItem();
      ItemStack itemstack1 = localplayer.getOffhandItem();
      if (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, itemstack)) {
         this.mainHandItem = itemstack;
      }

      if (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, itemstack1)) {
         this.offHandItem = itemstack1;
      }

      if (localplayer.isHandsBusy()) {
         this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
         this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
      } else {
         float f = localplayer.getItemSwapScale(1.0F);
         float f1 = this.mainHandItem != itemstack ? 0.0F : f * f * f;
         float f2 = this.offHandItem != itemstack1 ? 0.0F : 1.0F;
         this.mainHandHeight = this.mainHandHeight + Mth.clamp(f1 - this.mainHandHeight, -0.4F, 0.4F);
         this.offHandHeight = this.offHandHeight + Mth.clamp(f2 - this.offHandHeight, -0.4F, 0.4F);
      }

      if (this.mainHandHeight < 0.1F) {
         this.mainHandItem = itemstack;
      }

      if (this.offHandHeight < 0.1F) {
         this.offHandItem = itemstack1;
      }
   }

   public void itemUsed(InteractionHand p_109321_) {
      if (p_109321_ == InteractionHand.MAIN_HAND) {
         this.mainHandHeight = 0.0F;
      } else {
         this.offHandHeight = 0.0F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   @VisibleForTesting
   enum HandRenderSelection {
      RENDER_BOTH_HANDS(true, true),
      RENDER_MAIN_HAND_ONLY(true, false),
      RENDER_OFF_HAND_ONLY(false, true);

      final boolean renderMainHand;
      final boolean renderOffHand;

      HandRenderSelection(final boolean p_172928_, final boolean p_172929_) {
         this.renderMainHand = p_172928_;
         this.renderOffHand = p_172929_;
      }

      public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand p_172932_) {
         return p_172932_ == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
      }
   }
}
