package net.minecraft.network.protocol.game;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.ping.ClientPongPacketListener;

public interface ClientGamePacketListener extends ClientCommonPacketListener, ClientPongPacketListener {
   @Override
   default ConnectionProtocol protocol() {
      return ConnectionProtocol.PLAY;
   }

   void handleAddEntity(ClientboundAddEntityPacket packet);

   void handleAddObjective(ClientboundSetObjectivePacket packet);

   void handleAnimate(ClientboundAnimatePacket packet);

   void handleHurtAnimation(ClientboundHurtAnimationPacket packet);

   void handleAwardStats(ClientboundAwardStatsPacket packet);

   void handleRecipeBookAdd(ClientboundRecipeBookAddPacket packet);

   void handleRecipeBookRemove(ClientboundRecipeBookRemovePacket packet);

   void handleRecipeBookSettings(ClientboundRecipeBookSettingsPacket packet);

   void handleBlockDestruction(ClientboundBlockDestructionPacket packet);

   void handleOpenSignEditor(ClientboundOpenSignEditorPacket packet);

   void handleBlockEntityData(ClientboundBlockEntityDataPacket packet);

   void handleBlockEvent(ClientboundBlockEventPacket packet);

   void handleBlockUpdate(ClientboundBlockUpdatePacket packet);

   void handleSystemChat(ClientboundSystemChatPacket packet);

   void handlePlayerChat(ClientboundPlayerChatPacket packet);

   void handleDisguisedChat(ClientboundDisguisedChatPacket packet);

   void handleDeleteChat(ClientboundDeleteChatPacket packet);

   void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet);

   void handleMapItemData(ClientboundMapItemDataPacket packet);

   void handleContainerClose(ClientboundContainerClosePacket packet);

   void handleContainerContent(ClientboundContainerSetContentPacket packet);

   void handleMountScreenOpen(ClientboundMountScreenOpenPacket packet);

   void handleContainerSetData(ClientboundContainerSetDataPacket packet);

   void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet);

   void handleEntityEvent(ClientboundEntityEventPacket packet);

   void handleEntityLinkPacket(ClientboundSetEntityLinkPacket packet);

   void handleSetEntityPassengersPacket(ClientboundSetPassengersPacket packet);

   void handleExplosion(ClientboundExplodePacket packet);

   void handleGameEvent(ClientboundGameEventPacket packet);

   void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet);

   void handleChunksBiomes(ClientboundChunksBiomesPacket packet);

   void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet);

   void handleLevelEvent(ClientboundLevelEventPacket packet);

   void handleLogin(ClientboundLoginPacket packet);

   void handleMoveEntity(ClientboundMoveEntityPacket packet);

   void handleMinecartAlongTrack(ClientboundMoveMinecartPacket packet);

   void handleMovePlayer(ClientboundPlayerPositionPacket packet);

   void handleRotatePlayer(ClientboundPlayerRotationPacket packet);

   void handleParticleEvent(ClientboundLevelParticlesPacket packet);

   void handlePlayerAbilities(ClientboundPlayerAbilitiesPacket packet);

   void handleGameRuleValues(ClientboundGameRuleValuesPacket packet);

   void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet);

   void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet);

   void handleRemoveEntities(ClientboundRemoveEntitiesPacket packet);

   void handleRemoveMobEffect(ClientboundRemoveMobEffectPacket packet);

   void handleRespawn(ClientboundRespawnPacket packet);

   void handleRotateMob(ClientboundRotateHeadPacket packet);

   void handleSetHeldSlot(ClientboundSetHeldSlotPacket packet);

   void handleSetDisplayObjective(ClientboundSetDisplayObjectivePacket packet);

   void handleSetEntityData(ClientboundSetEntityDataPacket packet);

   void handleSetEntityMotion(ClientboundSetEntityMotionPacket packet);

   void handleSetEquipment(ClientboundSetEquipmentPacket packet);

   void handleSetExperience(ClientboundSetExperiencePacket packet);

   void handleSetHealth(ClientboundSetHealthPacket packet);

   void handleSetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet);

   void handleSetScore(ClientboundSetScorePacket packet);

   void handleResetScore(ClientboundResetScorePacket packet);

   void handleSetSpawn(ClientboundSetDefaultSpawnPositionPacket packet);

   void handleSetTime(ClientboundSetTimePacket packet);

   void handleSoundEvent(ClientboundSoundPacket packet);

   void handleSoundEntityEvent(ClientboundSoundEntityPacket packet);

   void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet);

   void handleEntityPositionSync(ClientboundEntityPositionSyncPacket packet);

   void handleTeleportEntity(ClientboundTeleportEntityPacket packet);

   void handleTickingState(ClientboundTickingStatePacket packet);

   void handleTickingStep(ClientboundTickingStepPacket packet);

   void handleUpdateAttributes(ClientboundUpdateAttributesPacket packet);

   void handleUpdateMobEffect(ClientboundUpdateMobEffectPacket packet);

   void handlePlayerCombatEnd(ClientboundPlayerCombatEndPacket packet);

   void handlePlayerCombatEnter(ClientboundPlayerCombatEnterPacket packet);

   void handlePlayerCombatKill(ClientboundPlayerCombatKillPacket packet);

   void handleChangeDifficulty(ClientboundChangeDifficultyPacket packet);

   void handleSetCamera(ClientboundSetCameraPacket packet);

   void handleInitializeBorder(ClientboundInitializeBorderPacket packet);

   void handleSetBorderLerpSize(ClientboundSetBorderLerpSizePacket packet);

   void handleSetBorderSize(ClientboundSetBorderSizePacket packet);

   void handleSetBorderWarningDelay(ClientboundSetBorderWarningDelayPacket packet);

   void handleSetBorderWarningDistance(ClientboundSetBorderWarningDistancePacket packet);

   void handleSetBorderCenter(ClientboundSetBorderCenterPacket packet);

   void handleTabListCustomisation(ClientboundTabListPacket packet);

   void handleBossUpdate(ClientboundBossEventPacket packet);

   void handleItemCooldown(ClientboundCooldownPacket packet);

   void handleMoveVehicle(ClientboundMoveVehiclePacket packet);

   void handleUpdateAdvancementsPacket(ClientboundUpdateAdvancementsPacket packet);

   void handleSelectAdvancementsTab(ClientboundSelectAdvancementsTabPacket packet);

   void handlePlaceRecipe(ClientboundPlaceGhostRecipePacket packet);

   void handleCommands(ClientboundCommandsPacket packet);

   void handleStopSoundEvent(ClientboundStopSoundPacket packet);

   void handleCommandSuggestions(ClientboundCommandSuggestionsPacket packet);

   void handleUpdateRecipes(ClientboundUpdateRecipesPacket packet);

   void handleLookAt(ClientboundPlayerLookAtPacket packet);

   void handleTagQueryPacket(ClientboundTagQueryPacket packet);

   void handleLightUpdatePacket(ClientboundLightUpdatePacket packet);

   void handleOpenBook(ClientboundOpenBookPacket packet);

   void handleOpenScreen(ClientboundOpenScreenPacket packet);

   void handleMerchantOffers(ClientboundMerchantOffersPacket packet);

   void handleSetChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet);

   void handleSetSimulationDistance(ClientboundSetSimulationDistancePacket packet);

   void handleSetChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet);

   void handleBlockChangedAck(ClientboundBlockChangedAckPacket packet);

   void setActionBarText(ClientboundSetActionBarTextPacket packet);

   void setSubtitleText(ClientboundSetSubtitleTextPacket packet);

   void setTitleText(ClientboundSetTitleTextPacket packet);

   void setTitlesAnimation(ClientboundSetTitlesAnimationPacket packet);

   void handleTitlesClear(ClientboundClearTitlesPacket packet);

   void handleServerData(ClientboundServerDataPacket packet);

   void handleCustomChatCompletions(ClientboundCustomChatCompletionsPacket packet);

   void handleBundlePacket(ClientboundBundlePacket packet);

   void handleDamageEvent(ClientboundDamageEventPacket packet);

   void handleConfigurationStart(ClientboundStartConfigurationPacket packet);

   void handleChunkBatchStart(ClientboundChunkBatchStartPacket packet);

   void handleChunkBatchFinished(ClientboundChunkBatchFinishedPacket packet);

   void handleDebugSample(ClientboundDebugSamplePacket packet);

   void handleProjectilePowerPacket(ClientboundProjectilePowerPacket packet);

   void handleSetCursorItem(ClientboundSetCursorItemPacket packet);

   void handleSetPlayerInventory(ClientboundSetPlayerInventoryPacket packet);

   void handleTestInstanceBlockStatus(ClientboundTestInstanceBlockStatus packet);

   void handleWaypoint(ClientboundTrackedWaypointPacket packet);

   void handleDebugChunkValue(ClientboundDebugChunkValuePacket packet);

   void handleDebugBlockValue(ClientboundDebugBlockValuePacket packet);

   void handleDebugEntityValue(ClientboundDebugEntityValuePacket packet);

   void handleDebugEvent(ClientboundDebugEventPacket packet);

   void handleGameTestHighlightPos(ClientboundGameTestHighlightPosPacket packet);

   void handleLowDiskSpaceWarning(ClientboundLowDiskSpaceWarningPacket packet);
}
