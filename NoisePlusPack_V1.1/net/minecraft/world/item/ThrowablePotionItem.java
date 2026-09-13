package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;

public abstract class ThrowablePotionItem extends PotionItem implements ProjectileItem {
   public static float PROJECTILE_SHOOT_POWER = 0.5F;

   public ThrowablePotionItem(Item.Properties p_43301_) {
      super(p_43301_);
   }

   @Override
   public InteractionResult use(Level p_43303_, Player p_43304_, InteractionHand p_43305_) {
      ItemStack itemstack = p_43304_.getItemInHand(p_43305_);
      if (p_43303_ instanceof ServerLevel serverlevel) {
         Projectile.spawnProjectileFromRotation(this::createPotion, serverlevel, itemstack, p_43304_, -20.0F, PROJECTILE_SHOOT_POWER, 1.0F);
      }

      p_43304_.awardStat(Stats.ITEM_USED.get(this));
      itemstack.consume(1, p_43304_);
      return InteractionResult.SUCCESS;
   }

   protected abstract AbstractThrownPotion createPotion(ServerLevel var1, LivingEntity var2, ItemStack var3);

   protected abstract AbstractThrownPotion createPotion(Level var1, Position var2, ItemStack var3);

   @Override
   public Projectile asProjectile(Level p_332520_, Position p_329324_, ItemStack p_333928_, Direction p_335406_) {
      return this.createPotion(p_332520_, p_329324_, p_333928_);
   }

   @Override
   public ProjectileItem.DispenseConfig createDispenseConfig() {
      return ProjectileItem.DispenseConfig.builder()
         .uncertainty(ProjectileItem.DispenseConfig.DEFAULT.uncertainty() * 0.5F)
         .power(ProjectileItem.DispenseConfig.DEFAULT.power() * 1.25F)
         .build();
   }
}
