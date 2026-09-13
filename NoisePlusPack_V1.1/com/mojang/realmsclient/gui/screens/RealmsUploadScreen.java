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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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
   final AtomicReference<@Nullable RealmsWorldUpload> currentUpload = new AtomicReference<>();
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

   public RealmsUploadScreen(@Nullable RealmCreationTask p_332847_, long p_90083_, int p_90084_, RealmsResetWorldScreen p_90085_, LevelSummary p_90086_) {
      super(GameNarrator.NO_TITLE);
      this.realmCreationTask = p_332847_;
      this.realmId = p_90083_;
      this.slotId = p_90084_;
      this.lastScreen = p_90085_;
      this.selectedLevel = p_90086_;
      this.uploadStatus = new UploadStatus();
      this.narrationRateLimiter = RateLimiter.create(0.1F);
   }

   @Override
   public void init() {
      this.backButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_90118_ -> this.onBack()).build());
      this.backButton.visible = false;
      this.cancelButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, p_90104_ -> this.onCancel()).build());
      if (!this.uploadStarted) {
         if (this.lastScreen.slot == -1) {
            this.uploadStarted = true;
            this.upload();
         } else {
            List<LongRunningTask> list = new ArrayList<>();
            if (this.realmCreationTask != null) {
               list.add(this.realmCreationTask);
            }

            list.add(new SwitchSlotTask(this.realmId, this.lastScreen.slot, () -> {
               if (!this.uploadStarted) {
                  this.uploadStarted = true;
                  this.minecraft.execute(() -> {
                     this.minecraft.setScreen(this);
                     this.upload();
                  });
               }
            }));
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, list.toArray(new LongRunningTask[0])));
         }
      }

      this.layout.visitWidgets(p_325163_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325163_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   private void onBack() {
      this.minecraft.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.realmId));
   }

   private void onCancel() {
      this.cancelled = true;
      RealmsWorldUpload realmsworldupload = this.currentUpload.get();
      if (realmsworldupload != null) {
         realmsworldupload.cancel();
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_429097_) {
      if (p_429097_.key() == 256) {
         if (this.showDots) {
            this.onCancel();
         } else {
            this.onBack();
         }

         return true;
      } else {
         return super.keyPressed(p_429097_);
      }
   }

   @Override
   public void render(GuiGraphics p_282140_, int p_90097_, int p_90098_, float p_90099_) {
      super.render(p_282140_, p_90097_, p_90098_, p_90099_);
      if (!this.uploadFinished && this.uploadStatus.uploadStarted() && this.uploadStatus.uploadCompleted() && this.cancelButton != null) {
         this.status = VERIFYING_TEXT;
         this.cancelButton.active = false;
      }

      p_282140_.drawCenteredString(this.font, this.status, this.width / 2, 50, -1);
      if (this.showDots) {
         p_282140_.drawString(this.font, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + this.font.width(this.status) / 2 + 5, 50, -1);
      }

      if (this.uploadStatus.uploadStarted() && !this.cancelled) {
         this.drawProgressBar(p_282140_);
         this.drawUploadSpeed(p_282140_);
      }

      Component[] acomponent = this.errorMessage;
      if (acomponent != null) {
         for (int i = 0; i < acomponent.length; i++) {
            p_282140_.drawCenteredString(this.font, acomponent[i], this.width / 2, 110 + 12 * i, -65536);
         }
      }
   }

   private void drawProgressBar(GuiGraphics p_282575_) {
      double d0 = this.uploadStatus.getPercentage();
      this.progress = String.format(Locale.ROOT, "%.1f", d0 * 100.0);
      int i = (this.width - 200) / 2;
      int j = i + (int)Math.round(200.0 * d0);
      p_282575_.fill(i - 1, 79, j + 1, 96, -1);
      p_282575_.fill(i, 80, j, 95, -8355712);
      p_282575_.drawCenteredString(this.font, Component.translatable("mco.upload.percent", this.progress), this.width / 2, 84, -1);
   }

   private void drawUploadSpeed(GuiGraphics p_281884_) {
      this.drawUploadSpeed0(p_281884_, this.uploadStatus.getBytesPerSecond());
   }

   private void drawUploadSpeed0(GuiGraphics p_282279_, long p_282827_) {
      String s = this.progress;
      if (p_282827_ > 0L && s != null) {
         int i = this.font.width(s);
         String s1 = "(" + Unit.humanReadable(p_282827_) + "/s)";
         p_282279_.drawString(this.font, s1, this.width / 2 + i / 2 + 15, 84, -1);
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.tickCount++;
      this.uploadStatus.refreshBytesPerSecond();
      if (this.narrationRateLimiter.tryAcquire(1)) {
         Component component = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().saySystemNow(component);
      }
   }

   private Component createProgressNarrationMessage() {
      List<Component> list = Lists.newArrayList();
      list.add(this.status);
      if (this.progress != null) {
         list.add(Component.translatable("mco.upload.percent", this.progress));
      }

      Component[] acomponent = this.errorMessage;
      if (acomponent != null) {
         list.addAll(Arrays.asList(acomponent));
      }

      return CommonComponents.joinLines(list);
   }

   private void upload() {
      Path path = this.minecraft.gameDirectory.toPath().resolve("saves").resolve(this.selectedLevel.getLevelId());
      RealmsWorldOptions realmsworldoptions = RealmsWorldOptions.createFromSettings(
         this.selectedLevel.getSettings(), this.selectedLevel.levelVersion().minecraftVersionName()
      );
      RealmsSlot realmsslot = new RealmsSlot(
         this.slotId, realmsworldoptions, List.of(RealmsSetting.hardcoreSetting(this.selectedLevel.getSettings().hardcore()))
      );
      RealmsWorldUpload realmsworldupload = new RealmsWorldUpload(path, realmsslot, this.minecraft.getUser(), this.realmId, this);
      if (!this.currentUpload.compareAndSet(null, realmsworldupload)) {
         throw new IllegalStateException("Tried to start uploading but was already uploading");
      }

      realmsworldupload.packAndUpload().handleAsync((p_357567_, p_357568_) -> {
         if (p_357568_ != null) {
            if (p_357568_ instanceof CompletionException completionexception) {
               p_357568_ = completionexception.getCause();
            }

            if (p_357568_ instanceof RealmsUploadException realmsuploadexception) {
               if (realmsuploadexception.getStatusMessage() != null) {
                  this.status = realmsuploadexception.getStatusMessage();
               }

               this.setErrorMessage(realmsuploadexception.getErrorMessages());
            } else {
               this.status = Component.translatable("mco.upload.failed", p_357568_.getMessage());
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

   private void setErrorMessage(Component @Nullable ... p_90113_) {
      this.errorMessage = p_90113_;
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
