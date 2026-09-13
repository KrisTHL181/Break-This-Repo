package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class SwampHutStructure extends Structure {
   public static final MapCodec<SwampHutStructure> CODEC = simpleCodec(SwampHutStructure::new);

   public SwampHutStructure(final Structure.StructureSettings settings) {
      super(settings);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> generatePieces(builder, context));
   }

   private static void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      builder.addPiece(new SwampHutPiece(context.random(), context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()));
   }

   @Override
   public StructureType<?> type() {
      return StructureType.SWAMP_HUT;
   }
}
