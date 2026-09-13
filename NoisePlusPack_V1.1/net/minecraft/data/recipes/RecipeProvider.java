package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.EnterBlockTrigger;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import org.jspecify.annotations.Nullable;

public abstract class RecipeProvider {
   protected final HolderLookup.Provider registries;
   private final HolderGetter<Item> items;
   protected final RecipeOutput output;
   private static final Map<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider> SHAPE_BUILDERS = ImmutableMap.builder()
      .put(
         BlockFamily.Variant.BUTTON,
         (RecipeProvider.FamilyRecipeProvider)(p_358405_, p_358406_, p_358407_) -> p_358405_.buttonBuilder(p_358406_, Ingredient.of(p_358407_))
      )
      .put(
         BlockFamily.Variant.CHISELED,
         (RecipeProvider.FamilyRecipeProvider)(p_358408_, p_358409_, p_358410_) -> p_358408_.chiseledBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_358409_, Ingredient.of(p_358410_)
         )
      )
      .put(
         BlockFamily.Variant.CUT,
         (RecipeProvider.FamilyRecipeProvider)(p_358435_, p_358436_, p_358437_) -> p_358435_.cutBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_358436_, Ingredient.of(p_358437_)
         )
      )
      .put(
         BlockFamily.Variant.DOOR,
         (RecipeProvider.FamilyRecipeProvider)(p_358411_, p_358412_, p_358413_) -> p_358411_.doorBuilder(p_358412_, Ingredient.of(p_358413_))
      )
      .put(
         BlockFamily.Variant.CUSTOM_FENCE,
         (RecipeProvider.FamilyRecipeProvider)(p_358423_, p_358424_, p_358425_) -> p_358423_.fenceBuilder(p_358424_, Ingredient.of(p_358425_))
      )
      .put(
         BlockFamily.Variant.FENCE,
         (RecipeProvider.FamilyRecipeProvider)(p_358450_, p_358451_, p_358452_) -> p_358450_.fenceBuilder(p_358451_, Ingredient.of(p_358452_))
      )
      .put(
         BlockFamily.Variant.CUSTOM_FENCE_GATE,
         (RecipeProvider.FamilyRecipeProvider)(p_358429_, p_358430_, p_358431_) -> p_358429_.fenceGateBuilder(p_358430_, Ingredient.of(p_358431_))
      )
      .put(
         BlockFamily.Variant.FENCE_GATE,
         (RecipeProvider.FamilyRecipeProvider)(p_358417_, p_358418_, p_358419_) -> p_358417_.fenceGateBuilder(p_358418_, Ingredient.of(p_358419_))
      )
      .put(
         BlockFamily.Variant.SIGN,
         (RecipeProvider.FamilyRecipeProvider)(p_358420_, p_358421_, p_358422_) -> p_358420_.signBuilder(p_358421_, Ingredient.of(p_358422_))
      )
      .put(
         BlockFamily.Variant.SLAB,
         (RecipeProvider.FamilyRecipeProvider)(p_358414_, p_358415_, p_358416_) -> p_358414_.slabBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_358415_, Ingredient.of(p_358416_)
         )
      )
      .put(
         BlockFamily.Variant.STAIRS,
         (RecipeProvider.FamilyRecipeProvider)(p_358426_, p_358427_, p_358428_) -> p_358426_.stairBuilder(p_358427_, Ingredient.of(p_358428_))
      )
      .put(
         BlockFamily.Variant.PRESSURE_PLATE,
         (RecipeProvider.FamilyRecipeProvider)(p_358447_, p_358448_, p_358449_) -> p_358447_.pressurePlateBuilder(
            RecipeCategory.REDSTONE, p_358448_, Ingredient.of(p_358449_)
         )
      )
      .put(
         BlockFamily.Variant.POLISHED,
         (RecipeProvider.FamilyRecipeProvider)(p_358399_, p_358400_, p_358401_) -> p_358399_.polishedBuilder(
            RecipeCategory.BUILDING_BLOCKS, p_358400_, Ingredient.of(p_358401_)
         )
      )
      .put(
         BlockFamily.Variant.TRAPDOOR,
         (RecipeProvider.FamilyRecipeProvider)(p_358432_, p_358433_, p_358434_) -> p_358432_.trapdoorBuilder(p_358433_, Ingredient.of(p_358434_))
      )
      .put(
         BlockFamily.Variant.WALL,
         (RecipeProvider.FamilyRecipeProvider)(p_358402_, p_358403_, p_358404_) -> p_358402_.wallBuilder(
            RecipeCategory.DECORATIONS, p_358403_, Ingredient.of(p_358404_)
         )
      )
      .build();

   protected RecipeProvider(HolderLookup.Provider p_361709_, RecipeOutput p_365321_) {
      this.registries = p_361709_;
      this.items = p_361709_.lookupOrThrow(Registries.ITEM);
      this.output = p_365321_;
   }

   protected abstract void buildRecipes();

   protected void generateForEnabledBlockFamilies(FeatureFlagSet p_251836_) {
      BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach(p_358446_ -> this.generateRecipes(p_358446_, p_251836_));
   }

   protected void oneToOneConversionRecipe(ItemLike p_176558_, ItemLike p_176559_, @Nullable String p_176560_) {
      this.oneToOneConversionRecipe(p_176558_, p_176559_, p_176560_, 1);
   }

   protected void oneToOneConversionRecipe(ItemLike p_176553_, ItemLike p_176554_, @Nullable String p_176555_, int p_369925_) {
      this.shapeless(RecipeCategory.MISC, p_176553_, p_369925_)
         .requires(p_176554_)
         .group(p_176555_)
         .unlockedBy(getHasName(p_176554_), this.has(p_176554_))
         .save(this.output, getConversionRecipeName(p_176553_, p_176554_));
   }

   protected void oreSmelting(List<ItemLike> p_250172_, RecipeCategory p_250588_, ItemLike p_251868_, float p_250789_, int p_252144_, String p_251687_) {
      this.oreCooking(RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, p_250172_, p_250588_, p_251868_, p_250789_, p_252144_, p_251687_, "_from_smelting");
   }

   protected void oreBlasting(List<ItemLike> p_251504_, RecipeCategory p_248846_, ItemLike p_249735_, float p_248783_, int p_250303_, String p_251984_) {
      this.oreCooking(RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, p_251504_, p_248846_, p_249735_, p_248783_, p_250303_, p_251984_, "_from_blasting");
   }

   private <T extends AbstractCookingRecipe> void oreCooking(
      RecipeSerializer<T> p_251817_,
      AbstractCookingRecipe.Factory<T> p_312098_,
      List<ItemLike> p_249619_,
      RecipeCategory p_251154_,
      ItemLike p_250066_,
      float p_251871_,
      int p_251316_,
      String p_251450_,
      String p_249236_
   ) {
      for (ItemLike itemlike : p_249619_) {
         SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), p_251154_, p_250066_, p_251871_, p_251316_, p_251817_, p_312098_)
            .group(p_251450_)
            .unlockedBy(getHasName(itemlike), this.has(itemlike))
            .save(this.output, getItemName(p_250066_) + p_249236_ + "_" + getItemName(itemlike));
      }
   }

   protected void netheriteSmithing(Item p_250046_, RecipeCategory p_248986_, Item p_250389_) {
      SmithingTransformRecipeBuilder.smithing(
            Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.of(p_250046_),
            this.tag(ItemTags.NETHERITE_TOOL_MATERIALS),
            p_248986_,
            p_250389_
         )
         .unlocks("has_netherite_ingot", this.has(ItemTags.NETHERITE_TOOL_MATERIALS))
         .save(this.output, getItemName(p_250389_) + "_smithing");
   }

   protected void trimSmithing(Item p_285461_, ResourceKey<TrimPattern> p_364939_, ResourceKey<Recipe<?>> p_392668_) {
      Holder.Reference<TrimPattern> reference = this.registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(p_364939_);
      SmithingTrimRecipeBuilder.smithingTrim(
            Ingredient.of(p_285461_), this.tag(ItemTags.TRIMMABLE_ARMOR), this.tag(ItemTags.TRIM_MATERIALS), reference, RecipeCategory.MISC
         )
         .unlocks("has_smithing_trim_template", this.has(p_285461_))
         .save(this.output, p_392668_);
   }

   protected void twoByTwoPacker(RecipeCategory p_250881_, ItemLike p_252184_, ItemLike p_249710_) {
      this.shaped(p_250881_, p_252184_, 1)
         .define('#', p_249710_)
         .pattern("##")
         .pattern("##")
         .unlockedBy(getHasName(p_249710_), this.has(p_249710_))
         .save(this.output);
   }

   protected void threeByThreePacker(RecipeCategory p_259186_, ItemLike p_259360_, ItemLike p_259263_, String p_361463_) {
      this.shapeless(p_259186_, p_259360_).requires(p_259263_, 9).unlockedBy(p_361463_, this.has(p_259263_)).save(this.output);
   }

   protected void threeByThreePacker(RecipeCategory p_259247_, ItemLike p_259376_, ItemLike p_259717_) {
      this.threeByThreePacker(p_259247_, p_259376_, p_259717_, getHasName(p_259717_));
   }

   protected void planksFromLog(ItemLike p_259052_, TagKey<Item> p_259045_, int p_259471_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_259052_, p_259471_)
         .requires(p_259045_)
         .group("planks")
         .unlockedBy("has_log", this.has(p_259045_))
         .save(this.output);
   }

   protected void planksFromLogs(ItemLike p_259193_, TagKey<Item> p_259818_, int p_259807_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_259193_, p_259807_)
         .requires(p_259818_)
         .group("planks")
         .unlockedBy("has_logs", this.has(p_259818_))
         .save(this.output);
   }

   protected void woodFromLogs(ItemLike p_126004_, ItemLike p_126005_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126004_, 3)
         .define('#', p_126005_)
         .pattern("##")
         .pattern("##")
         .group("bark")
         .unlockedBy("has_log", this.has(p_126005_))
         .save(this.output);
   }

   protected void woodenBoat(ItemLike p_126023_, ItemLike p_126024_) {
      this.shaped(RecipeCategory.TRANSPORTATION, p_126023_)
         .define('#', p_126024_)
         .pattern("# #")
         .pattern("###")
         .group("boat")
         .unlockedBy("in_water", insideOf(Blocks.WATER))
         .save(this.output);
   }

   protected void chestBoat(ItemLike p_236373_, ItemLike p_236374_) {
      this.shapeless(RecipeCategory.TRANSPORTATION, p_236373_)
         .requires(Blocks.CHEST)
         .requires(p_236374_)
         .group("chest_boat")
         .unlockedBy("has_boat", this.has(ItemTags.BOATS))
         .save(this.output);
   }

   private RecipeBuilder buttonBuilder(ItemLike p_176659_, Ingredient p_176660_) {
      return this.shapeless(RecipeCategory.REDSTONE, p_176659_).requires(p_176660_);
   }

   protected RecipeBuilder doorBuilder(ItemLike p_176671_, Ingredient p_176672_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176671_, 3).define('#', p_176672_).pattern("##").pattern("##").pattern("##");
   }

   private RecipeBuilder fenceBuilder(ItemLike p_176679_, Ingredient p_176680_) {
      int i = p_176679_ == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
      Item item = p_176679_ == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
      return this.shaped(RecipeCategory.DECORATIONS, p_176679_, i).define('W', p_176680_).define('#', item).pattern("W#W").pattern("W#W");
   }

   private RecipeBuilder fenceGateBuilder(ItemLike p_176685_, Ingredient p_176686_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176685_).define('#', Items.STICK).define('W', p_176686_).pattern("#W#").pattern("#W#");
   }

   protected void pressurePlate(ItemLike p_176692_, ItemLike p_176693_) {
      this.pressurePlateBuilder(RecipeCategory.REDSTONE, p_176692_, Ingredient.of(p_176693_))
         .unlockedBy(getHasName(p_176693_), this.has(p_176693_))
         .save(this.output);
   }

   private RecipeBuilder pressurePlateBuilder(RecipeCategory p_251447_, ItemLike p_251989_, Ingredient p_249211_) {
      return this.shaped(p_251447_, p_251989_).define('#', p_249211_).pattern("##");
   }

   protected void slab(RecipeCategory p_251848_, ItemLike p_249368_, ItemLike p_252133_) {
      this.slabBuilder(p_251848_, p_249368_, Ingredient.of(p_252133_)).unlockedBy(getHasName(p_252133_), this.has(p_252133_)).save(this.output);
   }

   protected void shelf(ItemLike p_423283_, ItemLike p_429276_) {
      this.shaped(RecipeCategory.DECORATIONS, p_423283_, 6)
         .define('#', p_429276_)
         .pattern("###")
         .pattern("   ")
         .pattern("###")
         .group("shelf")
         .unlockedBy(getHasName(p_429276_), this.has(p_429276_))
         .save(this.output);
   }

   protected RecipeBuilder slabBuilder(RecipeCategory p_251707_, ItemLike p_251284_, Ingredient p_248824_) {
      return this.shaped(p_251707_, p_251284_, 6).define('#', p_248824_).pattern("###");
   }

   protected RecipeBuilder stairBuilder(ItemLike p_176711_, Ingredient p_176712_) {
      return this.shaped(RecipeCategory.BUILDING_BLOCKS, p_176711_, 4).define('#', p_176712_).pattern("#  ").pattern("## ").pattern("###");
   }

   protected RecipeBuilder trapdoorBuilder(ItemLike p_176721_, Ingredient p_176722_) {
      return this.shaped(RecipeCategory.REDSTONE, p_176721_, 2).define('#', p_176722_).pattern("###").pattern("###");
   }

   private RecipeBuilder signBuilder(ItemLike p_176727_, Ingredient p_176728_) {
      return this.shaped(RecipeCategory.DECORATIONS, p_176727_, 3)
         .group("sign")
         .define('#', p_176728_)
         .define('X', Items.STICK)
         .pattern("###")
         .pattern("###")
         .pattern(" X ");
   }

   protected void hangingSign(ItemLike p_252355_, ItemLike p_250437_) {
      this.shaped(RecipeCategory.DECORATIONS, p_252355_, 6)
         .group("hanging_sign")
         .define('#', p_250437_)
         .define('X', Items.IRON_CHAIN)
         .pattern("X X")
         .pattern("###")
         .pattern("###")
         .unlockedBy("has_stripped_logs", this.has(p_250437_))
         .save(this.output);
   }

   protected void colorItemWithDye(List<Item> p_407034_, List<Item> p_408997_, String p_407929_, RecipeCategory p_408943_) {
      this.colorWithDye(p_407034_, p_408997_, null, p_407929_, p_408943_);
   }

   protected void colorWithDye(List<Item> p_368136_, List<Item> p_368703_, @Nullable Item p_362653_, String p_362940_, RecipeCategory p_368238_) {
      for (int i = 0; i < p_368136_.size(); i++) {
         Item item = p_368136_.get(i);
         Item item1 = p_368703_.get(i);
         Stream<Item> stream = p_368703_.stream().filter(p_288265_ -> !p_288265_.equals(item1));
         if (p_362653_ != null) {
            stream = Stream.concat(stream, Stream.of(p_362653_));
         }

         this.shapeless(p_368238_, item1)
            .requires(item)
            .requires(Ingredient.of(stream))
            .group(p_362940_)
            .unlockedBy("has_needed_dye", this.has(item))
            .save(this.output, "dye_" + getItemName(item1));
      }
   }

   protected void carpet(ItemLike p_176718_, ItemLike p_176719_) {
      this.shaped(RecipeCategory.DECORATIONS, p_176718_, 3)
         .define('#', p_176719_)
         .pattern("##")
         .group("carpet")
         .unlockedBy(getHasName(p_176719_), this.has(p_176719_))
         .save(this.output);
   }

   protected void bedFromPlanksAndWool(ItemLike p_126075_, ItemLike p_126076_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126075_)
         .define('#', p_126076_)
         .define('X', ItemTags.PLANKS)
         .pattern("###")
         .pattern("XXX")
         .group("bed")
         .unlockedBy(getHasName(p_126076_), this.has(p_126076_))
         .save(this.output);
   }

   protected void banner(ItemLike p_126083_, ItemLike p_126084_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126083_)
         .define('#', p_126084_)
         .define('|', Items.STICK)
         .pattern("###")
         .pattern("###")
         .pattern(" | ")
         .group("banner")
         .unlockedBy(getHasName(p_126084_), this.has(p_126084_))
         .save(this.output);
   }

   protected void stainedGlassFromGlassAndDye(ItemLike p_126087_, ItemLike p_126088_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126087_, 8)
         .define('#', Blocks.GLASS)
         .define('X', p_126088_)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("stained_glass")
         .unlockedBy("has_glass", this.has(Blocks.GLASS))
         .save(this.output);
   }

   protected void dryGhast(ItemLike p_406729_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_406729_, 1)
         .define('#', Items.GHAST_TEAR)
         .define('X', Items.SOUL_SAND)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("dry_ghast")
         .unlockedBy(getHasName(Items.GHAST_TEAR), this.has(Items.GHAST_TEAR))
         .save(this.output);
   }

   protected void harness(ItemLike p_409535_, ItemLike p_406689_) {
      this.shaped(RecipeCategory.COMBAT, p_409535_)
         .define('#', p_406689_)
         .define('G', Items.GLASS)
         .define('L', Items.LEATHER)
         .pattern("LLL")
         .pattern("G#G")
         .group("harness")
         .unlockedBy("has_dried_ghast", this.has(Blocks.DRIED_GHAST))
         .save(this.output);
   }

   protected void stainedGlassPaneFromStainedGlass(ItemLike p_126091_, ItemLike p_126092_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126091_, 16)
         .define('#', p_126092_)
         .pattern("###")
         .pattern("###")
         .group("stained_glass_pane")
         .unlockedBy("has_glass", this.has(p_126092_))
         .save(this.output);
   }

   protected void stainedGlassPaneFromGlassPaneAndDye(ItemLike p_126095_, ItemLike p_126096_) {
      this.shaped(RecipeCategory.DECORATIONS, p_126095_, 8)
         .define('#', Blocks.GLASS_PANE)
         .define('$', p_126096_)
         .pattern("###")
         .pattern("#$#")
         .pattern("###")
         .group("stained_glass_pane")
         .unlockedBy("has_glass_pane", this.has(Blocks.GLASS_PANE))
         .unlockedBy(getHasName(p_126096_), this.has(p_126096_))
         .save(this.output, getConversionRecipeName(p_126095_, Blocks.GLASS_PANE));
   }

   protected void coloredTerracottaFromTerracottaAndDye(ItemLike p_126099_, ItemLike p_126100_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_126099_, 8)
         .define('#', Blocks.TERRACOTTA)
         .define('X', p_126100_)
         .pattern("###")
         .pattern("#X#")
         .pattern("###")
         .group("stained_terracotta")
         .unlockedBy("has_terracotta", this.has(Blocks.TERRACOTTA))
         .save(this.output);
   }

   protected void concretePowder(ItemLike p_126103_, ItemLike p_126104_) {
      this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_126103_, 8)
         .requires(p_126104_)
         .requires(Blocks.SAND, 4)
         .requires(Blocks.GRAVEL, 4)
         .group("concrete_powder")
         .unlockedBy("has_sand", this.has(Blocks.SAND))
         .unlockedBy("has_gravel", this.has(Blocks.GRAVEL))
         .save(this.output);
   }

   protected void candle(ItemLike p_176544_, ItemLike p_176545_) {
      this.shapeless(RecipeCategory.DECORATIONS, p_176544_)
         .requires(Blocks.CANDLE)
         .requires(p_176545_)
         .group("dyed_candle")
         .unlockedBy(getHasName(p_176545_), this.has(p_176545_))
         .save(this.output);
   }

   protected void wall(RecipeCategory p_251148_, ItemLike p_250499_, ItemLike p_249970_) {
      this.wallBuilder(p_251148_, p_250499_, Ingredient.of(p_249970_)).unlockedBy(getHasName(p_249970_), this.has(p_249970_)).save(this.output);
   }

   private RecipeBuilder wallBuilder(RecipeCategory p_249083_, ItemLike p_250754_, Ingredient p_250311_) {
      return this.shaped(p_249083_, p_250754_, 6).define('#', p_250311_).pattern("###").pattern("###");
   }

   protected void polished(RecipeCategory p_248719_, ItemLike p_250032_, ItemLike p_250021_) {
      this.polishedBuilder(p_248719_, p_250032_, Ingredient.of(p_250021_)).unlockedBy(getHasName(p_250021_), this.has(p_250021_)).save(this.output);
   }

   private RecipeBuilder polishedBuilder(RecipeCategory p_249131_, ItemLike p_251242_, Ingredient p_251412_) {
      return this.shaped(p_249131_, p_251242_, 4).define('S', p_251412_).pattern("SS").pattern("SS");
   }

   protected void cut(RecipeCategory p_252306_, ItemLike p_249686_, ItemLike p_251100_) {
      this.cutBuilder(p_252306_, p_249686_, Ingredient.of(p_251100_)).unlockedBy(getHasName(p_251100_), this.has(p_251100_)).save(this.output);
   }

   private ShapedRecipeBuilder cutBuilder(RecipeCategory p_250895_, ItemLike p_251147_, Ingredient p_251563_) {
      return this.shaped(p_250895_, p_251147_, 4).define('#', p_251563_).pattern("##").pattern("##");
   }

   protected void chiseled(RecipeCategory p_251604_, ItemLike p_251049_, ItemLike p_252267_) {
      this.chiseledBuilder(p_251604_, p_251049_, Ingredient.of(p_252267_)).unlockedBy(getHasName(p_252267_), this.has(p_252267_)).save(this.output);
   }

   protected void mosaicBuilder(RecipeCategory p_248788_, ItemLike p_251925_, ItemLike p_252242_) {
      this.shaped(p_248788_, p_251925_)
         .define('#', p_252242_)
         .pattern("#")
         .pattern("#")
         .unlockedBy(getHasName(p_252242_), this.has(p_252242_))
         .save(this.output);
   }

   protected ShapedRecipeBuilder chiseledBuilder(RecipeCategory p_251755_, ItemLike p_249782_, Ingredient p_250087_) {
      return this.shaped(p_251755_, p_249782_).define('#', p_250087_).pattern("#").pattern("#");
   }

   protected void stonecutterResultFromBase(RecipeCategory p_248911_, ItemLike p_251265_, ItemLike p_250033_) {
      this.stonecutterResultFromBase(p_248911_, p_251265_, p_250033_, 1);
   }

   protected void stonecutterResultFromBase(RecipeCategory p_250609_, ItemLike p_251254_, ItemLike p_249666_, int p_368611_) {
      SingleItemRecipeBuilder.stonecutting(Ingredient.of(p_249666_), p_250609_, p_251254_, p_368611_)
         .unlockedBy(getHasName(p_249666_), this.has(p_249666_))
         .save(this.output, getConversionRecipeName(p_251254_, p_249666_) + "_stonecutting");
   }

   private void smeltingResultFromBase(ItemLike p_176741_, ItemLike p_176742_) {
      SimpleCookingRecipeBuilder.smelting(Ingredient.of(p_176742_), RecipeCategory.BUILDING_BLOCKS, p_176741_, 0.1F, 200)
         .unlockedBy(getHasName(p_176742_), this.has(p_176742_))
         .save(this.output);
   }

   protected void nineBlockStorageRecipes(RecipeCategory p_251203_, ItemLike p_251689_, RecipeCategory p_251376_, ItemLike p_248771_) {
      this.nineBlockStorageRecipes(p_251203_, p_251689_, p_251376_, p_248771_, getSimpleRecipeName(p_248771_), null, getSimpleRecipeName(p_251689_), null);
   }

   protected void nineBlockStorageRecipesWithCustomPacking(
      RecipeCategory p_250885_, ItemLike p_251651_, RecipeCategory p_250874_, ItemLike p_248576_, String p_250171_, String p_249386_
   ) {
      this.nineBlockStorageRecipes(p_250885_, p_251651_, p_250874_, p_248576_, p_250171_, p_249386_, getSimpleRecipeName(p_251651_), null);
   }

   protected void nineBlockStorageRecipesRecipesWithCustomUnpacking(
      RecipeCategory p_248979_, ItemLike p_249101_, RecipeCategory p_252036_, ItemLike p_250886_, String p_248768_, String p_250847_
   ) {
      this.nineBlockStorageRecipes(p_248979_, p_249101_, p_252036_, p_250886_, getSimpleRecipeName(p_250886_), null, p_248768_, p_250847_);
   }

   private void nineBlockStorageRecipes(
      RecipeCategory p_250083_,
      ItemLike p_250042_,
      RecipeCategory p_248977_,
      ItemLike p_251911_,
      String p_250475_,
      @Nullable String p_248641_,
      String p_252237_,
      @Nullable String p_250414_
   ) {
      this.shapeless(p_250083_, p_250042_, 9)
         .requires(p_251911_)
         .group(p_250414_)
         .unlockedBy(getHasName(p_251911_), this.has(p_251911_))
         .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_252237_)));
      this.shaped(p_248977_, p_251911_)
         .define('#', p_250042_)
         .pattern("###")
         .pattern("###")
         .pattern("###")
         .group(p_248641_)
         .unlockedBy(getHasName(p_250042_), this.has(p_250042_))
         .save(this.output, ResourceKey.create(Registries.RECIPE, Identifier.parse(p_250475_)));
   }

   protected void copySmithingTemplate(ItemLike p_345069_, ItemLike p_362058_) {
      this.shaped(RecipeCategory.MISC, p_345069_, 2)
         .define('#', Items.DIAMOND)
         .define('C', p_362058_)
         .define('S', p_345069_)
         .pattern("#S#")
         .pattern("#C#")
         .pattern("###")
         .unlockedBy(getHasName(p_345069_), this.has(p_345069_))
         .save(this.output);
   }

   protected void copySmithingTemplate(ItemLike p_267133_, Ingredient p_363155_) {
      this.shaped(RecipeCategory.MISC, p_267133_, 2)
         .define('#', Items.DIAMOND)
         .define('C', p_363155_)
         .define('S', p_267133_)
         .pattern("#S#")
         .pattern("#C#")
         .pattern("###")
         .unlockedBy(getHasName(p_267133_), this.has(p_267133_))
         .save(this.output);
   }

   protected <T extends AbstractCookingRecipe> void cookRecipes(
      String p_126008_, RecipeSerializer<T> p_250529_, AbstractCookingRecipe.Factory<T> p_312851_, int p_126010_
   ) {
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.BEEF, Items.COOKED_BEEF, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.COD, Items.COOKED_COD, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.KELP, Items.DRIED_KELP, 0.1F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.SALMON, Items.COOKED_SALMON, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.POTATO, Items.BAKED_POTATO, 0.35F);
      this.simpleCookingRecipe(p_126008_, p_250529_, p_312851_, p_126010_, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
   }

   private <T extends AbstractCookingRecipe> void simpleCookingRecipe(
      String p_249709_,
      RecipeSerializer<T> p_251876_,
      AbstractCookingRecipe.Factory<T> p_311509_,
      int p_249258_,
      ItemLike p_250669_,
      ItemLike p_250224_,
      float p_252138_
   ) {
      SimpleCookingRecipeBuilder.generic(Ingredient.of(p_250669_), RecipeCategory.FOOD, p_250224_, p_252138_, p_249258_, p_251876_, p_311509_)
         .unlockedBy(getHasName(p_250669_), this.has(p_250669_))
         .save(this.output, getItemName(p_250224_) + "_from_" + p_249709_);
   }

   protected void waxRecipes(FeatureFlagSet p_312821_) {
      HoneycombItem.WAXABLES
         .get()
         .forEach(
            (p_448713_, p_448714_) -> {
               if (p_448714_.requiredFeatures().isSubsetOf(p_312821_)) {
                  Pair<RecipeCategory, String> pair = (Pair<RecipeCategory, String>)HoneycombItem.WAXED_RECIPES
                     .getOrDefault(p_448714_, Pair.of(RecipeCategory.BUILDING_BLOCKS, getItemName(p_448714_)));
                  RecipeCategory recipecategory = (RecipeCategory)pair.getFirst();
                  String s = (String)pair.getSecond();
                  this.shapeless(recipecategory, p_448714_)
                     .requires(p_448713_)
                     .requires(Items.HONEYCOMB)
                     .group(s)
                     .unlockedBy(getHasName(p_448713_), this.has(p_448713_))
                     .save(this.output, getConversionRecipeName(p_448714_, Items.HONEYCOMB));
               }
            }
         );
   }

   protected void grate(Block p_309854_, Block p_310802_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_309854_, 4)
         .define('M', p_310802_)
         .pattern(" M ")
         .pattern("M M")
         .pattern(" M ")
         .group(getItemName(p_309854_))
         .unlockedBy(getHasName(p_310802_), this.has(p_310802_))
         .save(this.output);
   }

   protected void copperBulb(Block p_312293_, Block p_312258_) {
      this.shaped(RecipeCategory.REDSTONE, p_312293_, 4)
         .define('C', p_312258_)
         .define('R', Items.REDSTONE)
         .define('B', Items.BLAZE_ROD)
         .pattern(" C ")
         .pattern("CBC")
         .pattern(" R ")
         .unlockedBy(getHasName(p_312258_), this.has(p_312258_))
         .group(getItemName(p_312293_))
         .save(this.output);
   }

   protected void waxedChiseled(Block p_457878_, Block p_455275_) {
      this.shaped(RecipeCategory.BUILDING_BLOCKS, p_457878_)
         .define('M', p_455275_)
         .pattern(" M ")
         .pattern(" M ")
         .group(getItemName(p_457878_))
         .unlockedBy(getHasName(p_455275_), this.has(p_455275_))
         .save(this.output);
   }

   protected void suspiciousStew(Item p_368852_, SuspiciousEffectHolder p_367042_) {
      ItemStack itemstack = new ItemStack(
         Items.SUSPICIOUS_STEW.builtInRegistryHolder(),
         1,
         DataComponentPatch.builder().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, p_367042_.getSuspiciousEffects()).build()
      );
      this.shapeless(RecipeCategory.FOOD, itemstack)
         .requires(Items.BOWL)
         .requires(Items.BROWN_MUSHROOM)
         .requires(Items.RED_MUSHROOM)
         .requires(p_368852_)
         .group("suspicious_stew")
         .unlockedBy(getHasName(p_368852_), this.has(p_368852_))
         .save(this.output, getItemName(itemstack.getItem()) + "_from_" + getItemName(p_368852_));
   }

   protected void generateRecipes(BlockFamily p_176582_, FeatureFlagSet p_312313_) {
      p_176582_.getVariants()
         .forEach(
            (p_358443_, p_358444_) -> {
               if (p_358444_.requiredFeatures().isSubsetOf(p_312313_)) {
                  RecipeProvider.FamilyRecipeProvider recipeprovider$familyrecipeprovider = SHAPE_BUILDERS.get(p_358443_);
                  ItemLike itemlike = this.getBaseBlock(p_176582_, p_358443_);
                  if (recipeprovider$familyrecipeprovider != null) {
                     RecipeBuilder recipebuilder = recipeprovider$familyrecipeprovider.create(this, p_358444_, itemlike);
                     p_176582_.getRecipeGroupPrefix()
                        .ifPresent(p_296361_ -> recipebuilder.group(p_296361_ + (p_358443_ == BlockFamily.Variant.CUT ? "" : "_" + p_358443_.getRecipeGroup())));
                     recipebuilder.unlockedBy(p_176582_.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                     recipebuilder.save(this.output);
                  }

                  if (p_358443_ == BlockFamily.Variant.CRACKED) {
                     this.smeltingResultFromBase(p_358444_, itemlike);
                  }
               }
            }
         );
   }

   private Block getBaseBlock(BlockFamily p_176524_, BlockFamily.Variant p_176525_) {
      if (p_176525_ == BlockFamily.Variant.CHISELED) {
         if (!p_176524_.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
            throw new IllegalStateException("Slab is not defined for the family.");
         } else {
            return p_176524_.get(BlockFamily.Variant.SLAB);
         }
      } else {
         return p_176524_.getBaseBlock();
      }
   }

   private static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block p_125980_) {
      return CriteriaTriggers.ENTER_BLOCK
         .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(p_125980_.builtInRegistryHolder()), Optional.empty()));
   }

   private Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints p_452393_, ItemLike p_176522_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_176522_).withCount(p_452393_));
   }

   protected Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike p_298497_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_298497_));
   }

   protected Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> p_299059_) {
      return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, p_299059_));
   }

   private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... p_454544_) {
      return inventoryTrigger(Arrays.stream(p_454544_).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
   }

   private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... p_460682_) {
      return CriteriaTriggers.INVENTORY_CHANGED
         .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(p_460682_)));
   }

   protected static String getHasName(ItemLike p_176603_) {
      return "has_" + getItemName(p_176603_);
   }

   protected static String getItemName(ItemLike p_176633_) {
      return BuiltInRegistries.ITEM.getKey(p_176633_.asItem()).getPath();
   }

   protected static String getSimpleRecipeName(ItemLike p_176645_) {
      return getItemName(p_176645_);
   }

   protected static String getConversionRecipeName(ItemLike p_176518_, ItemLike p_176519_) {
      return getItemName(p_176518_) + "_from_" + getItemName(p_176519_);
   }

   protected static String getSmeltingRecipeName(ItemLike p_176657_) {
      return getItemName(p_176657_) + "_from_smelting";
   }

   protected static String getBlastingRecipeName(ItemLike p_176669_) {
      return getItemName(p_176669_) + "_from_blasting";
   }

   protected Ingredient tag(TagKey<Item> p_365400_) {
      return Ingredient.of(this.items.getOrThrow(p_365400_));
   }

   protected ShapedRecipeBuilder shaped(RecipeCategory p_365278_, ItemLike p_365955_) {
      return ShapedRecipeBuilder.shaped(this.items, p_365278_, p_365955_);
   }

   protected ShapedRecipeBuilder shaped(RecipeCategory p_365505_, ItemLike p_362162_, int p_368883_) {
      return ShapedRecipeBuilder.shaped(this.items, p_365505_, p_362162_, p_368883_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_367419_, ItemStack p_369370_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_367419_, p_369370_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_370012_, ItemLike p_361338_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_370012_, p_361338_);
   }

   protected ShapelessRecipeBuilder shapeless(RecipeCategory p_361742_, ItemLike p_365052_, int p_363232_) {
      return ShapelessRecipeBuilder.shapeless(this.items, p_361742_, p_365052_, p_363232_);
   }

   @FunctionalInterface
   interface FamilyRecipeProvider {
      RecipeBuilder create(RecipeProvider var1, ItemLike var2, ItemLike var3);
   }

   protected abstract static class Runner implements DataProvider {
      private final PackOutput packOutput;
      private final CompletableFuture<HolderLookup.Provider> registries;

      protected Runner(PackOutput p_365720_, CompletableFuture<HolderLookup.Provider> p_365098_) {
         this.packOutput = p_365720_;
         this.registries = p_365098_;
      }

      @Override
      public final CompletableFuture<?> run(CachedOutput p_363906_) {
         return this.registries
            .thenCompose(
               p_362805_ -> {
                  final PackOutput.PathProvider packoutput$pathprovider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                  final PackOutput.PathProvider packoutput$pathprovider1 = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                  final Set<ResourceKey<Recipe<?>>> set = Sets.newHashSet();
                  final List<CompletableFuture<?>> list = new ArrayList<>();
                  RecipeOutput recipeoutput = new RecipeOutput() {
                     @Override
                     public void accept(ResourceKey<Recipe<?>> p_361204_, Recipe<?> p_363495_, @Nullable AdvancementHolder p_364191_) {
                        if (!set.add(p_361204_)) {
                           throw new IllegalStateException("Duplicate recipe " + p_361204_.identifier());
                        }

                        this.saveRecipe(p_361204_, p_363495_);
                        if (p_364191_ != null) {
                           this.saveAdvancement(p_364191_);
                        }
                     }

                     @Override
                     public Advancement.Builder advancement() {
                        return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                     }

                     @Override
                     public void includeRootAdvancement() {
                        AdvancementHolder advancementholder = Advancement.Builder.recipeAdvancement()
                           .addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                           .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                        this.saveAdvancement(advancementholder);
                     }

                     private void saveRecipe(ResourceKey<Recipe<?>> p_368864_, Recipe<?> p_368184_) {
                        list.add(DataProvider.saveStable(p_363906_, p_362805_, Recipe.CODEC, p_368184_, packoutput$pathprovider.json(p_368864_.identifier())));
                     }

                     private void saveAdvancement(AdvancementHolder p_361824_) {
                        list.add(
                           DataProvider.saveStable(p_363906_, p_362805_, Advancement.CODEC, p_361824_.value(), packoutput$pathprovider1.json(p_361824_.id()))
                        );
                     }
                  };
                  this.createRecipeProvider(p_362805_, recipeoutput).buildRecipes();
                  return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
               }
            );
      }

      protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider var1, RecipeOutput var2);
   }
}
