package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import net.minecraft.util.thread.ConsecutiveExecutor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerList {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ConsecutiveExecutor IO_EXECUTOR = new ConsecutiveExecutor(Util.backgroundExecutor(), "server-list-io");
   private static final int MAX_HIDDEN_SERVERS = 16;
   private final Minecraft minecraft;
   private final List<ServerData> serverList = Lists.newArrayList();
   private final List<ServerData> hiddenServerList = Lists.newArrayList();

   public ServerList(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   public void load() {
      try {
         this.serverList.clear();
         this.hiddenServerList.clear();
         CompoundTag tag = NbtIo.read(this.minecraft.gameDirectory.toPath().resolve("servers.dat"));
         if (tag == null) {
            return;
         }

         tag.getListOrEmpty("servers").compoundStream().forEach(serverTag -> {
            ServerData serverData = ServerData.read(serverTag);
            if (serverTag.getBooleanOr("hidden", false)) {
               this.hiddenServerList.add(serverData);
            } else {
               this.serverList.add(serverData);
            }
         });
      } catch (Exception e) {
         LOGGER.error("Couldn't load server list", e);
      }
   }

   public void save() {
      try {
         ListTag serverTags = new ListTag();

         for (ServerData server : this.serverList) {
            CompoundTag serverTag = server.write();
            serverTag.putBoolean("hidden", false);
            serverTags.add(serverTag);
         }

         for (ServerData server : this.hiddenServerList) {
            CompoundTag serverTag = server.write();
            serverTag.putBoolean("hidden", true);
            serverTags.add(serverTag);
         }

         CompoundTag tag = new CompoundTag();
         tag.put("servers", serverTags);
         Path gameDirectoryPath = this.minecraft.gameDirectory.toPath();
         Path newFile = Files.createTempFile(gameDirectoryPath, "servers", ".dat");
         NbtIo.write(tag, newFile);
         Path oldFile = gameDirectoryPath.resolve("servers.dat_old");
         Path currentFile = gameDirectoryPath.resolve("servers.dat");
         Util.safeReplaceFile(currentFile, newFile, oldFile);
      } catch (Exception e) {
         LOGGER.error("Couldn't save server list", e);
      }
   }

   public ServerData get(final int index) {
      return this.serverList.get(index);
   }

   public @Nullable ServerData get(final String ip) {
      for (ServerData serverData : this.serverList) {
         if (serverData.ip.equals(ip)) {
            return serverData;
         }
      }

      for (ServerData serverData : this.hiddenServerList) {
         if (serverData.ip.equals(ip)) {
            return serverData;
         }
      }

      return null;
   }

   public @Nullable ServerData unhide(final String ip) {
      for (int i = 0; i < this.hiddenServerList.size(); i++) {
         ServerData serverData = this.hiddenServerList.get(i);
         if (serverData.ip.equals(ip)) {
            this.hiddenServerList.remove(i);
            this.serverList.add(serverData);
            return serverData;
         }
      }

      return null;
   }

   public void remove(final ServerData thing) {
      if (!this.serverList.remove(thing)) {
         this.hiddenServerList.remove(thing);
      }
   }

   public void add(final ServerData server, final boolean hidden) {
      if (hidden) {
         this.hiddenServerList.add(0, server);

         while (this.hiddenServerList.size() > 16) {
            this.hiddenServerList.remove(this.hiddenServerList.size() - 1);
         }
      } else {
         this.serverList.add(server);
      }
   }

   public int size() {
      return this.serverList.size();
   }

   public void swap(final int a, final int b) {
      ServerData swap = this.get(a);
      this.serverList.set(a, this.get(b));
      this.serverList.set(b, swap);
      this.save();
   }

   public void replace(final int id, final ServerData data) {
      this.serverList.set(id, data);
   }

   private static boolean set(final ServerData data, final List<ServerData> list) {
      for (int i = 0; i < list.size(); i++) {
         ServerData target = list.get(i);
         if (Objects.equals(target.name, data.name) && target.ip.equals(data.ip)) {
            list.set(i, data);
            return true;
         }
      }

      return false;
   }

   public static void saveSingleServer(final ServerData data) {
      IO_EXECUTOR.schedule(() -> {
         ServerList list = new ServerList(Minecraft.getInstance());
         list.load();
         if (!set(data, list.serverList)) {
            set(data, list.hiddenServerList);
         }

         list.save();
      });
   }
}
