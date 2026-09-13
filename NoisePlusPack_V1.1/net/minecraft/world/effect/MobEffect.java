package net.minecraft.world.effect;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;

public class MobEffect implements FeatureElement {
   public static final Codec<Holder<MobEffect>> CODEC = BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);
   private static final int AMBIENT_ALPHA = Mth.floor(38.25F);
   private final Map<Holder<Attribute>, MobEffect.AttributeTemplate> attributeModifiers = new Object2ObjectOpenHashMap();
   private final MobEffectCategory category;
   private final int color;
   private final Function<MobEffectInstance, ParticleOptions> particleFactory;
   private @Nullable String descriptionId;
   private int blendInDurationTicks;
   private int blendOutDurationTicks;
   private int blendOutAdvanceTicks;
   private Optional<SoundEvent> soundOnAdded = Optional.empty();
   private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;

   protected MobEffect(MobEffectCategory p_19451_, int p_19452_) {
      this.category = p_19451_;
      this.color = p_19452_;
      this.particleFactory = p_358853_ -> {
         int i = p_358853_.isAmbient() ? AMBIENT_ALPHA : 255;
         return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(i, p_19452_));
      };
   }

   protected MobEffect(MobEffectCategory p_335432_, int p_334901_, ParticleOptions p_331136_) {
      this.category = p_335432_;
      this.color = p_334901_;
      this.particleFactory = p_326745_ -> p_331136_;
   }

   public int getBlendInDurationTicks() {
      return this.blendInDurationTicks;
   }

   public int getBlendOutDurationTicks() {
      return this.blendOutDurationTicks;
   }

   public int getBlendOutAdvanceTicks() {
      return this.blendOutAdvanceTicks;
   }

   public boolean applyEffectTick(ServerLevel p_361900_, LivingEntity p_333541_, int p_333570_) {
      return true;
   }

   public void applyInstantenousEffect(
      ServerLevel p_369379_, @Nullable Entity p_19462_, @Nullable Entity p_19463_, LivingEntity p_19464_, int p_19465_, double p_19466_
   ) {
      this.applyEffectTick(p_369379_, p_19464_, p_19465_);
   }

   public boolean shouldApplyEffectTickThisTick(int p_297908_, int p_301085_) {
      return false;
   }

   public void onEffectStarted(LivingEntity p_299085_, int p_297449_) {
   }

   public void onEffectAdded(LivingEntity p_335100_, int p_336309_) {
      this.soundOnAdded
         .ifPresent(
            p_449379_ -> p_335100_.level()
               .playSound(null, p_335100_.getX(), p_335100_.getY(), p_335100_.getZ(), p_449379_, p_335100_.getSoundSource(), 1.0F, 1.0F)
         );
   }

   public void onMobRemoved(ServerLevel p_368376_, LivingEntity p_335815_, int p_328980_, Entity.RemovalReason p_328413_) {
   }

   public void onMobHurt(ServerLevel p_367917_, LivingEntity p_19467_, int p_19468_, DamageSource p_334111_, float p_330556_) {
   }

   public boolean isInstantenous() {
      return false;
   }

   protected String getOrCreateDescriptionId() {
      if (this.descriptionId == null) {
         this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
      }

      return this.descriptionId;
   }

   public String getDescriptionId() {
      return this.getOrCreateDescriptionId();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getDescriptionId());
   }

   public MobEffectCategory getCategory() {
      return this.category;
   }

   public int getColor() {
      return this.color;
   }

   public MobEffect addAttributeModifier(Holder<Attribute> p_332101_, Identifier p_457796_, double p_19475_, AttributeModifier.Operation p_19476_) {
      this.attributeModifiers.put(p_332101_, new MobEffect.AttributeTemplate(p_457796_, p_19475_, p_19476_));
      return this;
   }

   public MobEffect setBlendDuration(int p_328727_) {
      return this.setBlendDuration(p_328727_, p_328727_, p_328727_);
   }

   public MobEffect setBlendDuration(int p_392325_, int p_393853_, int p_392943_) {
      this.blendInDurationTicks = p_392325_;
      this.blendOutDurationTicks = p_393853_;
      this.blendOutAdvanceTicks = p_392943_;
      return this;
   }

   public void createModifiers(int p_334564_, BiConsumer<Holder<Attribute>, AttributeModifier> p_333602_) {
      this.attributeModifiers.forEach((p_341255_, p_341256_) -> p_333602_.accept((Holder<Attribute>)p_341255_, p_341256_.create(p_334564_)));
   }

   public void removeAttributeModifiers(AttributeMap p_19470_) {
      for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attributeinstance = p_19470_.getInstance(entry.getKey());
         if (attributeinstance != null) {
            attributeinstance.removeModifier(entry.getValue().id());
         }
      }
   }

   public void addAttributeModifiers(AttributeMap p_19479_, int p_19480_) {
      for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attributeinstance = p_19479_.getInstance(entry.getKey());
         if (attributeinstance != null) {
            attributeinstance.removeModifier(entry.getValue().id());
            attributeinstance.addPermanentModifier(entry.getValue().create(p_19480_));
         }
      }
   }

   public boolean isBeneficial() {
      return this.category == MobEffectCategory.BENEFICIAL;
   }

   public ParticleOptions createParticleOptions(MobEffectInstance p_332465_) {
      return this.particleFactory.apply(p_332465_);
   }

   public MobEffect withSoundOnAdded(SoundEvent p_329951_) {
      this.soundOnAdded = Optional.of(p_329951_);
      return this;
   }

   public MobEffect requiredFeatures(FeatureFlag... p_329270_) {
      this.requiredFeatures = FeatureFlags.REGISTRY.subset(p_329270_);
      return this;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   record AttributeTemplate(Identifier id, double amount, AttributeModifier.Operation operation) {
      public AttributeModifier create(int p_332230_) {
         return new AttributeModifier(this.id, this.amount * (p_332230_ + 1), this.operation);
      }
   }
}
