package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MerchantScreen extends AbstractContainerScreen<MerchantMenu> {
   private static final Identifier OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/out_of_stock");
   private static final Identifier EXPERIENCE_BAR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
   private static final Identifier EXPERIENCE_BAR_CURRENT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
   private static final Identifier EXPERIENCE_BAR_RESULT_SPRITE = Identifier.withDefaultNamespace("container/villager/experience_bar_result");
   private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
   private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
   private static final Identifier TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
   private static final Identifier TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
   private static final Identifier DISCOUNT_STRIKETHRUOGH_SPRITE = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");
   private static final Identifier VILLAGER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/villager.png");
   private static final int TEXTURE_WIDTH = 512;
   private static final int TEXTURE_HEIGHT = 256;
   private static final int MERCHANT_MENU_PART_X = 99;
   private static final int PROGRESS_BAR_X = 136;
   private static final int PROGRESS_BAR_Y = 16;
   private static final int SELL_ITEM_1_X = 5;
   private static final int SELL_ITEM_2_X = 35;
   private static final int BUY_ITEM_X = 68;
   private static final int LABEL_Y = 6;
   private static final int NUMBER_OF_OFFER_BUTTONS = 7;
   private static final int TRADE_BUTTON_X = 5;
   private static final int TRADE_BUTTON_HEIGHT = 20;
   private static final int TRADE_BUTTON_WIDTH = 88;
   private static final int SCROLLER_HEIGHT = 27;
   private static final int SCROLLER_WIDTH = 6;
   private static final int SCROLL_BAR_HEIGHT = 139;
   private static final int SCROLL_BAR_TOP_POS_Y = 18;
   private static final int SCROLL_BAR_START_X = 94;
   private static final Component TRADES_LABEL = Component.translatable("merchant.trades");
   private static final Component DEPRECATED_TOOLTIP = Component.translatable("merchant.deprecated");
   private int shopItem;
   private final MerchantScreen.TradeOfferButton[] tradeOfferButtons = new MerchantScreen.TradeOfferButton[7];
   int scrollOff;
   private boolean isDragging;

   public MerchantScreen(MerchantMenu p_99123_, Inventory p_99124_, Component p_99125_) {
      super(p_99123_, p_99124_, p_99125_);
      this.imageWidth = 276;
      this.inventoryLabelX = 107;
   }

   private void postButtonClick() {
      this.menu.setSelectionHint(this.shopItem);
      this.menu.tryMoveItems(this.shopItem);
      this.minecraft.getConnection().send(new ServerboundSelectTradePacket(this.shopItem));
   }

   @Override
   protected void init() {
      super.init();
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      int k = j + 16 + 2;

      for (int l = 0; l < 7; l++) {
         this.tradeOfferButtons[l] = this.addRenderableWidget(new MerchantScreen.TradeOfferButton(i + 5, k, l, p_99174_ -> {
            if (p_99174_ instanceof MerchantScreen.TradeOfferButton) {
               this.shopItem = ((MerchantScreen.TradeOfferButton)p_99174_).getIndex() + this.scrollOff;
               this.postButtonClick();
            }
         }));
         k += 20;
      }
   }

   @Override
   protected void renderLabels(GuiGraphics p_283337_, int p_282009_, int p_283691_) {
      int i = this.menu.getTraderLevel();
      if (i > 0 && i <= 5 && this.menu.showProgressBar()) {
         Component component = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + i));
         int j = this.font.width(component);
         int k = 49 + this.imageWidth / 2 - j / 2;
         p_283337_.drawString(this.font, component, k, 6, -12566464, false);
      } else {
         p_283337_.drawString(this.font, this.title, 49 + this.imageWidth / 2 - this.font.width(this.title) / 2, 6, -12566464, false);
      }

      p_283337_.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
      int l = this.font.width(TRADES_LABEL);
      p_283337_.drawString(this.font, TRADES_LABEL, 5 - l / 2 + 48, 6, -12566464, false);
   }

   @Override
   protected void renderBg(GuiGraphics p_283072_, float p_281275_, int p_282312_, int p_282984_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      p_283072_.blit(RenderPipelines.GUI_TEXTURED, VILLAGER_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
      MerchantOffers merchantoffers = this.menu.getOffers();
      if (!merchantoffers.isEmpty()) {
         int k = this.shopItem;
         if (k < 0 || k >= merchantoffers.size()) {
            return;
         }

         MerchantOffer merchantoffer = merchantoffers.get(k);
         if (merchantoffer.isOutOfStock()) {
            p_283072_.blitSprite(RenderPipelines.GUI_TEXTURED, OUT_OF_STOCK_SPRITE, this.leftPos + 83 + 99, this.topPos + 35, 28, 21);
         }
      }
   }

   private void renderProgressBar(GuiGraphics p_281426_, int p_283008_, int p_283085_, MerchantOffer p_282094_) {
      int i = this.menu.getTraderLevel();
      int j = this.menu.getTraderXp();
      if (i < 5) {
         p_281426_.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, p_283008_ + 136, p_283085_ + 16, 102, 5);
         int k = VillagerData.getMinXpPerLevel(i);
         if (j >= k && VillagerData.canLevelUp(i)) {
            int l = 102;
            float f = 102.0F / (VillagerData.getMaxXpPerLevel(i) - k);
            int i1 = Math.min(Mth.floor(f * (j - k)), 102);
            p_281426_.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT_SPRITE, 102, 5, 0, 0, p_283008_ + 136, p_283085_ + 16, i1, 5);
            int j1 = this.menu.getFutureTraderXp();
            if (j1 > 0) {
               int k1 = Math.min(Mth.floor(j1 * f), 102 - i1);
               p_281426_.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_RESULT_SPRITE, 102, 5, i1, 0, p_283008_ + 136 + i1, p_283085_ + 16, k1, 5);
            }
         }
      }
   }

   private void renderScroller(GuiGraphics p_283030_, int p_283154_, int p_281664_, int p_459651_, int p_459554_, MerchantOffers p_282877_) {
      int i = p_282877_.size() + 1 - 7;
      if (i > 1) {
         int j = 139 - (27 + (i - 1) * 139 / i);
         int k = 1 + j / i + 139 / i;
         int l = 113;
         int i1 = Math.min(113, this.scrollOff * k);
         if (this.scrollOff == i - 1) {
            i1 = 113;
         }

         int j1 = p_283154_ + 94;
         int k1 = p_281664_ + 18 + i1;
         p_283030_.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, j1, k1, 6, 27);
         if (p_459651_ >= j1 && p_459651_ < p_283154_ + 94 + 6 && p_459554_ >= k1 && p_459554_ <= k1 + 27) {
            p_283030_.requestCursor(this.isDragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
         }
      } else {
         p_283030_.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, p_283154_ + 94, p_281664_ + 18, 6, 27);
      }
   }

   @Override
   public void renderContents(GuiGraphics p_283487_, int p_281994_, int p_282099_, float p_281815_) {
      super.renderContents(p_283487_, p_281994_, p_282099_, p_281815_);
      MerchantOffers merchantoffers = this.menu.getOffers();
      if (!merchantoffers.isEmpty()) {
         int i = (this.width - this.imageWidth) / 2;
         int j = (this.height - this.imageHeight) / 2;
         int k = j + 16 + 1;
         int l = i + 5 + 5;
         this.renderScroller(p_283487_, i, j, p_281994_, p_282099_, merchantoffers);
         int i1 = 0;

         for (MerchantOffer merchantoffer : merchantoffers) {
            if (!this.canScroll(merchantoffers.size()) || i1 >= this.scrollOff && i1 < 7 + this.scrollOff) {
               ItemStack itemstack = merchantoffer.getBaseCostA();
               ItemStack itemstack1 = merchantoffer.getCostA();
               ItemStack itemstack2 = merchantoffer.getCostB();
               ItemStack itemstack3 = merchantoffer.getResult();
               int j1 = k + 2;
               this.renderAndDecorateCostA(p_283487_, itemstack1, itemstack, l, j1);
               if (!itemstack2.isEmpty()) {
                  p_283487_.renderFakeItem(itemstack2, i + 5 + 35, j1);
                  p_283487_.renderItemDecorations(this.font, itemstack2, i + 5 + 35, j1);
               }

               this.renderButtonArrows(p_283487_, merchantoffer, i, j1);
               p_283487_.renderFakeItem(itemstack3, i + 5 + 68, j1);
               p_283487_.renderItemDecorations(this.font, itemstack3, i + 5 + 68, j1);
               k += 20;
               i1++;
            } else {
               i1++;
            }
         }

         int k1 = this.shopItem;
         MerchantOffer merchantoffer1 = merchantoffers.get(k1);
         if (this.menu.showProgressBar()) {
            this.renderProgressBar(p_283487_, i, j, merchantoffer1);
         }

         if (merchantoffer1.isOutOfStock() && this.isHovering(186, 35, 22, 21, p_281994_, p_282099_) && this.menu.canRestock()) {
            p_283487_.setTooltipForNextFrame(this.font, DEPRECATED_TOOLTIP, p_281994_, p_282099_);
         }

         for (MerchantScreen.TradeOfferButton merchantscreen$tradeofferbutton : this.tradeOfferButtons) {
            if (merchantscreen$tradeofferbutton.isHoveredOrFocused()) {
               merchantscreen$tradeofferbutton.renderToolTip(p_283487_, p_281994_, p_282099_);
            }

            merchantscreen$tradeofferbutton.visible = merchantscreen$tradeofferbutton.index < this.menu.getOffers().size();
         }
      }

      this.renderTooltip(p_283487_, p_281994_, p_282099_);
   }

   private void renderButtonArrows(GuiGraphics p_283020_, MerchantOffer p_281926_, int p_282752_, int p_282179_) {
      if (p_281926_.isOutOfStock()) {
         p_283020_.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_OUT_OF_STOCK_SPRITE, p_282752_ + 5 + 35 + 20, p_282179_ + 3, 10, 9);
      } else {
         p_283020_.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_SPRITE, p_282752_ + 5 + 35 + 20, p_282179_ + 3, 10, 9);
      }
   }

   private void renderAndDecorateCostA(GuiGraphics p_281357_, ItemStack p_283466_, ItemStack p_282046_, int p_282403_, int p_283601_) {
      p_281357_.renderFakeItem(p_283466_, p_282403_, p_283601_);
      if (p_282046_.getCount() == p_283466_.getCount()) {
         p_281357_.renderItemDecorations(this.font, p_283466_, p_282403_, p_283601_);
      } else {
         p_281357_.renderItemDecorations(this.font, p_282046_, p_282403_, p_283601_, p_282046_.getCount() == 1 ? "1" : null);
         p_281357_.renderItemDecorations(this.font, p_283466_, p_282403_ + 14, p_283601_, p_283466_.getCount() == 1 ? "1" : null);
         p_281357_.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHRUOGH_SPRITE, p_282403_ + 7, p_283601_ + 12, 9, 2);
      }
   }

   private boolean canScroll(int p_99141_) {
      return p_99141_ > 7;
   }

   @Override
   public boolean mouseScrolled(double p_99127_, double p_99128_, double p_99129_, double p_298933_) {
      if (super.mouseScrolled(p_99127_, p_99128_, p_99129_, p_298933_)) {
         return true;
      }

      int i = this.menu.getOffers().size();
      if (this.canScroll(i)) {
         int j = i - 7;
         this.scrollOff = Mth.clamp((int)(this.scrollOff - p_298933_), 0, j);
      }

      return true;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_426368_, double p_99135_, double p_99136_) {
      int i = this.menu.getOffers().size();
      if (this.isDragging) {
         int j = this.topPos + 18;
         int k = j + 139;
         int l = i - 7;
         float f = ((float)p_426368_.y() - j - 13.5F) / (k - j - 27.0F);
         f = f * l + 0.5F;
         this.scrollOff = Mth.clamp((int)f, 0, l);
         return true;
      } else {
         return super.mouseDragged(p_426368_, p_99135_, p_99136_);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_431292_, boolean p_431680_) {
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      if (this.canScroll(this.menu.getOffers().size())
         && p_431292_.x() > i + 94
         && p_431292_.x() < i + 94 + 6
         && p_431292_.y() > j + 18
         && p_431292_.y() <= j + 18 + 139 + 1) {
         this.isDragging = true;
      }

      return super.mouseClicked(p_431292_, p_431680_);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_452907_) {
      this.isDragging = false;
      return super.mouseReleased(p_452907_);
   }

   @OnlyIn(Dist.CLIENT)
   class TradeOfferButton extends Button.Plain {
      final int index;

      public TradeOfferButton(final int p_99205_, final int p_99206_, final int p_99207_, final Button.OnPress p_99208_) {
         super(p_99205_, p_99206_, 88, 20, CommonComponents.EMPTY, p_99208_, DEFAULT_NARRATION);
         this.index = p_99207_;
         this.visible = false;
      }

      public int getIndex() {
         return this.index;
      }

      public void renderToolTip(GuiGraphics p_281313_, int p_283342_, int p_283060_) {
         if (this.isHovered && MerchantScreen.this.menu.getOffers().size() > this.index + MerchantScreen.this.scrollOff) {
            if (p_283342_ < this.getX() + 20) {
               ItemStack itemstack = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostA();
               p_281313_.setTooltipForNextFrame(MerchantScreen.this.font, itemstack, p_283342_, p_283060_);
            } else if (p_283342_ < this.getX() + 50 && p_283342_ > this.getX() + 30) {
               ItemStack itemstack2 = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostB();
               if (!itemstack2.isEmpty()) {
                  p_281313_.setTooltipForNextFrame(MerchantScreen.this.font, itemstack2, p_283342_, p_283060_);
               }
            } else if (p_283342_ > this.getX() + 65) {
               ItemStack itemstack1 = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getResult();
               p_281313_.setTooltipForNextFrame(MerchantScreen.this.font, itemstack1, p_283342_, p_283060_);
            }
         }
      }
   }
}
