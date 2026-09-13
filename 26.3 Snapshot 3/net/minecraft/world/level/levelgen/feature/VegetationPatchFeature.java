package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.util.Function10;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class VegetationPatchFeature implements Feature {
   public static final MapCodec<VegetationPatchFeature> CODEC = makeCodec(VegetationPatchFeature::new);
   protected final HolderSet<Block> replaceable;
   protected final BlockStateProvider groundState;
   protected final Holder<PlacedFeature> vegetationFeature;
   protected final CaveSurface surface;
   protected final IntProvider depth;
   protected final float extraBottomBlockChance;
   protected final int verticalRange;
   protected final float vegetationChance;
   protected final IntProvider xzRadius;
   protected final float extraEdgeColumnChance;

   protected static <T extends VegetationPatchFeature> MapCodec<T> makeCodec(
      final Function10<HolderSet<Block>, BlockStateProvider, Holder<PlacedFeature>, CaveSurface, IntProvider, Float, Integer, Float, IntProvider, Float, T> constructor
   ) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable").forGetter(f -> f.replaceable),
               BlockStateProvider.CODEC.fieldOf("ground_state").forGetter(f -> f.groundState),
               PlacedFeature.CODEC.fieldOf("vegetation_feature").forGetter(f -> f.vegetationFeature),
               CaveSurface.CODEC.fieldOf("surface").forGetter(f -> f.surface),
               IntProviders.codec(1, 128).fieldOf("depth").forGetter(f -> f.depth),
               Codec.floatRange(0.0F, 1.0F).fieldOf("extra_bottom_block_chance").forGetter(f -> f.extraBottomBlockChance),
               Codec.intRange(1, 256).fieldOf("vertical_range").forGetter(f -> f.verticalRange),
               Codec.floatRange(0.0F, 1.0F).fieldOf("vegetation_chance").forGetter(f -> f.vegetationChance),
               IntProviders.CODEC.fieldOf("xz_radius").forGetter(f -> f.xzRadius),
               Codec.floatRange(0.0F, 1.0F).fieldOf("extra_edge_column_chance").forGetter(f -> f.extraEdgeColumnChance)
            )
            .apply(i, constructor)
      );
   }

   public VegetationPatchFeature(
      final HolderSet<Block> replaceable,
      final BlockStateProvider groundState,
      final Holder<PlacedFeature> vegetationFeature,
      final CaveSurface surface,
      final IntProvider depth,
      final float extraBottomBlockChance,
      final int verticalRange,
      final float vegetationChance,
      final IntProvider xzRadius,
      final float extraEdgeColumnChance
   ) {
      this.replaceable = replaceable;
      this.groundState = groundState;
      this.vegetationFeature = vegetationFeature;
      this.surface = surface;
      this.depth = depth;
      this.extraBottomBlockChance = extraBottomBlockChance;
      this.verticalRange = verticalRange;
      this.vegetationChance = vegetationChance;
      this.xzRadius = xzRadius;
      this.extraEdgeColumnChance = extraEdgeColumnChance;
   }

   @Override
   public MapCodec<? extends VegetationPatchFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int xRadius = this.xzRadius.sample(random) + 1;
      int zRadius = this.xzRadius.sample(random) + 1;
      Set<BlockPos> surface = this.placeGroundPatch(level, random, origin, s -> s.is(this.replaceable), xRadius, zRadius);
      this.distributeVegetation(level, chunkGenerator, random, surface);
      return !surface.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(
      final WorldGenLevel level,
      final RandomSource random,
      final BlockPos origin,
      final Predicate<BlockState> replaceable,
      final int xRadius,
      final int zRadius
   ) {
      BlockPos.MutableBlockPos pos = origin.mutable();
      BlockPos.MutableBlockPos belowPos = pos.mutable();
      Direction inwards = this.surface.getDirection();
      Direction outwards = inwards.getOpposite();
      Set<BlockPos> surface = new HashSet<>();

      for (int dx = -xRadius; dx <= xRadius; dx++) {
         boolean isXEdge = dx == -xRadius || dx == xRadius;

         for (int dz = -zRadius; dz <= zRadius; dz++) {
            boolean isZEdge = dz == -zRadius || dz == zRadius;
            boolean isEdge = isXEdge || isZEdge;
            boolean isCorner = isXEdge && isZEdge;
            boolean isEdgeButNotCorner = isEdge && !isCorner;
            if (!isCorner && (!isEdgeButNotCorner || this.extraEdgeColumnChance != 0.0F && !(random.nextFloat() > this.extraEdgeColumnChance))) {
               pos.setWithOffset(origin, dx, 0, dz);

               for (int offset = 0; level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::isAir) && offset < this.verticalRange; offset++) {
                  pos.move(inwards);
               }

               for (int var24 = 0; level.isStateAtPosition(pos, s -> !s.isAir()) && var24 < this.verticalRange; var24++) {
                  pos.move(outwards);
               }

               belowPos.setWithOffset(pos, this.surface.getDirection());
               BlockState belowState = level.getBlockState(belowPos);
               if (level.isEmptyBlock(pos) && belowState.isFaceSturdy(level, belowPos, this.surface.getDirection().getOpposite())) {
                  int depth = this.depth.sample(random) + (this.extraBottomBlockChance > 0.0F && random.nextFloat() < this.extraBottomBlockChance ? 1 : 0);
                  BlockPos groundPos = belowPos.immutable();
                  boolean groundPlaced = this.placeGround(level, replaceable, random, belowPos, depth);
                  if (groundPlaced) {
                     surface.add(groundPos);
                  }
               }
            }
         }
      }

      return surface;
   }

   private void distributeVegetation(final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final Set<BlockPos> surface) {
      for (BlockPos surfacePos : surface) {
         if (this.vegetationChance > 0.0F && random.nextFloat() < this.vegetationChance) {
            this.placeVegetation(level, generator, random, surfacePos);
         }
      }
   }

   protected boolean placeVegetation(final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final BlockPos vegetationPos) {
      return this.vegetationFeature.value().place(level, generator, random, vegetationPos.relative(this.surface.getDirection().getOpposite()));
   }

   private boolean placeGround(
      final WorldGenLevel level, final Predicate<BlockState> replaceable, final RandomSource random, final BlockPos.MutableBlockPos belowPos, final int depth
   ) {
      for (int i = 0; i < depth; i++) {
         BlockState stateToPlace = this.groundState.getState(level, random, belowPos);
         BlockState belowState = level.getBlockState(belowPos);
         if (!stateToPlace.is(belowState.getBlock())) {
            if (!replaceable.test(belowState)) {
               return i != 0;
            }

            level.setBlock(belowPos, stateToPlace, 2);
            belowPos.move(this.surface.getDirection());
         }
      }

      return true;
   }
}
