package net.minecraft.world.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public interface SignApplicator {
   boolean tryApplyToSign(Level var1, SignBlockEntity var2, boolean var3, Player var4);

   default boolean canApplyToSign(SignText p_278084_, Player p_277515_) {
      return p_278084_.hasMessage(p_277515_);
   }
}
