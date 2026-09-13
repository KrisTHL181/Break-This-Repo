package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class FeaturePoolElement extends StructurePoolElement {
   public static final MapCodec<FeaturePoolElement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(e -> e.feature), projectionCodec()).apply(i, FeaturePoolElement::new)
   );
   private static final Identifier DEFAULT_JIGSAW_NAME = Identifier.withDefaultNamespace("bottom");
   private final Holder<PlacedFeature> feature;
   private final CompoundTag defaultJigsawNBT;

   protected FeaturePoolElement(final Holder<PlacedFeature> feature, final StructureTemplatePool.Projection projection) {
      super(projection);
      this.feature = feature;
      this.defaultJigsawNBT = this.fillDefaultJigsawNBT();
   }

   private CompoundTag fillDefaultJigsawNBT() {
      CompoundTag tag = new CompoundTag();
      tag.store("name", Identifier.CODEC, DEFAULT_JIGSAW_NAME);
      tag.putString("final_state", "minecraft:air");
      tag.store("pool", JigsawBlockEntity.POOL_CODEC, Pools.EMPTY);
      tag.store("target", Identifier.CODEC, JigsawBlockEntity.EMPTY_ID);
      tag.store("joint", JigsawBlockEntity.JointType.CODEC, JigsawBlockEntity.JointType.ROLLABLE);
      return tag;
   }

   @Override
   public Vec3i getSize(final StructureTemplateManager structureTemplateManager, final Rotation rotation) {
      return Vec3i.ZERO;
   }

   @Override
   public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation, final RandomSource random
   ) {
      return List.of(
         StructureTemplate.JigsawBlockInfo.of(
            new StructureTemplate.StructureBlockInfo(
               position,
               Blocks.JIGSAW.defaultBlockState().setValue(JigsawBlock.ORIENTATION, FrontAndTop.fromFrontAndTop(Direction.DOWN, Direction.SOUTH)),
               this.defaultJigsawNBT
            )
         )
      );
   }

   @Override
   public BoundingBox getBoundingBox(final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation) {
      Vec3i size = this.getSize(structureTemplateManager, rotation);
      return new BoundingBox(
         position.getX(), position.getY(), position.getZ(), position.getX() + size.getX(), position.getY() + size.getY(), position.getZ() + size.getZ()
      );
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
      return this.feature.value().place(level, generator, random, position);
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.FEATURE;
   }

   @Override
   public String toString() {
      return "Feature[" + this.feature + "]";
   }
}
