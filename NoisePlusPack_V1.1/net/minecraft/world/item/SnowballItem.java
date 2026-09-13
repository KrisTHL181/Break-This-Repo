package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;

public class SnowballItem extends Item implements ProjectileItem {
   public static float PROJECTILE_SHOOT_POWER = 1.5F;

   public SnowballItem(Item.Properties p_43140_) {
      super(p_43140_);
   }

   @Override
   public InteractionResult use(Level p_43142_, Player p_43143_, InteractionHand p_43144_) {
      ItemStack itemstack = p_43143_.getItemInHand(p_43144_);
      p_43142_.playSound(
         null,
         p_43143_.getX(),
         p_43143_.getY(),
         p_43143_.getZ(),
         SoundEvents.SNOWBALL_THROW,
         SoundSource.NEUTRAL,
         0.5F,
         0.4F / (p_43142_.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (p_43142_ instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileFromRotation(Snowball::new, serverlevel, itemstack, p_43143_, 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
      }

      p_43143_.awardStat(Stats.ITEM_USED.get(this));
      itemstack.consume(1, p_43143_);
      return InteractionResult.SUCCESS;
   }

   @Override
   public Projectile asProjectile(Level p_331733_, Position p_331858_, ItemStack p_327677_, Direction p_328077_) {
      return new Snowball(p_331733_, p_331858_.x(), p_331858_.y(), p_331858_.z(), p_327677_);
   }
}
