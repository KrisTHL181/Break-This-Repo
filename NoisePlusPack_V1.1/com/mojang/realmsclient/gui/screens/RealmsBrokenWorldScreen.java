package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBrokenWorldScreen extends RealmsScreen {
   private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private final Screen lastScreen;
   private @Nullable RealmsServer serverData;
   private final long serverId;
   private final Component[] message = new Component[]{
      Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")
   };
   private int leftX;
   private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
   private int animTick;

   public RealmsBrokenWorldScreen(Screen p_88296_, long p_88298_, boolean p_88299_) {
      super(p_88299_ ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
      this.lastScreen = p_88296_;
      this.serverId = p_88298_;
   }

   @Override
   public void init() {
      this.leftX = this.width / 2 - 150;
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, p_296045_ -> this.onClose()).bounds((this.width - 150) / 2, row(13) - 5, 150, 20).build()
      );
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      } else {
         this.addButtons();
      }
   }

   @Override
   public Component getNarrationMessage() {
      return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
   }

   private void addButtons() {
      for (Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
         int i = entry.getKey();
         boolean flag = i != this.serverData.activeSlot || this.serverData.isMinigameActive();
         Button button;
         if (flag) {
            button = Button.builder(
                  Component.translatable("mco.brokenworld.play"),
                  p_308044_ -> this.minecraft
                     .setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, i, this::doSwitchOrReset)))
               )
               .bounds(this.getFramePositionX(i), row(8), 80, 20)
               .build();
            button.active = !this.serverData.slots.get(i).options.empty;
         } else {
            button = Button.builder(
                  Component.translatable("mco.brokenworld.download"),
                  p_340712_ -> this.minecraft
                     .setScreen(
                        RealmsPopups.infoPopupScreen(
                           this, Component.translatable("mco.configure.world.restore.download.question.line1"), p_340710_ -> this.downloadWorld(i)
                        )
                     )
               )
               .bounds(this.getFramePositionX(i), row(8), 80, 20)
               .build();
         }

         if (this.slotsThatHasBeenDownloaded.contains(i)) {
            button.active = false;
            button.setMessage(Component.translatable("mco.brokenworld.downloaded"));
         }

         this.addRenderableWidget(button);
      }
   }

   @Override
   public void tick() {
      this.animTick++;
   }

   @Override
   public void render(GuiGraphics p_282934_, int p_88317_, int p_88318_, float p_88319_) {
      super.render(p_282934_, p_88317_, p_88318_, p_88319_);
      p_282934_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);

      for (int i = 0; i < this.message.length; i++) {
         p_282934_.drawCenteredString(this.font, this.message[i], this.width / 2, row(-1) + 3 + i * 12, -6250336);
      }

      if (this.serverData != null) {
         for (Entry<Integer, RealmsSlot> entry : this.serverData.slots.entrySet()) {
            if (entry.getValue().options.templateImage != null && entry.getValue().options.templateId != -1L) {
               this.drawSlotFrame(
                  p_282934_,
                  this.getFramePositionX(entry.getKey()),
                  row(1) + 5,
                  p_88317_,
                  p_88318_,
                  this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
                  entry.getValue().options.getSlotName(entry.getKey()),
                  entry.getKey(),
                  entry.getValue().options.templateId,
                  entry.getValue().options.templateImage,
                  entry.getValue().options.empty
               );
            } else {
               this.drawSlotFrame(
                  p_282934_,
                  this.getFramePositionX(entry.getKey()),
                  row(1) + 5,
                  p_88317_,
                  p_88318_,
                  this.serverData.activeSlot == entry.getKey() && !this.isMinigame(),
                  entry.getValue().options.getSlotName(entry.getKey()),
                  entry.getKey(),
                  -1L,
                  null,
                  entry.getValue().options.empty
               );
            }
         }
      }
   }

   private int getFramePositionX(int p_88302_) {
      return this.leftX + (p_88302_ - 1) * 110;
   }

   public Screen createErrorScreen(RealmsServiceException p_409209_) {
      return new RealmsGenericErrorScreen(p_409209_, this.lastScreen);
   }

   private void fetchServerData(long p_88314_) {
      RealmsUtil.<RealmsServer>supplyAsync(
            p_404756_ -> p_404756_.getOwnRealm(p_88314_), RealmsUtil.openScreenAndLogOnFailure(this::createErrorScreen, "Couldn't get own world")
         )
         .thenAcceptAsync(p_404759_ -> {
            this.serverData = p_404759_;
            this.addButtons();
         }, this.minecraft);
   }

   public void doSwitchOrReset() {
      new Thread(
            () -> {
               RealmsClient realmsclient = RealmsClient.getOrCreate();
               if (this.serverData.state == RealmsServer.State.CLOSED) {
                  this.minecraft
                     .execute(
                        () -> this.minecraft
                           .setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, true, this.minecraft)))
                     );
               } else {
                  try {
                     RealmsServer realmsserver = realmsclient.getOwnRealm(this.serverId);
                     this.minecraft.execute(() -> RealmsMainScreen.play(realmsserver, this));
                  } catch (RealmsServiceException realmsserviceexception) {
                     LOGGER.error("Couldn't get own world", realmsserviceexception);
                     this.minecraft.execute(() -> this.minecraft.setScreen(this.createErrorScreen(realmsserviceexception)));
                  }
               }
            }
         )
         .start();
   }

   private void downloadWorld(int p_88336_) {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      try {
         WorldDownload worlddownload = realmsclient.requestDownloadInfo(this.serverData.id, p_88336_);
         RealmsDownloadLatestWorldScreen realmsdownloadlatestworldscreen = new RealmsDownloadLatestWorldScreen(
            this, worlddownload, this.serverData.getWorldName(p_88336_), p_420600_ -> {
               if (p_420600_) {
                  this.slotsThatHasBeenDownloaded.add(p_88336_);
                  this.clearWidgets();
                  this.addButtons();
               } else {
                  this.minecraft.setScreen(this);
               }
            }
         );
         this.minecraft.setScreen(realmsdownloadlatestworldscreen);
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't download world data", realmsserviceexception);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.isMinigameActive();
   }

   private void drawSlotFrame(
      GuiGraphics p_281929_,
      int p_283393_,
      int p_281553_,
      int p_283523_,
      int p_282823_,
      boolean p_283032_,
      String p_283498_,
      int p_283330_,
      long p_283588_,
      @Nullable String p_282484_,
      boolean p_282283_
   ) {
      Identifier identifier;
      if (p_282283_) {
         identifier = RealmsWorldSlotButton.EMPTY_SLOT_LOCATION;
      } else if (p_282484_ != null && p_283588_ != -1L) {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(p_283588_), p_282484_);
      } else if (p_283330_ == 1) {
         identifier = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1;
      } else if (p_283330_ == 2) {
         identifier = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2;
      } else if (p_283330_ == 3) {
         identifier = RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3;
      } else {
         identifier = RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage);
      }

      if (p_283032_) {
         float f = 0.9F + 0.1F * Mth.cos(this.animTick * 0.2F);
         p_281929_.blit(
            RenderPipelines.GUI_TEXTURED, identifier, p_283393_ + 3, p_281553_ + 3, 0.0F, 0.0F, 74, 74, 74, 74, 74, 74, ARGB.colorFromFloat(1.0F, f, f, f)
         );
         p_281929_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, p_283393_, p_281553_, 80, 80);
      } else {
         int i = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
         p_281929_.blit(RenderPipelines.GUI_TEXTURED, identifier, p_283393_ + 3, p_281553_ + 3, 0.0F, 0.0F, 74, 74, 74, 74, 74, 74, i);
         p_281929_.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, p_283393_, p_281553_, 80, 80, i);
      }

      p_281929_.drawCenteredString(this.font, p_283498_, p_283393_ + 40, p_281553_ + 66, -1);
   }
}
