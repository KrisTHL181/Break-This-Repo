package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class JukeboxSongPlayer {
   public static final int PLAY_EVENT_INTERVAL_TICKS = 20;
   private long ticksSinceSongStarted;
   private @Nullable Holder<JukeboxSong> song;
   private final BlockPos blockPos;
   private final JukeboxSongPlayer.OnSongChanged onSongChanged;

   public JukeboxSongPlayer(final JukeboxSongPlayer.OnSongChanged onSongChanged, final BlockPos blockPos) {
      this.onSongChanged = onSongChanged;
      this.blockPos = blockPos;
   }

   public boolean isPlaying() {
      return this.song != null;
   }

   public @Nullable JukeboxSong getSong() {
      return this.song == null ? null : this.song.value();
   }

   public long getTicksSinceSongStarted() {
      return this.ticksSinceSongStarted;
   }

   public void setSongWithoutPlaying(final Holder<JukeboxSong> song, final long ticksSinceSongStarted) {
      if (!song.value().hasFinished(ticksSinceSongStarted)) {
         this.song = song;
         this.ticksSinceSongStarted = ticksSinceSongStarted;
      }
   }

   public void play(final LevelAccessor level, final Holder<JukeboxSong> song) {
      this.song = song;
      this.ticksSinceSongStarted = 0L;
      int songId = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(this.song.value());
      level.levelEvent(null, 1010, this.blockPos, songId);
      this.onSongChanged.notifyChange();
   }

   public void stop(final LevelAccessor level, final @Nullable BlockState blockState) {
      if (this.song != null) {
         this.song = null;
         this.ticksSinceSongStarted = 0L;
         level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.blockPos, GameEvent.Context.of(blockState));
         level.levelEvent(1011, this.blockPos, 0);
         this.onSongChanged.notifyChange();
      }
   }

   public void tick(final LevelAccessor level, final @Nullable BlockState blockState) {
      if (this.song != null) {
         if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
            this.stop(level, blockState);
         } else {
            if (this.shouldEmitJukeboxPlayingEvent()) {
               level.gameEvent(GameEvent.JUKEBOX_PLAY, this.blockPos, GameEvent.Context.of(blockState));
               spawnMusicParticles(level, this.blockPos);
            }

            this.ticksSinceSongStarted++;
         }
      }
   }

   private boolean shouldEmitJukeboxPlayingEvent() {
      return this.ticksSinceSongStarted % 20L == 0L;
   }

   private static void spawnMusicParticles(final LevelAccessor level, final BlockPos blockPos) {
      if (level instanceof ServerLevel serverLevel) {
         Vec3 pos = Vec3.atBottomCenterOf(blockPos).add(0.0, 1.2F, 0.0);
         float randomColor = level.getRandom().nextInt(4) / 24.0F;
         serverLevel.sendParticles(ParticleTypes.NOTE, pos.x(), pos.y(), pos.z(), 0, randomColor, 0.0, 0.0, 1.0);
      }
   }

   @FunctionalInterface
   public interface OnSongChanged {
      void notifyChange();
   }
}
