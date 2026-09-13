package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class PlaceOnWaterBlockItem extends BlockItem {
   public PlaceOnWaterBlockItem(Block p_220226_, Item.Properties p_220227_) {
      super(p_220226_, p_220227_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_220229_) {
      return InteractionResult.PASS;
   }

   @Override
   public InteractionResult use(Level p_220231_, Player p_220232_, InteractionHand p_220233_) {
      BlockHitResult blockhitresult = getPlayerPOVHitResult(p_220231_, p_220232_, ClipContext.Fluid.SOURCE_ONLY);
      BlockHitResult blockhitresult1 = blockhitresult.withPosition(blockhitresult.getBlockPos().above());
      return super.useOn(new UseOnContext(p_220232_, p_220233_, blockhitresult1));
   }
}
