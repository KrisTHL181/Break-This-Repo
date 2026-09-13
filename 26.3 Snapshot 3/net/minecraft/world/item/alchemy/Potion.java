package net.minecraft.world.item.alchemy;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class Potion implements FeatureElement {
   public static final Codec<Holder<Potion>> CODEC = BuiltInRegistries.POTION.holderByNameCodec();
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Potion>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.POTION);
   private final String name;
   private final List<MobEffectInstance> effects;
   private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;

   public Potion(final String name, final MobEffectInstance... effects) {
      this.name = name;
      this.effects = List.of(effects);
   }

   public Potion requiredFeatures(final FeatureFlag... flags) {
      this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
      return this;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   public List<MobEffectInstance> getEffects() {
      return this.effects;
   }

   public String name() {
      return this.name;
   }

   public boolean hasInstantEffects() {
      for (MobEffectInstance effect : this.effects) {
         if (effect.getEffect().value().isInstantaneous()) {
            return true;
         }
      }

      return false;
   }
}
