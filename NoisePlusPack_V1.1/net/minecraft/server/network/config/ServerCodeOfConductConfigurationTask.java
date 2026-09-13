package net.minecraft.server.network.config;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.server.network.ConfigurationTask;

public class ServerCodeOfConductConfigurationTask implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("server_code_of_conduct");
   private final Supplier<String> codeOfConduct;

   public ServerCodeOfConductConfigurationTask(Supplier<String> p_429314_) {
      this.codeOfConduct = p_429314_;
   }

   @Override
   public void start(Consumer<Packet<?>> p_430018_) {
      p_430018_.accept(new ClientboundCodeOfConductPacket(this.codeOfConduct.get()));
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }
}
