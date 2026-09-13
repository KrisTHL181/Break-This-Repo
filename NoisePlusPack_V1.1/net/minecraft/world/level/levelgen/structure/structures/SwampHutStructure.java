package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class SwampHutStructure extends Structure {
   public static final MapCodec<SwampHutStructure> CODEC = simpleCodec(SwampHutStructure::new);

   public SwampHutStructure(Structure.StructureSettings p_229974_) {
      super(p_229974_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229976_) {
      return onTopOfChunkCenter(p_229976_, Heightmap.Types.WORLD_SURFACE_WG, p_229979_ -> generatePieces(p_229979_, p_229976_));
   }

   private static void generatePieces(StructurePiecesBuilder p_229981_, Structure.GenerationContext p_229982_) {
      p_229981_.addPiece(new SwampHutPiece(p_229982_.random(), p_229982_.chunkPos().getMinBlockX(), p_229982_.chunkPos().getMinBlockZ()));
   }

   @Override
   public StructureType<?> type() {
      return StructureType.SWAMP_HUT;
   }
}
