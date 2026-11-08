package net.minecraft.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.ColoredRectangleRenderState;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.client.gui.render.state.TiledBlitRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;
import net.minecraft.client.gui.render.state.pip.GuiBookModelRenderState;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.render.state.pip.GuiProfilerChartRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSignRenderState;
import net.minecraft.client.gui.render.state.pip.GuiSkinRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.AtlasIds;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GuiGraphics {
   private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
   final Minecraft minecraft;
   private final Matrix3x2fStack pose;
   private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
   private final MaterialSet materials;
   private final TextureAtlas guiSprites;
   final GuiRenderState guiRenderState;
   private CursorType pendingCursor = CursorType.DEFAULT;
   final int mouseX;
   final int mouseY;
   private @Nullable Runnable deferredTooltip;
   @Nullable Style hoveredTextStyle;
   @Nullable Style clickableTextStyle;

   private GuiGraphics(Minecraft p_283406_, Matrix3x2fStack p_406378_, GuiRenderState p_409957_, int p_458689_, int p_460559_) {
      this.minecraft = p_283406_;
      this.pose = p_406378_;
      this.mouseX = p_458689_;
      this.mouseY = p_460559_;
      AtlasManager atlasmanager = p_283406_.getAtlasManager();
      this.materials = atlasmanager;
      this.guiSprites = atlasmanager.getAtlasOrThrow(AtlasIds.GUI);
      this.guiRenderState = p_409957_;
   }

   public GuiGraphics(Minecraft p_282144_, GuiRenderState p_406280_, int p_460485_, int p_451827_) {
      this(p_282144_, new Matrix3x2fStack(16), p_406280_, p_460485_, p_451827_);
   }

   public void requestCursor(CursorType p_431106_) {
      this.pendingCursor = p_431106_;
   }

   public void applyCursor(Window p_422780_) {
      p_422780_.selectCursor(this.pendingCursor);
   }

   public int guiWidth() {
      return this.minecraft.getWindow().getGuiScaledWidth();
   }

   public int guiHeight() {
      return this.minecraft.getWindow().getGuiScaledHeight();
   }

   public void nextStratum() {
      this.guiRenderState.nextStratum();
   }

   public void blurBeforeThisStratum() {
      this.guiRenderState.blurBeforeThisStratum();
   }

   public Matrix3x2fStack pose() {
      return this.pose;
   }

   public void hLine(int p_283318_, int p_281662_, int p_281346_, int p_281672_) {
      if (p_281662_ < p_283318_) {
         int i = p_283318_;
         p_283318_ = p_281662_;
         p_281662_ = i;
      }

      this.fill(p_283318_, p_281346_, p_281662_ + 1, p_281346_ + 1, p_281672_);
   }

   public void vLine(int p_282951_, int p_281591_, int p_281568_, int p_282718_) {
      if (p_281568_ < p_281591_) {
         int i = p_281591_;
         p_281591_ = p_281568_;
         p_281568_ = i;
      }

      this.fill(p_282951_, p_281591_ + 1, p_282951_ + 1, p_281568_, p_282718_);
   }

   public void enableScissor(int p_281479_, int p_282788_, int p_282924_, int p_282826_) {
      ScreenRectangle screenrectangle = new ScreenRectangle(p_281479_, p_282788_, p_282924_ - p_281479_, p_282826_ - p_282788_).transformAxisAligned(this.pose);
      this.scissorStack.push(screenrectangle);
   }

   public void disableScissor() {
      this.scissorStack.pop();
   }

   public boolean containsPointInScissor(int p_334767_, int p_334338_) {
      return this.scissorStack.containsPoint(p_334767_, p_334338_);
   }

   public void fill(int p_282988_, int p_282861_, int p_281278_, int p_281710_, int p_281470_) {
      this.fill(RenderPipelines.GUI, p_282988_, p_282861_, p_281278_, p_281710_, p_281470_);
   }

   public void fill(RenderPipeline p_407385_, int p_286738_, int p_286614_, int p_286741_, int p_286610_, int p_286560_) {
      if (p_286738_ < p_286741_) {
         int i = p_286738_;
         p_286738_ = p_286741_;
         p_286741_ = i;
      }

      if (p_286614_ < p_286610_) {
         int j = p_286614_;
         p_286614_ = p_286610_;
         p_286610_ = j;
      }

      this.submitColoredRectangle(p_407385_, TextureSetup.noTexture(), p_286738_, p_286614_, p_286741_, p_286610_, p_286560_, null);
   }

   public void fillGradient(int p_283290_, int p_283278_, int p_282670_, int p_281698_, int p_283374_, int p_283076_) {
      this.submitColoredRectangle(RenderPipelines.GUI, TextureSetup.noTexture(), p_283290_, p_283278_, p_282670_, p_281698_, p_283374_, p_283076_);
   }

   public void fill(RenderPipeline p_410604_, TextureSetup p_409448_, int p_281437_, int p_283660_, int p_282606_, int p_283413_) {
      this.submitColoredRectangle(p_410604_, p_409448_, p_281437_, p_283660_, p_282606_, p_283413_, -1, null);
   }

   private void submitColoredRectangle(
      RenderPipeline p_408958_, TextureSetup p_406846_, int p_406031_, int p_406797_, int p_406143_, int p_407910_, int p_406535_, @Nullable Integer p_406983_
   ) {
      this.guiRenderState
         .submitGuiElement(
            new ColoredRectangleRenderState(
               p_408958_,
               p_406846_,
               new Matrix3x2f(this.pose),
               p_406031_,
               p_406797_,
               p_406143_,
               p_407910_,
               p_406535_,
               p_406983_ != null ? p_406983_ : p_406535_,
               this.scissorStack.peek()
            )
         );
   }

   public void textHighlight(int p_410781_, int p_410804_, int p_410778_, int p_410796_, boolean p_459708_) {
      if (p_459708_) {
         this.fill(RenderPipelines.GUI_INVERT, p_410781_, p_410804_, p_410778_, p_410796_, -1);
      }

      this.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, p_410781_, p_410804_, p_410778_, p_410796_, -16776961);
   }

   public void drawCenteredString(Font p_282122_, String p_282898_, int p_281490_, int p_282853_, int p_281258_) {
      this.drawString(p_282122_, p_282898_, p_281490_ - p_282122_.width(p_282898_) / 2, p_282853_, p_281258_);
   }

   public void drawCenteredString(Font p_282901_, Component p_282456_, int p_283083_, int p_282276_, int p_281457_) {
      FormattedCharSequence formattedcharsequence = p_282456_.getVisualOrderText();
      this.drawString(p_282901_, formattedcharsequence, p_283083_ - p_282901_.width(formattedcharsequence) / 2, p_282276_, p_281457_);
   }

   public void drawCenteredString(Font p_282592_, FormattedCharSequence p_281854_, int p_281573_, int p_283511_, int p_282577_) {
      this.drawString(p_282592_, p_281854_, p_281573_ - p_282592_.width(p_281854_) / 2, p_283511_, p_282577_);
   }

   public void drawString(Font p_283343_, @Nullable String p_281896_, int p_283569_, int p_283418_, int p_281560_) {
      this.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_, true);
   }

   public void drawString(Font p_282003_, @Nullable String p_281403_, int p_282714_, int p_282041_, int p_281908_, boolean p_407210_) {
      if (p_281403_ != null) {
         this.drawString(p_282003_, Language.getInstance().getVisualOrder(FormattedText.of(p_281403_)), p_282714_, p_282041_, p_281908_, p_407210_);
      }
   }

   public void drawString(Font p_281547_, FormattedCharSequence p_410231_, int p_282857_, int p_281250_, int p_282195_) {
      this.drawString(p_281547_, p_410231_, p_282857_, p_281250_, p_282195_, true);
   }

   public void drawString(Font p_283019_, FormattedCharSequence p_283376_, int p_283379_, int p_283346_, int p_282119_, boolean p_407561_) {
      if (ARGB.alpha(p_282119_) != 0) {
         this.guiRenderState
            .submitText(
               new GuiTextRenderState(
                  p_283019_, p_283376_, new Matrix3x2f(this.pose), p_283379_, p_283346_, p_282119_, 0, p_407561_, false, this.scissorStack.peek()
               )
            );
      }
   }

   public void drawString(Font p_282636_, Component p_408884_, int p_281586_, int p_282816_, int p_281743_) {
      this.drawString(p_282636_, p_408884_, p_281586_, p_282816_, p_281743_, true);
   }

   public void drawString(Font p_281653_, Component p_283140_, int p_283102_, int p_282347_, int p_281429_, boolean p_410536_) {
      this.drawString(p_281653_, p_283140_.getVisualOrderText(), p_283102_, p_282347_, p_281429_, p_410536_);
   }

   public void drawWordWrap(Font p_281494_, FormattedText p_283463_, int p_282183_, int p_283250_, int p_282564_, int p_282629_) {
      this.drawWordWrap(p_281494_, p_283463_, p_282183_, p_283250_, p_282564_, p_282629_, true);
   }

   public void drawWordWrap(Font p_378519_, FormattedText p_378432_, int p_377858_, int p_376136_, int p_378596_, int p_378166_, boolean p_376508_) {
      for (FormattedCharSequence formattedcharsequence : p_378519_.split(p_378432_, p_378596_)) {
         this.drawString(p_378519_, formattedcharsequence, p_377858_, p_376136_, p_378166_, p_376508_);
         p_376136_ += 9;
      }
   }

   public void drawStringWithBackdrop(Font p_344926_, Component p_342324_, int p_342814_, int p_345075_, int p_343565_, int p_342787_) {
      int i = this.minecraft.options.getBackgroundColor(0.0F);
      if (i != 0) {
         int j = 2;
         this.fill(p_342814_ - 2, p_345075_ - 2, p_342814_ + p_343565_ + 2, p_345075_ + 9 + 2, ARGB.multiply(i, p_342787_));
      }

      this.drawString(p_344926_, p_342324_, p_342814_, p_345075_, p_342787_, true);
   }

   public void renderOutline(int p_454160_, int p_450739_, int p_456235_, int p_457970_, int p_455404_) {
      this.fill(p_454160_, p_450739_, p_454160_ + p_456235_, p_450739_ + 1, p_455404_);
      this.fill(p_454160_, p_450739_ + p_457970_ - 1, p_454160_ + p_456235_, p_450739_ + p_457970_, p_455404_);
      this.fill(p_454160_, p_450739_ + 1, p_454160_ + 1, p_450739_ + p_457970_ - 1, p_455404_);
      this.fill(p_454160_ + p_456235_ - 1, p_450739_ + 1, p_454160_ + p_456235_, p_450739_ + p_457970_ - 1, p_455404_);
   }

   public void blitSprite(RenderPipeline p_410383_, Identifier p_452857_, int p_409539_, int p_406738_, int p_406205_, int p_408966_) {
      this.blitSprite(p_410383_, p_452857_, p_409539_, p_406738_, p_406205_, p_408966_, -1);
   }

   public void blitSprite(RenderPipeline p_410126_, Identifier p_457814_, int p_297264_, int p_301178_, int p_297744_, int p_299331_, float p_410465_) {
      this.blitSprite(p_410126_, p_457814_, p_297264_, p_301178_, p_297744_, p_299331_, ARGB.white(p_410465_));
   }

   private static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite p_431673_) {
      return p_431673_.contents().getAdditionalMetadata(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT).scaling();
   }

   public void blitSprite(RenderPipeline p_406925_, Identifier p_456808_, int p_298718_, int p_298541_, int p_300996_, int p_298426_, int p_364958_) {
      TextureAtlasSprite textureatlassprite = this.guiSprites.getSprite(p_456808_);
      GuiSpriteScaling guispritescaling = getSpriteScaling(textureatlassprite);
      switch (guispritescaling) {
         case GuiSpriteScaling.Stretch guispritescaling$stretch:
            this.blitSprite(p_406925_, textureatlassprite, p_298718_, p_298541_, p_300996_, p_298426_, p_364958_);
            break;
         case GuiSpriteScaling.Tile guispritescaling$tile:
            this.blitTiledSprite(
               p_406925_,
               textureatlassprite,
               p_298718_,
               p_298541_,
               p_300996_,
               p_298426_,
               0,
               0,
               guispritescaling$tile.width(),
               guispritescaling$tile.height(),
               guispritescaling$tile.width(),
               guispritescaling$tile.height(),
               p_364958_
            );
            break;
         case GuiSpriteScaling.NineSlice guispritescaling$nineslice:
            this.blitNineSlicedSprite(p_406925_, textureatlassprite, guispritescaling$nineslice, p_298718_, p_298541_, p_300996_, p_298426_, p_364958_);
            break;
         default:
      }
   }

   public void blitSprite(
      RenderPipeline p_406152_,
      Identifier p_459460_,
      int p_300417_,
      int p_298256_,
      int p_299965_,
      int p_300008_,
      int p_407931_,
      int p_409421_,
      int p_408055_,
      int p_410507_
   ) {
      this.blitSprite(p_406152_, p_459460_, p_300417_, p_298256_, p_299965_, p_300008_, p_407931_, p_409421_, p_408055_, p_410507_, -1);
   }

   public void blitSprite(
      RenderPipeline p_408729_,
      Identifier p_451246_,
      int p_300402_,
      int p_300310_,
      int p_300994_,
      int p_297577_,
      int p_299466_,
      int p_301260_,
      int p_298369_,
      int p_300819_,
      int p_409529_
   ) {
      TextureAtlasSprite textureatlassprite = this.guiSprites.getSprite(p_451246_);
      GuiSpriteScaling guispritescaling = getSpriteScaling(textureatlassprite);
      if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
         this.blitSprite(p_408729_, textureatlassprite, p_300402_, p_300310_, p_300994_, p_297577_, p_299466_, p_301260_, p_298369_, p_300819_, p_409529_);
      } else {
         this.enableScissor(p_299466_, p_301260_, p_299466_ + p_298369_, p_301260_ + p_300819_);
         this.blitSprite(p_408729_, p_451246_, p_299466_ - p_300994_, p_301260_ - p_297577_, p_300402_, p_300310_, p_409529_);
         this.disableScissor();
      }
   }

   public void blitSprite(RenderPipeline p_408225_, TextureAtlasSprite p_363987_, int p_301241_, int p_298760_, int p_299400_, int p_299966_) {
      this.blitSprite(p_408225_, p_363987_, p_301241_, p_298760_, p_299400_, p_299966_, -1);
   }

   public void blitSprite(RenderPipeline p_409977_, TextureAtlasSprite p_407577_, int p_409085_, int p_406528_, int p_410018_, int p_409761_, int p_410681_) {
      if (p_410018_ != 0 && p_409761_ != 0) {
         this.innerBlit(
            p_409977_,
            p_407577_.atlasLocation(),
            p_409085_,
            p_409085_ + p_410018_,
            p_406528_,
            p_406528_ + p_409761_,
            p_407577_.getU0(),
            p_407577_.getU1(),
            p_407577_.getV0(),
            p_407577_.getV1(),
            p_410681_
         );
      }
   }

   private void blitSprite(
      RenderPipeline p_407498_,
      TextureAtlasSprite p_299484_,
      int p_297573_,
      int p_300435_,
      int p_299725_,
      int p_300673_,
      int p_301130_,
      int p_362878_,
      int p_362501_,
      int p_362210_,
      int p_363944_
   ) {
      if (p_362501_ != 0 && p_362210_ != 0) {
         this.innerBlit(
            p_407498_,
            p_299484_.atlasLocation(),
            p_301130_,
            p_301130_ + p_362501_,
            p_362878_,
            p_362878_ + p_362210_,
            p_299484_.getU((float)p_299725_ / p_297573_),
            p_299484_.getU((float)(p_299725_ + p_362501_) / p_297573_),
            p_299484_.getV((float)p_300673_ / p_300435_),
            p_299484_.getV((float)(p_300673_ + p_362210_) / p_300435_),
            p_363944_
         );
      }
   }

   private void blitNineSlicedSprite(
      RenderPipeline p_408954_,
      TextureAtlasSprite p_300154_,
      GuiSpriteScaling.NineSlice p_300599_,
      int p_297486_,
      int p_298301_,
      int p_299602_,
      int p_299587_,
      int p_299827_
   ) {
      GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = p_300599_.border();
      int i = Math.min(guispritescaling$nineslice$border.left(), p_299602_ / 2);
      int j = Math.min(guispritescaling$nineslice$border.right(), p_299602_ / 2);
      int k = Math.min(guispritescaling$nineslice$border.top(), p_299587_ / 2);
      int l = Math.min(guispritescaling$nineslice$border.bottom(), p_299587_ / 2);
      if (p_299602_ == p_300599_.width() && p_299587_ == p_300599_.height()) {
         this.blitSprite(p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), 0, 0, p_297486_, p_298301_, p_299602_, p_299587_, p_299827_);
      } else if (p_299587_ == p_300599_.height()) {
         this.blitSprite(p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), 0, 0, p_297486_, p_298301_, i, p_299587_, p_299827_);
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_ + i,
            p_298301_,
            p_299602_ - j - i,
            p_299587_,
            i,
            0,
            p_300599_.width() - j - i,
            p_300599_.height(),
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitSprite(
            p_408954_,
            p_300154_,
            p_300599_.width(),
            p_300599_.height(),
            p_300599_.width() - j,
            0,
            p_297486_ + p_299602_ - j,
            p_298301_,
            j,
            p_299587_,
            p_299827_
         );
      } else if (p_299602_ == p_300599_.width()) {
         this.blitSprite(p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), 0, 0, p_297486_, p_298301_, p_299602_, k, p_299827_);
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_,
            p_298301_ + k,
            p_299602_,
            p_299587_ - l - k,
            0,
            k,
            p_300599_.width(),
            p_300599_.height() - l - k,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitSprite(
            p_408954_,
            p_300154_,
            p_300599_.width(),
            p_300599_.height(),
            0,
            p_300599_.height() - l,
            p_297486_,
            p_298301_ + p_299587_ - l,
            p_299602_,
            l,
            p_299827_
         );
      } else {
         this.blitSprite(p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), 0, 0, p_297486_, p_298301_, i, k, p_299827_);
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_ + i,
            p_298301_,
            p_299602_ - j - i,
            k,
            i,
            0,
            p_300599_.width() - j - i,
            k,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitSprite(
            p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), p_300599_.width() - j, 0, p_297486_ + p_299602_ - j, p_298301_, j, k, p_299827_
         );
         this.blitSprite(
            p_408954_, p_300154_, p_300599_.width(), p_300599_.height(), 0, p_300599_.height() - l, p_297486_, p_298301_ + p_299587_ - l, i, l, p_299827_
         );
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_ + i,
            p_298301_ + p_299587_ - l,
            p_299602_ - j - i,
            l,
            i,
            p_300599_.height() - l,
            p_300599_.width() - j - i,
            l,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitSprite(
            p_408954_,
            p_300154_,
            p_300599_.width(),
            p_300599_.height(),
            p_300599_.width() - j,
            p_300599_.height() - l,
            p_297486_ + p_299602_ - j,
            p_298301_ + p_299587_ - l,
            j,
            l,
            p_299827_
         );
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_,
            p_298301_ + k,
            i,
            p_299587_ - l - k,
            0,
            k,
            i,
            p_300599_.height() - l - k,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_ + i,
            p_298301_ + k,
            p_299602_ - j - i,
            p_299587_ - l - k,
            i,
            k,
            p_300599_.width() - j - i,
            p_300599_.height() - l - k,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
         this.blitNineSliceInnerSegment(
            p_408954_,
            p_300599_,
            p_300154_,
            p_297486_ + p_299602_ - j,
            p_298301_ + k,
            j,
            p_299587_ - l - k,
            p_300599_.width() - j,
            k,
            j,
            p_300599_.height() - l - k,
            p_300599_.width(),
            p_300599_.height(),
            p_299827_
         );
      }
   }

   private void blitNineSliceInnerSegment(
      RenderPipeline p_409828_,
      GuiSpriteScaling.NineSlice p_361460_,
      TextureAtlasSprite p_364978_,
      int p_364957_,
      int p_367994_,
      int p_362572_,
      int p_366826_,
      int p_365488_,
      int p_366188_,
      int p_369698_,
      int p_362666_,
      int p_367341_,
      int p_362743_,
      int p_364128_
   ) {
      if (p_362572_ > 0 && p_366826_ > 0) {
         if (p_361460_.stretchInner()) {
            this.innerBlit(
               p_409828_,
               p_364978_.atlasLocation(),
               p_364957_,
               p_364957_ + p_362572_,
               p_367994_,
               p_367994_ + p_366826_,
               p_364978_.getU((float)p_365488_ / p_367341_),
               p_364978_.getU((float)(p_365488_ + p_369698_) / p_367341_),
               p_364978_.getV((float)p_366188_ / p_362743_),
               p_364978_.getV((float)(p_366188_ + p_362666_) / p_362743_),
               p_364128_
            );
         } else {
            this.blitTiledSprite(
               p_409828_, p_364978_, p_364957_, p_367994_, p_362572_, p_366826_, p_365488_, p_366188_, p_369698_, p_362666_, p_367341_, p_362743_, p_364128_
            );
         }
      }
   }

   private void blitTiledSprite(
      RenderPipeline p_406979_,
      TextureAtlasSprite p_298835_,
      int p_297456_,
      int p_300732_,
      int p_297241_,
      int p_300646_,
      int p_299561_,
      int p_298797_,
      int p_299557_,
      int p_297684_,
      int p_299756_,
      int p_297303_,
      int p_299619_
   ) {
      if (p_297241_ > 0 && p_300646_ > 0) {
         if (p_299557_ <= 0 || p_297684_ <= 0) {
            throw new IllegalArgumentException("Tile size must be positive, got " + p_299557_ + "x" + p_297684_);
         }

         AbstractTexture abstracttexture = this.minecraft.getTextureManager().getTexture(p_298835_.atlasLocation());
         GpuTextureView gputextureview = abstracttexture.getTextureView();
         this.submitTiledBlit(
            p_406979_,
            gputextureview,
            abstracttexture.getSampler(),
            p_299557_,
            p_297684_,
            p_297456_,
            p_300732_,
            p_297456_ + p_297241_,
            p_300732_ + p_300646_,
            p_298835_.getU((float)p_299561_ / p_299756_),
            p_298835_.getU((float)(p_299561_ + p_299557_) / p_299756_),
            p_298835_.getV((float)p_298797_ / p_297303_),
            p_298835_.getV((float)(p_298797_ + p_297684_) / p_297303_),
            p_299619_
         );
      }
   }

   public void blit(
      RenderPipeline p_408163_,
      Identifier p_460519_,
      int p_410313_,
      int p_408519_,
      float p_410485_,
      float p_409076_,
      int p_409235_,
      int p_406448_,
      int p_409555_,
      int p_409246_,
      int p_407351_
   ) {
      this.blit(p_408163_, p_460519_, p_410313_, p_408519_, p_410485_, p_409076_, p_409235_, p_406448_, p_409235_, p_406448_, p_409555_, p_409246_, p_407351_);
   }

   public void blit(
      RenderPipeline p_460208_,
      Identifier p_452026_,
      int p_282225_,
      int p_281487_,
      float p_365061_,
      float p_368643_,
      int p_281985_,
      int p_281329_,
      int p_458330_,
      int p_455802_
   ) {
      this.blit(p_460208_, p_452026_, p_282225_, p_281487_, p_365061_, p_368643_, p_281985_, p_281329_, p_281985_, p_281329_, p_458330_, p_455802_);
   }

   public void blit(
      RenderPipeline p_407549_,
      Identifier p_460352_,
      int p_282732_,
      int p_283541_,
      float p_282660_,
      float p_281522_,
      int p_281760_,
      int p_283298_,
      int p_283429_,
      int p_282193_,
      int p_281980_,
      int p_408133_
   ) {
      this.blit(p_407549_, p_460352_, p_282732_, p_283541_, p_282660_, p_281522_, p_281760_, p_283298_, p_283429_, p_282193_, p_281980_, p_408133_, -1);
   }

   public void blit(
      RenderPipeline p_407230_,
      Identifier p_452910_,
      int p_283574_,
      int p_283670_,
      float p_283029_,
      float p_283061_,
      int p_283545_,
      int p_282845_,
      int p_282558_,
      int p_282832_,
      int p_281851_,
      int p_366628_,
      int p_364363_
   ) {
      this.innerBlit(
         p_407230_,
         p_452910_,
         p_283574_,
         p_283574_ + p_283545_,
         p_283670_,
         p_283670_ + p_282845_,
         (p_283029_ + 0.0F) / p_281851_,
         (p_283029_ + p_282558_) / p_281851_,
         (p_283061_ + 0.0F) / p_366628_,
         (p_283061_ + p_282832_) / p_366628_,
         p_364363_
      );
   }

   public void blit(
      Identifier p_460932_, int p_281970_, int p_282111_, int p_283134_, int p_282778_, float p_367108_, float p_362374_, float p_450186_, float p_453603_
   ) {
      this.innerBlit(RenderPipelines.GUI_TEXTURED, p_460932_, p_281970_, p_283134_, p_282111_, p_282778_, p_367108_, p_362374_, p_450186_, p_453603_, -1);
   }

   private void innerBlit(
      RenderPipeline p_408871_,
      Identifier p_458466_,
      int p_283092_,
      int p_281930_,
      int p_282113_,
      int p_281388_,
      float p_281327_,
      float p_281676_,
      float p_283166_,
      float p_282630_,
      int p_283583_
   ) {
      AbstractTexture abstracttexture = this.minecraft.getTextureManager().getTexture(p_458466_);
      this.submitBlit(
         p_408871_,
         abstracttexture.getTextureView(),
         abstracttexture.getSampler(),
         p_283092_,
         p_282113_,
         p_281930_,
         p_281388_,
         p_281327_,
         p_281676_,
         p_283166_,
         p_282630_,
         p_283583_
      );
   }

   private void submitBlit(
      RenderPipeline p_406984_,
      GpuTextureView p_409367_,
      GpuSampler p_458499_,
      int p_407012_,
      int p_406315_,
      int p_405990_,
      int p_406362_,
      float p_410000_,
      float p_408961_,
      float p_405835_,
      float p_409756_,
      int p_408898_
   ) {
      this.guiRenderState
         .submitGuiElement(
            new BlitRenderState(
               p_406984_,
               TextureSetup.singleTexture(p_409367_, p_458499_),
               new Matrix3x2f(this.pose),
               p_407012_,
               p_406315_,
               p_405990_,
               p_406362_,
               p_410000_,
               p_408961_,
               p_405835_,
               p_409756_,
               p_408898_,
               this.scissorStack.peek()
            )
         );
   }

   private void submitTiledBlit(
      RenderPipeline p_427986_,
      GpuTextureView p_426152_,
      GpuSampler p_458424_,
      int p_423047_,
      int p_430570_,
      int p_424688_,
      int p_423470_,
      int p_429682_,
      int p_431724_,
      float p_427369_,
      float p_424659_,
      float p_425598_,
      float p_424528_,
      int p_428927_
   ) {
      this.guiRenderState
         .submitGuiElement(
            new TiledBlitRenderState(
               p_427986_,
               TextureSetup.singleTexture(p_426152_, p_458424_),
               new Matrix3x2f(this.pose),
               p_423047_,
               p_430570_,
               p_424688_,
               p_423470_,
               p_429682_,
               p_431724_,
               p_427369_,
               p_424659_,
               p_425598_,
               p_424528_,
               p_428927_,
               this.scissorStack.peek()
            )
         );
   }

   public void renderItem(ItemStack p_281978_, int p_282647_, int p_281944_) {
      this.renderItem(this.minecraft.player, this.minecraft.level, p_281978_, p_282647_, p_281944_, 0);
   }

   public void renderItem(ItemStack p_282262_, int p_283221_, int p_283496_, int p_283435_) {
      this.renderItem(this.minecraft.player, this.minecraft.level, p_282262_, p_283221_, p_283496_, p_283435_);
   }

   public void renderFakeItem(ItemStack p_281946_, int p_283299_, int p_283674_) {
      this.renderFakeItem(p_281946_, p_283299_, p_283674_, 0);
   }

   public void renderFakeItem(ItemStack p_309605_, int p_310104_, int p_309448_, int p_310674_) {
      this.renderItem(null, this.minecraft.level, p_309605_, p_310104_, p_309448_, p_310674_);
   }

   public void renderItem(LivingEntity p_282154_, ItemStack p_282777_, int p_282110_, int p_281371_, int p_283572_) {
      this.renderItem(p_282154_, p_282154_.level(), p_282777_, p_282110_, p_281371_, p_283572_);
   }

   private void renderItem(@Nullable LivingEntity p_283524_, @Nullable Level p_282461_, ItemStack p_283653_, int p_283141_, int p_282560_, int p_282425_) {
      if (!p_283653_.isEmpty()) {
         TrackingItemStackRenderState trackingitemstackrenderstate = new TrackingItemStackRenderState();
         this.minecraft
            .getItemModelResolver()
            .updateForTopItem(trackingitemstackrenderstate, p_283653_, ItemDisplayContext.GUI, p_282461_, p_283524_, p_282425_);

         try {
            this.guiRenderState
               .submitItem(
                  new GuiItemRenderState(
                     p_283653_.getItem().getName().toString(),
                     new Matrix3x2f(this.pose),
                     trackingitemstackrenderstate,
                     p_283141_,
                     p_282560_,
                     this.scissorStack.peek()
                  )
               );
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
            crashreportcategory.setDetail("Item Type", () -> String.valueOf(p_283653_.getItem()));
            crashreportcategory.setDetail("Item Components", () -> String.valueOf(p_283653_.getComponents()));
            crashreportcategory.setDetail("Item Foil", () -> String.valueOf(p_283653_.hasFoil()));
            throw new ReportedException(crashreport);
         }
      }
   }

   public void renderItemDecorations(Font p_281721_, ItemStack p_281514_, int p_282056_, int p_282683_) {
      this.renderItemDecorations(p_281721_, p_281514_, p_282056_, p_282683_, null);
   }

   public void renderItemDecorations(Font p_282005_, ItemStack p_283349_, int p_282641_, int p_282146_, @Nullable String p_282803_) {
      if (!p_283349_.isEmpty()) {
         this.pose.pushMatrix();
         this.renderItemBar(p_283349_, p_282641_, p_282146_);
         this.renderItemCooldown(p_283349_, p_282641_, p_282146_);
         this.renderItemCount(p_282005_, p_283349_, p_282641_, p_282146_, p_282803_);
         this.pose.popMatrix();
      }
   }

   public void setTooltipForNextFrame(Component p_408985_, int p_410587_, int p_406771_) {
      this.setTooltipForNextFrame(List.of(p_408985_.getVisualOrderText()), p_410587_, p_406771_);
   }

   public void setTooltipForNextFrame(List<FormattedCharSequence> p_407686_, int p_410475_, int p_407139_) {
      this.setTooltipForNextFrame(this.minecraft.font, p_407686_, DefaultTooltipPositioner.INSTANCE, p_410475_, p_407139_, false);
   }

   public void setTooltipForNextFrame(Font p_408152_, ItemStack p_410145_, int p_410585_, int p_408354_) {
      this.setTooltipForNextFrame(
         p_408152_,
         Screen.getTooltipFromItem(this.minecraft, p_410145_),
         p_410145_.getTooltipImage(),
         p_410585_,
         p_408354_,
         p_410145_.get(DataComponents.TOOLTIP_STYLE)
      );
   }

   public void setTooltipForNextFrame(Font p_407170_, List<Component> p_407123_, Optional<TooltipComponent> p_409349_, int p_406573_, int p_409911_) {
      this.setTooltipForNextFrame(p_407170_, p_407123_, p_409349_, p_406573_, p_409911_, null);
   }

   public void setTooltipForNextFrame(
      Font p_408450_, List<Component> p_405939_, Optional<TooltipComponent> p_456997_, int p_409519_, int p_410392_, @Nullable Identifier p_460731_
   ) {
      List<ClientTooltipComponent> list = p_405939_.stream()
         .map(Component::getVisualOrderText)
         .map(ClientTooltipComponent::create)
         .collect(Util.toMutableList());
      p_456997_.ifPresent(p_325321_ -> list.add(list.isEmpty() ? 0 : 1, ClientTooltipComponent.create(p_325321_)));
      this.setTooltipForNextFrameInternal(p_408450_, list, p_409519_, p_410392_, DefaultTooltipPositioner.INSTANCE, p_460731_, false);
   }

   public void setTooltipForNextFrame(Font p_409171_, Component p_410160_, int p_407143_, int p_406368_) {
      this.setTooltipForNextFrame(p_409171_, p_410160_, p_407143_, p_406368_, null);
   }

   public void setTooltipForNextFrame(Font p_407847_, Component p_450494_, int p_409369_, int p_407472_, @Nullable Identifier p_460242_) {
      this.setTooltipForNextFrame(p_407847_, List.of(p_450494_.getVisualOrderText()), p_409369_, p_407472_, p_460242_);
   }

   public void setComponentTooltipForNextFrame(Font p_406239_, List<Component> p_410275_, int p_410594_, int p_409343_) {
      this.setComponentTooltipForNextFrame(p_406239_, p_410275_, p_410594_, p_409343_, null);
   }

   public void setComponentTooltipForNextFrame(Font p_408650_, List<Component> p_407512_, int p_406019_, int p_408123_, @Nullable Identifier p_456486_) {
      this.setTooltipForNextFrameInternal(
         p_408650_,
         p_407512_.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList(),
         p_406019_,
         p_408123_,
         DefaultTooltipPositioner.INSTANCE,
         p_456486_,
         false
      );
   }

   public void setTooltipForNextFrame(Font p_410183_, List<? extends FormattedCharSequence> p_406154_, int p_406932_, int p_408529_) {
      this.setTooltipForNextFrame(p_410183_, p_406154_, p_406932_, p_408529_, null);
   }

   public void setTooltipForNextFrame(
      Font p_410103_, List<? extends FormattedCharSequence> p_458475_, int p_409194_, int p_407609_, @Nullable Identifier p_457286_
   ) {
      this.setTooltipForNextFrameInternal(
         p_410103_,
         p_458475_.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
         p_409194_,
         p_407609_,
         DefaultTooltipPositioner.INSTANCE,
         p_457286_,
         false
      );
   }

   public void setTooltipForNextFrame(
      Font p_406074_, List<FormattedCharSequence> p_406480_, ClientTooltipPositioner p_410090_, int p_408374_, int p_408941_, boolean p_405861_
   ) {
      this.setTooltipForNextFrameInternal(
         p_406074_, p_406480_.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), p_408374_, p_408941_, p_410090_, null, p_405861_
      );
   }

   private void setTooltipForNextFrameInternal(
      Font p_406605_,
      List<ClientTooltipComponent> p_409928_,
      int p_407794_,
      int p_408784_,
      ClientTooltipPositioner p_408798_,
      @Nullable Identifier p_451079_,
      boolean p_410618_
   ) {
      if (!p_409928_.isEmpty() && (this.deferredTooltip == null || p_410618_)) {
         this.deferredTooltip = () -> this.renderTooltip(p_406605_, p_409928_, p_407794_, p_408784_, p_408798_, p_451079_);
      }
   }

   public void renderTooltip(
      Font p_283128_, List<ClientTooltipComponent> p_282716_, int p_283678_, int p_281696_, ClientTooltipPositioner p_409451_, @Nullable Identifier p_459396_
   ) {
      int i = 0;
      int j = p_282716_.size() == 1 ? -2 : 0;

      for (ClientTooltipComponent clienttooltipcomponent : p_282716_) {
         int k = clienttooltipcomponent.getWidth(p_283128_);
         if (k > i) {
            i = k;
         }

         j += clienttooltipcomponent.getHeight(p_283128_);
      }

      int l1 = i;
      int i2 = j;
      Vector2ic vector2ic = p_409451_.positionTooltip(this.guiWidth(), this.guiHeight(), p_283678_, p_281696_, i, j);
      int l = vector2ic.x();
      int i1 = vector2ic.y();
      this.pose.pushMatrix();
      TooltipRenderUtil.renderTooltipBackground(this, l, i1, i, j, p_459396_);
      int j1 = i1;

      for (int k1 = 0; k1 < p_282716_.size(); k1++) {
         ClientTooltipComponent clienttooltipcomponent1 = p_282716_.get(k1);
         clienttooltipcomponent1.renderText(this, p_283128_, l, j1);
         j1 += clienttooltipcomponent1.getHeight(p_283128_) + (k1 == 0 ? 2 : 0);
      }

      j1 = i1;

      for (int j2 = 0; j2 < p_282716_.size(); j2++) {
         ClientTooltipComponent clienttooltipcomponent2 = p_282716_.get(j2);
         clienttooltipcomponent2.renderImage(p_283128_, l, j1, l1, i2, this);
         j1 += clienttooltipcomponent2.getHeight(p_283128_) + (j2 == 0 ? 2 : 0);
      }

      this.pose.popMatrix();
   }

   public void renderDeferredElements() {
      if (this.hoveredTextStyle != null) {
         this.renderComponentHoverEffect(this.minecraft.font, this.hoveredTextStyle, this.mouseX, this.mouseY);
      }

      if (this.clickableTextStyle != null && this.clickableTextStyle.getClickEvent() != null) {
         this.requestCursor(CursorTypes.POINTING_HAND);
      }

      if (this.deferredTooltip != null) {
         this.nextStratum();
         this.deferredTooltip.run();
         this.deferredTooltip = null;
      }
   }

   private void renderItemBar(ItemStack p_367359_, int p_362139_, int p_368464_) {
      if (p_367359_.isBarVisible()) {
         int i = p_362139_ + 2;
         int j = p_368464_ + 13;
         this.fill(RenderPipelines.GUI, i, j, i + 13, j + 2, -16777216);
         this.fill(RenderPipelines.GUI, i, j, i + p_367359_.getBarWidth(), j + 1, ARGB.opaque(p_367359_.getBarColor()));
      }
   }

   private void renderItemCount(Font p_363240_, ItemStack p_367163_, int p_369299_, int p_364530_, @Nullable String p_368187_) {
      if (p_367163_.getCount() != 1 || p_368187_ != null) {
         String s = p_368187_ == null ? String.valueOf(p_367163_.getCount()) : p_368187_;
         this.drawString(p_363240_, s, p_369299_ + 19 - 2 - p_363240_.width(s), p_364530_ + 6 + 3, -1, true);
      }
   }

   private void renderItemCooldown(ItemStack p_365241_, int p_364235_, int p_369346_) {
      LocalPlayer localplayer = this.minecraft.player;
      float f = localplayer == null
         ? 0.0F
         : localplayer.getCooldowns().getCooldownPercent(p_365241_, this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
      if (f > 0.0F) {
         int i = p_369346_ + Mth.floor(16.0F * (1.0F - f));
         int j = i + Mth.ceil(16.0F * f);
         this.fill(RenderPipelines.GUI, p_364235_, i, p_364235_ + 16, j, Integer.MAX_VALUE);
      }
   }

   public void renderComponentHoverEffect(Font p_282584_, @Nullable Style p_282156_, int p_283623_, int p_282114_) {
      if (p_282156_ != null && p_282156_.getHoverEvent() != null) {
         switch (p_282156_.getHoverEvent()) {
            case HoverEvent.ShowItem(ItemStack itemstack):
               this.setTooltipForNextFrame(p_282584_, itemstack, p_283623_, p_282114_);
               break;
            case HoverEvent.ShowEntity(HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo1):
               HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = hoverevent$entitytooltipinfo1;
               if (this.minecraft.options.advancedItemTooltips) {
                  this.setComponentTooltipForNextFrame(p_282584_, hoverevent$entitytooltipinfo.getTooltipLines(), p_283623_, p_282114_);
               }
               break;
            case HoverEvent.ShowText(Component component):
               this.setTooltipForNextFrame(p_282584_, p_282584_.split(component, Math.max(this.guiWidth() / 2, 200)), p_283623_, p_282114_);
               break;
            default:
         }
      }
   }

   public void submitMapRenderState(MapRenderState p_407157_) {
      Minecraft minecraft = Minecraft.getInstance();
      TextureManager texturemanager = minecraft.getTextureManager();
      AbstractTexture abstracttexture = texturemanager.getTexture(p_407157_.texture);
      this.submitBlit(RenderPipelines.GUI_TEXTURED, abstracttexture.getTextureView(), abstracttexture.getSampler(), 0, 0, 128, 128, 0.0F, 1.0F, 0.0F, 1.0F, -1);

      for (MapRenderState.MapDecorationRenderState maprenderstate$mapdecorationrenderstate : p_407157_.decorations) {
         if (maprenderstate$mapdecorationrenderstate.renderOnFrame) {
            this.pose.pushMatrix();
            this.pose.translate(maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F);
            this.pose.rotate((float) (Math.PI / 180.0) * maprenderstate$mapdecorationrenderstate.rot * 360.0F / 16.0F);
            this.pose.scale(4.0F, 4.0F);
            this.pose.translate(-0.125F, 0.125F);
            TextureAtlasSprite textureatlassprite = maprenderstate$mapdecorationrenderstate.atlasSprite;
            if (textureatlassprite != null) {
               AbstractTexture abstracttexture1 = texturemanager.getTexture(textureatlassprite.atlasLocation());
               this.submitBlit(
                  RenderPipelines.GUI_TEXTURED,
                  abstracttexture1.getTextureView(),
                  abstracttexture1.getSampler(),
                  -1,
                  -1,
                  1,
                  1,
                  textureatlassprite.getU0(),
                  textureatlassprite.getU1(),
                  textureatlassprite.getV1(),
                  textureatlassprite.getV0(),
                  -1
               );
            }

            this.pose.popMatrix();
            if (maprenderstate$mapdecorationrenderstate.name != null) {
               Font font = minecraft.font;
               float f = font.width(maprenderstate$mapdecorationrenderstate.name);
               float f1 = Mth.clamp(25.0F / f, 0.0F, 0.6666667F);
               this.pose.pushMatrix();
               this.pose
                  .translate(
                     maprenderstate$mapdecorationrenderstate.x / 2.0F + 64.0F - f * f1 / 2.0F, maprenderstate$mapdecorationrenderstate.y / 2.0F + 64.0F + 4.0F
                  );
               this.pose.scale(f1, f1);
               this.guiRenderState
                  .submitText(
                     new GuiTextRenderState(
                        font,
                        maprenderstate$mapdecorationrenderstate.name.getVisualOrderText(),
                        new Matrix3x2f(this.pose),
                        0,
                        0,
                        -1,
                        Integer.MIN_VALUE,
                        false,
                        false,
                        this.scissorStack.peek()
                     )
                  );
               this.pose.popMatrix();
            }
         }
      }
   }

   public void submitEntityRenderState(
      EntityRenderState p_407670_,
      float p_408616_,
      Vector3f p_409950_,
      Quaternionf p_408602_,
      @Nullable Quaternionf p_410448_,
      int p_410232_,
      int p_409702_,
      int p_409353_,
      int p_407717_
   ) {
      this.guiRenderState
         .submitPicturesInPictureState(
            new GuiEntityRenderState(
               p_407670_, p_409950_, p_408602_, p_410448_, p_410232_, p_409702_, p_409353_, p_407717_, p_408616_, this.scissorStack.peek()
            )
         );
   }

   public void submitSkinRenderState(
      PlayerModel p_459567_,
      Identifier p_450855_,
      float p_409604_,
      float p_408709_,
      float p_410281_,
      float p_409960_,
      int p_408581_,
      int p_409954_,
      int p_408176_,
      int p_407311_
   ) {
      this.guiRenderState
         .submitPicturesInPictureState(
            new GuiSkinRenderState(
               p_459567_, p_450855_, p_408709_, p_410281_, p_409960_, p_408581_, p_409954_, p_408176_, p_407311_, p_409604_, this.scissorStack.peek()
            )
         );
   }

   public void submitBookModelRenderState(
      BookModel p_456911_, Identifier p_457282_, float p_406155_, float p_407406_, float p_408835_, int p_409216_, int p_409705_, int p_410693_, int p_409112_
   ) {
      this.guiRenderState
         .submitPicturesInPictureState(
            new GuiBookModelRenderState(
               p_456911_, p_457282_, p_407406_, p_408835_, p_409216_, p_409705_, p_410693_, p_409112_, p_406155_, this.scissorStack.peek()
            )
         );
   }

   public void submitBannerPatternRenderState(
      BannerFlagModel p_454369_, DyeColor p_408541_, BannerPatternLayers p_406492_, int p_408397_, int p_406537_, int p_410642_, int p_408792_
   ) {
      this.guiRenderState
         .submitPicturesInPictureState(
            new GuiBannerResultRenderState(p_454369_, p_408541_, p_406492_, p_408397_, p_406537_, p_410642_, p_408792_, this.scissorStack.peek())
         );
   }

   public void submitSignRenderState(Model.Simple p_426217_, float p_410478_, WoodType p_409665_, int p_408918_, int p_408272_, int p_407816_, int p_407066_) {
      this.guiRenderState
         .submitPicturesInPictureState(
            new GuiSignRenderState(p_426217_, p_409665_, p_408918_, p_408272_, p_407816_, p_407066_, p_410478_, this.scissorStack.peek())
         );
   }

   public void submitProfilerChartRenderState(List<ResultField> p_408565_, int p_410147_, int p_406156_, int p_407365_, int p_408180_) {
      this.guiRenderState
         .submitPicturesInPictureState(new GuiProfilerChartRenderState(p_408565_, p_410147_, p_406156_, p_407365_, p_408180_, this.scissorStack.peek()));
   }

   public TextureAtlasSprite getSprite(Material p_430745_) {
      return this.materials.get(p_430745_);
   }

   public ActiveTextCollector textRendererForWidget(AbstractWidget p_455866_, GuiGraphics.HoveredTextEffects p_451167_) {
      return new GuiGraphics.RenderingTextCollector(this.createDefaultTextParameters(p_455866_.getAlpha()), p_451167_, null);
   }

   public ActiveTextCollector textRenderer() {
      return this.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_ONLY);
   }

   public ActiveTextCollector textRenderer(GuiGraphics.HoveredTextEffects p_460484_) {
      return this.textRenderer(p_460484_, null);
   }

   public ActiveTextCollector textRenderer(GuiGraphics.HoveredTextEffects p_453901_, @Nullable Consumer<Style> p_454043_) {
      return new GuiGraphics.RenderingTextCollector(this.createDefaultTextParameters(1.0F), p_453901_, p_454043_);
   }

   private ActiveTextCollector.Parameters createDefaultTextParameters(float p_456552_) {
      return new ActiveTextCollector.Parameters(new Matrix3x2f(this.pose), p_456552_, this.scissorStack.peek());
   }

   @OnlyIn(Dist.CLIENT)
   public enum HoveredTextEffects {
      NONE(false, false),
      TOOLTIP_ONLY(true, false),
      TOOLTIP_AND_CURSOR(true, true);

      public final boolean allowTooltip;
      public final boolean allowCursorChanges;

      HoveredTextEffects(final boolean p_460939_, final boolean p_460474_) {
         this.allowTooltip = p_460939_;
         this.allowCursorChanges = p_460474_;
      }

      public static GuiGraphics.HoveredTextEffects notClickable(boolean p_455838_) {
         return p_455838_ ? TOOLTIP_ONLY : NONE;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RenderingTextCollector implements ActiveTextCollector, Consumer<Style> {
      private ActiveTextCollector.Parameters defaultParameters;
      private final GuiGraphics.HoveredTextEffects hoveredTextEffects;
      private final @Nullable Consumer<Style> additionalConsumer;

      RenderingTextCollector(
         final ActiveTextCollector.Parameters p_455395_, final GuiGraphics.@Nullable HoveredTextEffects p_455217_, final Consumer<Style> p_456226_
      ) {
         this.defaultParameters = p_455395_;
         this.hoveredTextEffects = p_455217_;
         this.additionalConsumer = p_456226_;
      }

      @Override
      public ActiveTextCollector.Parameters defaultParameters() {
         return this.defaultParameters;
      }

      @Override
      public void defaultParameters(ActiveTextCollector.Parameters p_450937_) {
         this.defaultParameters = p_450937_;
      }

      public void accept(Style p_452568_) {
         if (this.hoveredTextEffects.allowTooltip && p_452568_.getHoverEvent() != null) {
            GuiGraphics.this.hoveredTextStyle = p_452568_;
         }

         if (this.hoveredTextEffects.allowCursorChanges && p_452568_.getClickEvent() != null) {
            GuiGraphics.this.clickableTextStyle = p_452568_;
         }

         if (this.additionalConsumer != null) {
            this.additionalConsumer.accept(p_452568_);
         }
      }

      @Override
      public void accept(TextAlignment p_454703_, int p_451939_, int p_455491_, ActiveTextCollector.Parameters p_460092_, FormattedCharSequence p_460885_) {
         boolean flag = this.hoveredTextEffects.allowCursorChanges || this.hoveredTextEffects.allowTooltip || this.additionalConsumer != null;
         int i = p_454703_.calculateLeft(p_451939_, GuiGraphics.this.minecraft.font, p_460885_);
         GuiTextRenderState guitextrenderstate = new GuiTextRenderState(
            GuiGraphics.this.minecraft.font, p_460885_, p_460092_.pose(), i, p_455491_, ARGB.white(p_460092_.opacity()), 0, true, flag, p_460092_.scissor()
         );
         if (ARGB.as8BitChannel(p_460092_.opacity()) != 0) {
            GuiGraphics.this.guiRenderState.submitText(guitextrenderstate);
         }

         if (flag) {
            ActiveTextCollector.findElementUnderCursor(guitextrenderstate, GuiGraphics.this.mouseX, GuiGraphics.this.mouseY, this);
         }
      }

      @Override
      public void acceptScrolling(
         Component p_459250_, int p_459751_, int p_460164_, int p_450879_, int p_456421_, int p_455879_, ActiveTextCollector.Parameters p_450787_
      ) {
         int i = GuiGraphics.this.minecraft.font.width(p_459250_);
         int j = 9;
         this.defaultScrollingHelper(p_459250_, p_459751_, p_460164_, p_450879_, p_456421_, p_455879_, i, j, p_450787_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class ScissorStack {
      private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

      public ScreenRectangle push(ScreenRectangle p_281812_) {
         ScreenRectangle screenrectangle = this.stack.peekLast();
         if (screenrectangle != null) {
            ScreenRectangle screenrectangle1 = Objects.requireNonNullElse(p_281812_.intersection(screenrectangle), ScreenRectangle.empty());
            this.stack.addLast(screenrectangle1);
            return screenrectangle1;
         } else {
            this.stack.addLast(p_281812_);
            return p_281812_;
         }
      }

      public @Nullable ScreenRectangle pop() {
         if (this.stack.isEmpty()) {
            throw new IllegalStateException("Scissor stack underflow");
         }

         this.stack.removeLast();
         return this.stack.peekLast();
      }

      public @Nullable ScreenRectangle peek() {
         return this.stack.peekLast();
      }

      public boolean containsPoint(int p_329411_, int p_333404_) {
         return this.stack.isEmpty() ? true : this.stack.peek().containsPoint(p_329411_, p_333404_);
      }
   }
}
