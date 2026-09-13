package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;

public class SpawnEggItemBehavior extends DefaultDispenseItemBehavior {
   public static final SpawnEggItemBehavior INSTANCE = new SpawnEggItemBehavior();

   @Override
   public ItemStack execute(final BlockSource source, final ItemStack dispensed) {
      Direction direction = source.state().getValue(DispenserBlock.FACING);
      EntityType<?> type = SpawnEggItem.getType(dispensed);
      if (type == null) {
         return dispensed;
      }

      try {
         Entity spawned = type.spawn(
            source.level(), dispensed, null, source.pos().relative(direction), EntitySpawnReason.DISPENSER, direction != Direction.UP, false
         );
         if (spawned == null) {
            return dispensed;
         }
      } catch (Exception e) {
         LOGGER.error("Error while dispensing spawn egg from dispenser at {}", source.pos(), e);
         return ItemStack.EMPTY;
      }

      dispensed.shrink(1);
      source.level().gameEvent(null, GameEvent.ENTITY_PLACE, source.pos());
      return dispensed;
   }
}
