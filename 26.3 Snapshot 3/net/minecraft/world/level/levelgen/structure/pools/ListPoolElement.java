package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

public class ListPoolElement extends StructurePoolElement {
   public static final MapCodec<ListPoolElement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(StructurePoolElement.CODEC.listOf().fieldOf("elements").forGetter(e -> e.elements), projectionCodec()).apply(i, ListPoolElement::new)
   );
   private final List<StructurePoolElement> elements;

   public ListPoolElement(final List<StructurePoolElement> elements, final StructureTemplatePool.Projection projection) {
      super(projection);
      if (elements.isEmpty()) {
         throw new IllegalArgumentException("Elements are empty");
      }

      this.elements = elements;
      this.setProjectionOnEachElement(projection);
   }

   @Override
   public Vec3i getSize(final StructureTemplateManager structureTemplateManager, final Rotation rotation) {
      int sizeX = 0;
      int sizeY = 0;
      int sizeZ = 0;

      for (StructurePoolElement element : this.elements) {
         Vec3i size = element.getSize(structureTemplateManager, rotation);
         sizeX = Math.max(sizeX, size.getX());
         sizeY = Math.max(sizeY, size.getY());
         sizeZ = Math.max(sizeZ, size.getZ());
      }

      return new Vec3i(sizeX, sizeY, sizeZ);
   }

   @Override
   public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation, final RandomSource random
   ) {
      return this.elements.get(0).getShuffledJigsawBlocks(structureTemplateManager, position, rotation, random);
   }

   @Override
   public BoundingBox getBoundingBox(final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation) {
      Stream<BoundingBox> stream = this.elements
         .stream()
         .filter(e -> e != EmptyPoolElement.INSTANCE)
         .map(e -> e.getBoundingBox(structureTemplateManager, position, rotation));
      return BoundingBox.encapsulatingBoxes(stream::iterator)
         .orElseThrow(() -> new IllegalStateException("Unable to calculate boundingbox for ListPoolElement"));
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
      for (StructurePoolElement element : this.elements) {
         if (!element.place(
            structureTemplateManager, level, structureManager, generator, position, referencePos, rotation, chunkBB, random, liquidSettings, keepJigsaws
         )) {
            return false;
         }
      }

      return true;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.LIST;
   }

   @Override
   public StructurePoolElement setProjection(final StructureTemplatePool.Projection projection) {
      super.setProjection(projection);
      this.setProjectionOnEachElement(projection);
      return this;
   }

   @Override
   public String toString() {
      return "List[" + this.elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
   }

   private void setProjectionOnEachElement(final StructureTemplatePool.Projection projection) {
      this.elements.forEach(k -> k.setProjection(projection));
   }

   @VisibleForTesting
   public List<StructurePoolElement> getElements() {
      return this.elements;
   }
}
