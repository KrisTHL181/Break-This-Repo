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
      p_341259_ -> p_341259_.group(
            MobEffect.CODEC.fieldOf("id").forGetter(MobEffectInstance::getEffect), MobEffectInstance.Details.MAP_CODEC.forGetter(MobEffectInstance::asDetails)
         )
         .apply(p_341259_, MobEffectInstance::new)
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

   public MobEffectInstance(Holder<MobEffect> p_333937_) {
      this(p_333937_, 0, 0);
   }

   public MobEffectInstance(Holder<MobEffect> p_332556_, int p_19523_) {
      this(p_332556_, p_19523_, 0);
   }

   public MobEffectInstance(Holder<MobEffect> p_334453_, int p_328066_, int p_330997_) {
      this(p_334453_, p_328066_, p_330997_, false, true);
   }

   public MobEffectInstance(Holder<MobEffect> p_327781_, int p_19529_, int p_19530_, boolean p_19531_, boolean p_19532_) {
      this(p_327781_, p_19529_, p_19530_, p_19531_, p_19532_, p_19532_);
   }

   public MobEffectInstance(Holder<MobEffect> p_333122_, int p_216888_, int p_216889_, boolean p_216890_, boolean p_216891_, boolean p_216892_) {
      this(p_333122_, p_216888_, p_216889_, p_216890_, p_216891_, p_216892_, null);
   }

   public MobEffectInstance(
      Holder<MobEffect> p_334558_, int p_19519_, int p_19520_, boolean p_332448_, boolean p_327855_, boolean p_334281_, @Nullable MobEffectInstance p_332569_
   ) {
      this.effect = p_334558_;
      this.duration = p_19519_;
      this.amplifier = Mth.clamp(p_19520_, 0, 255);
      this.ambient = p_332448_;
      this.visible = p_327855_;
      this.showIcon = p_334281_;
      this.hiddenEffect = p_332569_;
   }

   public MobEffectInstance(MobEffectInstance p_19543_) {
      this.effect = p_19543_.effect;
      this.setDetailsFrom(p_19543_);
   }

   private MobEffectInstance(Holder<MobEffect> p_330051_, MobEffectInstance.Details p_332322_) {
      this(
         p_330051_,
         p_332322_.duration(),
         p_332322_.amplifier(),
         p_332322_.ambient(),
         p_332322_.showParticles(),
         p_332322_.showIcon(),
         p_332322_.hiddenEffect().map(p_326756_ -> new MobEffectInstance(p_330051_, p_326756_)).orElse(null)
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

   public float getBlendFactor(LivingEntity p_333473_, float p_327866_) {
      return this.blendState.getFactor(p_333473_, p_327866_);
   }

   public ParticleOptions getParticleOptions() {
      return this.effect.value().createParticleOptions(this);
   }

   void setDetailsFrom(MobEffectInstance p_19549_) {
      this.duration = p_19549_.duration;
      this.amplifier = p_19549_.amplifier;
      this.ambient = p_19549_.ambient;
      this.visible = p_19549_.visible;
      this.showIcon = p_19549_.showIcon;
   }

   public boolean update(MobEffectInstance p_19559_) {
      if (!this.effect.equals(p_19559_.effect)) {
         LOGGER.warn("This method should only be called for matching effects!");
      }

      boolean flag = false;
      if (p_19559_.amplifier > this.amplifier) {
         if (p_19559_.isShorterDurationThan(this)) {
            MobEffectInstance mobeffectinstance = this.hiddenEffect;
            this.hiddenEffect = new MobEffectInstance(this);
            this.hiddenEffect.hiddenEffect = mobeffectinstance;
         }

         this.amplifier = p_19559_.amplifier;
         this.duration = p_19559_.duration;
         flag = true;
      } else if (this.isShorterDurationThan(p_19559_)) {
         if (p_19559_.amplifier == this.amplifier) {
            this.duration = p_19559_.duration;
            flag = true;
         } else if (this.hiddenEffect == null) {
            this.hiddenEffect = new MobEffectInstance(p_19559_);
         } else {
            this.hiddenEffect.update(p_19559_);
         }
      }

      if (!p_19559_.ambient && this.ambient || flag) {
         this.ambient = p_19559_.ambient;
         flag = true;
      }

      if (p_19559_.visible != this.visible) {
         this.visible = p_19559_.visible;
         flag = true;
      }

      if (p_19559_.showIcon != this.showIcon) {
         this.showIcon = p_19559_.showIcon;
         flag = true;
      }

      return flag;
   }

   private boolean isShorterDurationThan(MobEffectInstance p_268133_) {
      return !this.isInfiniteDuration() && (this.duration < p_268133_.duration || p_268133_.isInfiniteDuration());
   }

   public boolean isInfiniteDuration() {
      return this.duration == -1;
   }

   public boolean endsWithin(int p_268088_) {
      return !this.isInfiniteDuration() && this.duration <= p_268088_;
   }

   public MobEffectInstance withScaledDuration(float p_395912_) {
      MobEffectInstance mobeffectinstance = new MobEffectInstance(this);
      mobeffectinstance.duration = mobeffectinstance.mapDuration(p_390476_ -> Math.max(Mth.floor(p_390476_ * p_395912_), 1));
      return mobeffectinstance;
   }

   public int mapDuration(Int2IntFunction p_268089_) {
      return !this.isInfiniteDuration() && this.duration != 0 ? p_268089_.applyAsInt(this.duration) : this.duration;
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

   public boolean tickServer(ServerLevel p_396790_, LivingEntity p_395235_, Runnable p_391834_) {
      if (!this.hasRemainingDuration()) {
         return false;
      }

      int i = this.isInfiniteDuration() ? p_395235_.tickCount : this.duration;
      if (this.effect.value().shouldApplyEffectTickThisTick(i, this.amplifier) && !this.effect.value().applyEffectTick(p_396790_, p_395235_, this.amplifier)) {
         return false;
      }

      this.tickDownDuration();
      if (this.downgradeToHiddenEffect()) {
         p_391834_.run();
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

      this.duration = this.mapDuration(p_267916_ -> p_267916_ - 1);
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

   public void onEffectStarted(LivingEntity p_297679_) {
      this.effect.value().onEffectStarted(p_297679_, this.amplifier);
   }

   public void onMobRemoved(ServerLevel p_364127_, LivingEntity p_329318_, Entity.RemovalReason p_333232_) {
      this.effect.value().onMobRemoved(p_364127_, p_329318_, this.amplifier, p_333232_);
   }

   public void onMobHurt(ServerLevel p_361358_, LivingEntity p_327684_, DamageSource p_328403_, float p_331463_) {
      this.effect.value().onMobHurt(p_361358_, p_327684_, this.amplifier, p_328403_, p_331463_);
   }

   public String getDescriptionId() {
      return this.effect.value().getDescriptionId();
   }

   @Override
   public String toString() {
      String s;
      if (this.amplifier > 0) {
         s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
      } else {
         s = this.getDescriptionId() + ", Duration: " + this.describeDuration();
      }

      if (!this.visible) {
         s = s + ", Particles: false";
      }

      if (!this.showIcon) {
         s = s + ", Show Icon: false";
      }

      return s;
   }

   private String describeDuration() {
      return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
   }

   @Override
   public boolean equals(Object p_19574_) {
      if (this == p_19574_) {
         return true;
      } else {
         return p_19574_ instanceof MobEffectInstance mobeffectinstance
            ? this.duration == mobeffectinstance.duration
               && this.amplifier == mobeffectinstance.amplifier
               && this.ambient == mobeffectinstance.ambient
               && this.visible == mobeffectinstance.visible
               && this.showIcon == mobeffectinstance.showIcon
               && this.effect.equals(mobeffectinstance.effect)
            : false;
      }
   }

   @Override
   public int hashCode() {
      int i = this.effect.hashCode();
      i = 31 * i + this.duration;
      i = 31 * i + this.amplifier;
      i = 31 * i + (this.ambient ? 1 : 0);
      i = 31 * i + (this.visible ? 1 : 0);
      return 31 * i + (this.showIcon ? 1 : 0);
   }

   public int compareTo(MobEffectInstance p_19566_) {
      int i = 32147;
      return this.getDuration() > 32147 && p_19566_.getDuration() > 32147 || this.isAmbient() && p_19566_.isAmbient()
         ? ComparisonChain.start()
            .compare(this.isAmbient(), p_19566_.isAmbient())
            .compare(this.getEffect().value().getColor(), p_19566_.getEffect().value().getColor())
            .result()
         : ComparisonChain.start()
            .compareFalseFirst(this.isAmbient(), p_19566_.isAmbient())
            .compareFalseFirst(this.isInfiniteDuration(), p_19566_.isInfiniteDuration())
            .compare(this.getDuration(), p_19566_.getDuration())
            .compare(this.getEffect().value().getColor(), p_19566_.getEffect().value().getColor())
            .result();
   }

   public void onEffectAdded(LivingEntity p_334348_) {
      this.effect.value().onEffectAdded(p_334348_, this.amplifier);
   }

   public boolean is(Holder<MobEffect> p_329529_) {
      return this.effect.equals(p_329529_);
   }

   public void copyBlendState(MobEffectInstance p_335404_) {
      this.blendState.copyFrom(p_335404_.blendState);
   }

   public void skipBlending() {
      this.blendState.setImmediate(this);
   }

   static class BlendState {
      private float factor;
      private float factorPreviousFrame;

      public void setImmediate(MobEffectInstance p_333918_) {
         this.factor = hasEffect(p_333918_) ? 1.0F : 0.0F;
         this.factorPreviousFrame = this.factor;
      }

      public void copyFrom(MobEffectInstance.BlendState p_327821_) {
         this.factor = p_327821_.factor;
         this.factorPreviousFrame = p_327821_.factorPreviousFrame;
      }

      public void tick(MobEffectInstance p_330345_) {
         this.factorPreviousFrame = this.factor;
         boolean flag = hasEffect(p_330345_);
         float f = flag ? 1.0F : 0.0F;
         if (this.factor != f) {
            MobEffect mobeffect = p_330345_.getEffect().value();
            int i = flag ? mobeffect.getBlendInDurationTicks() : mobeffect.getBlendOutDurationTicks();
            if (i == 0) {
               this.factor = f;
            } else {
               float f1 = 1.0F / i;
               this.factor = this.factor + Mth.clamp(f - this.factor, -f1, f1);
            }
         }
      }

      private static boolean hasEffect(MobEffectInstance p_394489_) {
         return !p_394489_.endsWithin(p_394489_.getEffect().value().getBlendOutAdvanceTicks());
      }

      public float getFactor(LivingEntity p_333208_, float p_330792_) {
         if (p_333208_.isRemoved()) {
            this.factorPreviousFrame = this.factor;
         }

         return Mth.lerp(p_330792_, this.factorPreviousFrame, this.factor);
      }
   }

   record Details(int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, Optional<MobEffectInstance.Details> hiddenEffect) {
      public static final MapCodec<MobEffectInstance.Details> MAP_CODEC = MapCodec.recursive(
         "MobEffectInstance.Details",
         p_332855_ -> RecordCodecBuilder.mapCodec(
            p_327980_ -> p_327980_.group(
                  ExtraCodecs.UNSIGNED_BYTE.optionalFieldOf("amplifier", 0).forGetter(MobEffectInstance.Details::amplifier),
                  Codec.INT.optionalFieldOf("duration", 0).forGetter(MobEffectInstance.Details::duration),
                  Codec.BOOL.optionalFieldOf("ambient", false).forGetter(MobEffectInstance.Details::ambient),
                  Codec.BOOL.optionalFieldOf("show_particles", true).forGetter(MobEffectInstance.Details::showParticles),
                  Codec.BOOL.optionalFieldOf("show_icon").forGetter(p_330483_ -> Optional.of(p_330483_.showIcon())),
                  p_332855_.optionalFieldOf("hidden_effect").forGetter(MobEffectInstance.Details::hiddenEffect)
               )
               .apply(p_327980_, MobEffectInstance.Details::create)
         )
      );
      public static final StreamCodec<ByteBuf, MobEffectInstance.Details> STREAM_CODEC = StreamCodec.recursive(
         p_333279_ -> StreamCodec.composite(
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
            p_333279_.apply(ByteBufCodecs::optional),
            MobEffectInstance.Details::hiddenEffect,
            MobEffectInstance.Details::new
         )
      );

      private static MobEffectInstance.Details create(
         int p_334251_, int p_332882_, boolean p_330487_, boolean p_334607_, Optional<Boolean> p_329280_, Optional<MobEffectInstance.Details> p_330477_
      ) {
         return new MobEffectInstance.Details(p_334251_, p_332882_, p_330487_, p_334607_, p_329280_.orElse(p_334607_), p_330477_);
      }
   }
}
