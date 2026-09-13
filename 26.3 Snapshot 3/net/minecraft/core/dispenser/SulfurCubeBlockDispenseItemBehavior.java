package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class SulfurCubeBlockDispenseItemBehavior extends DefaultDispenseItemBehavior {
   public static final SulfurCubeBlockDispenseItemBehavior INSTANCE = new SulfurCubeBlockDispenseItemBehavior();

   @Override
   protected ItemStack execute(final BlockSource source, final ItemStack dispensed) {
      return dispenseBlock(source.level(), source.pos().relative(source.state().getValue(DispenserBlock.FACING)), dispensed)
         ? dispensed
         : super.execute(source, dispensed);
   }

   public static boolean dispenseBlock(final ServerLevel level, final BlockPos pos, final ItemStack dispensed) {
      for (SulfurCube entity : level.getEntitiesOfClass(SulfurCube.class, new AABB(pos))) {
         if (entity.equipItem(dispensed)) {
            dispensed.shrink(1);
            return true;
         }
      }

      return false;
   }
}
