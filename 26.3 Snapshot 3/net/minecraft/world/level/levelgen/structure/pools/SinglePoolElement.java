package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class SinglePoolElement extends StructurePoolElement {
   private static final Comparator<StructureTemplate.JigsawBlockInfo> HIGHEST_SELECTION_PRIORITY_FIRST = Comparator.comparingInt(
         StructureTemplate.JigsawBlockInfo::selectionPriority
      )
      .reversed();
   private static final Codec<Either<Identifier, StructureTemplate>> TEMPLATE_CODEC = Codec.of(
      SinglePoolElement::encodeTemplate, Identifier.CODEC.map(Either::left)
   );
   public static final MapCodec<SinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec()).apply(i, SinglePoolElement::new)
   );
   protected final Either<Identifier, StructureTemplate> template;
   protected final Holder<StructureProcessorList> processors;
   protected final Optional<LiquidSettings> overrideLiquidSettings;

   private static <T> DataResult<T> encodeTemplate(final Either<Identifier, StructureTemplate> template, final DynamicOps<T> ops, final T prefix) {
      Optional<Identifier> location = template.left();
      return location.isEmpty() ? DataResult.error(() -> "Can not serialize a runtime pool element") : Identifier.CODEC.encode(location.get(), ops, prefix);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Holder<StructureProcessorList>> processorsCodec() {
      return StructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter(t -> t.processors);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Optional<LiquidSettings>> overrideLiquidSettingsCodec() {
      return LiquidSettings.CODEC.optionalFieldOf("override_liquid_settings").forGetter(t -> t.overrideLiquidSettings);
   }

   protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<Identifier, StructureTemplate>> templateCodec() {
      return TEMPLATE_CODEC.fieldOf("location").forGetter(t -> t.template);
   }

   protected SinglePoolElement(
      final Either<Identifier, StructureTemplate> template,
      final Holder<StructureProcessorList> processors,
      final StructureTemplatePool.Projection projection,
      final Optional<LiquidSettings> overrideLiquidSettings
   ) {
      super(projection);
      this.template = template;
      this.processors = processors;
      this.overrideLiquidSettings = overrideLiquidSettings;
   }

   @Override
   public Vec3i getSize(final StructureTemplateManager structureTemplateManager, final Rotation rotation) {
      StructureTemplate template = this.getTemplate(structureTemplateManager);
      return template.getSize(rotation);
   }

   private StructureTemplate getTemplate(final StructureTemplateManager structureTemplateManager) {
      return (StructureTemplate)this.template.map(structureTemplateManager::getOrCreate, Function.identity());
   }

   public List<StructureTemplate.StructureBlockInfo> getDataMarkers(
      final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation, final boolean absolute
   ) {
      StructureTemplate template = this.getTemplate(structureTemplateManager);
      List<StructureTemplate.StructureBlockInfo> structureBlocks = template.filterBlocks(
         position, new StructurePlaceSettings().setRotation(rotation), Blocks.STRUCTURE_BLOCK, absolute
      );
      List<StructureTemplate.StructureBlockInfo> dataMarkers = Lists.newArrayList();

      for (StructureTemplate.StructureBlockInfo info : structureBlocks) {
         CompoundTag nbt = info.nbt();
         if (nbt != null) {
            StructureMode mode = nbt.<StructureMode>read("mode", StructureMode.LEGACY_CODEC).orElseThrow();
            if (mode == StructureMode.DATA) {
               dataMarkers.add(info);
            }
         }
      }

      return dataMarkers;
   }

   @Override
   public List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(
      final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation, final RandomSource random
   ) {
      List<StructureTemplate.JigsawBlockInfo> jigsaws = this.getTemplate(structureTemplateManager).getJigsaws(position, rotation);
      Util.shuffle(jigsaws, random);
      sortBySelectionPriority(jigsaws);
      return jigsaws;
   }

   @VisibleForTesting
   static void sortBySelectionPriority(final List<StructureTemplate.JigsawBlockInfo> blocks) {
      blocks.sort(HIGHEST_SELECTION_PRIORITY_FIRST);
   }

   @Override
   public BoundingBox getBoundingBox(final StructureTemplateManager structureTemplateManager, final BlockPos position, final Rotation rotation) {
      StructureTemplate template = this.getTemplate(structureTemplateManager);
      return template.getBoundingBox(new StructurePlaceSettings().setRotation(rotation), position);
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
      StructureTemplate template = this.getTemplate(structureTemplateManager);
      StructurePlaceSettings settings = this.getSettings(rotation, chunkBB, liquidSettings, keepJigsaws);
      if (!template.placeInWorld(level, position, referencePos, settings, random, 18)) {
         return false;
      }

      for (StructureTemplate.StructureBlockInfo dataMarker : StructureTemplate.processBlockInfos(
         level, position, referencePos, settings, this.getDataMarkers(structureTemplateManager, position, rotation, false)
      )) {
         this.handleDataMarker(level, dataMarker, position, rotation, random, chunkBB);
      }

      return true;
   }

   protected StructurePlaceSettings getSettings(
      final Rotation rotation, final BoundingBox chunkBB, final LiquidSettings liquidSettings, final boolean keepJigsaws
   ) {
      StructurePlaceSettings settings = new StructurePlaceSettings();
      settings.setBoundingBox(chunkBB);
      settings.setRotation(rotation);
      settings.setKnownShape(true);
      settings.setIgnoreEntities(false);
      settings.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      settings.setFinalizeEntities(true);
      settings.setLiquidSettings(this.overrideLiquidSettings.orElse(liquidSettings));
      if (!keepJigsaws) {
         settings.addProcessor(JigsawReplacementProcessor.INSTANCE);
      }

      this.processors.value().list().forEach(settings::addProcessor);
      this.getProjection().getProcessors().forEach(settings::addProcessor);
      return settings;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.SINGLE;
   }

   @Override
   public String toString() {
      return "Single[" + this.template + "]";
   }

   @VisibleForTesting
   public Identifier getTemplateLocation() {
      return (Identifier)this.template.orThrow();
   }
}
