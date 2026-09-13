package net.minecraft.data.recipes.packs;

import net.minecraft.data.recipes.BrewingProvider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;

public class VanillaBrewingProvider extends BrewingProvider {
   protected VanillaBrewingProvider(final RecipeOutput recipeOutput) {
      super(recipeOutput);
   }

   @Override
   protected void addContainers() {
      this.addContainer(Items.LINGERING_POTION);
      this.addContainer(Items.POTION);
      this.addContainer(Items.SPLASH_POTION);
   }

   @Override
   protected void addContainerTransformations() {
      this.addContainerTransformation(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
      this.addContainerTransformation(Items.SPLASH_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION);
   }

   @Override
   protected void buildMixes() {
      this.buildMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
      this.buildMix(Potions.WATER, Items.REDSTONE, Potions.MUNDANE);
      this.buildMix(Potions.WATER, Items.NETHER_WART, Potions.AWKWARD);
      this.buildStartMix(Items.BREEZE_ROD, Potions.WIND_CHARGED);
      this.buildStartMix(Items.SLIME_BLOCK, Potions.OOZING);
      this.buildStartMix(Items.STONE, Potions.INFESTED);
      this.buildStartMix(Items.COBWEB, Potions.WEAVING);
      this.buildMix(Potions.AWKWARD, Items.GOLDEN_CARROT, Potions.NIGHT_VISION);
      this.buildMix(Potions.NIGHT_VISION, Items.REDSTONE, Potions.LONG_NIGHT_VISION);
      this.buildMix(Potions.NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.INVISIBILITY);
      this.buildMix(Potions.LONG_NIGHT_VISION, Items.FERMENTED_SPIDER_EYE, Potions.LONG_INVISIBILITY);
      this.buildMix(Potions.INVISIBILITY, Items.REDSTONE, Potions.LONG_INVISIBILITY);
      this.buildStartMix(Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
      this.buildMix(Potions.FIRE_RESISTANCE, Items.REDSTONE, Potions.LONG_FIRE_RESISTANCE);
      this.buildStartMix(Items.RABBIT_FOOT, Potions.LEAPING);
      this.buildMix(Potions.LEAPING, Items.REDSTONE, Potions.LONG_LEAPING);
      this.buildMix(Potions.LEAPING, Items.GLOWSTONE_DUST, Potions.STRONG_LEAPING);
      this.buildMix(Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      this.buildMix(Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      this.buildMix(Potions.SLOWNESS, Items.REDSTONE, Potions.LONG_SLOWNESS);
      this.buildMix(Potions.SLOWNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SLOWNESS);
      this.buildMix(Potions.AWKWARD, Items.TURTLE_HELMET, Potions.TURTLE_MASTER);
      this.buildMix(Potions.TURTLE_MASTER, Items.REDSTONE, Potions.LONG_TURTLE_MASTER);
      this.buildMix(Potions.TURTLE_MASTER, Items.GLOWSTONE_DUST, Potions.STRONG_TURTLE_MASTER);
      this.buildMix(Potions.SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS);
      this.buildMix(Potions.LONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS);
      this.buildStartMix(Items.SUGAR, Potions.SWIFTNESS);
      this.buildMix(Potions.SWIFTNESS, Items.REDSTONE, Potions.LONG_SWIFTNESS);
      this.buildMix(Potions.SWIFTNESS, Items.GLOWSTONE_DUST, Potions.STRONG_SWIFTNESS);
      this.buildMix(Potions.AWKWARD, Items.PUFFERFISH, Potions.WATER_BREATHING);
      this.buildMix(Potions.WATER_BREATHING, Items.REDSTONE, Potions.LONG_WATER_BREATHING);
      this.buildStartMix(Items.GLISTERING_MELON_SLICE, Potions.HEALING);
      this.buildMix(Potions.HEALING, Items.GLOWSTONE_DUST, Potions.STRONG_HEALING);
      this.buildMix(Potions.HEALING, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      this.buildMix(Potions.STRONG_HEALING, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      this.buildMix(Potions.HARMING, Items.GLOWSTONE_DUST, Potions.STRONG_HARMING);
      this.buildMix(Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      this.buildMix(Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING);
      this.buildMix(Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
      this.buildStartMix(Items.SPIDER_EYE, Potions.POISON);
      this.buildMix(Potions.POISON, Items.REDSTONE, Potions.LONG_POISON);
      this.buildMix(Potions.POISON, Items.GLOWSTONE_DUST, Potions.STRONG_POISON);
      this.buildStartMix(Items.GHAST_TEAR, Potions.REGENERATION);
      this.buildMix(Potions.REGENERATION, Items.REDSTONE, Potions.LONG_REGENERATION);
      this.buildMix(Potions.REGENERATION, Items.GLOWSTONE_DUST, Potions.STRONG_REGENERATION);
      this.buildStartMix(Items.BLAZE_POWDER, Potions.STRENGTH);
      this.buildMix(Potions.STRENGTH, Items.REDSTONE, Potions.LONG_STRENGTH);
      this.buildMix(Potions.STRENGTH, Items.GLOWSTONE_DUST, Potions.STRONG_STRENGTH);
      this.buildMix(Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
      this.buildMix(Potions.WEAKNESS, Items.REDSTONE, Potions.LONG_WEAKNESS);
      this.buildMix(Potions.AWKWARD, Items.PHANTOM_MEMBRANE, Potions.SLOW_FALLING);
      this.buildMix(Potions.SLOW_FALLING, Items.REDSTONE, Potions.LONG_SLOW_FALLING);
   }
}
