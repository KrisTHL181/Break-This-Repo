package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.HashedStack;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.LastSeenMessagesValidator;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSignatureCache;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.chat.SignableCommand;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.FutureChain;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.StringUtil;
import net.minecraft.util.TickThrottler;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl
   extends ServerCommonPacketListenerImpl
   implements GameProtocols.Context,
   ServerGamePacketListener,
   ServerPlayerConnection,
   TickablePacketListener {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_BLOCK_UPDATES_TO_ACK = -1;
   private static final int TRACKED_MESSAGE_DISCONNECT_THRESHOLD = 4096;
   private static final int MAXIMUM_FLYING_TICKS = 80;
   private static final int ATTACK_INDICATOR_TOLERANCE_TICKS = 5;
   public static final int CLIENT_LOADED_TIMEOUT_TIME = 60;
   private static final Component CHAT_VALIDATION_FAILED = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private static final Component INVALID_COMMAND_SIGNATURE = Component.translatable("chat.disabled.invalid_command_signature").withStyle(ChatFormatting.RED);
   private static final int MAX_COMMAND_SUGGESTIONS = 1000;
   public ServerPlayer player;
   public final PlayerChunkSender chunkSender;
   private int tickCount;
   private int ackBlockChangesUpTo = -1;
   private final TickThrottler chatSpamThrottler = new TickThrottler(20, 200);
   private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1480);
   private double firstGoodX;
   private double firstGoodY;
   private double firstGoodZ;
   private double lastGoodX;
   private double lastGoodY;
   private double lastGoodZ;
   private @Nullable Entity lastVehicle;
   private double vehicleFirstGoodX;
   private double vehicleFirstGoodY;
   private double vehicleFirstGoodZ;
   private double vehicleLastGoodX;
   private double vehicleLastGoodY;
   private double vehicleLastGoodZ;
   private @Nullable Vec3 awaitingPositionFromClient;
   private int awaitingTeleport;
   private int awaitingTeleportTime;
   private boolean clientIsFloating;
   private int aboveGroundTickCount;
   private boolean clientVehicleIsFloating;
   private int aboveGroundVehicleTickCount;
   private int receivedMovePacketCount;
   private int knownMovePacketCount;
   private boolean receivedMovementThisTick;
   private @Nullable RemoteChatSession chatSession;
   private SignedMessageChain.Decoder signedMessageDecoder;
   private final LastSeenMessagesValidator lastSeenMessages = new LastSeenMessagesValidator(20);
   private int nextChatIndex;
   private final MessageSignatureCache messageSignatureCache = MessageSignatureCache.createDefault();
   private final FutureChain chatMessageChain;
   private boolean waitingForSwitchToConfig;
   private boolean waitingForRespawn;
   private int clientLoadedTimeoutTimer;

   public ServerGamePacketListenerImpl(MinecraftServer p_9770_, Connection p_9771_, ServerPlayer p_9772_, CommonListenerCookie p_300908_) {
      super(p_9770_, p_9771_, p_300908_);
      this.restartClientLoadTimerAfterRespawn();
      this.chunkSender = new PlayerChunkSender(p_9771_.isMemoryConnection());
      this.player = p_9772_;
      p_9772_.connection = this;
      p_9772_.getTextFilter().join();
      this.signedMessageDecoder = SignedMessageChain.Decoder.unsigned(p_9772_.getUUID(), p_9770_::enforceSecureProfile);
      this.chatMessageChain = new FutureChain(p_9770_);
   }

   @Override
   public void tick() {
      if (this.ackBlockChangesUpTo > -1) {
         this.send(new ClientboundBlockChangedAckPacket(this.ackBlockChangesUpTo));
         this.ackBlockChangesUpTo = -1;
      }

      if (this.server.isPaused() || !this.tickPlayer()) {
         this.keepConnectionAlive();
         this.chatSpamThrottler.tick();
         this.dropSpamThrottler.tick();
         if (this.player.getLastActionTime() > 0L
            && this.server.playerIdleTimeout() > 0
            && Util.getMillis() - this.player.getLastActionTime() > TimeUnit.MINUTES.toMillis(this.server.playerIdleTimeout())
            && !this.player.wonGame) {
            this.disconnect(Component.translatable("multiplayer.disconnect.idling"));
         }
      }
   }

   private boolean tickPlayer() {
      this.resetPosition();
      this.player.xo = this.player.getX();
      this.player.yo = this.player.getY();
      this.player.zo = this.player.getZ();
      this.player.doTick();
      this.player.absSnapTo(this.firstGoodX, this.firstGoodY, this.firstGoodZ, this.player.getYRot(), this.player.getXRot());
      this.tickCount++;
      this.knownMovePacketCount = this.receivedMovePacketCount;
      if (this.clientIsFloating && !this.player.isSleeping() && !this.player.isPassenger() && !this.player.isDeadOrDying()) {
         if (++this.aboveGroundTickCount > this.getMaximumFlyingTicks(this.player)) {
            LOGGER.warn("{} was kicked for floating too long!", this.player.getPlainTextName());
            this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
            return true;
         }
      } else {
         this.clientIsFloating = false;
         this.aboveGroundTickCount = 0;
      }

      this.lastVehicle = this.player.getRootVehicle();
      if (this.lastVehicle != this.player && this.lastVehicle.getControllingPassenger() == this.player) {
         this.vehicleFirstGoodX = this.lastVehicle.getX();
         this.vehicleFirstGoodY = this.lastVehicle.getY();
         this.vehicleFirstGoodZ = this.lastVehicle.getZ();
         this.vehicleLastGoodX = this.lastVehicle.getX();
         this.vehicleLastGoodY = this.lastVehicle.getY();
         this.vehicleLastGoodZ = this.lastVehicle.getZ();
         if (this.clientVehicleIsFloating && this.lastVehicle.getControllingPassenger() == this.player) {
            if (++this.aboveGroundVehicleTickCount > this.getMaximumFlyingTicks(this.lastVehicle)) {
               LOGGER.warn("{} was kicked for floating a vehicle too long!", this.player.getPlainTextName());
               this.disconnect(Component.translatable("multiplayer.disconnect.flying"));
               return true;
            }
         } else {
            this.clientVehicleIsFloating = false;
            this.aboveGroundVehicleTickCount = 0;
         }
      } else {
         this.lastVehicle = null;
         this.clientVehicleIsFloating = false;
         this.aboveGroundVehicleTickCount = 0;
      }

      return false;
   }

   private int getMaximumFlyingTicks(Entity p_331182_) {
      double d0 = p_331182_.getGravity();
      if (d0 < 1.0E-5F) {
         return Integer.MAX_VALUE;
      }

      double d1 = 0.08 / d0;
      return Mth.ceil(80.0 * Math.max(d1, 1.0));
   }

   public void resetFlyingTicks() {
      this.aboveGroundTickCount = 0;
      this.aboveGroundVehicleTickCount = 0;
   }

   public void resetPosition() {
      this.firstGoodX = this.player.getX();
      this.firstGoodY = this.player.getY();
      this.firstGoodZ = this.player.getZ();
      this.lastGoodX = this.player.getX();
      this.lastGoodY = this.player.getY();
      this.lastGoodZ = this.player.getZ();
   }

   @Override
   public boolean isAcceptingMessages() {
      return this.connection.isConnected() && !this.waitingForSwitchToConfig;
   }

   @Override
   public boolean shouldHandleMessage(Packet<?> p_298646_) {
      return super.shouldHandleMessage(p_298646_)
         ? true
         : this.waitingForSwitchToConfig && this.connection.isConnected() && p_298646_ instanceof ServerboundConfigurationAcknowledgedPacket;
   }

   @Override
   protected GameProfile playerProfile() {
      return this.player.getGameProfile();
   }

   private <T, R> CompletableFuture<R> filterTextPacket(T p_243240_, BiFunction<TextFilter, T, CompletableFuture<R>> p_243271_) {
      return p_243271_.apply(this.player.getTextFilter(), p_243240_).thenApply(p_264862_ -> {
         if (!this.isAcceptingMessages()) {
            LOGGER.debug("Ignoring packet due to disconnection");
            throw new CancellationException("disconnected");
         } else {
            return (R)p_264862_;
         }
      });
   }

   private CompletableFuture<FilteredText> filterTextPacket(String p_243213_) {
      return this.filterTextPacket(p_243213_, TextFilter::processStreamMessage);
   }

   private CompletableFuture<List<FilteredText>> filterTextPacket(List<String> p_243258_) {
      return this.filterTextPacket(p_243258_, TextFilter::processMessageBundle);
   }

   @Override
   public void handlePlayerInput(ServerboundPlayerInputPacket p_9893_) {
      PacketUtils.ensureRunningOnSameThread(p_9893_, this, this.player.level());
      this.player.setLastClientInput(p_9893_.input());
      if (this.hasClientLoaded()) {
         this.player.resetLastActionTime();
         this.player.setShiftKeyDown(p_9893_.input().shift());
      }
   }

   private static boolean containsInvalidValues(double p_143664_, double p_143665_, double p_143666_, float p_143667_, float p_143668_) {
      return false;
   }

   private static double clampHorizontal(double p_143610_) {
      return p_143610_;
   }

   private static double clampVertical(double p_143654_) {
      return p_143654_;
   }

   @Override
   public void handleMoveVehicle(ServerboundMoveVehiclePacket p_9876_) {
      PacketUtils.ensureRunningOnSameThread(p_9876_, this, this.player.level());
      if (containsInvalidValues(p_9876_.position().x(), p_9876_.position().y(), p_9876_.position().z(), p_9876_.yRot(), p_9876_.xRot())) {
         this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
      } else if (!this.updateAwaitingTeleport() && this.hasClientLoaded()) {
         Entity entity = this.player.getRootVehicle();
         if (entity != this.player && entity.getControllingPassenger() == this.player && entity == this.lastVehicle) {
            ServerLevel serverlevel = this.player.level();
            double d0 = entity.getX();
            double d1 = entity.getY();
            double d2 = entity.getZ();
            double d3 = clampHorizontal(p_9876_.position().x());
            double d4 = clampVertical(p_9876_.position().y());
            double d5 = clampHorizontal(p_9876_.position().z());
            float f = Mth.wrapDegrees(p_9876_.yRot());
            float f1 = Mth.wrapDegrees(p_9876_.xRot());
            double d6 = d3 - this.vehicleFirstGoodX;
            double d7 = d4 - this.vehicleFirstGoodY;
            double d8 = d5 - this.vehicleFirstGoodZ;
            double d9 = entity.getDeltaMovement().lengthSqr();
            double d10 = d6 * d6 + d7 * d7 + d8 * d8;
            if (d10 - d9 > 100.0 && !this.isSingleplayerOwner()) {
               LOGGER.warn(
                  "{} (vehicle of {}) moved too quickly! {},{},{}", new Object[]{entity.getPlainTextName(), this.player.getPlainTextName(), d6, d7, d8}
               );
               this.send(ClientboundMoveVehiclePacket.fromEntity(entity));
               return;
            }

            AABB aabb = entity.getBoundingBox();
            d6 = d3 - this.vehicleLastGoodX;
            d7 = d4 - this.vehicleLastGoodY;
            d8 = d5 - this.vehicleLastGoodZ;
            boolean flag = entity.verticalCollisionBelow;
            if (entity instanceof LivingEntity livingentity && livingentity.onClimbable()) {
               livingentity.resetFallDistance();
            }

            entity.move(MoverType.PLAYER, new Vec3(d6, d7, d8));
            d6 = d3 - entity.getX();
            d7 = d4 - entity.getY();
            if (d7 > -0.5 || d7 < 0.5) {
               d7 = 0.0;
            }

            d8 = d5 - entity.getZ();
            d10 = d6 * d6 + d7 * d7 + d8 * d8;
            boolean flag1 = false;
            if (d10 > 0.0625) {
               flag1 = true;
               LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", new Object[]{entity.getPlainTextName(), this.player.getPlainTextName(), Math.sqrt(d10)});
            }

            if (flag1 && serverlevel.noCollision(entity, aabb) || this.isEntityCollidingWithAnythingNew(serverlevel, entity, aabb, d3, d4, d5)) {
               entity.absSnapTo(d0, d1, d2, f, f1);
               this.send(ClientboundMoveVehiclePacket.fromEntity(entity));
               entity.removeLatestMovementRecording();
               return;
            }

            entity.absSnapTo(d3, d4, d5, f, f1);
            this.player.level().getChunkSource().move(this.player);
            Vec3 vec3 = new Vec3(entity.getX() - d0, entity.getY() - d1, entity.getZ() - d2);
            this.handlePlayerKnownMovement(vec3);
            entity.setOnGroundWithMovement(p_9876_.onGround(), vec3);
            entity.doCheckFallDamage(vec3.x, vec3.y, vec3.z, p_9876_.onGround());
            this.player.checkMovementStatistics(vec3.x, vec3.y, vec3.z);
            this.clientVehicleIsFloating = d7 >= -0.03125
               && !flag
               && !this.server.allowFlight()
               && !entity.isFlyingVehicle()
               && !entity.isNoGravity()
               && this.noBlocksAround(entity);
            this.vehicleLastGoodX = entity.getX();
            this.vehicleLastGoodY = entity.getY();
            this.vehicleLastGoodZ = entity.getZ();
         }
      }
   }

   private boolean noBlocksAround(Entity p_9794_) {
      return p_9794_.level()
         .getBlockStates(p_9794_.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0))
         .allMatch(BlockBehaviour.BlockStateBase::isAir);
   }

   @Override
   public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket p_9835_) {
      PacketUtils.ensureRunningOnSameThread(p_9835_, this, this.player.level());
      if (p_9835_.getId() == this.awaitingTeleport) {
         if (this.awaitingPositionFromClient == null) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
            return;
         }

         this.player
            .absSnapTo(
               this.awaitingPositionFromClient.x,
               this.awaitingPositionFromClient.y,
               this.awaitingPositionFromClient.z,
               this.player.getYRot(),
               this.player.getXRot()
            );
         this.lastGoodX = this.awaitingPositionFromClient.x;
         this.lastGoodY = this.awaitingPositionFromClient.y;
         this.lastGoodZ = this.awaitingPositionFromClient.z;
         this.player.hasChangedDimension();
         this.awaitingPositionFromClient = null;
      }
   }

   @Override
   public void handleAcceptPlayerLoad(ServerboundPlayerLoadedPacket p_376469_) {
      PacketUtils.ensureRunningOnSameThread(p_376469_, this, this.player.level());
      this.markClientLoaded();
   }

   @Override
   public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket p_9897_) {
      PacketUtils.ensureRunningOnSameThread(p_9897_, this, this.player.level());
      RecipeManager.ServerDisplayInfo recipemanager$serverdisplayinfo = this.server.getRecipeManager().getRecipeFromDisplay(p_9897_.recipe());
      if (recipemanager$serverdisplayinfo != null) {
         this.player.getRecipeBook().removeHighlight(recipemanager$serverdisplayinfo.parent().id());
      }
   }

   @Override
   public void handleBundleItemSelectedPacket(ServerboundSelectBundleItemPacket p_365497_) {
      PacketUtils.ensureRunningOnSameThread(p_365497_, this, this.player.level());
      this.player.containerMenu.setSelectedBundleItemIndex(p_365497_.slotId(), p_365497_.selectedItemIndex());
   }

   @Override
   public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket p_9895_) {
      PacketUtils.ensureRunningOnSameThread(p_9895_, this, this.player.level());
      this.player.getRecipeBook().setBookSetting(p_9895_.getBookType(), p_9895_.isOpen(), p_9895_.isFiltering());
   }

   @Override
   public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket p_9903_) {
      PacketUtils.ensureRunningOnSameThread(p_9903_, this, this.player.level());
      if (p_9903_.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         Identifier identifier = Objects.requireNonNull(p_9903_.getTab());
         AdvancementHolder advancementholder = this.server.getAdvancements().get(identifier);
         if (advancementholder != null) {
            this.player.getAdvancements().setSelectedTab(advancementholder);
         }
      }
   }

   @Override
   public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket p_9847_) {
      PacketUtils.ensureRunningOnSameThread(p_9847_, this, this.player.level());
      StringReader stringreader = new StringReader(p_9847_.getCommand());
      if (stringreader.canRead() && stringreader.peek() == '/') {
         stringreader.skip();
      }

      ParseResults<CommandSourceStack> parseresults = this.server.getCommands().getDispatcher().parse(stringreader, this.player.createCommandSourceStack());
      this.server.getCommands().getDispatcher().getCompletionSuggestions(parseresults).thenAccept(p_326457_ -> {
         Suggestions suggestions = p_326457_.getList().size() <= 1000 ? p_326457_ : new Suggestions(p_326457_.getRange(), p_326457_.getList().subList(0, 1000));
         this.send(new ClientboundCommandSuggestionsPacket(p_9847_.getId(), suggestions));
      });
   }

   @Override
   public void handleSetCommandBlock(ServerboundSetCommandBlockPacket p_9911_) {
      PacketUtils.ensureRunningOnSameThread(p_9911_, this, this.player.level());
      if (!this.player.canUseGameMasterBlocks()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
      } else {
         BaseCommandBlock basecommandblock = null;
         CommandBlockEntity commandblockentity = null;
         BlockPos blockpos = p_9911_.getPos();
         BlockEntity blockentity = this.player.level().getBlockEntity(blockpos);
         if (blockentity instanceof CommandBlockEntity commandblockentity1) {
            commandblockentity = commandblockentity1;
            basecommandblock = commandblockentity1.getCommandBlock();
         }

         String s = p_9911_.getCommand();
         boolean flag = p_9911_.isTrackOutput();
         if (basecommandblock != null) {
            CommandBlockEntity.Mode commandblockentity$mode = commandblockentity.getMode();
            BlockState blockstate = this.player.level().getBlockState(blockpos);
            Direction direction = blockstate.getValue(CommandBlock.FACING);

            BlockState blockstate1 = switch (p_9911_.getMode()) {
               case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
               case AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
               default -> Blocks.COMMAND_BLOCK.defaultBlockState();
            };
            BlockState blockstate2 = blockstate1.setValue(CommandBlock.FACING, direction).setValue(CommandBlock.CONDITIONAL, p_9911_.isConditional());
            if (blockstate2 != blockstate) {
               this.player.level().setBlock(blockpos, blockstate2, 2);
               blockentity.setBlockState(blockstate2);
               this.player.level().getChunkAt(blockpos).setBlockEntity(blockentity);
            }

            basecommandblock.setCommand(s);
            basecommandblock.setTrackOutput(flag);
            if (!flag) {
               basecommandblock.setLastOutput(null);
            }

            commandblockentity.setAutomatic(p_9911_.isAutomatic());
            if (commandblockentity$mode != p_9911_.getMode()) {
               commandblockentity.onModeSwitch();
            }

            if (this.player.level().isCommandBlockEnabled()) {
               basecommandblock.onUpdated(this.player.level());
            }

            if (!StringUtil.isNullOrEmpty(s)) {
               this.player
                  .sendSystemMessage(
                     Component.translatable(this.player.level().isCommandBlockEnabled() ? "advMode.setCommand.success" : "advMode.setCommand.disabled", s)
                  );
            }
         }
      }
   }

   @Override
   public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket p_9913_) {
      PacketUtils.ensureRunningOnSameThread(p_9913_, this, this.player.level());
      if (!this.player.canUseGameMasterBlocks()) {
         this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
      } else {
         BaseCommandBlock basecommandblock = p_9913_.getCommandBlock(this.player.level());
         if (basecommandblock != null) {
            String s = p_9913_.getCommand();
            basecommandblock.setCommand(s);
            basecommandblock.setTrackOutput(p_9913_.isTrackOutput());
            if (!p_9913_.isTrackOutput()) {
               basecommandblock.setLastOutput(null);
            }

            boolean flag = this.player.level().isCommandBlockEnabled();
            if (flag) {
               basecommandblock.onUpdated(this.player.level());
            }

            if (!StringUtil.isNullOrEmpty(s)) {
               this.player.sendSystemMessage(Component.translatable(flag ? "advMode.setCommand.success" : "advMode.setCommand.disabled", s));
            }
         }
      }
   }

   @Override
   public void handlePickItemFromBlock(ServerboundPickItemFromBlockPacket p_376361_) {
      ServerLevel serverlevel = this.player.level();
      PacketUtils.ensureRunningOnSameThread(p_376361_, this, serverlevel);
      BlockPos blockpos = p_376361_.pos();
      if (this.player.isWithinBlockInteractionRange(blockpos, 1.0) && serverlevel.isLoaded(blockpos)) {
         BlockState blockstate = serverlevel.getBlockState(blockpos);
         boolean flag = this.player.hasInfiniteMaterials() && p_376361_.includeData();
         ItemStack itemstack = blockstate.getCloneItemStack(serverlevel, blockpos, flag);
         if (!itemstack.isEmpty()) {
            if (flag) {
               addBlockDataToItem(blockstate, serverlevel, blockpos, itemstack);
            }

            this.tryPickItem(itemstack);
         }
      }
   }

   private static void addBlockDataToItem(BlockState p_377004_, ServerLevel p_375777_, BlockPos p_376636_, ItemStack p_377811_) {
      BlockEntity blockentity = p_377004_.hasBlockEntity() ? p_375777_.getBlockEntity(p_376636_) : null;
      if (blockentity != null) {
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(blockentity.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_375777_.registryAccess());
            blockentity.saveCustomOnly(tagvalueoutput);
            blockentity.removeComponentsFromTag(tagvalueoutput);
            BlockItem.setBlockEntityData(p_377811_, blockentity.getType(), tagvalueoutput);
            p_377811_.applyComponents(blockentity.collectComponents());
         }
      }
   }

   @Override
   public void handlePickItemFromEntity(ServerboundPickItemFromEntityPacket p_375821_) {
      ServerLevel serverlevel = this.player.level();
      PacketUtils.ensureRunningOnSameThread(p_375821_, this, serverlevel);
      Entity entity = serverlevel.getEntityOrPart(p_375821_.id());
      if (entity != null && this.player.isWithinEntityInteractionRange(entity, 3.0)) {
         ItemStack itemstack = entity.getPickResult();
         if (itemstack != null && !itemstack.isEmpty()) {
            this.tryPickItem(itemstack);
         }
      }
   }

   private void tryPickItem(ItemStack p_377802_) {
      if (p_377802_.isItemEnabled(this.player.level().enabledFeatures())) {
         Inventory inventory = this.player.getInventory();
         int i = inventory.findSlotMatchingItem(p_377802_);
         if (i != -1) {
            if (Inventory.isHotbarSlot(i)) {
               inventory.setSelectedSlot(i);
            } else {
               inventory.pickSlot(i);
            }
         } else if (this.player.hasInfiniteMaterials()) {
            inventory.addAndPickItem(p_377802_);
         }

         this.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
         this.player.inventoryMenu.broadcastChanges();
      }
   }

   @Override
   public void handleRenameItem(ServerboundRenameItemPacket p_9899_) {
      PacketUtils.ensureRunningOnSameThread(p_9899_, this, this.player.level());
      if (this.player.containerMenu instanceof AnvilMenu anvilmenu) {
         if (!anvilmenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, anvilmenu);
            return;
         }

         anvilmenu.setItemName(p_9899_.getName());
      }
   }

   @Override
   public void handleSetBeaconPacket(ServerboundSetBeaconPacket p_9907_) {
      PacketUtils.ensureRunningOnSameThread(p_9907_, this, this.player.level());
      if (this.player.containerMenu instanceof BeaconMenu beaconmenu) {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            return;
         }

         beaconmenu.updateEffects(p_9907_.primary(), p_9907_.secondary());
      }
   }

   @Override
   public void handleSetStructureBlock(ServerboundSetStructureBlockPacket p_9919_) {
      PacketUtils.ensureRunningOnSameThread(p_9919_, this, this.player.level());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos blockpos = p_9919_.getPos();
         BlockState blockstate = this.player.level().getBlockState(blockpos);
         if (this.player.level().getBlockEntity(blockpos) instanceof StructureBlockEntity structureblockentity) {
            structureblockentity.setMode(p_9919_.getMode());
            structureblockentity.setStructureName(p_9919_.getName());
            structureblockentity.setStructurePos(p_9919_.getOffset());
            structureblockentity.setStructureSize(p_9919_.getSize());
            structureblockentity.setMirror(p_9919_.getMirror());
            structureblockentity.setRotation(p_9919_.getRotation());
            structureblockentity.setMetaData(p_9919_.getData());
            structureblockentity.setIgnoreEntities(p_9919_.isIgnoreEntities());
            structureblockentity.setStrict(p_9919_.isStrict());
            structureblockentity.setShowAir(p_9919_.isShowAir());
            structureblockentity.setShowBoundingBox(p_9919_.isShowBoundingBox());
            structureblockentity.setIntegrity(p_9919_.getIntegrity());
            structureblockentity.setSeed(p_9919_.getSeed());
            if (structureblockentity.hasStructureName()) {
               String s = structureblockentity.getStructureName();
               if (p_9919_.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                  if (structureblockentity.saveStructure()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.save_success", s), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.save_failure", s), false);
                  }
               } else if (p_9919_.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                  if (!structureblockentity.isStructureLoadable()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_not_found", s), false);
                  } else if (structureblockentity.placeStructureIfSameSize(this.player.level())) {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_success", s), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.load_prepare", s), false);
                  }
               } else if (p_9919_.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                  if (structureblockentity.detectSize()) {
                     this.player.displayClientMessage(Component.translatable("structure_block.size_success", s), false);
                  } else {
                     this.player.displayClientMessage(Component.translatable("structure_block.size_failure"), false);
                  }
               }
            } else {
               this.player.displayClientMessage(Component.translatable("structure_block.invalid_structure_name", p_9919_.getName()), false);
            }

            structureblockentity.setChanged();
            this.player.level().sendBlockUpdated(blockpos, blockstate, blockstate, 3);
         }
      }
   }

   @Override
   public void handleSetTestBlock(ServerboundSetTestBlockPacket p_397113_) {
      PacketUtils.ensureRunningOnSameThread(p_397113_, this, this.player.level());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos blockpos = p_397113_.position();
         BlockState blockstate = this.player.level().getBlockState(blockpos);
         if (this.player.level().getBlockEntity(blockpos) instanceof TestBlockEntity testblockentity) {
            testblockentity.setMode(p_397113_.mode());
            testblockentity.setMessage(p_397113_.message());
            testblockentity.setChanged();
            this.player.level().sendBlockUpdated(blockpos, blockstate, testblockentity.getBlockState(), 3);
         }
      }
   }

   @Override
   public void handleTestInstanceBlockAction(ServerboundTestInstanceBlockActionPacket p_393678_) {
      PacketUtils.ensureRunningOnSameThread(p_393678_, this, this.player.level());
      BlockPos blockpos = p_393678_.pos();
      if (this.player.canUseGameMasterBlocks() && this.player.level().getBlockEntity(blockpos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
         if (p_393678_.action() != ServerboundTestInstanceBlockActionPacket.Action.QUERY
            && p_393678_.action() != ServerboundTestInstanceBlockActionPacket.Action.INIT) {
            testinstanceblockentity.set(p_393678_.data());
            if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.RESET) {
               testinstanceblockentity.resetTest(this.player::sendSystemMessage);
            } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.SAVE) {
               testinstanceblockentity.saveTest(this.player::sendSystemMessage);
            } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.EXPORT) {
               testinstanceblockentity.exportTest(this.player::sendSystemMessage);
            } else if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.RUN) {
               testinstanceblockentity.runTest(this.player::sendSystemMessage);
            }

            BlockState blockstate = this.player.level().getBlockState(blockpos);
            this.player.level().sendBlockUpdated(blockpos, Blocks.AIR.defaultBlockState(), blockstate, 3);
         } else {
            Registry<GameTestInstance> registry = this.player.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
            Optional<Holder.Reference<GameTestInstance>> optional = p_393678_.data().test().flatMap(registry::get);
            Component component;
            if (optional.isPresent()) {
               component = optional.get().value().describe();
            } else {
               component = Component.translatable("test_instance.description.no_test").withStyle(ChatFormatting.RED);
            }

            Optional<Vec3i> optional1;
            if (p_393678_.action() == ServerboundTestInstanceBlockActionPacket.Action.QUERY) {
               optional1 = p_393678_.data()
                  .test()
                  .flatMap(p_405224_ -> TestInstanceBlockEntity.getStructureSize(this.player.level(), (ResourceKey<GameTestInstance>)p_405224_));
            } else {
               optional1 = Optional.empty();
            }

            this.connection.send(new ClientboundTestInstanceBlockStatus(component, optional1));
         }
      }
   }

   @Override
   public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket p_9917_) {
      PacketUtils.ensureRunningOnSameThread(p_9917_, this, this.player.level());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos blockpos = p_9917_.getPos();
         BlockState blockstate = this.player.level().getBlockState(blockpos);
         if (this.player.level().getBlockEntity(blockpos) instanceof JigsawBlockEntity jigsawblockentity) {
            jigsawblockentity.setName(p_9917_.getName());
            jigsawblockentity.setTarget(p_9917_.getTarget());
            jigsawblockentity.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, p_9917_.getPool()));
            jigsawblockentity.setFinalState(p_9917_.getFinalState());
            jigsawblockentity.setJoint(p_9917_.getJoint());
            jigsawblockentity.setPlacementPriority(p_9917_.getPlacementPriority());
            jigsawblockentity.setSelectionPriority(p_9917_.getSelectionPriority());
            jigsawblockentity.setChanged();
            this.player.level().sendBlockUpdated(blockpos, blockstate, blockstate, 3);
         }
      }
   }

   @Override
   public void handleJigsawGenerate(ServerboundJigsawGeneratePacket p_9868_) {
      PacketUtils.ensureRunningOnSameThread(p_9868_, this, this.player.level());
      if (this.player.canUseGameMasterBlocks()) {
         BlockPos blockpos = p_9868_.getPos();
         if (this.player.level().getBlockEntity(blockpos) instanceof JigsawBlockEntity jigsawblockentity) {
            jigsawblockentity.generate(this.player.level(), p_9868_.levels(), p_9868_.keepJigsaws());
         }
      }
   }

   @Override
   public void handleSelectTrade(ServerboundSelectTradePacket p_9905_) {
      PacketUtils.ensureRunningOnSameThread(p_9905_, this, this.player.level());
      int i = p_9905_.getItem();
      if (this.player.containerMenu instanceof MerchantMenu merchantmenu) {
         if (!merchantmenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, merchantmenu);
            return;
         }

         merchantmenu.setSelectionHint(i);
         merchantmenu.tryMoveItems(i);
      }
   }

   @Override
   public void handleEditBook(ServerboundEditBookPacket p_9862_) {
      int i = p_9862_.slot();
      if (Inventory.isHotbarSlot(i) || i == 40) {
         List<String> list = Lists.newArrayList();
         Optional<String> optional = p_9862_.title();
         optional.ifPresent(list::add);
         list.addAll(p_9862_.pages());
         Consumer<List<FilteredText>> consumer = optional.isPresent()
            ? p_238198_ -> this.signBook(p_238198_.get(0), p_238198_.subList(1, p_238198_.size()), i)
            : p_143627_ -> this.updateBookContents(p_143627_, i);
         this.filterTextPacket(list).thenAcceptAsync(consumer, this.server);
      }
   }

   private void updateBookContents(List<FilteredText> p_9813_, int p_9814_) {
      ItemStack itemstack = this.player.getInventory().getItem(p_9814_);
      if (itemstack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
         List<Filterable<String>> list = p_9813_.stream().map(this::filterableFromOutgoing).toList();
         itemstack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(list));
      }
   }

   private void signBook(FilteredText p_215209_, List<FilteredText> p_215210_, int p_215211_) {
      ItemStack itemstack = this.player.getInventory().getItem(p_215211_);
      if (itemstack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
         ItemStack itemstack1 = itemstack.transmuteCopy(Items.WRITTEN_BOOK);
         itemstack1.remove(DataComponents.WRITABLE_BOOK_CONTENT);
         List<Filterable<Component>> list = p_215210_.stream().map(p_326455_ -> this.filterableFromOutgoing(p_326455_).map(Component::literal)).toList();
         itemstack1.set(
            DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(this.filterableFromOutgoing(p_215209_), this.player.getPlainTextName(), 0, list, true)
         );
         this.player.getInventory().setItem(p_215211_, itemstack1);
      }
   }

   private Filterable<String> filterableFromOutgoing(FilteredText p_328315_) {
      return this.player.isTextFilteringEnabled() ? Filterable.passThrough(p_328315_.filteredOrEmpty()) : Filterable.from(p_328315_);
   }

   @Override
   public void handleEntityTagQuery(ServerboundEntityTagQueryPacket p_328694_) {
      PacketUtils.ensureRunningOnSameThread(p_328694_, this, this.player.level());
      if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
         Entity entity = this.player.level().getEntity(p_328694_.getEntityId());
         if (entity != null) {
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
               TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, entity.registryAccess());
               entity.saveWithoutId(tagvalueoutput);
               CompoundTag compoundtag = tagvalueoutput.buildResult();
               this.send(new ClientboundTagQueryPacket(p_328694_.getTransactionId(), compoundtag));
            }
         }
      }
   }

   @Override
   public void handleContainerSlotStateChanged(ServerboundContainerSlotStateChangedPacket p_309524_) {
      PacketUtils.ensureRunningOnSameThread(p_309524_, this, this.player.level());
      if (!this.player.isSpectator()
         && p_309524_.containerId() == this.player.containerMenu.containerId
         && this.player.containerMenu instanceof CrafterMenu craftermenu
         && craftermenu.getContainer() instanceof CrafterBlockEntity crafterblockentity) {
         crafterblockentity.setSlotState(p_309524_.slotId(), p_309524_.newState());
      }
   }

   @Override
   public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket p_330554_) {
      PacketUtils.ensureRunningOnSameThread(p_330554_, this, this.player.level());
      if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
         BlockEntity blockentity = this.player.level().getBlockEntity(p_330554_.getPos());
         CompoundTag compoundtag = blockentity != null ? blockentity.saveWithoutMetadata(this.player.registryAccess()) : null;
         this.send(new ClientboundTagQueryPacket(p_330554_.getTransactionId(), compoundtag));
      }
   }

   @Override
   public void handleMovePlayer(ServerboundMovePlayerPacket p_9874_) {
      PacketUtils.ensureRunningOnSameThread(p_9874_, this, this.player.level());
      if (containsInvalidValues(p_9874_.getX(0.0), p_9874_.getY(0.0), p_9874_.getZ(0.0), p_9874_.getYRot(0.0F), p_9874_.getXRot(0.0F))) {
         this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
      } else {
         ServerLevel serverlevel = this.player.level();
         if (!this.player.wonGame) {
            if (this.tickCount == 0) {
               this.resetPosition();
            }

            if (this.hasClientLoaded()) {
               float f = Mth.wrapDegrees(p_9874_.getYRot(this.player.getYRot()));
               float f1 = Mth.wrapDegrees(p_9874_.getXRot(this.player.getXRot()));
               if (this.updateAwaitingTeleport()) {
                  this.player.absSnapRotationTo(f, f1);
               } else {
                  double d0 = clampHorizontal(p_9874_.getX(this.player.getX()));
                  double d1 = clampVertical(p_9874_.getY(this.player.getY()));
                  double d2 = clampHorizontal(p_9874_.getZ(this.player.getZ()));
                  if (this.player.isPassenger()) {
                     this.player.absSnapTo(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                     this.player.level().getChunkSource().move(this.player);
                  } else {
                     double d3 = this.player.getX();
                     double d4 = this.player.getY();
                     double d5 = this.player.getZ();
                     double d6 = d0 - this.firstGoodX;
                     double d7 = d1 - this.firstGoodY;
                     double d8 = d2 - this.firstGoodZ;
                     double d9 = this.player.getDeltaMovement().lengthSqr();
                     double d10 = d6 * d6 + d7 * d7 + d8 * d8;
                     if (this.player.isSleeping()) {
                        if (d10 > 1.0) {
                           this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), f, f1);
                        }
                     } else {
                        boolean flag = this.player.isFallFlying();
                        if (serverlevel.tickRateManager().runsNormally()) {
                           this.receivedMovePacketCount++;
                           int i = this.receivedMovePacketCount - this.knownMovePacketCount;
                           if (i > 5) {
                              LOGGER.debug("{} is sending move packets too frequently ({} packets since last tick)", this.player.getPlainTextName(), i);
                              i = 1;
                           }

                           if (this.shouldCheckPlayerMovement(flag)) {
                              float f2 = flag ? 300.0F : 100.0F;
                              if (d10 - d9 > f2 * i) {
                                 LOGGER.warn("{} moved too quickly! {},{},{}", new Object[]{this.player.getPlainTextName(), d6, d7, d8});
                                 this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                                 return;
                              }
                           }
                        }

                        AABB aabb = this.player.getBoundingBox();
                        d6 = d0 - this.lastGoodX;
                        d7 = d1 - this.lastGoodY;
                        d8 = d2 - this.lastGoodZ;
                        boolean flag4 = d7 > 0.0;
                        if (this.player.onGround() && !p_9874_.isOnGround() && flag4) {
                           this.player.jumpFromGround();
                        }

                        boolean flag1 = this.player.verticalCollisionBelow;
                        this.player.move(MoverType.PLAYER, new Vec3(d6, d7, d8));
                        d6 = d0 - this.player.getX();
                        d7 = d1 - this.player.getY();
                        if (d7 > -0.5 || d7 < 0.5) {
                           d7 = 0.0;
                        }

                        d8 = d2 - this.player.getZ();
                        d10 = d6 * d6 + d7 * d7 + d8 * d8;
                        boolean flag2 = false;
                        if (!this.player.isChangingDimension()
                           && d10 > 0.0625
                           && !this.player.isSleeping()
                           && !this.player.isCreative()
                           && !this.player.isSpectator()
                           && !this.player.isInPostImpulseGraceTime()) {
                           flag2 = true;
                           LOGGER.warn("{} moved wrongly!", this.player.getPlainTextName());
                        }

                        if (this.player.noPhysics
                           || this.player.isSleeping()
                           || (!flag2 || !serverlevel.noCollision(this.player, aabb))
                              && !this.isEntityCollidingWithAnythingNew(serverlevel, this.player, aabb, d0, d1, d2)) {
                           this.player.absSnapTo(d0, d1, d2, f, f1);
                           boolean flag3 = this.player.isAutoSpinAttack();
                           this.clientIsFloating = d7 >= -0.03125
                              && !flag1
                              && !this.player.isSpectator()
                              && !this.server.allowFlight()
                              && !this.player.getAbilities().mayfly
                              && !this.player.hasEffect(MobEffects.LEVITATION)
                              && !flag
                              && !flag3
                              && this.noBlocksAround(this.player);
                           this.player.level().getChunkSource().move(this.player);
                           Vec3 vec3 = new Vec3(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                           this.player.setOnGroundWithMovement(p_9874_.isOnGround(), p_9874_.horizontalCollision(), vec3);
                           this.player.doCheckFallDamage(vec3.x, vec3.y, vec3.z, p_9874_.isOnGround());
                           this.handlePlayerKnownMovement(vec3);
                           if (flag4) {
                              this.player.resetFallDistance();
                           }

                           if (p_9874_.isOnGround()
                              || this.player.hasLandedInLiquid()
                              || this.player.onClimbable()
                              || this.player.isSpectator()
                              || flag
                              || flag3) {
                              this.player.tryResetCurrentImpulseContext();
                           }

                           this.player.checkMovementStatistics(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5);
                           this.lastGoodX = this.vehicleLastGoodX;
                           this.lastGoodY = this.vehicleLastGoodY;
                           this.lastGoodZ = this.vehicleLastGoodZ;
                        } else {
                           this.teleport(d3, d4, d5, f, f1);
                           this.player.doCheckFallDamage(this.player.getX() - d3, this.player.getY() - d4, this.player.getZ() - d5, p_9874_.isOnGround());
                           this.player.removeLatestMovementRecording();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldCheckPlayerMovement(boolean p_366249_) {
      if (this.isSingleplayerOwner()) {
         return false;
      }

      if (this.player.isChangingDimension()) {
         return false;
      }

      GameRules gamerules = this.player.level().getGameRules();
      return gamerules.get(GameRules.PLAYER_MOVEMENT_CHECK) && (!p_366249_ || gamerules.get(GameRules.ELYTRA_MOVEMENT_CHECK));
   }

   private boolean updateAwaitingTeleport() {
      if (this.awaitingPositionFromClient != null) {
         if (this.tickCount - this.awaitingTeleportTime > 20) {
            this.awaitingTeleportTime = this.tickCount;
            this.teleport(
               this.awaitingPositionFromClient.x,
               this.awaitingPositionFromClient.y,
               this.awaitingPositionFromClient.z,
               this.player.getYRot(),
               this.player.getXRot()
            );
         }

         return true;
      } else {
         this.awaitingTeleportTime = this.tickCount;
         return false;
      }
   }

   private boolean isEntityCollidingWithAnythingNew(
      LevelReader p_407553_, Entity p_406012_, AABB p_409267_, double p_406132_, double p_409660_, double p_410365_
   ) {
      AABB aabb = p_406012_.getBoundingBox().move(p_406132_ - p_406012_.getX(), p_409660_ - p_406012_.getY(), p_410365_ - p_406012_.getZ());
      Iterable<VoxelShape> iterable = p_407553_.getPreMoveCollisions(p_406012_, aabb.deflate(1.0E-5F), p_409267_.getBottomCenter());
      VoxelShape voxelshape = Shapes.create(p_409267_.deflate(1.0E-5F));

      for (VoxelShape voxelshape1 : iterable) {
         if (!Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.AND)) {
            return true;
         }
      }

      return false;
   }

   public void teleport(double p_9775_, double p_9776_, double p_9777_, float p_9778_, float p_9779_) {
      this.teleport(new PositionMoveRotation(new Vec3(p_9775_, p_9776_, p_9777_), Vec3.ZERO, p_9778_, p_9779_), Collections.emptySet());
   }

   public void teleport(PositionMoveRotation p_369546_, Set<Relative> p_9786_) {
      this.awaitingTeleportTime = this.tickCount;
      if (++this.awaitingTeleport == Integer.MAX_VALUE) {
         this.awaitingTeleport = 0;
      }

      this.player.teleportSetPosition(p_369546_, p_9786_);
      this.awaitingPositionFromClient = this.player.position();
      this.send(ClientboundPlayerPositionPacket.of(this.awaitingTeleport, p_369546_, p_9786_));
   }

   @Override
   public void handlePlayerAction(ServerboundPlayerActionPacket p_9889_) {
      PacketUtils.ensureRunningOnSameThread(p_9889_, this, this.player.level());
      if (this.hasClientLoaded()) {
         BlockPos blockpos = p_9889_.getPos();
         this.player.resetLastActionTime();
         ServerboundPlayerActionPacket.Action serverboundplayeractionpacket$action = p_9889_.getAction();
         switch (serverboundplayeractionpacket$action) {
            case STAB:
               if (this.player.isSpectator()) {
                  return;
               } else {
                  ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
                  if (this.player.cannotAttackWithItem(itemstack, 5)) {
                     return;
                  }

                  PiercingWeapon piercingweapon = itemstack.get(DataComponents.PIERCING_WEAPON);
                  if (piercingweapon != null) {
                     piercingweapon.attack(this.player, EquipmentSlot.MAINHAND);
                  }

                  return;
               }
            case SWAP_ITEM_WITH_OFFHAND:
               if (!this.player.isSpectator()) {
                  ItemStack itemstack1 = this.player.getItemInHand(InteractionHand.OFF_HAND);
                  this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
                  this.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack1);
                  this.player.stopUsingItem();
               }

               return;
            case DROP_ITEM:
               if (!this.player.isSpectator()) {
                  this.player.drop(false);
               }

               return;
            case DROP_ALL_ITEMS:
               if (!this.player.isSpectator()) {
                  this.player.drop(true);
               }

               return;
            case RELEASE_USE_ITEM:
               this.player.releaseUsingItem();
               return;
            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
               this.player
                  .gameMode
                  .handleBlockBreakAction(
                     blockpos, serverboundplayeractionpacket$action, p_9889_.getDirection(), this.player.level().getMaxY(), p_9889_.getSequence()
                  );
               this.ackBlockChangesUpTo(p_9889_.getSequence());
               return;
            default:
               throw new IllegalArgumentException("Invalid player action");
         }
      }
   }

   private static boolean wasBlockPlacementAttempt(ServerPlayer p_9791_, ItemStack p_9792_) {
      if (p_9792_.isEmpty()) {
         return false;
      }

      Item item = p_9792_.getItem();
      return (item instanceof BlockItem || item instanceof BucketItem bucketitem && bucketitem.getContent() != Fluids.EMPTY)
         && !p_9791_.getCooldowns().isOnCooldown(p_9792_);
   }

   @Override
   public void handleUseItemOn(ServerboundUseItemOnPacket p_9930_) {
      PacketUtils.ensureRunningOnSameThread(p_9930_, this, this.player.level());
      if (this.hasClientLoaded()) {
         this.ackBlockChangesUpTo(p_9930_.getSequence());
         ServerLevel serverlevel = this.player.level();
         InteractionHand interactionhand = p_9930_.getHand();
         ItemStack itemstack = this.player.getItemInHand(interactionhand);
         if (itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
            BlockHitResult blockhitresult = p_9930_.getHitResult();
            Vec3 vec3 = blockhitresult.getLocation();
            BlockPos blockpos = blockhitresult.getBlockPos();
            if (this.player.isWithinBlockInteractionRange(blockpos, 1.0)) {
               Vec3 vec31 = vec3.subtract(Vec3.atCenterOf(blockpos));
               double d0 = 1.0000001;
               if (Math.abs(vec31.x()) < 1.0000001 && Math.abs(vec31.y()) < 1.0000001 && Math.abs(vec31.z()) < 1.0000001) {
                  Direction direction = blockhitresult.getDirection();
                  this.player.resetLastActionTime();
                  int i = this.player.level().getMaxY();
                  if (blockpos.getY() <= i) {
                     if (this.awaitingPositionFromClient == null && serverlevel.mayInteract(this.player, blockpos)) {
                        InteractionResult interactionresult = this.player
                           .gameMode
                           .useItemOn(this.player, serverlevel, itemstack, interactionhand, blockhitresult);
                        if (interactionresult.consumesAction()) {
                           CriteriaTriggers.ANY_BLOCK_USE.trigger(this.player, blockhitresult.getBlockPos(), itemstack.copy());
                        }

                        if (direction == Direction.UP
                           && !interactionresult.consumesAction()
                           && blockpos.getY() >= i
                           && wasBlockPlacementAttempt(this.player, itemstack)) {
                           Component component = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
                           this.player.sendSystemMessage(component, true);
                        } else if (interactionresult instanceof InteractionResult.Success interactionresult$success
                           && interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
                           this.player.swing(interactionhand, true);
                        }
                     }
                  } else {
                     Component component1 = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
                     this.player.sendSystemMessage(component1, true);
                  }

                  this.send(new ClientboundBlockUpdatePacket(serverlevel, blockpos));
                  this.send(new ClientboundBlockUpdatePacket(serverlevel, blockpos.relative(direction)));
               } else {
                  LOGGER.warn(
                     "Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.",
                     new Object[]{this.player.getGameProfile().name(), vec3, blockpos}
                  );
               }
            }
         }
      }
   }

   @Override
   public void handleUseItem(ServerboundUseItemPacket p_9932_) {
      PacketUtils.ensureRunningOnSameThread(p_9932_, this, this.player.level());
      if (this.hasClientLoaded()) {
         this.ackBlockChangesUpTo(p_9932_.getSequence());
         ServerLevel serverlevel = this.player.level();
         InteractionHand interactionhand = p_9932_.getHand();
         ItemStack itemstack = this.player.getItemInHand(interactionhand);
         this.player.resetLastActionTime();
         if (!itemstack.isEmpty() && itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
            float f = Mth.wrapDegrees(p_9932_.getYRot());
            float f1 = Mth.wrapDegrees(p_9932_.getXRot());
            if (f1 != this.player.getXRot() || f != this.player.getYRot()) {
               this.player.absSnapRotationTo(f, f1);
            }

            if (this.player.gameMode.useItem(this.player, serverlevel, itemstack, interactionhand) instanceof InteractionResult.Success interactionresult$success
               && interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
               this.player.swing(interactionhand, true);
            }
         }
      }
   }

   @Override
   public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket p_9928_) {
      PacketUtils.ensureRunningOnSameThread(p_9928_, this, this.player.level());
      if (this.player.isSpectator()) {
         for (ServerLevel serverlevel : this.server.getAllLevels()) {
            Entity entity = p_9928_.getEntity(serverlevel);
            if (entity != null) {
               this.player.teleportTo(serverlevel, entity.getX(), entity.getY(), entity.getZ(), Set.of(), entity.getYRot(), entity.getXRot(), true);
               return;
            }
         }
      }
   }

   @Override
   public void handlePaddleBoat(ServerboundPaddleBoatPacket p_9878_) {
      PacketUtils.ensureRunningOnSameThread(p_9878_, this, this.player.level());
      if (this.player.getControlledVehicle() instanceof AbstractBoat abstractboat) {
         abstractboat.setPaddleState(p_9878_.getLeft(), p_9878_.getRight());
      }
   }

   @Override
   public void onDisconnect(DisconnectionDetails p_342442_) {
      LOGGER.info("{} lost connection: {}", this.player.getPlainTextName(), p_342442_.reason().getString());
      this.removePlayerFromWorld();
      super.onDisconnect(p_342442_);
   }

   private void removePlayerFromWorld() {
      this.chatMessageChain.close();
      this.server.invalidateStatus();
      this.server
         .getPlayerList()
         .broadcastSystemMessage(Component.translatable("multiplayer.player.left", this.player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
      this.player.disconnect();
      this.server.getPlayerList().remove(this.player);
      this.player.getTextFilter().leave();
   }

   public void ackBlockChangesUpTo(int p_215202_) {
      if (p_215202_ < 0) {
         throw new IllegalArgumentException("Expected packet sequence nr >= 0");
      }

      this.ackBlockChangesUpTo = Math.max(p_215202_, this.ackBlockChangesUpTo);
   }

   @Override
   public void handleSetCarriedItem(ServerboundSetCarriedItemPacket p_9909_) {
      PacketUtils.ensureRunningOnSameThread(p_9909_, this, this.player.level());
      if (p_9909_.getSlot() >= 0 && p_9909_.getSlot() < Inventory.getSelectionSize()) {
         if (this.player.getInventory().getSelectedSlot() != p_9909_.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            this.player.stopUsingItem();
         }

         this.player.getInventory().setSelectedSlot(p_9909_.getSlot());
         this.player.resetLastActionTime();
      } else {
         LOGGER.warn("{} tried to set an invalid carried item", this.player.getPlainTextName());
      }
   }

   @Override
   public void handleChat(ServerboundChatPacket p_9841_) {
      Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(p_9841_.lastSeenMessages());
      if (!optional.isEmpty()) {
         this.tryHandleChat(p_9841_.message(), false, () -> {
            PlayerChatMessage playerchatmessage;
            try {
               playerchatmessage = this.getSignedMessage(p_9841_, optional.get());
            } catch (SignedMessageChain.DecodeException signedmessagechain$decodeexception) {
               this.handleMessageDecodeFailure(signedmessagechain$decodeexception);
               return;
            }

            CompletableFuture<FilteredText> completablefuture = this.filterTextPacket(playerchatmessage.signedContent());
            Component component = this.server.getChatDecorator().decorate(this.player, playerchatmessage.decoratedContent());
            this.chatMessageChain.append(completablefuture, p_296589_ -> {
               PlayerChatMessage playerchatmessage1 = playerchatmessage.withUnsignedContent(component).filter(p_296589_.mask());
               this.broadcastChatMessage(playerchatmessage1);
            });
         });
      }
   }

   @Override
   public void handleChatCommand(ServerboundChatCommandPacket p_215225_) {
      this.tryHandleChat(p_215225_.command(), true, () -> {
         this.performUnsignedChatCommand(p_215225_.command());
         this.detectRateSpam();
      });
   }

   private void performUnsignedChatCommand(String p_334078_) {
      ParseResults<CommandSourceStack> parseresults = this.parseCommand(p_334078_);
      if (this.server.enforceSecureProfile() && SignableCommand.hasSignableArguments(parseresults)) {
         LOGGER.error(
            "Received unsigned command packet from {}, but the command requires signable arguments: {}", this.player.getGameProfile().name(), p_334078_
         );
         this.player.sendSystemMessage(INVALID_COMMAND_SIGNATURE);
      } else {
         this.server.getCommands().performCommand(parseresults, p_334078_);
      }
   }

   @Override
   public void handleSignedChatCommand(ServerboundChatCommandSignedPacket p_330005_) {
      Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(p_330005_.lastSeenMessages());
      if (!optional.isEmpty()) {
         this.tryHandleChat(p_330005_.command(), true, () -> {
            this.performSignedChatCommand(p_330005_, optional.get());
            this.detectRateSpam();
         });
      }
   }

   private void performSignedChatCommand(ServerboundChatCommandSignedPacket p_335197_, LastSeenMessages p_250484_) {
      ParseResults<CommandSourceStack> parseresults = this.parseCommand(p_335197_.command());

      Map<String, PlayerChatMessage> map;
      try {
         map = this.collectSignedArguments(p_335197_, SignableCommand.of(parseresults), p_250484_);
      } catch (SignedMessageChain.DecodeException signedmessagechain$decodeexception) {
         this.handleMessageDecodeFailure(signedmessagechain$decodeexception);
         return;
      }

      CommandSigningContext commandsigningcontext = new CommandSigningContext.SignedArguments(map);
      parseresults = Commands.mapSource(parseresults, p_296586_ -> p_296586_.withSigningContext(commandsigningcontext, this.chatMessageChain));
      this.server.getCommands().performCommand(parseresults, p_335197_.command());
   }

   private void handleMessageDecodeFailure(SignedMessageChain.DecodeException p_252068_) {
      LOGGER.warn("Failed to update secure chat state for {}: '{}'", this.player.getGameProfile().name(), p_252068_.getComponent().getString());
      this.player.sendSystemMessage(p_252068_.getComponent().copy().withStyle(ChatFormatting.RED));
   }

   private <S> Map<String, PlayerChatMessage> collectSignedArguments(
      ServerboundChatCommandSignedPacket p_333226_, SignableCommand<S> p_250039_, LastSeenMessages p_249207_
   ) throws SignedMessageChain.DecodeException {
      List<ArgumentSignatures.Entry> list = p_333226_.argumentSignatures().entries();
      List<SignableCommand.Argument<S>> list1 = p_250039_.arguments();
      if (list.isEmpty()) {
         return this.collectUnsignedArguments(list1);
      }

      Map<String, PlayerChatMessage> map = new Object2ObjectOpenHashMap();

      for (ArgumentSignatures.Entry argumentsignatures$entry : list) {
         SignableCommand.Argument<S> argument = p_250039_.getArgument(argumentsignatures$entry.name());
         if (argument == null) {
            this.signedMessageDecoder.setChainBroken();
            throw createSignedArgumentMismatchException(p_333226_.command(), list, list1);
         }

         SignedMessageBody signedmessagebody = new SignedMessageBody(argument.value(), p_333226_.timeStamp(), p_333226_.salt(), p_249207_);
         map.put(argument.name(), this.signedMessageDecoder.unpack(argumentsignatures$entry.signature(), signedmessagebody));
      }

      for (SignableCommand.Argument<S> argument1 : list1) {
         if (!map.containsKey(argument1.name())) {
            throw createSignedArgumentMismatchException(p_333226_.command(), list, list1);
         }
      }

      return map;
   }

   private <S> Map<String, PlayerChatMessage> collectUnsignedArguments(List<SignableCommand.Argument<S>> p_328249_) throws SignedMessageChain.DecodeException {
      Map<String, PlayerChatMessage> map = new HashMap<>();

      for (SignableCommand.Argument<S> argument : p_328249_) {
         SignedMessageBody signedmessagebody = SignedMessageBody.unsigned(argument.value());
         map.put(argument.name(), this.signedMessageDecoder.unpack(null, signedmessagebody));
      }

      return map;
   }

   private static <S> SignedMessageChain.DecodeException createSignedArgumentMismatchException(
      String p_328555_, List<ArgumentSignatures.Entry> p_331214_, List<SignableCommand.Argument<S>> p_329273_
   ) {
      String s = p_331214_.stream().map(ArgumentSignatures.Entry::name).collect(Collectors.joining(", "));
      String s1 = p_329273_.stream().map(SignableCommand.Argument::name).collect(Collectors.joining(", "));
      LOGGER.error("Signed command mismatch between server and client ('{}'): got [{}] from client, but expected [{}]", new Object[]{p_328555_, s, s1});
      return new SignedMessageChain.DecodeException(INVALID_COMMAND_SIGNATURE);
   }

   private ParseResults<CommandSourceStack> parseCommand(String p_242938_) {
      CommandDispatcher<CommandSourceStack> commanddispatcher = this.server.getCommands().getDispatcher();
      return commanddispatcher.parse(p_242938_, this.player.createCommandSourceStack());
   }

   private void tryHandleChat(String p_334915_, boolean p_424872_, Runnable p_336106_) {
      if (isChatMessageIllegal(p_334915_)) {
         this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
      } else if (!p_424872_ && this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
         this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
      } else {
         this.player.resetLastActionTime();
         this.server.execute(p_336106_);
      }
   }

   private Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update p_249673_) {
      synchronized (this.lastSeenMessages) {
         Optional optional;
         try {
            LastSeenMessages lastseenmessages = this.lastSeenMessages.applyUpdate(p_249673_);
            optional = Optional.of(lastseenmessages);
         } catch (LastSeenMessagesValidator.ValidationException lastseenmessagesvalidator$validationexception) {
            LOGGER.error(
               "Failed to validate message acknowledgements from {}: {}",
               this.player.getPlainTextName(),
               lastseenmessagesvalidator$validationexception.getMessage()
            );
            this.disconnect(CHAT_VALIDATION_FAILED);
            return Optional.empty();
         }

         return optional;
      }
   }

   private static boolean isChatMessageIllegal(String p_215215_) {
      for (int i = 0; i < p_215215_.length(); i++) {
         if (!StringUtil.isAllowedChatCharacter(p_215215_.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   private PlayerChatMessage getSignedMessage(ServerboundChatPacket p_251061_, LastSeenMessages p_250566_) throws SignedMessageChain.DecodeException {
      SignedMessageBody signedmessagebody = new SignedMessageBody(p_251061_.message(), p_251061_.timeStamp(), p_251061_.salt(), p_250566_);
      return this.signedMessageDecoder.unpack(p_251061_.signature(), signedmessagebody);
   }

   private void broadcastChatMessage(PlayerChatMessage p_243277_) {
      this.server.getPlayerList().broadcastChatMessage(p_243277_, this.player, ChatType.bind(ChatType.CHAT, this.player));
      this.detectRateSpam();
   }

   private void detectRateSpam() {
      this.chatSpamThrottler.increment();
      if (!this.chatSpamThrottler.isUnderThreshold()
         && !this.server.getPlayerList().isOp(this.player.nameAndId())
         && !this.server.isSingleplayerOwner(this.player.nameAndId())) {
         this.disconnect(Component.translatable("disconnect.spam"));
      }
   }

   @Override
   public void handleChatAck(ServerboundChatAckPacket p_242387_) {
      synchronized (this.lastSeenMessages) {
         try {
            this.lastSeenMessages.applyOffset(p_242387_.offset());
         } catch (LastSeenMessagesValidator.ValidationException lastseenmessagesvalidator$validationexception) {
            LOGGER.error(
               "Failed to validate message acknowledgement offset from {}: {}",
               this.player.getPlainTextName(),
               lastseenmessagesvalidator$validationexception.getMessage()
            );
            this.disconnect(CHAT_VALIDATION_FAILED);
         }
      }
   }

   @Override
   public void handleAnimate(ServerboundSwingPacket p_9926_) {
      PacketUtils.ensureRunningOnSameThread(p_9926_, this, this.player.level());
      this.player.resetLastActionTime();
      this.player.swing(p_9926_.getHand());
   }

   @Override
   public void handlePlayerCommand(ServerboundPlayerCommandPacket p_9891_) {
      PacketUtils.ensureRunningOnSameThread(p_9891_, this, this.player.level());
      if (this.hasClientLoaded()) {
         this.player.resetLastActionTime();
         switch (p_9891_.getAction()) {
            case START_SPRINTING:
               this.player.setSprinting(true);
               break;
            case STOP_SPRINTING:
               this.player.setSprinting(false);
               break;
            case STOP_SLEEPING:
               if (this.player.isSleeping()) {
                  this.player.stopSleepInBed(false, true);
                  this.awaitingPositionFromClient = this.player.position();
               }
               break;
            case START_RIDING_JUMP:
               if (this.player.getControlledVehicle() instanceof PlayerRideableJumping playerrideablejumping1) {
                  int i = p_9891_.getData();
                  if (playerrideablejumping1.canJump() && i > 0) {
                     playerrideablejumping1.handleStartJump(i);
                  }
               }
               break;
            case STOP_RIDING_JUMP:
               if (this.player.getControlledVehicle() instanceof PlayerRideableJumping playerrideablejumping) {
                  playerrideablejumping.handleStopJump();
               }
               break;
            case OPEN_INVENTORY:
               if (this.player.getVehicle() instanceof HasCustomInventoryScreen hascustominventoryscreen) {
                  hascustominventoryscreen.openCustomInventoryScreen(this.player);
               }
               break;
            case START_FALL_FLYING:
               if (!this.player.tryToStartFallFlying()) {
                  this.player.stopFallFlying();
               }
               break;
            default:
               throw new IllegalArgumentException("Invalid client command!");
         }
      }
   }

   public void sendPlayerChatMessage(PlayerChatMessage p_250321_, ChatType.Bound p_250910_) {
      this.send(
         new ClientboundPlayerChatPacket(
            this.nextChatIndex++,
            p_250321_.link().sender(),
            p_250321_.link().index(),
            p_250321_.signature(),
            p_250321_.signedBody().pack(this.messageSignatureCache),
            p_250321_.unsignedContent(),
            p_250321_.filterMask(),
            p_250910_
         )
      );
      MessageSignature messagesignature = p_250321_.signature();
      if (messagesignature != null) {
         this.messageSignatureCache.push(p_250321_.signedBody(), p_250321_.signature());
         int i;
         synchronized (this.lastSeenMessages) {
            this.lastSeenMessages.addPending(messagesignature);
            i = this.lastSeenMessages.trackedMessagesCount();
         }

         if (i > 4096) {
            this.disconnect(Component.translatable("multiplayer.disconnect.too_many_pending_chats"));
         }
      }
   }

   public void sendDisguisedChatMessage(Component p_251804_, ChatType.Bound p_250040_) {
      this.send(new ClientboundDisguisedChatPacket(p_251804_, p_250040_));
   }

   public SocketAddress getRemoteAddress() {
      return this.connection.getRemoteAddress();
   }

   public void switchToConfig() {
      this.waitingForSwitchToConfig = true;
      this.removePlayerFromWorld();
      this.send(ClientboundStartConfigurationPacket.INSTANCE);
      this.connection.setupOutboundProtocol(ConfigurationProtocols.CLIENTBOUND);
   }

   @Override
   public void handlePingRequest(ServerboundPingRequestPacket p_332170_) {
      this.connection.send(new ClientboundPongResponsePacket(p_332170_.getTime()));
   }

   @Override
   public void handleInteract(ServerboundInteractPacket p_9866_) {
      PacketUtils.ensureRunningOnSameThread(p_9866_, this, this.player.level());
      if (this.hasClientLoaded()) {
         final ServerLevel serverlevel = this.player.level();
         final Entity entity = p_9866_.getTarget(serverlevel);
         this.player.resetLastActionTime();
         this.player.setShiftKeyDown(p_9866_.isUsingSecondaryAction());
         if (entity != null) {
            if (!serverlevel.getWorldBorder().isWithinBounds(entity.blockPosition())) {
               return;
            }

            AABB aabb = entity.getBoundingBox();
            if (p_9866_.isWithinRange(this.player, aabb, 3.0)) {
               p_9866_.dispatch(
                  new ServerboundInteractPacket.Handler() {
                     private void performInteraction(InteractionHand p_143679_, ServerGamePacketListenerImpl.EntityInteraction p_143680_) {
                        ItemStack itemstack = ServerGamePacketListenerImpl.this.player.getItemInHand(p_143679_);
                        if (itemstack.isItemEnabled(serverlevel.enabledFeatures())) {
                           ItemStack itemstack1 = itemstack.copy();
                           if (p_143680_.run(ServerGamePacketListenerImpl.this.player, entity, p_143679_) instanceof InteractionResult.Success interactionresult$success
                              )
                            {
                              ItemStack itemstack2 = interactionresult$success.wasItemInteraction() ? itemstack1 : ItemStack.EMPTY;
                              CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(ServerGamePacketListenerImpl.this.player, itemstack2, entity);
                              if (interactionresult$success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                 ServerGamePacketListenerImpl.this.player.swing(p_143679_, true);
                              }
                           }
                        }
                     }

                     @Override
                     public void onInteraction(InteractionHand p_143677_) {
                        this.performInteraction(p_143677_, Player::interactOn);
                     }

                     @Override
                     public void onInteraction(InteractionHand p_143682_, Vec3 p_143683_) {
                        this.performInteraction(p_143682_, (p_143686_, p_143687_, p_143688_) -> p_143687_.interactAt(p_143686_, p_143683_, p_143688_));
                     }

                     @Override
                     public void onAttack() {
                        if (!(entity instanceof ItemEntity)
                           && !(entity instanceof ExperienceOrb)
                           && entity != ServerGamePacketListenerImpl.this.player
                           && !(entity instanceof AbstractArrow abstractarrow && !abstractarrow.isAttackable())) {
                           ItemStack itemstack = ServerGamePacketListenerImpl.this.player.getItemInHand(InteractionHand.MAIN_HAND);
                           if (itemstack.isItemEnabled(serverlevel.enabledFeatures())
                              && !ServerGamePacketListenerImpl.this.player.cannotAttackWithItem(itemstack, 5)) {
                              ServerGamePacketListenerImpl.this.player.attack(entity);
                           }
                        } else {
                           ServerGamePacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
                           ServerGamePacketListenerImpl.LOGGER
                              .warn("Player {} tried to attack an invalid entity", ServerGamePacketListenerImpl.this.player.getPlainTextName());
                        }
                     }
                  }
               );
            }
         }
      }
   }

   @Override
   public void handleClientCommand(ServerboundClientCommandPacket p_9843_) {
      PacketUtils.ensureRunningOnSameThread(p_9843_, this, this.player.level());
      this.player.resetLastActionTime();
      ServerboundClientCommandPacket.Action serverboundclientcommandpacket$action = p_9843_.getAction();
      switch (serverboundclientcommandpacket$action) {
         case PERFORM_RESPAWN:
            if (this.player.wonGame) {
               this.player.wonGame = false;
               this.player = this.server.getPlayerList().respawn(this.player, true, Entity.RemovalReason.CHANGED_DIMENSION);
               this.resetPosition();
               this.restartClientLoadTimerAfterRespawn();
               CriteriaTriggers.CHANGED_DIMENSION.trigger(this.player, Level.END, Level.OVERWORLD);
            } else {
               if (this.player.getHealth() > 0.0F) {
                  return;
               }

               this.player = this.server.getPlayerList().respawn(this.player, false, Entity.RemovalReason.KILLED);
               this.resetPosition();
               this.restartClientLoadTimerAfterRespawn();
               if (this.server.isHardcore()) {
                  this.player.setGameMode(GameType.SPECTATOR);
                  this.player.level().getGameRules().set(GameRules.SPECTATORS_GENERATE_CHUNKS, false, this.server);
               }
            }
            break;
         case REQUEST_STATS:
            this.player.getStats().sendStats(this.player);
      }
   }

   @Override
   public void handleContainerClose(ServerboundContainerClosePacket p_9858_) {
      PacketUtils.ensureRunningOnSameThread(p_9858_, this, this.player.level());
      this.player.doCloseContainer();
   }

   @Override
   public void handleContainerClick(ServerboundContainerClickPacket p_9856_) {
      PacketUtils.ensureRunningOnSameThread(p_9856_, this, this.player.level());
      this.player.resetLastActionTime();
      if (this.player.containerMenu.containerId == p_9856_.containerId()) {
         if (this.player.isSpectator()) {
            this.player.containerMenu.sendAllDataToRemote();
         } else if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            int i = p_9856_.slotNum();
            if (!this.player.containerMenu.isValidSlotIndex(i)) {
               LOGGER.debug(
                  "Player {} clicked invalid slot index: {}, available slots: {}",
                  new Object[]{this.player.getPlainTextName(), i, this.player.containerMenu.slots.size()}
               );
            } else {
               boolean flag = p_9856_.stateId() != this.player.containerMenu.getStateId();
               this.player.containerMenu.suppressRemoteUpdates();
               this.player.containerMenu.clicked(i, p_9856_.buttonNum(), p_9856_.clickType(), this.player);
               ObjectIterator var4 = Int2ObjectMaps.fastIterable(p_9856_.changedSlots()).iterator();

               while (var4.hasNext()) {
                  Entry<HashedStack> entry = (Entry<HashedStack>)var4.next();
                  this.player.containerMenu.setRemoteSlotUnsafe(entry.getIntKey(), (HashedStack)entry.getValue());
               }

               this.player.containerMenu.setRemoteCarried(p_9856_.carriedItem());
               this.player.containerMenu.resumeRemoteUpdates();
               if (flag) {
                  this.player.containerMenu.broadcastFullState();
               } else {
                  this.player.containerMenu.broadcastChanges();
               }
            }
         }
      }
   }

   @Override
   public void handlePlaceRecipe(ServerboundPlaceRecipePacket p_9882_) {
      PacketUtils.ensureRunningOnSameThread(p_9882_, this, this.player.level());
      this.player.resetLastActionTime();
      if (!this.player.isSpectator() && this.player.containerMenu.containerId == p_9882_.containerId()) {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            RecipeManager.ServerDisplayInfo recipemanager$serverdisplayinfo = this.server.getRecipeManager().getRecipeFromDisplay(p_9882_.recipe());
            if (recipemanager$serverdisplayinfo != null) {
               RecipeHolder<?> recipeholder = recipemanager$serverdisplayinfo.parent();
               if (this.player.getRecipeBook().contains(recipeholder.id()) && this.player.containerMenu instanceof RecipeBookMenu recipebookmenu) {
                  if (recipeholder.value().placementInfo().isImpossibleToPlace()) {
                     LOGGER.debug("Player {} tried to place impossible recipe {}", this.player, recipeholder.id().identifier());
                     return;
                  }

                  RecipeBookMenu.PostPlaceAction recipebookmenu$postplaceaction = recipebookmenu.handlePlacement(
                     p_9882_.useMaxItems(), this.player.isCreative(), recipeholder, this.player.level(), this.player.getInventory()
                  );
                  if (recipebookmenu$postplaceaction == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
                     this.send(
                        new ClientboundPlaceGhostRecipePacket(this.player.containerMenu.containerId, recipemanager$serverdisplayinfo.display().display())
                     );
                  }
               }
            }
         }
      }
   }

   @Override
   public void handleContainerButtonClick(ServerboundContainerButtonClickPacket p_9854_) {
      PacketUtils.ensureRunningOnSameThread(p_9854_, this, this.player.level());
      this.player.resetLastActionTime();
      if (this.player.containerMenu.containerId == p_9854_.containerId() && !this.player.isSpectator()) {
         if (!this.player.containerMenu.stillValid(this.player)) {
            LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
         } else {
            boolean flag = this.player.containerMenu.clickMenuButton(this.player, p_9854_.buttonId());
            if (flag) {
               this.player.containerMenu.broadcastChanges();
            }
         }
      }
   }

   @Override
   public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket p_9915_) {
      PacketUtils.ensureRunningOnSameThread(p_9915_, this, this.player.level());
      if (this.player.hasInfiniteMaterials()) {
         boolean flag = p_9915_.slotNum() < 0;
         ItemStack itemstack = p_9915_.itemStack();
         if (!itemstack.isItemEnabled(this.player.level().enabledFeatures())) {
            return;
         }

         boolean flag1 = p_9915_.slotNum() >= 1 && p_9915_.slotNum() <= 45;
         boolean flag2 = itemstack.isEmpty() || itemstack.getCount() <= itemstack.getMaxStackSize();
         if (flag1 && flag2) {
            this.player.inventoryMenu.getSlot(p_9915_.slotNum()).setByPlayer(itemstack);
            this.player.inventoryMenu.setRemoteSlot(p_9915_.slotNum(), itemstack);
            this.player.inventoryMenu.broadcastChanges();
         } else if (flag && flag2) {
            if (this.dropSpamThrottler.isUnderThreshold()) {
               this.dropSpamThrottler.increment();
               this.player.drop(itemstack, true);
            } else {
               LOGGER.warn("Player {} was dropping items too fast in creative mode, ignoring.", this.player.getPlainTextName());
            }
         }
      }
   }

   @Override
   public void handleSignUpdate(ServerboundSignUpdatePacket p_9921_) {
      List<String> list = Stream.of(p_9921_.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
      this.filterTextPacket(list).thenAcceptAsync(p_215245_ -> this.updateSignText(p_9921_, (List<FilteredText>)p_215245_), this.server);
   }

   private void updateSignText(ServerboundSignUpdatePacket p_9923_, List<FilteredText> p_9924_) {
      this.player.resetLastActionTime();
      ServerLevel serverlevel = this.player.level();
      BlockPos blockpos = p_9923_.getPos();
      if (serverlevel.hasChunkAt(blockpos)) {
         if (!(serverlevel.getBlockEntity(blockpos) instanceof SignBlockEntity signblockentity)) {
            return;
         }

         signblockentity.updateSignText(this.player, p_9923_.isFrontText(), p_9924_);
      }
   }

   @Override
   public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket p_9887_) {
      PacketUtils.ensureRunningOnSameThread(p_9887_, this, this.player.level());
      this.player.getAbilities().flying = p_9887_.isFlying() && this.player.getAbilities().mayfly;
   }

   @Override
   public void handleClientInformation(ServerboundClientInformationPacket p_298714_) {
      PacketUtils.ensureRunningOnSameThread(p_298714_, this, this.player.level());
      boolean flag = this.player.isModelPartShown(PlayerModelPart.HAT);
      this.player.updateOptions(p_298714_.information());
      if (this.player.isModelPartShown(PlayerModelPart.HAT) != flag) {
         this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT, this.player));
      }
   }

   @Override
   public void handleChangeDifficulty(ServerboundChangeDifficultyPacket p_9839_) {
      PacketUtils.ensureRunningOnSameThread(p_9839_, this, this.player.level());
      if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !this.isSingleplayerOwner()) {
         LOGGER.warn(
            "Player {} tried to change difficulty to {} without required permissions",
            this.player.getGameProfile().name(),
            p_9839_.difficulty().getDisplayName()
         );
      } else {
         this.server.setDifficulty(p_9839_.difficulty(), false);
      }
   }

   @Override
   public void handleChangeGameMode(ServerboundChangeGameModePacket p_409310_) {
      PacketUtils.ensureRunningOnSameThread(p_409310_, this, this.player.level());
      if (!GameModeCommand.PERMISSION_CHECK.check(this.player.permissions())) {
         LOGGER.warn(
            "Player {} tried to change game mode to {} without required permissions",
            this.player.getGameProfile().name(),
            p_409310_.mode().getShortDisplayName().getString()
         );
      } else {
         GameModeCommand.setGameMode(this.player, p_409310_.mode());
      }
   }

   @Override
   public void handleLockDifficulty(ServerboundLockDifficultyPacket p_9872_) {
      PacketUtils.ensureRunningOnSameThread(p_9872_, this, this.player.level());
      if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || this.isSingleplayerOwner()) {
         this.server.setDifficultyLocked(p_9872_.isLocked());
      }
   }

   @Override
   public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket p_253950_) {
      PacketUtils.ensureRunningOnSameThread(p_253950_, this, this.player.level());
      RemoteChatSession.Data remotechatsession$data = p_253950_.chatSession();
      ProfilePublicKey.Data profilepublickey$data = this.chatSession != null ? this.chatSession.profilePublicKey().data() : null;
      ProfilePublicKey.Data profilepublickey$data1 = remotechatsession$data.profilePublicKey();
      if (!Objects.equals(profilepublickey$data, profilepublickey$data1)) {
         if (profilepublickey$data != null && profilepublickey$data1.expiresAt().isBefore(profilepublickey$data.expiresAt())) {
            this.disconnect(ProfilePublicKey.EXPIRED_PROFILE_PUBLIC_KEY);
         } else {
            try {
               SignatureValidator signaturevalidator = this.server.services().profileKeySignatureValidator();
               if (signaturevalidator == null) {
                  LOGGER.warn("Ignoring chat session from {} due to missing Services public key", this.player.getGameProfile().name());
                  return;
               }

               this.resetPlayerChatState(remotechatsession$data.validate(this.player.getGameProfile(), signaturevalidator));
            } catch (ProfilePublicKey.ValidationException profilepublickey$validationexception) {
               LOGGER.error("Failed to validate profile key: {}", profilepublickey$validationexception.getMessage());
               this.disconnect(profilepublickey$validationexception.getComponent());
            }
         }
      }
   }

   @Override
   public void handleConfigurationAcknowledged(ServerboundConfigurationAcknowledgedPacket p_299199_) {
      if (!this.waitingForSwitchToConfig) {
         throw new IllegalStateException("Client acknowledged config, but none was requested");
      }

      this.connection
         .setupInboundProtocol(
            ConfigurationProtocols.SERVERBOUND,
            new ServerConfigurationPacketListenerImpl(this.server, this.connection, this.createCookie(this.player.clientInformation()))
         );
   }

   @Override
   public void handleChunkBatchReceived(ServerboundChunkBatchReceivedPacket p_298310_) {
      PacketUtils.ensureRunningOnSameThread(p_298310_, this, this.player.level());
      this.chunkSender.onChunkBatchReceivedByClient(p_298310_.desiredChunksPerTick());
   }

   @Override
   public void handleDebugSubscriptionRequest(ServerboundDebugSubscriptionRequestPacket p_424766_) {
      PacketUtils.ensureRunningOnSameThread(p_424766_, this, this.player.level());
      this.player.requestDebugSubscriptions(p_424766_.subscriptions());
   }

   private void resetPlayerChatState(RemoteChatSession p_253823_) {
      this.chatSession = p_253823_;
      this.signedMessageDecoder = p_253823_.createMessageDecoder(this.player.getUUID());
      this.chatMessageChain
         .append(
            () -> {
               this.player.setChatSession(p_253823_);
               this.server
                  .getPlayerList()
                  .broadcastAll(
                     new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT), List.of(this.player))
                  );
            }
         );
   }

   @Override
   public void handleCustomPayload(ServerboundCustomPayloadPacket p_329963_) {
   }

   @Override
   public void handleClientTickEnd(ServerboundClientTickEndPacket p_363521_) {
      PacketUtils.ensureRunningOnSameThread(p_363521_, this, this.player.level());
      if (!this.receivedMovementThisTick) {
         this.player.setKnownMovement(Vec3.ZERO);
      }

      this.receivedMovementThisTick = false;
   }

   private void handlePlayerKnownMovement(Vec3 p_367333_) {
      if (p_367333_.lengthSqr() > 1.0E-5F) {
         this.player.resetLastActionTime();
      }

      this.player.setKnownMovement(p_367333_);
      this.receivedMovementThisTick = true;
   }

   @Override
   public boolean hasInfiniteMaterials() {
      return this.player.hasInfiniteMaterials();
   }

   @Override
   public ServerPlayer getPlayer() {
      return this.player;
   }

   public boolean hasClientLoaded() {
      return !this.waitingForRespawn && this.clientLoadedTimeoutTimer <= 0;
   }

   public void tickClientLoadTimeout() {
      if (this.clientLoadedTimeoutTimer > 0) {
         this.clientLoadedTimeoutTimer--;
      }
   }

   private void markClientLoaded() {
      this.clientLoadedTimeoutTimer = 0;
   }

   public void markClientUnloadedAfterDeath() {
      this.waitingForRespawn = true;
   }

   private void restartClientLoadTimerAfterRespawn() {
      this.waitingForRespawn = false;
      this.clientLoadedTimeoutTimer = 60;
   }

   @FunctionalInterface
   interface EntityInteraction {
      InteractionResult run(ServerPlayer var1, Entity var2, InteractionHand var3);
   }
}
