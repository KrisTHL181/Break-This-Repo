package net.minecraft.world.entity.projectile.arrow;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Arrow extends AbstractArrow {
   private static final int EXPOSED_POTION_DECAY_TIME = 600;
   private static final int NO_EFFECT_COLOR = -1;
   private static final EntityDataAccessor<Integer> ID_EFFECT_COLOR = SynchedEntityData.defineId(Arrow.class, EntityDataSerializers.INT);
   private static final byte EVENT_POTION_PUFF = 0;

   public Arrow(EntityType<? extends Arrow> p_451639_, Level p_450854_) {
      super(p_451639_, p_450854_);
   }

   public Arrow(Level p_453816_, double p_459002_, double p_460176_, double p_450378_, ItemStack p_460785_, @Nullable ItemStack p_455384_) {
      super(EntityType.ARROW, p_459002_, p_460176_, p_450378_, p_453816_, p_460785_, p_455384_);
      this.updateColor();
   }

   public Arrow(Level p_455240_, LivingEntity p_451575_, ItemStack p_452605_, @Nullable ItemStack p_450609_) {
      super(EntityType.ARROW, p_451575_, p_455240_, p_452605_, p_450609_);
      this.updateColor();
   }

   private PotionContents getPotionContents() {
      return this.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
   }

   private float getPotionDurationScale() {
      return this.getPickupItemStackOrigin().getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
   }

   private void setPotionContents(PotionContents p_455544_) {
      this.getPickupItemStackOrigin().set(DataComponents.POTION_CONTENTS, p_455544_);
      this.updateColor();
   }

   @Override
   protected void setPickupItemStack(ItemStack p_452448_) {
      super.setPickupItemStack(p_452448_);
      this.updateColor();
   }

   private void updateColor() {
      PotionContents potioncontents = this.getPotionContents();
      this.entityData.set(ID_EFFECT_COLOR, potioncontents.equals(PotionContents.EMPTY) ? -1 : potioncontents.getColor());
   }

   public void addEffect(MobEffectInstance p_455244_) {
      this.setPotionContents(this.getPotionContents().withEffectAdded(p_455244_));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder p_454050_) {
      super.defineSynchedData(p_454050_);
      p_454050_.define(ID_EFFECT_COLOR, -1);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.isInGround()) {
            if (this.inGroundTime % 5 == 0) {
               this.makeParticle(1);
            }
         } else {
            this.makeParticle(2);
         }
      } else if (this.isInGround() && this.inGroundTime != 0 && !this.getPotionContents().equals(PotionContents.EMPTY) && this.inGroundTime >= 600) {
         this.level().broadcastEntityEvent(this, (byte)0);
         this.setPickupItemStack(new ItemStack(Items.ARROW));
      }
   }

   private void makeParticle(int p_451839_) {
      int i = this.getColor();
      if (i != -1 && p_451839_ > 0) {
         for (int j = 0; j < p_451839_; j++) {
            this.level()
               .addParticle(
                  ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, i), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0
               );
         }
      }
   }

   public int getColor() {
      return this.entityData.get(ID_EFFECT_COLOR);
   }

   @Override
   protected void doPostHurtEffects(LivingEntity p_460434_) {
      super.doPostHurtEffects(p_460434_);
      Entity entity = this.getEffectSource();
      PotionContents potioncontents = this.getPotionContents();
      float f = this.getPotionDurationScale();
      potioncontents.forEachEffect(p_455051_ -> p_460434_.addEffect(p_455051_, entity), f);
   }

   @Override
   protected ItemStack getDefaultPickupItem() {
      return new ItemStack(Items.ARROW);
   }

   @Override
   public void handleEntityEvent(byte p_460473_) {
      if (p_460473_ == 0) {
         int i = this.getColor();
         if (i != -1) {
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i >> 0 & 0xFF) / 255.0F;

            for (int j = 0; j < 20; j++) {
               this.level()
                  .addParticle(
                     ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, f, f1, f2),
                     this.getRandomX(0.5),
                     this.getRandomY(),
                     this.getRandomZ(0.5),
                     0.0,
                     0.0,
                     0.0
                  );
            }
         }
      } else {
         super.handleEntityEvent(p_460473_);
      }
   }
}
