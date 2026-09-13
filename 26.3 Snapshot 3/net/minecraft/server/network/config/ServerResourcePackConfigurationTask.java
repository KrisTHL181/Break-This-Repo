package net.minecraft.server.network.config;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;

public class ServerResourcePackConfigurationTask implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("server_resource_pack");
   private final MinecraftServer.ServerResourcePackInfo info;

   public ServerResourcePackConfigurationTask(final MinecraftServer.ServerResourcePackInfo info) {
      this.info = info;
   }

   @Override
   public void start(final Consumer<Packet<?>> connection) {
      connection.accept(
         new ClientboundResourcePackPushPacket(
            this.info.id(), this.info.url(), this.info.hash(), this.info.isRequired(), Optional.ofNullable(this.info.prompt())
         )
      );
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
