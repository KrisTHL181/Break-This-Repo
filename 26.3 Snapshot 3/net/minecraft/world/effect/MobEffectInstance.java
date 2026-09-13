package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int INFINITE_DURATION = -1;
   public static final int MIN_AMPLIFIER = 0;
   public static final int MAX_AMPLIFIER = 255;
   public static final Codec<MobEffectInstance> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect), MobEffectInstance.Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)
         )
         .apply(i, MobEffectInstance::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MobEffectInstance> STREAM_CODEC = StreamCodec.composite(
      MobEffect.STREAM_CODEC, MobEffectInstance::getEffect, MobEffectInstance.Details.STREAM_CODEC, MobEffectInstance::asDetails, MobEffectInstance::new
   );
   private final Holder<MobEffect> effect;
   private int duration;
   private int amplifier;
   private boolean ambient;
   private boolean visible;
   private boolean showIcon;
   private @Nullable MobEffectInstance hiddenEffect;
   private final MobEffectInstance.BlendState blendState = new MobEffectInstance.BlendState();

   public MobEffectInstance(final Holder<MobEffect> effect) {
      this(effect, 0, 0);
   }

   public MobEffectInstance(final Holder<MobEffect> effect, final int duration) {
      this(effect, duration, 0);
   }

   public MobEffectInstance(final Holder<MobEffect> effect, final int duration, final int amplifier) {
      this(effect, duration, amplifier, false, true);
   }

   public MobEffectInstance(final Holder<MobEffect> effect, final int duration, final int amplifier, final boolean ambient, final boolean visible) {
      this(effect, duration, amplifier, ambient, visible, visible);
   }

   public MobEffectInstance(
      final Holder<MobEffect> effect, final int duration, final int amplifier, final boolean ambient, final boolean visible, final boolean showIcon
   ) {
      this(effect, duration, amplifier, ambient, visible, showIcon, null);
   }

   public MobEffectInstance(
      final Holder<MobEffect> effect,
      final int duration,
      final int amplifier,
      final boolean ambient,
      final boolean visible,
      final boolean showIcon,
      final @Nullable MobEffectInstance hiddenEffect
   ) {
      this.effect = effect;
      this.duration = duration;
      this.amplifier = Mth.clamp(amplifier, 0, 255);
      this.ambient = ambient;
      this.visible = visible;
      this.showIcon = showIcon;
      this.hiddenEffect = hiddenEffect;
   }

   public MobEffectInstance(final MobEffectInstance copy) {
      this.effect = copy.effect;
      this.setDetailsFrom(copy);
   }

   private MobEffectInstance(final Holder<MobEffect> effect, final MobEffectInstance.Details details) {
      this(
         effect,
         details.duration(),
         details.amplifier(),
         details.ambient(),
         details.showParticles(),
         details.showIcon(),
         details.hiddenEffect().map(hidden -> new MobEffectInstance(effect, hidden)).orElse(null)
      );
   }

   private MobEffectInstance.Details asDetails() {
      return new MobEffectInstance.Details(
         this.getAmplifier(),
         this.getDuration(),
         this.isAmbient(),
         this.isVisible(),
         this.showIcon(),
         Optional.ofNullable(this.hiddenEffect).map(MobEffectInstance::asDetails)
      );
   }

   public float getBlendFactor(final LivingEntity livingEntity, final float partialTickTime) {
      return this.blendState.getFactor(livingEntity, partialTickTime);
   }

   public ParticleOptions getParticleOptions() {
      return this.effect.value().createParticleOptions(this);
   }

   private void setDetailsFrom(final MobEffectInstance copy) {
      this.duration = copy.duration;
      this.amplifier = copy.amplifier;
      this.ambient = copy.ambient;
      this.visible = copy.visible;
      this.showIcon = copy.showIcon;
   }

   public boolean update(final MobEffectInstance takeOver) {
      if (!this.effect.equals(takeOver.effect)) {
         LOGGER.warn("This method should only be called for matching effects!");
      }

      boolean changed = false;
      if (takeOver.amplifier > this.amplifier) {
         if (takeOver.isShorterDurationThan(this)) {
            MobEffectInstance prevHiddenEffect = this.hiddenEffect;
            this.hiddenEffect = new MobEffectInstance(this);
            this.hiddenEffect.hiddenEffect = prevHiddenEffect;
         }

         this.amplifier = takeOver.amplifier;
         this.duration = takeOver.duration;
         changed = true;
      } else if (this.isShorterDurationThan(takeOver)) {
         if (takeOver.amplifier == this.amplifier) {
            this.duration = takeOver.duration;
            changed = true;
         } else if (this.hiddenEffect == null) {
            this.hiddenEffect = new MobEffectInstance(takeOver);
         } else {
            this.hiddenEffect.update(takeOver);
         }
      }

      if (!takeOver.ambient && this.ambient || changed) {
         this.ambient = takeOver.ambient;
         changed = true;
      }

      if (takeOver.visible != this.visible) {
         this.visible = takeOver.visible;
         changed = true;
      }

      if (takeOver.showIcon != this.showIcon) {
         this.showIcon = takeOver.showIcon;
         changed = true;
      }

      return changed;
   }

   private boolean isShorterDurationThan(final MobEffectInstance other) {
      return !this.isInfiniteDuration() && (this.duration < other.duration || other.isInfiniteDuration());
   }

   public boolean isInfiniteDuration() {
      return this.duration == -1;
   }

   public boolean endsWithin(final int ticks) {
      return !this.isInfiniteDuration() && this.duration <= ticks;
   }

   public MobEffectInstance withScaledDuration(final float scale) {
      MobEffectInstance copy = new MobEffectInstance(this);
      copy.duration = copy.mapDuration(duration -> Math.max(Mth.floor(duration * scale), 1));
      return copy;
   }

   public int mapDuration(final Int2IntFunction mapper) {
      return !this.isInfiniteDuration() && this.duration != 0 ? mapper.applyAsInt(this.duration) : this.duration;
   }

   public Holder<MobEffect> getEffect() {
      return this.effect;
   }

   public int getDuration() {
      return this.duration;
   }

   public int getAmplifier() {
      return this.amplifier;
   }

   public boolean isAmbient() {
      return this.ambient;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public boolean showIcon() {
      return this.showIcon;
   }

   public boolean tickServer(final ServerLevel serverLevel, final LivingEntity target, final Runnable onEffectUpdate) {
      if (!this.hasRemainingDuration()) {
         return false;
      }

      int tickCount = this.isInfiniteDuration() ? target.tickCount : this.duration;
      if (this.effect.value().shouldApplyEffectTickThisTick(tickCount, this.amplifier)
         && !this.effect.value().applyEffectTick(serverLevel, target, this.amplifier)) {
         return false;
      }

      this.tickDownDuration();
      if (this.downgradeToHiddenEffect()) {
         onEffectUpdate.run();
      }

      return this.hasRemainingDuration();
   }

   public void tickClient() {
      if (this.hasRemainingDuration()) {
         this.tickDownDuration();
         this.downgradeToHiddenEffect();
      }

      this.blendState.tick(this);
   }

   private boolean hasRemainingDuration() {
      return this.isInfiniteDuration() || this.duration > 0;
   }

   private void tickDownDuration() {
      if (this.hiddenEffect != null) {
         this.hiddenEffect.tickDownDuration();
      }

      this.duration = this.mapDuration(d -> d - 1);
   }

   private boolean downgradeToHiddenEffect() {
      if (this.duration == 0 && this.hiddenEffect != null) {
         this.setDetailsFrom(this.hiddenEffect);
         this.hiddenEffect = this.hiddenEffect.hiddenEffect;
         return true;
      } else {
         return false;
      }
   }

   public void onEffectStarted(final LivingEntity mob) {
      this.effect.value().onEffectStarted(mob, this.amplifier);
   }

   public void onMobRemoved(final ServerLevel level, final LivingEntity mob, final Entity.RemovalReason reason) {
      this.effect.value().onMobRemoved(level, mob, this.amplifier, reason);
   }

   public void onMobHurt(final ServerLevel level, final LivingEntity mob, final DamageSource source, final float damage) {
      this.effect.value().onMobHurt(level, mob, this.amplifier, source, damage);
   }

   public String getDescriptionId() {
      return this.effect.value().getDescriptionId();
   }

   @Override
   public String toString() {
      String result;
      if (this.amplifier > 0) {
         result = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
      } else {
         result = this.getDescriptionId() + ", Duration: " + this.describeDuration();
      }

      if (!this.visible) {
         result = result + ", Particles: false";
      }

      if (!this.showIcon) {
         result = result + ", Show Icon: false";
      }

      return result;
   }

   private String describeDuration() {
      return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof MobEffectInstance that)
            ? false
            : this.duration == that.duration
               && this.amplifier == that.amplifier
               && this.ambient == that.ambient
               && this.visible == that.visible
               && this.showIcon == that.showIcon
               && this.effect.equals(that.effect);
      }
   }

   @Override
   public int hashCode() {
      int result = this.effect.hashCode();
      result = 31 * result + this.duration;
      result = 31 * result + this.amplifier;
      result = 31 * result + (this.ambient ? 1 : 0);
      result = 31 * result + (this.visible ? 1 : 0);
      return 31 * result + (this.showIcon ? 1 : 0);
   }

   public int compareTo(final MobEffectInstance o) {
      int updateCutOff = 32147;
      return (this.getDuration() <= 32147 || o.getDuration() <= 32147) && (!this.isAmbient() || !o.isAmbient())
         ? ComparisonChain.start()
            .compareFalseFirst(this.isAmbient(), o.isAmbient())
            .compareFalseFirst(this.isInfiniteDuration(), o.isInfiniteDuration())
            .compare(this.getDuration(), o.getDuration())
            .compare(this.getEffect().value().getColor(), o.getEffect().value().getColor())
            .result()
         : ComparisonChain.start()
            .compare(this.isAmbient(), o.isAmbient())
            .compare(this.getEffect().value().getColor(), o.getEffect().value().getColor())
            .result();
   }

   public void onEffectAdded(final LivingEntity livingEntity) {
      this.effect.value().onEffectAdded(livingEntity, this.amplifier);
   }

   public boolean is(final Holder<MobEffect> effect) {
      return this.effect.equals(effect);
   }

   public void copyBlendState(final MobEffectInstance instance) {
      this.blendState.copyFrom(instance.blendState);
   }

   public void skipBlending() {
      this.blendState.setImmediate(this);
   }

   private static class BlendState {
      private float factor;
      private float factorPreviousFrame;

      public void setImmediate(final MobEffectInstance instance) {
         this.factor = hasEffect(instance) ? 1.0F : 0.0F;
         this.factorPreviousFrame = this.factor;
      }

      public void copyFrom(final MobEffectInstance.BlendState other) {
         this.factor = other.factor;
         this.factorPreviousFrame = other.factorPreviousFrame;
      }

      public void tick(final MobEffectInstance instance) {
         this.factorPreviousFrame = this.factor;
         boolean hasEffect = hasEffect(instance);
         float target = hasEffect ? 1.0F : 0.0F;
         if (this.factor != target) {
            MobEffect effect = instance.getEffect().value();
            int blendDuration = hasEffect ? effect.getBlendInDurationTicks() : effect.getBlendOutDurationTicks();
            if (blendDuration == 0) {
               this.factor = target;
            } else {
               float maxDeltaPerTick = 1.0F / blendDuration;
               this.factor = this.factor + Mth.clamp(target - this.factor, -maxDeltaPerTick, maxDeltaPerTick);
            }
         }
      }

      private static boolean hasEffect(final MobEffectInstance instance) {
         return !instance.endsWithin(instance.getEffect().value().getBlendOutAdvanceTicks());
      }

      public float getFactor(final LivingEntity livingEntity, final float partialTickTime) {
         if (livingEntity.isRemoved()) {
            this.factorPreviousFrame = this.factor;
         }

         return Mth.lerp(partialTickTime, this.factorPreviousFrame, this.factor);
      }
   }

   private record Details(
      int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect
   ) {
      public static final MapCodec<MobEffectInstance.Details> MAP_CODEC = MapCodec.recursive(
         "MobEffectInstance.Details",
         codec -> RecordCodecBuilder.mapCodec(
            i -> i.group(
                  ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance.Details::amplifier),
                  Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance.Details::duration),
                  Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance.Details::ambient),
                  Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance.Details::showParticles),
                  Codec.BOOL.optionalFieldOf("show_icon").forGetter(d -> Optional.of(d.showIcon())),
                  codec.optionalFieldOf("hidden_effect").forGetter(MobEffectInstance.Details::hiddenEffect)
               )
               .apply(i, MobEffectInstance.Details::create)
         )
      );
      public static final StreamCodec<ByteBuf, MobEffectInstance.Details> STREAM_CODEC = StreamCodec.recursive(
         subCodec -> StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MobEffectInstance.Details::amplifier,
            ByteBufCodecs.VAR_INT,
            MobEffectInstance.Details::duration,
            ByteBufCodecs.BOOL,
            MobEffectInstance.Details::ambient,
            ByteBufCodecs.BOOL,
            MobEffectInstance.Details::showParticles,
            ByteBufCodecs.BOOL,
            MobEffectInstance.Details::showIcon,
            subCodec.apply(ByteBufCodecs::optional),
            MobEffectInstance.Details::hiddenEffect,
            MobEffectInstance.Details::new
         )
      );

      private static MobEffectInstance.Details create(
         final int amplifier,
         final int duration,
         final boolean ambient,
         final boolean showParticles,
         final Optional<Boolean> showIcon,
         final Optional<MobEffectInstance.Details> hiddenEffect
      ) {
         return new MobEffectInstance.Details(amplifier, duration, ambient, showParticles, showIcon.orElse(showParticles), hiddenEffect);
      }
   }
}
