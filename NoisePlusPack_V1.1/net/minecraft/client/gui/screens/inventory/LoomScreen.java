package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
   private static final Identifier BANNER_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner");
   private static final Identifier DYE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/dye");
   private static final Identifier PATTERN_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner_pattern");
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller_disabled");
   private static final Identifier PATTERN_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_selected");
   private static final Identifier PATTERN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_highlighted");
   private static final Identifier PATTERN_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern");
   private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/loom/error");
   private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/loom.png");
   private static final int PATTERN_COLUMNS = 4;
   private static final int PATTERN_ROWS = 4;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int PATTERN_IMAGE_SIZE = 14;
   private static final int SCROLLER_FULL_HEIGHT = 56;
   private static final int PATTERNS_X = 60;
   private static final int PATTERNS_Y = 13;
   private static final float BANNER_PATTERN_TEXTURE_SIZE = 64.0F;
   private static final float BANNER_PATTERN_WIDTH = 21.0F;
   private static final float BANNER_PATTERN_HEIGHT = 40.0F;
   private BannerFlagModel flag;
   private @Nullable BannerPatternLayers resultBannerPatterns;
   private ItemStack bannerStack = ItemStack.EMPTY;
   private ItemStack dyeStack = ItemStack.EMPTY;
   private ItemStack patternStack = ItemStack.EMPTY;
   private boolean displayPatterns;
   private boolean hasMaxPatterns;
   private float scrollOffs;
   private boolean scrolling;
   private int startRow;

   public LoomScreen(LoomMenu p_99075_, Inventory p_99076_, Component p_99077_) {
      super(p_99075_, p_99076_, p_99077_);
      p_99075_.registerUpdateListener(this::containerChanged);
      this.titleLabelY -= 2;
   }

   @Override
   protected void init() {
      super.init();
      ModelPart modelpart = this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
      this.flag = new BannerFlagModel(modelpart);
   }

   @Override
   public void render(GuiGraphics p_283513_, int p_282700_, int p_282637_, float p_281433_) {
      super.render(p_283513_, p_282700_, p_282637_, p_281433_);
      this.renderTooltip(p_283513_, p_282700_, p_282637_);
   }

   private int totalRowCount() {
      return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
   }

   @Override
   protected void renderBg(GuiGraphics p_282870_, float p_281777_, int p_283331_, int p_283087_) {
      int i = this.leftPos;
      int j = this.topPos;
      p_282870_.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
      Slot slot = this.menu.getBannerSlot();
      Slot slot1 = this.menu.getDyeSlot();
      Slot slot2 = this.menu.getPatternSlot();
      Slot slot3 = this.menu.getResultSlot();
      if (!slot.hasItem()) {
         p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_SPRITE, i + slot.x, j + slot.y, 16, 16);
      }

      if (!slot1.hasItem()) {
         p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, DYE_SLOT_SPRITE, i + slot1.x, j + slot1.y, 16, 16);
      }

      if (!slot2.hasItem()) {
         p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_SPRITE, i + slot2.x, j + slot2.y, 16, 16);
      }

      int k = (int)(41.0F * this.scrollOffs);
      Identifier identifier = this.displayPatterns ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      int l = i + 119;
      int i1 = j + 13 + k;
      p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, l, i1, 12, 15);
      if (p_283331_ >= l && p_283331_ < l + 12 && p_283087_ >= i1 && p_283087_ < i1 + 15) {
         p_282870_.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
      }

      if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
         DyeColor dyecolor = ((BannerItem)slot3.getItem().getItem()).getColor();
         int j1 = i + 141;
         int k1 = j + 8;
         p_282870_.submitBannerPatternRenderState(this.flag, dyecolor, this.resultBannerPatterns, j1, k1, j1 + 20, k1 + 40);
      } else if (this.hasMaxPatterns) {
         p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, i + slot3.x - 5, j + slot3.y - 5, 26, 26);
      }

      if (this.displayPatterns) {
         int j3 = i + 60;
         int k3 = j + 13;
         List<Holder<BannerPattern>> list = this.menu.getSelectablePatterns();

         label79:
         for (int l1 = 0; l1 < 4; l1++) {
            for (int i2 = 0; i2 < 4; i2++) {
               int j2 = l1 + this.startRow;
               int k2 = j2 * 4 + i2;
               if (k2 >= list.size()) {
                  break label79;
               }

               int l2 = j3 + i2 * 14;
               int i3 = k3 + l1 * 14;
               Holder<BannerPattern> holder = list.get(k2);
               boolean flag = p_283331_ >= l2 && p_283087_ >= i3 && p_283331_ < l2 + 14 && p_283087_ < i3 + 14;
               Identifier identifier1;
               if (k2 == this.menu.getSelectedBannerPatternIndex()) {
                  identifier1 = PATTERN_SELECTED_SPRITE;
               } else if (flag) {
                  identifier1 = PATTERN_HIGHLIGHTED_SPRITE;
                  DyeColor dyecolor1 = ((DyeItem)this.dyeStack.getItem()).getDyeColor();
                  p_282870_.setTooltipForNextFrame(Component.translatable(holder.value().translationKey() + "." + dyecolor1.getName()), p_283331_, p_283087_);
                  p_282870_.requestCursor(CursorTypes.POINTING_HAND);
               } else {
                  identifier1 = PATTERN_SPRITE;
               }

               p_282870_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier1, l2, i3, 14, 14);
               TextureAtlasSprite textureatlassprite = p_282870_.getSprite(Sheets.getBannerMaterial(holder));
               this.renderBannerOnButton(p_282870_, l2, i3, textureatlassprite);
            }
         }
      }

      Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
   }

   private void renderBannerOnButton(GuiGraphics p_410574_, int p_408841_, int p_407567_, TextureAtlasSprite p_409613_) {
      p_410574_.pose().pushMatrix();
      p_410574_.pose().translate(p_408841_ + 4, p_407567_ + 2);
      float f = p_409613_.getU0();
      float f1 = f + (p_409613_.getU1() - p_409613_.getU0()) * 21.0F / 64.0F;
      float f2 = p_409613_.getV1() - p_409613_.getV0();
      float f3 = p_409613_.getV0() + f2 / 64.0F;
      float f4 = f3 + f2 * 40.0F / 64.0F;
      int i = 5;
      int j = 10;
      p_410574_.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
      p_410574_.blit(p_409613_.atlasLocation(), 0, 0, 5, 10, f, f1, f3, f4);
      p_410574_.pose().popMatrix();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_422860_, boolean p_424197_) {
      if (this.displayPatterns) {
         int i = this.leftPos + 60;
         int j = this.topPos + 13;

         for (int k = 0; k < 4; k++) {
            for (int l = 0; l < 4; l++) {
               double d0 = p_422860_.x() - (i + l * 14);
               double d1 = p_422860_.y() - (j + k * 14);
               int i1 = k + this.startRow;
               int j1 = i1 * 4 + l;
               if (d0 >= 0.0 && d1 >= 0.0 && d0 < 14.0 && d1 < 14.0 && this.menu.clickMenuButton(this.minecraft.player, j1)) {
                  Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                  this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, j1);
                  return true;
               }
            }
         }

         i = this.leftPos + 119;
         j = this.topPos + 9;
         if (p_422860_.x() >= i && p_422860_.x() < i + 12 && p_422860_.y() >= j && p_422860_.y() < j + 56) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(p_422860_, p_424197_);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_422938_, double p_99087_, double p_99088_) {
      int i = this.totalRowCount() - 4;
      if (this.scrolling && this.displayPatterns && i > 0) {
         int j = this.topPos + 13;
         int k = j + 56;
         this.scrollOffs = ((float)p_422938_.y() - j - 7.5F) / (k - j - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startRow = Math.max((int)(this.scrollOffs * i + 0.5), 0);
         return true;
      } else {
         return super.mouseDragged(p_422938_, p_99087_, p_99088_);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_456806_) {
      this.scrolling = false;
      return super.mouseReleased(p_456806_);
   }

   @Override
   public boolean mouseScrolled(double p_99079_, double p_99080_, double p_99081_, double p_298992_) {
      if (super.mouseScrolled(p_99079_, p_99080_, p_99081_, p_298992_)) {
         return true;
      }

      int i = this.totalRowCount() - 4;
      if (this.displayPatterns && i > 0) {
         float f = (float)p_298992_ / i;
         this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
         this.startRow = Math.max((int)(this.scrollOffs * i + 0.5F), 0);
      }

      return true;
   }

   @Override
   protected boolean hasClickedOutside(double p_99093_, double p_99094_, int p_99095_, int p_99096_) {
      return p_99093_ < p_99095_ || p_99094_ < p_99096_ || p_99093_ >= p_99095_ + this.imageWidth || p_99094_ >= p_99096_ + this.imageHeight;
   }

   private void containerChanged() {
      ItemStack itemstack = this.menu.getResultSlot().getItem();
      if (itemstack.isEmpty()) {
         this.resultBannerPatterns = null;
      } else {
         this.resultBannerPatterns = itemstack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      }

      ItemStack itemstack1 = this.menu.getBannerSlot().getItem();
      ItemStack itemstack2 = this.menu.getDyeSlot().getItem();
      ItemStack itemstack3 = this.menu.getPatternSlot().getItem();
      BannerPatternLayers bannerpatternlayers = itemstack1.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      this.hasMaxPatterns = bannerpatternlayers.layers().size() >= 6;
      if (this.hasMaxPatterns) {
         this.resultBannerPatterns = null;
      }

      if (!ItemStack.matches(itemstack1, this.bannerStack)
         || !ItemStack.matches(itemstack2, this.dyeStack)
         || !ItemStack.matches(itemstack3, this.patternStack)) {
         this.displayPatterns = !itemstack1.isEmpty() && !itemstack2.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
      }

      if (this.startRow >= this.totalRowCount()) {
         this.startRow = 0;
         this.scrollOffs = 0.0F;
      }

      this.bannerStack = itemstack1.copy();
      this.dyeStack = itemstack2.copy();
      this.patternStack = itemstack3.copy();
   }
}
