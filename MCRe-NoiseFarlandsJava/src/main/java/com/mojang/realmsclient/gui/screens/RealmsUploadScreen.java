package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.client.worldupload.RealmsUploadException;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUpload;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUploadStatusTracker;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadScreen extends RealmsScreen implements RealmsWorldUploadStatusTracker {
    private static final int BAR_WIDTH = 200;
    private static final int BAR_TOP = 80;
    private static final int BAR_BOTTOM = 95;
    private static final int BAR_BORDER = 1;
    private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
    private static final Component VERIFYING_TEXT = Component.translatable("mco.upload.verifying");
    private final RealmsResetWorldScreen lastScreen;
    private final LevelSummary selectedLevel;
    private final @Nullable RealmCreationTask realmCreationTask;
    private final long realmId;
    private final int slotId;
    private final AtomicReference<@Nullable RealmsWorldUpload> currentUpload = new AtomicReference<>();
    private final UploadStatus uploadStatus;
    private final RateLimiter narrationRateLimiter;
    private volatile Component @Nullable [] errorMessage;
    private volatile Component status = Component.translatable("mco.upload.preparing");
    private volatile @Nullable String progress;
    private volatile boolean cancelled;
    private volatile boolean uploadFinished;
    private volatile boolean showDots = true;
    private volatile boolean uploadStarted;
    private @Nullable Button backButton;
    private @Nullable Button cancelButton;
    private int tickCount;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public RealmsUploadScreen(
        final @Nullable RealmCreationTask realmCreationTask,
        final long realmId,
        final int slotId,
        final RealmsResetWorldScreen lastScreen,
        final LevelSummary selectedLevel
    ) {
        super(GameNarrator.NO_TITLE);
        this.realmCreationTask = realmCreationTask;
        this.realmId = realmId;
        this.slotId = slotId;
        this.lastScreen = lastScreen;
        this.selectedLevel = selectedLevel;
        this.uploadStatus = new UploadStatus();
        this.narrationRateLimiter = RateLimiter.create(0.1F);
    }

    @Override
    public void init() {
        this.backButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onBack()).build());
        this.backButton.visible = false;
        this.cancelButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).build());
        if (!this.uploadStarted) {
            if (this.lastScreen.slot == -1) {
                this.uploadStarted = true;
                this.upload();
            } else {
                List<LongRunningTask> tasks = new ArrayList<>();
                if (this.realmCreationTask != null) {
                    tasks.add(this.realmCreationTask);
                }

                tasks.add(new SwitchSlotTask(this.realmId, this.lastScreen.slot, () -> {
                    if (!this.uploadStarted) {
                        this.uploadStarted = true;
                        this.minecraft.execute(() -> {
                            this.minecraft.gui.setScreen(this);
                            this.upload();
                        });
                    }
                }));
                this.minecraft.gui.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, tasks.toArray(new LongRunningTask[0])));
            }
        }

        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    private void onBack() {
        this.minecraft.gui.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.realmId));
    }

    private void onCancel() {
        this.cancelled = true;
        RealmsWorldUpload realmsWorldUpload = this.currentUpload.get();
        if (realmsWorldUpload != null) {
            realmsWorldUpload.cancel();
        } else {
            this.minecraft.gui.setScreen(this.lastScreen);
        }
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (event.isEscape()) {
            if (this.showDots) {
                this.onCancel();
            } else {
                this.onBack();
            }

            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int xm, final int ym, final float a) {
        super.extractRenderState(graphics, xm, ym, a);
        if (!this.uploadFinished && this.uploadStatus.uploadStarted() && this.uploadStatus.uploadCompleted() && this.cancelButton != null) {
            this.status = VERIFYING_TEXT;
            this.cancelButton.active = false;
        }

        graphics.centeredText(this.font, this.status, this.width / 2, 50, -1);
        if (this.showDots) {
            graphics.text(this.font, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + this.font.width(this.status) / 2 + 5, 50, -1);
        }

        if (this.uploadStatus.uploadStarted() && !this.cancelled) {
            this.extractProgressBar(graphics);
            this.extractUploadSpeed(graphics);
        }

        Component[] errorMessages = this.errorMessage;
        if (errorMessages != null) {
            for (int i = 0; i < errorMessages.length; i++) {
                graphics.centeredText(this.font, errorMessages[i], this.width / 2, 110 + 12 * i, -65536);
            }
        }
    }

    private void extractProgressBar(final GuiGraphicsExtractor graphics) {
        double percentage = this.uploadStatus.getPercentage();
        this.progress = String.format(Locale.ROOT, "%.1f", percentage * 100.0);
        int left = (this.width - 200) / 2;
        int right = left + (int)Math.round(200.0 * percentage);
        graphics.fill(left - 1, 79, right + 1, 96, -1);
        graphics.fill(left, 80, right, 95, -8355712);
        graphics.centeredText(this.font, Component.translatable("mco.upload.percent", this.progress), this.width / 2, 84, -1);
    }

    private void extractUploadSpeed(final GuiGraphicsExtractor graphics) {
        this.extractUploadSpeed0(graphics, this.uploadStatus.getBytesPerSecond());
    }

    private void extractUploadSpeed0(final GuiGraphicsExtractor graphics, final long bytesPerSecond) {
        String uploadProgress = this.progress;
        if (bytesPerSecond > 0L && uploadProgress != null) {
            int progressLength = this.font.width(uploadProgress);
            String stringPresentation = "(" + Unit.humanReadable(bytesPerSecond) + "/s)";
            graphics.text(this.font, stringPresentation, this.width / 2 + progressLength / 2 + 15, 84, -1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCount++;
        this.uploadStatus.refreshBytesPerSecond();
        if (this.narrationRateLimiter.tryAcquire(1)) {
            Component message = this.createProgressNarrationMessage();
            this.minecraft.getNarrator().saySystemNow(message);
        }
    }

    private Component createProgressNarrationMessage() {
        List<Component> elements = Lists.newArrayList();
        elements.add(this.status);
        if (this.progress != null) {
            elements.add(Component.translatable("mco.upload.percent", this.progress));
        }

        Component[] errorMessages = this.errorMessage;
        if (errorMessages != null) {
            elements.addAll(Arrays.asList(errorMessages));
        }

        return CommonComponents.joinLines(elements);
    }

    private void upload() {
        Path worldFolder = this.minecraft.gameDirectory.toPath().resolve("saves").resolve(this.selectedLevel.getLevelId());
        RealmsWorldOptions worldOptions = RealmsWorldOptions.createFromSettings(
            this.selectedLevel.getSettings(), this.selectedLevel.levelVersion().minecraftVersionName()
        );
        RealmsSlot realmsSlot = new RealmsSlot(this.slotId, worldOptions, List.of(RealmsSetting.hardcoreSetting(this.selectedLevel.isHardcore())));
        RealmsWorldUpload newUpload = new RealmsWorldUpload(worldFolder, realmsSlot, this.minecraft.getUser(), this.realmId, this);
        if (!this.currentUpload.compareAndSet(null, newUpload)) {
            throw new IllegalStateException("Tried to start uploading but was already uploading");
        }

        newUpload.packAndUpload().handleAsync((result, exception) -> {
            if (exception != null) {
                if (exception instanceof CompletionException e) {
                    exception = e.getCause();
                }

                if (exception instanceof RealmsUploadException e) {
                    if (e.getStatusMessage() != null) {
                        this.status = e.getStatusMessage();
                    }

                    this.setErrorMessage(e.getErrorMessages());
                } else {
                    this.status = Component.translatable("mco.upload.failed", exception.getMessage());
                }
            } else {
                this.status = Component.translatable("mco.upload.done");
                if (this.backButton != null) {
                    this.backButton.setMessage(CommonComponents.GUI_DONE);
                }
            }

            this.uploadFinished = true;
            this.showDots = false;
            if (this.backButton != null) {
                this.backButton.visible = true;
            }

            if (this.cancelButton != null) {
                this.cancelButton.visible = false;
            }

            this.currentUpload.set(null);
            return null;
        }, this.minecraft);
    }

    private void setErrorMessage(final Component @Nullable ... messages) {
        this.errorMessage = messages;
    }

    @Override
    public UploadStatus getUploadStatus() {
        return this.uploadStatus;
    }

    @Override
    public void setUploading() {
        this.status = Component.translatable("mco.upload.uploading", this.selectedLevel.getLevelName());
    }
}