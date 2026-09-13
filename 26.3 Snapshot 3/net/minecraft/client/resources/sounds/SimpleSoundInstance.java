package net.minecraft.client.resources.sounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class SimpleSoundInstance extends AbstractSoundInstance {
   public SimpleSoundInstance(
      final SoundEvent sound, final SoundSource source, final float volume, final float pitch, final RandomSource random, final BlockPos pos
   ) {
      this(sound, source, volume, pitch, random, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   public static SimpleSoundInstance forUI(final SoundEvent sound, final float pitch) {
      return forUI(sound, pitch, 0.25F);
   }

   public static SimpleSoundInstance forUI(final Holder<SoundEvent> sound, final float pitch) {
      return forUI(sound.value(), pitch);
   }

   public static SimpleSoundInstance forUI(final SoundEvent sound, final float pitch, final float volume) {
      return new SimpleSoundInstance(
         sound.location(), SoundSource.UI, volume, pitch, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true
      );
   }

   public static SimpleSoundInstance forMusic(final SoundEvent sound) {
      return new SimpleSoundInstance(
         sound.location(), SoundSource.MUSIC, 1.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true
      );
   }

   public static SimpleSoundInstance forJukeboxSong(final SoundEvent sound, final Vec3 pos) {
      return new SimpleSoundInstance(
         sound, SoundSource.RECORDS, 4.0F, 1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.LINEAR, pos.x, pos.y, pos.z
      );
   }

   public static SimpleSoundInstance forLocalAmbience(final SoundEvent sound, final float pitch, final float volume) {
      return new SimpleSoundInstance(
         sound.location(),
         SoundSource.AMBIENT,
         volume,
         pitch,
         SoundInstance.createUnseededRandom(),
         false,
         0,
         SoundInstance.Attenuation.NONE,
         0.0,
         0.0,
         0.0,
         true
      );
   }

   public static SimpleSoundInstance forAmbientAddition(final SoundEvent sound) {
      return forLocalAmbience(sound, 1.0F, 1.0F);
   }

   public static SimpleSoundInstance forAmbientMood(final SoundEvent sound, final RandomSource random, final double x, final double y, final double z) {
      return new SimpleSoundInstance(sound, SoundSource.AMBIENT, 1.0F, 1.0F, random, false, 0, SoundInstance.Attenuation.LINEAR, x, y, z);
   }

   public SimpleSoundInstance(
      final SoundEvent sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final RandomSource random,
      final double x,
      final double y,
      final double z
   ) {
      this(sound, source, volume, pitch, random, false, 0, SoundInstance.Attenuation.LINEAR, x, y, z);
   }

   private SimpleSoundInstance(
      final SoundEvent sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final RandomSource random,
      final boolean looping,
      final int delay,
      final SoundInstance.Attenuation attenuation,
      final double x,
      final double y,
      final double z
   ) {
      this(sound.location(), source, volume, pitch, random, looping, delay, attenuation, x, y, z, false);
   }

   public SimpleSoundInstance(
      final Identifier location,
      final SoundSource source,
      final float volume,
      final float pitch,
      final RandomSource random,
      final boolean looping,
      final int delay,
      final SoundInstance.Attenuation attenuation,
      final double x,
      final double y,
      final double z,
      final boolean relative
   ) {
      super(location, source, random);
      this.volume = volume;
      this.pitch = pitch;
      this.x = x;
      this.y = y;
      this.z = z;
      this.looping = looping;
      this.delay = delay;
      this.attenuation = attenuation;
      this.relative = relative;
   }
}
