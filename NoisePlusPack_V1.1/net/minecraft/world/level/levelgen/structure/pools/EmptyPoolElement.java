package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class EmptyPoolElement extends StructurePoolElement {
   public static final MapCodec<EmptyPoolElement> CODEC = MapCodec.unit(() -> EmptyPoolElement.INSTANCE);
   public static final EmptyPoolElement INSTANCE = new EmptyPoolElement();

   private EmptyPoolElement() {
      super(StructureTemplatePool.Projection.TERRAIN_MATCHING);
   }

   @Override
   public Vec3i getSize(StructureTemplateManager p_227169_, Rotation p_227170_) {
      return Vec3i.ZERO;
   }

   @Override
   public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager p_227176_, BlockPos p_227177_, Rotation p_227178_, RandomSource p_227179_
   ) {
      return Collections.emptyList();
   }

   @Override
   public BoundingBox getBoundingBox(StructureTemplateManager p_227172_, BlockPos p_227173_, Rotation p_227174_) {
      throw new IllegalStateException("Invalid call to EmptyPoolElement.getBoundingBox, filter me!");
   }

   @Override
   public boolean place(
      StructureTemplateManager p_227158_,
      WorldGenLevel p_227159_,
      StructureManager p_227160_,
      ChunkGenerator p_227161_,
      BlockPos p_227162_,
      BlockPos p_227163_,
      Rotation p_227164_,
      BoundingBox p_227165_,
      RandomSource p_227166_,
      LiquidSettings p_345294_,
      boolean p_227167_
   ) {
      return true;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.EMPTY;
   }

   @Override
   public String toString() {
      return "Empty";
   }
}
