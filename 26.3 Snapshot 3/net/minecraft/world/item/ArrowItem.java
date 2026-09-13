package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ArrowItem extends Item implements ProjectileItem {
   public ArrowItem(final Item.Properties properties) {
      super(properties);
   }

   public AbstractArrow createArrow(final Level level, final ItemStack itemStack, final LivingEntity owner, final @Nullable ItemStack firedFromWeapon) {
      return new Arrow(level, owner, itemStack.copyWithCount(1), firedFromWeapon);
   }

   @Override
   public Projectile asProjectile(final Level level, final Position position, final ItemStack itemStack, final Direction direction) {
      Arrow arrow = new Arrow(level, position.x(), position.y(), position.z(), itemStack.copyWithCount(1), null);
      arrow.pickup = AbstractArrow.Pickup.ALLOWED;
      return arrow;
   }
}
