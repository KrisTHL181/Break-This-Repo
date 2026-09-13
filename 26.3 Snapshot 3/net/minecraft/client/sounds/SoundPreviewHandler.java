package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.cow.CowSoundVariants;
import org.jspecify.annotations.Nullable;

public final class SoundPreviewHandler {
   private static @Nullable SoundInstance activePreview;
   private static @Nullable SoundSource previousCategory;

   public static void preview(final SoundManager soundManager, final SoundSource category, final float volume) {
      stopOtherCategoryPreview(soundManager, category);
      if (canPlaySound(soundManager)) {
         SoundEvent previewSound = switch (category) {
            case RECORDS -> (SoundEvent)SoundEvents.NOTE_BLOCK_GUITAR.value();
            case WEATHER -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case BLOCKS -> SoundEvents.GRASS_PLACE;
            case HOSTILE -> SoundEvents.ZOMBIE_AMBIENT;
            case NEUTRAL -> (SoundEvent)SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.CLASSIC).ambientSound().value();
            case PLAYERS -> (SoundEvent)SoundEvents.GENERIC_EAT.value();
            case AMBIENT -> (SoundEvent)SoundEvents.AMBIENT_CAVE.value();
            case UI -> (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
            default -> SoundEvents.EMPTY;
         };
         if (previewSound != SoundEvents.EMPTY) {
            activePreview = SimpleSoundInstance.forUI(previewSound, 1.0F, volume);
            soundManager.play(activePreview);
         }
      }
   }

   private static void stopOtherCategoryPreview(final SoundManager soundManager, final SoundSource category) {
      if (previousCategory != category) {
         previousCategory = category;
         if (activePreview != null) {
            soundManager.stop(activePreview);
         }
      }
   }

   private static boolean canPlaySound(final SoundManager soundManager) {
      return activePreview == null || !soundManager.isActive(activePreview);
   }
}
