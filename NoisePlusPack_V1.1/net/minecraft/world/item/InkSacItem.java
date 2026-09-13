package net.minecraft.world.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public class InkSacItem extends Item implements SignApplicator {
   public InkSacItem(Item.Properties p_277643_) {
      super(p_277643_);
   }

   @Override
   public boolean tryApplyToSign(Level p_277633_, SignBlockEntity p_277698_, boolean p_277634_, Player p_277815_) {
      if (p_277698_.updateText(p_277425_ -> p_277425_.setHasGlowingText(false), p_277634_)) {
         p_277633_.playSound(null, p_277698_.getBlockPos(), SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }
}
