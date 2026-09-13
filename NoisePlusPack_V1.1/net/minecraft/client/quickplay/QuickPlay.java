package net.minecraft.client.quickplay;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class QuickPlay {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Component ERROR_TITLE = Component.translatable("quickplay.error.title");
   private static final Component INVALID_IDENTIFIER = Component.translatable("quickplay.error.invalid_identifier");
   private static final Component REALM_CONNECT = Component.translatable("quickplay.error.realm_connect");
   private static final Component REALM_PERMISSION = Component.translatable("quickplay.error.realm_permission");
   private static final Component TO_TITLE = Component.translatable("gui.toTitle");
   private static final Component TO_WORLD_LIST = Component.translatable("gui.toWorld");
   private static final Component TO_REALMS_LIST = Component.translatable("gui.toRealms");

   public static void connect(Minecraft p_279319_, GameConfig.QuickPlayVariant p_410522_, RealmsClient p_279322_) {
      if (!p_410522_.isEnabled()) {
         LOGGER.error("Quick play disabled");
         p_279319_.setScreen(new TitleScreen());
      } else {
         switch (p_410522_) {
            case GameConfig.QuickPlayMultiplayerData gameconfig$quickplaymultiplayerdata:
               joinMultiplayerWorld(p_279319_, gameconfig$quickplaymultiplayerdata.serverAddress());
               break;
            case GameConfig.QuickPlayRealmsData gameconfig$quickplayrealmsdata:
               joinRealmsWorld(p_279319_, p_279322_, gameconfig$quickplayrealmsdata.realmId());
               break;
            case GameConfig.QuickPlaySinglePlayerData gameconfig$quickplaysingleplayerdata:
               String s = gameconfig$quickplaysingleplayerdata.worldId();
               if (StringUtil.isBlank(s)) {
                  s = getLatestSingleplayerWorld(p_279319_.getLevelSource());
               }

               joinSingleplayerWorld(p_279319_, s);
               break;
            case GameConfig.QuickPlayDisabled gameconfig$quickplaydisabled:
               LOGGER.error("Quick play disabled");
               p_279319_.setScreen(new TitleScreen());
               break;
            default:
               throw new MatchException(null, null);
         }
      }
   }

   private static @Nullable String getLatestSingleplayerWorld(LevelStorageSource p_410369_) {
      try {
         List<LevelSummary> list = p_410369_.loadLevelSummaries(p_410369_.findLevelCandidates()).get();
         if (list.isEmpty()) {
            LOGGER.warn("no latest singleplayer world found");
            return null;
         } else {
            return list.getFirst().getLevelId();
         }
      } catch (ExecutionException | InterruptedException interruptedexception) {
         LOGGER.error("failed to load singleplayer world summaries", interruptedexception);
         return null;
      }
   }

   private static void joinSingleplayerWorld(Minecraft p_279420_, @Nullable String p_279459_) {
      if (!StringUtil.isBlank(p_279459_) && p_279420_.getLevelSource().levelExists(p_279459_)) {
         p_279420_.createWorldOpenFlows().openWorld(p_279459_, () -> p_279420_.setScreen(new TitleScreen()));
      } else {
         Screen screen = new SelectWorldScreen(new TitleScreen());
         p_279420_.setScreen(new DisconnectedScreen(screen, ERROR_TITLE, INVALID_IDENTIFIER, TO_WORLD_LIST));
      }
   }

   private static void joinMultiplayerWorld(Minecraft p_279276_, String p_279128_) {
      ServerList serverlist = new ServerList(p_279276_);
      serverlist.load();
      ServerData serverdata = serverlist.get(p_279128_);
      if (serverdata == null) {
         serverdata = new ServerData(I18n.get("selectServer.defaultName"), p_279128_, ServerData.Type.OTHER);
         serverlist.add(serverdata, true);
         serverlist.save();
      }

      ServerAddress serveraddress = ServerAddress.parseString(p_279128_);
      ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), p_279276_, serveraddress, serverdata, true, null);
   }

   private static void joinRealmsWorld(Minecraft p_279320_, RealmsClient p_279468_, String p_279371_) {
      long i;
      RealmsServerList realmsserverlist;
      try {
         i = Long.parseLong(p_279371_);
         realmsserverlist = p_279468_.listRealms();
      } catch (NumberFormatException numberformatexception) {
         Screen screen1 = new RealmsMainScreen(new TitleScreen());
         p_279320_.setScreen(new DisconnectedScreen(screen1, ERROR_TITLE, INVALID_IDENTIFIER, TO_REALMS_LIST));
         return;
      } catch (RealmsServiceException realmsserviceexception) {
         Screen screen = new TitleScreen();
         p_279320_.setScreen(new DisconnectedScreen(screen, ERROR_TITLE, REALM_CONNECT, TO_TITLE));
         return;
      }

      RealmsServer realmsserver = realmsserverlist.servers().stream().filter(p_279424_ -> p_279424_.id == i).findFirst().orElse(null);
      if (realmsserver == null) {
         Screen screen2 = new RealmsMainScreen(new TitleScreen());
         p_279320_.setScreen(new DisconnectedScreen(screen2, ERROR_TITLE, REALM_PERMISSION, TO_REALMS_LIST));
      } else {
         TitleScreen titlescreen = new TitleScreen();
         p_279320_.setScreen(new RealmsLongRunningMcoTaskScreen(titlescreen, new GetServerDetailsTask(titlescreen, realmsserver)));
      }
   }
}
