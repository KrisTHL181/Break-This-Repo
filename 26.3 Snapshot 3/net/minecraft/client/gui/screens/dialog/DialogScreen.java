package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandlers;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public abstract class DialogScreen<T extends Dialog> extends Screen {
   public static final Component DISCONNECT = Component.translatable("menu.custom_screen_info.disconnect");
   private static final int WARNING_BUTTON_SIZE = 20;
   private static final WidgetSprites WARNING_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("dialog/warning_button"),
      Identifier.withDefaultNamespace("dialog/warning_button_disabled"),
      Identifier.withDefaultNamespace("dialog/warning_button_highlighted")
   );
   private final T dialog;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final @Nullable Screen previousScreen;
   private @Nullable ScrollableLayout bodyScroll;
   private Button warningButton;
   private final DialogConnectionAccess connectionAccess;
   private Supplier<Optional<ClickEvent>> onClose = DialogControlSet.EMPTY_ACTION;

   public DialogScreen(final @Nullable Screen previousScreen, final T dialog, final DialogConnectionAccess connectionAccess) {
      super(dialog.common().title());
      this.dialog = dialog;
      this.previousScreen = previousScreen;
      this.connectionAccess = connectionAccess;
   }

   @Override
   protected final void init() {
      super.init();
      this.warningButton = this.createWarningButton();
      this.warningButton.setTabOrderGroup(-10);
      DialogControlSet controlSet = new DialogControlSet(this);
      LinearLayout body = LinearLayout.vertical().spacing(10);
      body.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addToHeader(this.createTitleWithWarningButton());

      for (DialogBody dialogBody : this.dialog.common().body()) {
         LayoutElement bodyElement = DialogBodyHandlers.createBodyElement(this, dialogBody);
         if (bodyElement != null) {
            body.addChild(bodyElement);
         }
      }

      for (Input input : this.dialog.common().inputs()) {
         controlSet.addInput(input, body::addChild);
      }

      this.populateBodyElements(body, controlSet, this.dialog, this.connectionAccess);
      this.bodyScroll = new ScrollableLayout(this.minecraft, body, this.layout.getContentHeight());
      this.layout.addToContents(this.bodyScroll);
      this.updateHeaderAndFooter(this.layout, controlSet, this.dialog, this.connectionAccess);
      this.onClose = controlSet.bindAction(this.dialog.onCancel());
      this.layout.visitWidgets(widget -> {
         if (widget != this.warningButton) {
            this.addRenderableWidget(widget);
         }
      });
      this.addRenderableWidget(this.warningButton);
      this.repositionElements();
   }

   protected void populateBodyElements(
      final LinearLayout layout, final DialogControlSet controlSet, final T dialog, final DialogConnectionAccess connectionAccess
   ) {
   }

   protected void updateHeaderAndFooter(
      final HeaderAndFooterLayout layout, final DialogControlSet controlSet, final T dialog, final DialogConnectionAccess connectionAccess
   ) {
   }

   @Override
   protected void repositionElements() {
      this.bodyScroll.arrangeElements();
      this.bodyScroll.setMaxHeight(this.layout.getContentHeight());
      this.layout.arrangeElements();
      this.makeSureWarningButtonIsInBounds();
   }

   protected LayoutElement createTitleWithWarningButton() {
      LinearLayout layout = LinearLayout.horizontal().spacing(10);
      layout.defaultCellSetting().alignHorizontallyCenter().alignVerticallyMiddle();
      layout.addChild(new StringWidget(this.title, this.font));
      layout.addChild(this.warningButton);
      return layout;
   }

   protected void makeSureWarningButtonIsInBounds() {
      int x = this.warningButton.getX();
      int y = this.warningButton.getY();
      if (x < 0 || y < 0 || x > this.width - 20 || y > this.height - 20) {
         this.warningButton.setX(Math.max(0, this.width - 40));
         this.warningButton.setY(Math.min(5, this.height));
      }
   }

   private Button createWarningButton() {
      ImageButton result = new ImageButton(
         0,
         0,
         20,
         20,
         WARNING_BUTTON_SPRITES,
         var1x -> this.minecraft.gui.setScreen(DialogScreen.WarningScreen.create(this.minecraft, this.connectionAccess, this)),
         Component.translatable("menu.custom_screen_info.button_narration")
      );
      result.setTooltip(Tooltip.create(Component.translatable("menu.custom_screen_info.tooltip")));
      return result;
   }

   @Override
   public boolean isPauseScreen() {
      return this.dialog.common().pause();
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.dialog.common().canCloseWithEscape();
   }

   @Override
   public void onClose() {
      this.runAction(this.onClose.get(), DialogAction.CLOSE);
   }

   public void runAction(final Optional<ClickEvent> closeAction) {
      this.runAction(closeAction, this.dialog.common().afterAction());
   }

   public void runAction(final Optional<ClickEvent> closeAction, final DialogAction afterAction) {
      Screen screenToActivate = switch (afterAction) {
         case NONE -> this;
         case CLOSE -> this.previousScreen;
         case WAIT_FOR_RESPONSE -> new WaitingForResponseScreen(this.previousScreen);
      };
      if (closeAction.isPresent()) {
         this.handleDialogClickEvent(closeAction.get(), screenToActivate);
      } else {
         this.minecraft.gui.setScreen(screenToActivate);
      }
   }

   private void handleDialogClickEvent(final ClickEvent event, final @Nullable Screen activeScreen) {
      switch (event) {
         case ClickEvent.RunCommand(String command):
            this.connectionAccess.runCommand(Commands.trimOptionalPrefix(command), activeScreen);
            break;
         case ClickEvent.ShowDialog dialog:
            this.connectionAccess.openDialog(dialog.dialog(), activeScreen);
            break;
         case ClickEvent.Custom custom:
            this.connectionAccess.sendCustomAction(custom.id(), custom.payload());
            this.minecraft.gui.setScreen(activeScreen);
            break;
         default:
            defaultHandleClickEvent(event, this.minecraft, activeScreen);
      }
   }

   public @Nullable Screen previousScreen() {
      return this.previousScreen;
   }

   protected static LayoutElement packControlsIntoColumns(final List<? extends LayoutElement> controls, final int columns) {
      GridLayout gridLayout = new GridLayout();
      gridLayout.defaultCellSetting().alignHorizontallyCenter();
      gridLayout.columnSpacing(2).rowSpacing(2);
      int count = controls.size();
      int lastFullRow = count / columns;
      int countInFullRows = lastFullRow * columns;

      for (int i = 0; i < countInFullRows; i++) {
         gridLayout.addChild(controls.get(i), i / columns, i % columns);
      }

      if (count != countInFullRows) {
         LinearLayout lastRow = LinearLayout.horizontal().spacing(2);
         lastRow.defaultCellSetting().alignHorizontallyCenter();

         for (int i = countInFullRows; i < count; i++) {
            lastRow.addChild(controls.get(i));
         }

         gridLayout.addChild(lastRow, lastFullRow, 0, 1, columns);
      }

      return gridLayout;
   }

   public static class WarningScreen extends ConfirmScreen {
      private final MutableObject<@Nullable Screen> returnScreen;

      public static Screen create(final Minecraft minecraft, final DialogConnectionAccess connectionAccess, final Screen returnScreen) {
         return new DialogScreen.WarningScreen(minecraft, connectionAccess, new MutableObject(returnScreen));
      }

      private WarningScreen(final Minecraft minecraft, final DialogConnectionAccess connectionAccess, final MutableObject<Screen> returnScreen) {
         super(
            disconnect -> {
               if (disconnect) {
                  connectionAccess.disconnect(DialogScreen.DISCONNECT);
               } else {
                  minecraft.gui.setScreen((Screen)returnScreen.get());
               }
            },
            Component.translatable("menu.custom_screen_info.title"),
            Component.translatable("menu.custom_screen_info.contents"),
            CommonComponents.disconnectButtonLabel(minecraft.isLocalServer()),
            CommonComponents.GUI_BACK
         );
         this.returnScreen = returnScreen;
      }

      public @Nullable Screen returnScreen() {
         return (Screen)this.returnScreen.get();
      }

      public void updateReturnScreen(final @Nullable Screen newReturnScreen) {
         this.returnScreen.setValue(newReturnScreen);
      }
   }
}
