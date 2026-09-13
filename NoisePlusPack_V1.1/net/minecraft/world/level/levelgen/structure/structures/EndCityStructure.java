package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class EndCityStructure extends Structure {
   public static final MapCodec<EndCityStructure> CODEC = simpleCodec(EndCityStructure::new);

   public EndCityStructure(Structure.StructureSettings p_227526_) {
      super(p_227526_);
   }

   @Override
   public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_227528_) {
      Rotation rotation = Rotation.getRandom(p_227528_.random());
      BlockPos blockpos = this.getLowestYIn5by5BoxOffset7Blocks(p_227528_, rotation);
      return blockpos.getY() < 60
         ? Optional.empty()
         : Optional.of(new Structure.GenerationStub(blockpos, p_227538_ -> this.generatePieces(p_227538_, blockpos, rotation, p_227528_)));
   }

   private void generatePieces(StructurePiecesBuilder p_227530_, BlockPos p_227531_, Rotation p_227532_, Structure.GenerationContext p_227533_) {
      List<StructurePiece> list = Lists.newArrayList();
      EndCityPieces.startHouseTower(p_227533_.structureTemplateManager(), p_227531_, p_227532_, list, p_227533_.random());
      list.forEach(p_227530_::addPiece);
   }

   @Override
   public StructureType<?> type() {
      return StructureType.END_CITY;
   }
}
