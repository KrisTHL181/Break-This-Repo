package net.minecraft.world.flag;

import com.mojang.serialization.Codec;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

public class FeatureFlags {
   public static final FeatureFlag VANILLA;
   public static final FeatureFlag TRADE_REBALANCE;
   public static final FeatureFlag REDSTONE_EXPERIMENTS;
   public static final FeatureFlag MINECART_IMPROVEMENTS;
   public static final FeatureFlagRegistry REGISTRY;
   public static final Codec<FeatureFlagSet> CODEC;
   public static final FeatureFlagSet VANILLA_SET;
   public static final FeatureFlagSet DEFAULT_FLAGS;

   public static String printMissingFlags(final FeatureFlagSet allowedFlags, final FeatureFlagSet requestedFlags) {
      return printMissingFlags(REGISTRY, allowedFlags, requestedFlags);
   }

   public static String printMissingFlags(final FeatureFlagRegistry registry, final FeatureFlagSet allowedFlags, final FeatureFlagSet requestedFlags) {
      Set<Identifier> requestedFlagIds = registry.toNames(requestedFlags);
      Set<Identifier> allowedFlagsIds = registry.toNames(allowedFlags);
      return requestedFlagIds.stream().filter(f -> !allowedFlagsIds.contains(f)).map(Identifier::toString).collect(Collectors.joining(", "));
   }

   public static boolean isExperimental(final FeatureFlagSet features) {
      return !features.isSubsetOf(VANILLA_SET);
   }

   static {
      FeatureFlagRegistry.Builder builder = new FeatureFlagRegistry.Builder("main");
      VANILLA = builder.createVanilla("vanilla");
      TRADE_REBALANCE = builder.createVanilla("trade_rebalance");
      REDSTONE_EXPERIMENTS = builder.createVanilla("redstone_experiments");
      MINECART_IMPROVEMENTS = builder.createVanilla("minecart_improvements");
      REGISTRY = builder.build();
      CODEC = REGISTRY.codec();
      VANILLA_SET = FeatureFlagSet.of(VANILLA);
      DEFAULT_FLAGS = VANILLA_SET;
   }
}
