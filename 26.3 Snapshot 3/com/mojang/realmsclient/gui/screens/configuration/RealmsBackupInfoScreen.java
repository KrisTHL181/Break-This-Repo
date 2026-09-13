package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;

public class RealmsBackupInfoScreen extends RealmsScreen {
   private static final Component TITLE = Component.translatable("mco.backup.info.title");
   private static final Component UNKNOWN = Component.translatable("mco.backup.unknown");
   private final Screen lastScreen;
   private final Backup backup;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private RealmsBackupInfoScreen.BackupInfoList backupInfoList;

   public RealmsBackupInfoScreen(final Screen lastScreen, final Backup backup) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.backup = backup;
   }

   @Override
   public void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.backupInfoList = this.layout.addToContents(new RealmsBackupInfoScreen.BackupInfoList(this.minecraft));
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
      this.repositionElements();
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
   }

   @Override
   protected void repositionElements() {
      this.backupInfoList.updateSize(this.width, this.layout);
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   private Component checkForSpecificMetadata(final String key, final String value) {
      String k = key.toLowerCase(Locale.ROOT);
      if (k.contains("game") && k.contains("mode")) {
         return this.gameModeMetadata(value);
      } else if (k.contains("game") && k.contains("difficulty")) {
         return this.gameDifficultyMetadata(value);
      } else {
         return key.equals("world_type") ? this.parseWorldType(value) : Component.literal(value);
      }
   }

   private Component gameDifficultyMetadata(final String value) {
      try {
         return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(value)).getDisplayName();
      } catch (Exception ignored) {
         return UNKNOWN;
      }
   }

   private Component gameModeMetadata(final String value) {
      try {
         return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(value)).getShortDisplayName();
      } catch (Exception ignored) {
         return UNKNOWN;
      }
   }

   private Component parseWorldType(final String value) {
      try {
         return RealmsServer.WorldType.valueOf(value.toUpperCase(Locale.ROOT)).getDisplayName();
      } catch (Exception ignored) {
         return RealmsServer.WorldType.UNKNOWN.getDisplayName();
      }
   }

   private class BackupInfoList extends ObjectSelectionList<RealmsBackupInfoScreen.BackupInfoListEntry> {
      public BackupInfoList(final Minecraft minecraft) {
         super(
            minecraft,
            RealmsBackupInfoScreen.this.width,
            RealmsBackupInfoScreen.this.layout.getContentHeight(),
            RealmsBackupInfoScreen.this.layout.getHeaderHeight(),
            36
         );
         if (RealmsBackupInfoScreen.this.backup.changeList != null) {
            RealmsBackupInfoScreen.this.backup
               .changeList
               .forEach((key, value) -> this.addEntry(RealmsBackupInfoScreen.this.new BackupInfoListEntry(key, value)));
         }
      }
   }

   private class BackupInfoListEntry extends ObjectSelectionList.Entry<RealmsBackupInfoScreen.BackupInfoListEntry> {
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

      public BackupInfoListEntry(final String key, final String value) {
         this.key = key;
         this.value = value;
         this.keyComponent = this.translateKey(key);
         this.valueComponent = RealmsBackupInfoScreen.this.checkForSpecificMetadata(key, value);
      }

      @Override
      public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         graphics.text(RealmsBackupInfoScreen.this.font, this.keyComponent, this.getContentX(), this.getContentY(), -6250336);
         graphics.text(RealmsBackupInfoScreen.this.font, this.valueComponent, this.getContentX(), this.getContentY() + 12, -1);
      }

      private Component translateKey(final String key) {
         return switch (key) {
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
