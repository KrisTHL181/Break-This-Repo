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
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
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

   public PlayerList(MinecraftServer p_203842_, LayeredRegistryAccess<RegistryLayer> p_251844_, PlayerDataStorage p_203844_, NotificationService p_425184_) {
      this.server = p_203842_;
      this.registries = p_251844_;
      this.playerIo = p_203844_;
      this.whitelist = new UserWhiteList(WHITELIST_FILE, p_425184_);
      this.ops = new ServerOpList(OPLIST_FILE, p_425184_);
      this.bans = new UserBanList(USERBANLIST_FILE, p_425184_);
      this.ipBans = new IpBanList(IPBANLIST_FILE, p_425184_);
   }

   public void placeNewPlayer(Connection p_11262_, ServerPlayer p_11263_, CommonListenerCookie p_297215_) {
      NameAndId nameandid = p_11263_.nameAndId();
      UserNameToIdResolver usernametoidresolver = this.server.services().nameToIdCache();
      Optional<NameAndId> optional = usernametoidresolver.get(nameandid.id());
      String s = optional.map(NameAndId::name).orElse(nameandid.name());
      usernametoidresolver.add(nameandid);
      ServerLevel serverlevel = p_11263_.level();
      String s1 = p_11262_.getLoggableAddress(this.server.logIPs());
      LOGGER.info(
         "{}[{}] logged in with entity id {} at ({}, {}, {})",
         new Object[]{p_11263_.getPlainTextName(), s1, p_11263_.getId(), p_11263_.getX(), p_11263_.getY(), p_11263_.getZ()}
      );
      LevelData leveldata = serverlevel.getLevelData();
      ServerGamePacketListenerImpl servergamepacketlistenerimpl = new ServerGamePacketListenerImpl(this.server, p_11262_, p_11263_, p_297215_);
      p_11262_.setupInboundProtocol(
         GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(this.server.registryAccess()), servergamepacketlistenerimpl),
         servergamepacketlistenerimpl
      );
      servergamepacketlistenerimpl.suspendFlushing();
      GameRules gamerules = serverlevel.getGameRules();
      boolean flag = gamerules.get(GameRules.IMMEDIATE_RESPAWN);
      boolean flag1 = gamerules.get(GameRules.REDUCED_DEBUG_INFO);
      boolean flag2 = gamerules.get(GameRules.LIMITED_CRAFTING);
      servergamepacketlistenerimpl.send(
         new ClientboundLoginPacket(
            p_11263_.getId(),
            leveldata.isHardcore(),
            this.server.levelKeys(),
            this.getMaxPlayers(),
            this.getViewDistance(),
            this.getSimulationDistance(),
            flag1,
            !flag,
            flag2,
            p_11263_.createCommonSpawnInfo(serverlevel),
            this.server.enforceSecureProfile()
         )
      );
      servergamepacketlistenerimpl.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
      servergamepacketlistenerimpl.send(new ClientboundPlayerAbilitiesPacket(p_11263_.getAbilities()));
      servergamepacketlistenerimpl.send(new ClientboundSetHeldSlotPacket(p_11263_.getInventory().getSelectedSlot()));
      RecipeManager recipemanager = this.server.getRecipeManager();
      servergamepacketlistenerimpl.send(
         new ClientboundUpdateRecipesPacket(recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes())
      );
      this.sendPlayerPermissionLevel(p_11263_);
      p_11263_.getStats().markAllDirty();
      p_11263_.getRecipeBook().sendInitialRecipeBook(p_11263_);
      this.updateEntireScoreboard(serverlevel.getScoreboard(), p_11263_);
      this.server.invalidateStatus();
      MutableComponent mutablecomponent;
      if (p_11263_.getGameProfile().name().equalsIgnoreCase(s)) {
         mutablecomponent = Component.translatable("multiplayer.player.joined", p_11263_.getDisplayName());
      } else {
         mutablecomponent = Component.translatable("multiplayer.player.joined.renamed", p_11263_.getDisplayName(), s);
      }

      this.broadcastSystemMessage(mutablecomponent.withStyle(ChatFormatting.YELLOW), false);
      servergamepacketlistenerimpl.teleport(p_11263_.getX(), p_11263_.getY(), p_11263_.getZ(), p_11263_.getYRot(), p_11263_.getXRot());
      ServerStatus serverstatus = this.server.getStatus();
      if (serverstatus != null && !p_297215_.transferred()) {
         p_11263_.sendServerStatus(serverstatus);
      }

      p_11263_.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(this.players));
      this.players.add(p_11263_);
      this.playersByUUID.put(p_11263_.getUUID(), p_11263_);
      this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(p_11263_)));
      this.sendLevelInfo(p_11263_, serverlevel);
      serverlevel.addNewPlayer(p_11263_);
      this.server.getCustomBossEvents().onPlayerConnect(p_11263_);
      this.sendActivePlayerEffects(p_11263_);
      p_11263_.initInventoryMenu();
      this.server.notificationManager().playerJoined(p_11263_);
      servergamepacketlistenerimpl.resumeFlushing();
   }

   protected void updateEntireScoreboard(ServerScoreboard p_11274_, ServerPlayer p_11275_) {
      Set<Objective> set = Sets.newHashSet();

      for (PlayerTeam playerteam : p_11274_.getPlayerTeams()) {
         p_11275_.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(playerteam, true));
      }

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         Objective objective = p_11274_.getDisplayObjective(displayslot);
         if (objective != null && !set.contains(objective)) {
            for (Packet<?> packet : p_11274_.getStartTrackingPackets(objective)) {
               p_11275_.connection.send(packet);
            }

            set.add(objective);
         }
      }
   }

   public void addWorldborderListener(final ServerLevel p_184210_) {
      p_184210_.getWorldBorder().addListener(new BorderChangeListener() {
         @Override
         public void onSetSize(WorldBorder p_427674_, double p_430162_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderSizePacket(p_427674_), p_184210_.dimension());
         }

         @Override
         public void onLerpSize(WorldBorder p_427221_, double p_423218_, double p_431392_, long p_424323_, long p_459193_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderLerpSizePacket(p_427221_), p_184210_.dimension());
         }

         @Override
         public void onSetCenter(WorldBorder p_430241_, double p_429384_, double p_427117_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderCenterPacket(p_430241_), p_184210_.dimension());
         }

         @Override
         public void onSetWarningTime(WorldBorder p_426124_, int p_424040_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDelayPacket(p_426124_), p_184210_.dimension());
         }

         @Override
         public void onSetWarningBlocks(WorldBorder p_429459_, int p_430035_) {
            PlayerList.this.broadcastAll(new ClientboundSetBorderWarningDistancePacket(p_429459_), p_184210_.dimension());
         }

         @Override
         public void onSetDamagePerBlock(WorldBorder p_424621_, double p_430466_) {
         }

         @Override
         public void onSetSafeZone(WorldBorder p_425557_, double p_424484_) {
         }
      });
   }

   public Optional<CompoundTag> loadPlayerData(NameAndId p_428198_) {
      CompoundTag compoundtag = this.server.getWorldData().getLoadedPlayerTag();
      if (this.server.isSingleplayerOwner(p_428198_) && compoundtag != null) {
         LOGGER.debug("loading single player");
         return Optional.of(compoundtag);
      } else {
         return this.playerIo.load(p_428198_);
      }
   }

   protected void save(ServerPlayer p_11277_) {
      this.playerIo.save(p_11277_);
      ServerStatsCounter serverstatscounter = this.stats.get(p_11277_.getUUID());
      if (serverstatscounter != null) {
         serverstatscounter.save();
      }

      PlayerAdvancements playeradvancements = this.advancements.get(p_11277_.getUUID());
      if (playeradvancements != null) {
         playeradvancements.save();
      }
   }

   public void remove(ServerPlayer p_11287_) {
      ServerLevel serverlevel = p_11287_.level();
      p_11287_.awardStat(Stats.LEAVE_GAME);
      this.save(p_11287_);
      if (p_11287_.isPassenger()) {
         Entity entity = p_11287_.getRootVehicle();
         if (entity.hasExactlyOnePlayerPassenger()) {
            LOGGER.debug("Removing player mount");
            p_11287_.stopRiding();
            entity.getPassengersAndSelf().forEach(p_215620_ -> p_215620_.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER));
         }
      }

      p_11287_.unRide();

      for (ThrownEnderpearl thrownenderpearl : p_11287_.getEnderPearls()) {
         thrownenderpearl.setRemoved(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      }

      serverlevel.removePlayerImmediately(p_11287_, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
      p_11287_.getAdvancements().stopListening();
      this.players.remove(p_11287_);
      this.server.getCustomBossEvents().onPlayerDisconnect(p_11287_);
      UUID uuid = p_11287_.getUUID();
      ServerPlayer serverplayer = this.playersByUUID.get(uuid);
      if (serverplayer == p_11287_) {
         this.playersByUUID.remove(uuid);
         this.stats.remove(uuid);
         this.advancements.remove(uuid);
         this.server.notificationManager().playerLeft(p_11287_);
      }

      this.broadcastAll(new ClientboundPlayerInfoRemovePacket(List.of(p_11287_.getUUID())));
   }

   public @Nullable Component canPlayerLogin(SocketAddress p_11257_, NameAndId p_426791_) {
      if (this.bans.isBanned(p_426791_)) {
         UserBanListEntry userbanlistentry = this.bans.get(p_426791_);
         MutableComponent mutablecomponent1 = Component.translatable("multiplayer.disconnect.banned.reason", userbanlistentry.getReasonMessage());
         if (userbanlistentry.getExpires() != null) {
            mutablecomponent1.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(userbanlistentry.getExpires())));
         }

         return mutablecomponent1;
      } else {
         if (!this.isWhiteListed(p_426791_)) {
            return Component.translatable("multiplayer.disconnect.not_whitelisted");
         }

         if (this.ipBans.isBanned(p_11257_)) {
            IpBanListEntry ipbanlistentry = this.ipBans.get(p_11257_);
            MutableComponent mutablecomponent = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanlistentry.getReasonMessage());
            if (ipbanlistentry.getExpires() != null) {
               mutablecomponent.append(
                  Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanlistentry.getExpires()))
               );
            }

            return mutablecomponent;
         } else {
            return this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(p_426791_)
               ? Component.translatable("multiplayer.disconnect.server_full")
               : null;
         }
      }
   }

   public boolean disconnectAllPlayersWithProfile(UUID p_429846_) {
      Set<ServerPlayer> set = Sets.newIdentityHashSet();

      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getUUID().equals(p_429846_)) {
            set.add(serverplayer);
         }
      }

      ServerPlayer serverplayer2 = this.playersByUUID.get(p_429846_);
      if (serverplayer2 != null) {
         set.add(serverplayer2);
      }

      for (ServerPlayer serverplayer1 : set) {
         serverplayer1.connection.disconnect(DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
      }

      return !set.isEmpty();
   }

   public ServerPlayer respawn(ServerPlayer p_11237_, boolean p_11238_, Entity.RemovalReason p_344452_) {
      TeleportTransition teleporttransition = p_11237_.findRespawnPositionAndUseSpawnBlock(!p_11238_, TeleportTransition.DO_NOTHING);
      this.players.remove(p_11237_);
      p_11237_.level().removePlayerImmediately(p_11237_, p_344452_);
      ServerLevel serverlevel = teleporttransition.newLevel();
      ServerPlayer serverplayer = new ServerPlayer(this.server, serverlevel, p_11237_.getGameProfile(), p_11237_.clientInformation());
      serverplayer.connection = p_11237_.connection;
      serverplayer.restoreFrom(p_11237_, p_11238_);
      serverplayer.setId(p_11237_.getId());
      serverplayer.setMainArm(p_11237_.getMainArm());
      if (!teleporttransition.missingRespawnBlock()) {
         serverplayer.copyRespawnPosition(p_11237_);
      }

      for (String s : p_11237_.getTags()) {
         serverplayer.addTag(s);
      }

      Vec3 vec3 = teleporttransition.position();
      serverplayer.snapTo(vec3.x, vec3.y, vec3.z, teleporttransition.yRot(), teleporttransition.xRot());
      if (teleporttransition.missingRespawnBlock()) {
         serverplayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
      }

      byte b0 = (byte)(p_11238_ ? 1 : 0);
      ServerLevel serverlevel1 = serverplayer.level();
      LevelData leveldata = serverlevel1.getLevelData();
      serverplayer.connection.send(new ClientboundRespawnPacket(serverplayer.createCommonSpawnInfo(serverlevel1), b0));
      serverplayer.connection.teleport(serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(), serverplayer.getYRot(), serverplayer.getXRot());
      serverplayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverlevel.getRespawnData()));
      serverplayer.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
      serverplayer.connection
         .send(new ClientboundSetExperiencePacket(serverplayer.experienceProgress, serverplayer.totalExperience, serverplayer.experienceLevel));
      this.sendActivePlayerEffects(serverplayer);
      this.sendLevelInfo(serverplayer, serverlevel);
      this.sendPlayerPermissionLevel(serverplayer);
      serverlevel.addRespawnedPlayer(serverplayer);
      this.players.add(serverplayer);
      this.playersByUUID.put(serverplayer.getUUID(), serverplayer);
      serverplayer.initInventoryMenu();
      serverplayer.setHealth(serverplayer.getHealth());
      ServerPlayer.RespawnConfig serverplayer$respawnconfig = serverplayer.getRespawnConfig();
      if (!p_11238_ && serverplayer$respawnconfig != null) {
         LevelData.RespawnData leveldata$respawndata = serverplayer$respawnconfig.respawnData();
         ServerLevel serverlevel2 = this.server.getLevel(leveldata$respawndata.dimension());
         if (serverlevel2 != null) {
            BlockPos blockpos = leveldata$respawndata.pos();
            BlockState blockstate = serverlevel2.getBlockState(blockpos);
            if (blockstate.is(Blocks.RESPAWN_ANCHOR)) {
               serverplayer.connection
                  .send(
                     new ClientboundSoundPacket(
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                        SoundSource.BLOCKS,
                        blockpos.getX(),
                        blockpos.getY(),
                        blockpos.getZ(),
                        1.0F,
                        1.0F,
                        serverlevel.getRandom().nextLong()
                     )
                  );
            }
         }
      }

      return serverplayer;
   }

   public void sendActivePlayerEffects(ServerPlayer p_344496_) {
      this.sendActiveEffects(p_344496_, p_344496_.connection);
   }

   public void sendActiveEffects(LivingEntity p_344934_, ServerGamePacketListenerImpl p_343169_) {
      for (MobEffectInstance mobeffectinstance : p_344934_.getActiveEffects()) {
         p_343169_.send(new ClientboundUpdateMobEffectPacket(p_344934_.getId(), mobeffectinstance, false));
      }
   }

   public void sendPlayerPermissionLevel(ServerPlayer p_11290_) {
      LevelBasedPermissionSet levelbasedpermissionset = this.server.getProfilePermissions(p_11290_.nameAndId());
      this.sendPlayerPermissionLevel(p_11290_, levelbasedpermissionset);
   }

   public void tick() {
      if (++this.sendAllPlayerInfoIn > 600) {
         this.broadcastAll(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY), this.players));
         this.sendAllPlayerInfoIn = 0;
      }
   }

   public void broadcastAll(Packet<?> p_11269_) {
      for (ServerPlayer serverplayer : this.players) {
         serverplayer.connection.send(p_11269_);
      }
   }

   public void broadcastAll(Packet<?> p_11271_, ResourceKey<Level> p_11272_) {
      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.level().dimension() == p_11272_) {
            serverplayer.connection.send(p_11271_);
         }
      }
   }

   public void broadcastSystemToTeam(Player p_215622_, Component p_215623_) {
      Team team = p_215622_.getTeam();
      if (team != null) {
         for (String s : team.getPlayers()) {
            ServerPlayer serverplayer = this.getPlayerByName(s);
            if (serverplayer != null && serverplayer != p_215622_) {
               serverplayer.sendSystemMessage(p_215623_);
            }
         }
      }
   }

   public void broadcastSystemToAllExceptTeam(Player p_215650_, Component p_215651_) {
      Team team = p_215650_.getTeam();
      if (team == null) {
         this.broadcastSystemMessage(p_215651_, false);
      } else {
         for (int i = 0; i < this.players.size(); i++) {
            ServerPlayer serverplayer = this.players.get(i);
            if (serverplayer.getTeam() != team) {
               serverplayer.sendSystemMessage(p_215651_);
            }
         }
      }
   }

   public String[] getPlayerNamesArray() {
      String[] astring = new String[this.players.size()];

      for (int i = 0; i < this.players.size(); i++) {
         astring[i] = this.players.get(i).getGameProfile().name();
      }

      return astring;
   }

   public UserBanList getBans() {
      return this.bans;
   }

   public IpBanList getIpBans() {
      return this.ipBans;
   }

   public void op(NameAndId p_423883_) {
      this.op(p_423883_, Optional.empty(), Optional.empty());
   }

   public void op(NameAndId p_425211_, Optional<LevelBasedPermissionSet> p_427477_, Optional<Boolean> p_426308_) {
      this.ops
         .add(
            new ServerOpListEntry(
               p_425211_, p_427477_.orElse(this.server.operatorUserPermissions()), p_426308_.orElse(this.ops.canBypassPlayerLimit(p_425211_))
            )
         );
      ServerPlayer serverplayer = this.getPlayer(p_425211_.id());
      if (serverplayer != null) {
         this.sendPlayerPermissionLevel(serverplayer);
      }
   }

   public void deop(NameAndId p_428113_) {
      if (this.ops.remove(p_428113_)) {
         ServerPlayer serverplayer = this.getPlayer(p_428113_.id());
         if (serverplayer != null) {
            this.sendPlayerPermissionLevel(serverplayer);
         }
      }
   }

   private void sendPlayerPermissionLevel(ServerPlayer p_11227_, LevelBasedPermissionSet p_454787_) {
      if (p_11227_.connection != null) {
         byte b0 = switch (p_454787_.level()) {
            case ALL -> 24;
            case MODERATORS -> 25;
            case GAMEMASTERS -> 26;
            case ADMINS -> 27;
            case OWNERS -> 28;
         };
         p_11227_.connection.send(new ClientboundEntityEventPacket(p_11227_, b0));
      }

      this.server.getCommands().sendCommands(p_11227_);
   }

   public boolean isWhiteListed(NameAndId p_428911_) {
      return !this.isUsingWhitelist() || this.ops.contains(p_428911_) || this.whitelist.contains(p_428911_);
   }

   public boolean isOp(NameAndId p_430489_) {
      return this.ops.contains(p_430489_)
         || this.server.isSingleplayerOwner(p_430489_) && this.server.getWorldData().isAllowCommands()
         || this.allowCommandsForAllPlayers;
   }

   public @Nullable ServerPlayer getPlayerByName(String p_11256_) {
      int i = this.players.size();

      for (int j = 0; j < i; j++) {
         ServerPlayer serverplayer = this.players.get(j);
         if (serverplayer.getGameProfile().name().equalsIgnoreCase(p_11256_)) {
            return serverplayer;
         }
      }

      return null;
   }

   public void broadcast(
      @Nullable Player p_11242_, double p_11243_, double p_11244_, double p_11245_, double p_11246_, ResourceKey<Level> p_11247_, Packet<?> p_11248_
   ) {
      for (int i = 0; i < this.players.size(); i++) {
         ServerPlayer serverplayer = this.players.get(i);
         if (serverplayer != p_11242_ && serverplayer.level().dimension() == p_11247_) {
            double d0 = p_11243_ - serverplayer.getX();
            double d1 = p_11244_ - serverplayer.getY();
            double d2 = p_11245_ - serverplayer.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < p_11246_ * p_11246_) {
               serverplayer.connection.send(p_11248_);
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

   public void sendLevelInfo(ServerPlayer p_11230_, ServerLevel p_11231_) {
      WorldBorder worldborder = p_11231_.getWorldBorder();
      p_11230_.connection.send(new ClientboundInitializeBorderPacket(worldborder));
      p_11230_.connection
         .send(new ClientboundSetTimePacket(p_11231_.getGameTime(), p_11231_.getDayTime(), p_11231_.getGameRules().get(GameRules.ADVANCE_TIME)));
      p_11230_.connection.send(new ClientboundSetDefaultSpawnPositionPacket(p_11231_.getRespawnData()));
      if (p_11231_.isRaining()) {
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, p_11231_.getRainLevel(1.0F)));
         p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, p_11231_.getThunderLevel(1.0F)));
      }

      p_11230_.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0F));
      this.server.tickRateManager().updateJoiningPlayer(p_11230_);
   }

   public void sendAllPlayerInfo(ServerPlayer p_11293_) {
      p_11293_.inventoryMenu.sendAllDataToRemote();
      p_11293_.resetSentInfo();
      p_11293_.connection.send(new ClientboundSetHeldSlotPacket(p_11293_.getInventory().getSelectedSlot()));
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

   public List<ServerPlayer> getPlayersWithAddress(String p_11283_) {
      List<ServerPlayer> list = Lists.newArrayList();

      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getIpAddress().equals(p_11283_)) {
            list.add(serverplayer);
         }
      }

      return list;
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

   public @Nullable CompoundTag getSingleplayerData() {
      return null;
   }

   public void setAllowCommandsForAllPlayers(boolean p_335066_) {
      this.allowCommandsForAllPlayers = p_335066_;
   }

   public void removeAll() {
      for (int i = 0; i < this.players.size(); i++) {
         this.players.get(i).connection.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
      }
   }

   public void broadcastSystemMessage(Component p_240618_, boolean p_240644_) {
      this.broadcastSystemMessage(p_240618_, p_215639_ -> p_240618_, p_240644_);
   }

   public void broadcastSystemMessage(Component p_240526_, Function<ServerPlayer, Component> p_240594_, boolean p_240648_) {
      this.server.sendSystemMessage(p_240526_);

      for (ServerPlayer serverplayer : this.players) {
         Component component = p_240594_.apply(serverplayer);
         if (component != null) {
            serverplayer.sendSystemMessage(component, p_240648_);
         }
      }
   }

   public void broadcastChatMessage(PlayerChatMessage p_243229_, CommandSourceStack p_243254_, ChatType.Bound p_243255_) {
      this.broadcastChatMessage(p_243229_, p_243254_::shouldFilterMessageTo, p_243254_.getPlayer(), p_243255_);
   }

   public void broadcastChatMessage(PlayerChatMessage p_243264_, ServerPlayer p_243234_, ChatType.Bound p_243204_) {
      this.broadcastChatMessage(p_243264_, p_243234_::shouldFilterMessageTo, p_243234_, p_243204_);
   }

   private void broadcastChatMessage(PlayerChatMessage p_249952_, Predicate<ServerPlayer> p_250784_, @Nullable ServerPlayer p_249623_, ChatType.Bound p_250276_) {
      boolean flag = this.verifyChatTrusted(p_249952_);
      this.server.logChatMessage(p_249952_.decoratedContent(), p_250276_, flag ? null : "Not Secure");
      OutgoingChatMessage outgoingchatmessage = OutgoingChatMessage.create(p_249952_);
      boolean flag1 = false;

      for (ServerPlayer serverplayer : this.players) {
         boolean flag2 = p_250784_.test(serverplayer);
         serverplayer.sendChatMessage(outgoingchatmessage, flag2, p_250276_);
         flag1 |= flag2 && p_249952_.isFullyFiltered();
      }

      if (flag1 && p_249623_ != null) {
         p_249623_.sendSystemMessage(CHAT_FILTERED_FULL);
      }
   }

   private boolean verifyChatTrusted(PlayerChatMessage p_251384_) {
      return p_251384_.hasSignature() && !p_251384_.hasExpiredServer(Instant.now());
   }

   public ServerStatsCounter getPlayerStats(Player p_11240_) {
      GameProfile gameprofile = p_11240_.getGameProfile();
      return this.stats.computeIfAbsent(gameprofile.id(), p_449201_ -> {
         Path path = this.locateStatsFile(gameprofile);
         return new ServerStatsCounter(this.server, path);
      });
   }

   private Path locateStatsFile(GameProfile p_455756_) {
      Path path = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
      Path path1 = path.resolve(p_455756_.id() + ".json");
      if (Files.exists(path1)) {
         return path1;
      }

      String s = p_455756_.name() + ".json";
      if (FileUtil.isValidPathSegment(s)) {
         Path path2 = path.resolve(s);
         if (Files.isRegularFile(path2)) {
            try {
               return Files.move(path2, path1);
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to copy file {} to {}", s, path1);
               return path2;
            }
         }
      }

      return path1;
   }

   public PlayerAdvancements getPlayerAdvancements(ServerPlayer p_11297_) {
      UUID uuid = p_11297_.getUUID();
      PlayerAdvancements playeradvancements = this.advancements.get(uuid);
      if (playeradvancements == null) {
         Path path = this.server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
         playeradvancements = new PlayerAdvancements(this.server.getFixerUpper(), this, this.server.getAdvancements(), path, p_11297_);
         this.advancements.put(uuid, playeradvancements);
      }

      playeradvancements.setPlayer(p_11297_);
      return playeradvancements;
   }

   public void setViewDistance(int p_11218_) {
      this.viewDistance = p_11218_;
      this.broadcastAll(new ClientboundSetChunkCacheRadiusPacket(p_11218_));

      for (ServerLevel serverlevel : this.server.getAllLevels()) {
         serverlevel.getChunkSource().setViewDistance(p_11218_);
      }
   }

   public void setSimulationDistance(int p_184212_) {
      this.simulationDistance = p_184212_;
      this.broadcastAll(new ClientboundSetSimulationDistancePacket(p_184212_));

      for (ServerLevel serverlevel : this.server.getAllLevels()) {
         serverlevel.getChunkSource().setSimulationDistance(p_184212_);
      }
   }

   public List<ServerPlayer> getPlayers() {
      return this.players;
   }

   public @Nullable ServerPlayer getPlayer(UUID p_11260_) {
      return this.playersByUUID.get(p_11260_);
   }

   public @Nullable ServerPlayer getPlayer(String p_429821_) {
      for (ServerPlayer serverplayer : this.players) {
         if (serverplayer.getGameProfile().name().equalsIgnoreCase(p_429821_)) {
            return serverplayer;
         }
      }

      return null;
   }

   public boolean canBypassPlayerLimit(NameAndId p_428989_) {
      return false;
   }

   public void reloadResources() {
      for (PlayerAdvancements playeradvancements : this.advancements.values()) {
         playeradvancements.reload(this.server.getAdvancements());
      }

      this.broadcastAll(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(this.registries)));
      RecipeManager recipemanager = this.server.getRecipeManager();
      ClientboundUpdateRecipesPacket clientboundupdaterecipespacket = new ClientboundUpdateRecipesPacket(
         recipemanager.getSynchronizedItemProperties(), recipemanager.getSynchronizedStonecutterRecipes()
      );

      for (ServerPlayer serverplayer : this.players) {
         serverplayer.connection.send(clientboundupdaterecipespacket);
         serverplayer.getRecipeBook().sendInitialRecipeBook(serverplayer);
      }
   }

   public boolean isAllowCommandsForAllPlayers() {
      return this.allowCommandsForAllPlayers;
   }
}
