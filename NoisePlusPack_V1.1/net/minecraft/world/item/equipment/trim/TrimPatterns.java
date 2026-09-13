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

   public static void bootstrap(BootstrapContext<TrimPattern> p_362921_) {
      register(p_362921_, SENTRY);
      register(p_362921_, DUNE);
      register(p_362921_, COAST);
      register(p_362921_, WILD);
      register(p_362921_, WARD);
      register(p_362921_, EYE);
      register(p_362921_, VEX);
      register(p_362921_, TIDE);
      register(p_362921_, SNOUT);
      register(p_362921_, RIB);
      register(p_362921_, SPIRE);
      register(p_362921_, WAYFINDER);
      register(p_362921_, SHAPER);
      register(p_362921_, SILENCE);
      register(p_362921_, RAISER);
      register(p_362921_, HOST);
      register(p_362921_, FLOW);
      register(p_362921_, BOLT);
   }

   public static void register(BootstrapContext<TrimPattern> p_363436_, ResourceKey<TrimPattern> p_366846_) {
      TrimPattern trimpattern = new TrimPattern(
         defaultAssetId(p_366846_), Component.translatable(Util.makeDescriptionId("trim_pattern", p_366846_.identifier())), false
      );
      p_363436_.register(p_366846_, trimpattern);
   }

   private static ResourceKey<TrimPattern> registryKey(String p_368467_) {
      return ResourceKey.create(Registries.TRIM_PATTERN, Identifier.withDefaultNamespace(p_368467_));
   }

   public static Identifier defaultAssetId(ResourceKey<TrimPattern> p_394517_) {
      return p_394517_.identifier();
   }
}
