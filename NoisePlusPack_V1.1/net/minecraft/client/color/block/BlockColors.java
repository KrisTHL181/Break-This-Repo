package net.minecraft.client.color.block;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockColors {
   private static final int DEFAULT = -1;
   public static final int LILY_PAD_IN_WORLD = -14647248;
   public static final int LILY_PAD_DEFAULT = -9321636;
   private final IdMapper<BlockColor> blockColors = new IdMapper<>(32);
   private final Map<Block, Set<Property<?>>> coloringStates = Maps.newHashMap();

   public static BlockColors createDefault() {
      BlockColors blockcolors = new BlockColors();
      blockcolors.register(
         (p_276233_, p_276234_, p_276235_, p_276236_) -> p_276234_ != null && p_276235_ != null
            ? BiomeColors.getAverageGrassColor(p_276234_, p_276233_.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? p_276235_.below() : p_276235_)
            : GrassColor.getDefaultColor(),
         Blocks.LARGE_FERN,
         Blocks.TALL_GRASS
      );
      blockcolors.addColoringState(DoublePlantBlock.HALF, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
      blockcolors.register(
         (p_276237_, p_276238_, p_276239_, p_276240_) -> p_276238_ != null && p_276239_ != null
            ? BiomeColors.getAverageGrassColor(p_276238_, p_276239_)
            : GrassColor.getDefaultColor(),
         Blocks.GRASS_BLOCK,
         Blocks.FERN,
         Blocks.SHORT_GRASS,
         Blocks.POTTED_FERN,
         Blocks.BUSH
      );
      blockcolors.register((p_276241_, p_276242_, p_276243_, p_276244_) -> {
         if (p_276244_ == 0) {
            return -1;
         } else {
            return p_276242_ != null && p_276243_ != null ? BiomeColors.getAverageGrassColor(p_276242_, p_276243_) : GrassColor.getDefaultColor();
         }
      }, Blocks.PINK_PETALS, Blocks.WILDFLOWERS);
      blockcolors.register((p_92631_, p_92632_, p_92633_, p_92634_) -> -10380959, Blocks.SPRUCE_LEAVES);
      blockcolors.register((p_92606_, p_92607_, p_92608_, p_92609_) -> -8345771, Blocks.BIRCH_LEAVES);
      blockcolors.register(
         (p_92626_, p_92627_, p_92628_, p_92629_) -> p_92627_ != null && p_92628_ != null ? BiomeColors.getAverageFoliageColor(p_92627_, p_92628_) : -12012264,
         Blocks.OAK_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.ACACIA_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.VINE,
         Blocks.MANGROVE_LEAVES
      );
      blockcolors.register(
         (p_389153_, p_389154_, p_389155_, p_389156_) -> p_389154_ != null && p_389155_ != null
            ? BiomeColors.getAverageDryFoliageColor(p_389154_, p_389155_)
            : -10732494,
         Blocks.LEAF_LITTER
      );
      blockcolors.register(
         (p_92621_, p_92622_, p_92623_, p_92624_) -> p_92622_ != null && p_92623_ != null ? BiomeColors.getAverageWaterColor(p_92622_, p_92623_) : -1,
         Blocks.WATER,
         Blocks.BUBBLE_COLUMN,
         Blocks.WATER_CAULDRON
      );
      blockcolors.register(
         (p_92616_, p_92617_, p_92618_, p_92619_) -> RedStoneWireBlock.getColorForPower(p_92616_.getValue(RedStoneWireBlock.POWER)), Blocks.REDSTONE_WIRE
      );
      blockcolors.addColoringState(RedStoneWireBlock.POWER, Blocks.REDSTONE_WIRE);
      blockcolors.register(
         (p_92611_, p_92612_, p_92613_, p_92614_) -> p_92612_ != null && p_92613_ != null ? BiomeColors.getAverageGrassColor(p_92612_, p_92613_) : -1,
         Blocks.SUGAR_CANE
      );
      blockcolors.register((p_92636_, p_92637_, p_92638_, p_92639_) -> -2046180, Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
      blockcolors.register((p_357650_, p_357651_, p_357652_, p_357653_) -> {
         int i = p_357650_.getValue(StemBlock.AGE);
         return ARGB.color(i * 32, 255 - i * 8, i * 4);
      }, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
      blockcolors.addColoringState(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
      blockcolors.register((p_92596_, p_92597_, p_92598_, p_92599_) -> p_92597_ != null && p_92598_ != null ? -14647248 : -9321636, Blocks.LILY_PAD);
      return blockcolors;
   }

   public int getColor(BlockState p_92583_, Level p_92584_, BlockPos p_92585_) {
      BlockColor blockcolor = this.blockColors.byId(BuiltInRegistries.BLOCK.getId(p_92583_.getBlock()));
      if (blockcolor != null) {
         return blockcolor.getColor(p_92583_, null, null, 0);
      }

      MapColor mapcolor = p_92583_.getMapColor(p_92584_, p_92585_);
      return mapcolor != null ? mapcolor.col : -1;
   }

   public int getColor(BlockState p_92578_, @Nullable BlockAndTintGetter p_92579_, @Nullable BlockPos p_92580_, int p_92581_) {
      BlockColor blockcolor = this.blockColors.byId(BuiltInRegistries.BLOCK.getId(p_92578_.getBlock()));
      return blockcolor == null ? -1 : blockcolor.getColor(p_92578_, p_92579_, p_92580_, p_92581_);
   }

   public void register(BlockColor p_92590_, Block... p_92591_) {
      for (Block block : p_92591_) {
         this.blockColors.addMapping(p_92590_, BuiltInRegistries.BLOCK.getId(block));
      }
   }

   private void addColoringStates(Set<Property<?>> p_92593_, Block... p_92594_) {
      for (Block block : p_92594_) {
         this.coloringStates.put(block, p_92593_);
      }
   }

   private void addColoringState(Property<?> p_92587_, Block... p_92588_) {
      this.addColoringStates(ImmutableSet.of(p_92587_), p_92588_);
   }

   public Set<Property<?>> getColoringProperties(Block p_92576_) {
      return this.coloringStates.getOrDefault(p_92576_, ImmutableSet.of());
   }
}
