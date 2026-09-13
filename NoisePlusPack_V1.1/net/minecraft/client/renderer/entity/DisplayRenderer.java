package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S, ST extends DisplayEntityRenderState> extends EntityRenderer<T, ST> {
   private final EntityRenderDispatcher entityRenderDispatcher;

   protected DisplayRenderer(EntityRendererProvider.Context p_270168_) {
      super(p_270168_);
      this.entityRenderDispatcher = p_270168_.getEntityRenderDispatcher();
   }

   protected AABB getBoundingBoxForCulling(T p_368254_) {
      return p_368254_.getBoundingBoxForCulling();
   }

   protected boolean affectedByCulling(T p_365810_) {
      return p_365810_.affectedByCulling();
   }

   private static int getBrightnessOverride(Display p_365446_) {
      Display.RenderState display$renderstate = p_365446_.renderState();
      return display$renderstate != null ? display$renderstate.brightnessOverride() : -1;
   }

   protected int getSkyLightLevel(T p_367797_, BlockPos p_364805_) {
      int i = getBrightnessOverride(p_367797_);
      return i != -1 ? LightTexture.sky(i) : super.getSkyLightLevel(p_367797_, p_364805_);
   }

   protected int getBlockLightLevel(T p_362888_, BlockPos p_365686_) {
      int i = getBrightnessOverride(p_362888_);
      return i != -1 ? LightTexture.block(i) : super.getBlockLightLevel(p_362888_, p_365686_);
   }

   protected float getShadowRadius(ST p_376159_) {
      Display.RenderState display$renderstate = p_376159_.renderState;
      return display$renderstate == null ? 0.0F : display$renderstate.shadowRadius().get(p_376159_.interpolationProgress);
   }

   protected float getShadowStrength(ST p_377182_) {
      Display.RenderState display$renderstate = p_377182_.renderState;
      return display$renderstate == null ? 0.0F : display$renderstate.shadowStrength().get(p_377182_.interpolationProgress);
   }

   public void submit(ST p_427607_, PoseStack p_431293_, SubmitNodeCollector p_426541_, CameraRenderState p_428304_) {
      Display.RenderState display$renderstate = p_427607_.renderState;
      if (display$renderstate != null && p_427607_.hasSubState()) {
         float f = p_427607_.interpolationProgress;
         super.submit(p_427607_, p_431293_, p_426541_, p_428304_);
         p_431293_.pushPose();
         p_431293_.mulPose(this.calculateOrientation(display$renderstate, p_427607_, new Quaternionf()));
         Transformation transformation = display$renderstate.transformation().get(f);
         p_431293_.mulPose(transformation.getMatrix());
         this.submitInner(p_427607_, p_431293_, p_426541_, p_427607_.lightCoords, f);
         p_431293_.popPose();
      }
   }

   private Quaternionf calculateOrientation(Display.RenderState p_277846_, ST p_361564_, Quaternionf p_298476_) {
      return switch (p_277846_.billboardConstraints()) {
         case FIXED -> p_298476_.rotationYXZ((float) (-Math.PI / 180.0) * p_361564_.entityYRot, (float) (Math.PI / 180.0) * p_361564_.entityXRot, 0.0F);
         case HORIZONTAL -> p_298476_.rotationYXZ(
            (float) (-Math.PI / 180.0) * p_361564_.entityYRot, (float) (Math.PI / 180.0) * transformXRot(p_361564_.cameraXRot), 0.0F
         );
         case VERTICAL -> p_298476_.rotationYXZ(
            (float) (-Math.PI / 180.0) * transformYRot(p_361564_.cameraYRot), (float) (Math.PI / 180.0) * p_361564_.entityXRot, 0.0F
         );
         case CENTER -> p_298476_.rotationYXZ(
            (float) (-Math.PI / 180.0) * transformYRot(p_361564_.cameraYRot), (float) (Math.PI / 180.0) * transformXRot(p_361564_.cameraXRot), 0.0F
         );
      };
   }

   private static float transformYRot(float p_427882_) {
      return p_427882_ - 180.0F;
   }

   private static float transformXRot(float p_430055_) {
      return -p_430055_;
   }

   private static <T extends Display> float entityYRot(T p_297849_, float p_297686_) {
      return p_297849_.getYRot(p_297686_);
   }

   private static <T extends Display> float entityXRot(T p_298651_, float p_297691_) {
      return p_298651_.getXRot(p_297691_);
   }

   protected abstract void submitInner(ST var1, PoseStack var2, SubmitNodeCollector var3, int var4, float var5);

   public void extractRenderState(T p_364120_, ST p_362498_, float p_362522_) {
      super.extractRenderState(p_364120_, p_362498_, p_362522_);
      p_362498_.renderState = p_364120_.renderState();
      p_362498_.interpolationProgress = p_364120_.calculateInterpolationProgress(p_362522_);
      p_362498_.entityYRot = entityYRot(p_364120_, p_362522_);
      p_362498_.entityXRot = entityXRot(p_364120_, p_362522_);
      Camera camera = this.entityRenderDispatcher.camera;
      p_362498_.cameraXRot = camera.xRot();
      p_362498_.cameraYRot = camera.yRot();
   }

   @OnlyIn(Dist.CLIENT)
   public static class BlockDisplayRenderer extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState, BlockDisplayEntityRenderState> {
      protected BlockDisplayRenderer(EntityRendererProvider.Context p_270283_) {
         super(p_270283_);
      }

      public BlockDisplayEntityRenderState createRenderState() {
         return new BlockDisplayEntityRenderState();
      }

      public void extractRenderState(Display.BlockDisplay p_367120_, BlockDisplayEntityRenderState p_364696_, float p_367582_) {
         super.extractRenderState(p_367120_, p_364696_, p_367582_);
         p_364696_.blockRenderState = p_367120_.blockRenderState();
      }

      public void submitInner(BlockDisplayEntityRenderState p_422489_, PoseStack p_423226_, SubmitNodeCollector p_430190_, int p_430709_, float p_426688_) {
         p_430190_.submitBlock(p_423226_, p_422489_.blockRenderState.blockState(), p_430709_, OverlayTexture.NO_OVERLAY, p_422489_.outlineColor);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState, ItemDisplayEntityRenderState> {
      private final ItemModelResolver itemModelResolver;

      protected ItemDisplayRenderer(EntityRendererProvider.Context p_270110_) {
         super(p_270110_);
         this.itemModelResolver = p_270110_.getItemModelResolver();
      }

      public ItemDisplayEntityRenderState createRenderState() {
         return new ItemDisplayEntityRenderState();
      }

      public void extractRenderState(Display.ItemDisplay p_368800_, ItemDisplayEntityRenderState p_363947_, float p_365503_) {
         super.extractRenderState(p_368800_, p_363947_, p_365503_);
         Display.ItemDisplay.ItemRenderState display$itemdisplay$itemrenderstate = p_368800_.itemRenderState();
         if (display$itemdisplay$itemrenderstate != null) {
            this.itemModelResolver
               .updateForNonLiving(
                  p_363947_.item, display$itemdisplay$itemrenderstate.itemStack(), display$itemdisplay$itemrenderstate.itemTransform(), p_368800_
               );
         } else {
            p_363947_.item.clear();
         }
      }

      public void submitInner(ItemDisplayEntityRenderState p_424585_, PoseStack p_422882_, SubmitNodeCollector p_427780_, int p_427985_, float p_431391_) {
         if (!p_424585_.item.isEmpty()) {
            p_422882_.mulPose(Axis.YP.rotation((float) Math.PI));
            p_424585_.item.submit(p_422882_, p_427780_, p_427985_, OverlayTexture.NO_OVERLAY, p_424585_.outlineColor);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {
      private final Font font;

      protected TextDisplayRenderer(EntityRendererProvider.Context p_271012_) {
         super(p_271012_);
         this.font = p_271012_.getFont();
      }

      public TextDisplayEntityRenderState createRenderState() {
         return new TextDisplayEntityRenderState();
      }

      public void extractRenderState(Display.TextDisplay p_365496_, TextDisplayEntityRenderState p_366254_, float p_368471_) {
         super.extractRenderState(p_365496_, p_366254_, p_368471_);
         p_366254_.textRenderState = p_365496_.textRenderState();
         p_366254_.cachedInfo = p_365496_.cacheDisplay(this::splitLines);
      }

      private Display.TextDisplay.CachedInfo splitLines(Component p_270823_, int p_270893_) {
         List<FormattedCharSequence> list = this.font.split(p_270823_, p_270893_);
         List<Display.TextDisplay.CachedLine> list1 = new ArrayList<>(list.size());
         int i = 0;

         for (FormattedCharSequence formattedcharsequence : list) {
            int j = this.font.width(formattedcharsequence);
            i = Math.max(i, j);
            list1.add(new Display.TextDisplay.CachedLine(formattedcharsequence, j));
         }

         return new Display.TextDisplay.CachedInfo(list1, i);
      }

      public void submitInner(TextDisplayEntityRenderState p_423612_, PoseStack p_427601_, SubmitNodeCollector p_422866_, int p_430234_, float p_428401_) {
         Display.TextDisplay.TextRenderState display$textdisplay$textrenderstate = p_423612_.textRenderState;
         byte b0 = display$textdisplay$textrenderstate.flags();
         boolean flag = (b0 & 2) != 0;
         boolean flag1 = (b0 & 4) != 0;
         boolean flag2 = (b0 & 1) != 0;
         Display.TextDisplay.Align display$textdisplay$align = Display.TextDisplay.getAlign(b0);
         byte b1 = (byte)display$textdisplay$textrenderstate.textOpacity().get(p_428401_);
         int i;
         if (flag1) {
            float f = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            i = (int)(f * 255.0F) << 24;
         } else {
            i = display$textdisplay$textrenderstate.backgroundColor().get(p_428401_);
         }

         float f2 = 0.0F;
         Matrix4f matrix4f = p_427601_.last().pose();
         matrix4f.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
         matrix4f.scale(-0.025F, -0.025F, -0.025F);
         Display.TextDisplay.CachedInfo display$textdisplay$cachedinfo = p_423612_.cachedInfo;
         int j = 1;
         int k = 10;
         int l = display$textdisplay$cachedinfo.width();
         int i1 = display$textdisplay$cachedinfo.lines().size() * k - 1;
         matrix4f.translate(1.0F - l / 2.0F, -i1, 0.0F);
         if (i != 0) {
            p_422866_.submitCustomGeometry(p_427601_, flag ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(), (p_428981_, p_430758_) -> {
               p_430758_.addVertex(p_428981_, -1.0F, -1.0F, 0.0F).setColor(i).setLight(p_430234_);
               p_430758_.addVertex(p_428981_, -1.0F, i1, 0.0F).setColor(i).setLight(p_430234_);
               p_430758_.addVertex(p_428981_, l, i1, 0.0F).setColor(i).setLight(p_430234_);
               p_430758_.addVertex(p_428981_, l, -1.0F, 0.0F).setColor(i).setLight(p_430234_);
            });
         }

         OrderedSubmitNodeCollector orderedsubmitnodecollector = p_422866_.order(i != 0 ? 1 : 0);

         for (Display.TextDisplay.CachedLine display$textdisplay$cachedline : display$textdisplay$cachedinfo.lines()) {
            float f1 = switch (display$textdisplay$align) {
               case LEFT -> 0.0F;
               case RIGHT -> l - display$textdisplay$cachedline.width();
               case CENTER -> l / 2.0F - display$textdisplay$cachedline.width() / 2.0F;
            };
            orderedsubmitnodecollector.submitText(
               p_427601_,
               f1,
               f2,
               display$textdisplay$cachedline.contents(),
               flag2,
               flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
               p_430234_,
               b1 << 24 | 16777215,
               0,
               0
            );
            f2 += k;
         }
      }
   }
}
