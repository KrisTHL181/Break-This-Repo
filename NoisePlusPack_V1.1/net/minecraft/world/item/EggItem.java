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
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.level.Level;

public class EggItem extends Item implements ProjectileItem {
   public static final float PROJECTILE_SHOOT_POWER = 1.5F;

   public EggItem(Item.Properties p_41126_) {
      super(p_41126_);
   }

   @Override
   public InteractionResult use(Level p_41128_, Player p_41129_, InteractionHand p_41130_) {
      ItemStack itemstack = p_41129_.getItemInHand(p_41130_);
      p_41128_.playSound(
         null,
         p_41129_.getX(),
         p_41129_.getY(),
         p_41129_.getZ(),
         SoundEvents.EGG_THROW,
         SoundSource.PLAYERS,
         0.5F,
         0.4F / (p_41128_.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (p_41128_ instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileFromRotation(ThrownEgg::new, serverlevel, itemstack, p_41129_, 0.0F, 1.5F, 1.0F);
      }

      p_41129_.awardStat(Stats.ITEM_USED.get(this));
      itemstack.consume(1, p_41129_);
      return InteractionResult.SUCCESS;
   }

   @Override
   public Projectile asProjectile(Level p_334937_, Position p_334000_, ItemStack p_330091_, Direction p_336145_) {
      return new ThrownEgg(p_334937_, p_334000_.x(), p_334000_.y(), p_334000_.z(), p_330091_);
   }
}
