package net.minecraft.server.network.config;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.server.network.ConfigurationTask;

public class JoinWorldTask implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("join_world");

   @Override
   public void start(Consumer<Packet<?>> p_299501_) {
      p_299501_.accept(ClientboundFinishConfigurationPacket.INSTANCE);
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
