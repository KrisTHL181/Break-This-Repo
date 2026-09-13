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
   public Vec3i getSize(final StructureTemplateManager structureTemplateManager, final Rotation rotation) {
      return Vec3i.ZERO;
   }

   @Override
   public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation, final RandomSource random
   ) {
      return Collections.emptyList();
   }

   @Override
   public BoundingBox getBoundingBox(final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation) {
      throw new IllegalStateException("Invalid call to EmptyPoolElement.getBoundingBox, filter me!");
   }

   @Override
   public boolean place(
      final StructureTemplateManager structureTemplateManager,
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final BlockPos position,
      final BlockPos referencePos,
      final Rotation rotation,
      final BoundingBox chunkBB,
      final RandomSource random,
      final LiquidSettings liquidSettings,
      final boolean keepJigsaws
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
