package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

public class DecoratedPotPatterns {
   public static final ResourceKey<DecoratedPotPattern> ANGLER = create("angler");
   public static final ResourceKey<DecoratedPotPattern> ARCHER = create("archer");
   public static final ResourceKey<DecoratedPotPattern> ARMS_UP = create("arms_up");
   public static final ResourceKey<DecoratedPotPattern> BLADE = create("blade");
   public static final ResourceKey<DecoratedPotPattern> BREWER = create("brewer");
   public static final ResourceKey<DecoratedPotPattern> BURN = create("burn");
   public static final ResourceKey<DecoratedPotPattern> DANGER = create("danger");
   public static final ResourceKey<DecoratedPotPattern> EXPLORER = create("explorer");
   public static final ResourceKey<DecoratedPotPattern> FLOW = create("flow");
   public static final ResourceKey<DecoratedPotPattern> FRIEND = create("friend");
   public static final ResourceKey<DecoratedPotPattern> GUSTER = create("guster");
   public static final ResourceKey<DecoratedPotPattern> HEART = create("heart");
   public static final ResourceKey<DecoratedPotPattern> HEARTBREAK = create("heartbreak");
   public static final ResourceKey<DecoratedPotPattern> HOWL = create("howl");
   public static final ResourceKey<DecoratedPotPattern> MINER = create("miner");
   public static final ResourceKey<DecoratedPotPattern> MOURNER = create("mourner");
   public static final ResourceKey<DecoratedPotPattern> PLENTY = create("plenty");
   public static final ResourceKey<DecoratedPotPattern> PRIZE = create("prize");
   public static final ResourceKey<DecoratedPotPattern> SCRAPE = create("scrape");
   public static final ResourceKey<DecoratedPotPattern> SHEAF = create("sheaf");
   public static final ResourceKey<DecoratedPotPattern> SHELTER = create("shelter");
   public static final ResourceKey<DecoratedPotPattern> SKULL = create("skull");
   public static final ResourceKey<DecoratedPotPattern> SNORT = create("snort");
   public static final Codec<Holder<DecoratedPotPattern>> CODEC = RegistryFixedCodec.create(Registries.DECORATED_POT_PATTERN);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DecoratedPotPattern>> STREAM_CODEC = ByteBufCodecs.holderRegistry(
      Registries.DECORATED_POT_PATTERN
   );

   private static ResourceKey<DecoratedPotPattern> create(final String id) {
      return ResourceKey.create(Registries.DECORATED_POT_PATTERN, Identifier.withDefaultNamespace(id));
   }

   public static void bootstrap(final BootstrapContext<DecoratedPotPattern> registry) {
      registerWithDefaultAsset(registry, ANGLER);
      registerWithDefaultAsset(registry, ARCHER);
      registerWithDefaultAsset(registry, ARMS_UP);
      registerWithDefaultAsset(registry, BLADE);
      registerWithDefaultAsset(registry, BREWER);
      registerWithDefaultAsset(registry, BURN);
      registerWithDefaultAsset(registry, DANGER);
      registerWithDefaultAsset(registry, EXPLORER);
      registerWithDefaultAsset(registry, FLOW);
      registerWithDefaultAsset(registry, FRIEND);
      registerWithDefaultAsset(registry, GUSTER);
      registerWithDefaultAsset(registry, HEART);
      registerWithDefaultAsset(registry, HEARTBREAK);
      registerWithDefaultAsset(registry, HOWL);
      registerWithDefaultAsset(registry, MINER);
      registerWithDefaultAsset(registry, MOURNER);
      registerWithDefaultAsset(registry, PLENTY);
      registerWithDefaultAsset(registry, PRIZE);
      registerWithDefaultAsset(registry, SCRAPE);
      registerWithDefaultAsset(registry, SHEAF);
      registerWithDefaultAsset(registry, SHELTER);
      registerWithDefaultAsset(registry, SKULL);
      registerWithDefaultAsset(registry, SNORT);
   }

   private static void registerWithDefaultAsset(final BootstrapContext<DecoratedPotPattern> registry, final ResourceKey<DecoratedPotPattern> key) {
      registry.register(key, new DecoratedPotPattern(key.identifier().withSuffix("_pottery_pattern")));
   }
}
