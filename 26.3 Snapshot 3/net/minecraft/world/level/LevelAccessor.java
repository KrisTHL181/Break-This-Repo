package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.jspecify.annotations.Nullable;

public interface LevelAccessor extends CommonLevelAccessor, ScheduledTickAccess {
   long nextSubTickCount();

   @Override
   default <T> ScheduledTick<T> createTick(final BlockPos pos, final T type, final int tickDelay, final TickPriority priority) {
      return new ScheduledTick<>(type, pos, this.getGameTime() + tickDelay, priority, this.nextSubTickCount());
   }

   @Override
   default <T> ScheduledTick<T> createTick(final BlockPos pos, final T type, final int tickDelay) {
      return new ScheduledTick<>(type, pos, this.getGameTime() + tickDelay, this.nextSubTickCount());
   }

   LevelData getLevelData();

   default long getGameTime() {
      return this.getLevelData().getGameTime();
   }

   @Nullable MinecraftServer getServer();

   default Difficulty getDifficulty() {
      return this.getLevelData().getDifficulty();
   }

   ChunkSource getChunkSource();

   @Override
   default boolean hasChunk(final int chunkX, final int chunkZ) {
      return this.getChunkSource().hasChunk(chunkX, chunkZ);
   }

   RandomSource getRandom();

   default void updateNeighborsAt(final BlockPos pos, final Block sourceBlock) {
   }

   default void neighborShapeChanged(
      final Direction direction,
      final BlockPos pos,
      final BlockPos neighborPos,
      final BlockState neighborState,
      final @Block.UpdateFlags int updateFlags,
      final int updateLimit
   ) {
      NeighborUpdater.executeShapeUpdate(this, direction, pos, neighborPos, neighborState, updateFlags, updateLimit - 1);
   }

   default void playSound(final @Nullable Entity except, final BlockPos pos, final SoundEvent soundEvent, final SoundSource source) {
      this.playSound(except, pos, soundEvent, source, 1.0F, 1.0F);
   }

   void playSound(final @Nullable Entity except, final BlockPos pos, final SoundEvent sound, final SoundSource source, final float volume, final float pitch);

   void addParticle(final ParticleOptions particle, final double x, final double y, final double z, final double xd, final double yd, final double zd);

   void levelEvent(final @Nullable Entity source, final int type, final BlockPos pos, final int data);

   default void levelEvent(final int type, final BlockPos pos, final int data) {
      this.levelEvent(null, type, pos, data);
   }

   void gameEvent(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context);

   default void gameEvent(final @Nullable Entity sourceEntity, final Holder<GameEvent> gameEvent, final Vec3 pos) {
      this.gameEvent(gameEvent, pos, new GameEvent.Context(sourceEntity, null));
   }

   default void gameEvent(final @Nullable Entity sourceEntity, final Holder<GameEvent> gameEvent, final BlockPos pos) {
      this.gameEvent(gameEvent, pos, new GameEvent.Context(sourceEntity, null));
   }

   default void gameEvent(final Holder<GameEvent> gameEvent, final BlockPos pos, final GameEvent.Context context) {
      this.gameEvent(gameEvent, Vec3.atCenterOf(pos), context);
   }

   default void gameEvent(final ResourceKey<GameEvent> gameEvent, final BlockPos pos, final GameEvent.Context context) {
      this.gameEvent(this.registryAccess().lookupOrThrow(Registries.GAME_EVENT).getOrThrow(gameEvent), pos, context);
   }
}
