package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public class WindChargeItem extends Item implements ProjectileItem {
   public static final float PROJECTILE_SHOOT_POWER = 1.5F;

   public WindChargeItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level instanceof ServerLevel serverLevel) {
         Projectile.spawnProjectileFromRotation(
            (source, l, itemStack) -> new WindCharge(player, level, player.position().x(), player.getEyePosition().y(), player.position().z()),
            serverLevel,
            stack,
            player,
            0.0F,
            1.5F,
            1.0F
         );
      }

      level.playSound(
         null,
         player.getX(),
         player.getY(),
         player.getZ(),
         SoundEvents.WIND_CHARGE_THROW,
         SoundSource.NEUTRAL,
         0.5F,
         0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
      );
      player.awardStat(Stats.ITEM_USED.get(this));
      stack.consume(1, player);
      return InteractionResult.SUCCESS;
   }

   @Override
   public Projectile asProjectile(final Level level, final Position position, final ItemStack itemStack, final Direction direction) {
      RandomSource random = level.getRandom();
      double dirX = random.triangle(direction.getStepX(), 0.11485000000000001);
      double dirY = random.triangle(direction.getStepY(), 0.11485000000000001);
      double dirZ = random.triangle(direction.getStepZ(), 0.11485000000000001);
      Vec3 dir = new Vec3(dirX, dirY, dirZ);
      WindCharge windCharge = new WindCharge(level, position.x(), position.y(), position.z(), dir);
      windCharge.setDeltaMovement(dir);
      return windCharge;
   }

   @Override
   public void shoot(final Projectile projectile, final double xd, final double yd, final double zd, final float pow, final float uncertainty) {
   }

   @Override
   public ProjectileItem.DispenseConfig createDispenseConfig() {
      return ProjectileItem.DispenseConfig.builder()
         .positionFunction((source, direction) -> DispenserBlock.getDispensePosition(source, 1.0, Vec3.ZERO))
         .uncertainty(6.6666665F)
         .power(1.0F)
         .overrideDispenseEvent(1051)
         .build();
   }
}
