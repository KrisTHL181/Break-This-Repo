package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignRenderer implements BlockEntityRenderer<SignBlockEntity, SignRenderState> {
   private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private final Font font;
   private final MaterialSet materials;

   public AbstractSignRenderer(BlockEntityRendererProvider.Context p_377237_) {
      this.font = p_377237_.font();
      this.materials = p_377237_.materials();
   }

   protected abstract Model.Simple getSignModel(BlockState var1, WoodType var2);

   protected abstract Material getSignMaterial(WoodType var1);

   protected abstract float getSignModelRenderScale();

   protected abstract float getSignTextRenderScale();

   protected abstract Vec3 getTextOffset();

   protected abstract void translateSign(PoseStack var1, float var2, BlockState var3);

   public void submit(SignRenderState p_427905_, PoseStack p_424465_, SubmitNodeCollector p_429928_, CameraRenderState p_425227_) {
      BlockState blockstate = p_427905_.blockState;
      SignBlock signblock = (SignBlock)blockstate.getBlock();
      Model.Simple model$simple = this.getSignModel(blockstate, signblock.type());
      this.submitSignWithText(p_427905_, p_424465_, blockstate, signblock, signblock.type(), model$simple, p_427905_.breakProgress, p_429928_);
   }

   private void submitSignWithText(
      SignRenderState p_422372_,
      PoseStack p_423315_,
      BlockState p_428932_,
      SignBlock p_427343_,
      WoodType p_425293_,
      Model.Simple p_431511_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_424361_,
      SubmitNodeCollector p_428891_
   ) {
      p_423315_.pushPose();
      this.translateSign(p_423315_, -p_427343_.getYRotationDegrees(p_428932_), p_428932_);
      this.submitSign(p_423315_, p_422372_.lightCoords, p_425293_, p_431511_, p_424361_, p_428891_);
      this.submitSignText(p_422372_, p_423315_, p_428891_, true);
      this.submitSignText(p_422372_, p_423315_, p_428891_, false);
      p_423315_.popPose();
   }

   protected void submitSign(
      PoseStack p_427475_,
      int p_423173_,
      WoodType p_430000_,
      Model.Simple p_427419_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_423457_,
      SubmitNodeCollector p_427645_
   ) {
      p_427475_.pushPose();
      float f = this.getSignModelRenderScale();
      p_427475_.scale(f, -f, -f);
      Material material = this.getSignMaterial(p_430000_);
      RenderType rendertype = material.renderType(p_427419_::renderType);
      p_427645_.submitModel(
         p_427419_, Unit.INSTANCE, p_427475_, rendertype, p_423173_, OverlayTexture.NO_OVERLAY, -1, this.materials.get(material), 0, p_423457_
      );
      p_427475_.popPose();
   }

   private void submitSignText(SignRenderState p_425491_, PoseStack p_426809_, SubmitNodeCollector p_423348_, boolean p_423601_) {
      SignText signtext = p_423601_ ? p_425491_.frontText : p_425491_.backText;
      if (signtext != null) {
         p_426809_.pushPose();
         this.translateSignText(p_426809_, p_423601_, this.getTextOffset());
         int i = getDarkColor(signtext);
         int j = 4 * p_425491_.textLineHeight / 2;
         FormattedCharSequence[] aformattedcharsequence = signtext.getRenderMessages(p_425491_.isTextFilteringEnabled, p_420901_ -> {
            List<FormattedCharSequence> list = this.font.split(p_420901_, p_425491_.maxTextLineWidth);
            return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
         });
         int k;
         boolean flag;
         int l;
         if (!signtext.hasGlowingText()) {
            k = i;
            flag = false;
            l = p_425491_.lightCoords;
         } else {
            k = signtext.getColor().getTextColor();
            flag = k == DyeColor.BLACK.getTextColor() || p_425491_.drawOutline;
            l = 15728880;
         }

         for (int i1 = 0; i1 < 4; i1++) {
            FormattedCharSequence formattedcharsequence = aformattedcharsequence[i1];
            float f = -this.font.width(formattedcharsequence) / 2;
            p_423348_.submitText(
               p_426809_, f, i1 * p_425491_.textLineHeight - j, formattedcharsequence, false, Font.DisplayMode.POLYGON_OFFSET, l, k, 0, flag ? i : 0
            );
         }

         p_426809_.popPose();
      }
   }

   private void translateSignText(PoseStack p_377496_, boolean p_376226_, Vec3 p_377669_) {
      if (!p_376226_) {
         p_377496_.mulPose(Axis.YP.rotationDegrees(180.0F));
      }

      float f = 0.015625F * this.getSignTextRenderScale();
      p_377496_.translate(p_377669_);
      p_377496_.scale(f, -f, f);
   }

   private static boolean isOutlineVisible(BlockPos p_376971_) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer localplayer = minecraft.player;
      if (localplayer != null && minecraft.options.getCameraType().isFirstPerson() && localplayer.isScoping()) {
         return true;
      }

      Entity entity = minecraft.getCameraEntity();
      return entity != null && entity.distanceToSqr(Vec3.atCenterOf(p_376971_)) < OUTLINE_RENDER_DISTANCE;
   }

   public static int getDarkColor(SignText p_376682_) {
      int i = p_376682_.getColor().getTextColor();
      return i == DyeColor.BLACK.getTextColor() && p_376682_.hasGlowingText() ? -988212 : ARGB.scaleRGB(i, 0.4F);
   }

   public SignRenderState createRenderState() {
      return new SignRenderState();
   }

   public void extractRenderState(
      SignBlockEntity p_423975_, SignRenderState p_428331_, float p_425097_, Vec3 p_431235_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_423679_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_423975_, p_428331_, p_425097_, p_431235_, p_423679_);
      p_428331_.maxTextLineWidth = p_423975_.getMaxTextLineWidth();
      p_428331_.textLineHeight = p_423975_.getTextLineHeight();
      p_428331_.frontText = p_423975_.getFrontText();
      p_428331_.backText = p_423975_.getBackText();
      p_428331_.isTextFilteringEnabled = Minecraft.getInstance().isTextFilteringEnabled();
      p_428331_.drawOutline = isOutlineVisible(p_423975_.getBlockPos());
   }
}
