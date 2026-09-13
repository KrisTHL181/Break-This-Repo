package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class ShipwreckStructure extends Structure {
   public static final MapCodec<ShipwreckStructure> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(settingsCodec(i), Codec.BOOL.fieldOf("is_beached").forGetter(s -> s.isBeached)).apply(i, ShipwreckStructure::new)
   );
   public final boolean isBeached;

   public ShipwreckStructure(final Structure.StructureSettings settings, final boolean isBeached) {
      super(settings);
      this.isBeached = isBeached;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(final Structure.GenerationContext context) {
      Heightmap.Types type = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
      return onTopOfChunkCenter(context, type, builder -> this.generatePieces(builder, context));
   }

   private void generatePieces(final StructurePiecesBuilder builder, final Structure.GenerationContext context) {
      Rotation rotation = Rotation.getRandom(context.random());
      BlockPos offset = new BlockPos(context.chunkPos().getMinBlockX(), 90, context.chunkPos().getMinBlockZ());
      ShipwreckPieces.ShipwreckPiece piece = ShipwreckPieces.addRandomPiece(
         context.structureTemplateManager(), offset, rotation, builder, context.random(), this.isBeached
      );
      if (piece.isTooBigToFitInWorldGenRegion()) {
         BoundingBox bb = piece.getBoundingBox();
         int height;
         if (this.isBeached) {
            int minY = Structure.getLowestY(context, bb.minX(), bb.getXSpan(), bb.minZ(), bb.getZSpan());
            height = piece.calculateBeachedPosition(minY, context.random());
         } else {
            height = Structure.getMeanFirstOccupiedHeight(context, bb.minX(), bb.getXSpan(), bb.minZ(), bb.getZSpan());
         }

         piece.adjustPositionHeight(height);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.SHIPWRECK;
   }
}
