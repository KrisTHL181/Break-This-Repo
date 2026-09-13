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
   public ArrowItem(Item.Properties p_40512_) {
      super(p_40512_);
   }

   public AbstractArrow createArrow(Level p_40513_, ItemStack p_40514_, LivingEntity p_40515_, @Nullable ItemStack p_343176_) {
      return new Arrow(p_40513_, p_40515_, p_40514_.copyWithCount(1), p_343176_);
   }

   @Override
   public Projectile asProjectile(Level p_330586_, Position p_330823_, ItemStack p_335593_, Direction p_335240_) {
      Arrow arrow = new Arrow(p_330586_, p_330823_.x(), p_330823_.y(), p_330823_.z(), p_335593_.copyWithCount(1), null);
      arrow.pickup = AbstractArrow.Pickup.ALLOWED;
      return arrow;
   }
}
