package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure extends Structure {
   private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{
      "ruined_portal/portal_1",
      "ruined_portal/portal_2",
      "ruined_portal/portal_3",
      "ruined_portal/portal_4",
      "ruined_portal/portal_5",
      "ruined_portal/portal_6",
      "ruined_portal/portal_7",
      "ruined_portal/portal_8",
      "ruined_portal/portal_9",
      "ruined_portal/portal_10"
   };
   private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{
      "ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"
   };
   private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05F;
   private static final int MIN_Y_INDEX = 15;
   private final List<RuinedPortalStructure.Setup> setups;
   public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(
      p_229304_ -> p_229304_.group(
            settingsCodec(p_229304_),
            ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter(p_229299_ -> p_229299_.setups)
         )
         .apply(p_229304_, RuinedPortalStructure::new)
   );

   public RuinedPortalStructure(Structure.StructureSettings p_229260_, List<RuinedPortalStructure.Setup> p_229261_) {
      super(p_229260_);
      this.setups = p_229261_;
   }

   public RuinedPortalStructure(Structure.StructureSettings p_229257_, RuinedPortalStructure.Setup p_229258_) {
      this(p_229257_, List.of(p_229258_));
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229285_) {
      RuinedPortalPiece.Properties ruinedportalpiece$properties = new RuinedPortalPiece.Properties();
      WorldgenRandom worldgenrandom = p_229285_.random();
      RuinedPortalStructure.Setup ruinedportalstructure$setup = null;
      if (this.setups.size() > 1) {
         float f = 0.0F;

         for (RuinedPortalStructure.Setup ruinedportalstructure$setup1 : this.setups) {
            f += ruinedportalstructure$setup1.weight();
         }

         float f1 = worldgenrandom.nextFloat();

         for (RuinedPortalStructure.Setup ruinedportalstructure$setup2 : this.setups) {
            f1 -= ruinedportalstructure$setup2.weight() / f;
            if (f1 < 0.0F) {
               ruinedportalstructure$setup = ruinedportalstructure$setup2;
               break;
            }
         }
      } else {
         ruinedportalstructure$setup = this.setups.get(0);
      }

      if (ruinedportalstructure$setup == null) {
         throw new IllegalStateException();
      }

      RuinedPortalStructure.Setup ruinedportalstructure$setup3 = ruinedportalstructure$setup;
      ruinedportalpiece$properties.airPocket = sample(worldgenrandom, ruinedportalstructure$setup3.airPocketProbability());
      ruinedportalpiece$properties.mossiness = ruinedportalstructure$setup3.mossiness();
      ruinedportalpiece$properties.overgrown = ruinedportalstructure$setup3.overgrown();
      ruinedportalpiece$properties.vines = ruinedportalstructure$setup3.vines();
      ruinedportalpiece$properties.replaceWithBlackstone = ruinedportalstructure$setup3.replaceWithBlackstone();
      Identifier identifier;
      if (worldgenrandom.nextFloat() < 0.05F) {
         identifier = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_GIANT_PORTALS[worldgenrandom.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
      } else {
         identifier = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_PORTALS[worldgenrandom.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
      }

      StructureTemplate structuretemplate = p_229285_.structureTemplateManager().getOrCreate(identifier);
      Rotation rotation = Util.getRandom(Rotation.values(), worldgenrandom);
      Mirror mirror = worldgenrandom.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
      BlockPos blockpos = new BlockPos(structuretemplate.getSize().getX() / 2, 0, structuretemplate.getSize().getZ() / 2);
      ChunkGenerator chunkgenerator = p_229285_.chunkGenerator();
      LevelHeightAccessor levelheightaccessor = p_229285_.heightAccessor();
      RandomState randomstate = p_229285_.randomState();
      BlockPos blockpos1 = p_229285_.chunkPos().getWorldPosition();
      BoundingBox boundingbox = structuretemplate.getBoundingBox(blockpos1, rotation, blockpos, mirror);
      BlockPos blockpos2 = boundingbox.getCenter();
      int i = chunkgenerator.getBaseHeight(
            blockpos2.getX(), blockpos2.getZ(), RuinedPortalPiece.getHeightMapType(ruinedportalstructure$setup3.placement()), levelheightaccessor, randomstate
         )
         - 1;
      int j = findSuitableY(
         worldgenrandom,
         chunkgenerator,
         ruinedportalstructure$setup3.placement(),
         ruinedportalpiece$properties.airPocket,
         i,
         boundingbox.getYSpan(),
         boundingbox,
         levelheightaccessor,
         randomstate
      );
      BlockPos blockpos3 = new BlockPos(blockpos1.getX(), j, blockpos1.getZ());
      return Optional.of(
         new Structure.GenerationStub(
            blockpos3,
            p_229297_ -> {
               if (ruinedportalstructure$setup3.canBeCold()) {
                  ruinedportalpiece$properties.cold = isCold(
                     blockpos3,
                     p_229285_.chunkGenerator()
                        .getBiomeSource()
                        .getNoiseBiome(
                           QuartPos.fromBlock(blockpos3.getX()),
                           QuartPos.fromBlock(blockpos3.getY()),
                           QuartPos.fromBlock(blockpos3.getZ()),
                           randomstate.sampler()
                        ),
                     chunkgenerator.getSeaLevel()
                  );
               }

               p_229297_.addPiece(
                  new RuinedPortalPiece(
                     p_229285_.structureTemplateManager(),
                     blockpos3,
                     ruinedportalstructure$setup3.placement(),
                     ruinedportalpiece$properties,
                     identifier,
                     structuretemplate,
                     rotation,
                     mirror,
                     blockpos
                  )
               );
            }
         )
      );
   }

   private static boolean sample(WorldgenRandom p_229282_, float p_229283_) {
      if (p_229283_ == 0.0F) {
         return false;
      } else {
         return p_229283_ == 1.0F ? true : p_229282_.nextFloat() < p_229283_;
      }
   }

   private static boolean isCold(BlockPos p_229301_, Holder<Biome> p_229302_, int p_361622_) {
      return p_229302_.value().coldEnoughToSnow(p_229301_, p_361622_);
   }

   private static int findSuitableY(
      RandomSource p_229267_,
      ChunkGenerator p_229268_,
      RuinedPortalPiece.VerticalPlacement p_229269_,
      boolean p_229270_,
      int p_229271_,
      int p_229272_,
      BoundingBox p_229273_,
      LevelHeightAccessor p_229274_,
      RandomState p_229275_
   ) {
      int j = p_229274_.getMinY() + 15;
      int i;
      if (p_229269_ == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
         if (p_229270_) {
            i = Mth.randomBetweenInclusive(p_229267_, 32, 100);
         } else if (p_229267_.nextFloat() < 0.5F) {
            i = Mth.randomBetweenInclusive(p_229267_, 27, 29);
         } else {
            i = Mth.randomBetweenInclusive(p_229267_, 29, 100);
         }
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
         int k = p_229271_ - p_229272_;
         i = getRandomWithinInterval(p_229267_, 70, k);
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
         int j1 = p_229271_ - p_229272_;
         i = getRandomWithinInterval(p_229267_, j, j1);
      } else if (p_229269_ == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
         i = p_229271_ - p_229272_ + Mth.randomBetweenInclusive(p_229267_, 2, 8);
      } else {
         i = p_229271_;
      }

      List<BlockPos> list1 = ImmutableList.of(
         new BlockPos(p_229273_.minX(), 0, p_229273_.minZ()),
         new BlockPos(p_229273_.maxX(), 0, p_229273_.minZ()),
         new BlockPos(p_229273_.minX(), 0, p_229273_.maxZ()),
         new BlockPos(p_229273_.maxX(), 0, p_229273_.maxZ())
      );
      List<NoiseColumn> list = list1.stream()
         .map(p_229280_ -> p_229268_.getBaseColumn(p_229280_.getX(), p_229280_.getZ(), p_229274_, p_229275_))
         .collect(Collectors.toList());
      Heightmap.Types heightmap$types = p_229269_ == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR
         ? Heightmap.Types.OCEAN_FLOOR_WG
         : Heightmap.Types.WORLD_SURFACE_WG;

      int l;
      for (l = i; l > j; l--) {
         int i1 = 0;

         for (NoiseColumn noisecolumn : list) {
            BlockState blockstate = noisecolumn.getBlock(l);
            if (heightmap$types.isOpaque().test(blockstate)) {
               if (++i1 == 3) {
                  return l;
               }
            }
         }
      }

      return l;
   }

   private static int getRandomWithinInterval(RandomSource p_229263_, int p_229264_, int p_229265_) {
      return p_229264_ < p_229265_ ? Mth.randomBetweenInclusive(p_229263_, p_229264_, p_229265_) : p_229265_;
   }

   @Override
   public StructureType<?> type() {
      return StructureType.RUINED_PORTAL;
   }

   public record Setup(
      RuinedPortalPiece.VerticalPlacement placement,
      float airPocketProbability,
      float mossiness,
      boolean overgrown,
      boolean vines,
      boolean canBeCold,
      boolean replaceWithBlackstone,
      float weight
   ) {
      public static final Codec<RuinedPortalStructure.Setup> CODEC = RecordCodecBuilder.create(
         p_229327_ -> p_229327_.group(
               RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement),
               Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability),
               Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness),
               Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
               Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
               Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
               Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
               ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
            )
            .apply(p_229327_, RuinedPortalStructure.Setup::new)
      );
   }
}
