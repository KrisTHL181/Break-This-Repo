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

   public JukeboxSongPlayer(JukeboxSongPlayer.OnSongChanged p_342806_, BlockPos p_342798_) {
      this.onSongChanged = p_342806_;
      this.blockPos = p_342798_;
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

   public void setSongWithoutPlaying(Holder<JukeboxSong> p_343041_, long p_342718_) {
      if (!p_343041_.value().hasFinished(p_342718_)) {
         this.song = p_343041_;
         this.ticksSinceSongStarted = p_342718_;
      }
   }

   public void play(LevelAccessor p_342919_, Holder<JukeboxSong> p_342120_) {
      this.song = p_342120_;
      this.ticksSinceSongStarted = 0L;
      int i = p_342919_.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(this.song.value());
      p_342919_.levelEvent(null, 1010, this.blockPos, i);
      this.onSongChanged.notifyChange();
   }

   public void stop(LevelAccessor p_342211_, @Nullable BlockState p_342866_) {
      if (this.song != null) {
         this.song = null;
         this.ticksSinceSongStarted = 0L;
         p_342211_.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.blockPos, GameEvent.Context.of(p_342866_));
         p_342211_.levelEvent(1011, this.blockPos, 0);
         this.onSongChanged.notifyChange();
      }
   }

   public void tick(LevelAccessor p_345493_, @Nullable BlockState p_344954_) {
      if (this.song != null) {
         if (this.song.value().hasFinished(this.ticksSinceSongStarted)) {
            this.stop(p_345493_, p_344954_);
         } else {
            if (this.shouldEmitJukeboxPlayingEvent()) {
               p_345493_.gameEvent(GameEvent.JUKEBOX_PLAY, this.blockPos, GameEvent.Context.of(p_344954_));
               spawnMusicParticles(p_345493_, this.blockPos);
            }

            this.ticksSinceSongStarted++;
         }
      }
   }

   private boolean shouldEmitJukeboxPlayingEvent() {
      return this.ticksSinceSongStarted % 20L == 0L;
   }

   private static void spawnMusicParticles(LevelAccessor p_343992_, BlockPos p_342425_) {
      if (p_343992_ instanceof ServerLevel serverlevel) {
         Vec3 vec3 = Vec3.atBottomCenterOf(p_342425_).add(0.0, 1.2F, 0.0);
         float f = p_343992_.getRandom().nextInt(4) / 24.0F;
         serverlevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, f, 0.0, 0.0, 1.0);
      }
   }

   @FunctionalInterface
   public interface OnSongChanged {
      void notifyChange();
   }
}
