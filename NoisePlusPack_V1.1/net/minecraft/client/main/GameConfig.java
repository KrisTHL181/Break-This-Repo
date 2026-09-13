package net.minecraft.client.main;

import com.mojang.blaze3d.platform.DisplayData;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import net.minecraft.client.User;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GameConfig {
   public final GameConfig.UserData user;
   public final DisplayData display;
   public final GameConfig.FolderData location;
   public final GameConfig.GameData game;
   public final GameConfig.QuickPlayData quickPlay;

   public GameConfig(
      GameConfig.UserData p_279448_, DisplayData p_279368_, GameConfig.FolderData p_279174_, GameConfig.GameData p_279138_, GameConfig.QuickPlayData p_279425_
   ) {
      this.user = p_279448_;
      this.display = p_279368_;
      this.location = p_279174_;
      this.game = p_279138_;
      this.quickPlay = p_279425_;
   }

   @OnlyIn(Dist.CLIENT)
   public static class FolderData {
      public final File gameDirectory;
      public final File resourcePackDirectory;
      public final File assetDirectory;
      public final @Nullable String assetIndex;

      public FolderData(File p_101921_, File p_101922_, File p_101923_, @Nullable String p_101924_) {
         this.gameDirectory = p_101921_;
         this.resourcePackDirectory = p_101922_;
         this.assetDirectory = p_101923_;
         this.assetIndex = p_101924_;
      }

      public Path getExternalAssetSource() {
         return this.assetIndex == null ? this.assetDirectory.toPath() : IndexedAssetSource.createIndexFs(this.assetDirectory.toPath(), this.assetIndex);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GameData {
      public final boolean demo;
      public final String launchVersion;
      public final String versionType;
      public final boolean disableMultiplayer;
      public final boolean disableChat;
      public final boolean captureTracyImages;
      public final boolean renderDebugLabels;
      public final boolean offlineDeveloperMode;

      public GameData(
         boolean p_101932_, String p_101933_, String p_101934_, boolean p_101935_, boolean p_101936_, boolean p_370094_, boolean p_392577_, boolean p_428465_
      ) {
         this.demo = p_101932_;
         this.launchVersion = p_101933_;
         this.versionType = p_101934_;
         this.disableMultiplayer = p_101935_;
         this.disableChat = p_101936_;
         this.captureTracyImages = p_370094_;
         this.renderDebugLabels = p_392577_;
         this.offlineDeveloperMode = p_428465_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record QuickPlayData(@Nullable String logPath, GameConfig.QuickPlayVariant variant) {
      public boolean isEnabled() {
         return this.variant.isEnabled();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record QuickPlayDisabled() implements GameConfig.QuickPlayVariant {
      @Override
      public boolean isEnabled() {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record QuickPlayMultiplayerData(String serverAddress) implements GameConfig.QuickPlayVariant {
      @Override
      public boolean isEnabled() {
         return !StringUtil.isBlank(this.serverAddress);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record QuickPlayRealmsData(String realmId) implements GameConfig.QuickPlayVariant {
      @Override
      public boolean isEnabled() {
         return !StringUtil.isBlank(this.realmId);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record QuickPlaySinglePlayerData(@Nullable String worldId) implements GameConfig.QuickPlayVariant {
      @Override
      public boolean isEnabled() {
         return true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public sealed interface QuickPlayVariant
      permits GameConfig.QuickPlaySinglePlayerData,
      GameConfig.QuickPlayMultiplayerData,
      GameConfig.QuickPlayRealmsData,
      GameConfig.QuickPlayDisabled {
      GameConfig.QuickPlayVariant DISABLED = new GameConfig.QuickPlayDisabled();

      boolean isEnabled();
   }

   @OnlyIn(Dist.CLIENT)
   public static class UserData {
      public final User user;
      public final Proxy proxy;

      public UserData(User p_101947_, Proxy p_101950_) {
         this.user = p_101947_;
         this.proxy = p_101950_;
      }
   }
}
