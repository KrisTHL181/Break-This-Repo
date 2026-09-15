package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.primitives.Floats;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import net.minecraft.advancements.triggers.CriteriaTriggers;
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
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
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
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
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
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
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
import net.minecraft.server.commands.FetchProfileCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.PlayerList;
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
import net.minecraft.world.entity.Avatar;
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
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerGamePacketListenerImpl
    extends ServerCommonPacketListenerImpl
    implements ServerGamePacketListener,
    ServerPlayerConnection,
    TickablePacketListener,
    GameProtocols.Context {
    private static final Logger LOGGER = LogUtils.getLogger();
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
    private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1480);
    private final TickThrottler chatSpamThrottler;
    private final TickThrottler commandSpamThrottler;
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

    public ServerGamePacketListenerImpl(final MinecraftServer server, final Connection connection, final ServerPlayer player, final CommonListenerCookie cookie) {
        super(server, connection, cookie);
        this.commandSpamThrottler = new TickThrottler(20, 20 * server.getCommandSpamThresholdSeconds());
        this.chatSpamThrottler = new TickThrottler(20, 20 * server.getChatSpamThresholdSeconds());
        this.restartClientLoadTimerAfterRespawn();
        this.chunkSender = new PlayerChunkSender(connection.isMemoryConnection());
        this.player = player;
        player.connection = this;
        player.getTextFilter().join();
        this.signedMessageDecoder = SignedMessageChain.Decoder.unsigned(player.getUUID(), server::enforceSecureProfile);
        this.chatMessageChain = new FutureChain(server);
    }
    
    private static boolean allowIllegalValuePlayerPositionMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.allowIllegalValuePlayerPosition;
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
            this.commandSpamThrottler.tick();
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

    private int getMaximumFlyingTicks(final Entity entity) {
        double gravity = entity.getGravity();
        if (gravity < 1.0E-5F) {
            return Integer.MAX_VALUE;
        }

        double gravityModifier = 0.08 / gravity;
        return Mth.ceil(80.0 * Math.max(gravityModifier, 1.0));
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
    public boolean shouldHandleMessage(final Packet<?> packet) {
        return super.shouldHandleMessage(packet)
            ? true
            : this.waitingForSwitchToConfig && this.connection.isConnected() && packet instanceof ServerboundConfigurationAcknowledgedPacket;
    }

    @Override
    protected GameProfile playerProfile() {
        return this.player.getGameProfile();
    }

    private <T, R> CompletableFuture<R> filterTextPacket(final T message, final BiFunction<TextFilter, T, CompletableFuture<R>> action) {
        return action.apply(this.player.getTextFilter(), message).thenApply(result -> {
            if (!this.isAcceptingMessages()) {
                LOGGER.debug("Ignoring packet due to disconnection");
                throw new CancellationException("disconnected");
            } else {
                return (R)result;
            }
        });
    }

    private CompletableFuture<FilteredText> filterTextPacket(final String message) {
        return this.filterTextPacket(message, TextFilter::processStreamMessage);
    }

    private CompletableFuture<List<FilteredText>> filterTextPacket(final List<String> message) {
        return this.filterTextPacket(message, TextFilter::processMessageBundle);
    }

    @Override
    public void handlePlayerInput(final ServerboundPlayerInputPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.setLastClientInput(packet.input());
        if (this.hasClientLoaded()) {
            this.player.resetLastActionTime();
            this.player.setShiftKeyDown(packet.input().shift());
        }
    }

    private static boolean containsInvalidValues(final double x, final double y, final double z, final float yRot, final float xRot) {
        if (allowIllegalValuePlayerPositionMode()) {
            return false;
        }
        return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || !Floats.isFinite(xRot) || !Floats.isFinite(yRot);
    }

    private static double clampHorizontal(final double value) {
        return Mth.clamp(value, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    private static double clampVertical(final double value) {
        return value;
    }

    @Override
    public void handleMoveVehicle(final ServerboundMoveVehiclePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (containsInvalidValues(packet.position().x(), packet.position().y(), packet.position().z(), packet.yRot(), packet.xRot())) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_vehicle_movement"));
        } else if (!this.updateAwaitingTeleport() && this.hasClientLoaded()) {
            Entity vehicle = this.player.getRootVehicle();
            if (vehicle != this.player && vehicle.getControllingPassenger() == this.player && vehicle == this.lastVehicle) {
                ServerLevel level = this.player.level();
                double oldX = vehicle.getX();
                double oldY = vehicle.getY();
                double oldZ = vehicle.getZ();
                double targetX = clampHorizontal(packet.position().x());
                double targetY = clampVertical(packet.position().y());
                double targetZ = clampHorizontal(packet.position().z());
                float targetYRot = Mth.wrapDegrees(packet.yRot());
                float targetXRot = Mth.wrapDegrees(packet.xRot());
                double xDist = targetX - this.vehicleFirstGoodX;
                double yDist = targetY - this.vehicleFirstGoodY;
                double zDist = targetZ - this.vehicleFirstGoodZ;
                double expectedDist = vehicle.getDeltaMovement().lengthSqr();
                double movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
                if (movedDist - expectedDist > 100.0 && !this.isSingleplayerOwner()) {
                    LOGGER.warn(
                        "{} (vehicle of {}) moved too quickly! {},{},{}", vehicle.getPlainTextName(), this.player.getPlainTextName(), xDist, yDist, zDist
                    );
                    this.send(ClientboundMoveVehiclePacket.fromEntity(vehicle));
                    return;
                }

                AABB oldAABB = vehicle.getBoundingBox();
                xDist = targetX - this.vehicleLastGoodX;
                yDist = targetY - this.vehicleLastGoodY;
                zDist = targetZ - this.vehicleLastGoodZ;
                boolean vehicleRestsOnSomething = vehicle.verticalCollisionBelow;
                if (vehicle instanceof LivingEntity livingVehicle && livingVehicle.onClimbable()) {
                    livingVehicle.resetFallDistance();
                }

                vehicle.move(MoverType.PLAYER, new Vec3(xDist, yDist, zDist));
                double oyDist = yDist;
                xDist = targetX - vehicle.getX();
                yDist = targetY - vehicle.getY();
                if (yDist > -0.5 || yDist < 0.5) {
                    yDist = 0.0;
                }

                zDist = targetZ - vehicle.getZ();
                movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
                boolean fail = false;
                if (movedDist > 0.0625) {
                    fail = true;
                    LOGGER.warn("{} (vehicle of {}) moved wrongly! {}", vehicle.getPlainTextName(), this.player.getPlainTextName(), Math.sqrt(movedDist));
                }

                if (fail && level.noCollision(vehicle, oldAABB) || this.isEntityCollidingWithAnythingNew(level, vehicle, oldAABB, targetX, targetY, targetZ)) {
                    vehicle.absSnapTo(oldX, oldY, oldZ, targetYRot, targetXRot);
                    this.send(ClientboundMoveVehiclePacket.fromEntity(vehicle));
                    vehicle.removeLatestMovementRecording();
                    return;
                }

                vehicle.absSnapTo(targetX, targetY, targetZ, targetYRot, targetXRot);
                this.player.level().getChunkSource().move(this.player);
                Vec3 clientDeltaMovement = new Vec3(vehicle.getX() - oldX, vehicle.getY() - oldY, vehicle.getZ() - oldZ);
                this.handlePlayerKnownMovement(clientDeltaMovement);
                vehicle.setOnGroundWithMovement(packet.onGround(), clientDeltaMovement);
                vehicle.doCheckFallDamage(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z, packet.onGround());
                this.player.checkMovementStatistics(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z);
                this.clientVehicleIsFloating = oyDist >= -0.03125
                    && !vehicleRestsOnSomething
                    && !this.server.allowFlight()
                    && !vehicle.isFlyingVehicle()
                    && !vehicle.isNoGravity()
                    && this.noBlocksAround(vehicle);
                this.vehicleLastGoodX = vehicle.getX();
                this.vehicleLastGoodY = vehicle.getY();
                this.vehicleLastGoodZ = vehicle.getZ();
            }
        }
    }

    private boolean noBlocksAround(final Entity entity) {
        return entity.level()
            .getBlockStates(entity.getBoundingBox().inflate(0.0625).expandTowards(0.0, -0.55, 0.0))
            .allMatch(BlockBehaviour.BlockStateBase::isAir);
    }

    @Override
    public void handleAcceptTeleportPacket(final ServerboundAcceptTeleportationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getId() == this.awaitingTeleport) {
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
    public void handleAcceptPlayerLoad(final ServerboundPlayerLoadedPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.markClientLoaded();
    }

    @Override
    public void handleRecipeBookSeenRecipePacket(final ServerboundRecipeBookSeenRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        RecipeManager.ServerDisplayInfo entry = this.server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (entry != null) {
            this.player.getRecipeBook().removeHighlight(entry.parent().id());
        }
    }

    @Override
    public void handleBundleItemSelectedPacket(final ServerboundSelectBundleItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.containerMenu.setSelectedBundleItemIndex(packet.slotId(), packet.selectedItemIndex());
    }

    @Override
    public void handleRecipeBookChangeSettingsPacket(final ServerboundRecipeBookChangeSettingsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.getRecipeBook().setBookSetting(packet.getBookType(), packet.isOpen(), packet.isFiltering());
    }

    @Override
    public void handleSeenAdvancements(final ServerboundSeenAdvancementsPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getAction() == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
            Identifier id = Objects.requireNonNull(packet.getTab());
            AdvancementHolder advancement = this.server.getAdvancements().get(id);
            if (advancement != null) {
                this.player.getAdvancements().setSelectedTab(advancement);
            }
        }
    }

    @Override
    public void handleCustomCommandSuggestions(final ServerboundCommandSuggestionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        StringReader command = new StringReader(packet.getCommand());
        if (command.canRead() && command.peek() == '/') {
            command.skip();
        }

        ParseResults<CommandSourceStack> parse = this.server.getCommands().getDispatcher().parse(command, this.player.createCommandSourceStack());
        this.server.getCommands().getDispatcher().getCompletionSuggestions(parse).thenAccept(results -> {
            Suggestions suggestions = results.getList().size() <= 1000 ? results : new Suggestions(results.getRange(), results.getList().subList(0, 1000));
            this.send(new ClientboundCommandSuggestionsPacket(packet.getId(), suggestions));
        });
    }

    @Override
    public void handleSetCommandBlock(final ServerboundSetCommandBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
        } else {
            BaseCommandBlock commandBlock = null;
            CommandBlockEntity autoCommandBlock = null;
            BlockPos blockPos = packet.getPos();
            BlockEntity blockEntity = this.player.level().getBlockEntity(blockPos);
            if (blockEntity instanceof CommandBlockEntity commandBlockEntity) {
                autoCommandBlock = commandBlockEntity;
                commandBlock = autoCommandBlock.getCommandBlock();
            }

            String command = packet.getCommand();
            boolean trackOutput = packet.isTrackOutput();
            if (commandBlock != null) {
                CommandBlockEntity.Mode oldMode = autoCommandBlock.getMode();
                BlockState currentBlockState = this.player.level().getBlockState(blockPos);
                Direction direction = currentBlockState.getValue(CommandBlock.FACING);

                BlockState baseBlockState = switch (packet.getMode()) {
                    case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
                    case AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
                    default -> Blocks.COMMAND_BLOCK.defaultBlockState();
                };
                BlockState blockState = baseBlockState.setValue(CommandBlock.FACING, direction).setValue(CommandBlock.CONDITIONAL, packet.isConditional());
                if (blockState != currentBlockState) {
                    this.player.level().setBlock(blockPos, blockState, 2);
                    blockEntity.setBlockState(blockState);
                    this.player.level().getChunkAt(blockPos).setBlockEntity(blockEntity);
                }

                commandBlock.setCommand(command);
                commandBlock.setTrackOutput(trackOutput);
                if (!trackOutput) {
                    commandBlock.setLastOutput(null);
                }

                autoCommandBlock.setAutomatic(packet.isAutomatic());
                if (oldMode != packet.getMode()) {
                    autoCommandBlock.onModeSwitch();
                }

                if (this.player.level().isCommandBlockEnabled()) {
                    commandBlock.onUpdated(this.player.level());
                }

                if (!StringUtil.isNullOrEmpty(command)) {
                    this.player
                        .sendSystemMessage(
                            Component.translatable(
                                this.player.level().isCommandBlockEnabled() ? "advMode.setCommand.success" : "advMode.setCommand.disabled", command
                            )
                        );
                }
            }
        }
    }

    @Override
    public void handleSetCommandMinecart(final ServerboundSetCommandMinecartPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.canUseGameMasterBlocks()) {
            this.player.sendSystemMessage(Component.translatable("advMode.notAllowed"));
        } else {
            BaseCommandBlock commandBlock = packet.getCommandBlock(this.player.level());
            if (commandBlock != null) {
                String command = packet.getCommand();
                commandBlock.setCommand(command);
                commandBlock.setTrackOutput(packet.isTrackOutput());
                if (!packet.isTrackOutput()) {
                    commandBlock.setLastOutput(null);
                }

                boolean commandBlockEnabled = this.player.level().isCommandBlockEnabled();
                if (commandBlockEnabled) {
                    commandBlock.onUpdated(this.player.level());
                }

                if (!StringUtil.isNullOrEmpty(command)) {
                    this.player
                        .sendSystemMessage(Component.translatable(commandBlockEnabled ? "advMode.setCommand.success" : "advMode.setCommand.disabled", command));
                }
            }
        }
    }

    @Override
    public void handlePickItemFromBlock(final ServerboundPickItemFromBlockPacket packet) {
        ServerLevel level = this.player.level();
        PacketUtils.ensureRunningOnSameThread(packet, this, level);
        BlockPos pos = packet.pos();
        if (this.player.isWithinBlockInteractionRange(pos, 1.0)) {
            if (level.isLoaded(pos)) {
                BlockState blockState = level.getBlockState(pos);
                boolean includeData = this.player.hasInfiniteMaterials() && packet.includeData();
                ItemStack itemStack = blockState.getCloneItemStack(level, pos, includeData);
                if (!itemStack.isEmpty()) {
                    if (includeData) {
                        addBlockDataToItem(blockState, level, pos, itemStack);
                    }

                    this.tryPickItem(itemStack);
                }
            }
        }
    }

    private static void addBlockDataToItem(final BlockState blockState, final ServerLevel level, final BlockPos pos, final ItemStack itemStack) {
        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (blockEntity != null) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, level.registryAccess());
                blockEntity.saveCustomOnly(output);
                blockEntity.removeComponentsFromTag(output);
                BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), output);
                itemStack.applyComponents(blockEntity.collectComponents());
            }
        }
    }

    @Override
    public void handlePickItemFromEntity(final ServerboundPickItemFromEntityPacket packet) {
        ServerLevel level = this.player.level();
        PacketUtils.ensureRunningOnSameThread(packet, this, level);
        Entity entity = level.getEntityOrPart(packet.id());
        if (entity != null && this.player.isWithinEntityInteractionRange(entity, 3.0)) {
            ItemStack itemStack = entity.getPickResult();
            if (itemStack != null && !itemStack.isEmpty()) {
                this.tryPickItem(itemStack);
            }

            if (packet.includeData() && this.player.canUseGameMasterBlocks() && entity instanceof Avatar avatar) {
                FetchProfileCommand.printForAvatar(this.player.createCommandSourceStack(), avatar);
            }
        }
    }

    private void tryPickItem(final ItemStack itemStack) {
        if (itemStack.isItemEnabled(this.player.level().enabledFeatures())) {
            Inventory inventory = this.player.getInventory();
            int slotWithExistingItem = inventory.findSlotMatchingItem(itemStack);
            if (slotWithExistingItem != -1) {
                if (Inventory.isHotbarSlot(slotWithExistingItem)) {
                    inventory.setSelectedSlot(slotWithExistingItem);
                } else {
                    inventory.pickSlot(slotWithExistingItem);
                }
            } else if (this.player.hasInfiniteMaterials()) {
                inventory.addAndPickItem(itemStack);
            }

            this.send(new ClientboundSetHeldSlotPacket(inventory.getSelectedSlot()));
            this.player.inventoryMenu.broadcastChanges();
        }
    }

    @Override
    public void handleRenameItem(final ServerboundRenameItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.containerMenu instanceof AnvilMenu menu) {
            if (!menu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, menu);
                return;
            }

            menu.setItemName(packet.getName());
        }
    }

    @Override
    public void handleSetBeaconPacket(final ServerboundSetBeaconPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.containerMenu instanceof BeaconMenu menu) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
                return;
            }

            if (!menu.updateEffects(packet.primary(), packet.secondary())) {
                this.disconnect(Component.translatable("multiplayer.disconnect.generic"));
                LOGGER.warn("Player {} tried to set invalid beacon effects", this.player);
            }
        }
    }

    @Override
    public void handleSetGameRule(final ServerboundSetGameRulePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            LOGGER.warn("Player {} tried to set game rule values without required permissions", this.player.getGameProfile().name());
        } else {
            GameRules gameRules = this.player.level().getGameRules();

            for (ServerboundSetGameRulePacket.Entry entry : packet.entries()) {
                GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue(entry.gameRuleKey());
                if (rule != null) {
                    this.setGameRuleValue(gameRules, rule, entry.value());
                } else {
                    LOGGER.warn("Received request to set unknown game rule: {}", entry.gameRuleKey());
                }
            }
        }
    }

    private <T> void setGameRuleValue(final GameRules gameRules, final GameRule<T> rule, final String value) {
        rule.deserialize(value).result().ifPresent(parsedValue -> {
            gameRules.set(rule, (T)parsedValue, this.server);
            this.broadcastGameRuleChangeToOperators(rule, (T)parsedValue);
        });
    }

    private <T> void broadcastGameRuleChangeToOperators(final GameRule<T> rule, final T value) {
        Component message = Component.translatable("commands.gamerule.set", rule.id(), rule.serialize(value));
        PlayerList playerList = this.server.getPlayerList();
        playerList.getPlayers().stream().filter(op -> playerList.isOp(op.nameAndId())).forEach(op -> op.sendSystemMessage(message));
    }

    @Override
    public void handleSetStructureBlock(final ServerboundSetStructureBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            BlockState state = this.player.level().getBlockState(blockPos);
            if (this.player.level().getBlockEntity(blockPos) instanceof StructureBlockEntity structure) {
                structure.setMode(packet.getMode());
                structure.setStructureName(packet.getName());
                structure.setStructurePos(packet.getOffset());
                structure.setStructureSize(packet.getSize());
                structure.setMirror(packet.getMirror());
                structure.setRotation(packet.getRotation());
                structure.setMetaData(packet.getData());
                structure.setIgnoreEntities(packet.isIgnoreEntities());
                structure.setStrict(packet.isStrict());
                structure.setShowAir(packet.isShowAir());
                structure.setShowBoundingBox(packet.isShowBoundingBox());
                structure.setIntegrity(packet.getIntegrity());
                structure.setSeed(packet.getSeed());
                if (structure.hasStructureName()) {
                    String actualStructureName = structure.getStructureName();
                    if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SAVE_AREA) {
                        if (structure.saveStructure()) {
                            this.player.sendSystemMessage(Component.translatable("structure_block.save_success", actualStructureName));
                        } else {
                            this.player.sendSystemMessage(Component.translatable("structure_block.save_failure", actualStructureName));
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.LOAD_AREA) {
                        if (!structure.isStructureLoadable()) {
                            this.player.sendSystemMessage(Component.translatable("structure_block.load_not_found", actualStructureName));
                        } else if (structure.placeStructureIfSameSize(this.player.level())) {
                            this.player.sendSystemMessage(Component.translatable("structure_block.load_success", actualStructureName));
                        } else {
                            this.player.sendSystemMessage(Component.translatable("structure_block.load_prepare", actualStructureName));
                        }
                    } else if (packet.getUpdateType() == StructureBlockEntity.UpdateType.SCAN_AREA) {
                        if (structure.detectSize()) {
                            this.player.sendSystemMessage(Component.translatable("structure_block.size_success", actualStructureName));
                        } else {
                            this.player.sendSystemMessage(Component.translatable("structure_block.size_failure"));
                        }
                    }
                } else {
                    this.player.sendSystemMessage(Component.translatable("structure_block.invalid_structure_name", packet.getName()));
                }

                structure.setChanged();
                this.player.level().sendBlockUpdated(blockPos, state, state, 3);
            }
        }
    }

    @Override
    public void handleSetTestBlock(final ServerboundSetTestBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.position();
            BlockState initialState = this.player.level().getBlockState(blockPos);
            if (this.player.level().getBlockEntity(blockPos) instanceof TestBlockEntity testBlock) {
                testBlock.setMode(packet.mode());
                testBlock.setMessage(packet.message());
                testBlock.setChanged();
                this.player.level().sendBlockUpdated(blockPos, initialState, testBlock.getBlockState(), 3);
            }
        }
    }

    @Override
    public void handleTestInstanceBlockAction(final ServerboundTestInstanceBlockActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        BlockPos pos = packet.pos();
        if (this.player.canUseGameMasterBlocks() && this.player.level().getBlockEntity(pos) instanceof TestInstanceBlockEntity blockEntity) {
            if (packet.action() != ServerboundTestInstanceBlockActionPacket.Action.QUERY
                && packet.action() != ServerboundTestInstanceBlockActionPacket.Action.INIT) {
                blockEntity.set(packet.data());
                if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.RESET) {
                    blockEntity.resetTest(this.player::sendSystemMessage);
                } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.SAVE) {
                    blockEntity.saveTest(this.player::sendSystemMessage);
                } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.EXPORT) {
                    blockEntity.exportTest(this.player::sendSystemMessage);
                } else if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.RUN) {
                    blockEntity.runTest(this.player::sendSystemMessage);
                }

                BlockState state = this.player.level().getBlockState(pos);
                this.player.level().sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), state, 3);
            } else {
                Registry<GameTestInstance> registry = this.player.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
                Optional<Holder.Reference<GameTestInstance>> test = packet.data().test().flatMap(registry::get);
                Component status;
                if (test.isPresent()) {
                    status = test.get().value().describe();
                } else {
                    status = Component.translatable("test_instance.description.no_test").withStyle(ChatFormatting.RED);
                }

                Optional<Vec3i> size;
                if (packet.action() == ServerboundTestInstanceBlockActionPacket.Action.QUERY) {
                    size = packet.data()
                        .test()
                        .flatMap(testKey -> TestInstanceBlockEntity.getStructureSize(this.player.level(), (ResourceKey<GameTestInstance>)testKey));
                } else {
                    size = Optional.empty();
                }

                this.connection.send(new ClientboundTestInstanceBlockStatus(status, size));
            }
        }
    }

    @Override
    public void handleSetJigsawBlock(final ServerboundSetJigsawBlockPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            BlockState state = this.player.level().getBlockState(blockPos);
            if (this.player.level().getBlockEntity(blockPos) instanceof JigsawBlockEntity jigsaw) {
                jigsaw.setName(packet.getName());
                jigsaw.setTarget(packet.getTarget());
                jigsaw.setPool(ResourceKey.create(Registries.TEMPLATE_POOL, packet.getPool()));
                jigsaw.setFinalState(packet.getFinalState());
                jigsaw.setJoint(packet.getJoint());
                jigsaw.setPlacementPriority(packet.getPlacementPriority());
                jigsaw.setSelectionPriority(packet.getSelectionPriority());
                jigsaw.setChanged();
                this.player.level().sendBlockUpdated(blockPos, state, state, 3);
            }
        }
    }

    @Override
    public void handleJigsawGenerate(final ServerboundJigsawGeneratePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.canUseGameMasterBlocks()) {
            BlockPos blockPos = packet.getPos();
            if (this.player.level().getBlockEntity(blockPos) instanceof JigsawBlockEntity jigsaw) {
                jigsaw.generate(this.player.level(), packet.levels(), packet.keepJigsaws());
            }
        }
    }

    @Override
    public void handleSelectTrade(final ServerboundSelectTradePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        int selection = packet.getItem();
        if (this.player.containerMenu instanceof MerchantMenu menu) {
            if (!menu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, menu);
                return;
            }

            menu.setSelectionHint(selection);
            menu.tryMoveItems(selection);
        }
    }

    @Override
    public void handleEditBook(final ServerboundEditBookPacket packet) {
        int slot = packet.slot();
        if (Inventory.isHotbarSlot(slot) || slot == 40) {
            List<String> contents = Lists.newArrayList();
            Optional<String> title = packet.title();
            title.ifPresent(contents::add);
            contents.addAll(packet.pages());
            Consumer<List<FilteredText>> handler = title.isPresent()
                ? filteredContents -> this.signBook(filteredContents.get(0), filteredContents.subList(1, filteredContents.size()), slot)
                : filteredContents -> this.updateBookContents(filteredContents, slot);
            this.filterTextPacket(contents).thenAcceptAsync(handler, this.server);
        }
    }

    private void updateBookContents(final List<FilteredText> contents, final int slot) {
        ItemStack carried = this.player.getInventory().getItem(slot);
        if (carried.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            List<Filterable<String>> pages = contents.stream().map(this::filterableFromOutgoing).toList();
            carried.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
        }
    }

    private void signBook(final FilteredText title, final List<FilteredText> contents, final int slot) {
        ItemStack carried = this.player.getInventory().getItem(slot);
        if (carried.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            ItemStack writtenBook = carried.transmuteCopy(Items.WRITTEN_BOOK);
            writtenBook.remove(DataComponents.WRITABLE_BOOK_CONTENT);
            List<Filterable<Component>> pages = contents.stream().map(page -> this.filterableFromOutgoing(page).<Component>map(Component::literal)).toList();
            writtenBook.set(
                DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(this.filterableFromOutgoing(title), this.player.getPlainTextName(), 0, pages, true)
            );
            this.player.getInventory().setItem(slot, writtenBook);
        }
    }

    private Filterable<String> filterableFromOutgoing(final FilteredText text) {
        return this.player.isTextFilteringEnabled() ? Filterable.passThrough(text.filteredOrEmpty()) : Filterable.from(text);
    }

    @Override
    public void handleEntityTagQuery(final ServerboundEntityTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            Entity entity = this.player.level().getEntity(packet.getEntityId());
            if (entity != null) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
                    TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
                    entity.saveWithoutId(output);
                    CompoundTag result = output.buildResult();
                    this.send(new ClientboundTagQueryPacket(packet.getTransactionId(), result));
                }
            }
        }
    }

    @Override
    public void handleContainerSlotStateChanged(final ServerboundContainerSlotStateChangedPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.isSpectator() && packet.containerId() == this.player.containerMenu.containerId) {
            if (this.player.containerMenu instanceof CrafterMenu crafterMenu && crafterMenu.getContainer() instanceof CrafterBlockEntity crafterBlockEntity) {
                crafterBlockEntity.setSlotState(packet.slotId(), packet.newState());
            }
        }
    }

    @Override
    public void handleBlockEntityTagQuery(final ServerboundBlockEntityTagQueryPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            BlockEntity blockEntity = this.player.level().getBlockEntity(packet.getPos());
            CompoundTag tag = blockEntity != null ? blockEntity.saveWithoutMetadata(this.player.registryAccess()) : null;
            this.send(new ClientboundTagQueryPacket(packet.getTransactionId(), tag));
        }
    }

    @Override
    public void handleMovePlayer(final ServerboundMovePlayerPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (containsInvalidValues(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0), packet.getYRot(0.0F), packet.getXRot(0.0F))) {
            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_player_movement"));
        } else {
            ServerLevel level = this.player.level();
            if (!this.player.wonGame) {
                if (this.tickCount == 0) {
                    this.resetPosition();
                }

                if (this.hasClientLoaded()) {
                    float targetYRot = Mth.wrapDegrees(packet.getYRot(this.player.getYRot()));
                    float targetXRot = Mth.wrapDegrees(packet.getXRot(this.player.getXRot()));
                    if (this.updateAwaitingTeleport()) {
                        this.player.absSnapRotationTo(targetYRot, targetXRot);
                    } else {
                        double targetX = clampHorizontal(packet.getX(this.player.getX()));
                        double targetY = clampVertical(packet.getY(this.player.getY()));
                        double targetZ = clampHorizontal(packet.getZ(this.player.getZ()));
                        if (this.player.isPassenger()) {
                            this.player.absSnapTo(this.player.getX(), this.player.getY(), this.player.getZ(), targetYRot, targetXRot);
                            this.player.level().getChunkSource().move(this.player);
                        } else {
                            double startX = this.player.getX();
                            double startY = this.player.getY();
                            double startZ = this.player.getZ();
                            double xDist = targetX - this.firstGoodX;
                            double yDist = targetY - this.firstGoodY;
                            double zDist = targetZ - this.firstGoodZ;
                            double expectedDist = this.player.getDeltaMovement().lengthSqr();
                            double movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
                            if (this.player.isSleeping()) {
                                if (movedDist > 1.0) {
                                    this.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), targetYRot, targetXRot);
                                }
                            } else {
                                boolean isFallFlying = this.player.isFallFlying();
                                if (level.tickRateManager().runsNormally()) {
                                    this.receivedMovePacketCount++;
                                    int deltaPackets = this.receivedMovePacketCount - this.knownMovePacketCount;
                                    if (deltaPackets > 5) {
                                        LOGGER.debug(
                                            "{} is sending move packets too frequently ({} packets since last tick)",
                                            this.player.getPlainTextName(),
                                            deltaPackets
                                        );
                                        deltaPackets = 1;
                                    }

                                    if (this.shouldCheckPlayerMovement(isFallFlying)) {
                                        float metersPerTick = isFallFlying ? 300.0F : 100.0F;
                                        if (movedDist - expectedDist > metersPerTick * deltaPackets) {
                                            LOGGER.warn("{} moved too quickly! {},{},{}", this.player.getPlainTextName(), xDist, yDist, zDist);
                                            this.teleport(
                                                this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot()
                                            );
                                            return;
                                        }
                                    }
                                }

                                AABB oldAABB = this.player.getBoundingBox();
                                xDist = targetX - this.lastGoodX;
                                yDist = targetY - this.lastGoodY;
                                zDist = targetZ - this.lastGoodZ;
                                boolean movedUpwards = yDist > 0.0;
                                if (this.player.onGround() && !packet.isOnGround() && movedUpwards) {
                                    this.player.jumpFromGround();
                                }

                                boolean playerStandsOnSomething = this.player.verticalCollisionBelow;
                                this.player.move(MoverType.PLAYER, new Vec3(xDist, yDist, zDist));
                                double oyDist = yDist;
                                xDist = targetX - this.player.getX();
                                yDist = targetY - this.player.getY();
                                if (yDist > -0.5 || yDist < 0.5) {
                                    yDist = 0.0;
                                }

                                zDist = targetZ - this.player.getZ();
                                movedDist = xDist * xDist + yDist * yDist + zDist * zDist;
                                boolean fail = false;
                                if (!this.player.isChangingDimension()
                                    && movedDist > 0.0625
                                    && !this.player.isSleeping()
                                    && !this.player.isCreative()
                                    && !this.player.isSpectator()
                                    && !this.player.isInPostImpulseGraceTime()) {
                                    fail = true;
                                    LOGGER.warn("{} moved wrongly!", this.player.getPlainTextName());
                                }

                                if (this.player.noPhysics
                                    || this.player.isSleeping()
                                    || (!fail || !level.noCollision(this.player, oldAABB))
                                        && !this.isEntityCollidingWithAnythingNew(level, this.player, oldAABB, targetX, targetY, targetZ)) {
                                    this.player.absSnapTo(targetX, targetY, targetZ, targetYRot, targetXRot);
                                    boolean isAutoSpinAttack = this.player.isAutoSpinAttack();
                                    this.clientIsFloating = oyDist >= -0.03125
                                        && !playerStandsOnSomething
                                        && !this.player.isSpectator()
                                        && !this.server.allowFlight()
                                        && !this.player.getAbilities().mayfly
                                        && !this.player.hasEffect(MobEffects.LEVITATION)
                                        && !isFallFlying
                                        && !isAutoSpinAttack
                                        && this.noBlocksAround(this.player);
                                    this.player.level().getChunkSource().move(this.player);
                                    Vec3 clientDeltaMovement = new Vec3(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ);
                                    this.player.setOnGroundWithMovement(packet.isOnGround(), packet.horizontalCollision(), clientDeltaMovement);
                                    this.player.doCheckFallDamage(clientDeltaMovement.x, clientDeltaMovement.y, clientDeltaMovement.z, packet.isOnGround());
                                    this.handlePlayerKnownMovement(clientDeltaMovement);
                                    if (movedUpwards) {
                                        this.player.resetFallDistance();
                                    }

                                    if (packet.isOnGround()
                                        || this.player.hasLandedInLiquid()
                                        || this.player.onClimbable()
                                        || this.player.isSpectator()
                                        || isFallFlying
                                        || isAutoSpinAttack) {
                                        this.player.tryResetCurrentImpulseContext();
                                    }

                                    this.player.checkMovementStatistics(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ);
                                    this.lastGoodX = this.player.getX();
                                    this.lastGoodY = this.player.getY();
                                    this.lastGoodZ = this.player.getZ();
                                } else {
                                    this.teleport(startX, startY, startZ, targetYRot, targetXRot);
                                    this.player
                                        .doCheckFallDamage(
                                            this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ, packet.isOnGround()
                                        );
                                    this.player.removeLatestMovementRecording();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldCheckPlayerMovement(final boolean isFallFlying) {
        if (this.isSingleplayerOwner()) {
            return false;
        }

        if (this.player.isChangingDimension()) {
            return false;
        }

        GameRules gameRules = this.player.level().getGameRules();
        return !gameRules.get(GameRules.PLAYER_MOVEMENT_CHECK) ? false : !isFallFlying || gameRules.get(GameRules.ELYTRA_MOVEMENT_CHECK);
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
        final LevelReader level, final Entity entity, final AABB oldAABB, final double newX, final double newY, final double newZ
    ) {
        AABB newAABB = entity.getBoundingBox().move(newX - entity.getX(), newY - entity.getY(), newZ - entity.getZ());
        Iterable<VoxelShape> newCollisions = level.getPreMoveCollisions(entity, newAABB.deflate(1.0E-5F), oldAABB.getBottomCenter());
        VoxelShape oldShape = Shapes.create(oldAABB.deflate(1.0E-5F));

        for (VoxelShape shape : newCollisions) {
            if (!Shapes.joinIsNotEmpty(shape, oldShape, BooleanOp.AND)) {
                return true;
            }
        }

        return false;
    }

    public void teleport(final double x, final double y, final double z, final float yRot, final float xRot) {
        this.teleport(new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yRot, xRot), Collections.emptySet());
    }

    public void teleport(final PositionMoveRotation destination, final Set<Relative> relatives) {
        this.awaitingTeleportTime = this.tickCount;
        if (++this.awaitingTeleport == Integer.MAX_VALUE) {
            this.awaitingTeleport = 0;
        }

        this.player.teleportSetPosition(destination, relatives);
        this.awaitingPositionFromClient = this.player.position();
        this.send(ClientboundPlayerPositionPacket.of(this.awaitingTeleport, destination, relatives));
    }

    @Override
    public void handlePlayerAction(final ServerboundPlayerActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded()) {
            BlockPos pos = packet.getPos();
            this.player.resetLastActionTime();
            ServerboundPlayerActionPacket.Action action = packet.getAction();
            switch (action) {
                case STAB:
                    if (this.player.isSpectator()) {
                        return;
                    } else {
                        ItemStack itemInHand = this.player.getItemInHand(InteractionHand.MAIN_HAND);
                        if (this.player.cannotAttackWithItem(itemInHand, 5)) {
                            return;
                        }

                        PiercingWeapon piercingWeapon = itemInHand.get(DataComponents.PIERCING_WEAPON);
                        if (piercingWeapon != null) {
                            piercingWeapon.attack(this.player, EquipmentSlot.MAINHAND);
                        }

                        return;
                    }
                case SWAP_ITEM_WITH_OFFHAND:
                    if (!this.player.isSpectator()) {
                        ItemStack swap = this.player.getItemInHand(InteractionHand.OFF_HAND);
                        this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
                        this.player.setItemInHand(InteractionHand.MAIN_HAND, swap);
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
                    this.player.gameMode.handleBlockBreakAction(pos, action, packet.getDirection(), this.player.level().getMaxY(), packet.getSequence());
                    this.ackBlockChangesUpTo(packet.getSequence());
                    return;
                default:
                    throw new IllegalArgumentException("Invalid player action");
            }
        }
    }

    private static boolean wasBlockPlacementAttempt(final ServerPlayer player, final ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();
        return (item instanceof BlockItem || item instanceof BucketItem bucket && bucket.getContent() != Fluids.EMPTY)
            && !player.getCooldowns().isOnCooldown(itemStack);
    }

    @Override
    public void handleUseItemOn(final ServerboundUseItemOnPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded()) {
            this.ackBlockChangesUpTo(packet.getSequence());
            ServerLevel level = this.player.level();
            InteractionHand hand = packet.getHand();
            ItemStack itemStack = this.player.getItemInHand(hand);
            if (itemStack.isItemEnabled(level.enabledFeatures())) {
                BlockHitResult blockHit = packet.getHitResult();
                Vec3 location = blockHit.getLocation();
                BlockPos pos = blockHit.getBlockPos();
                if (this.player.isWithinBlockInteractionRange(pos, 1.0)) {
                    Vec3 distance = location.subtract(Vec3.atCenterOf(pos));
                    double limit = 1.0000001;
                    if (Math.abs(distance.x()) < 1.0000001 && Math.abs(distance.y()) < 1.0000001 && Math.abs(distance.z()) < 1.0000001) {
                        Direction direction = blockHit.getDirection();
                        this.player.resetLastActionTime();
                        int maxY = level.getMaxY();
                        int minY = level.getMinY();
                        if (pos.getY() > maxY) {
                            this.player.sendBuildLimitMessage(true, maxY);
                        } else if (pos.getY() < minY) {
                            this.player.sendBuildLimitMessage(false, minY);
                        } else {
                            if (this.server.isUnderSpawnProtection(level, pos, this.player)) {
                                this.player.sendSpawnProtectionMessage(pos);
                            } else if (this.awaitingPositionFromClient == null && level.mayInteract(this.player, pos)) {
                                InteractionResult interactionResult = this.player.gameMode.useItemOn(this.player, level, itemStack, hand, blockHit);
                                if (interactionResult.consumesAction()) {
                                    CriteriaTriggers.ANY_BLOCK_USE.trigger(this.player, blockHit.getBlockPos(), itemStack);
                                }

                                if (direction == Direction.UP
                                    && !interactionResult.consumesAction()
                                    && pos.getY() >= maxY
                                    && wasBlockPlacementAttempt(this.player, itemStack)) {
                                    this.player.sendBuildLimitMessage(true, maxY);
                                } else if (interactionResult instanceof InteractionResult.Success success
                                    && success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                    this.player.swing(hand, true);
                                }

                                if (!interactionResult.consumesAction() && wasBlockPlacementAttempt(this.player, itemStack)) {
                                    if (direction == Direction.UP && pos.getY() >= maxY) {
                                        this.player.sendBuildLimitMessage(true, maxY);
                                    } else if (direction == Direction.DOWN && pos.getY() <= minY) {
                                        this.player.sendBuildLimitMessage(false, minY);
                                    }
                                } else if (interactionResult instanceof InteractionResult.Success success
                                    && success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                    this.player.swing(hand, true);
                                }
                            } else {
                                this.player.sendBuildLimitMessage(true, maxY);
                            }

                            this.send(new ClientboundBlockUpdatePacket(level, pos));
                            this.send(new ClientboundBlockUpdatePacket(level, pos.relative(direction)));
                        }
                    } else {
                        LOGGER.warn(
                            "Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.",
                            this.player.getGameProfile().name(),
                            location,
                            pos
                        );
                    }
                }
            }
        }
    }

    @Override
    public void handleUseItem(final ServerboundUseItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded()) {
            this.ackBlockChangesUpTo(packet.getSequence());
            ServerLevel level = this.player.level();
            InteractionHand hand = packet.getHand();
            ItemStack itemStack = this.player.getItemInHand(hand);
            this.player.resetLastActionTime();
            if (!itemStack.isEmpty() && itemStack.isItemEnabled(level.enabledFeatures())) {
                float targetYRot = Mth.wrapDegrees(packet.getYRot());
                float targetXRot = Mth.wrapDegrees(packet.getXRot());
                if (targetXRot != this.player.getXRot() || targetYRot != this.player.getYRot()) {
                    this.player.absSnapRotationTo(targetYRot, targetXRot);
                }

                if (this.player.gameMode.useItem(this.player, level, itemStack, hand) instanceof InteractionResult.Success success
                    && success.swingSource() == InteractionResult.SwingSource.SERVER) {
                    this.player.swing(hand, true);
                }
            }
        }
    }

    @Override
    public void handleTeleportToEntityPacket(final ServerboundTeleportToEntityPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.isSpectator()) {
            for (ServerLevel level : this.server.getAllLevels()) {
                Entity entity = packet.getEntity(level);
                if (entity != null) {
                    this.player.teleportTo(level, entity.getX(), entity.getY(), entity.getZ(), Set.of(), entity.getYRot(), entity.getXRot(), true);
                    return;
                }
            }
        }
    }

    @Override
    public void handlePaddleBoat(final ServerboundPaddleBoatPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.getControlledVehicle() instanceof AbstractBoat boat) {
            boat.setPaddleState(packet.getLeft(), packet.getRight());
        }
    }

    @Override
    public void onDisconnect(final DisconnectionDetails details) {
        LOGGER.info("{} lost connection: {}", this.player.getPlainTextName(), details.reason().getString());
        this.removePlayerFromWorld();
        super.onDisconnect(details);
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

    public void ackBlockChangesUpTo(final int packetSequenceNr) {
        if (packetSequenceNr < 0) {
            throw new IllegalArgumentException("Expected packet sequence nr >= 0");
        }

        this.ackBlockChangesUpTo = Math.max(packetSequenceNr, this.ackBlockChangesUpTo);
    }

    @Override
    public void handleSetCarriedItem(final ServerboundSetCarriedItemPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (packet.getSlot() >= 0 && packet.getSlot() < Inventory.getSelectionSize()) {
            if (this.player.getInventory().getSelectedSlot() != packet.getSlot() && this.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                this.player.stopUsingItem();
            }

            this.player.getInventory().setSelectedSlot(packet.getSlot());
            this.player.resetLastActionTime();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", this.player.getPlainTextName());
        }
    }

    @Override
    public void handleChat(final ServerboundChatPacket packet) {
        Optional<LastSeenMessages> unpackedLastSeen = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
        if (!unpackedLastSeen.isEmpty()) {
            this.tryHandleChat(packet.message(), false, () -> {
                PlayerChatMessage signedMessage;
                try {
                    signedMessage = this.getSignedMessage(packet, unpackedLastSeen.get());
                } catch (SignedMessageChain.DecodeException e) {
                    this.handleMessageDecodeFailure(e);
                    return;
                }

                CompletableFuture<FilteredText> filteredFuture = this.filterTextPacket(signedMessage.signedContent());
                Component decorated = this.server.getChatDecorator().decorate(this.player, signedMessage.decoratedContent());
                this.chatMessageChain.append(filteredFuture, filtered -> {
                    PlayerChatMessage filteredMessage = signedMessage.withUnsignedContent(decorated).filter(filtered.mask());
                    this.broadcastChatMessage(filteredMessage);
                });
            });
        }
    }

    @Override
    public void handleChatCommand(final ServerboundChatCommandPacket packet) {
        this.tryHandleChat(packet.command(), true, () -> {
            this.performUnsignedChatCommand(packet.command());
            this.detectCommandRateSpam();
        });
    }

    private void performUnsignedChatCommand(final String command) {
        ParseResults<CommandSourceStack> parsed = this.parseCommand(command);
        if (this.server.enforceSecureProfile() && SignableCommand.hasSignableArguments(parsed)) {
            LOGGER.error(
                "Received unsigned command packet from {}, but the command requires signable arguments: {}", this.player.getGameProfile().name(), command
            );
            this.player.sendSystemMessage(INVALID_COMMAND_SIGNATURE);
        } else {
            this.server.getCommands().performCommand(parsed, command);
        }
    }

    @Override
    public void handleSignedChatCommand(final ServerboundChatCommandSignedPacket packet) {
        Optional<LastSeenMessages> unpackedLastSeen = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
        if (!unpackedLastSeen.isEmpty()) {
            this.tryHandleChat(packet.command(), true, () -> {
                this.performSignedChatCommand(packet, unpackedLastSeen.get());
                this.detectCommandRateSpam();
            });
        }
    }

    private void performSignedChatCommand(final ServerboundChatCommandSignedPacket packet, final LastSeenMessages lastSeenMessages) {
        ParseResults<CommandSourceStack> command = this.parseCommand(packet.command());

        Map<String, PlayerChatMessage> signedArguments;
        try {
            signedArguments = this.collectSignedArguments(packet, SignableCommand.of(command), lastSeenMessages);
        } catch (SignedMessageChain.DecodeException e) {
            this.handleMessageDecodeFailure(e);
            return;
        }

        CommandSigningContext signingContext = new CommandSigningContext.SignedArguments(signedArguments);
        command = Commands.mapSource(command, source -> source.withSigningContext(signingContext, this.chatMessageChain));
        this.server.getCommands().performCommand(command, packet.command());
    }

    private void handleMessageDecodeFailure(final SignedMessageChain.DecodeException e) {
        LOGGER.warn("Failed to update secure chat state for {}: '{}'", this.player.getGameProfile().name(), e.getComponent().getString());
        this.player.sendSystemMessage(e.getComponent().copy().withStyle(ChatFormatting.RED));
    }

    private <S> Map<String, PlayerChatMessage> collectSignedArguments(
        final ServerboundChatCommandSignedPacket packet, final SignableCommand<S> command, final LastSeenMessages lastSeenMessages
    ) throws SignedMessageChain.DecodeException {
        List<ArgumentSignatures.Entry> argumentSignatures = packet.argumentSignatures().entries();
        List<SignableCommand.Argument<S>> parsedArguments = command.arguments();
        if (argumentSignatures.isEmpty()) {
            return this.collectUnsignedArguments(parsedArguments);
        }

        Map<String, PlayerChatMessage> signedArguments = new Object2ObjectOpenHashMap<>();

        for (ArgumentSignatures.Entry clientArgument : argumentSignatures) {
            SignableCommand.Argument<S> expectedArgument = command.getArgument(clientArgument.name());
            if (expectedArgument == null) {
                this.signedMessageDecoder.setChainBroken();
                throw createSignedArgumentMismatchException(packet.command(), argumentSignatures, parsedArguments);
            }

            SignedMessageBody body = new SignedMessageBody(expectedArgument.value(), packet.timeStamp(), packet.salt(), lastSeenMessages);
            signedArguments.put(expectedArgument.name(), this.signedMessageDecoder.unpack(clientArgument.signature(), body));
        }

        for (SignableCommand.Argument<S> expectedArgument : parsedArguments) {
            if (!signedArguments.containsKey(expectedArgument.name())) {
                throw createSignedArgumentMismatchException(packet.command(), argumentSignatures, parsedArguments);
            }
        }

        return signedArguments;
    }

    private <S> Map<String, PlayerChatMessage> collectUnsignedArguments(final List<SignableCommand.Argument<S>> parsedArguments) throws SignedMessageChain.DecodeException {
        Map<String, PlayerChatMessage> arguments = new HashMap<>();

        for (SignableCommand.Argument<S> parsedArgument : parsedArguments) {
            SignedMessageBody body = SignedMessageBody.unsigned(parsedArgument.value());
            arguments.put(parsedArgument.name(), this.signedMessageDecoder.unpack(null, body));
        }

        return arguments;
    }

    private static <S> SignedMessageChain.DecodeException createSignedArgumentMismatchException(
        final String command, final List<ArgumentSignatures.Entry> clientArguments, final List<SignableCommand.Argument<S>> expectedArguments
    ) {
        String clientNames = clientArguments.stream().map(ArgumentSignatures.Entry::name).collect(Collectors.joining(", "));
        String expectedNames = expectedArguments.stream().map(SignableCommand.Argument::name).collect(Collectors.joining(", "));
        LOGGER.error("Signed command mismatch between server and client ('{}'): got [{}] from client, but expected [{}]", command, clientNames, expectedNames);
        return new SignedMessageChain.DecodeException(INVALID_COMMAND_SIGNATURE);
    }

    private ParseResults<CommandSourceStack> parseCommand(final String command) {
        CommandDispatcher<CommandSourceStack> commands = this.server.getCommands().getDispatcher();
        return commands.parse(command, this.player.createCommandSourceStack());
    }

    private void tryHandleChat(final String message, final boolean isCommand, final Runnable chatHandler) {
        if (isChatMessageIllegal(message)) {
            this.disconnect(Component.translatable("multiplayer.disconnect.illegal_characters"));
        } else if (!isCommand && this.player.getChatVisibility() == ChatVisiblity.HIDDEN) {
            this.send(new ClientboundSystemChatPacket(Component.translatable("chat.disabled.options").withStyle(ChatFormatting.RED), false));
        } else {
            this.player.resetLastActionTime();
            this.server.execute(chatHandler);
        }
    }

    private Optional<LastSeenMessages> unpackAndApplyLastSeen(final LastSeenMessages.Update update) {
        synchronized (this.lastSeenMessages) {
            Optional var10000;
            try {
                LastSeenMessages result = this.lastSeenMessages.applyUpdate(update);
                var10000 = Optional.of(result);
            } catch (LastSeenMessagesValidator.ValidationException e) {
                LOGGER.error("Failed to validate message acknowledgements from {}: {}", this.player.getPlainTextName(), e.getMessage());
                this.disconnect(CHAT_VALIDATION_FAILED);
                return Optional.empty();
            }

            return var10000;
        }
    }

    private static boolean isChatMessageIllegal(final String message) {
        for (int i = 0; i < message.length(); i++) {
            if (!StringUtil.isAllowedChatCharacter(message.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    private PlayerChatMessage getSignedMessage(final ServerboundChatPacket packet, final LastSeenMessages lastSeenMessages) throws SignedMessageChain.DecodeException {
        SignedMessageBody body = new SignedMessageBody(packet.message(), packet.timeStamp(), packet.salt(), lastSeenMessages);
        return this.signedMessageDecoder.unpack(packet.signature(), body);
    }

    private void broadcastChatMessage(final PlayerChatMessage message) {
        this.server.getPlayerList().broadcastChatMessage(message, this.player, ChatType.bind(ChatType.CHAT, this.player));
        this.detectChatRateSpam();
    }

    private void detectRateSpam(final TickThrottler throttler) {
        throttler.increment();
        if (!throttler.isUnderThreshold()
            && !this.server.getPlayerList().isOp(this.player.nameAndId())
            && !this.server.isSingleplayerOwner(this.player.nameAndId())) {
            this.disconnect(Component.translatable("disconnect.spam"));
        }
    }

    private void detectCommandRateSpam() {
        this.detectRateSpam(this.commandSpamThrottler);
    }

    private void detectChatRateSpam() {
        this.detectRateSpam(this.chatSpamThrottler);
    }

    @Override
    public void handleChatAck(final ServerboundChatAckPacket packet) {
        synchronized (this.lastSeenMessages) {
            try {
                this.lastSeenMessages.applyOffset(packet.offset());
            } catch (LastSeenMessagesValidator.ValidationException e) {
                LOGGER.error("Failed to validate message acknowledgement offset from {}: {}", this.player.getPlainTextName(), e.getMessage());
                this.disconnect(CHAT_VALIDATION_FAILED);
            }
        }
    }

    @Override
    public void handleAnimate(final ServerboundSwingPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        this.player.swing(packet.getHand());
    }

    @Override
    public void handlePlayerCommand(final ServerboundPlayerCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded()) {
            this.player.resetLastActionTime();
            switch (packet.getAction()) {
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
                    if (this.player.getControlledVehicle() instanceof PlayerRideableJumping vehicle) {
                        int data = packet.getData();
                        if (vehicle.canJump() && data > 0) {
                            vehicle.handleStartJump(data);
                        }
                    }
                    break;
                case STOP_RIDING_JUMP:
                    if (this.player.getControlledVehicle() instanceof PlayerRideableJumping vehicle) {
                        vehicle.handleStopJump();
                    }
                    break;
                case OPEN_INVENTORY:
                    if (this.player.getVehicle() instanceof HasCustomInventoryScreen vehicleWithInventory) {
                        vehicleWithInventory.openCustomInventoryScreen(this.player);
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

    public void sendPlayerChatMessage(final PlayerChatMessage message, final ChatType.Bound chatType) {
        this.send(
            new ClientboundPlayerChatPacket(
                this.nextChatIndex++,
                message.link().sender(),
                message.link().index(),
                message.signature(),
                message.signedBody().pack(this.messageSignatureCache),
                message.unsignedContent(),
                message.filterMask(),
                chatType
            )
        );
        MessageSignature signature = message.signature();
        if (signature != null) {
            this.messageSignatureCache.push(message.signedBody(), message.signature());
            int trackedCount;
            synchronized (this.lastSeenMessages) {
                this.lastSeenMessages.addPending(signature);
                trackedCount = this.lastSeenMessages.trackedMessagesCount();
            }

            if (trackedCount > 4096) {
                this.disconnect(Component.translatable("multiplayer.disconnect.too_many_pending_chats"));
            }
        }
    }

    public void sendDisguisedChatMessage(final Component content, final ChatType.Bound chatType) {
        this.send(new ClientboundDisguisedChatPacket(content, chatType));
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
    public void handlePingRequest(final ServerboundPingRequestPacket packet) {
        this.connection.send(new ClientboundPongResponsePacket(packet.getTime()));
    }

    @Override
    public void handleAttack(final ServerboundAttackPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded() && !this.player.isSpectator()) {
            ServerLevel level = this.player.level();
            Entity target = level.getEntityOrPart(packet.entityId());
            this.player.resetLastActionTime();
            if (target != null && level.getWorldBorder().isWithinBounds(target.blockPosition())) {
                AABB targetBounds = target.getBoundingBox();
                ItemStack mainHandItem = this.player.getMainHandItem();
                if (this.player.isWithinAttackRange(mainHandItem, targetBounds, 3.0)) {
                    if (!mainHandItem.has(DataComponents.PIERCING_WEAPON)) {
                        if (target instanceof ItemEntity
                            || target instanceof ExperienceOrb
                            || target == this.player
                            || target instanceof AbstractArrow abstractArrow && !abstractArrow.isAttackable()) {
                            this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
                            LOGGER.warn("Player {} tried to attack an invalid entity", this.player.getPlainTextName());
                        } else if (mainHandItem.isItemEnabled(level.enabledFeatures())) {
                            if (!this.player.cannotAttackWithItem(mainHandItem, 5)) {
                                this.player.attack(target);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleInteract(final ServerboundInteractPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded()) {
            ServerLevel level = this.player.level();
            Entity target = level.getEntityOrPart(packet.entityId());
            this.player.resetLastActionTime();
            this.player.setShiftKeyDown(packet.usingSecondaryAction());
            if (target != null && level.getWorldBorder().isWithinBounds(target.blockPosition())) {
                AABB targetBounds = target.getBoundingBox();
                if (this.player.isWithinEntityInteractionRange(targetBounds, 3.0)) {
                    InteractionHand hand = packet.hand();
                    Vec3 location = packet.location();
                    ItemStack tool = this.player.getItemInHand(hand);
                    if (tool.isItemEnabled(level.enabledFeatures())) {
                        ItemStack usedItemStack = tool.copy();
                        if (this.player.interactOn(target, hand, location) instanceof InteractionResult.Success success) {
                            ItemStack awardedForStack = success.wasItemInteraction() ? usedItemStack : ItemStack.EMPTY;
                            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(this.player, awardedForStack, target);
                            if (success.swingSource() == InteractionResult.SwingSource.SERVER) {
                                this.player.swing(hand, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleSpectatorAction(final ServerboundSpectatorActionPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.hasClientLoaded() && this.player.isSpectator()) {
            this.player.resetLastActionTime();
            if (packet.spectateEntityId().isPresent()) {
                ServerLevel level = this.player.level();
                Entity target = level.getEntityOrPart(packet.spectateEntityId().getAsInt());
                if (target != null && level.getWorldBorder().isWithinBounds(target.blockPosition())) {
                    if (this.player.isWithinEntityInteractionRange(target.getBoundingBox(), 3.0)) {
                        if (target.isPickable()) {
                            this.player.setCamera(target);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleClientCommand(final ServerboundClientCommandPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        ServerboundClientCommandPacket.Action action = packet.getAction();
        switch (action) {
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
                    }
                }
                break;
            case REQUEST_STATS:
                this.player.getStats().sendStats(this.player);
                break;
            case REQUEST_GAMERULE_VALUES:
                this.sendGameRuleValues();
        }
    }

    private void sendGameRuleValues() {
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            LOGGER.warn("Player {} tried to request game rule values without required permissions", this.player.getGameProfile().name());
        } else {
            GameRules gameRules = this.player.level().getGameRules();
            Map<ResourceKey<GameRule<?>>, String> values = new HashMap<>();
            gameRules.availableRules().forEach(rule -> addGameRuleValue(gameRules, values, (GameRule<?>)rule));
            this.send(new ClientboundGameRuleValuesPacket(values));
        }
    }

    private static <T> void addGameRuleValue(final GameRules gameRules, final Map<ResourceKey<GameRule<?>>, String> values, final GameRule<T> rule) {
        BuiltInRegistries.GAME_RULE.getResourceKey(rule).ifPresent(key -> values.put((ResourceKey<GameRule<?>>)key, rule.serialize(gameRules.get(rule))));
    }

    @Override
    public void handleContainerClose(final ServerboundContainerClosePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.doCloseContainer();
    }

    @Override
    public void handleContainerClick(final ServerboundContainerClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.containerId()) {
            if (this.player.isSpectator() || this.player.isDeadOrDying()) {
                this.player.containerMenu.sendAllDataToRemote();
            } else if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                int slotIndex = packet.slotNum();
                if (!this.player.containerMenu.isValidSlotIndex(slotIndex)) {
                    LOGGER.debug(
                        "Player {} clicked invalid slot index: {}, available slots: {}",
                        this.player.getPlainTextName(),
                        slotIndex,
                        this.player.containerMenu.slots.size()
                    );
                } else {
                    boolean fullResyncNeeded = packet.stateId() != this.player.containerMenu.getStateId();
                    this.player.containerMenu.suppressRemoteUpdates();
                    this.player.containerMenu.clicked(slotIndex, packet.buttonNum(), packet.containerInput(), this.player);

                    for (Entry<HashedStack> e : Int2ObjectMaps.fastIterable(packet.changedSlots())) {
                        this.player.containerMenu.setRemoteSlotUnsafe(e.getIntKey(), e.getValue());
                    }

                    this.player.containerMenu.setRemoteCarried(packet.carriedItem());
                    this.player.containerMenu.resumeRemoteUpdates();
                    if (fullResyncNeeded) {
                        this.player.containerMenu.broadcastFullState();
                    } else {
                        this.player.containerMenu.broadcastChanges();
                    }
                }
            }
        }
    }

    @Override
    public void handlePlaceRecipe(final ServerboundPlaceRecipePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (!this.player.isSpectator() && this.player.containerMenu.containerId == packet.containerId()) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                RecipeManager.ServerDisplayInfo displayInfo = this.server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
                if (displayInfo != null) {
                    RecipeHolder<?> recipe = displayInfo.parent();
                    if (this.player.getRecipeBook().contains(recipe.id())) {
                        if (this.player.containerMenu instanceof RecipeBookMenu recipeBookMenu) {
                            if (recipe.value().placementInfo().isImpossibleToPlace()) {
                                LOGGER.debug("Player {} tried to place impossible recipe {}", this.player, recipe.id().identifier());
                                return;
                            }

                            RecipeBookMenu.PostPlaceAction postPlaceAction = recipeBookMenu.handlePlacement(
                                packet.useMaxItems(), this.player.isCreative(), recipe, this.player.level(), this.player.getInventory()
                            );
                            if (postPlaceAction == RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE) {
                                this.send(new ClientboundPlaceGhostRecipePacket(this.player.containerMenu.containerId, displayInfo.display().display()));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleContainerButtonClick(final ServerboundContainerButtonClickPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.resetLastActionTime();
        if (this.player.containerMenu.containerId == packet.containerId() && !this.player.isSpectator()) {
            if (!this.player.containerMenu.stillValid(this.player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", this.player, this.player.containerMenu);
            } else {
                boolean clickAccepted = this.player.containerMenu.clickMenuButton(this.player, packet.buttonId());
                if (clickAccepted) {
                    this.player.containerMenu.broadcastChanges();
                }
            }
        }
    }

    @Override
    public void handleSetCreativeModeSlot(final ServerboundSetCreativeModeSlotPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.hasInfiniteMaterials()) {
            boolean drop = packet.slotNum() < 0;
            ItemStack itemStack = packet.itemStack();
            if (!itemStack.isItemEnabled(this.player.level().enabledFeatures())) {
                return;
            }

            boolean validSlot = packet.slotNum() >= 1 && packet.slotNum() <= 45;
            boolean validData = itemStack.isEmpty() || itemStack.getCount() <= itemStack.getMaxStackSize();
            if (validSlot && validData) {
                this.player.inventoryMenu.getSlot(packet.slotNum()).setByPlayer(itemStack);
                this.player.inventoryMenu.setRemoteSlot(packet.slotNum(), itemStack);
                this.player.inventoryMenu.broadcastChanges();
            } else if (drop && validData) {
                if (this.dropSpamThrottler.isUnderThreshold()) {
                    this.dropSpamThrottler.increment();
                    this.player.drop(itemStack, true);
                } else {
                    LOGGER.warn("Player {} was dropping items too fast in creative mode, ignoring.", this.player.getPlainTextName());
                }
            }
        }
    }

    @Override
    public void handleSignUpdate(final ServerboundSignUpdatePacket packet) {
        List<String> lines = Stream.of(packet.getLines()).map(ChatFormatting::stripFormatting).collect(Collectors.toList());
        this.filterTextPacket(lines).thenAcceptAsync(filteredLines -> this.updateSignText(packet, (List<FilteredText>)filteredLines), this.server);
    }

    private void updateSignText(final ServerboundSignUpdatePacket packet, final List<FilteredText> lines) {
        this.player.resetLastActionTime();
        ServerLevel level = this.player.level();
        BlockPos pos = packet.getPos();
        if (level.hasChunkAt(pos)) {
            if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
                return;
            }

            sign.updateSignText(this.player, packet.isFrontText(), lines);
        }
    }

    @Override
    public void handlePlayerAbilities(final ServerboundPlayerAbilitiesPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.getAbilities().flying = packet.isFlying() && this.player.getAbilities().mayfly;
    }

    @Override
    public void handleClientInformation(final ServerboundClientInformationPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        boolean wasHatShown = this.player.isModelPartShown(PlayerModelPart.HAT);
        this.player.updateOptions(packet.information());
        if (this.player.isModelPartShown(PlayerModelPart.HAT) != wasHatShown) {
            this.server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT, this.player));
        }
    }

    @Override
    public void handleChangeDifficulty(final ServerboundChangeDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) && !this.isSingleplayerOwner()) {
            LOGGER.warn(
                "Player {} tried to change difficulty to {} without required permissions",
                this.player.getGameProfile().name(),
                packet.difficulty().getDisplayName()
            );
        } else {
            this.server.setDifficulty(packet.difficulty(), false);
        }
    }

    @Override
    public void handleChangeGameMode(final ServerboundChangeGameModePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!GameModeCommand.PERMISSION_CHECK.check(this.player.permissions())) {
            LOGGER.warn(
                "Player {} tried to change game mode to {} without required permissions",
                this.player.getGameProfile().name(),
                packet.mode().getShortDisplayName().getString()
            );
        } else {
            GameModeCommand.setGameMode(this.player, packet.mode());
        }
    }

    @Override
    public void handleLockDifficulty(final ServerboundLockDifficultyPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (this.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) || this.isSingleplayerOwner()) {
            this.server.setDifficultyLocked(packet.isLocked());
        }
    }

    @Override
    public void handleChatSessionUpdate(final ServerboundChatSessionUpdatePacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        RemoteChatSession.Data newChatSession = packet.chatSession();
        ProfilePublicKey.Data oldProfileKey = this.chatSession != null ? this.chatSession.profilePublicKey().data() : null;
        ProfilePublicKey.Data newProfileKey = newChatSession.profilePublicKey();
        if (!Objects.equals(oldProfileKey, newProfileKey)) {
            if (oldProfileKey != null && newProfileKey.expiresAt().isBefore(oldProfileKey.expiresAt())) {
                this.disconnect(ProfilePublicKey.EXPIRED_PROFILE_PUBLIC_KEY);
            } else {
                try {
                    SignatureValidator profileKeySignatureValidator = this.server.services().profileKeySignatureValidator();
                    if (profileKeySignatureValidator == null) {
                        LOGGER.warn("Ignoring chat session from {} due to missing Services public key", this.player.getGameProfile().name());
                        return;
                    }

                    this.resetPlayerChatState(newChatSession.validate(this.player.getGameProfile(), profileKeySignatureValidator));
                } catch (ProfilePublicKey.ValidationException e) {
                    LOGGER.error("Failed to validate profile key: {}", e.getMessage());
                    this.disconnect(e.getComponent());
                }
            }
        }
    }

    @Override
    public void handleConfigurationAcknowledged(final ServerboundConfigurationAcknowledgedPacket packet) {
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
    public void handleChunkBatchReceived(final ServerboundChunkBatchReceivedPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.chunkSender.onChunkBatchReceivedByClient(packet.desiredChunksPerTick());
    }

    @Override
    public void handleDebugSubscriptionRequest(final ServerboundDebugSubscriptionRequestPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        this.player.requestDebugSubscriptions(packet.subscriptions());
    }

    private void resetPlayerChatState(final RemoteChatSession chatSession) {
        this.chatSession = chatSession;
        this.signedMessageDecoder = chatSession.createMessageDecoder(this.player.getUUID());
        this.chatMessageChain
            .append(
                () -> {
                    this.player.setChatSession(chatSession);
                    this.server
                        .getPlayerList()
                        .broadcastAll(
                            new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT), List.of(this.player))
                        );
                }
            );
    }

    @Override
    public void handleCustomPayload(final ServerboundCustomPayloadPacket packet) {
    }

    @Override
    public void handleClientTickEnd(final ServerboundClientTickEndPacket packet) {
        PacketUtils.ensureRunningOnSameThread(packet, this, this.player.level());
        if (!this.receivedMovementThisTick) {
            this.player.setKnownMovement(Vec3.ZERO);
        }

        this.receivedMovementThisTick = false;
    }

    private void handlePlayerKnownMovement(final Vec3 movement) {
        if (movement.lengthSqr() > 1.0E-5F) {
            this.player.resetLastActionTime();
        }

        this.player.setKnownMovement(movement);
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
}
