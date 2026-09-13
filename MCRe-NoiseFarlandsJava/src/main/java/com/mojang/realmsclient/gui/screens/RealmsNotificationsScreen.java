package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private final CompletableFuture<Boolean> validClient = RealmsAvailability.get().thenApply(result -> result.type() == RealmsAvailability.Type.SUCCESS);
    private DataFetcher.@Nullable Subscription realmsDataSubscription;
    private RealmsNotificationsScreen.@Nullable DataFetcherConfiguration currentConfiguration;
    private volatile int numberOfPendingInvites;
    private static boolean trialAvailable;
    private static boolean hasUnreadNews;
    private static boolean hasUnseenNotifications;
    private final RealmsNotificationsScreen.DataFetcherConfiguration showAll = new RealmsNotificationsScreen.DataFetcherConfiguration() {
        @Override
        public DataFetcher.Subscription initDataFetcher(final RealmsDataFetcher dataSource) {
            DataFetcher.Subscription result = dataSource.dataFetcher.createSubscription();
            RealmsNotificationsScreen.this.addNewsAndInvitesSubscriptions(dataSource, result);
            RealmsNotificationsScreen.this.addNotificationsSubscriptions(dataSource, result);
            return result;
        }

        @Override
        public boolean showOldNotifications() {
            return true;
        }
    };
    private final RealmsNotificationsScreen.DataFetcherConfiguration onlyNotifications = new RealmsNotificationsScreen.DataFetcherConfiguration() {
        @Override
        public DataFetcher.Subscription initDataFetcher(final RealmsDataFetcher dataSource) {
            DataFetcher.Subscription result = dataSource.dataFetcher.createSubscription();
            RealmsNotificationsScreen.this.addNotificationsSubscriptions(dataSource, result);
            return result;
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
        boolean realmsEnabled = this.inTitleScreen() && this.validClient.getNow(false);
        if (!realmsEnabled) {
            return null;
        } else {
            return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
        }
    }

    @Override
    public void tick() {
        RealmsNotificationsScreen.DataFetcherConfiguration dataFetcherConfiguration = this.getConfiguration();
        if (!Objects.equals(this.currentConfiguration, dataFetcherConfiguration)) {
            this.currentConfiguration = dataFetcherConfiguration;
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
        return this.minecraft.gui.screen() instanceof TitleScreen;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (this.validClient.getNow(false)) {
            this.extractIcons(graphics);
        }
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
    }

    private void extractIcons(final GuiGraphicsExtractor graphics) {
        int pendingInvitesCount = this.numberOfPendingInvites;
        int spacing = 24;
        int topPos = this.height / 4 + 48;
        int buttonRight = this.width / 2 + 100;
        int baseY = topPos + 48 + 2;
        int iconRight = buttonRight - 3;
        if (hasUnseenNotifications) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, UNSEEN_NOTIFICATION_SPRITE, iconRight - 12, baseY + 3, 10, 10);
            iconRight -= 16;
        }

        if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
            if (hasUnreadNews) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEWS_SPRITE, iconRight - 14, baseY + 1, 14, 14);
                iconRight -= 16;
            }

            if (pendingInvitesCount != 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INVITE_SPRITE, iconRight - 14, baseY + 1, 14, 14);
                iconRight -= 16;
            }

            if (trialAvailable) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRIAL_AVAILABLE_SPRITE, iconRight - 10, baseY + 4, 8, 8);
            }
        }
    }

    private void addNewsAndInvitesSubscriptions(final RealmsDataFetcher dataSource, final DataFetcher.Subscription result) {
        result.subscribe(dataSource.pendingInvitesTask, value -> this.numberOfPendingInvites = value);
        result.subscribe(dataSource.trialAvailabilityTask, value -> trialAvailable = value);
        result.subscribe(dataSource.newsTask, value -> {
            dataSource.newsManager.updateUnreadNews(value);
            hasUnreadNews = dataSource.newsManager.hasUnreadNews();
        });
    }

    private void addNotificationsSubscriptions(final RealmsDataFetcher dataSource, final DataFetcher.Subscription result) {
        result.subscribe(dataSource.notificationsTask, notifications -> {
            hasUnseenNotifications = false;

            for (RealmsNotification notification : notifications) {
                if (!notification.seen()) {
                    hasUnseenNotifications = true;
                    break;
                }
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private interface DataFetcherConfiguration {
        DataFetcher.Subscription initDataFetcher(RealmsDataFetcher realmsDataFetcher);

        boolean showOldNotifications();
    }
}