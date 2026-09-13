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

public interface LevelAccessor extends CommonLevelAccessor, LevelReader, ScheduledTickAccess {
   long nextSubTickCount();

   @Override
   default <T> ScheduledTick<T> createTick(BlockPos p_186483_, T p_186484_, int p_186485_, TickPriority p_186486_) {
      return new ScheduledTick<>(p_186484_, p_186483_, this.getGameTime() + p_186485_, p_186486_, this.nextSubTickCount());
   }

   @Override
   default <T> ScheduledTick<T> createTick(BlockPos p_186479_, T p_186480_, int p_186481_) {
      return new ScheduledTick<>(p_186480_, p_186479_, this.getGameTime() + p_186481_, this.nextSubTickCount());
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
   default boolean hasChunk(int p_46794_, int p_46795_) {
      return this.getChunkSource().hasChunk(p_46794_, p_46795_);
   }

   RandomSource getRandom();

   default void updateNeighborsAt(BlockPos p_46781_, Block p_46782_) {
   }

   default void neighborShapeChanged(
      Direction p_220411_, BlockPos p_220413_, BlockPos p_220414_, BlockState p_220412_, @Block.UpdateFlags int p_220415_, int p_220416_
   ) {
      NeighborUpdater.executeShapeUpdate(this, p_220411_, p_220413_, p_220414_, p_220412_, p_220415_, p_220416_ - 1);
   }

   default void playSound(@Nullable Entity p_397377_, BlockPos p_250192_, SoundEvent p_249887_, SoundSource p_250593_) {
      this.playSound(p_397377_, p_250192_, p_249887_, p_250593_, 1.0F, 1.0F);
   }

   void playSound(@Nullable Entity var1, BlockPos var2, SoundEvent var3, SoundSource var4, float var5, float var6);

   void addParticle(ParticleOptions var1, double var2, double var4, double var6, double var8, double var10, double var12);

   void levelEvent(@Nullable Entity var1, int var2, BlockPos var3, int var4);

   default void levelEvent(int p_46797_, BlockPos p_46798_, int p_46799_) {
      this.levelEvent(null, p_46797_, p_46798_, p_46799_);
   }

   void gameEvent(Holder<GameEvent> var1, Vec3 var2, GameEvent.Context var3);

   default void gameEvent(@Nullable Entity p_220401_, Holder<GameEvent> p_334728_, Vec3 p_220403_) {
      this.gameEvent(p_334728_, p_220403_, new GameEvent.Context(p_220401_, null));
   }

   default void gameEvent(@Nullable Entity p_151549_, Holder<GameEvent> p_331725_, BlockPos p_151551_) {
      this.gameEvent(p_331725_, p_151551_, new GameEvent.Context(p_151549_, null));
   }

   default void gameEvent(Holder<GameEvent> p_333522_, BlockPos p_334961_, GameEvent.Context p_335744_) {
      this.gameEvent(p_333522_, Vec3.atCenterOf(p_334961_), p_335744_);
   }

   default void gameEvent(ResourceKey<GameEvent> p_332741_, BlockPos p_220409_, GameEvent.Context p_220410_) {
      this.gameEvent(this.registryAccess().lookupOrThrow(Registries.GAME_EVENT).getOrThrow(p_332741_), p_220409_, p_220410_);
   }
}
