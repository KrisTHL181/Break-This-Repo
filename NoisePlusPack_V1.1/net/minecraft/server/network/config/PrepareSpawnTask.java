package net.minecraft.server.network.config;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PrepareSpawnTask implements ConfigurationTask {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type("prepare_spawn");
   public static final int PREPARE_CHUNK_RADIUS = 3;
   final MinecraftServer server;
   final NameAndId nameAndId;
   final LevelLoadListener loadListener;
   private PrepareSpawnTask.@Nullable State state;

   public PrepareSpawnTask(MinecraftServer p_422620_, NameAndId p_426998_) {
      this.server = p_422620_;
      this.nameAndId = p_426998_;
      this.loadListener = p_422620_.getLevelLoadListener();
   }

   @Override
   public void start(Consumer<Packet<?>> p_423242_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
         Optional<ValueInput> optional = this.server
            .getPlayerList()
            .loadPlayerData(this.nameAndId)
            .map(p_431055_ -> TagValueInput.create(problemreporter$scopedcollector, this.server.registryAccess(), p_431055_));
         ServerPlayer.SavedPosition serverplayer$savedposition = optional.<ServerPlayer.SavedPosition>flatMap(
               p_428022_ -> p_428022_.read(ServerPlayer.SavedPosition.MAP_CODEC)
            )
            .orElse(ServerPlayer.SavedPosition.EMPTY);
         LevelData.RespawnData leveldata$respawndata = this.server.getWorldData().overworldData().getRespawnData();
         ServerLevel serverlevel = serverplayer$savedposition.dimension().map(this.server::getLevel).orElseGet(() -> {
            ServerLevel serverlevel1 = this.server.getLevel(leveldata$respawndata.dimension());
            return serverlevel1 != null ? serverlevel1 : this.server.overworld();
         });
         CompletableFuture<Vec3> completablefuture = serverplayer$savedposition.position()
            .map(CompletableFuture::completedFuture)
            .orElseGet(() -> PlayerSpawnFinder.findSpawn(serverlevel, leveldata$respawndata.pos()));
         Vec2 vec2 = serverplayer$savedposition.rotation().orElse(new Vec2(leveldata$respawndata.yaw(), leveldata$respawndata.pitch()));
         this.state = new PrepareSpawnTask.Preparing(serverlevel, completablefuture, vec2);
      }
   }

   @Override
   public boolean tick() {
      return switch (this.state) {
         case null -> false;
         case PrepareSpawnTask.Preparing preparespawntask$preparing -> {
            PrepareSpawnTask.Ready preparespawntask$ready1 = preparespawntask$preparing.tick();
            if (preparespawntask$ready1 != null) {
               this.state = preparespawntask$ready1;
               yield true;
            } else {
               yield false;
            }
         }
         case PrepareSpawnTask.Ready preparespawntask$ready -> true;
         default -> throw new MatchException(null, null);
      };
   }

   public ServerPlayer spawnPlayer(Connection p_427518_, CommonListenerCookie p_427275_) {
      if (this.state instanceof PrepareSpawnTask.Ready preparespawntask$ready) {
         return preparespawntask$ready.spawn(p_427518_, p_427275_);
      } else {
         throw new IllegalStateException("Player spawn was not ready");
      }
   }

   public void keepAlive() {
      if (this.state instanceof PrepareSpawnTask.Ready preparespawntask$ready) {
         preparespawntask$ready.keepAlive();
      }
   }

   public void close() {
      if (this.state instanceof PrepareSpawnTask.Preparing preparespawntask$preparing) {
         preparespawntask$preparing.cancel();
      }

      this.state = null;
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }

   final class Preparing implements PrepareSpawnTask.State {
      private final ServerLevel spawnLevel;
      private final CompletableFuture<Vec3> spawnPosition;
      private final Vec2 spawnAngle;
      private @Nullable CompletableFuture<?> chunkLoadFuture;
      private final ChunkLoadCounter chunkLoadCounter = new ChunkLoadCounter();

      Preparing(final ServerLevel p_430479_, final CompletableFuture<Vec3> p_429688_, final Vec2 p_430859_) {
         this.spawnLevel = p_430479_;
         this.spawnPosition = p_429688_;
         this.spawnAngle = p_430859_;
      }

      public void cancel() {
         this.spawnPosition.cancel(false);
      }

      public PrepareSpawnTask.@Nullable Ready tick() {
         if (!this.spawnPosition.isDone()) {
            return null;
         }

         Vec3 vec3 = this.spawnPosition.join();
         if (this.chunkLoadFuture == null) {
            ChunkPos chunkpos = new ChunkPos(BlockPos.containing(vec3));
            this.chunkLoadCounter
               .track(
                  this.spawnLevel,
                  () -> this.chunkLoadFuture = this.spawnLevel.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN, chunkpos, 3)
               );
            PrepareSpawnTask.this.loadListener.start(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS, this.chunkLoadCounter.totalChunks());
            PrepareSpawnTask.this.loadListener.updateFocus(this.spawnLevel.dimension(), chunkpos);
         }

         PrepareSpawnTask.this.loadListener
            .update(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS, this.chunkLoadCounter.readyChunks(), this.chunkLoadCounter.totalChunks());
         if (!this.chunkLoadFuture.isDone()) {
            return null;
         }

         PrepareSpawnTask.this.loadListener.finish(LevelLoadListener.Stage.LOAD_PLAYER_CHUNKS);
         return PrepareSpawnTask.this.new Ready(this.spawnLevel, vec3, this.spawnAngle);
      }
   }

   final class Ready implements PrepareSpawnTask.State {
      private final ServerLevel spawnLevel;
      private final Vec3 spawnPosition;
      private final Vec2 spawnAngle;

      Ready(final ServerLevel p_424619_, final Vec3 p_423464_, final Vec2 p_431046_) {
         this.spawnLevel = p_424619_;
         this.spawnPosition = p_423464_;
         this.spawnAngle = p_431046_;
      }

      public void keepAlive() {
         this.spawnLevel.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN, new ChunkPos(BlockPos.containing(this.spawnPosition)), 3);
      }

      public ServerPlayer spawn(Connection p_429818_, CommonListenerCookie p_430225_) {
         ChunkPos chunkpos = new ChunkPos(BlockPos.containing(this.spawnPosition));
         this.spawnLevel.waitForEntities(chunkpos, 3);
         ServerPlayer serverplayer = new ServerPlayer(PrepareSpawnTask.this.server, this.spawnLevel, p_430225_.gameProfile(), p_430225_.clientInformation());

         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(
               serverplayer.problemPath(), PrepareSpawnTask.LOGGER
            )) {
            Optional<ValueInput> optional = PrepareSpawnTask.this.server
               .getPlayerList()
               .loadPlayerData(PrepareSpawnTask.this.nameAndId)
               .map(p_423730_ -> TagValueInput.create(problemreporter$scopedcollector, PrepareSpawnTask.this.server.registryAccess(), p_423730_));
            optional.ifPresent(serverplayer::load);
            serverplayer.snapTo(this.spawnPosition, this.spawnAngle.x, this.spawnAngle.y);
            PrepareSpawnTask.this.server.getPlayerList().placeNewPlayer(p_429818_, serverplayer, p_430225_);
            optional.ifPresent(p_422988_ -> {
               serverplayer.loadAndSpawnEnderPearls(p_422988_);
               serverplayer.loadAndSpawnParentVehicle(p_422988_);
            });
            return serverplayer;
         }
      }
   }

   sealed interface State permits PrepareSpawnTask.Preparing, PrepareSpawnTask.Ready {
   }
}
