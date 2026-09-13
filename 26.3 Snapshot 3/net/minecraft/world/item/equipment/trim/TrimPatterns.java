package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

public class TrimPatterns {
   public static final ResourceKey<TrimPattern> SENTRY = registryKey("sentry");
   public static final ResourceKey<TrimPattern> DUNE = registryKey("dune");
   public static final ResourceKey<TrimPattern> COAST = registryKey("coast");
   public static final ResourceKey<TrimPattern> WILD = registryKey("wild");
   public static final ResourceKey<TrimPattern> WARD = registryKey("ward");
   public static final ResourceKey<TrimPattern> EYE = registryKey("eye");
   public static final ResourceKey<TrimPattern> VEX = registryKey("vex");
   public static final ResourceKey<TrimPattern> TIDE = registryKey("tide");
   public static final ResourceKey<TrimPattern> SNOUT = registryKey("snout");
   public static final ResourceKey<TrimPattern> RIB = registryKey("rib");
   public static final ResourceKey<TrimPattern> SPIRE = registryKey("spire");
   public static final ResourceKey<TrimPattern> WAYFINDER = registryKey("wayfinder");
   public static final ResourceKey<TrimPattern> SHAPER = registryKey("shaper");
   public static final ResourceKey<TrimPattern> SILENCE = registryKey("silence");
   public static final ResourceKey<TrimPattern> RAISER = registryKey("raiser");
   public static final ResourceKey<TrimPattern> HOST = registryKey("host");
   public static final ResourceKey<TrimPattern> FLOW = registryKey("flow");
   public static final ResourceKey<TrimPattern> BOLT = registryKey("bolt");

   public static void bootstrap(final BootstrapContext<TrimPattern> context) {
      register(context, SENTRY);
      register(context, DUNE);
      register(context, COAST);
      register(context, WILD);
      register(context, WARD);
      register(context, EYE);
      register(context, VEX);
      register(context, TIDE);
      register(context, SNOUT);
      register(context, RIB);
      register(context, SPIRE);
      register(context, WAYFINDER);
      register(context, SHAPER);
      register(context, SILENCE);
      register(context, RAISER);
      register(context, HOST);
      register(context, FLOW);
      register(context, BOLT);
   }

   public static void register(final BootstrapContext<TrimPattern> context, final ResourceKey<TrimPattern> registryKey) {
      TrimPattern pattern = new TrimPattern(
         defaultAssetId(registryKey), Component.translatable(Util.makeDescriptionId("trim_pattern", registryKey.identifier())), false
      );
      context.register(registryKey, pattern);
   }

   private static ResourceKey<TrimPattern> registryKey(final String id) {
      return ResourceKey.create(Registries.TRIM_PATTERN, Identifier.withDefaultNamespace(id));
   }

   public static Identifier defaultAssetId(final ResourceKey<TrimPattern> registryKey) {
      return registryKey.identifier();
   }
}
