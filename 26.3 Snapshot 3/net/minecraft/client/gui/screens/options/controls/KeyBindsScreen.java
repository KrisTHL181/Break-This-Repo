package net.minecraft.client.gui.screens.options.controls;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class KeyBindsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("controls.keybinds.title");
   public @Nullable KeyMapping selectedKey;
   public long lastKeySelection;
   private KeyBindsList keyBindsList;
   private Button resetButton;

   public KeyBindsScreen(final Screen lastScreen, final Options options) {
      super(lastScreen, options, TITLE);
   }

   @Override
   protected void addContents() {
      this.keyBindsList = this.layout.addToContents(new KeyBindsList(this, this.minecraft));
   }

   @Override
   protected void addOptions() {
   }

   @Override
   protected void addFooter() {
      this.resetButton = Button.builder(Component.translatable("controls.resetAll"), button -> {
         for (KeyMapping key : this.options.keyMappings) {
            key.setKey(key.getDefaultKey());
         }

         this.keyBindsList.resetMappingAndUpdateButtons();
      }).build();
      LinearLayout bottomButtons = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      bottomButtons.addChild(this.resetButton);
      bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).build());
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.keyBindsList.updateSize(this.width, this.layout);
   }

   @Override
   public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      if (this.selectedKey != null) {
         this.selectedKey.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
         this.selectedKey = null;
         this.keyBindsList.resetMappingAndUpdateButtons();
         return true;
      } else {
         return super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (this.selectedKey != null) {
         if (event.isEscape()) {
            this.selectedKey.setKey(InputConstants.UNKNOWN);
         } else {
            this.selectedKey.setKey(InputConstants.getKey(event));
         }

         this.selectedKey = null;
         this.lastKeySelection = Util.getMillis();
         this.keyBindsList.resetMappingAndUpdateButtons();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      boolean canReset = false;

      for (KeyMapping key : this.options.keyMappings) {
         if (!key.isDefault()) {
            canReset = true;
            break;
         }
      }

      this.resetButton.active = canReset;
   }
}
