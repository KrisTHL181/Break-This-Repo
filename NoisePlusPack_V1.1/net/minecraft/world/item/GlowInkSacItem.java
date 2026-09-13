package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class GlowInkSacItem extends Item implements SignApplicator {
   public GlowInkSacItem(Item.Properties p_277648_) {
      super(p_277648_);
   }

   @Override
   public boolean tryApplyToSign(Level p_278089_, SignBlockEntity p_277706_, boolean p_277442_, Player p_277983_) {
      if (p_277706_.updateText(p_277781_ -> p_277781_.setHasGlowingText(true), p_277442_)) {
         p_278089_.playSound(null, p_277706_.getBlockPos(), SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }
}
