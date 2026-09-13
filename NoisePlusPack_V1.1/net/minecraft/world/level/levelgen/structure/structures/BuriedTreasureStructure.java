package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class BuriedTreasureStructure extends Structure {
   public static final MapCodec<BuriedTreasureStructure> CODEC = simpleCodec(BuriedTreasureStructure::new);

   public BuriedTreasureStructure(Structure.StructureSettings p_227385_) {
      super(p_227385_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_227387_) {
      return onTopOfChunkCenter(p_227387_, Heightmap.Types.OCEAN_FLOOR_WG, p_227390_ -> generatePieces(p_227390_, p_227387_));
   }

   private static void generatePieces(StructurePiecesBuilder p_227392_, Structure.GenerationContext p_227393_) {
      BlockPos blockpos = new BlockPos(p_227393_.chunkPos().getBlockX(9), 90, p_227393_.chunkPos().getBlockZ(9));
      p_227392_.addPiece(new BuriedTreasurePieces.BuriedTreasurePiece(blockpos));
   }

   @Override
   public StructureType<?> type() {
      return StructureType.BURIED_TREASURE;
   }
}
