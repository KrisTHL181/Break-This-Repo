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
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;

public class ExperienceBottleItem extends Item implements ProjectileItem {
   public ExperienceBottleItem(Item.Properties p_41194_) {
      super(p_41194_);
   }

   @Override
   public InteractionResult use(Level p_41196_, Player p_41197_, InteractionHand p_41198_) {
      ItemStack itemstack = p_41197_.getItemInHand(p_41198_);
      p_41196_.playSound(
         null,
         p_41197_.getX(),
         p_41197_.getY(),
         p_41197_.getZ(),
         SoundEvents.EXPERIENCE_BOTTLE_THROW,
         SoundSource.NEUTRAL,
         0.5F,
         0.4F / (p_41196_.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      if (p_41196_ instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileFromRotation(ThrownExperienceBottle::new, serverlevel, itemstack, p_41197_, -20.0F, 0.7F, 1.0F);
      }

      p_41197_.awardStat(Stats.ITEM_USED.get(this));
      itemstack.consume(1, p_41197_);
      return InteractionResult.SUCCESS;
   }

   @Override
   public Projectile asProjectile(Level p_329027_, Position p_329351_, ItemStack p_330574_, Direction p_336102_) {
      return new ThrownExperienceBottle(p_329027_, p_329351_.x(), p_329351_.y(), p_329351_.z(), p_330574_);
   }

   @Override
   public ProjectileItem.DispenseConfig createDispenseConfig() {
      return ProjectileItem.DispenseConfig.builder()
         .uncertainty(ProjectileItem.DispenseConfig.DEFAULT.uncertainty() * 0.5F)
         .power(ProjectileItem.DispenseConfig.DEFAULT.power() * 1.25F)
         .build();
   }
}
