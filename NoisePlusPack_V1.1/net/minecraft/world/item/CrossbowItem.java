package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
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

   public CrossbowItem(Item.Properties p_40850_) {
      super(p_40850_);
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
   public InteractionResult use(Level p_40920_, Player p_40921_, InteractionHand p_40922_) {
      ItemStack itemstack = p_40921_.getItemInHand(p_40922_);
      ChargedProjectiles chargedprojectiles = itemstack.get(DataComponents.CHARGED_PROJECTILES);
      if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
         this.performShooting(p_40920_, p_40921_, p_40922_, itemstack, getShootingPower(chargedprojectiles), 1.0F, null);
         return InteractionResult.CONSUME;
      } else if (!p_40921_.getProjectile(itemstack).isEmpty()) {
         this.startSoundPlayed = false;
         this.midLoadSoundPlayed = false;
         p_40921_.startUsingItem(p_40922_);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static float getShootingPower(ChargedProjectiles p_331334_) {
      return p_331334_.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
   }

   @Override
   public boolean releaseUsing(ItemStack p_40875_, Level p_40876_, LivingEntity p_40877_, int p_40878_) {
      int i = this.getUseDuration(p_40875_, p_40877_) - p_40878_;
      return getPowerForTime(i, p_40875_, p_40877_) >= 1.0F && isCharged(p_40875_);
   }

   private static boolean tryLoadProjectiles(LivingEntity p_40860_, ItemStack p_40861_) {
      List<ItemStack> list = draw(p_40861_, p_40860_.getProjectile(p_40861_), p_40860_);
      if (!list.isEmpty()) {
         p_40861_.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
         return true;
      } else {
         return false;
      }
   }

   public static boolean isCharged(ItemStack p_40933_) {
      ChargedProjectiles chargedprojectiles = p_40933_.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
      return !chargedprojectiles.isEmpty();
   }

   @Override
   protected void shootProjectile(
      LivingEntity p_40896_, Projectile p_335393_, int p_333089_, float p_40900_, float p_40902_, float p_40903_, @Nullable LivingEntity p_328705_
   ) {
      Vector3f vector3f;
      if (p_328705_ != null) {
         double d0 = p_328705_.getX() - p_40896_.getX();
         double d1 = p_328705_.getZ() - p_40896_.getZ();
         double d2 = Math.sqrt(d0 * d0 + d1 * d1);
         double d3 = p_328705_.getY(0.3333333333333333) - p_335393_.getY() + d2 * 0.2F;
         vector3f = getProjectileShotVector(p_40896_, new Vec3(d0, d3, d1), p_40903_);
      } else {
         Vec3 vec3 = p_40896_.getUpVector(1.0F);
         Quaternionf quaternionf = new Quaternionf().setAngleAxis(p_40903_ * (float) (Math.PI / 180.0), vec3.x, vec3.y, vec3.z);
         Vec3 vec31 = p_40896_.getViewVector(1.0F);
         vector3f = vec31.toVector3f().rotate(quaternionf);
      }

      p_335393_.shoot(vector3f.x(), vector3f.y(), vector3f.z(), p_40900_, p_40902_);
      float f = getShotPitch(p_40896_.getRandom(), p_333089_);
      p_40896_.level().playSound(null, p_40896_.getX(), p_40896_.getY(), p_40896_.getZ(), SoundEvents.CROSSBOW_SHOOT, p_40896_.getSoundSource(), 1.0F, f);
   }

   private static Vector3f getProjectileShotVector(LivingEntity p_333832_, Vec3 p_332433_, float p_331595_) {
      Vector3f vector3f = p_332433_.toVector3f().normalize();
      Vector3f vector3f1 = new Vector3f(vector3f).cross(new Vector3f(0.0F, 1.0F, 0.0F));
      if (vector3f1.lengthSquared() <= 1.0E-7) {
         Vec3 vec3 = p_333832_.getUpVector(1.0F);
         vector3f1 = new Vector3f(vector3f).cross(vec3.toVector3f());
      }

      Vector3f vector3f2 = new Vector3f(vector3f).rotateAxis((float) (Math.PI / 2), vector3f1.x, vector3f1.y, vector3f1.z);
      return new Vector3f(vector3f).rotateAxis(p_331595_ * (float) (Math.PI / 180.0), vector3f2.x, vector3f2.y, vector3f2.z);
   }

   @Override
   protected Projectile createProjectile(Level p_329989_, LivingEntity p_40863_, ItemStack p_40864_, ItemStack p_40865_, boolean p_40866_) {
      if (p_40865_.is(Items.FIREWORK_ROCKET)) {
         return new FireworkRocketEntity(p_329989_, p_40865_, p_40863_, p_40863_.getX(), p_40863_.getEyeY() - 0.15F, p_40863_.getZ(), true);
      }

      Projectile projectile = super.createProjectile(p_329989_, p_40863_, p_40864_, p_40865_, p_40866_);
      if (projectile instanceof AbstractArrow abstractarrow) {
         abstractarrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
      }

      return projectile;
   }

   @Override
   protected int getDurabilityUse(ItemStack p_335533_) {
      return p_335533_.is(Items.FIREWORK_ROCKET) ? 3 : 1;
   }

   public void performShooting(
      Level p_40888_, LivingEntity p_40889_, InteractionHand p_40890_, ItemStack p_40891_, float p_40892_, float p_40893_, @Nullable LivingEntity p_329478_
   ) {
      if (p_40888_ instanceof ServerLevel serverlevel) {
         ChargedProjectiles chargedprojectiles = p_40891_.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
         if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            this.shoot(serverlevel, p_40889_, p_40890_, p_40891_, chargedprojectiles.getItems(), p_40892_, p_40893_, p_40889_ instanceof Player, p_329478_);
            if (p_40889_ instanceof ServerPlayer serverplayer) {
               CriteriaTriggers.SHOT_CROSSBOW.trigger(serverplayer, p_40891_);
               serverplayer.awardStat(Stats.ITEM_USED.get(p_40891_.getItem()));
            }
         }
      }
   }

   private static float getShotPitch(RandomSource p_335611_, int p_331713_) {
      return p_331713_ == 0 ? 1.0F : getRandomShotPitch((p_331713_ & 1) == 1, p_335611_);
   }

   private static float getRandomShotPitch(boolean p_220026_, RandomSource p_220027_) {
      float f = p_220026_ ? 0.63F : 0.43F;
      return 1.0F / (p_220027_.nextFloat() * 0.5F + 1.8F) + f;
   }

   @Override
   public void onUseTick(Level p_40910_, LivingEntity p_40911_, ItemStack p_40912_, int p_40913_) {
      if (!p_40910_.isClientSide()) {
         CrossbowItem.ChargingSounds crossbowitem$chargingsounds = this.getChargingSounds(p_40912_);
         float f = (float)(p_40912_.getUseDuration(p_40911_) - p_40913_) / getChargeDuration(p_40912_, p_40911_);
         if (f < 0.2F) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
         }

         if (f >= 0.2F && !this.startSoundPlayed) {
            this.startSoundPlayed = true;
            crossbowitem$chargingsounds.start()
               .ifPresent(
                  p_449785_ -> p_40910_.playSound(null, p_40911_.getX(), p_40911_.getY(), p_40911_.getZ(), p_449785_.value(), SoundSource.PLAYERS, 0.5F, 1.0F)
               );
         }

         if (f >= 0.5F && !this.midLoadSoundPlayed) {
            this.midLoadSoundPlayed = true;
            crossbowitem$chargingsounds.mid()
               .ifPresent(
                  p_449782_ -> p_40910_.playSound(null, p_40911_.getX(), p_40911_.getY(), p_40911_.getZ(), p_449782_.value(), SoundSource.PLAYERS, 0.5F, 1.0F)
               );
         }

         if (f >= 1.0F && !isCharged(p_40912_) && tryLoadProjectiles(p_40911_, p_40912_)) {
            crossbowitem$chargingsounds.end()
               .ifPresent(
                  p_449788_ -> p_40910_.playSound(
                     null,
                     p_40911_.getX(),
                     p_40911_.getY(),
                     p_40911_.getZ(),
                     p_449788_.value(),
                     p_40911_.getSoundSource(),
                     1.0F,
                     1.0F / (p_40910_.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                  )
               );
         }
      }
   }

   @Override
   public int getUseDuration(ItemStack p_40938_, LivingEntity p_342603_) {
      return 72000;
   }

   public static int getChargeDuration(ItemStack p_40940_, LivingEntity p_344015_) {
      float f = EnchantmentHelper.modifyCrossbowChargingTime(p_40940_, p_344015_, 1.25F);
      return Mth.floor(f * 20.0F);
   }

   @Override
   public ItemUseAnimation getUseAnimation(ItemStack p_40935_) {
      return ItemUseAnimation.CROSSBOW;
   }

   CrossbowItem.ChargingSounds getChargingSounds(ItemStack p_345404_) {
      return EnchantmentHelper.pickHighestLevel(p_345404_, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
   }

   private static float getPowerForTime(int p_40854_, ItemStack p_40855_, LivingEntity p_343301_) {
      float f = (float)p_40854_ / getChargeDuration(p_40855_, p_343301_);
      if (f > 1.0F) {
         f = 1.0F;
      }

      return f;
   }

   @Override
   public boolean useOnRelease(ItemStack p_150801_) {
      return p_150801_.is(this);
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

      ChargeType(final String p_378184_) {
         this.name = p_378184_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
      public static final Codec<CrossbowItem.ChargingSounds> CODEC = RecordCodecBuilder.create(
         p_344158_ -> p_344158_.group(
               SoundEvent.CODEC.optionalFieldOf("start").forGetter(CrossbowItem.ChargingSounds::start),
               SoundEvent.CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.ChargingSounds::mid),
               SoundEvent.CODEC.optionalFieldOf("end").forGetter(CrossbowItem.ChargingSounds::end)
            )
            .apply(p_344158_, CrossbowItem.ChargingSounds::new)
      );
   }
}
