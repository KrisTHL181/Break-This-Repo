package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class IglooStructure extends Structure {
   public static final MapCodec<IglooStructure> CODEC = simpleCodec(IglooStructure::new);

   public IglooStructure(final Structure.StructureSettings settings) {
      super(settings);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> this.generatePieces(builder, context));
   }

   private void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      ChunkPos chunkPos = context.chunkPos();
      WorldgenRandom random = context.random();
      BlockPos startPos = new BlockPos(chunkPos.getMinBlockX(), 90, chunkPos.getMinBlockZ());
      Rotation rotation = Rotation.getRandom(random);
      IglooPieces.addPieces(context.structureTemplateManager(), startPos, rotation, builder, random);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.IGLOO;
   }
}
