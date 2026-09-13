package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item {
   private final EntityType<T> canInteractWith;
   private final int consumeItemDamage;

   public FoodOnAStickItem(final EntityType<T> canInteractWith, final int consumeItemDamage, final Item.Properties properties) {
      super(properties);
      this.canInteractWith = canInteractWith;
      this.consumeItemDamage = consumeItemDamage;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (level.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         Entity vehicle = player.getControlledVehicle();
         if (player.isPassenger() && vehicle instanceof ItemSteerable steerable && vehicle.is(this.canInteractWith) && steerable.boost()) {
            EquipmentSlot slot = hand.asEquipmentSlot();
            ItemStack result = itemStack.hurtAndConvertOnBreak(this.consumeItemDamage, Items.FISHING_ROD, player, slot);
            return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(result);
         } else {
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.PASS;
         }
      }
   }
}
