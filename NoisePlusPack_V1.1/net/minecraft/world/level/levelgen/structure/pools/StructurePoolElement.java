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

   protected StructurePoolElement(StructureTemplatePool.Projection p_210471_) {
      this.projection = p_210471_;
   }

   public abstract Vec3i getSize(StructureTemplateManager var1, Rotation var2);

   public abstract List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager var1, BlockPos var2, Rotation var3, RandomSource var4
   );

   public abstract BoundingBox getBoundingBox(StructureTemplateManager var1, BlockPos var2, Rotation var3);

   public abstract boolean place(
      StructureTemplateManager var1,
      WorldGenLevel var2,
      StructureManager var3,
      ChunkGenerator var4,
      BlockPos var5,
      BlockPos var6,
      Rotation var7,
      BoundingBox var8,
      RandomSource var9,
      LiquidSettings var10,
      boolean var11
   );

   public abstract StructurePoolElementType<?> getType();

   public void handleDataMarker(
      LevelAccessor p_227330_,
      StructureTemplate.StructureBlockInfo p_227331_,
      BlockPos p_227332_,
      Rotation p_227333_,
      RandomSource p_227334_,
      BoundingBox p_227335_
   ) {
   }

   public StructurePoolElement setProjection(StructureTemplatePool.Projection p_210479_) {
      this.projection = p_210479_;
      return this;
   }

   public StructureTemplatePool.Projection getProjection() {
      StructureTemplatePool.Projection structuretemplatepool$projection = this.projection;
      if (structuretemplatepool$projection == null) {
         throw new IllegalStateException();
      } else {
         return structuretemplatepool$projection;
      }
   }

   public int getGroundLevelDelta() {
      return 1;
   }

   public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
      return p_210525_ -> EmptyPoolElement.INSTANCE;
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String p_210508_) {
      return p_450032_ -> new LegacySinglePoolElement(Either.left(Identifier.parse(p_210508_)), EMPTY, p_450032_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String p_210513_, Holder<StructureProcessorList> p_210514_) {
      return p_450030_ -> new LegacySinglePoolElement(Either.left(Identifier.parse(p_210513_)), p_210514_, p_450030_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_210527_) {
      return p_450027_ -> new SinglePoolElement(Either.left(Identifier.parse(p_210527_)), EMPTY, p_450027_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_210532_, Holder<StructureProcessorList> p_210533_) {
      return p_450035_ -> new SinglePoolElement(Either.left(Identifier.parse(p_210532_)), p_210533_, p_450035_, Optional.empty());
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String p_342465_, LiquidSettings p_344698_) {
      return p_450038_ -> new SinglePoolElement(Either.left(Identifier.parse(p_342465_)), EMPTY, p_450038_, Optional.of(p_344698_));
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(
      String p_343039_, Holder<StructureProcessorList> p_343135_, LiquidSettings p_342378_
   ) {
      return p_450025_ -> new SinglePoolElement(Either.left(Identifier.parse(p_343039_)), p_343135_, p_450025_, Optional.of(p_342378_));
   }

   public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(Holder<PlacedFeature> p_210503_) {
      return p_210506_ -> new FeaturePoolElement(p_210503_, p_210506_);
   }

   public static Function<StructureTemplatePool.Projection, ListPoolElement> list(
      List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> p_210520_
   ) {
      return p_210523_ -> new ListPoolElement(p_210520_.stream().map(p_210482_ -> p_210482_.apply(p_210523_)).collect(Collectors.toList()), p_210523_);
   }
}
