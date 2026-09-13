package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.ping.ServerPingPacketListener;

public interface ServerGamePacketListener extends ServerCommonPacketListener, ServerPingPacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.PLAY;
   }

   void handleAnimate(ServerboundSwingPacket var1);

   void handleChat(ServerboundChatPacket var1);

   void handleChatCommand(ServerboundChatCommandPacket var1);

   void handleSignedChatCommand(ServerboundChatCommandSignedPacket var1);

   void handleChatAck(ServerboundChatAckPacket var1);

   void handleClientCommand(ServerboundClientCommandPacket var1);

   void handleContainerButtonClick(ServerboundContainerButtonClickPacket var1);

   void handleContainerClick(ServerboundContainerClickPacket var1);

   void handlePlaceRecipe(ServerboundPlaceRecipePacket var1);

   void handleContainerClose(ServerboundContainerClosePacket var1);

   void handleInteract(ServerboundInteractPacket var1);

   void handleMovePlayer(ServerboundMovePlayerPacket var1);

   void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket var1);

   void handlePlayerAction(ServerboundPlayerActionPacket var1);

   void handlePlayerCommand(ServerboundPlayerCommandPacket var1);

   void handlePlayerInput(ServerboundPlayerInputPacket var1);

   void handleSetCarriedItem(ServerboundSetCarriedItemPacket var1);

   void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket var1);

   void handleSignUpdate(ServerboundSignUpdatePacket var1);

   void handleUseItemOn(ServerboundUseItemOnPacket var1);

   void handleUseItem(ServerboundUseItemPacket var1);

   void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket var1);

   void handlePaddleBoat(ServerboundPaddleBoatPacket var1);

   void handleMoveVehicle(ServerboundMoveVehiclePacket var1);

   void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket var1);

   void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket var1);

   void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket var1);

   void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket var1);

   void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket var1);

   void handleSeenAdvancements(ServerboundSeenAdvancementsPacket var1);

   void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket var1);

   void handleSetCommandBlock(ServerboundSetCommandBlockPacket var1);

   void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket var1);

   void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket var1);

   void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket var1);

   void handleRenameItem(ServerboundRenameItemPacket var1);

   void handleSetBeaconPacket(ServerboundSetBeaconPacket var1);

   void handleSetStructureBlock(ServerboundSetStructureBlockPacket var1);

   void handleSetTestBlock(ServerboundSetTestBlockPacket var1);

   void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket var1);

   void handleSelectTrade(ServerboundSelectTradePacket var1);

   void handleEditBook(ServerboundEditBookPacket var1);

   void handleEntityTagQuery(ServerboundEntityTagQueryPacket var1);

   void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket var1);

   void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket var1);

   void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket var1);

   void handleJigsawGenerate(ServerboundJigsawGeneratePacket var1);

   void handleChangeDifficulty(ServerboundChangeDifficultyPacket var1);

   void handleChangeGameMode(ServerboundChangeGameModePacket var1);

   void handleLockDifficulty(ServerboundLockDifficultyPacket var1);

   void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket var1);

   void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket var1);

   void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket var1);

   void handleDebugSubscriptionRequest(ServerboundDebugSubscriptionRequestPacket var1);

   void handleClientTickEnd(ServerboundClientTickEndPacket var1);
}
