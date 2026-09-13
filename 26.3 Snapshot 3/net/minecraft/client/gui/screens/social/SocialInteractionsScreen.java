package net.minecraft.client.gui.screens.social;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonLinks;
import org.jspecify.annotations.Nullable;

public class SocialInteractionsScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.socialInteractions.title");
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("social_interactions/background");
   private static final Identifier SEARCH_SPRITE = Identifier.withDefaultNamespace("icon/search");
   private static final Component TAB_ALL = Component.translatable("gui.socialInteractions.tab_all");
   private static final Component TAB_HIDDEN = Component.translatable("gui.socialInteractions.tab_hidden");
   private static final Component TAB_BLOCKED = Component.translatable("gui.socialInteractions.tab_blocked");
   private static final Component TAB_ALL_SELECTED = TAB_ALL.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_HIDDEN_SELECTED = TAB_HIDDEN.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_BLOCKED_SELECTED = TAB_BLOCKED.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component SEARCH_HINT = Component.translatable("gui.socialInteractions.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
   private static final Component EMPTY_SEARCH = Component.translatable("gui.socialInteractions.search_empty").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_HIDDEN = Component.translatable("gui.socialInteractions.empty_hidden").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_BLOCKED = Component.translatable("gui.socialInteractions.empty_blocked").withStyle(ChatFormatting.GRAY);
   private static final Component BLOCKING_HINT = Component.translatable("gui.socialInteractions.blocking_hint");
   private static final int BG_BORDER_SIZE = 8;
   private static final int BG_WIDTH = 236;
   private static final int SEARCH_HEIGHT = 16;
   private static final int MARGIN_Y = 64;
   public static final int SEARCH_START = 72;
   public static final int LIST_START = 88;
   private static final int IMAGE_WIDTH = 238;
   private static final int BUTTON_HEIGHT = 20;
   private static final int ITEM_HEIGHT = 36;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final @Nullable Screen lastScreen;
   private @Nullable SocialInteractionsPlayerList socialInteractionsPlayerList;
   private EditBox searchBox;
   private String lastSearch = "";
   private SocialInteractionsScreen.Page page = SocialInteractionsScreen.Page.ALL;
   private Button allButton;
   private Button hiddenButton;
   private Button blockedButton;
   private Button blockingHintButton;
   private @Nullable Component serverLabel;
   private int playerCount;

   public SocialInteractionsScreen() {
      this(null);
   }

   public SocialInteractionsScreen(final @Nullable Screen lastScreen) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.updateServerLabel(Minecraft.getInstance());
   }

   private int windowHeight() {
      return Math.max(52, this.height - 128 - 16);
   }

   private int listEnd() {
      return 80 + this.windowHeight() - 8;
   }

   private int marginX() {
      return (this.width - 238) / 2;
   }

   @Override
   public Component getNarrationMessage() {
      return this.serverLabel != null ? CommonComponents.joinForNarration(super.getNarrationMessage(), this.serverLabel) : super.getNarrationMessage();
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.socialInteractionsPlayerList = new SocialInteractionsPlayerList(this, this.minecraft, this.width, this.listEnd() - 88, 88, 36);
      int buttonWidth = this.socialInteractionsPlayerList.getRowWidth() / 3;
      int buttonLeft = this.socialInteractionsPlayerList.getRowLeft();
      int buttonRight = this.socialInteractionsPlayerList.getRowRight();
      this.allButton = this.addRenderableWidget(
         Button.builder(TAB_ALL, button -> this.showPage(SocialInteractionsScreen.Page.ALL)).bounds(buttonLeft, 45, buttonWidth, 20).build()
      );
      this.hiddenButton = this.addRenderableWidget(
         Button.builder(TAB_HIDDEN, button -> this.showPage(SocialInteractionsScreen.Page.HIDDEN))
            .bounds((buttonLeft + buttonRight - buttonWidth) / 2 + 1, 45, buttonWidth, 20)
            .build()
      );
      this.blockedButton = this.addRenderableWidget(
         Button.builder(TAB_BLOCKED, button -> this.showPage(SocialInteractionsScreen.Page.BLOCKED))
            .bounds(buttonRight - buttonWidth + 1, 45, buttonWidth, 20)
            .build()
      );
      String oldEdit = this.searchBox != null ? this.searchBox.getValue() : "";
      this.searchBox = this.addRenderableWidget(
         new EditBox(this.font, this.marginX() + 28, 74, 200, 15, SEARCH_HINT) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return !SocialInteractionsScreen.this.searchBox.getValue().isEmpty() && SocialInteractionsScreen.this.socialInteractionsPlayerList.isEmpty()
                  ? super.createNarrationMessage().append(", ").append(SocialInteractionsScreen.EMPTY_SEARCH)
                  : super.createNarrationMessage();
            }
         }
      );
      this.searchBox.setMaxLength(16);
      this.searchBox.setVisible(true);
      this.searchBox.setTextColor(-1);
      this.searchBox.setValue(oldEdit);
      this.searchBox.setHint(SEARCH_HINT);
      this.searchBox.setResponder(this::checkSearchStringUpdate);
      this.blockingHintButton = this.addRenderableWidget(
         Button.builder(BLOCKING_HINT, ConfirmLinkScreen.confirmLink(this, CommonLinks.BLOCKING_HELP))
            .bounds(this.width / 2 - 100, 64 + this.windowHeight(), 200, 20)
            .build()
      );
      this.addWidget(this.socialInteractionsPlayerList);
      this.showPage(this.page);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   public void added() {
      if (this.socialInteractionsPlayerList != null) {
         this.socialInteractionsPlayerList.refreshHasDraftReport();
      }
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      this.socialInteractionsPlayerList.updateSizeAndPosition(this.width, this.listEnd() - 88, 88);
      this.searchBox.setPosition(this.marginX() + 28, 74);
      int buttonLeft = this.socialInteractionsPlayerList.getRowLeft();
      int buttonRight = this.socialInteractionsPlayerList.getRowRight();
      int buttonWidth = this.socialInteractionsPlayerList.getRowWidth() / 3;
      this.allButton.setPosition(buttonLeft, 45);
      this.hiddenButton.setPosition((buttonLeft + buttonRight - buttonWidth) / 2 + 1, 45);
      this.blockedButton.setPosition(buttonRight - buttonWidth + 1, 45);
      this.blockingHintButton.setPosition(this.width / 2 - 100, 64 + this.windowHeight());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.searchBox);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   private void showPage(final SocialInteractionsScreen.Page page) {
      this.page = page;
      this.allButton.setMessage(TAB_ALL);
      this.hiddenButton.setMessage(TAB_HIDDEN);
      this.blockedButton.setMessage(TAB_BLOCKED);
      boolean isEmpty = false;
      switch (page) {
         case ALL:
            this.allButton.setMessage(TAB_ALL_SELECTED);
            Collection<UUID> onlinePlayerIds = this.minecraft.player.connection.getOnlinePlayerIds();
            this.socialInteractionsPlayerList.updatePlayerList(onlinePlayerIds, this.socialInteractionsPlayerList.scrollAmount(), true);
            break;
         case HIDDEN:
            this.hiddenButton.setMessage(TAB_HIDDEN_SELECTED);
            Set<UUID> hiddenPlayers = this.minecraft.getPlayerSocialManager().getHiddenPlayers();
            isEmpty = hiddenPlayers.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(hiddenPlayers, this.socialInteractionsPlayerList.scrollAmount(), false);
            break;
         case BLOCKED:
            this.blockedButton.setMessage(TAB_BLOCKED_SELECTED);
            PlayerSocialManager socialManager = this.minecraft.getPlayerSocialManager();
            Set<UUID> blockedPlayers = this.minecraft
               .player
               .connection
               .getOnlinePlayerIds()
               .stream()
               .filter(socialManager::isBlocked)
               .collect(Collectors.toSet());
            isEmpty = blockedPlayers.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(blockedPlayers, this.socialInteractionsPlayerList.scrollAmount(), false);
      }

      GameNarrator narrator = this.minecraft.getNarrator();
      if (!this.searchBox.getValue().isEmpty() && this.socialInteractionsPlayerList.isEmpty() && !this.searchBox.isFocused()) {
         narrator.saySystemNow(EMPTY_SEARCH);
      } else if (isEmpty) {
         if (page == SocialInteractionsScreen.Page.HIDDEN) {
            narrator.saySystemNow(EMPTY_HIDDEN);
         } else if (page == SocialInteractionsScreen.Page.BLOCKED) {
            narrator.saySystemNow(EMPTY_BLOCKED);
         }
      }
   }

   @Override
   public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractBackground(graphics, mouseX, mouseY, a);
      int marginX = this.marginX() + 3;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, marginX, 64, 236, this.windowHeight() + 16);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SEARCH_SPRITE, marginX + 10, 76, 12, 12);
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      this.updateServerLabel(this.minecraft);
      if (this.serverLabel != null) {
         graphics.text(this.minecraft.font, this.serverLabel, this.marginX() + 8, 35, -1);
      }

      if (!this.socialInteractionsPlayerList.isEmpty()) {
         this.socialInteractionsPlayerList.extractRenderState(graphics, mouseX, mouseY, a);
      } else if (!this.searchBox.getValue().isEmpty()) {
         graphics.centeredText(this.minecraft.font, EMPTY_SEARCH, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.HIDDEN) {
         graphics.centeredText(this.minecraft.font, EMPTY_HIDDEN, this.width / 2, (72 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.BLOCKED) {
         graphics.centeredText(this.minecraft.font, EMPTY_BLOCKED, this.width / 2, (72 + this.listEnd()) / 2, -1);
      }

      this.blockingHintButton.visible = this.page == SocialInteractionsScreen.Page.BLOCKED;
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (!this.isInputCaptured() && this.minecraft.options.keySocialInteractions.matches(event)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void checkSearchStringUpdate(String searchText) {
      searchText = searchText.toLowerCase(Locale.ROOT);
      if (!searchText.equals(this.lastSearch)) {
         this.socialInteractionsPlayerList.setFilter(searchText);
         this.lastSearch = searchText;
         this.showPage(this.page);
      }
   }

   private void updateServerLabel(final Minecraft minecraft) {
      int playerCount = minecraft.getConnection().getOnlinePlayers().size();
      if (this.playerCount != playerCount) {
         String serverName = "";
         ServerData currentServer = minecraft.getCurrentServer();
         if (minecraft.isLocalServer()) {
            serverName = minecraft.getSingleplayerServer().getMotd();
         } else if (currentServer != null) {
            serverName = currentServer.name;
         }

         if (playerCount > 1) {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.multiple", serverName, playerCount);
         } else {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.single", serverName, playerCount);
         }

         this.playerCount = playerCount;
      }
   }

   public void onAddPlayer(final PlayerInfo info) {
      this.socialInteractionsPlayerList.addPlayer(info, this.page);
   }

   public void onRemovePlayer(final UUID id) {
      this.socialInteractionsPlayerList.removePlayer(id);
   }

   public enum Page {
      ALL,
      HIDDEN,
      BLOCKED;
   }
}
