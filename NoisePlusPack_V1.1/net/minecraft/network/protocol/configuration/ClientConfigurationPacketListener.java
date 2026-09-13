package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;

public interface ClientConfigurationPacketListener extends ClientCommonPacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.CONFIGURATION;
   }

   void handleCodeOfConduct(ClientboundCodeOfConductPacket var1);

   void handleConfigurationFinished(ClientboundFinishConfigurationPacket var1);

   void handleRegistryData(ClientboundRegistryDataPacket var1);

   void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket var1);

   void handleSelectKnownPacks(ClientboundSelectKnownPacks var1);

   void handleResetChat(ClientboundResetChatPacket var1);
}
