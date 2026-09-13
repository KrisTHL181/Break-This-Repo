package net.minecraft.client.gui.screens.packs;

import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TransferableSelectionList extends ObjectSelectionList<TransferableSelectionList.Entry> {
   static final Identifier SELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/select_highlighted");
   static final Identifier SELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/select");
   static final Identifier UNSELECT_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect_highlighted");
   static final Identifier UNSELECT_SPRITE = Identifier.withDefaultNamespace("transferable_list/unselect");
   static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up_highlighted");
   static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_up");
   static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down_highlighted");
   static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("transferable_list/move_down");
   static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible");
   static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
   private static final int ENTRY_PADDING = 2;
   private final Component title;
   final PackSelectionScreen screen;

   public TransferableSelectionList(Minecraft p_265029_, PackSelectionScreen p_265777_, int p_265774_, int p_265153_, Component p_265124_) {
      super(p_265029_, p_265774_, p_265153_, 33, 36);
      this.screen = p_265777_;
      this.title = p_265124_;
      this.centerListVertically = false;
   }

   @Override
   public int getRowWidth() {
      return this.width - 4;
   }

   @Override
   protected int scrollBarX() {
      return this.getRight() - 6;
   }

   @Override
   public boolean keyPressed(KeyEvent p_425615_) {
      return this.getSelected() != null ? this.getSelected().keyPressed(p_425615_) : super.keyPressed(p_425615_);
   }

   public void updateList(Stream<PackSelectionModel.Entry> p_429245_, PackSelectionModel.@Nullable EntryBase p_430177_) {
      this.clearEntries();
      Component component = Component.empty().append(this.title).withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);
      this.addEntry(new TransferableSelectionList.HeaderEntry(this.minecraft.font, component), 13);
      this.setSelected(null);
      p_429245_.forEach(p_430444_ -> {
         TransferableSelectionList.PackEntry transferableselectionlist$packentry = new TransferableSelectionList.PackEntry(this.minecraft, this, p_430444_);
         this.addEntry(transferableselectionlist$packentry);
         if (p_430177_ != null && p_430177_.getId().equals(p_430444_.getId())) {
            this.screen.setFocused(this);
            this.setFocused(transferableselectionlist$packentry);
         }
      });
      this.refreshScrollAmount();
   }

   @OnlyIn(Dist.CLIENT)
   public abstract class Entry extends ObjectSelectionList.Entry<TransferableSelectionList.Entry> {
      @Override
      public int getWidth() {
         return super.getWidth() - (TransferableSelectionList.this.scrollbarVisible() ? 6 : 0);
      }

      public abstract String getPackId();
   }

   @OnlyIn(Dist.CLIENT)
   public class HeaderEntry extends TransferableSelectionList.Entry {
      private final Font font;
      private final Component text;

      public HeaderEntry(final Font p_427652_, final Component p_427819_) {
         this.font = p_427652_;
         this.text = p_427819_;
      }

      @Override
      public void renderContent(GuiGraphics p_431062_, int p_422850_, int p_424561_, boolean p_429431_, float p_424656_) {
         p_431062_.drawCenteredString(this.font, this.text, this.getX() + this.getWidth() / 2, this.getContentYMiddle() - 4, -1);
      }

      @Override
      public Component getNarration() {
         return this.text;
      }

      @Override
      public String getPackId() {
         return "";
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class PackEntry extends TransferableSelectionList.Entry implements SelectableEntry {
      private static final int MAX_DESCRIPTION_WIDTH_PIXELS = 157;
      public static final int ICON_SIZE = 32;
      private final TransferableSelectionList parent;
      protected final Minecraft minecraft;
      private final PackSelectionModel.Entry pack;
      private final StringWidget nameWidget;
      private final MultiLineTextWidget descriptionWidget;

      public PackEntry(final Minecraft p_265717_, final TransferableSelectionList p_426565_, final PackSelectionModel.Entry p_265360_) {
         this.minecraft = p_265717_;
         this.pack = p_265360_;
         this.parent = p_426565_;
         this.nameWidget = new StringWidget(p_265360_.getTitle(), p_265717_.font);
         this.descriptionWidget = new MultiLineTextWidget(
            ComponentUtils.mergeStyles(p_265360_.getExtendedDescription(), Style.EMPTY.withColor(-8355712)), p_265717_.font
         );
         this.descriptionWidget.setMaxRows(2);
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.pack.getTitle());
      }

      @Override
      public void renderContent(GuiGraphics p_425257_, int p_428146_, int p_422806_, boolean p_426963_, float p_426560_) {
         PackCompatibility packcompatibility = this.pack.getCompatibility();
         if (!packcompatibility.isCompatible()) {
            int i = this.getContentX() - 1;
            int j = this.getContentY() - 1;
            int k = this.getContentRight() + 1;
            int l = this.getContentBottom() + 1;
            p_425257_.fill(i, j, k, l, -8978432);
         }

         p_425257_.blit(RenderPipelines.GUI_TEXTURED, this.pack.getIconTexture(), this.getContentX(), this.getContentY(), 0.0F, 0.0F, 32, 32, 32, 32);
         if (!this.nameWidget.getMessage().equals(this.pack.getTitle())) {
            this.nameWidget.setMessage(this.pack.getTitle());
         }

         if (!this.descriptionWidget.getMessage().getContents().equals(this.pack.getExtendedDescription().getContents())) {
            this.descriptionWidget.setMessage(ComponentUtils.mergeStyles(this.pack.getExtendedDescription(), Style.EMPTY.withColor(-8355712)));
         }

         if (this.showHoverOverlay()
            && (this.minecraft.options.touchscreen().get() || p_426963_ || this.parent.getSelected() == this && this.parent.isFocused())) {
            p_425257_.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
            int i1 = p_428146_ - this.getContentX();
            int j1 = p_422806_ - this.getContentY();
            if (!this.pack.getCompatibility().isCompatible()) {
               this.nameWidget.setMessage(TransferableSelectionList.INCOMPATIBLE_TITLE);
               this.descriptionWidget.setMessage(this.pack.getCompatibility().getDescription());
            }

            if (this.pack.canSelect()) {
               if (this.mouseOverIcon(i1, j1, 32)) {
                  p_425257_.blitSprite(
                     RenderPipelines.GUI_TEXTURED, TransferableSelectionList.SELECT_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                  );
                  TransferableSelectionList.this.handleCursor(p_425257_);
               } else {
                  p_425257_.blitSprite(RenderPipelines.GUI_TEXTURED, TransferableSelectionList.SELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
               }
            } else {
               if (this.pack.canUnselect()) {
                  if (this.mouseOverLeftHalf(i1, j1, 32)) {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.UNSELECT_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                     TransferableSelectionList.this.handleCursor(p_425257_);
                  } else {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.UNSELECT_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                  }
               }

               if (this.pack.canMoveUp()) {
                  if (this.mouseOverTopRightQuarter(i1, j1, 32)) {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                     TransferableSelectionList.this.handleCursor(p_425257_);
                  } else {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                  }
               }

               if (this.pack.canMoveDown()) {
                  if (this.mouseOverBottomRightQuarter(i1, j1, 32)) {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                     TransferableSelectionList.this.handleCursor(p_425257_);
                  } else {
                     p_425257_.blitSprite(
                        RenderPipelines.GUI_TEXTURED, TransferableSelectionList.MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                     );
                  }
               }
            }
         }

         this.nameWidget.setMaxWidth(157 - (TransferableSelectionList.this.scrollbarVisible() ? 6 : 0));
         this.nameWidget.setPosition(this.getContentX() + 32 + 2, this.getContentY() + 1);
         this.nameWidget.render(p_425257_, p_428146_, p_422806_, p_426560_);
         this.descriptionWidget.setMaxWidth(157 - (TransferableSelectionList.this.scrollbarVisible() ? 6 : 0));
         this.descriptionWidget.setPosition(this.getContentX() + 32 + 2, this.getContentY() + 12);
         this.descriptionWidget.render(p_425257_, p_428146_, p_422806_, p_426560_);
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_423761_, boolean p_429246_) {
         if (this.showHoverOverlay()) {
            int i = (int)p_423761_.x() - this.getContentX();
            int j = (int)p_423761_.y() - this.getContentY();
            if (this.pack.canSelect() && this.mouseOverIcon(i, j, 32)) {
               this.handlePackSelection();
               return true;
            }

            if (this.pack.canUnselect() && this.mouseOverLeftHalf(i, j, 32)) {
               this.pack.unselect();
               return true;
            }

            if (this.pack.canMoveUp() && this.mouseOverTopRightQuarter(i, j, 32)) {
               this.pack.moveUp();
               return true;
            }

            if (this.pack.canMoveDown() && this.mouseOverBottomRightQuarter(i, j, 32)) {
               this.pack.moveDown();
               return true;
            }
         }

         return super.mouseClicked(p_423761_, p_429246_);
      }

      @Override
      public boolean keyPressed(KeyEvent p_429543_) {
         if (p_429543_.isConfirmation()) {
            this.keyboardSelection();
            return true;
         }

         if (p_429543_.hasShiftDown()) {
            if (p_429543_.isUp()) {
               this.keyboardMoveUp();
               return true;
            }

            if (p_429543_.isDown()) {
               this.keyboardMoveDown();
               return true;
            }
         }

         return super.keyPressed(p_429543_);
      }

      private boolean showHoverOverlay() {
         return !this.pack.isFixedPosition() || !this.pack.isRequired();
      }

      public void keyboardSelection() {
         if (this.pack.canSelect()) {
            this.handlePackSelection();
         } else if (this.pack.canUnselect()) {
            this.pack.unselect();
         }
      }

      private void keyboardMoveUp() {
         if (this.pack.canMoveUp()) {
            this.pack.moveUp();
         }
      }

      private void keyboardMoveDown() {
         if (this.pack.canMoveDown()) {
            this.pack.moveDown();
         }
      }

      private void handlePackSelection() {
         if (this.pack.getCompatibility().isCompatible()) {
            this.pack.select();
         } else {
            Component component = this.pack.getCompatibility().getConfirmation();
            this.minecraft.setScreen(new ConfirmScreen(p_264693_ -> {
               this.minecraft.setScreen(this.parent.screen);
               if (p_264693_) {
                  this.pack.select();
               }
            }, TransferableSelectionList.INCOMPATIBLE_CONFIRM_TITLE, component));
         }
      }

      @Override
      public String getPackId() {
         return this.pack.getId();
      }

      @Override
      public boolean shouldTakeFocusAfterInteraction() {
         return TransferableSelectionList.this.children().stream().anyMatch(p_420774_ -> p_420774_.getPackId().equals(this.getPackId()));
      }
   }
}
