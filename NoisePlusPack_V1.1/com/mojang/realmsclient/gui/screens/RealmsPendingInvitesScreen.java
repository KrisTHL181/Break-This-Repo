package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPendingInvitesScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
   private final Screen lastScreen;
   private final CompletableFuture<List<PendingInvite>> pendingInvites = CompletableFuture.supplyAsync(() -> {
      try {
         return RealmsClient.getOrCreate().pendingInvites().pendingInvites();
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't list invites", realmsserviceexception);
         return List.of();
      }
   }, Util.ioPool());
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   RealmsPendingInvitesScreen.@Nullable PendingInvitationSelectionList pendingInvitationSelectionList;

   public RealmsPendingInvitesScreen(Screen p_279260_, Component p_279122_) {
      super(p_279122_);
      this.lastScreen = p_279260_;
   }

   @Override
   public void init() {
      RealmsMainScreen.refreshPendingInvites();
      this.layout.addTitleHeader(this.title, this.font);
      this.pendingInvitationSelectionList = this.layout.addToContents(new RealmsPendingInvitesScreen.PendingInvitationSelectionList(this.minecraft));
      this.pendingInvites.thenAcceptAsync(p_404762_ -> {
         List<RealmsPendingInvitesScreen.Entry> list = p_404762_.stream().map(p_296073_ -> new RealmsPendingInvitesScreen.Entry(p_296073_)).toList();
         this.pendingInvitationSelectionList.replaceEntries(list);
         if (list.isEmpty()) {
            this.minecraft.getNarrator().saySystemQueued(NO_PENDING_INVITES_TEXT);
         }
      }, this.screenExecutor);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_296072_ -> this.onClose()).width(200).build());
      this.layout.visitWidgets(p_420602_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_420602_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.pendingInvitationSelectionList != null) {
         this.pendingInvitationSelectionList.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void render(GuiGraphics p_282787_, int p_88900_, int p_88901_, float p_88902_) {
      super.render(p_282787_, p_88900_, p_88901_, p_88902_);
      if (this.pendingInvites.isDone() && this.pendingInvitationSelectionList.hasPendingInvites()) {
         p_282787_.drawCenteredString(this.font, NO_PENDING_INVITES_TEXT, this.width / 2, this.height / 2 - 20, -1);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ContainerObjectSelectionList.Entry<RealmsPendingInvitesScreen.Entry> {
      private static final Component ACCEPT_INVITE = Component.translatable("mco.invites.button.accept");
      private static final Component REJECT_INVITE = Component.translatable("mco.invites.button.reject");
      private static final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(
         Identifier.withDefaultNamespace("pending_invite/accept"), Identifier.withDefaultNamespace("pending_invite/accept_highlighted")
      );
      private static final WidgetSprites REJECT_SPRITE = new WidgetSprites(
         Identifier.withDefaultNamespace("pending_invite/reject"), Identifier.withDefaultNamespace("pending_invite/reject_highlighted")
      );
      private static final int SPRITE_TEXTURE_SIZE = 18;
      private static final int SPRITE_SIZE = 21;
      private static final int TEXT_LEFT = 38;
      private final PendingInvite pendingInvite;
      private final List<AbstractWidget> children = new ArrayList<>();
      private final SpriteIconButton acceptButton;
      private final SpriteIconButton rejectButton;
      private final StringWidget realmName;
      private final StringWidget realmOwnerName;
      private final StringWidget inviteDate;

      Entry(final PendingInvite p_88996_) {
         this.pendingInvite = p_88996_;
         int i = RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.getRowWidth() - 32 - 32 - 42;
         this.realmName = new StringWidget(Component.literal(p_88996_.realmName()), RealmsPendingInvitesScreen.this.font).setMaxWidth(i);
         this.realmOwnerName = new StringWidget(Component.literal(p_88996_.realmOwnerName()).withColor(-6250336), RealmsPendingInvitesScreen.this.font)
            .setMaxWidth(i);
         this.inviteDate = new StringWidget(
               ComponentUtils.mergeStyles(RealmsUtil.convertToAgePresentationFromInstant(p_88996_.date()), Style.EMPTY.withColor(-6250336)),
               RealmsPendingInvitesScreen.this.font
            )
            .setMaxWidth(i);
         Button.CreateNarration button$createnarration = this.getCreateNarration(p_88996_);
         this.acceptButton = SpriteIconButton.builder(ACCEPT_INVITE, p_427450_ -> this.handleInvitation(true), false)
            .sprite(ACCEPT_SPRITE, 18, 18)
            .size(21, 21)
            .narration(button$createnarration)
            .withTootip()
            .build();
         this.rejectButton = SpriteIconButton.builder(REJECT_INVITE, p_424740_ -> this.handleInvitation(false), false)
            .sprite(REJECT_SPRITE, 18, 18)
            .size(21, 21)
            .narration(button$createnarration)
            .withTootip()
            .build();
         this.children.addAll(List.of(this.acceptButton, this.rejectButton));
      }

      private Button.CreateNarration getCreateNarration(PendingInvite p_430008_) {
         return p_447748_ -> {
            MutableComponent mutablecomponent = CommonComponents.joinForNarration(
               p_447748_.get(),
               Component.literal(p_430008_.realmName()),
               Component.literal(p_430008_.realmOwnerName()),
               RealmsUtil.convertToAgePresentationFromInstant(p_430008_.date())
            );
            return Component.translatable("narrator.select", mutablecomponent);
         };
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      @Override
      public void renderContent(GuiGraphics p_424966_, int p_423896_, int p_430835_, boolean p_428558_, float p_429107_) {
         int i = this.getContentX();
         int j = this.getContentY();
         int k = i + 38;
         RealmsUtil.renderPlayerFace(p_424966_, i, j, 32, this.pendingInvite.realmOwnerUuid());
         this.realmName.setPosition(k, j + 1);
         this.realmName.renderWidget(p_424966_, p_423896_, p_430835_, i);
         this.realmOwnerName.setPosition(k, j + 12);
         this.realmOwnerName.renderWidget(p_424966_, p_423896_, p_430835_, i);
         this.inviteDate.setPosition(k, j + 24);
         this.inviteDate.renderWidget(p_424966_, p_423896_, p_430835_, i);
         int l = j + this.getContentHeight() / 2 - 10;
         this.acceptButton.setPosition(i + this.getContentWidth() - 16 - 42, l);
         this.acceptButton.render(p_424966_, p_423896_, p_430835_, p_429107_);
         this.rejectButton.setPosition(i + this.getContentWidth() - 8 - 21, l);
         this.rejectButton.render(p_424966_, p_423896_, p_430835_, p_429107_);
      }

      private void handleInvitation(boolean p_427736_) {
         String s = this.pendingInvite.invitationId();
         CompletableFuture.<Boolean>supplyAsync(() -> {
            try {
               RealmsClient realmsclient = RealmsClient.getOrCreate();
               if (p_427736_) {
                  realmsclient.acceptInvitation(s);
               } else {
                  realmsclient.rejectInvitation(s);
               }

               return true;
            } catch (RealmsServiceException realmsserviceexception) {
               RealmsPendingInvitesScreen.LOGGER.error("Couldn't handle invite", realmsserviceexception);
               return false;
            }
         }, Util.ioPool()).thenAcceptAsync(p_430244_ -> {
            if (p_430244_) {
               RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.removeInvitation(this);
               RealmsDataFetcher realmsdatafetcher = RealmsPendingInvitesScreen.this.minecraft.realmsDataFetcher();
               if (p_427736_) {
                  realmsdatafetcher.serverListUpdateTask.reset();
               }

               realmsdatafetcher.pendingInvitesTask.reset();
            }
         }, RealmsPendingInvitesScreen.this.screenExecutor);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class PendingInvitationSelectionList extends ContainerObjectSelectionList<RealmsPendingInvitesScreen.Entry> {
      public static final int ITEM_HEIGHT = 36;

      public PendingInvitationSelectionList(final Minecraft p_423423_) {
         super(
            p_423423_,
            RealmsPendingInvitesScreen.this.width,
            RealmsPendingInvitesScreen.this.layout.getContentHeight(),
            RealmsPendingInvitesScreen.this.layout.getHeaderHeight(),
            36
         );
      }

      @Override
      public int getRowWidth() {
         return 280;
      }

      public boolean hasPendingInvites() {
         return this.getItemCount() == 0;
      }

      public void removeInvitation(RealmsPendingInvitesScreen.Entry p_367374_) {
         this.removeEntry(p_367374_);
      }
   }
}
