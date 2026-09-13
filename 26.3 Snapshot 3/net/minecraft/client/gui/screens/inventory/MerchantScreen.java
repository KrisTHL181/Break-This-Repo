package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
   private int scrollOff;
   private boolean isDragging;

   public MerchantScreen(final MerchantMenu menu, final Inventory inventory, final Component title) {
      super(menu, inventory, title, 276, 166);
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
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      int buttonY = yo + 16 + 2;

      for (int i = 0; i < 7; i++) {
         this.tradeOfferButtons[i] = this.addRenderableWidget(new MerchantScreen.TradeOfferButton(xo + 5, buttonY, i, button -> {
            if (button instanceof MerchantScreen.TradeOfferButton tradeOfferButton) {
               this.shopItem = tradeOfferButton.getIndex() + this.scrollOff;
               this.postButtonClick();
            }
         }));
         buttonY += 20;
      }
   }

   @Override
   protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
      int traderLevel = this.menu.getTraderLevel();
      if (traderLevel > 0 && traderLevel <= 5 && this.menu.showProgressBar()) {
         Component titleAndLevel = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + traderLevel));
         int totalWidth = this.font.width(titleAndLevel);
         int startX = 49 + this.imageWidth / 2 - totalWidth / 2;
         graphics.text(this.font, titleAndLevel, startX, 6, -12566464, false);
      } else {
         graphics.text(this.font, this.title, 49 + this.imageWidth / 2 - this.font.width(this.title) / 2, 6, -12566464, false);
      }

      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
      int textWidth = this.font.width(TRADES_LABEL);
      graphics.text(this.font, TRADES_LABEL, 5 - textWidth / 2 + 48, 6, -12566464, false);
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      graphics.blit(RenderPipelines.GUI_TEXTURED, VILLAGER_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 512, 256);
      MerchantOffers offers = this.menu.getOffers();
      if (!offers.isEmpty()) {
         int itemIndex = this.shopItem;
         if (itemIndex < 0 || itemIndex >= offers.size()) {
            return;
         }

         MerchantOffer offer = offers.get(itemIndex);
         if (offer.isOutOfStock()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OUT_OF_STOCK_SPRITE, this.leftPos + 83 + 99, this.topPos + 35, 28, 21);
         }
      }
   }

   private void extractProgressBar(final GuiGraphicsExtractor graphics, final int xo, final int yo, final MerchantOffer offer) {
      int traderLevel = this.menu.getTraderLevel();
      int traderXp = this.menu.getTraderXp();
      if (traderLevel < 5) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND_SPRITE, xo + 136, yo + 16, 102, 5);
         int minXp = VillagerData.getMinXpPerLevel(traderLevel);
         if (traderXp >= minXp && VillagerData.canLevelUp(traderLevel)) {
            int progressLength = 102;
            float multiplier = 102.0F / (VillagerData.getMaxXpPerLevel(traderLevel) - minXp);
            int w = Math.min(Mth.floor(multiplier * (traderXp - minXp)), 102);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT_SPRITE, 102, 5, 0, 0, xo + 136, yo + 16, w, 5);
            int futureXp = this.menu.getFutureTraderXp();
            if (futureXp > 0) {
               int futureXpWidth = Math.min(Mth.floor(futureXp * multiplier), 102 - w);
               graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_RESULT_SPRITE, 102, 5, w, 0, xo + 136 + w, yo + 16, futureXpWidth, 5);
            }
         }
      }
   }

   private void extractScroller(
      final GuiGraphicsExtractor graphics, final int xo, final int yo, final int mouseX, final int mouseY, final MerchantOffers offers
   ) {
      int steps = offers.size() + 1 - 7;
      if (steps > 1) {
         int leftOver = 139 - (27 + (steps - 1) * 139 / steps);
         int stepHeight = 1 + leftOver / steps + 139 / steps;
         int maxScrollerOff = 113;
         int scrollerYOff = Math.min(113, this.scrollOff * stepHeight);
         if (this.scrollOff == steps - 1) {
            scrollerYOff = 113;
         }

         int scrollerX = xo + 94;
         int scrollerY = yo + 18 + scrollerYOff;
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollerX, scrollerY, 6, 27);
         if (mouseX >= scrollerX && mouseX < xo + 94 + 6 && mouseY >= scrollerY && mouseY <= scrollerY + 27) {
            graphics.requestCursor(this.isDragging ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
         }
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, xo + 94, yo + 18, 6, 27);
      }
   }

   @Override
   public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractContents(graphics, mouseX, mouseY, a);
      MerchantOffers offers = this.menu.getOffers();
      if (!offers.isEmpty()) {
         int xo = (this.width - this.imageWidth) / 2;
         int yo = (this.height - this.imageHeight) / 2;
         int offerY = yo + 16 + 1;
         int sellItem1X = xo + 5 + 5;
         this.extractScroller(graphics, xo, yo, mouseX, mouseY, offers);
         int currentOfferIndex = 0;

         for (MerchantOffer offer : offers) {
            if (!this.canScroll(offers.size()) || currentOfferIndex >= this.scrollOff && currentOfferIndex < 7 + this.scrollOff) {
               ItemStack baseCostA = offer.getBaseCostA();
               ItemStack costA = offer.getCostA();
               ItemStack costB = offer.getCostB();
               ItemStack result = offer.getResult();
               int decorHeight = offerY + 2;
               this.extractAndDecorateCostA(graphics, costA, baseCostA, sellItem1X, decorHeight);
               if (!costB.isEmpty()) {
                  graphics.fakeItem(costB, xo + 5 + 35, decorHeight);
                  graphics.itemDecorations(this.font, costB, xo + 5 + 35, decorHeight);
               }

               this.extractButtonArrows(graphics, offer, xo, decorHeight);
               graphics.fakeItem(result, xo + 5 + 68, decorHeight);
               graphics.itemDecorations(this.font, result, xo + 5 + 68, decorHeight);
               offerY += 20;
               currentOfferIndex++;
            } else {
               currentOfferIndex++;
            }
         }

         int itemIndex = this.shopItem;
         MerchantOffer selectedOffer = offers.get(itemIndex);
         if (this.menu.showProgressBar()) {
            this.extractProgressBar(graphics, xo, yo, selectedOffer);
         }

         if (selectedOffer.isOutOfStock() && this.isHovering(186, 35, 22, 21, mouseX, mouseY) && this.menu.canRestock()) {
            graphics.setTooltipForNextFrame(this.font, DEPRECATED_TOOLTIP, mouseX, mouseY);
         }

         for (MerchantScreen.TradeOfferButton button : this.tradeOfferButtons) {
            if (button.isHoveredOrFocused()) {
               button.extractToolTip(graphics, mouseX, mouseY);
            }

            button.visible = button.index < this.menu.getOffers().size();
         }
      }
   }

   private void extractButtonArrows(final GuiGraphicsExtractor graphics, final MerchantOffer offer, final int xo, final int decorHeight) {
      if (offer.isOutOfStock()) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_OUT_OF_STOCK_SPRITE, xo + 5 + 35 + 20, decorHeight + 3, 10, 9);
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRADE_ARROW_SPRITE, xo + 5 + 35 + 20, decorHeight + 3, 10, 9);
      }
   }

   private void extractAndDecorateCostA(
      final GuiGraphicsExtractor graphics, final ItemStack costA, final ItemStack baseCostA, final int sellItem1X, final int decorHeight
   ) {
      graphics.fakeItem(costA, sellItem1X, decorHeight);
      if (baseCostA.getCount() == costA.getCount()) {
         graphics.itemDecorations(this.font, costA, sellItem1X, decorHeight);
      } else {
         graphics.itemDecorations(this.font, baseCostA, sellItem1X, decorHeight, baseCostA.getCount() == 1 ? "1" : null);
         graphics.itemDecorations(this.font, costA, sellItem1X + 14, decorHeight, costA.getCount() == 1 ? "1" : null);
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHRUOGH_SPRITE, sellItem1X + 7, decorHeight + 12, 9, 2);
      }
   }

   private boolean canScroll(final int numberOfOffers) {
      return numberOfOffers > 7;
   }

   @Override
   public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
      if (super.mouseScrolled(x, y, scrollX, scrollY)) {
         return true;
      }

      int numberOfOffers = this.menu.getOffers().size();
      if (this.canScroll(numberOfOffers)) {
         int maxScrollOff = numberOfOffers - 7;
         this.scrollOff = Mth.clamp((int)(this.scrollOff - scrollY), 0, maxScrollOff);
      }

      return true;
   }

   @Override
   public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
      int numberOfOffers = this.menu.getOffers().size();
      if (this.isDragging) {
         int fullScrollTopPos = this.topPos + 18;
         int fullScrollBottomPos = fullScrollTopPos + 139;
         int maxScrollOff = numberOfOffers - 7;
         float scrolling = ((float)event.y() - fullScrollTopPos - 13.5F) / (fullScrollBottomPos - fullScrollTopPos - 27.0F);
         scrolling = scrolling * maxScrollOff + 0.5F;
         this.scrollOff = Mth.clamp((int)scrolling, 0, maxScrollOff);
         return true;
      } else {
         return super.mouseDragged(event, dx, dy);
      }
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      int xo = (this.width - this.imageWidth) / 2;
      int yo = (this.height - this.imageHeight) / 2;
      if (this.canScroll(this.menu.getOffers().size())
         && event.x() > xo + 94
         && event.x() < xo + 94 + 6
         && event.y() > yo + 18
         && event.y() <= yo + 18 + 139 + 1) {
         this.isDragging = true;
      }

      return super.mouseClicked(event, doubleClick);
   }

   @Override
   public boolean mouseReleased(final MouseButtonEvent event) {
      this.isDragging = false;
      return super.mouseReleased(event);
   }

   private class TradeOfferButton extends Button.Plain {
      private final int index;

      public TradeOfferButton(final int x, final int y, final int index, final Button.OnPress onPress) {
         super(x, y, 88, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
         this.index = index;
         this.visible = false;
      }

      public int getIndex() {
         return this.index;
      }

      public void extractToolTip(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
         if (this.isHovered && MerchantScreen.this.menu.getOffers().size() > this.index + MerchantScreen.this.scrollOff) {
            if (xm < this.getX() + 20) {
               ItemStack item = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostA();
               graphics.setTooltipForNextFrame(MerchantScreen.this.font, item, xm, ym);
            } else if (xm < this.getX() + 50 && xm > this.getX() + 30) {
               ItemStack item = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getCostB();
               if (!item.isEmpty()) {
                  graphics.setTooltipForNextFrame(MerchantScreen.this.font, item, xm, ym);
               }
            } else if (xm > this.getX() + 65) {
               ItemStack item = MerchantScreen.this.menu.getOffers().get(this.index + MerchantScreen.this.scrollOff).getResult();
               graphics.setTooltipForNextFrame(MerchantScreen.this.font, item, xm, ym);
            }
         }
      }
   }
}
