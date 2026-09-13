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
      p_229401_ -> p_229401_.group(settingsCodec(p_229401_), Codec.BOOL.fieldOf("is_beached").forGetter(p_229399_ -> p_229399_.isBeached))
         .apply(p_229401_, ShipwreckStructure::new)
   );
   public final boolean isBeached;

   public ShipwreckStructure(Structure.StructureSettings p_229388_, boolean p_229389_) {
      super(p_229388_);
      this.isBeached = p_229389_;
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229391_) {
      Heightmap.Types heightmap$types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
      return onTopOfChunkCenter(p_229391_, heightmap$types, p_229394_ -> this.generatePieces(p_229394_, p_229391_));
   }

   private void generatePieces(StructurePiecesBuilder p_229396_, Structure.GenerationContext p_229397_) {
      Rotation rotation = Rotation.getRandom(p_229397_.random());
      BlockPos blockpos = new BlockPos(p_229397_.chunkPos().getMinBlockX(), 90, p_229397_.chunkPos().getMinBlockZ());
      ShipwreckPieces.ShipwreckPiece shipwreckpieces$shipwreckpiece = ShipwreckPieces.addRandomPiece(
         p_229397_.structureTemplateManager(), blockpos, rotation, p_229396_, p_229397_.random(), this.isBeached
      );
      if (shipwreckpieces$shipwreckpiece.isTooBigToFitInWorldGenRegion()) {
         BoundingBox boundingbox = shipwreckpieces$shipwreckpiece.getBoundingBox();
         int i;
         if (this.isBeached) {
            int j = Structure.getLowestY(p_229397_, boundingbox.minX(), boundingbox.getXSpan(), boundingbox.minZ(), boundingbox.getZSpan());
            i = shipwreckpieces$shipwreckpiece.calculateBeachedPosition(j, p_229397_.random());
         } else {
            i = Structure.getMeanFirstOccupiedHeight(p_229397_, boundingbox.minX(), boundingbox.getXSpan(), boundingbox.minZ(), boundingbox.getZSpan());
         }

         shipwreckpieces$shipwreckpiece.adjustPositionHeight(i);
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.SHIPWRECK;
   }
}
