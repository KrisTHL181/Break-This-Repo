package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;

public class EnderpearlItem extends Item {
   public static float PROJECTILE_SHOOT_POWER = 1.5F;

   public EnderpearlItem(Item.Properties p_41188_) {
      super(p_41188_);
   }

   @Override
   public InteractionResult use(Level p_41190_, Player p_41191_, InteractionHand p_41192_) {
      ItemStack itemstack = p_41191_.getItemInHand(p_41192_);
      p_41190_.playSound(
         null,
         p_41191_.getX(),
         p_41191_.getY(),
         p_41191_.getZ(),
         SoundEvents.ENDER_PEARL_THROW,
         SoundSource.NEUTRAL,
         0.5F,
         0.4F / (p_41190_.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (p_41190_ instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileFromRotation(ThrownEnderpearl::new, serverlevel, itemstack, p_41191_, 0.0F, PROJECTILE_SHOOT_POWER, 1.0F);
      }

      p_41191_.awardStat(Stats.ITEM_USED.get(this));
      itemstack.consume(1, p_41191_);
      return InteractionResult.SUCCESS;
   }
}
