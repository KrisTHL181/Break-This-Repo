package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class LegacySinglePoolElement extends SinglePoolElement {
   public static final MapCodec<LegacySinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(templateCodec(), processorsCodec(), projectionCodec(), overrideLiquidSettingsCodec()).apply(i, LegacySinglePoolElement::new)
   );

   protected LegacySinglePoolElement(
      final Either<Identifier, StructureTemplate> template,
      final Holder<StructureProcessorList> processors,
      final StructureTemplatePool.Projection projection,
      final Optional<LiquidSettings> liquidSettings
   ) {
      super(template, processors, projection, liquidSettings);
   }

   @Override
   protected StructurePlaceSettings getSettings(
      final Rotation rotation, final BoundingBox chunkBB, final LiquidSettings liquidSettings, final boolean keepJigsaws
   ) {
      StructurePlaceSettings settings = super.getSettings(rotation, chunkBB, liquidSettings, keepJigsaws);
      settings.popProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      settings.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      return settings;
   }

   @Override
   public StructurePoolElementType<?> getType() {
      return StructurePoolElementType.LEGACY;
   }

   @Override
   public String toString() {
      return "LegacySingle[" + this.template + "]";
   }
}
