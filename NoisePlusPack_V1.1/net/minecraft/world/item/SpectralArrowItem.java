package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class SpectralArrowItem extends ArrowItem {
   public SpectralArrowItem(Item.Properties p_43235_) {
      super(p_43235_);
   }

   @Override
   public AbstractArrow createArrow(Level p_43237_, ItemStack p_43238_, LivingEntity p_43239_, @Nullable ItemStack p_344301_) {
      return new SpectralArrow(p_43237_, p_43239_, p_43238_.copyWithCount(1), p_344301_);
   }

   @Override
   public Projectile asProjectile(Level p_331476_, Position p_329787_, ItemStack p_328274_, Direction p_330256_) {
      SpectralArrow spectralarrow = new SpectralArrow(p_331476_, p_329787_.x(), p_329787_.y(), p_329787_.z(), p_328274_.copyWithCount(1), null);
      spectralarrow.pickup = AbstractArrow.Pickup.ALLOWED;
      return spectralarrow;
   }
}
