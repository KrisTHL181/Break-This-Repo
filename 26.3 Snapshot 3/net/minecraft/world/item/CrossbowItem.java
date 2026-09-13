package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class CrossbowItem extends ProjectileWeaponItem {
   private static final float MAX_CHARGE_DURATION = 1.25F;
   public static final int DEFAULT_RANGE = 8;
   private boolean startSoundPlayed = false;
   private boolean midLoadSoundPlayed = false;
   private static final float START_SOUND_PERCENT = 0.2F;
   private static final float MID_SOUND_PERCENT = 0.5F;
   private static final float ARROW_POWER = 3.15F;
   private static final float FIREWORK_POWER = 1.6F;
   public static final float MOB_ARROW_POWER = 1.6F;
   private static final CrossbowItem.ChargingSounds DEFAULT_SOUNDS = new CrossbowItem.ChargingSounds(
      Optional.of(SoundEvents.CROSSBOW_LOADING_START), Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEvents.CROSSBOW_LOADING_END)
   );

   public CrossbowItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public Predicate<ItemStack> getSupportedHeldProjectiles() {
      return ARROW_OR_FIREWORK;
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      ChargedProjectiles chargedProjectiles = itemStack.get(DataComponents.CHARGED_PROJECTILES);
      if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
         this.performShooting(level, player, hand, itemStack, getShootingPower(chargedProjectiles), 1.0F, null);
         return InteractionResult.CONSUME;
      } else if (!player.getProjectile(itemStack).isEmpty()) {
         this.startSoundPlayed = false;
         this.midLoadSoundPlayed = false;
         player.startUsingItem(hand);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static float getShootingPower(final ChargedProjectiles projectiles) {
      return projectiles.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
   }

   @Override
   public boolean releaseUsing(final ItemStack itemStack, final Level level, final LivingEntity entity, final int remainingTime) {
      int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
      return getPowerForTime(timeHeld, itemStack, entity) >= 1.0F && isCharged(itemStack);
   }

   private static boolean tryLoadProjectiles(final LivingEntity shooter, final ItemStack heldItem) {
      List<ItemStack> drawn = draw(heldItem, shooter.getProjectile(heldItem), shooter);
      if (!drawn.isEmpty()) {
         heldItem.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(drawn));
         return true;
      } else {
         return false;
      }
   }

   public static boolean isCharged(final ItemStack itemStack) {
      ChargedProjectiles projectiles = itemStack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
      return !projectiles.isEmpty();
   }

   @Override
   protected void shootProjectile(
      final LivingEntity livingEntity,
      final Projectile projectileEntity,
      final int index,
      final float power,
      final float uncertainty,
      final float angle,
      final @Nullable LivingEntity targetOverride
   ) {
      Vector3f shotVector;
      if (targetOverride != null) {
         double xd = targetOverride.getX() - livingEntity.getX();
         double zd = targetOverride.getZ() - livingEntity.getZ();
         double distanceToTarget = Math.sqrt(xd * xd + zd * zd);
         double yd = targetOverride.getY(0.3333333333333333) - projectileEntity.getY() + distanceToTarget * 0.2F;
         shotVector = getProjectileShotVector(livingEntity, new Vec3(xd, yd, zd), angle);
      } else {
         Vec3 upVector = livingEntity.getUpVector(1.0F);
         Quaternionf upQuaternion = new Quaternionf().setAngleAxis(angle * (float) (Math.PI / 180.0), upVector.x, upVector.y, upVector.z);
         Vec3 viewVec = livingEntity.getViewVector(1.0F);
         shotVector = viewVec.toVector3f().rotate(upQuaternion);
      }

      projectileEntity.shoot(shotVector.x(), shotVector.y(), shotVector.z(), power, uncertainty);
      float soundPitch = getShotPitch(livingEntity.getRandom(), index);
      livingEntity.level()
         .playSound(
            null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.CROSSBOW_SHOOT, livingEntity.getSoundSource(), 1.0F, soundPitch
         );
   }

   private static Vector3f getProjectileShotVector(final LivingEntity body, final Vec3 originalVector, final float angle) {
      Vector3f viewVec = originalVector.toVector3f().normalize();
      Vector3f rightVectorPreRot = new Vector3f(viewVec).cross(new Vector3f(0.0F, 1.0F, 0.0F));
      if (rightVectorPreRot.lengthSquared() <= 1.0E-7) {
         Vec3 up = body.getUpVector(1.0F);
         rightVectorPreRot = new Vector3f(viewVec).cross(up.toVector3f());
      }

      Vector3f viewVec3f = new Vector3f(viewVec).rotateAxis((float) (Math.PI / 2), rightVectorPreRot.x, rightVectorPreRot.y, rightVectorPreRot.z);
      return new Vector3f(viewVec).rotateAxis(angle * (float) (Math.PI / 180.0), viewVec3f.x, viewVec3f.y, viewVec3f.z);
   }

   @Override
   protected Projectile createProjectile(
      final Level level, final LivingEntity shooter, final ItemStack heldItem, final ItemStack projectile, final boolean isCrit
   ) {
      if (projectile.is(Items.FIREWORK_ROCKET)) {
         return new FireworkRocketEntity(level, projectile, shooter, shooter.getX(), shooter.getEyeY() - 0.15F, shooter.getZ(), true);
      }

      Projectile projectileEntity = super.createProjectile(level, shooter, heldItem, projectile, isCrit);
      if (projectileEntity instanceof AbstractArrow arrow) {
         arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
      }

      return projectileEntity;
   }

   @Override
   protected int getDurabilityUse(final ItemStack projectile) {
      return projectile.is(Items.FIREWORK_ROCKET) ? 3 : 1;
   }

   public void performShooting(
      final Level level,
      final LivingEntity shooter,
      final InteractionHand hand,
      final ItemStack weapon,
      final float power,
      final float uncertainty,
      final @Nullable LivingEntity targetOverride
   ) {
      if (level instanceof ServerLevel serverLevel) {
         ChargedProjectiles charged = weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
         if (charged != null && !charged.isEmpty()) {
            List<ItemStack> projectiles = charged.itemCopies().toList();
            this.shoot(serverLevel, shooter, hand, weapon, projectiles, power, uncertainty, shooter instanceof Player, targetOverride);
            if (shooter instanceof ServerPlayer player) {
               CriteriaTriggers.SHOT_CROSSBOW.trigger(player, weapon);
               player.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
            }
         }
      }
   }

   private static float getShotPitch(final RandomSource random, final int index) {
      return index == 0 ? 1.0F : getRandomShotPitch((index & 1) == 1, random);
   }

   private static float getRandomShotPitch(final boolean highPitch, final RandomSource random) {
      float rangeDecider = highPitch ? 0.63F : 0.43F;
      return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + rangeDecider;
   }

   @Override
   public void onUseTick(final Level level, final LivingEntity entity, final ItemStack itemStack, final int ticksRemaining) {
      if (!level.isClientSide()) {
         CrossbowItem.ChargingSounds sounds = getChargingSounds(itemStack);
         float tickPercent = (float)(itemStack.getUseDuration(entity) - ticksRemaining) / getChargeDuration(itemStack, entity);
         if (tickPercent < 0.2F) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
         }

         if (tickPercent >= 0.2F && !this.startSoundPlayed) {
            this.startSoundPlayed = true;
            sounds.start()
               .ifPresent(sound -> level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound.value(), SoundSource.PLAYERS, 0.5F, 1.0F));
         }

         if (tickPercent >= 0.5F && !this.midLoadSoundPlayed) {
            this.midLoadSoundPlayed = true;
            sounds.mid().ifPresent(sound -> level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound.value(), SoundSource.PLAYERS, 0.5F, 1.0F));
         }

         if (tickPercent >= 1.0F && !isCharged(itemStack) && tryLoadProjectiles(entity, itemStack)) {
            sounds.end()
               .ifPresent(
                  sound -> level.playSound(
                     null,
                     entity.getX(),
                     entity.getY(),
                     entity.getZ(),
                     sound.value(),
                     entity.getSoundSource(),
                     1.0F,
                     1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                  )
               );
         }
      }
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      return 72000;
   }

   public static int getChargeDuration(final ItemStack crossbow, final LivingEntity user) {
      float duration = EnchantmentHelper.modifyCrossbowChargingTime(crossbow, user, 1.25F);
      return Mth.floor(duration * 20.0F);
   }

   @Override
   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      return ItemUseAnimation.CROSSBOW;
   }

   private static CrossbowItem.ChargingSounds getChargingSounds(final ItemStack itemStack) {
      return EnchantmentHelper.pickHighestLevel(itemStack, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
   }

   private static float getPowerForTime(final int timeHeld, final ItemStack itemStack, final LivingEntity holder) {
      float pow = (float)timeHeld / getChargeDuration(itemStack, holder);
      if (pow > 1.0F) {
         pow = 1.0F;
      }

      return pow;
   }

   @Override
   public boolean useOnRelease(final ItemStack itemStack) {
      return itemStack.is(this);
   }

   @Override
   public int getDefaultProjectileRange() {
      return 8;
   }

   public enum ChargeType implements StringRepresentable {
      NONE("none"),
      ARROW("arrow"),
      ROCKET("rocket");

      public static final Codec<CrossbowItem.ChargeType> CODEC = StringRepresentable.fromEnum(CrossbowItem.ChargeType::values);
      private final String name;

      ChargeType(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
      public static final Codec<CrossbowItem.ChargingSounds> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               SoundEvent.CODEC.optionalFieldOf("start").forGetter(CrossbowItem.ChargingSounds::start),
               SoundEvent.CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.ChargingSounds::mid),
               SoundEvent.CODEC.optionalFieldOf("end").forGetter(CrossbowItem.ChargingSounds::end)
            )
            .apply(i, CrossbowItem.ChargingSounds::new)
      );
   }
}
