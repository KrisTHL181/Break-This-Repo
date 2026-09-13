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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
      Minecraft p_243293_, SocialInteractionsScreen p_243214_, UUID p_243288_, String p_243311_, Supplier<PlayerSkin> p_243309_, boolean p_243297_
   ) {
      this.minecraft = p_243293_;
      this.id = p_243288_;
      this.playerName = p_243311_;
      this.skinGetter = p_243309_;
      ReportingContext reportingcontext = p_243293_.getReportingContext();
      this.reportingEnabled = reportingcontext.sender().isEnabled();
      this.chatReportable = p_243297_;
      this.refreshHasDraftReport(reportingcontext);
      Component component = Component.translatable("gui.socialInteractions.narration.hide", p_243311_);
      Component component1 = Component.translatable("gui.socialInteractions.narration.show", p_243311_);
      PlayerSocialManager playersocialmanager = p_243293_.getPlayerSocialManager();
      boolean flag = p_243293_.getChatStatus().isChatAllowed(p_243293_.isLocalServer());
      boolean flag1 = !p_243293_.player.getUUID().equals(p_243288_);
      if (SharedConstants.DEBUG_SOCIAL_INTERACTIONS || flag1 && flag && !playersocialmanager.isBlocked(p_243288_)) {
         this.reportButton = new ImageButton(
            0,
            0,
            20,
            20,
            REPORT_BUTTON_SPRITES,
            p_238875_ -> reportingcontext.draftReportHandled(
               p_243293_, p_243214_, () -> p_243293_.setScreen(new ReportPlayerScreen(p_243214_, reportingcontext, this)), false
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
         this.hideButton = new ImageButton(0, 0, 20, 20, MUTE_BUTTON_SPRITES, p_100612_ -> {
            playersocialmanager.hidePlayer(p_243288_);
            this.onHiddenOrShown(true, Component.translatable("gui.socialInteractions.hidden_in_chat", p_243311_));
         }, Component.translatable("gui.socialInteractions.hide")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.hideButton.setTooltip(Tooltip.create(HIDE_TEXT_TOOLTIP, component));
         this.hideButton.setTooltipDelay(TOOLTIP_DELAY);
         this.showButton = new ImageButton(0, 0, 20, 20, UNMUTE_BUTTON_SPRITES, p_170074_ -> {
            playersocialmanager.showPlayer(p_243288_);
            this.onHiddenOrShown(false, Component.translatable("gui.socialInteractions.shown_in_chat", p_243311_));
         }, Component.translatable("gui.socialInteractions.show")) {
            @Override
            protected MutableComponent createNarrationMessage() {
               return PlayerEntry.this.getEntryNarationMessage(super.createNarrationMessage());
            }
         };
         this.showButton.setTooltip(Tooltip.create(SHOW_TEXT_TOOLTIP, component1));
         this.showButton.setTooltipDelay(TOOLTIP_DELAY);
         this.children = new ArrayList<>();
         this.children.add(this.hideButton);
         this.children.add(this.reportButton);
         this.updateHideAndShowButton(playersocialmanager.isHidden(this.id));
      } else {
         this.children = ImmutableList.of();
      }
   }

   public void refreshHasDraftReport(ReportingContext p_406677_) {
      this.hasDraftReport = p_406677_.hasDraftReportFor(this.id);
   }

   private Tooltip createReportButtonTooltip() {
      return !this.reportingEnabled
         ? Tooltip.create(REPORT_DISABLED_TOOLTIP)
         : Tooltip.create(REPORT_PLAYER_TOOLTIP, Component.translatable("gui.socialInteractions.narration.report", this.playerName));
   }

   @Override
   public void renderContent(GuiGraphics p_431504_, int p_425199_, int p_424281_, boolean p_422476_, float p_428030_) {
      int i = this.getContentX() + 4;
      int j = this.getContentY() + (this.getContentHeight() - 24) / 2;
      int k = i + 24 + 4;
      Component component = this.getStatusComponent();
      int l;
      if (component == CommonComponents.EMPTY) {
         p_431504_.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL);
         l = this.getContentY() + (this.getContentHeight() - 9) / 2;
      } else {
         p_431504_.fill(this.getContentX(), this.getContentY(), this.getContentRight(), this.getContentBottom(), BG_FILL_REMOVED);
         l = this.getContentY() + (this.getContentHeight() - 18) / 2;
         p_431504_.drawString(this.minecraft.font, component, k, l + 12, PLAYER_STATUS_COLOR);
      }

      PlayerFaceRenderer.draw(p_431504_, this.skinGetter.get(), i, j, 24);
      p_431504_.drawString(this.minecraft.font, this.playerName, k, l, PLAYERNAME_COLOR);
      if (this.isRemoved) {
         p_431504_.fill(i, j, i + 24, j + 24, SKIN_SHADE);
      }

      if (this.hideButton != null && this.showButton != null && this.reportButton != null) {
         float f = this.tooltipHoverTime;
         this.hideButton.setX(this.getContentX() + (this.getContentWidth() - this.hideButton.getWidth() - 4) - 20 - 4);
         this.hideButton.setY(this.getContentY() + (this.getContentHeight() - this.hideButton.getHeight()) / 2);
         this.hideButton.render(p_431504_, p_425199_, p_424281_, p_428030_);
         this.showButton.setX(this.getContentX() + (this.getContentWidth() - this.showButton.getWidth() - 4) - 20 - 4);
         this.showButton.setY(this.getContentY() + (this.getContentHeight() - this.showButton.getHeight()) / 2);
         this.showButton.render(p_431504_, p_425199_, p_424281_, p_428030_);
         this.reportButton.setX(this.getContentX() + (this.getContentWidth() - this.showButton.getWidth() - 4));
         this.reportButton.setY(this.getContentY() + (this.getContentHeight() - this.showButton.getHeight()) / 2);
         this.reportButton.render(p_431504_, p_425199_, p_424281_, p_428030_);
         if (f == this.tooltipHoverTime) {
            this.tooltipHoverTime = 0.0F;
         }
      }

      if (this.hasDraftReport && this.reportButton != null) {
         p_431504_.blitSprite(RenderPipelines.GUI_TEXTURED, DRAFT_REPORT_SPRITE, this.reportButton.getX() + 5, this.reportButton.getY() + 1, 15, 15);
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

   public void setRemoved(boolean p_100620_) {
      this.isRemoved = p_100620_;
   }

   public boolean isRemoved() {
      return this.isRemoved;
   }

   public void setHasRecentMessages(boolean p_240771_) {
      this.hasRecentMessages = p_240771_;
   }

   public boolean hasRecentMessages() {
      return this.hasRecentMessages;
   }

   public boolean isChatReportable() {
      return this.chatReportable;
   }

   private void onHiddenOrShown(boolean p_100597_, Component p_100598_) {
      this.updateHideAndShowButton(p_100597_);
      this.minecraft.gui.getChat().addMessage(p_100598_);
      this.minecraft.getNarrator().saySystemNow(p_100598_);
   }

   private void updateHideAndShowButton(boolean p_262638_) {
      this.showButton.visible = p_262638_;
      this.hideButton.visible = !p_262638_;
      this.children.set(0, p_262638_ ? this.showButton : this.hideButton);
   }

   MutableComponent getEntryNarationMessage(MutableComponent p_100595_) {
      Component component = this.getStatusComponent();
      return component == CommonComponents.EMPTY
         ? Component.literal(this.playerName).append(", ").append(p_100595_)
         : Component.literal(this.playerName).append(", ").append(component).append(", ").append(p_100595_);
   }

   private Component getStatusComponent() {
      boolean flag = this.minecraft.getPlayerSocialManager().isHidden(this.id);
      boolean flag1 = this.minecraft.getPlayerSocialManager().isBlocked(this.id);
      if (flag1 && this.isRemoved) {
         return BLOCKED_OFFLINE;
      } else if (flag && this.isRemoved) {
         return HIDDEN_OFFLINE;
      } else if (flag1) {
         return BLOCKED;
      } else if (flag) {
         return HIDDEN;
      } else {
         return this.isRemoved ? OFFLINE : CommonComponents.EMPTY;
      }
   }
}
