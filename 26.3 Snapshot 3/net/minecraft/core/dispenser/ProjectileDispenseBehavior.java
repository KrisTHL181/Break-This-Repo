package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.block.DispenserBlock;

public class ProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   private final ProjectileItem projectileItem;
   private final ProjectileItem.DispenseConfig dispenseConfig;

   public ProjectileDispenseBehavior(final Item item) {
      if (item instanceof ProjectileItem projectileItem) {
         this.projectileItem = projectileItem;
         this.dispenseConfig = projectileItem.createDispenseConfig();
      } else {
         throw new IllegalArgumentException(item + " not instance of " + ProjectileItem.class.getSimpleName());
      }
   }

   @Override
   public ItemStack execute(final BlockSource source, final ItemStack dispensed) {
      ServerLevel level = source.level();
      Direction direction = source.state().getValue(DispenserBlock.FACING);
      Position position = this.dispenseConfig.positionFunction().getDispensePosition(source, direction);
      Projectile.spawnProjectileUsingShoot(
         this.projectileItem.asProjectile(level, position, dispensed, direction),
         level,
         dispensed,
         direction.getStepX(),
         direction.getStepY(),
         direction.getStepZ(),
         this.dispenseConfig.power(),
         this.dispenseConfig.uncertainty()
      );
      dispensed.shrink(1);
      return dispensed;
   }

   @Override
   protected void playSound(final BlockSource source) {
      source.level().levelEvent(this.dispenseConfig.overrideDispenseEvent().orElse(1002), source.pos(), 0);
   }
}
