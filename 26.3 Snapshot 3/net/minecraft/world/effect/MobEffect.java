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

   protected MobEffect(final MobEffectCategory category, final int color) {
      this.category = category;
      this.color = color;
      this.particleFactory = effectInstance -> {
         int alpha = effectInstance.isAmbient() ? AMBIENT_ALPHA : 255;
         return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(alpha, color));
      };
   }

   protected MobEffect(final MobEffectCategory category, final int color, final ParticleOptions particleOptions) {
      this.category = category;
      this.color = color;
      this.particleFactory = ignored -> particleOptions;
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

   public boolean applyEffectTick(final ServerLevel serverLevel, final LivingEntity mob, final int amplification) {
      return true;
   }

   public void applyInstantaneousEffect(
      final ServerLevel level, final @Nullable Entity source, final @Nullable Entity owner, final LivingEntity mob, final int amplification, final double scale
   ) {
      this.applyEffectTick(level, mob, amplification);
   }

   public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
      return false;
   }

   public void onEffectStarted(final LivingEntity mob, final int amplifier) {
   }

   public void onEffectAdded(final LivingEntity mob, final int amplifier) {
      this.soundOnAdded.ifPresent(soundEvent -> mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), soundEvent, mob.getSoundSource(), 1.0F, 1.0F));
   }

   public void onMobRemoved(final ServerLevel level, final LivingEntity mob, final int amplifier, final Entity.RemovalReason reason) {
   }

   public void onMobHurt(final ServerLevel level, final LivingEntity mob, final int amplifier, final DamageSource source, final float damage) {
   }

   public boolean isInstantaneous() {
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

   public MobEffect addAttributeModifier(
      final Holder<Attribute> attribute, final Identifier id, final double amount, final AttributeModifier.Operation operation
   ) {
      this.attributeModifiers.put(attribute, new MobEffect.AttributeTemplate(id, amount, operation));
      return this;
   }

   public MobEffect setBlendDuration(final int ticks) {
      return this.setBlendDuration(ticks, ticks, ticks);
   }

   public MobEffect setBlendDuration(final int inTicks, final int outTicks, final int outAdvanceTicks) {
      this.blendInDurationTicks = inTicks;
      this.blendOutDurationTicks = outTicks;
      this.blendOutAdvanceTicks = outAdvanceTicks;
      return this;
   }

   public void createModifiers(final int amplifier, final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
      this.attributeModifiers.forEach((attribute, template) -> consumer.accept((Holder<Attribute>)attribute, template.create(amplifier)));
   }

   public void removeAttributeModifiers(final AttributeMap attributes) {
      for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attribute = attributes.getInstance(entry.getKey());
         if (attribute != null) {
            attribute.removeModifier(entry.getValue().id());
         }
      }
   }

   public void addAttributeModifiers(final AttributeMap attributes, final int amplifier) {
      for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
         AttributeInstance attribute = attributes.getInstance(entry.getKey());
         if (attribute != null) {
            attribute.removeModifier(entry.getValue().id());
            attribute.addPermanentModifier(entry.getValue().create(amplifier));
         }
      }
   }

   public boolean isBeneficial() {
      return this.category == MobEffectCategory.BENEFICIAL;
   }

   public ParticleOptions createParticleOptions(final MobEffectInstance mobEffectInstance) {
      return this.particleFactory.apply(mobEffectInstance);
   }

   public MobEffect withSoundOnAdded(final SoundEvent soundEvent) {
      this.soundOnAdded = Optional.of(soundEvent);
      return this;
   }

   public MobEffect requiredFeatures(final FeatureFlag... flags) {
      this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
      return this;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   private record AttributeTemplate(Identifier id, double amount, AttributeModifier.Operation operation) {
      public AttributeModifier create(final int amplifier) {
         return new AttributeModifier(this.id, this.amount * (amplifier + 1), this.operation);
      }
   }
}
