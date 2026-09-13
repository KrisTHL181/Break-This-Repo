package net.minecraft.world.item.alchemy;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PotionIds {
   public static final ResourceKey<Potion> WATER = register("water");
   public static final ResourceKey<Potion> MUNDANE = register("mundane");
   public static final ResourceKey<Potion> THICK = register("thick");
   public static final ResourceKey<Potion> AWKWARD = register("awkward");
   public static final ResourceKey<Potion> NIGHT_VISION = register("night_vision");
   public static final ResourceKey<Potion> LONG_NIGHT_VISION = register("long_night_vision");
   public static final ResourceKey<Potion> INVISIBILITY = register("invisibility");
   public static final ResourceKey<Potion> LONG_INVISIBILITY = register("long_invisibility");
   public static final ResourceKey<Potion> LEAPING = register("leaping");
   public static final ResourceKey<Potion> LONG_LEAPING = register("long_leaping");
   public static final ResourceKey<Potion> STRONG_LEAPING = register("strong_leaping");
   public static final ResourceKey<Potion> FIRE_RESISTANCE = register("fire_resistance");
   public static final ResourceKey<Potion> LONG_FIRE_RESISTANCE = register("long_fire_resistance");
   public static final ResourceKey<Potion> SWIFTNESS = register("swiftness");
   public static final ResourceKey<Potion> LONG_SWIFTNESS = register("long_swiftness");
   public static final ResourceKey<Potion> STRONG_SWIFTNESS = register("strong_swiftness");
   public static final ResourceKey<Potion> SLOWNESS = register("slowness");
   public static final ResourceKey<Potion> LONG_SLOWNESS = register("long_slowness");
   public static final ResourceKey<Potion> STRONG_SLOWNESS = register("strong_slowness");
   public static final ResourceKey<Potion> TURTLE_MASTER = register("turtle_master");
   public static final ResourceKey<Potion> LONG_TURTLE_MASTER = register("long_turtle_master");
   public static final ResourceKey<Potion> STRONG_TURTLE_MASTER = register("strong_turtle_master");
   public static final ResourceKey<Potion> WATER_BREATHING = register("water_breathing");
   public static final ResourceKey<Potion> LONG_WATER_BREATHING = register("long_water_breathing");
   public static final ResourceKey<Potion> HEALING = register("healing");
   public static final ResourceKey<Potion> STRONG_HEALING = register("strong_healing");
   public static final ResourceKey<Potion> HARMING = register("harming");
   public static final ResourceKey<Potion> STRONG_HARMING = register("strong_harming");
   public static final ResourceKey<Potion> POISON = register("poison");
   public static final ResourceKey<Potion> LONG_POISON = register("long_poison");
   public static final ResourceKey<Potion> STRONG_POISON = register("strong_poison");
   public static final ResourceKey<Potion> REGENERATION = register("regeneration");
   public static final ResourceKey<Potion> LONG_REGENERATION = register("long_regeneration");
   public static final ResourceKey<Potion> STRONG_REGENERATION = register("strong_regeneration");
   public static final ResourceKey<Potion> STRENGTH = register("strength");
   public static final ResourceKey<Potion> LONG_STRENGTH = register("long_strength");
   public static final ResourceKey<Potion> STRONG_STRENGTH = register("strong_strength");
   public static final ResourceKey<Potion> WEAKNESS = register("weakness");
   public static final ResourceKey<Potion> LONG_WEAKNESS = register("long_weakness");
   public static final ResourceKey<Potion> LUCK = register("luck");
   public static final ResourceKey<Potion> SLOW_FALLING = register("slow_falling");
   public static final ResourceKey<Potion> LONG_SLOW_FALLING = register("long_slow_falling");
   public static final ResourceKey<Potion> WIND_CHARGED = register("wind_charged");
   public static final ResourceKey<Potion> WEAVING = register("weaving");
   public static final ResourceKey<Potion> OOZING = register("oozing");
   public static final ResourceKey<Potion> INFESTED = register("infested");

   private static ResourceKey<Potion> register(final String name) {
      return ResourceKey.create(Registries.POTION, Identifier.withDefaultNamespace(name));
   }
}
