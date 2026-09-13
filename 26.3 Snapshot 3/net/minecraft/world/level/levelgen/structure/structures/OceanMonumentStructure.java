package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanMonumentStructure extends Structure {
   public static final MapCodec<OceanMonumentStructure> CODEC = simpleCodec(OceanMonumentStructure::new);

   public OceanMonumentStructure(final Structure.StructureSettings settings) {
      super(settings);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      int offsetX = context.chunkPos().getBlockX(9);
      int offsetZ = context.chunkPos().getBlockZ(9);

      for (Holder<Biome> biome : context.biomeSource()
         .getBiomesWithin(offsetX, context.chunkGenerator().getSeaLevel(), offsetZ, 29, context.randomState().sampler())) {
         if (!biome.is(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
            return Optional.empty();
         }
      }

      return onTopOfChunkCenter(context, Heightmap.Types.OCEAN_FLOOR_WG, builder -> generatePieces(builder, context));
   }

   private static StructurePiece createTopPiece(final ChunkPos chunkPos, final WorldgenRandom random) {
      int west = chunkPos.getMinBlockX() - 29;
      int north = chunkPos.getMinBlockZ() - 29;
      Direction orientation = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      return new OceanMonumentPieces.MonumentBuilding(random, west, north, orientation);
   }

   private static void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      builder.addPiece(createTopPiece(context.chunkPos(), context.random()));
   }

   public static PiecesContainer regeneratePiecesAfterLoad(final ChunkPos chunkPos, final long seed, final PiecesContainer savedPieces) {
      if (savedPieces.isEmpty()) {
         return savedPieces;
      }

      WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      random.setLargeFeatureSeed(seed, chunkPos.x(), chunkPos.z());
      StructurePiece oldTopPiece = savedPieces.pieces().get(0);
      BoundingBox oldBoundingBox = oldTopPiece.getBoundingBox();
      int west = oldBoundingBox.minX();
      int north = oldBoundingBox.minZ();
      Direction defaultOrientation = Direction.Plane.HORIZONTAL.getRandomDirection(random);
      Direction orientation = Objects.requireNonNullElse(oldTopPiece.getOrientation(), defaultOrientation);
      StructurePiece topPiece = new OceanMonumentPieces.MonumentBuilding(random, west, north, orientation);
      StructurePiecesBuilder result = new StructurePiecesBuilder();
      result.addPiece(topPiece);
      return result.build();
   }

   @Override
   public StructureType<?> type() {
      return StructureType.OCEAN_MONUMENT;
   }
}
