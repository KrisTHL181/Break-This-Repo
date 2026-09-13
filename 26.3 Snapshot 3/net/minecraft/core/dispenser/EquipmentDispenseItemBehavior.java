package net.minecraft.core.dispenser;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

public class EquipmentDispenseItemBehavior extends DefaultDispenseItemBehavior {
   public static final EquipmentDispenseItemBehavior INSTANCE = new EquipmentDispenseItemBehavior();

   @Override
   protected ItemStack execute(final BlockSource source, final ItemStack dispensed) {
      return dispenseEquipment(source, dispensed) ? dispensed : super.execute(source, dispensed);
   }

   public static boolean dispenseEquipment(final BlockSource source, final ItemStack dispensed) {
      BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
      List<LivingEntity> entities = source.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos), entity -> entity.canEquipWithDispenser(dispensed));
      if (entities.isEmpty()) {
         return false;
      }

      LivingEntity target = entities.getFirst();
      EquipmentSlot slot = target.getEquipmentSlotForItem(dispensed);
      ItemStack equip = dispensed.split(1);
      target.setItemSlot(slot, equip);
      if (target instanceof Mob targetMob) {
         targetMob.setGuaranteedDrop(slot);
         targetMob.setPersistenceRequired();
      }

      return true;
   }
}
