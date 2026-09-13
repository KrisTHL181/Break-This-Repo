package net.minecraft.server.dedicated;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.slf4j.Logger;

public class DedicatedPlayerList extends PlayerList {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DedicatedPlayerList(DedicatedServer p_203709_, LayeredRegistryAccess<RegistryLayer> p_251851_, PlayerDataStorage p_203711_) {
      super(p_203709_, p_251851_, p_203711_, p_203709_.notificationManager());
      this.setViewDistance(p_203709_.viewDistance());
      this.setSimulationDistance(p_203709_.simulationDistance());
      this.loadUserBanList();
      this.saveUserBanList();
      this.loadIpBanList();
      this.saveIpBanList();
      this.loadOps();
      this.loadWhiteList();
      this.saveOps();
      if (!this.getWhiteList().getFile().exists()) {
         this.saveWhiteList();
      }
   }

   @Override
   public void reloadWhiteList() {
      this.loadWhiteList();
   }

   private void saveIpBanList() {
      try {
         this.getIpBans().save();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to save ip banlist: ", ioexception);
      }
   }

   private void saveUserBanList() {
      try {
         this.getBans().save();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to save user banlist: ", ioexception);
      }
   }

   private void loadIpBanList() {
      try {
         this.getIpBans().load();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to load ip banlist: ", ioexception);
      }
   }

   private void loadUserBanList() {
      try {
         this.getBans().load();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to load user banlist: ", ioexception);
      }
   }

   private void loadOps() {
      try {
         this.getOps().load();
      } catch (Exception exception) {
         LOGGER.warn("Failed to load operators list: ", exception);
      }
   }

   private void saveOps() {
      try {
         this.getOps().save();
      } catch (Exception exception) {
         LOGGER.warn("Failed to save operators list: ", exception);
      }
   }

   private void loadWhiteList() {
      try {
         this.getWhiteList().load();
      } catch (Exception exception) {
         LOGGER.warn("Failed to load white-list: ", exception);
      }
   }

   private void saveWhiteList() {
      try {
         this.getWhiteList().save();
      } catch (Exception exception) {
         LOGGER.warn("Failed to save white-list: ", exception);
      }
   }

   @Override
   public boolean isWhiteListed(NameAndId p_425587_) {
      return !this.isUsingWhitelist() || this.isOp(p_425587_) || this.getWhiteList().isWhiteListed(p_425587_);
   }

   public DedicatedServer getServer() {
      return (DedicatedServer)super.getServer();
   }

   @Override
   public boolean canBypassPlayerLimit(NameAndId p_423426_) {
      return this.getOps().canBypassPlayerLimit(p_423426_);
   }
}
