package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.ping.ClientPongPacketListener;

public interface ClientGamePacketListener extends ClientCommonPacketListener, ClientPongPacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.PLAY;
   }

   void handleAddEntity(ClientboundAddEntityPacket var1);

   void handleAddObjective(ClientboundSetObjectivePacket var1);

   void handleAnimate(ClientboundAnimatePacket var1);

   void handleHurtAnimation(ClientboundHurtAnimationPacket var1);

   void handleAwardStats(ClientboundAwardStatsPacket var1);

   void handleRecipeBookAdd(ClientboundRecipeBookAddPacket var1);

   void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket var1);

   void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket var1);

   void handleBlockDestruction(ClientboundBlockDestructionPacket var1);

   void handleOpenSignEditor(ClientboundOpenSignEditorPacket var1);

   void handleBlockEntityData(ClientboundBlockEntityDataPacket var1);

   void handleBlockEvent(ClientboundBlockEventPacket var1);

   void handleBlockUpdate(ClientboundBlockUpdatePacket var1);

   void handleSystemChat(ClientboundSystemChatPacket var1);

   void handlePlayerChat(ClientboundPlayerChatPacket var1);

   void handleDisguisedChat(ClientboundDisguisedChatPacket var1);

   void handleDeleteChat(ClientboundDeleteChatPacket var1);

   void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket var1);

   void handleMapItemData(ClientboundMapItemDataPacket var1);

   void handleContainerClose(ClientboundContainerClosePacket var1);

   void handleContainerContent(ClientboundContainerSetContentPacket var1);

   void handleMountScreenOpen(ClientboundMountScreenOpenPacket var1);

   void handleContainerSetData(ClientboundContainerSetDataPacket var1);

   void handleContainerSetSlot(ClientboundContainerSetSlotPacket var1);

   void handleEntityEvent(ClientboundEntityEventPacket var1);

   void handleEntityLinkPacket(ClientboundSetEntityLinkPacket var1);

   void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket var1);

   void handleExplosion(ClientboundExplodePacket var1);

   void handleGameEvent(ClientboundGameEventPacket var1);

   void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket var1);

   void handleChunksBiomes(ClientboundChunksBiomesPacket var1);

   void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket var1);

   void handleLevelEvent(ClientboundLevelEventPacket var1);

   void handleLogin(ClientboundLoginPacket var1);

   void handleMoveEntity(ClientboundMoveEntityPacket var1);

   void handleMinecartAlongTrack(ClientboundMoveMinecartPacket var1);

   void handleMovePlayer(ClientboundPlayerPositionPacket var1);

   void handleRotatePlayer(ClientboundPlayerRotationPacket var1);

   void handleParticleEvent(ClientboundLevelParticlesPacket var1);

   void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket var1);

   void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket var1);

   void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket var1);

   void handleRemoveEntities(ClientboundRemoveEntitiesPacket var1);

   void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket var1);

   void handleRespawn(ClientboundRespawnPacket var1);

   void handleRotateMob(ClientboundRotateHeadPacket var1);

   void handleSetHeldSlot(ClientboundSetHeldSlotPacket var1);

   void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket var1);

   void handleSetEntityData(ClientboundSetEntityDataPacket var1);

   void handleSetEntityMotion(ClientboundSetEntityMotionPacket var1);

   void handleSetEquipment(ClientboundSetEquipmentPacket var1);

   void handleSetExperience(ClientboundSetExperiencePacket var1);

   void handleSetHealth(ClientboundSetHealthPacket var1);

   void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket var1);

   void handleSetScore(ClientboundSetScorePacket var1);

   void handleResetScore(ClientboundResetScorePacket var1);

   void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket var1);

   void handleSetTime(ClientboundSetTimePacket var1);

   void handleSoundEvent(ClientboundSoundPacket var1);

   void handleSoundEntityEvent(ClientboundSoundEntityPacket var1);

   void handleTakeItemEntity(ClientboundTakeItemEntityPacket var1);

   void handleEntityPositionSync(ClientboundEntityPositionSyncPacket var1);

   void handleTeleportEntity(ClientboundTeleportEntityPacket var1);

   void handleTickingState(ClientboundTickingStatePacket var1);

   void handleTickingStep(ClientboundTickingStepPacket var1);

   void handleUpdateAttributes(ClientboundUpdateAttributesPacket var1);

   void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket var1);

   void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket var1);

   void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket var1);

   void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket var1);

   void handleChangeDifficulty(ClientboundChangeDifficultyPacket var1);

   void handleSetCamera(ClientboundSetCameraPacket var1);

   void handleInitializeBorder(ClientboundInitializeBorderPacket var1);

   void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket var1);

   void handleSetBorderSize(ClientboundSetBorderSizePacket var1);

   void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket var1);

   void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket var1);

   void handleSetBorderCenter(ClientboundSetBorderCenterPacket var1);

   void handleTabListCustomisation(ClientboundTabListPacket var1);

   void handleBossUpdate(ClientboundBossEventPacket var1);

   void handleItemCooldown(ClientboundCooldownPacket var1);

   void handleMoveVehicle(ClientboundMoveVehiclePacket var1);

   void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket var1);

   void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket var1);

   void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket var1);

   void handleCommands(ClientboundCommandsPacket var1);

   void handleStopSoundEvent(ClientboundStopSoundPacket var1);

   void handleCommandSuggestions(ClientboundCommandSuggestionsPacket var1);

   void handleUpdateRecipes(ClientboundUpdateRecipesPacket var1);

   void handleLookAt(ClientboundPlayerLookAtPacket var1);

   void handleTagQueryPacket(ClientboundTagQueryPacket var1);

   void handleLightUpdatePacket(ClientboundLightUpdatePacket var1);

   void handleOpenBook(ClientboundOpenBookPacket var1);

   void handleOpenScreen(ClientboundOpenScreenPacket var1);

   void handleMerchantOffers(ClientboundMerchantOffersPacket var1);

   void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket var1);

   void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket var1);

   void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket var1);

   void handleBlockChangedAck(ClientboundBlockChangedAckPacket var1);

   void setActionBarText(ClientboundSetActionBarTextPacket var1);

   void setSubtitleText(ClientboundSetSubtitleTextPacket var1);

   void setTitleText(ClientboundSetTitleTextPacket var1);

   void setTitlesAnimation(ClientboundSetTitlesAnimationPacket var1);

   void handleTitlesClear(ClientboundClearTitlesPacket var1);

   void handleServerData(ClientboundServerDataPacket var1);

   void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket var1);

   void handleBundlePacket(ClientboundBundlePacket var1);

   void handleDamageEvent(ClientboundDamageEventPacket var1);

   void handleConfigurationStart(ClientboundStartConfigurationPacket var1);

   void handleChunkBatchStart(ClientboundChunkBatchStartPacket var1);

   void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket var1);

   void handleDebugSample(ClientboundDebugSamplePacket var1);

   void handleProjectilePowerPacket(ClientboundProjectilePowerPacket var1);

   void handleSetCursorItem(ClientboundSetCursorItemPacket var1);

   void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket var1);

   void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus var1);

   void handleWaypoint(ClientboundTrackedWaypointPacket var1);

   void handleDebugChunkValue(ClientboundDebugChunkValuePacket var1);

   void handleDebugBlockValue(ClientboundDebugBlockValuePacket var1);

   void handleDebugEntityValue(ClientboundDebugEntityValuePacket var1);

   void handleDebugEvent(ClientboundDebugEventPacket var1);

   void handleGameTestHighlightPos(ClientboundGameTestHighlightPosPacket var1);
}
