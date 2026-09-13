package net.minecraft.data;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class BlockFamily {
   private final Block baseBlock;
   private final Map<BlockFamily.Variant, Block> variants = Maps.newHashMap();
   private boolean generateModel = true;
   private boolean generateCraftingRecipe = true;
   private boolean generateSmeltingRecipe = true;
   private boolean generateStonecutterRecipe = false;
   private @Nullable String recipeGroupPrefix;
   private @Nullable String recipeUnlockedBy;

   private BlockFamily(final Block baseBlock) {
      this.baseBlock = baseBlock;
   }

   public Block getBaseBlock() {
      return this.baseBlock;
   }

   public Map<BlockFamily.Variant, Block> getVariants() {
      return this.variants;
   }

   public Block get(final BlockFamily.Variant variant) {
      return this.variants.get(variant);
   }

   public boolean shouldGenerateModel() {
      return this.generateModel;
   }

   public boolean shouldGenerateCraftingRecipe() {
      return this.generateCraftingRecipe;
   }

   public boolean shouldGenerateSmeltingRecipe() {
      return this.generateSmeltingRecipe;
   }

   public boolean shouldGenerateStonecutterRecipe() {
      return this.generateStonecutterRecipe;
   }

   public Optional<String> getRecipeGroupPrefix() {
      return StringUtil.isBlank(this.recipeGroupPrefix) ? Optional.empty() : Optional.of(this.recipeGroupPrefix);
   }

   public Optional<String> getRecipeUnlockedBy() {
      return StringUtil.isBlank(this.recipeUnlockedBy) ? Optional.empty() : Optional.of(this.recipeUnlockedBy);
   }

   public static class Builder {
      private final BlockFamily family;

      public Builder(final Block baseBlock) {
         this.family = new BlockFamily(baseBlock);
      }

      public BlockFamily getFamily() {
         return this.family;
      }

      public BlockFamily.Builder button(final Block button) {
         this.family.variants.put(BlockFamily.Variant.BUTTON, button);
         return this;
      }

      public BlockFamily.Builder carpet(final Block carpet) {
         this.family.variants.put(BlockFamily.Variant.CARPET, carpet);
         return this;
      }

      public BlockFamily.Builder chiseled(final Block chiseled) {
         this.family.variants.put(BlockFamily.Variant.CHISELED, chiseled);
         return this;
      }

      public BlockFamily.Builder mosaic(final Block mosaic) {
         this.family.variants.put(BlockFamily.Variant.MOSAIC, mosaic);
         return this;
      }

      public BlockFamily.Builder cracked(final Block cracked) {
         this.family.variants.put(BlockFamily.Variant.CRACKED, cracked);
         return this;
      }

      public BlockFamily.Builder tiles(final Block tiles) {
         this.family.variants.put(BlockFamily.Variant.TILES, tiles);
         return this;
      }

      public BlockFamily.Builder pillar(final Block pillar) {
         this.family.variants.put(BlockFamily.Variant.PILLAR, pillar);
         return this;
      }

      public BlockFamily.Builder cut(final Block cut) {
         this.family.variants.put(BlockFamily.Variant.CUT, cut);
         return this;
      }

      public BlockFamily.Builder door(final Block door) {
         this.family.variants.put(BlockFamily.Variant.DOOR, door);
         return this;
      }

      public BlockFamily.Builder customFence(final Block fence) {
         this.family.variants.put(BlockFamily.Variant.CUSTOM_FENCE, fence);
         return this;
      }

      public BlockFamily.Builder fence(final Block fence) {
         this.family.variants.put(BlockFamily.Variant.FENCE, fence);
         return this;
      }

      public BlockFamily.Builder customFenceGate(final Block fenceGate) {
         this.family.variants.put(BlockFamily.Variant.CUSTOM_FENCE_GATE, fenceGate);
         return this;
      }

      public BlockFamily.Builder fenceGate(final Block fenceGate) {
         this.family.variants.put(BlockFamily.Variant.FENCE_GATE, fenceGate);
         return this;
      }

      public BlockFamily.Builder sign(final Block sign, final Block wallSign) {
         this.family.variants.put(BlockFamily.Variant.SIGN, sign);
         this.family.variants.put(BlockFamily.Variant.WALL_SIGN, wallSign);
         return this;
      }

      public BlockFamily.Builder customHangingSign(final Block sign, final Block wallSign) {
         this.family.variants.put(BlockFamily.Variant.CUSTOM_HANGING_SIGN, sign);
         this.family.variants.put(BlockFamily.Variant.CUSTOM_WALL_HANGING_SIGN, wallSign);
         return this;
      }

      public BlockFamily.Builder hangingSign(final Block sign, final Block wallSign) {
         this.family.variants.put(BlockFamily.Variant.HANGING_SIGN, sign);
         this.family.variants.put(BlockFamily.Variant.WALL_HANGING_SIGN, wallSign);
         return this;
      }

      public BlockFamily.Builder log(final Block log) {
         this.family.variants.put(BlockFamily.Variant.LOG, log);
         return this;
      }

      public BlockFamily.Builder strippedLog(final Block strippedLog) {
         this.family.variants.put(BlockFamily.Variant.STRIPPED_LOG, strippedLog);
         return this;
      }

      public BlockFamily.Builder slab(final Block slab) {
         this.family.variants.put(BlockFamily.Variant.SLAB, slab);
         return this;
      }

      public BlockFamily.Builder stairs(final Block stairs) {
         this.family.variants.put(BlockFamily.Variant.STAIRS, stairs);
         return this;
      }

      public BlockFamily.Builder pressurePlate(final Block pressurePlate) {
         this.family.variants.put(BlockFamily.Variant.PRESSURE_PLATE, pressurePlate);
         return this;
      }

      public BlockFamily.Builder polished(final Block polished) {
         this.family.variants.put(BlockFamily.Variant.POLISHED, polished);
         return this;
      }

      public BlockFamily.Builder trapdoor(final Block trapdoor) {
         this.family.variants.put(BlockFamily.Variant.TRAPDOOR, trapdoor);
         return this;
      }

      public BlockFamily.Builder wall(final Block wall) {
         this.family.variants.put(BlockFamily.Variant.WALL, wall);
         return this;
      }

      public BlockFamily.Builder cobbled(final Block cobble) {
         this.family.variants.put(BlockFamily.Variant.COBBLED, cobble);
         return this;
      }

      public BlockFamily.Builder bricks(final Block bricks) {
         this.family.variants.put(BlockFamily.Variant.BRICKS, bricks);
         return this;
      }

      public BlockFamily.Builder dontGenerateModel() {
         this.family.generateModel = false;
         return this;
      }

      public BlockFamily.Builder dontGenerateCraftingRecipe() {
         this.family.generateCraftingRecipe = false;
         return this;
      }

      public BlockFamily.Builder dontGenerateSmeltingRecipe() {
         this.family.generateSmeltingRecipe = false;
         return this;
      }

      public BlockFamily.Builder generateStonecutterRecipe() {
         this.family.generateStonecutterRecipe = true;
         return this;
      }

      public BlockFamily.Builder recipeGroupPrefix(final String recipeGroupPrefix) {
         this.family.recipeGroupPrefix = recipeGroupPrefix;
         return this;
      }

      public BlockFamily.Builder recipeUnlockedBy(final String recipeUnlockedBy) {
         this.family.recipeUnlockedBy = recipeUnlockedBy;
         return this;
      }
   }

   public enum Variant {
      BUTTON("button"),
      CARPET("carpet"),
      CHISELED("chiseled") {
         @Override
         public BlockFamily.Variant getBaseVariantForCrafting() {
            return SLAB;
         }
      },
      CRACKED("cracked"),
      CUT("cut") {
         @Override
         public String getPrefixedRecipeGroup(final String prefix) {
            return prefix;
         }
      },
      DOOR("door"),
      CUSTOM_FENCE("fence"),
      FENCE("fence"),
      CUSTOM_FENCE_GATE("fence_gate"),
      FENCE_GATE("fence_gate"),
      CUSTOM_HANGING_SIGN("hanging_sign") {
         @Override
         public BlockFamily.Variant getBaseVariantForCrafting() {
            return STRIPPED_LOG;
         }
      },
      HANGING_SIGN("hanging_sign") {
         @Override
         public BlockFamily.Variant getBaseVariantForCrafting() {
            return STRIPPED_LOG;
         }
      },
      LOG("log"),
      STRIPPED_LOG("stripped_log"),
      MOSAIC("mosaic"),
      SIGN("sign"),
      SLAB("slab"),
      STAIRS("stairs"),
      PRESSURE_PLATE("pressure_plate"),
      POLISHED("polished"),
      TRAPDOOR("trapdoor"),
      WALL("wall"),
      WALL_SIGN("wall_sign"),
      CUSTOM_WALL_HANGING_SIGN("wall_hanging_sign"),
      WALL_HANGING_SIGN("wall_hanging_sign"),
      BRICKS("bricks"),
      COBBLED("cobbled"),
      TILES("tiles"),
      PILLAR("pillar");

      private final String recipeGroup;

      Variant(final String recipeGroup) {
         this.recipeGroup = recipeGroup;
      }

      public String getRecipeGroup() {
         return this.recipeGroup;
      }

      public String getPrefixedRecipeGroup(final String prefix) {
         return prefix + "_" + this.getRecipeGroup();
      }

      public BlockFamily.@Nullable Variant getBaseVariantForCrafting() {
         return null;
      }
   }
}
