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

   public OceanMonumentStructure(Structure.StructureSettings p_228955_) {
      super(p_228955_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_228964_) {
      int i = p_228964_.chunkPos().getBlockX(9);
      int j = p_228964_.chunkPos().getBlockZ(9);

      for (Holder<Biome> holder : p_228964_.biomeSource()
         .getBiomesWithin(i, p_228964_.chunkGenerator().getSeaLevel(), j, 29, p_228964_.randomState().sampler())) {
         if (!holder.is(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
            return Optional.empty();
         }
      }

      return onTopOfChunkCenter(p_228964_, Heightmap.Types.OCEAN_FLOOR_WG, p_228967_ -> generatePieces(p_228967_, p_228964_));
   }

   private static StructurePiece createTopPiece(ChunkPos p_228961_, WorldgenRandom p_228962_) {
      int i = p_228961_.getMinBlockX() - 29;
      int j = p_228961_.getMinBlockZ() - 29;
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(p_228962_);
      return new OceanMonumentPieces.MonumentBuilding(p_228962_, i, j, direction);
   }

   private static void generatePieces(StructurePiecesBuilder p_228969_, Structure.GenerationContext p_228970_) {
      p_228969_.addPiece(createTopPiece(p_228970_.chunkPos(), p_228970_.random()));
   }

   public static PiecesContainer regeneratePiecesAfterLoad(ChunkPos p_228957_, long p_228958_, PiecesContainer p_228959_) {
      if (p_228959_.isEmpty()) {
         return p_228959_;
      }

      WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      worldgenrandom.setLargeFeatureSeed(p_228958_, p_228957_.x, p_228957_.z);
      StructurePiece structurepiece = p_228959_.pieces().get(0);
      BoundingBox boundingbox = structurepiece.getBoundingBox();
      int i = boundingbox.minX();
      int j = boundingbox.minZ();
      Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(worldgenrandom);
      Direction direction1 = Objects.requireNonNullElse(structurepiece.getOrientation(), direction);
      StructurePiece structurepiece1 = new OceanMonumentPieces.MonumentBuilding(worldgenrandom, i, j, direction1);
      StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
      structurepiecesbuilder.addPiece(structurepiece1);
      return structurepiecesbuilder.build();
   }

   @Override
   public StructureType<?> type() {
      return StructureType.OCEAN_MONUMENT;
   }
}
