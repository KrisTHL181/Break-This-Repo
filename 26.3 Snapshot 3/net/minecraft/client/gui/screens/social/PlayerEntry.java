package net.minecraft.client.gui.screens.social;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.reporting.ReportPlayerScreen;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

public class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
   private static final Identifier DRAFT_REPORT_SPRITE = Identifier.withDefaultNamespace("icon/draft_report");
   private static final Duration TOOLTIP_DELAY = Duration.ofMillis(500L);
   private static final WidgetSprites REPORT_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("social_interactions/report_button"),
      Identifier.withDefaultNamespace("social_interactions/report_button_disabled"),
      Identifier.withDefaultNamespace("social_interactions/report_button_highlighted")
   );
   private static final WidgetSprites MUTE_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("social_interactions/mute_button"), Identifier.withDefaultNamespace("social_interactions/mute_button_highlighted")
   );
   private static final WidgetSprites UNMUTE_BUTTON_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("social_interactions/unmute_button"), Identifier.withDefaultNamespace("social_interactions/unmute_button_highlighted")
   );
   private final Minecraft minecraft;
   private final List<AbstractWidget> children;
   private final UUID id;
   private final String playerName;
   private final Supplier<PlayerSkin> skinGetter;
   private boolean isRemoved;
   private boolean hasRecentMessages;
   private final boolean reportingEnabled;
   private boolean hasDraftReport;
   private final boolean chatReportable;
   private @Nullable Button hideButton;
   private @Nullable Button showButton;
   private @Nullable Button reportButton;
   private float tooltipHoverTime;
   private static final Component HIDDEN = Component.translatable("gui.socialInteractions.status_hidden").withStyle(ChatFormatting.ITALIC);
   private static final Component BLOCKED = Component.translatable("gui.socialInteractions.status_blocked").withStyle(ChatFormatting.ITALIC);
   private static final Component OFFLINE = Component.translatable("gui.socialInteractions.status_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component HIDDEN_OFFLINE = Component.translatable("gui.socialInteractions.status_hidden_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component BLOCKED_OFFLINE = Component.translatable("gui.socialInteractions.status_blocked_offline").withStyle(ChatFormatting.ITALIC);
   private static final Component REPORT_DISABLED_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.report.disabled");
   private static final Component HIDE_TEXT_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.hide");
   private static final Component SHOW_TEXT_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.show");
   private static final Component REPORT_PLAYER_TOOLTIP = Component.translatable("gui.socialInteractions.tooltip.report");
   private static final int SKIN_SIZE = 24;
   private static final int PADDING = 4;
   public static final int SKIN_SHADE = ARGB.color(190, 0, 0, 0);
   private static final int CHAT_TOGGLE_ICON_SIZE = 20;
   public static final int BG_FILL = ARGB.color(255, 74, 74, 74);
   public static final int BG_FILL_REMOVED = ARGB.color(255, 48, 48, 48);
   public static final int PLAYERNAME_COLOR = ARGB.color(255, 255, 255, 255);
   public static final int PLAYER_STATUS_COLOR = ARGB.color(140, 255, 255, 255);

   public PlayerEntry(
      final Minecraft minecraft,
      final SocialInteractionsScreen socialInteractionsScreen,
      final UUID id,
      final String playerName,
      final Supplier<PlayerSkin> skinGetter,
      final boolean chatReportable
   ) {
      this.minecraft = minecraft;
      this.id = id;
      this.playerName = playerName;
      this.skinGetter = skinGetter;
      ReportingContext reportingContext = minecraft.getReportingContext();
      this.reportingEnabled = reportingContext.sender().isEnabled();
      this.chatReportable = chatReportable;
      this.refreshHasDraftReport(reportingContext);
      Component hideNarration = Component.translatable("gui.socialInteractions.narration.hide", playerName);
      Component showNarration = Component.translatable("gui.socialInteractions.narration.show", playerName);
      PlayerSocialManager socialManager = minecraft.getPlayerSocialManager();
      boolean chatDisabledOrBlocked = !minecraft.player.chatAbilities().canReceivePlayerMessages() || socialManager.isBlocked(id);
      boolean notLocalPlayer = !minecraft.player.getUUID().equals(id);
      if (!SharedConstants.DEBUG_SOCIAL_INTERACTIONS && !notLocalPlayer) {
         this.children = ImmutableList.of();
      } else {
         this.reportButton = new ImageButton(
            0,
            0,
            20,
            20,
            REPORT_BUTTON_SPRITES,
            button -> reportingContext.draftReportHandled(
               minecraft,
               socialInteractionsScreen,
               () -> minecraft.gui.setScreen(new ReportPlayerScreen(socialInteractionsScreen, reportingContext, this, chatDisabledOrBlocked)),
               false
            ),
            Component.translatable("gui.socialInteractions.report")
         ) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.reportButton.active = this.reportingEnabled;
         this.reportButton.setTooltip(this.createReportButtonTooltip());
         this.reportButton.setTooltipDelay(TOOLTIP_DELAY);
         this.hideButton = new ImageButton(0, 0, 20, 20, MUTE_BUTTON_SPRITES, button -> {
            socialManager.hidePlayer(id);
            this.onHiddenOrShown(true, Component.translatable("gui.socialInteractions.hidden_in_chat", playerName));
         }, Component.translatable("gui.socialInteractions.hide")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.hideButton.setTooltip(Tooltip.create(HIDE_TEXT_TOOLTIP, hideNarration));
         this.hideButton.setTooltipDelay(TOOLTIP_DELAY);
         this.showButton = new ImageButton(0, 0, 20, 20, UNMUTE_BUTTON_SPRITES, button -> {
            socialManager.showPlayer(id);
            this.onHiddenOrShown(false, Component.translatable("gui.socialInteractions.shown_in_chat", playerName));
         }, Component.translatable("gui.socialInteractions.show")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.showButton.setTooltip(Tooltip.create(SHOW_TEXT_TOOLTIP, showNarration));
         this.showButton.setTooltipDelay(TOOLTIP_DELAY);
         this.children = new ArrayList<>();
         this.children.add(this.hideButton);
         this.children.add(this.reportButton);
         this.updateHideAndShowButton(socialManager.isHidden(this.id));
      }
   }

   public void refreshHasDraftReport(final ReportingContext reportingContext) {
      this.hasDraftReport = reportingContext.hasDraftReportFor(this.id);
   }

   private Tooltip createReportButtonTooltip() {
      return !this.reportingEnabled
         ? Tooltip.create(REPORT_DISABLED_TOOLTIP)
         : Tooltip.create(REPORT_PLAYER_TOOLTIP, Component.translatable("gui.socialInteractions.narration.report", this.playerName));
   }

   @Override
   public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
      int skinX = this.getContentX() + 4;
      int skinY = this.getContentY() + (this.getContentHeight() - 24) / 2;
      int textStartX = skinX + 24 + 4;
      Component status = this.getStatusComponent();
      int textStartY;
      if (status == CommonComponents.EMPTY) {
         graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL);
         textStartY = this.getContentY() + (this.getContentHeight() - 9) / 2;
      } else {
         graphics.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL_REMOVED);
         textStartY = this.getContentY() + (this.getContentHeight() - (9 + 9)) / 2;
         graphics.text(this.minecraft.font, status, textStartX, textStartY + 12, PLAYER_STATUS_COLOR);
      }

      PlayerFaceExtractor.extractRenderState(graphics, this.skinGetter.get(), skinX, skinY, 24);
      graphics.text(this.minecraft.font, this.playerName, textStartX, textStartY, PLAYERNAME_COLOR);
      if (this.isRemoved) {
         graphics.fill(skinX, skinY, skinX + 24, skinY + 24, SKIN_SHADE);
      }

      if (this.hideButton != null && this.showButton != null && this.reportButton != null) {
         float lastHoverTime = this.tooltipHoverTime;
         this.hideButton.setX(this.getContentX() + (this.getContentWidth() - this.hideButton.getWidth() - 4) - 20 - 4);
         this.hideButton.setY(this.getContentY() + (this.getContentHeight() - this.hideButton.getHeight()) / 2);
         this.hideButton.extractRenderState(graphics, mouseX, mouseY, a);
         this.showButton.setX(this.getContentX() + (this.getContentWidth() - this.showButton.getWidth() - 4) - 20 - 4);
         this.showButton.setY(this.getContentY() + (this.getContentHeight() - this.showButton.getHeight()) / 2);
         this.showButton.extractRenderState(graphics, mouseX, mouseY, a);
         this.reportButton.setX(this.getContentX() + (this.getContentWidth() - this.showButton.getWidth() - 4));
         this.reportButton.setY(this.getContentY() + (this.getContentHeight() - this.showButton.getHeight()) / 2);
         this.reportButton.extractRenderState(graphics, mouseX, mouseY, a);
         if (lastHoverTime == this.tooltipHoverTime) {
            this.tooltipHoverTime = 0.0F;
         }
      }

      if (this.hasDraftReport && this.reportButton != null) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DRAFT_REPORT_SPRITE, this.reportButton.getX() + 5, this.reportButton.getY() + 1, 15, 15);
      }
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return this.children;
   }

   @Override
   public List<? extends NarratableEntry> narratables() {
      return this.children;
   }

   public String getPlayerName() {
      return this.playerName;
   }

   public UUID getPlayerId() {
      return this.id;
   }

   public Supplier<PlayerSkin> getSkinGetter() {
      return this.skinGetter;
   }

   public void setRemoved(final boolean isRemoved) {
      this.isRemoved = isRemoved;
   }

   public boolean isRemoved() {
      return this.isRemoved;
   }

   public void setHasRecentMessages(final boolean hasRecentMessages) {
      this.hasRecentMessages = hasRecentMessages;
   }

   public boolean hasRecentMessages() {
      return this.hasRecentMessages;
   }

   public boolean isChatReportable() {
      return this.chatReportable;
   }

   private void onHiddenOrShown(final boolean isHidden, final Component message) {
      this.updateHideAndShowButton(isHidden);
      this.minecraft.gui.hud.getChat().addClientSystemMessage(message);
      this.minecraft.getNarrator().saySystemNow(message);
   }

   private void updateHideAndShowButton(final boolean isHidden) {
      this.showButton.visible = isHidden;
      this.hideButton.visible = !isHidden;
      this.children.set(0, isHidden ? this.showButton : this.hideButton);
   }

   private MutableComponent getEntryNarationMessage(final MutableComponent buttonNarrationMessage) {
      Component status = this.getStatusComponent();
      return status == CommonComponents.EMPTY
         ? Component.literal(this.playerName).append(", ").append(buttonNarrationMessage)
         : Component.literal(this.playerName).append(", ").append(status).append(", ").append(buttonNarrationMessage);
   }

   private Component getStatusComponent() {
      boolean isHidden = this.minecraft.getPlayerSocialManager().isHidden(this.id);
      boolean isBlocked = this.minecraft.getPlayerSocialManager().isBlocked(this.id);
      if (isBlocked && this.isRemoved) {
         return BLOCKED_OFFLINE;
      } else if (isHidden && this.isRemoved) {
         return HIDDEN_OFFLINE;
      } else if (isBlocked) {
         return BLOCKED;
      } else if (isHidden) {
         return HIDDEN;
      } else {
         return this.isRemoved ? OFFLINE : CommonComponents.EMPTY;
      }
   }
}
