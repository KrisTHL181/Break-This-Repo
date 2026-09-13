package net.minecraft.client.resources.sounds;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.AmbientAdditionsSettings;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.jspecify.annotations.Nullable;

public class BiomeAmbientSoundsHandler implements AmbientSoundHandler {
   private static final int LOOP_SOUND_CROSS_FADE_TIME = 40;
   private static final float SKY_MOOD_RECOVERY_RATE = 0.001F;
   private final LocalPlayer player;
   private final SoundManager soundManager;
   private final RandomSource random;
   private final Object2ObjectArrayMap<Holder<SoundEvent>, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds = new Object2ObjectArrayMap();
   private float moodiness;
   private @Nullable Holder<SoundEvent> previousLoopSound;

   public BiomeAmbientSoundsHandler(final LocalPlayer player, final SoundManager soundManager) {
      this.random = player.level().getRandom();
      this.player = player;
      this.soundManager = soundManager;
   }

   public float getMoodiness() {
      return this.moodiness;
   }

   @Override
   public void tick() {
      this.loopSounds.values().removeIf(AbstractTickableSoundInstance::isStopped);
      Level level = this.player.level();
      EnvironmentAttributeSystem environmentAttributes = level.environmentAttributes();
      AmbientSounds ambientSounds = environmentAttributes.getValue(EnvironmentAttributes.AMBIENT_SOUNDS, this.player.position());
      Holder<SoundEvent> currentLoopSound = ambientSounds.loop().orElse(null);
      if (!Objects.equals(currentLoopSound, this.previousLoopSound)) {
         this.previousLoopSound = currentLoopSound;
         this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
         if (currentLoopSound != null) {
            this.loopSounds.compute(currentLoopSound, (biomeKey, soundInstance) -> {
               if (soundInstance == null) {
                  soundInstance = new BiomeAmbientSoundsHandler.LoopSoundInstance(currentLoopSound.value());
                  this.soundManager.play(soundInstance);
               }

               soundInstance.fadeIn();
               return soundInstance;
            });
         }
      }

      for (AmbientAdditionsSettings additions : ambientSounds.additions()) {
         if (this.random.nextDouble() < additions.tickChance()) {
            this.soundManager.play(SimpleSoundInstance.forAmbientAddition(additions.soundEvent().value()));
         }
      }

      ambientSounds.mood()
         .ifPresent(
            mood -> {
               int searchSpan = mood.blockSearchExtent() * 2 + 1;
               BlockPos blockSamplingPos = BlockPos.containing(
                  this.player.getX() + this.random.nextInt(searchSpan) - mood.blockSearchExtent(),
                  this.player.getEyeY() + this.random.nextInt(searchSpan) - mood.blockSearchExtent(),
                  this.player.getZ() + this.random.nextInt(searchSpan) - mood.blockSearchExtent()
               );
               int skyBrightness = level.getBrightness(LightLayer.SKY, blockSamplingPos);
               if (skyBrightness > 0) {
                  this.moodiness -= skyBrightness / 15.0F * 0.001F;
               } else {
                  this.moodiness = this.moodiness - (float)(level.getBrightness(LightLayer.BLOCK, blockSamplingPos) - 1) / mood.tickDelay();
               }

               if (this.moodiness >= 1.0F) {
                  double blockSampleX = blockSamplingPos.getX() + 0.5;
                  double blockSampleY = blockSamplingPos.getY() + 0.5;
                  double blockSampleZ = blockSamplingPos.getZ() + 0.5;
                  double blockDirectionX = blockSampleX - this.player.getX();
                  double blockDirectionY = blockSampleY - this.player.getEyeY();
                  double blockDirectionZ = blockSampleZ - this.player.getZ();
                  double blockDistance = Math.sqrt(blockDirectionX * blockDirectionX + blockDirectionY * blockDirectionY + blockDirectionZ * blockDirectionZ);
                  double soundSourceDistance = blockDistance + mood.soundPositionOffset();
                  SimpleSoundInstance moodSoundInstance = SimpleSoundInstance.forAmbientMood(
                     mood.soundEvent().value(),
                     this.random,
                     this.player.getX() + blockDirectionX / blockDistance * soundSourceDistance,
                     this.player.getEyeY() + blockDirectionY / blockDistance * soundSourceDistance,
                     this.player.getZ() + blockDirectionZ / blockDistance * soundSourceDistance
                  );
                  this.soundManager.play(moodSoundInstance);
                  this.moodiness = 0.0F;
               } else {
                  this.moodiness = Math.max(this.moodiness, 0.0F);
               }
            }
         );
   }

   public static class LoopSoundInstance extends AbstractTickableSoundInstance {
      private int fadeDirection;
      private int fade;

      public LoopSoundInstance(final SoundEvent soundEvent) {
         super(soundEvent, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
         this.looping = true;
         this.delay = 0;
         this.volume = 1.0F;
         this.relative = true;
      }

      @Override
      public void tick() {
         if (this.fade < 0) {
            this.stop();
         }

         this.fade = this.fade + this.fadeDirection;
         this.volume = Mth.clamp(this.fade / 40.0F, 0.0F, 1.0F);
      }

      public void fadeOut() {
         this.fade = Math.min(this.fade, 40);
         this.fadeDirection = -1;
      }

      public void fadeIn() {
         this.fade = Math.max(0, this.fade);
         this.fadeDirection = 1;
      }
   }
}
