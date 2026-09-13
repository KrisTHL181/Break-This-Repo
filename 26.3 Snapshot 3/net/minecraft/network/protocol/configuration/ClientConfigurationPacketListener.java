package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;

public interface ClientConfigurationPacketListener extends ClientCommonPacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.CONFIGURATION;
   }

   void handleCodeOfConduct(ClientboundCodeOfConductPacket packet);

   void handleConfigurationFinished(ClientboundFinishConfigurationPacket packet);

   void handleRegistryData(ClientboundRegistryDataPacket packet);

   void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket packet);

   void handleSelectKnownPacks(ClientboundSelectKnownPacks packet);

   void handleResetChat(ClientboundResetChatPacket packet);
}
