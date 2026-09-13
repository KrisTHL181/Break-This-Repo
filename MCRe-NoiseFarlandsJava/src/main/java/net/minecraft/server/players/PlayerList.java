package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.FileUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class PlayerList {
    public static final File USERBANLIST_FILE = new File("banned-players.json");
    public static final File IPBANLIST_FILE = new File("banned-ips.json");
    public static final File OPLIST_FILE = new File("ops.json");
    public static final File WHITELIST_FILE = new File("whitelist.json");
    public static final Component CHAT_FILTERED_FULL = Component.translatable("chat.filtered_full");
    public static final Component DUPLICATE_LOGIN_DISCONNECT_MESSAGE = Component.translatable("multiplayer.disconnect.duplicate_login");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SEND_PLAYER_INFO_INTERVAL = 600;
    private static final SimpleDateFormat BAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z", Locale.ROOT);
    private final MinecraftServer server;
    private final List<ServerPlayer> players = Lists.newArrayList();
    private final Map<UUID, ServerPlayer> playersByUUID = Maps.newHashMap();
    private final UserBanList bans;
    private final IpBanList ipBans;
    private final ServerOpList ops;
    private final UserWhiteList whitelist;
    private final Map<UUID, ServerStatsCounter> stats = Maps.newHashMap();
    private final Map<UUID, PlayerAdvancements> advancements = Maps.newHashMap();
    private final PlayerDataStorage playerIo;
    private final LayeredRegistryAccess<RegistryLayer> registries;
    private int viewDistance;
    private int simulationDistance;
    private boolean allowCommandsForAllPlayers;
    private int sendAllPlayerInfoIn;

    public PlayerList(
        final MinecraftServer server,
        final LayeredRegistryAccess<RegistryLayer> registries,
        final PlayerDataStorage playerIo,
        final NotificationService notificationService
    ) {
        this.server = server;
        this.registries = registries;
        this.playerIo = playerIo;
        this.whitelist = new UserWhiteList(WHITELIST_FILE, notificationService);
        this.ops = new ServerOpList(OPLIST_FILE, notificationService);
        this.bans = new UserBanList(USERBANLIST_FILE, notificationService);
        this.ipBans = new IpBanList(IPBANLIST_FILE, notificationService);
    }

    public void placeNewPlayer(final Connection connection, final ServerPlayer player, final CommonListenerCookie cookie) {
        NameAndId gameProfile = player.nameAndId();
        UserNameToIdResolver profileCache = this.server.services().nameToIdCache();
        Optional<NameAndId> oldProfile = profileCache.get(gameProfile.id());
        String oldName = oldProfile.map(NameAndId::name).orElse(gameProfile.name());
        profileCache.add(gameProfile);
        ServerLevel level = player.level();
        String address = connection.getLoggableAddress(this.server.logIPs());
        LOGGER.info(
            "{}[{}] logged in with entity id {} at ({}, {}, {})",
            player.getPlainTextName(),
            address,
            player.getId(),
            player.getX(),
            player.getY(),
            player.getZ()
        );
        LevelData levelData = level.getLevelData();
        ServerGamePacketListenerImpl playerConnection = new ServerGamePacketListenerImpl(this.server, connection, player, cookie);
        connection.setupInboundProtocol(
            GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess()), playerConnection), playerConnection
        );
        playerConnection.suspendFlushing();
        GameRules gameRules = level.getGameRules();
        boolean immediateRespawn = gameRules.get(GameRules.IMMEDIATE_RESPAWN);
        boolean reducedDebugInfo = gameRules.get(GameRules.REDUCED_DEBUG_INFO);
        boolean doLimitedCrafting = gameRules.get(GameRules.LIMITED_CRAFTING);
        playerConnection.send(
            new ClientboundLoginPacket(
                player.getId(),
                levelData.isHardcore(),
                this.server.levelKeys(),
                this.getMaxPlayers(),
                this.getViewDistance(),
                this.getSimulationDistance(),
                reducedDebugInfo,
                !immediateRespawn,
                doLimitedCrafting,
                player.createCommonSpawnInfo(level),
                this.server.usesAuthentication(),
                this.server.enforceSecureProfile()
            )
        );
        playerConnection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        playerConnection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
        playerConnection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
        RecipeManager recipeManager = this.server.getRecipeManager();
        playerConnection.send(
            new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), recipeManager.getSynchronizedStonecutterRecipes())
        );
        this.sendPlayerPermissionLevel(player);
        player.getStats().markAllDirty();
        player.getRecipeBook().sendInitialRecipeBook(player);
        this.updateEntireScoreboard(level.getScoreboard(), player);
        this.server.invalidateStatus();
        MutableComponent component;
        if (player.getGameProfile().name().equalsIgnoreCase(oldName)) {
            component = Component.translatable("multiplayer.player.joined", player.getDisplayName());
        } else {
            component = Component.translatable("multiplayer.player.joined.renamed", player.getDisplayName(), oldName);
        }

        this.broadcastSystemMessage(component.withStyle(ChatFormatting.YELLOW), false);
        playerConnection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        ServerStatus status = this.server.getStatus();
        if (status != null && !cookie.transferred()) {
            player.sendServerStatus(status);
        }

        player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        this.sendLevelInfo(player, level);
        level.addNewPlayer(player);
        this.server.getCustomBossEvents().onPlayerConnect(player);
        this.sendActivePlayerEffects(player);
        player.initInventoryMenu();
        this.server.notificationManager().playerJoined(player);
        playerConnection.resumeFlushing();
    }

    protected void updateEntireScoreboard(final ServerScoreboard scoreboard, final ServerPlayer player) {
        Set<Objective> objectives = Sets.newHashSet();

        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        }

        for (DisplaySlot slot : DisplaySlot.values()) {
            Objective objective = scoreboard.getDisplayObjective(slot);
            if (objective != null && !objectives.contains(objective)) {
                for (Packet<?> packet : scoreboard.getStartTrackingPackets(objective)) {
                    player.connection.send(packet);
                }

                objectives.add(objective);
            }
        }
    }

    public void addWorldborderListener(final ServerLevel level) {
        level.getWorldBorder().addListener(new BorderChangeListener() {
            @Override
            public void onSetSize(final WorldBorder border, final double newSize) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(border), level.dimension());
            }

            @Override
            public void onLerpSize(final WorldBorder border, final double fromSize, final double targetSize, final long ticks, final long gameTime) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(border), level.dimension());
            }

            @Override
            public void onSetCenter(final WorldBorder border, final double x, final double z) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(border), level.dimension());
            }

            @Override
            public void onSetWarningTime(final WorldBorder border, final int time) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(border), level.dimension());
            }

            @Override
            public void onSetWarningBlocks(final WorldBorder border, final int blocks) {
                PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(border), level.dimension());
            }

            @Override
            public void onSetDamagePerBlock(final WorldBorder border, final double damagePerBlock) {
            }

            @Override
            public void onSetSafeZone(final WorldBorder border, final double safeZone) {
            }
        });
    }

    public Optional<CompoundTag> loadPlayerData(final NameAndId nameAndId) {
        UUID lastSingleplayerOwnerUUID = this.server.getWorldData().getSinglePlayerUUID();
        if (this.server.isSingleplayerOwner(nameAndId) && lastSingleplayerOwnerUUID != null) {
            LOGGER.debug("loading single player");
            return this.playerIo.load(new NameAndId(lastSingleplayerOwnerUUID, "<singleplayer owner>"));
        } else {
            return this.playerIo.load(nameAndId);
        }
    }

    protected void save(final ServerPlayer player) {
        this.playerIo.save(player);
        ServerStatsCounter stats = this.stats.get(player.getUUID());
        if (stats != null) {
            stats.save();
        }

        PlayerAdvancements advancements = this.advancements.get(player.getUUID());
        if (advancements != null) {
            advancements.save();
        }
    }

    public void remove(final ServerPlayer player) {
        ServerLevel level = player.level();
        player.awardStat(Stats.LEAVE_GAME);
        this.save(player);
        if (player.isPassenger()) {
            Entity vehicle = player.getRootVehicle();
            if (vehicle.hasExactlyOnePlayerPassenger()) {
                LOGGER.debug("Removing player mount");
                player.stopRiding();
                vehicle.getPassengersAndSelf().forEach(e -> e.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
            }
        }

        player.unRide();

        for (ThrownEnderpearl enderpearl : player.getEnderPearls()) {
            enderpearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        }

        level.removePlayerImmediately(player, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
        player.getAdvancements().clearTriggers();
        this.players.remove(player);
        this.server.getCustomBossEvents().onPlayerDisconnect(player);
        UUID uuid = player.getUUID();
        ServerPlayer serverPlayer = this.playersByUUID.get(uuid);
        if (serverPlayer == player) {
            this.playersByUUID.remove(uuid);
            this.stats.remove(uuid);
            this.advancements.remove(uuid);
            this.server.notificationManager().playerLeft(player);
        }

        this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
    }

    public @Nullable Component canPlayerLogin(final SocketAddress address, final NameAndId nameAndId) {
        if (this.bans.isBanned(nameAndId)) {
            UserBanListEntry ban = this.bans.get(nameAndId);
            MutableComponent reason = Component.translatable("multiplayer.disconnect.banned.reason", ban.getReasonMessage());
            if (ban.getExpires() != null) {
                reason.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(ban.getExpires())));
            }

            return reason;
        } else {
            if (!this.isWhiteListed(nameAndId)) {
                return Component.translatable("multiplayer.disconnect.not_whitelisted");
            }

            if (this.ipBans.isBanned(address)) {
                IpBanListEntry ban = this.ipBans.get(address);
                MutableComponent reason = Component.translatable("multiplayer.disconnect.banned_ip.reason", ban.getReasonMessage());
                if (ban.getExpires() != null) {
                    reason.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ban.getExpires())));
                }

                return reason;
            } else {
                return this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(nameAndId)
                    ? Component.translatable("multiplayer.disconnect.server_full")
                    : null;
            }
        }
    }

    public boolean disconnectAllPlayersWithProfile(final UUID playerId) {
        Set<ServerPlayer> dupes = Sets.newIdentityHashSet();

        for (ServerPlayer player : this.players) {
            if (player.getUUID().equals(playerId)) {
                dupes.add(player);
            }
        }

        ServerPlayer serverPlayer = this.playersByUUID.get(playerId);
        if (serverPlayer != null) {
            dupes.add(serverPlayer);
        }

        for (ServerPlayer player : dupes) {
            player.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
        }

        return !dupes.isEmpty();
    }

    public ServerPlayer respawn(final ServerPlayer serverPlayer, final boolean keepAllPlayerData, final Entity.RemovalReason removalReason) {
        TeleportTransition respawnInfo = serverPlayer.findRespawnPositionAndUseSpawnBlock(!keepAllPlayerData, TeleportTransition.DO_NOTHING);
        this.players.remove(serverPlayer);
        serverPlayer.level().removePlayerImmediately(serverPlayer, removalReason);
        ServerLevel level = respawnInfo.newLevel();
        ServerPlayer player = new ServerPlayer(this.server, level, serverPlayer.getGameProfile(), serverPlayer.clientInformation());
        player.connection = serverPlayer.connection;
        player.restoreFrom(serverPlayer, keepAllPlayerData);
        player.setId(serverPlayer.getId());
        player.setMainArm(serverPlayer.getMainArm());
        if (!respawnInfo.missingRespawnBlock()) {
            player.copyRespawnPosition(serverPlayer);
        }

        for (String tag : serverPlayer.entityTags()) {
            player.addTag(tag);
        }

        Vec3 pos = respawnInfo.position();
        player.snapTo(pos.x, pos.y, pos.z, respawnInfo.yRot(), respawnInfo.xRot());
        if (respawnInfo.missingRespawnBlock()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
        }

        byte dataToKeep = (byte)(keepAllPlayerData ? 1 : 0);
        ServerLevel playerLevel = player.level();
        LevelData levelData = playerLevel.getLevelData();
        player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(playerLevel), dataToKeep));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        player.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        player.connection.send(new ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel));
        this.sendActivePlayerEffects(player);
        this.sendLevelInfo(player, level);
        this.sendPlayerPermissionLevel(player);
        level.addRespawnedPlayer(player);
        this.players.add(player);
        this.playersByUUID.put(player.getUUID(), player);
        player.initInventoryMenu();
        player.setHealth(player.getHealth());
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (!keepAllPlayerData && respawnConfig != null) {
            LevelData.RespawnData respawnData = respawnConfig.respawnData();
            ServerLevel respawnLevel = this.server.getLevel(respawnData.dimension());
            if (respawnLevel != null) {
                BlockPos respawnPosition = respawnData.pos();
                BlockState blockState = respawnLevel.getBlockState(respawnPosition);
                if (blockState.is(Blocks.RESPAWN_ANCHOR)) {
                    player.connection
                        .send(
                            new ClientboundSoundPacket(
                                SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                                SoundSource.BLOCKS,
                                respawnPosition.getX(),
                                respawnPosition.getY(),
                                respawnPosition.getZ(),
                                1.0F,
                                1.0F,
                                level.getRandom().nextLong()
                            )
                        );
                }
            }
        }

        return player;
    }

    public void sendActivePlayerEffects(final ServerPlayer player) {
        this.sendActiveEffects(player, player.connection);
    }

    public void sendActiveEffects(final LivingEntity livingEntity, final ServerGamePacketListenerImpl connection) {
        for (MobEffectInstance effect : livingEntity.getActiveEffects()) {
            connection.send(new ClientboundUpdateMobEffectPacket(livingEntity.getId(), effect, false));
        }
    }

    public void sendPlayerPermissionLevel(final ServerPlayer player) {
        LevelBasedPermissionSet permissions = this.server.getProfilePermissions(player.nameAndId());
        this.sendPlayerPermissionLevel(player, permissions);
    }

    public void tick() {
        if (++this.sendAllPlayerInfoIn > 600) {
            this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
            this.sendAllPlayerInfoIn = 0;
        }
    }

    public void broadcastAll(final Packet<?> packet) {
        for (ServerPlayer player : this.players) {
            player.connection.send(packet);
        }
    }

    public void broadcastAll(final Packet<?> packet, final ResourceKey<Level> dimension) {
        for (ServerPlayer player : this.players) {
            if (player.level().dimension() == dimension) {
                player.connection.send(packet);
            }
        }
    }

    public void broadcastSystemToTeam(final Player player, final Component message) {
        Team team = player.getTeam();
        if (team != null) {
            for (String name : team.getPlayers()) {
                ServerPlayer teamPlayer = this.getPlayerByName(name);
                if (teamPlayer != null && teamPlayer != player) {
                    teamPlayer.sendSystemMessage(message);
                }
            }
        }
    }

    public void broadcastSystemToAllExceptTeam(final Player player, final Component message) {
        Team team = player.getTeam();
        if (team == null) {
            this.broadcastSystemMessage(message, false);
        } else {
            for (int i = 0; i < this.players.size(); i++) {
                ServerPlayer targetPlayer = this.players.get(i);
                if (targetPlayer.getTeam() != team) {
                    targetPlayer.sendSystemMessage(message);
                }
            }
        }
    }

    public String[] getPlayerNamesArray() {
        String[] names = new String[this.players.size()];

        for (int i = 0; i < this.players.size(); i++) {
            names[i] = this.players.get(i).getGameProfile().name();
        }

        return names;
    }

    public UserBanList getBans() {
        return this.bans;
    }

    public IpBanList getIpBans() {
        return this.ipBans;
    }

    public void op(final NameAndId nameAndId) {
        this.op(nameAndId, Optional.empty(), Optional.empty());
    }

    public void op(final NameAndId nameAndId, final Optional<LevelBasedPermissionSet> permissions, final Optional<Boolean> canBypassPlayerLimit) {
        this.ops
            .add(
                new ServerOpListEntry(
                    nameAndId, permissions.orElse(this.server.operatorUserPermissions()), canBypassPlayerLimit.orElse(this.ops.canBypassPlayerLimit(nameAndId))
                )
            );
        ServerPlayer player = this.getPlayer(nameAndId.id());
        if (player != null) {
            this.sendPlayerPermissionLevel(player);
        }
    }

    public void deop(final NameAndId nameAndId) {
        if (this.ops.remove(nameAndId)) {
            ServerPlayer player = this.getPlayer(nameAndId.id());
            if (player != null) {
                this.sendPlayerPermissionLevel(player);
            }
        }
    }

    private void sendPlayerPermissionLevel(final ServerPlayer player, final LevelBasedPermissionSet permissions) {
        if (player.connection != null) {
            byte eventId = switch (permissions.level()) {
                case ALL -> 24;
                case MODERATORS -> 25;
                case GAMEMASTERS -> 26;
                case ADMINS -> 27;
                case OWNERS -> 28;
            };
            player.connection.send(new ClientboundEntityEventPacket(player, eventId));
        }

        this.server.getCommands().sendCommands(player);
    }

    public boolean isWhiteListed(final NameAndId nameAndId) {
        return !this.isUsingWhitelist() || this.ops.contains(nameAndId) || this.whitelist.contains(nameAndId);
    }

    public boolean isOp(final NameAndId nameAndId) {
        if (this.ops.contains(nameAndId)) {
            return true;
        } else {
            return this.server.isSingleplayerOwner(nameAndId) ? this.server.getWorldData().isAllowCommands() : this.allowCommandsForAllPlayers;
        }
    }

    public @Nullable ServerPlayer getPlayerByName(final String name) {
        int size = this.players.size();

        for (int i = 0; i < size; i++) {
            ServerPlayer player = this.players.get(i);
            if (player.getGameProfile().name().equalsIgnoreCase(name)) {
                return player;
            }
        }

        return null;
    }

    public void broadcast(
        final @Nullable Player except,
        final double x,
        final double y,
        final double z,
        final double range,
        final ResourceKey<Level> dimension,
        final Packet<?> packet
    ) {
        for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer player = this.players.get(i);
            if (player != except && player.level().dimension() == dimension) {
                double xd = x - player.getX();
                double yd = y - player.getY();
                double zd = z - player.getZ();
                if (xd * xd + yd * yd + zd * zd < range * range) {
                    player.connection.send(packet);
                }
            }
        }
    }

    public void saveAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.save(this.players.get(i));
        }
    }

    public UserWhiteList getWhiteList() {
        return this.whitelist;
    }

    public String[] getWhiteListNames() {
        return this.whitelist.getUserList();
    }

    public ServerOpList getOps() {
        return this.ops;
    }

    public String[] getOpNames() {
        return this.ops.getUserList();
    }

    public void reloadWhiteList() {
    }

    public void sendLevelInfo(final ServerPlayer player, final ServerLevel level) {
        WorldBorder worldBorder = level.getWorldBorder();
        player.connection.send(new ClientboundInitializeBorderPacket(worldBorder));
        player.connection.send(this.server.clockManager().createFullSyncPacket());
        player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));
        if (level.isRaining()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)));
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)));
        }

        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
        this.server.tickRateManager().updateJoiningPlayer(player);
    }

    public void sendAllPlayerInfo(final ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        player.resetSentInfo();
        player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    public boolean isUsingWhitelist() {
        return this.server.isUsingWhitelist();
    }

    public List<ServerPlayer> getPlayersWithAddress(final String ip) {
        List<ServerPlayer> result = Lists.newArrayList();

        for (ServerPlayer player : this.players) {
            if (player.getIpAddress().equals(ip)) {
                result.add(player);
            }
        }

        return result;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public int getSimulationDistance() {
        return this.simulationDistance;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public void setAllowCommandsForAllPlayers(final boolean allowCommands) {
        this.allowCommandsForAllPlayers = allowCommands;
    }

    public void removeAll() {
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
        }
    }

    public void broadcastSystemMessage(final Component message, final boolean overlay) {
        this.broadcastSystemMessage(message, player -> message, overlay);
    }

    public void broadcastSystemMessage(final Component message, final Function<ServerPlayer, Component> playerMessages, final boolean overlay) {
        this.server.sendSystemMessage(message);

        for (ServerPlayer player : this.players) {
            Component playerMessage = playerMessages.apply(player);
            if (playerMessage != null) {
                player.sendSystemMessage(playerMessage, overlay);
            }
        }
    }

    public void broadcastChatMessage(final PlayerChatMessage message, final CommandSourceStack sender, final ChatType.Bound chatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender.getPlayer(), chatType);
    }

    public void broadcastChatMessage(final PlayerChatMessage message, final ServerPlayer sender, final ChatType.Bound chatType) {
        this.broadcastChatMessage(message, sender::shouldFilterMessageTo, sender, chatType);
    }

    private void broadcastChatMessage(
        final PlayerChatMessage message, final Predicate<ServerPlayer> isFiltered, final @Nullable ServerPlayer senderPlayer, final ChatType.Bound chatType
    ) {
        boolean trusted = this.verifyChatTrusted(message);
        this.server.logChatMessage(message.decoratedContent(), chatType, trusted ? null : "Not Secure");
        OutgoingChatMessage tracked = OutgoingChatMessage.create(message);
        boolean wasFullyFiltered = false;

        for (ServerPlayer player : this.players) {
            boolean filtered = isFiltered.test(player);
            player.sendChatMessage(tracked, filtered, chatType);
            wasFullyFiltered |= filtered && message.isFullyFiltered();
        }

        if (wasFullyFiltered && senderPlayer != null) {
            senderPlayer.sendSystemMessage(CHAT_FILTERED_FULL);
        }
    }

    private boolean verifyChatTrusted(final PlayerChatMessage message) {
        return message.hasSignature() && !message.hasExpiredServer(Instant.now());
    }

    public ServerStatsCounter getPlayerStats(final Player player) {
        GameProfile gameProfile = player.getGameProfile();
        return this.stats.computeIfAbsent(gameProfile.id(), id -> {
            Path targetFile = this.locateStatsFile(gameProfile);
            return new ServerStatsCounter(this.server, targetFile);
        });
    }

    private Path locateStatsFile(final GameProfile gameProfile) {
        Path statFolder = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        Path uuidStatsFile = statFolder.resolve(gameProfile.id() + ".json");
        if (Files.exists(uuidStatsFile)) {
            return uuidStatsFile;
        }

        String playerNameStatsFile = gameProfile.name() + ".json";
        if (FileUtil.isValidPathSegment(playerNameStatsFile)) {
            Path playerNameStatsPath = statFolder.resolve(playerNameStatsFile);
            if (Files.isRegularFile(playerNameStatsPath)) {
                try {
                    return Files.move(playerNameStatsPath, uuidStatsFile);
                } catch (IOException e) {
                    LOGGER.warn("Failed to copy file {} to {}", playerNameStatsFile, uuidStatsFile);
                    return playerNameStatsPath;
                }
            }
        }

        return uuidStatsFile;
    }

    public PlayerAdvancements getPlayerAdvancements(final ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerAdvancements result = this.advancements.get(uuid);
        if (result == null) {
            Path uuidStatsFile = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
            result = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), uuidStatsFile, player);
            this.advancements.put(uuid, result);
        }

        result.setPlayer(player);
        return result;
    }

    public void setViewDistance(final int viewDistance) {
        this.viewDistance = viewDistance;
        this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(viewDistance));

        for (ServerLevel level : this.server.getAllLevels()) {
            level.getChunkSource().setViewDistance(viewDistance);
        }
    }

    public void setSimulationDistance(final int simulationDistance) {
        this.simulationDistance = simulationDistance;
        this.broadcastAll(new ClientboundSetSimulationDistancePacket(simulationDistance));

        for (ServerLevel level : this.server.getAllLevels()) {
            level.getChunkSource().setSimulationDistance(simulationDistance);
        }
    }

    public List<ServerPlayer> getPlayers() {
        return this.players;
    }

    public Map<UUID, ServerPlayer> getPlayersByUUID() {
        return this.playersByUUID;
    }

    public @Nullable ServerPlayer getPlayer(final UUID uuid) {
        return this.playersByUUID.get(uuid);
    }

    public @Nullable ServerPlayer getPlayer(final String playerName) {
        for (ServerPlayer player : this.players) {
            if (player.getGameProfile().name().equalsIgnoreCase(playerName)) {
                return player;
            }
        }

        return null;
    }

    public boolean canBypassPlayerLimit(final NameAndId nameAndId) {
        return false;
    }

    public void reloadResources() {
        for (PlayerAdvancements advancements : this.advancements.values()) {
            advancements.reload(this.server.getAdvancements());
        }

        this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
        RecipeManager recipeManager = this.server.getRecipeManager();
        ClientboundUpdateRecipesPacket recipes = new ClientboundUpdateRecipesPacket(
            recipeManager.getSynchronizedItemProperties(), recipeManager.getSynchronizedStonecutterRecipes()
        );

        for (ServerPlayer player : this.players) {
            player.connection.send(recipes);
            player.getRecipeBook().sendInitialRecipeBook(player);
        }
    }

    public boolean isAllowCommandsForAllPlayers() {
        return this.allowCommandsForAllPlayers;
    }
}