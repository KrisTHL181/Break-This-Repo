package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SoundPreviewHandler {
   private static @Nullable SoundInstance activePreview;
   private static @Nullable SoundSource previousCategory;

   public static void preview(SoundManager p_426559_, SoundSource p_423593_, float p_429001_) {
      stopOtherCategoryPreview(p_426559_, p_423593_);
      if (canPlaySound(p_426559_)) {
         SoundEvent soundevent = switch (p_423593_) {
            case RECORDS -> (SoundEvent)SoundEvents.NOTE_BLOCK_GUITAR.value();
            case WEATHER -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case BLOCKS -> SoundEvents.GRASS_PLACE;
            case HOSTILE -> SoundEvents.ZOMBIE_AMBIENT;
            case NEUTRAL -> SoundEvents.COW_AMBIENT;
            case PLAYERS -> (SoundEvent)SoundEvents.GENERIC_EAT.value();
            case AMBIENT -> (SoundEvent)SoundEvents.AMBIENT_CAVE.value();
            case UI -> (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
            default -> SoundEvents.EMPTY;
         };
         if (soundevent != SoundEvents.EMPTY) {
            activePreview = SimpleSoundInstance.forUI(soundevent, 1.0F, p_429001_);
            p_426559_.play(activePreview);
         }
      }
   }

   private static void stopOtherCategoryPreview(SoundManager p_425183_, SoundSource p_428201_) {
      if (previousCategory != p_428201_) {
         previousCategory = p_428201_;
         if (activePreview != null) {
            p_425183_.stop(activePreview);
         }
      }
   }

   private static boolean canPlaySound(SoundManager p_430724_) {
      return activePreview == null || !p_430724_.isActive(activePreview);
   }
}
