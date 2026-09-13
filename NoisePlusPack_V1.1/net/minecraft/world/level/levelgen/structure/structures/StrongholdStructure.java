package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class StrongholdStructure extends Structure {
   public static final MapCodec<StrongholdStructure> CODEC = simpleCodec(StrongholdStructure::new);

   public StrongholdStructure(Structure.StructureSettings p_229939_) {
      super(p_229939_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229941_) {
      return Optional.of(new Structure.GenerationStub(p_229941_.chunkPos().getWorldPosition(), p_229944_ -> generatePieces(p_229944_, p_229941_)));
   }

   private static void generatePieces(StructurePiecesBuilder p_229946_, Structure.GenerationContext p_229947_) {
      int i = 0;

      StrongholdPieces.StartPiece strongholdpieces$startpiece;
      do {
         p_229946_.clear();
         p_229947_.random().setLargeFeatureSeed(p_229947_.seed() + i++, p_229947_.chunkPos().x, p_229947_.chunkPos().z);
         StrongholdPieces.resetPieces();
         strongholdpieces$startpiece = new StrongholdPieces.StartPiece(p_229947_.random(), p_229947_.chunkPos().getBlockX(2), p_229947_.chunkPos().getBlockZ(2));
         p_229946_.addPiece(strongholdpieces$startpiece);
         strongholdpieces$startpiece.addChildren(strongholdpieces$startpiece, p_229946_, p_229947_.random());
         List<StructurePiece> list = strongholdpieces$startpiece.pendingChildren;

         while (!list.isEmpty()) {
            int j = p_229947_.random().nextInt(list.size());
            StructurePiece structurepiece = list.remove(j);
            structurepiece.addChildren(strongholdpieces$startpiece, p_229946_, p_229947_.random());
         }

         p_229946_.moveBelowSeaLevel(p_229947_.chunkGenerator().getSeaLevel(), p_229947_.chunkGenerator().getMinY(), p_229947_.random(), 10);
      } while (p_229946_.isEmpty() || strongholdpieces$startpiece.portalRoomPiece == null);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.STRONGHOLD;
   }
}
