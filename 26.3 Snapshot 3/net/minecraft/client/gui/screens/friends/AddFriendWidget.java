package net.minecraft.client.gui.screens.friends;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

class AddFriendWidget extends AbstractContainerWidget {
   private static final WidgetSprites ADD_SPRITE = new WidgetSprites(Identifier.withDefaultNamespace("friends/send_request"));
   private static final Identifier LIST_SEPARATOR_TOP = Identifier.withDefaultNamespace("friends/list_separator_top");
   private static final Component ENTER_NICKNAME = Component.translatable("gui.friends.enter_nickname");
   private static final Component SEND_REQUEST = Component.translatable("gui.friends.send_request");
   private static final Component EMPTY_NICKNAME_MESSAGE = Component.translatable("gui.friends.empty_nickname");
   private static final Component COPY_TO_CLIPBOARD = Component.translatable("gui.friends.copy_to_clipboard");
   private static final Component PROFILE_NAME_LABEL = Component.translatable("gui.friends.my_profile_name").withColor(-6250336);
   private static final int WINDOW_MARGIN = 8;
   private static final int SEPARATOR_HEIGHT = 2;
   private static final int INPUT_SPACING = 3;
   private static final int ADD_BUTTON_SIZE = 20;
   private static final int SEPARATOR_PADDING = 4;
   private static final int PROFILE_NAME_HEIGHT = 9;
   private static final int PROFILE_NAME_MARGIN = 6;
   private final EditBox editBox;
   private final SpriteIconButton addButton;
   private final PlainTextButton profileNameButton;
   private final Minecraft minecraft = Minecraft.getInstance();
   private final LinearLayout layout;

   AddFriendWidget(final int width, final Runnable afterSend) {
      super(0, 0, width, 0, Component.empty());
      this.editBox = new EditBox(this.minecraft.font, width - 20 - 3 - 16, 20, ENTER_NICKNAME) {
         @Override
         public boolean keyPressed(final KeyEvent event) {
            boolean enterPressed = event.key() == 257 || event.key() == 335;
            boolean elementsActive = this.isActive() && AddFriendWidget.this.addButton.active;
            if (elementsActive && this.isFocused() && enterPressed) {
               AddFriendWidget.this.addButton.playDownSound(AddFriendWidget.this.minecraft.getSoundManager());
               AddFriendWidget.this.addButton.onPress(event);
               return true;
            } else {
               return super.keyPressed(event);
            }
         }
      };
      this.editBox.setHint(ENTER_NICKNAME);
      this.editBox.setResponder(this::editBoxResponder);
      this.addButton = SpriteIconButton.builder(SEND_REQUEST, var2 -> {
         String name = this.getValue();
         if (name.isBlank()) {
            this.applyState(AddFriendWidget.State.EMPTY_INPUT);
         } else {
            Component invalidInputReason = this.getInvalidInputReason(name);
            if (invalidInputReason != null) {
               SystemToast.addOrUpdate(this.minecraft.gui.toastManager(), SystemToast.SystemToastId.FRIEND_SYSTEM_NOTIFICATION, invalidInputReason, null);
               this.applyState(AddFriendWidget.State.READY);
            } else {
               this.applyState(AddFriendWidget.State.SENDING);
               this.minecraft.getPlayerSocialManager().sendFriendRequest(name).thenAcceptAsync(var2x -> {
                  this.editBox.setValue("");
                  this.applyState(AddFriendWidget.State.EMPTY_INPUT);
                  afterSend.run();
               }, this.minecraft);
            }
         }
      }, true).sprite(ADD_SPRITE, 15, 15).size(20, 20).tooltip(SEND_REQUEST).switchToLoadingAfterPress().build();
      this.applyState(AddFriendWidget.State.EMPTY_INPUT);
      String profileName = this.minecraft.getUser().getName();
      final Component profileNameComponent = Component.literal(profileName);
      int profileNameWidth = this.minecraft.font.width(profileNameComponent);
      this.profileNameButton = new PlainTextButton(
         0, 0, profileNameWidth, 9, profileNameComponent, var2 -> this.minecraft.keyboardHandler.setClipboard(profileName), this.minecraft.font
      ) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return wrapDefaultNarrationMessage(Component.translatable("gui.friends.my_profile_name.narration", profileNameComponent));
         }
      };
      this.profileNameButton.setTooltip(Tooltip.create(COPY_TO_CLIPBOARD));
      this.layout = LinearLayout.vertical();
      LinearLayout inputRow = LinearLayout.horizontal().spacing(3);
      inputRow.addChild(this.editBox);
      inputRow.addChild(this.addButton);
      this.layout.addChild(inputRow, settings -> settings.paddingLeft(8).paddingTop(3));
      this.layout.addChild(this.createProfileRow(), settings -> settings.paddingLeft(8).paddingTop(6));
      this.layout.addChild(ImageWidget.sprite(width, 2, LIST_SEPARATOR_TOP), settings -> settings.paddingTop(4));
      this.layout.arrangeElements();
      this.setHeight(this.layout.getHeight());
   }

   private LinearLayout createProfileRow() {
      LinearLayout profileRow = LinearLayout.horizontal();
      StringWidget profileNameLabel = new StringWidget(PROFILE_NAME_LABEL, this.minecraft.font);
      if (this.minecraft.font.isBidirectional()) {
         profileRow.addChild(this.profileNameButton);
         profileRow.addChild(profileNameLabel);
      } else {
         profileRow.addChild(profileNameLabel);
         profileRow.addChild(this.profileNameButton);
      }

      return profileRow;
   }

   private void editBoxResponder(final String value) {
      this.applyState(value.trim().isEmpty() ? AddFriendWidget.State.EMPTY_INPUT : AddFriendWidget.State.READY);
   }

   private @Nullable Component getInvalidInputReason(final String name) {
      PlayerSocialManager playerSocialManager = this.minecraft.getPlayerSocialManager();
      if (this.minecraft.getUser().getName().equalsIgnoreCase(name)) {
         return Component.translatable("gui.friends.validation.cannot_add_self");
      } else if (contains(playerSocialManager.getFriends(), name)) {
         return Component.translatable("gui.friends.validation.already_friend", name);
      } else if (contains(playerSocialManager.getOutgoingRequests(), name)) {
         return Component.translatable("gui.friends.validation.already_outgoing", name);
      } else {
         return contains(playerSocialManager.getIncomingRequests(), name) ? Component.translatable("gui.friends.validation.already_incoming", name) : null;
      }
   }

   private static boolean contains(final List<PlayerSocialManager.PlayerData> players, final String playerName) {
      for (PlayerSocialManager.PlayerData playerData : players) {
         if (playerData.name().equalsIgnoreCase(playerName)) {
            return true;
         }
      }

      return false;
   }

   public void applyState(final AddFriendWidget.State newState) {
      switch (newState) {
         case EMPTY_INPUT:
            this.editBox.setEditable(true);
            this.editBox.active = true;
            this.addButton.setLoading(false);
            this.addButton.active = false;
            this.addButton.setTooltip(Tooltip.create(EMPTY_NICKNAME_MESSAGE));
            break;
         case READY:
            RemoteFriendListUpdateHandler.State friendListState = this.minecraft.getPlayerSocialManager().getFriendListState();
            boolean listReady = friendListState == RemoteFriendListUpdateHandler.State.SUCCESS;
            this.editBox.setEditable(true);
            this.editBox.active = true;
            this.addButton.setLoading(false);
            this.addButton.active = listReady;
            this.addButton.setTooltip(Tooltip.create(SEND_REQUEST));
            break;
         case SENDING:
            this.editBox.setEditable(false);
            this.editBox.active = false;
            this.editBox.setFocused(false);
            this.addButton.active = false;
            this.addButton.setLoading(true);
            break;
         case DISABLED:
            this.editBox.setEditable(false);
            this.editBox.active = false;
            this.editBox.setFocused(false);
            this.addButton.active = false;
            this.addButton.setLoading(false);
      }
   }

   public EditBox getEditBox() {
      return this.editBox;
   }

   public String getValue() {
      return this.editBox.getValue().trim();
   }

   public void setValue(final String value) {
      this.editBox.setValue(value);
   }

   @Override
   protected int contentHeight() {
      return this.height;
   }

   @Override
   public void setX(final int x) {
      super.setX(x);
      this.layout.setX(x);
      this.layout.arrangeElements();
   }

   @Override
   public void setY(final int y) {
      super.setY(y);
      this.layout.setY(y);
      this.layout.arrangeElements();
   }

   @Override
   protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      this.layout.visitWidgets(child -> child.extractRenderState(graphics, mouseX, mouseY, a));
   }

   @Override
   protected void updateWidgetNarration(final NarrationElementOutput output) {
   }

   @Override
   public Collection<? extends NarratableEntry> getNarratables() {
      return List.of(this.editBox, this.addButton, this.profileNameButton);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(this.editBox, this.addButton, this.profileNameButton);
   }

   enum State {
      EMPTY_INPUT,
      READY,
      SENDING,
      DISABLED;
   }
}
