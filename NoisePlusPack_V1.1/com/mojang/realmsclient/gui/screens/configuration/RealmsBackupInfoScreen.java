package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsBackupInfoScreen extends RealmsScreen {
   private static final Component TITLE = Component.translatable("mco.backup.info.title");
   private static final Component UNKNOWN = Component.translatable("mco.backup.unknown");
   private final Screen lastScreen;
   final Backup backup;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private RealmsBackupInfoScreen.BackupInfoList backupInfoList;

   public RealmsBackupInfoScreen(Screen p_406449_, Backup p_409285_) {
      super(TITLE);
      this.lastScreen = p_406449_;
      this.backup = p_409285_;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.backupInfoList = this.layout.addToContents(new RealmsBackupInfoScreen.BackupInfoList(this.minecraft));
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, p_408203_ -> this.onClose()).build());
      this.repositionElements();
      this.layout.visitWidgets(p_408227_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_408227_);
      });
   }

   @Override
   protected void repositionElements() {
      this.backupInfoList.updateSize(this.width, this.layout);
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   Component checkForSpecificMetadata(String p_406384_, String p_408935_) {
      String s = p_406384_.toLowerCase(Locale.ROOT);
      if (s.contains("game") && s.contains("mode")) {
         return this.gameModeMetadata(p_408935_);
      } else if (s.contains("game") && s.contains("difficulty")) {
         return this.gameDifficultyMetadata(p_408935_);
      } else {
         return p_406384_.equals("world_type") ? this.parseWorldType(p_408935_) : Component.literal(p_408935_);
      }
   }

   private Component gameDifficultyMetadata(String p_407114_) {
      try {
         return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(p_407114_)).getDisplayName();
      } catch (Exception exception) {
         return UNKNOWN;
      }
   }

   private Component gameModeMetadata(String p_406287_) {
      try {
         return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(p_406287_)).getShortDisplayName();
      } catch (Exception exception) {
         return UNKNOWN;
      }
   }

   private Component parseWorldType(String p_456793_) {
      try {
         return RealmsServer.WorldType.valueOf(p_456793_.toUpperCase(Locale.ROOT)).getDisplayName();
      } catch (Exception exception) {
         return RealmsServer.WorldType.UNKNOWN.getDisplayName();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class BackupInfoList extends ObjectSelectionList<RealmsBackupInfoScreen.BackupInfoListEntry> {
      public BackupInfoList(final Minecraft p_407391_) {
         super(
            p_407391_,
            RealmsBackupInfoScreen.this.width,
            RealmsBackupInfoScreen.this.layout.getContentHeight(),
            RealmsBackupInfoScreen.this.layout.getHeaderHeight(),
            36
         );
         if (RealmsBackupInfoScreen.this.backup.changeList != null) {
            RealmsBackupInfoScreen.this.backup
               .changeList
               .forEach((p_420610_, p_420611_) -> this.addEntry(RealmsBackupInfoScreen.this.new BackupInfoListEntry(p_420610_, p_420611_)));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class BackupInfoListEntry extends ObjectSelectionList.Entry<RealmsBackupInfoScreen.BackupInfoListEntry> {
      private static final Component TEMPLATE_NAME = Component.translatable("mco.backup.entry.templateName");
      private static final Component GAME_DIFFICULTY = Component.translatable("mco.backup.entry.gameDifficulty");
      private static final Component NAME = Component.translatable("mco.backup.entry.name");
      private static final Component GAME_SERVER_VERSION = Component.translatable("mco.backup.entry.gameServerVersion");
      private static final Component UPLOADED = Component.translatable("mco.backup.entry.uploaded");
      private static final Component ENABLED_PACK = Component.translatable("mco.backup.entry.enabledPack");
      private static final Component DESCRIPTION = Component.translatable("mco.backup.entry.description");
      private static final Component GAME_MODE = Component.translatable("mco.backup.entry.gameMode");
      private static final Component SEED = Component.translatable("mco.backup.entry.seed");
      private static final Component WORLD_TYPE = Component.translatable("mco.backup.entry.worldType");
      private static final Component UNDEFINED = Component.translatable("mco.backup.entry.undefined");
      private final String key;
      private final String value;
      private final Component keyComponent;
      private final Component valueComponent;

      public BackupInfoListEntry(final String p_406405_, final String p_409432_) {
         this.key = p_406405_;
         this.value = p_409432_;
         this.keyComponent = this.translateKey(p_406405_);
         this.valueComponent = RealmsBackupInfoScreen.this.checkForSpecificMetadata(p_406405_, p_409432_);
      }

      @Override
      public void renderContent(GuiGraphics p_429898_, int p_423623_, int p_426194_, boolean p_430441_, float p_427873_) {
         p_429898_.drawString(RealmsBackupInfoScreen.this.font, this.keyComponent, this.getContentX(), this.getContentY(), -6250336);
         p_429898_.drawString(RealmsBackupInfoScreen.this.font, this.valueComponent, this.getContentX(), this.getContentY() + 12, -1);
      }

      private Component translateKey(String p_406977_) {
         return switch (p_406977_) {
            case "template_name" -> TEMPLATE_NAME;
            case "game_difficulty" -> GAME_DIFFICULTY;
            case "name" -> NAME;
            case "game_server_version" -> GAME_SERVER_VERSION;
            case "uploaded" -> UPLOADED;
            case "enabled_packs" -> ENABLED_PACK;
            case "description" -> DESCRIPTION;
            case "game_mode" -> GAME_MODE;
            case "seed" -> SEED;
            case "world_type" -> WORLD_TYPE;
            default -> UNDEFINED;
         };
      }

      @Override
      public Component getNarration() {
         return Component.translatable("narrator.select", this.key + " " + this.value);
      }
   }
}
