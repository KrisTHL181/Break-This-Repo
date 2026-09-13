package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

public abstract class StructurePoolElement {
   public static final Codec<StructurePoolElement> CODEC = BuiltInRegistries.STRUCTURE_POOL_ELEMENT
      .byNameCodec()
      .dispatch("element_type", StructurePoolElement::getType, StructurePoolElementType::codec);
   private static final Holder<StructureProcessorList> EMPTY = Holder.direct(new StructureProcessorList(List.of()));
   private volatile StructureTemplatePool.@Nullable Projection projection;

   protected static <E extends StructurePoolElement> RecordCodecBuilder<E, StructureTemplatePool.Projection> projectionCodec() {
      return StructureTemplatePool.Projection.CODEC.fieldOf("projection").forGetter(StructurePoolElement::getProjection);
   }

   protected StructurePoolElement(final StructureTemplatePool.Projection projection) {
      this.projection = projection;
   }

   public abstract Vec3i getSize(StructureTemplateManager structureTemplateManager, Rotation rotation);

   public abstract List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager structureTemplateManager, BlockPos position, Rotation rotation, RandomSource random
   );

   public abstract BoundingBox getBoundingBox(StructureTemplateManager structureTemplateManager, BlockPos position, Rotation rotation);

   public abstract boolean place(
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
   );

   public abstract StructurePoolElementType<?> getType();

   public void handleDataMarker(
      final LevelAccessor level,
      final StructureTemplate.StructureBlockInfo dataMarker,
      final BlockPos position,
      final Rotation rotation,
      final RandomSource random,
      final BoundingBox chunkBB
   ) {
   }

   public StructurePoolElement setProjection(final StructureTemplatePool.Projection projection) {
      this.projection = projection;
      return this;
   }

   public StructureTemplatePool.Projection getProjection() {
      StructureTemplatePool.Projection projection = this.projection;
      if (projection == null) {
         throw new IllegalStateException();
      } else {
         return projection;
      }
   }

   public int getGroundLevelDelta() {
      return 1;
   }

   public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
      return p -> EmptyPoolElement.INSTANCE;
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(final String location) {
      return p -> new LegacySinglePoolElement(Either.left(Identifier.parse(location)), EMPTY, p, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(
      final String location, final Holder<StructureProcessorList> processors
   ) {
      return p -> new LegacySinglePoolElement(Either.left(Identifier.parse(location)), processors, p, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(final String location) {
      return p -> new SinglePoolElement(Either.left(Identifier.parse(location)), EMPTY, p, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(final String location, final Holder<StructureProcessorList> processors) {
      return p -> new SinglePoolElement(Either.left(Identifier.parse(location)), processors, p, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(final String location, final LiquidSettings overrideLiquidSettings) {
      return p -> new SinglePoolElement(Either.left(Identifier.parse(location)), EMPTY, p, Optional.of(overrideLiquidSettings));
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(
      final String location, final Holder<StructureProcessorList> processors, final LiquidSettings overrideLiquidSettings
   ) {
      return p -> new SinglePoolElement(Either.left(Identifier.parse(location)), processors, p, Optional.of(overrideLiquidSettings));
   }

   public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(final Holder<PlacedFeature> feature) {
      return p -> new FeaturePoolElement(feature, p);
   }

   public static Function<StructureTemplatePool.Projection, ListPoolElement> list(
      final List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> elements
   ) {
      return p -> new ListPoolElement(elements.stream().map(e -> e.apply(p)).collect(Collectors.toList()), p);
   }
}
