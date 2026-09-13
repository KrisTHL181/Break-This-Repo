package net.minecraft.world.entity.projectile.arrow;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpectralArrow extends AbstractArrow {
   private static final int DEFAULT_DURATION = 200;
   private int duration = 200;

   public SpectralArrow(EntityType<? extends SpectralArrow> p_458263_, Level p_453274_) {
      super(p_458263_, p_453274_);
   }

   public SpectralArrow(Level p_460780_, LivingEntity p_453009_, ItemStack p_452960_, @Nullable ItemStack p_454895_) {
      super(EntityType.SPECTRAL_ARROW, p_453009_, p_460780_, p_452960_, p_454895_);
   }

   public SpectralArrow(Level p_450215_, double p_458810_, double p_458272_, double p_457324_, ItemStack p_453736_, @Nullable ItemStack p_457826_) {
      super(EntityType.SPECTRAL_ARROW, p_458810_, p_458272_, p_457324_, p_450215_, p_453736_, p_457826_);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide() && !this.isInGround()) {
         this.level().addParticle(SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0F), this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
      }
   }

   @Override
   protected void doPostHurtEffects(LivingEntity p_460491_) {
      super.doPostHurtEffects(p_460491_);
      MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.GLOWING, this.duration, 0);
      p_460491_.addEffect(mobeffectinstance, this.getEffectSource());
   }

   @Override
   protected void readAdditionalSaveData(ValueInput p_454541_) {
      super.readAdditionalSaveData(p_454541_);
      this.duration = p_454541_.getIntOr("Duration", 200);
   }

   @Override
   protected void addAdditionalSaveData(ValueOutput p_457309_) {
      super.addAdditionalSaveData(p_457309_);
      p_457309_.putInt("Duration", this.duration);
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.SPECTRAL_ARROW);
   }
}
