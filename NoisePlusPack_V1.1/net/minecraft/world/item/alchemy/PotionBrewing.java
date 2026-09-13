package net.minecraft.world.item.alchemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;

public class PotionBrewing {
   public static final int BREWING_TIME_SECONDS = 20;
   public static final PotionBrewing EMPTY = new PotionBrewing(List.of(), List.of(), List.of());
   private final List<Ingredient> containers;
   private final List<PotionBrewing.Mix<Potion>> potionMixes;
   private final List<PotionBrewing.Mix<Item>> containerMixes;

   PotionBrewing(List<Ingredient> p_331253_, List<PotionBrewing.Mix<Potion>> p_333814_, List<PotionBrewing.Mix<Item>> p_332419_) {
      this.containers = p_331253_;
      this.potionMixes = p_333814_;
      this.containerMixes = p_332419_;
   }

   public boolean isIngredient(ItemStack p_43507_) {
      return this.isContainerIngredient(p_43507_) || this.isPotionIngredient(p_43507_);
   }

   private boolean isContainer(ItemStack p_328293_) {
      for (Ingredient ingredient : this.containers) {
         if (ingredient.test(p_328293_)) {
            return true;
         }
      }

      return false;
   }

   public boolean isContainerIngredient(ItemStack p_43518_) {
      for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
         if (mix.ingredient.test(p_43518_)) {
            return true;
         }
      }

      return false;
   }

   public boolean isPotionIngredient(ItemStack p_43523_) {
      for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
         if (mix.ingredient.test(p_43523_)) {
            return true;
         }
      }

      return false;
   }

   public boolean isBrewablePotion(Holder<Potion> p_330984_) {
      for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
         if (mix.to.is(p_330984_)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasMix(ItemStack p_43509_, ItemStack p_43510_) {
      return !this.isContainer(p_43509_) ? false : this.hasContainerMix(p_43509_, p_43510_) || this.hasPotionMix(p_43509_, p_43510_);
   }

   public boolean hasContainerMix(ItemStack p_43520_, ItemStack p_43521_) {
      for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
         if (p_43520_.is(mix.from) && mix.ingredient.test(p_43521_)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasPotionMix(ItemStack p_43525_, ItemStack p_43526_) {
      Optional<Holder<Potion>> optional = p_43525_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
      if (optional.isEmpty()) {
         return false;
      }

      for (PotionBrewing.Mix<Potion> mix : this.potionMixes) {
         if (mix.from.is(optional.get()) && mix.ingredient.test(p_43526_)) {
            return true;
         }
      }

      return false;
   }

   public ItemStack mix(ItemStack p_43530_, ItemStack p_43531_) {
      if (p_43531_.isEmpty()) {
         return p_43531_;
      }

      Optional<Holder<Potion>> optional = p_43531_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
      if (optional.isEmpty()) {
         return p_43531_;
      }

      for (PotionBrewing.Mix<Item> mix : this.containerMixes) {
         if (p_43531_.is(mix.from) && mix.ingredient.test(p_43530_)) {
            return PotionContents.createItemStack(mix.to.value(), optional.get());
         }
      }

      for (PotionBrewing.Mix<Potion> mix1 : this.potionMixes) {
         if (mix1.from.is(optional.get()) && mix1.ingredient.test(p_43530_)) {
            return PotionContents.createItemStack(p_43531_.getItem(), mix1.to);
         }
      }

      return p_43531_;
   }

   public static PotionBrewing bootstrap(FeatureFlagSet p_329176_) {
      PotionBrewing.Builder potionbrewing$builder = new PotionBrewing.Builder(p_329176_);
      addVanillaMixes(potionbrewing$builder);
      return potionbrewing$builder.build();
   }

   public static void addVanillaMixes(PotionBrewing.Builder p_332525_) {
      p_332525_.addContainer(Items.POTION);
      p_332525_.addContainer(Items.SPLASH_POTION);
      p_332525_.addContainer(Items.LINGERING_POTION);
      p_332525_.addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
      p_332525_.addContainerRecipe(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
      p_332525_.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
      p_332525_.addMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
      p_332525_.addMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
      p_332525_.addStartMix(Items.BREEZE_ROD, Potions.WIND_CHARGED);
      p_332525_.addStartMix(Items.SLIME_BLOCK, Potions.OOZING);
      p_332525_.addStartMix(Items.STONE, Potions.INFESTED);
      p_332525_.addStartMix(Items.COBWEB, Potions.WEAVING);
      p_332525_.addMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
      p_332525_.addMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
      p_332525_.addMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
      p_332525_.addMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
      p_332525_.addMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
      p_332525_.addStartMix(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
      p_332525_.addMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
      p_332525_.addStartMix(Items.RABBIT_FOOT, Potions.LEAPING);
      p_332525_.addMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
      p_332525_.addMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
      p_332525_.addMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      p_332525_.addMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      p_332525_.addMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
      p_332525_.addMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
      p_332525_.addMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
      p_332525_.addMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
      p_332525_.addMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
      p_332525_.addMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      p_332525_.addMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      p_332525_.addStartMix(Items.SUGAR, Potions.SWIFTNESS);
      p_332525_.addMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
      p_332525_.addMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
      p_332525_.addMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
      p_332525_.addMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
      p_332525_.addStartMix(Items.GLISTERING_MELON_SLICE, Potions.HEALING);
      p_332525_.addMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
      p_332525_.addMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      p_332525_.addMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      p_332525_.addMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
      p_332525_.addMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      p_332525_.addMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      p_332525_.addMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      p_332525_.addStartMix(Items.SPIDER_EYE, Potions.POISON);
      p_332525_.addMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
      p_332525_.addMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
      p_332525_.addStartMix(Items.GHAST_TEAR, Potions.REGENERATION);
      p_332525_.addMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
      p_332525_.addMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
      p_332525_.addStartMix(Items.BLAZE_POWDER, Potions.STRENGTH);
      p_332525_.addMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
      p_332525_.addMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
      p_332525_.addMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
      p_332525_.addMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
      p_332525_.addMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
      p_332525_.addMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
   }

   public static class Builder {
      private final List<Ingredient> containers = new ArrayList<>();
      private final List<PotionBrewing.Mix<Potion>> potionMixes = new ArrayList<>();
      private final List<PotionBrewing.Mix<Item>> containerMixes = new ArrayList<>();
      private final FeatureFlagSet enabledFeatures;

      public Builder(FeatureFlagSet p_332559_) {
         this.enabledFeatures = p_332559_;
      }

      private static void expectPotion(Item p_335280_) {
         if (!(p_335280_ instanceof PotionItem)) {
            throw new IllegalArgumentException("Expected a potion, got: " + BuiltInRegistries.ITEM.getKey(p_335280_));
         }
      }

      public void addContainerRecipe(Item p_332816_, Item p_336336_, Item p_330133_) {
         if (p_332816_.isEnabled(this.enabledFeatures) && p_336336_.isEnabled(this.enabledFeatures) && p_330133_.isEnabled(this.enabledFeatures)) {
            expectPotion(p_332816_);
            expectPotion(p_330133_);
            this.containerMixes.add(new PotionBrewing.Mix<>(p_332816_.builtInRegistryHolder(), Ingredient.of(p_336336_), p_330133_.builtInRegistryHolder()));
         }
      }

      public void addContainer(Item p_329695_) {
         if (p_329695_.isEnabled(this.enabledFeatures)) {
            expectPotion(p_329695_);
            this.containers.add(Ingredient.of(p_329695_));
         }
      }

      public void addMix(Holder<Potion> p_333042_, Item p_331299_, Holder<Potion> p_328607_) {
         if (p_333042_.value().isEnabled(this.enabledFeatures)
            && p_331299_.isEnabled(this.enabledFeatures)
            && p_328607_.value().isEnabled(this.enabledFeatures)) {
            this.potionMixes.add(new PotionBrewing.Mix<>(p_333042_, Ingredient.of(p_331299_), p_328607_));
         }
      }

      public void addStartMix(Item p_327705_, Holder<Potion> p_328478_) {
         if (p_328478_.value().isEnabled(this.enabledFeatures)) {
            this.addMix(Potions.WATER, p_327705_, Potions.MUNDANE);
            this.addMix(Potions.AWKWARD, p_327705_, p_328478_);
         }
      }

      public PotionBrewing build() {
         return new PotionBrewing(List.copyOf(this.containers), List.copyOf(this.potionMixes), List.copyOf(this.containerMixes));
      }
   }

   record Mix<T>(Holder<T> from, Ingredient ingredient, Holder<T> to) {
   }
}
