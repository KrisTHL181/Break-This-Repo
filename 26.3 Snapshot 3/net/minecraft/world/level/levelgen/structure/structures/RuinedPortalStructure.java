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
      i -> i.group(settingsCodec(i), ExtraCodecs.nonEmptyList(RuinedPortalStructure.Setup.CODEC.listOf()).fieldOf("setups").forGetter(s -> s.setups))
         .apply(i, RuinedPortalStructure::new)
   );

   public RuinedPortalStructure(final Structure.StructureSettings settings, final List<RuinedPortalStructure.Setup> setups) {
      super(settings);
      this.setups = setups;
   }

   public RuinedPortalStructure(final Structure.StructureSettings settings, final RuinedPortalStructure.Setup setup) {
      this(settings, List.of(setup));
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      WorldgenRandom random = context.random();
      RuinedPortalStructure.Setup chosenSetup = null;
      if (this.setups.size() > 1) {
         float total = 0.0F;

         for (RuinedPortalStructure.Setup s : this.setups) {
            total += s.weight();
         }

         float pick = random.nextFloat();

         for (RuinedPortalStructure.Setup s : this.setups) {
            pick -= s.weight() / total;
            if (pick < 0.0F) {
               chosenSetup = s;
               break;
            }
         }
      } else {
         chosenSetup = this.setups.get(0);
      }

      if (chosenSetup == null) {
         throw new IllegalStateException();
      }

      RuinedPortalStructure.Setup setup = chosenSetup;
      boolean airPocket = sample(random, setup.airPocketProbability());
      Identifier templateLocation;
      if (random.nextFloat() < 0.05F) {
         templateLocation = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_GIANT_PORTALS[random.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
      } else {
         templateLocation = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_PORTALS[random.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
      }

      StructureTemplate template = context.structureTemplateManager().getOrCreate(templateLocation);
      Rotation rotation = Util.getRandom(Rotation.values(), random);
      Mirror mirror = random.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
      BlockPos pivot = new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
      ChunkGenerator chunkGenerator = context.chunkGenerator();
      LevelHeightAccessor heightAccessor = context.heightAccessor();
      RandomState randomState = context.randomState();
      BlockPos basePosition = context.chunkPos().getWorldPosition();
      BoundingBox boundingBox = template.getBoundingBox(basePosition, rotation, pivot, mirror);
      BlockPos center = boundingBox.getCenter();
      int surfaceY = chunkGenerator.getBaseHeight(
            center.getX(), center.getZ(), RuinedPortalPiece.getHeightMapType(setup.placement()), heightAccessor, randomState
         )
         - 1;
      int projectedY = findSuitableY(
         random, chunkGenerator, setup.placement(), airPocket, surfaceY, boundingBox.getYSpan(), boundingBox, heightAccessor, randomState
      );
      BlockPos origin = new BlockPos(basePosition.getX(), projectedY, basePosition.getZ());
      return Optional.of(
         new Structure.GenerationStub(
            origin,
            builder -> {
               RuinedPortalPiece.Properties properties = new RuinedPortalPiece.Properties(
                  setup.canBeCold()
                     && isCold(
                        origin,
                        context.chunkGenerator()
                           .getBiomeSource()
                           .getNoiseBiome(
                              QuartPos.fromBlock(origin.getX()), QuartPos.fromBlock(origin.getY()), QuartPos.fromBlock(origin.getZ()), randomState.sampler()
                           ),
                        chunkGenerator.getSeaLevel()
                     ),
                  setup.mossiness(),
                  airPocket,
                  setup.overgrown(),
                  setup.vines(),
                  setup.replaceWithBlackstone()
               );
               builder.addPiece(
                  new RuinedPortalPiece(
                     context.registryAccess(),
                     context.structureTemplateManager(),
                     origin,
                     setup.placement(),
                     properties,
                     templateLocation,
                     template,
                     rotation,
                     mirror,
                     pivot
                  )
               );
            }
         )
      );
   }

   private static boolean sample(final WorldgenRandom random, final float limit) {
      if (limit == 0.0F) {
         return false;
      } else {
         return limit == 1.0F ? true : random.nextFloat() < limit;
      }
   }

   private static boolean isCold(final BlockPos pos, final Holder<Biome> biome, final int seaLevel) {
      return biome.value().coldEnoughToSnow(pos, seaLevel);
   }

   private static int findSuitableY(
      final RandomSource random,
      final ChunkGenerator generator,
      final RuinedPortalPiece.VerticalPlacement verticalPlacement,
      final boolean airPocket,
      final int surfaceYAtCenter,
      final int ySpan,
      final BoundingBox boundingBox,
      final LevelHeightAccessor heightAccessor,
      final RandomState randomState
   ) {
      int minY = heightAccessor.getMinY() + 15;
      int newY;
      if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
         if (airPocket) {
            newY = Mth.randomBetweenInclusive(random, 32, 100);
         } else if (random.nextFloat() < 0.5F) {
            newY = Mth.randomBetweenInclusive(random, 27, 29);
         } else {
            newY = Mth.randomBetweenInclusive(random, 29, 100);
         }
      } else if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
         int maxY = surfaceYAtCenter - ySpan;
         newY = getRandomWithinInterval(random, 70, maxY);
      } else if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
         int maxY = surfaceYAtCenter - ySpan;
         newY = getRandomWithinInterval(random, minY, maxY);
      } else if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED) {
         newY = surfaceYAtCenter - ySpan + Mth.randomBetweenInclusive(random, 2, 8);
      } else {
         newY = surfaceYAtCenter;
      }

      List<BlockPos> bottomCorners = ImmutableList.of(
         new BlockPos(boundingBox.minX(), 0, boundingBox.minZ()),
         new BlockPos(boundingBox.maxX(), 0, boundingBox.minZ()),
         new BlockPos(boundingBox.minX(), 0, boundingBox.maxZ()),
         new BlockPos(boundingBox.maxX(), 0, boundingBox.maxZ())
      );
      List<NoiseColumn> columns = bottomCorners.stream()
         .map(p -> generator.getBaseColumn(p.getX(), p.getZ(), heightAccessor, randomState))
         .collect(Collectors.toList());
      Heightmap.Types heightmap = verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR
         ? Heightmap.Types.OCEAN_FLOOR_WG
         : Heightmap.Types.WORLD_SURFACE_WG;

      int projectedY;
      for (projectedY = newY; projectedY > minY; projectedY--) {
         int cornersOnSolidGround = 0;

         for (NoiseColumn column : columns) {
            BlockState blockState = column.getBlock(projectedY);
            if (heightmap.isOpaque().test(blockState)) {
               if (++cornersOnSolidGround == 3) {
                  return projectedY;
               }
            }
         }
      }

      return projectedY;
   }

   private static int getRandomWithinInterval(final RandomSource random, final int minPreferred, final int max) {
      return minPreferred < max ? Mth.randomBetweenInclusive(random, minPreferred, max) : max;
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
         i -> i.group(
               RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(RuinedPortalStructure.Setup::placement),
               Codec.floatRange(0.0F, 1.0F).fieldOf("air_pocket_probability").forGetter(RuinedPortalStructure.Setup::airPocketProbability),
               Codec.floatRange(0.0F, 1.0F).fieldOf("mossiness").forGetter(RuinedPortalStructure.Setup::mossiness),
               Codec.BOOL.fieldOf("overgrown").forGetter(RuinedPortalStructure.Setup::overgrown),
               Codec.BOOL.fieldOf("vines").forGetter(RuinedPortalStructure.Setup::vines),
               Codec.BOOL.fieldOf("can_be_cold").forGetter(RuinedPortalStructure.Setup::canBeCold),
               Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(RuinedPortalStructure.Setup::replaceWithBlackstone),
               ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(RuinedPortalStructure.Setup::weight)
            )
            .apply(i, RuinedPortalStructure.Setup::new)
      );
   }
}
