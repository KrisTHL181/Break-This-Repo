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

   public StrongholdStructure(final Structure.StructureSettings settings) {
      super(settings);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      return Optional.of(new Structure.GenerationStub(context.chunkPos().getWorldPosition(), builder -> generatePieces(builder, context)));
   }

   private static void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      int tries = 0;

      StrongholdPieces.StartPiece startRoom;
      do {
         builder.clear();
         context.random().setLargeFeatureSeed(context.seed() + tries++, context.chunkPos().x(), context.chunkPos().z());
         StrongholdPieces.resetPieces();
         startRoom = new StrongholdPieces.StartPiece(context.random(), context.chunkPos().getBlockX(2), context.chunkPos().getBlockZ(2));
         builder.addPiece(startRoom);
         startRoom.addChildren(startRoom, builder, context.random());
         List<StructurePiece> pendingChildren = startRoom.pendingChildren;

         while (!pendingChildren.isEmpty()) {
            int pos = context.random().nextInt(pendingChildren.size());
            StructurePiece structurePiece = pendingChildren.remove(pos);
            structurePiece.addChildren(startRoom, builder, context.random());
         }

         builder.moveBelowSeaLevel(context.chunkGenerator().getSeaLevel(), context.chunkGenerator().getMinY(), context.random(), 10);
      } while (builder.isEmpty() || startRoom.portalRoomPiece == null);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.STRONGHOLD;
   }
}
