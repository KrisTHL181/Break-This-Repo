package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsNotificationsScreen extends RealmsScreen {
   private static final Identifier UNSEEN_NOTIFICATION_SPRITE = Identifier.withDefaultNamespace("icon/unseen_notification");
   private static final Identifier NEWS_SPRITE = Identifier.withDefaultNamespace("icon/news");
   private static final Identifier INVITE_SPRITE = Identifier.withDefaultNamespace("icon/invite");
   private static final Identifier TRIAL_AVAILABLE_SPRITE = Identifier.withDefaultNamespace("icon/trial_available");
   private final CompletableFuture<Boolean> validClient = RealmsAvailability.get().thenApply(p_296063_ -> p_296063_.type() == RealmsAvailability.Type.SUCCESS);
   private DataFetcher.@Nullable Subscription realmsDataSubscription;
   private RealmsNotificationsScreen.@Nullable DataFetcherConfiguration currentConfiguration;
   private volatile int numberOfPendingInvites;
   private static boolean trialAvailable;
   private static boolean hasUnreadNews;
   private static boolean hasUnseenNotifications;
   private final RealmsNotificationsScreen.DataFetcherConfiguration showAll = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_301294_) {
         DataFetcher.Subscription datafetcher$subscription = p_301294_.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNewsAndInvitesSubscriptions(p_301294_, datafetcher$subscription);
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(p_301294_, datafetcher$subscription);
         return datafetcher$subscription;
      }

      @Override
      public boolean showOldNotifications() {
         return true;
      }
   };
   private final RealmsNotificationsScreen.DataFetcherConfiguration onlyNotifications = new RealmsNotificationsScreen.DataFetcherConfiguration() {
      @Override
      public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_275318_) {
         DataFetcher.Subscription datafetcher$subscription = p_275318_.dataFetcher.createSubscription();
         RealmsNotificationsScreen.this.addNotificationsSubscriptions(p_275318_, datafetcher$subscription);
         return datafetcher$subscription;
      }

      @Override
      public boolean showOldNotifications() {
         return false;
      }
   };

   public RealmsNotificationsScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public void init() {
      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.forceUpdate();
      }
   }

   @Override
   public void added() {
      super.added();
      this.minecraft.realmsDataFetcher().notificationsTask.reset();
   }

   private RealmsNotificationsScreen.@Nullable DataFetcherConfiguration getConfiguration() {
      boolean flag = this.inTitleScreen() && this.validClient.getNow(false);
      if (!flag) {
         return null;
      } else {
         return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
      }
   }

   @Override
   public void tick() {
      RealmsNotificationsScreen.DataFetcherConfiguration realmsnotificationsscreen$datafetcherconfiguration = this.getConfiguration();
      if (!Objects.equals(this.currentConfiguration, realmsnotificationsscreen$datafetcherconfiguration)) {
         this.currentConfiguration = realmsnotificationsscreen$datafetcherconfiguration;
         if (this.currentConfiguration != null) {
            this.realmsDataSubscription = this.currentConfiguration.initDataFetcher(this.minecraft.realmsDataFetcher());
         } else {
            this.realmsDataSubscription = null;
         }
      }

      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.tick();
      }
   }

   private boolean getRealmsNotificationsEnabled() {
      return this.minecraft.options.realmsNotifications().get();
   }

   private boolean inTitleScreen() {
      return this.minecraft.screen instanceof TitleScreen;
   }

   @Override
   public void render(GuiGraphics p_282587_, int p_282992_, int p_283028_, float p_281605_) {
      super.render(p_282587_, p_282992_, p_283028_, p_281605_);
      if (this.validClient.getNow(false)) {
         this.drawIcons(p_282587_);
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_300621_, int p_300416_, int p_300236_, float p_299573_) {
   }

   private void drawIcons(GuiGraphics p_282966_) {
      int i = this.numberOfPendingInvites;
      int j = 24;
      int k = this.height / 4 + 48;
      int l = this.width / 2 + 100;
      int i1 = k + 48 + 2;
      int j1 = l - 3;
      if (hasUnseenNotifications) {
         p_282966_.blitSprite(RenderPipelines.GUI_TEXTURED, UNSEEN_NOTIFICATION_SPRITE, j1 - 12, i1 + 3, 10, 10);
         j1 -= 16;
      }

      if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
         if (hasUnreadNews) {
            p_282966_.blitSprite(RenderPipelines.GUI_TEXTURED, NEWS_SPRITE, j1 - 14, i1 + 1, 14, 14);
            j1 -= 16;
         }

         if (i != 0) {
            p_282966_.blitSprite(RenderPipelines.GUI_TEXTURED, INVITE_SPRITE, j1 - 14, i1 + 1, 14, 14);
            j1 -= 16;
         }

         if (trialAvailable) {
            p_282966_.blitSprite(RenderPipelines.GUI_TEXTURED, TRIAL_AVAILABLE_SPRITE, j1 - 10, i1 + 4, 8, 8);
         }
      }
   }

   void addNewsAndInvitesSubscriptions(RealmsDataFetcher p_275490_, DataFetcher.Subscription p_275623_) {
      p_275623_.subscribe(p_275490_.pendingInvitesTask, p_239521_ -> this.numberOfPendingInvites = p_239521_);
      p_275623_.subscribe(p_275490_.trialAvailabilityTask, p_239494_ -> trialAvailable = p_239494_);
      p_275623_.subscribe(p_275490_.newsTask, p_238946_ -> {
         p_275490_.newsManager.updateUnreadNews(p_238946_);
         hasUnreadNews = p_275490_.newsManager.hasUnreadNews();
      });
   }

   void addNotificationsSubscriptions(RealmsDataFetcher p_275619_, DataFetcher.Subscription p_275628_) {
      p_275628_.subscribe(p_275619_.notificationsTask, p_274637_ -> {
         hasUnseenNotifications = false;

         for (RealmsNotification realmsnotification : p_274637_) {
            if (!realmsnotification.seen()) {
               hasUnseenNotifications = true;
               break;
            }
         }
      });
   }

   @OnlyIn(Dist.CLIENT)
   interface DataFetcherConfiguration {
      DataFetcher.Subscription initDataFetcher(RealmsDataFetcher var1);

      boolean showOldNotifications();
   }
}
