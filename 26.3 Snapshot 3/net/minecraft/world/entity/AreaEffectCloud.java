package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class AreaEffectCloud extends Entity implements TraceableEntity {
   private static final int TIME_BETWEEN_APPLICATIONS = 5;
   private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
   private static final float MAX_RADIUS = 32.0F;
   private static final int DEFAULT_AGE = 0;
   private static final int DEFAULT_DURATION_ON_USE = 0;
   private static final float DEFAULT_RADIUS_ON_USE = 0.0F;
   private static final float DEFAULT_RADIUS_PER_TICK = 0.0F;
   private static final float DEFAULT_POTION_DURATION_SCALE = 1.0F;
   private static final float MINIMAL_RADIUS = 0.5F;
   private static final float DEFAULT_RADIUS = 3.0F;
   public static final float DEFAULT_WIDTH = 6.0F;
   public static final float HEIGHT = 0.5F;
   public static final int INFINITE_DURATION = -1;
   public static final int DEFAULT_LINGERING_DURATION = 600;
   private static final int DEFAULT_WAIT_TIME = 20;
   private static final int DEFAULT_REAPPLICATION_DELAY = 20;
   private static final ColorParticleOption DEFAULT_PARTICLE = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1);
   private @Nullable ParticleOptions customParticle;
   private PotionContents potionContents = PotionContents.EMPTY;
   private float potionDurationScale = 1.0F;
   private final Map<Entity, Integer> victims = Maps.newHashMap();
   private int duration = -1;
   private int waitTime = 20;
   private int reapplicationDelay = 20;
   private int durationOnUse = 0;
   private float radiusOnUse = 0.0F;
   private float radiusPerTick = 0.0F;
   private @Nullable EntityReference<LivingEntity> owner;

   public AreaEffectCloud(final EntityType<? extends AreaEffectCloud> type, final Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public AreaEffectCloud(final Level level, final double x, final double y, final double z) {
      this(EntityTypes.AREA_EFFECT_CLOUD, level);
      this.setPos(x, y, z);
   }

   @Override
   protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
      entityData.define(DATA_RADIUS, 3.0F);
      entityData.define(DATA_WAITING, false);
      entityData.define(DATA_PARTICLE, DEFAULT_PARTICLE);
   }

   public void setRadius(final float radius) {
      if (!this.level().isClientSide()) {
         this.getEntityData().set(DATA_RADIUS, Mth.clamp(radius, 0.0F, 32.0F));
      }
   }

   @Override
   public void refreshDimensions() {
      double x = this.getX();
      double y = this.getY();
      double z = this.getZ();
      super.refreshDimensions();
      this.setPos(x, y, z);
   }

   public float getRadius() {
      return this.getEntityData().get(DATA_RADIUS);
   }

   public void setPotionContents(final PotionContents contents) {
      this.potionContents = contents;
      this.updateParticle();
   }

   public void setCustomParticle(final @Nullable ParticleOptions customParticle) {
      this.customParticle = customParticle;
      this.updateParticle();
   }

   public void setPotionDurationScale(final float scale) {
      this.potionDurationScale = scale;
   }

   private void updateParticle() {
      if (this.customParticle != null) {
         this.entityData.set(DATA_PARTICLE, this.customParticle);
      } else {
         int color = ARGB.opaque(this.potionContents.getColor());
         this.entityData.set(DATA_PARTICLE, ColorParticleOption.create(DEFAULT_PARTICLE.getType(), color));
      }
   }

   public void addEffect(final MobEffectInstance effect) {
      this.setPotionContents(this.potionContents.withEffectAdded(effect));
   }

   public ParticleOptions getParticle() {
      return this.getEntityData().get(DATA_PARTICLE);
   }

   protected void setWaiting(final boolean waiting) {
      this.getEntityData().set(DATA_WAITING, waiting);
   }

   public boolean isWaiting() {
      return this.getEntityData().get(DATA_WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(final int duration) {
      this.duration = duration;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel) {
         this.serverTick(serverLevel);
      } else {
         this.clientTick();
      }
   }

   private void clientTick() {
      boolean isWaiting = this.isWaiting();
      float radius = this.getRadius();
      if (!isWaiting || !this.random.nextBoolean()) {
         ParticleOptions particle = this.getParticle();
         int particleCount;
         float particleRadius;
         if (isWaiting) {
            particleCount = 2;
            particleRadius = 0.2F;
         } else {
            particleCount = Mth.ceil((float) Math.PI * radius * radius);
            particleRadius = radius;
         }

         for (int i = 0; i < particleCount; i++) {
            float angle = this.random.nextFloat() * (float) (Math.PI * 2);
            float distance = Mth.sqrt(this.random.nextFloat()) * particleRadius;
            double x = this.getX() + Mth.cos(angle) * distance;
            double y = this.getY();
            double z = this.getZ() + Mth.sin(angle) * distance;
            if (particle.getType() == ParticleTypes.ENTITY_EFFECT) {
               if (isWaiting && this.random.nextBoolean()) {
                  this.level().addAlwaysVisibleParticle(DEFAULT_PARTICLE, x, y, z, 0.0, 0.0, 0.0);
               } else {
                  this.level().addAlwaysVisibleParticle(particle, x, y, z, 0.0, 0.0, 0.0);
               }
            } else if (isWaiting) {
               this.level().addAlwaysVisibleParticle(particle, x, y, z, 0.0, 0.0, 0.0);
            } else {
               this.level()
                  .addAlwaysVisibleParticle(particle, x, y, z, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15);
            }
         }
      }
   }

   private void serverTick(final ServerLevel serverLevel) {
      if (this.duration != -1 && this.tickCount - this.waitTime >= this.duration) {
         this.discard();
      } else {
         boolean isWaiting = this.isWaiting();
         boolean shouldWait = this.tickCount < this.waitTime;
         if (isWaiting != shouldWait) {
            this.setWaiting(shouldWait);
         }

         if (!shouldWait) {
            float radius = this.getRadius();
            if (this.radiusPerTick != 0.0F) {
               radius += this.radiusPerTick;
               if (radius < 0.5F) {
                  this.discard();
                  return;
               }

               this.setRadius(radius);
            }

            if (this.tickCount % 5 == 0) {
               this.victims.entrySet().removeIf(entry -> this.tickCount >= entry.getValue());
               if (!this.potionContents.hasEffects()) {
                  this.victims.clear();
               } else {
                  List<MobEffectInstance> allEffects = new ArrayList<>();
                  this.potionContents.forEachEffect(allEffects::add, this.potionDurationScale);
                  List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
                  if (!entities.isEmpty()) {
                     for (LivingEntity entity : entities) {
                        if (!this.victims.containsKey(entity) && entity.isAffectedByPotions() && !allEffects.stream().noneMatch(entity::canBeAffected)) {
                           double xd = entity.getX() - this.getX();
                           double zd = entity.getZ() - this.getZ();
                           double dist = xd * xd + zd * zd;
                           if (dist <= radius * radius) {
                              this.victims.put(entity, this.tickCount + this.reapplicationDelay);

                              for (MobEffectInstance effect : allEffects) {
                                 if (effect.getEffect().value().isInstantaneous()) {
                                    effect.getEffect().value().applyInstantaneousEffect(serverLevel, this, this.getOwner(), entity, effect.getAmplifier(), 0.5);
                                 } else {
                                    entity.addEffect(new MobEffectInstance(effect), this);
                                 }
                              }

                              if (this.radiusOnUse != 0.0F) {
                                 radius += this.radiusOnUse;
                                 if (radius < 0.5F) {
                                    this.discard();
                                    return;
                                 }

                                 this.setRadius(radius);
                              }

                              if (this.durationOnUse != 0 && this.duration != -1) {
                                 this.duration = this.duration + this.durationOnUse;
                                 if (this.duration <= 0) {
                                    this.discard();
                                    return;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(final float radiusOnUse) {
      this.radiusOnUse = radiusOnUse;
   }

   public float getRadiusPerTick() {
      return this.radiusPerTick;
   }

   public void setRadiusPerTick(final float radiusPerTick) {
      this.radiusPerTick = radiusPerTick;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(final int durationOnUse) {
      this.durationOnUse = durationOnUse;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(final int waitTime) {
      this.waitTime = waitTime;
   }

   public void setOwner(final @Nullable LivingEntity owner) {
      this.owner = EntityReference.of(owner);
   }

   public @Nullable LivingEntity getOwner() {
      return EntityReference.getLivingEntity(this.owner, this.level());
   }

   @Override
   protected void readAdditionalSaveData(final ValueInput input) {
      this.tickCount = input.getIntOr("Age", 0);
      this.duration = input.getIntOr("Duration", -1);
      this.waitTime = input.getIntOr("WaitTime", 20);
      this.reapplicationDelay = input.getIntOr("ReapplicationDelay", 20);
      this.durationOnUse = input.getIntOr("DurationOnUse", 0);
      this.radiusOnUse = input.getFloatOr("RadiusOnUse", 0.0F);
      this.radiusPerTick = input.getFloatOr("RadiusPerTick", 0.0F);
      this.setRadius(input.getFloatOr("Radius", 3.0F));
      this.owner = EntityReference.read(input, "Owner");
      this.setCustomParticle(input.<ParticleOptions>read("custom_particle", ParticleTypes.CODEC).orElse(null));
      this.setPotionContents(input.<PotionContents>read("potion_contents", PotionContents.CODEC).orElse(PotionContents.EMPTY));
      this.potionDurationScale = input.getFloatOr("potion_duration_scale", 1.0F);
   }

   @Override
   protected void addAdditionalSaveData(final ValueOutput output) {
      output.putInt("Age", this.tickCount);
      output.putInt("Duration", this.duration);
      output.putInt("WaitTime", this.waitTime);
      output.putInt("ReapplicationDelay", this.reapplicationDelay);
      output.putInt("DurationOnUse", this.durationOnUse);
      output.putFloat("RadiusOnUse", this.radiusOnUse);
      output.putFloat("RadiusPerTick", this.radiusPerTick);
      output.putFloat("Radius", this.getRadius());
      output.storeNullable("custom_particle", ParticleTypes.CODEC, this.customParticle);
      EntityReference.store(this.owner, output, "Owner");
      if (!this.potionContents.equals(PotionContents.EMPTY)) {
         output.store("potion_contents", PotionContents.CODEC, this.potionContents);
      }

      if (this.potionDurationScale != 1.0F) {
         output.putFloat("potion_duration_scale", this.potionDurationScale);
      }
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      if (DATA_RADIUS.equals(accessor)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(accessor);
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public EntityDimensions getDimensions(final Pose pose) {
      return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
   }

   @Override
   public final boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
      return false;
   }

   @Override
   public <T> @Nullable T get(final DataComponentType<? extends T> type) {
      if (type == DataComponents.POTION_CONTENTS) {
         return castComponentValue((DataComponentType<T>)type, this.potionContents);
      } else {
         return type == DataComponents.POTION_DURATION_SCALE ? castComponentValue((DataComponentType<T>)type, this.potionDurationScale) : super.get(type);
      }
   }

   @Override
   protected void applyImplicitComponents(final DataComponentGetter components) {
      this.applyImplicitComponentIfPresent(components, DataComponents.POTION_CONTENTS);
      this.applyImplicitComponentIfPresent(components, DataComponents.POTION_DURATION_SCALE);
      super.applyImplicitComponents(components);
   }

   @Override
   protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
      if (type == DataComponents.POTION_CONTENTS) {
         this.setPotionContents(castComponentValue(DataComponents.POTION_CONTENTS, value));
         return true;
      } else if (type == DataComponents.POTION_DURATION_SCALE) {
         this.setPotionDurationScale(castComponentValue(DataComponents.POTION_DURATION_SCALE, value));
         return true;
      } else {
         return super.applyImplicitComponent(type, value);
      }
   }
}
