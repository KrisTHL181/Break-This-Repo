package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class BowItem extends ProjectileWeaponItem {
   public static final int MAX_DRAW_DURATION = 20;
   public static final int DEFAULT_RANGE = 15;

   public BowItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public boolean releaseUsing(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime) {
      if (entity instanceof Player player) {
         ItemStack projectile = player.getProjectile(itemStack);
         if (projectile.isEmpty()) {
            return false;
         }

         int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
         float pow = getPowerForTime(timeHeld);
         if (pow < 0.1) {
            return false;
         }

         List<ItemStack> firedProjectiles = draw(itemStack, projectile, player);
         if (level instanceof ServerLevel serverLevel && !firedProjectiles.isEmpty()) {
            this.shoot(serverLevel, player, player.getUsedItemHand(), itemStack, firedProjectiles, pow * 3.0F, 1.0F, pow == 1.0F, null);
         }

         level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F
         );
         player.awardStat(Stats.ITEM_USED.get(this));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void shootProjectile(
      final LivingEntity shooter,
      final Projectile projectileEntity,
      final int index,
      final float power,
      final float uncertainty,
      final float angle,
      final @Nullable LivingEntity targetOverrride
   ) {
      projectileEntity.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, power, uncertainty);
   }

   public static float getPowerForTime(final int timeHeld) {
      float pow = timeHeld / 20.0F;
      pow = (pow * pow + pow * 2.0F) / 3.0F;
      if (pow > 1.0F) {
         pow = 1.0F;
      }

      return pow;
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      return 72000;
   }

   @Override
   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      return ItemUseAnimation.BOW;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      boolean foundProjectile = !player.getProjectile(itemStack).isEmpty();
      if (!player.hasInfiniteMaterials() && !foundProjectile) {
         return InteractionResult.FAIL;
      }

      player.startUsingItem(hand);
      return InteractionResult.CONSUME;
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public int getDefaultProjectileRange() {
      return 15;
   }
}
