package net.minecraft.client.gui.screens.options.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
   private static final int ITEM_HEIGHT = 20;
   final KeyBindsScreen keyBindsScreen;
   private int maxNameWidth;

   public KeyBindsList(KeyBindsScreen p_344272_, Minecraft p_345192_) {
      super(p_345192_, p_344272_.width, p_344272_.layout.getContentHeight(), p_344272_.layout.getHeaderHeight(), 20);
      this.keyBindsScreen = p_344272_;
      KeyMapping[] akeymapping = (KeyMapping[])ArrayUtils.clone(p_345192_.options.keyMappings);
      Arrays.sort(akeymapping);
      KeyMapping.Category keymapping$category = null;

      for (KeyMapping keymapping : akeymapping) {
         KeyMapping.Category keymapping$category1 = keymapping.getCategory();
         if (keymapping$category1 != keymapping$category) {
            keymapping$category = keymapping$category1;
            this.addEntry(new KeyBindsList.CategoryEntry(keymapping$category1));
         }

         Component component = Component.translatable(keymapping.getName());
         int i = p_345192_.font.width(component);
         if (i > this.maxNameWidth) {
            this.maxNameWidth = i;
         }

         this.addEntry(new KeyBindsList.KeyEntry(keymapping, component));
      }
   }

   public void resetMappingAndUpdateButtons() {
      KeyMapping.resetMapping();
      this.refreshEntries();
   }

   public void refreshEntries() {
      this.children().forEach(KeyBindsList.Entry::refreshEntry);
   }

   @Override
   public int getRowWidth() {
      return 340;
   }

   @OnlyIn(Dist.CLIENT)
   public class CategoryEntry extends KeyBindsList.Entry {
      private final FocusableTextWidget categoryName;

      public CategoryEntry(final KeyMapping.Category p_423416_) {
         this.categoryName = FocusableTextWidget.builder(p_423416_.label(), KeyBindsList.this.minecraft.font)
            .alwaysShowBorder(false)
            .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
            .build();
      }

      @Override
      public void renderContent(GuiGraphics p_427814_, int p_427133_, int p_423159_, boolean p_423989_, float p_427776_) {
         this.categoryName.setPosition(KeyBindsList.this.width / 2 - this.categoryName.getWidth() / 2, this.getContentBottom() - this.categoryName.getHeight());
         this.categoryName.render(p_427814_, p_427133_, p_423159_, p_427776_);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.categoryName);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.categoryName);
      }

      @Override
      protected void refreshEntry() {
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
      abstract void refreshEntry();
   }

   @OnlyIn(Dist.CLIENT)
   public class KeyEntry extends KeyBindsList.Entry {
      private static final Component RESET_BUTTON_TITLE = Component.translatable("controls.reset");
      private static final int PADDING = 10;
      private final KeyMapping key;
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;
      private boolean hasCollision = false;

      KeyEntry(final KeyMapping p_343088_, final Component p_343976_) {
         this.key = p_343088_;
         this.name = p_343976_;
         this.changeButton = Button.builder(p_343976_, p_342196_ -> {
               KeyBindsList.this.keyBindsScreen.selectedKey = p_343088_;
               KeyBindsList.this.resetMappingAndUpdateButtons();
            })
            .bounds(0, 0, 75, 20)
            .createNarration(
               p_342179_ -> p_343088_.isUnbound()
                  ? Component.translatable("narrator.controls.unbound", p_343976_)
                  : Component.translatable("narrator.controls.bound", p_343976_, p_342179_.get())
            )
            .build();
         this.resetButton = Button.builder(RESET_BUTTON_TITLE, p_357685_ -> {
            p_343088_.setKey(p_343088_.getDefaultKey());
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 50, 20).createNarration(p_344192_ -> Component.translatable("narrator.controls.reset", p_343976_)).build();
         this.refreshEntry();
      }

      @Override
      public void renderContent(GuiGraphics p_425264_, int p_426918_, int p_427649_, boolean p_422824_, float p_425662_) {
         int i = KeyBindsList.this.scrollBarX() - this.resetButton.getWidth() - 10;
         int j = this.getContentY() - 2;
         this.resetButton.setPosition(i, j);
         this.resetButton.render(p_425264_, p_426918_, p_427649_, p_425662_);
         int k = i - 5 - this.changeButton.getWidth();
         this.changeButton.setPosition(k, j);
         this.changeButton.render(p_425264_, p_426918_, p_427649_, p_425662_);
         p_425264_.drawString(KeyBindsList.this.minecraft.font, this.name, this.getContentX(), this.getContentYMiddle() - 4, -1);
         if (this.hasCollision) {
            int l = 3;
            int i1 = this.changeButton.getX() - 6;
            p_425264_.fill(i1, this.getContentY() - 1, i1 + 3, this.getContentBottom(), -256);
         }
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      @Override
      protected void refreshEntry() {
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         this.resetButton.active = !this.key.isDefault();
         this.hasCollision = false;
         MutableComponent mutablecomponent = Component.empty();
         if (!this.key.isUnbound()) {
            for (KeyMapping keymapping : KeyBindsList.this.minecraft.options.keyMappings) {
               if (keymapping != this.key && this.key.same(keymapping) && (!keymapping.isDefault() || !this.key.isDefault())) {
                  if (this.hasCollision) {
                     mutablecomponent.append(", ");
                  }

                  this.hasCollision = true;
                  mutablecomponent.append(Component.translatable(keymapping.getName()));
               }
            }
         }

         if (this.hasCollision) {
            this.changeButton
               .setMessage(
                  Component.literal("[ ")
                     .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE))
                     .append(" ]")
                     .withStyle(ChatFormatting.YELLOW)
               );
            this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", mutablecomponent)));
         } else {
            this.changeButton.setTooltip(null);
         }

         if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
            this.changeButton
               .setMessage(
                  Component.literal("> ")
                     .append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
                     .append(" <")
                     .withStyle(ChatFormatting.YELLOW)
               );
         }
      }
   }
}
