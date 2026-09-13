package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public record TemplateFeature(WeightedList<TemplateFeature.TemplateEntry> templates) implements Feature {
   public static final MapCodec<TemplateFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeightedList.codec(TemplateFeature.TemplateEntry.CODEC).fieldOf("templates").forGetter(TemplateFeature::templates))
         .apply(i, TemplateFeature::new)
   );

   @Override
   public MapCodec<TemplateFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      TemplateFeature.TemplateEntry templateEntry = this.templates.getRandomOrThrow(random);
      Rotation rotation = Util.getRandom(templateEntry.rotations(), random);
      StructureTemplateManager structureTemplateManager = level.getLevel().getServer().getStructureManager();
      StructureTemplate template = structureTemplateManager.getOrCreate(templateEntry.template());
      Vec3i offsetX = this.getRotatedOffset(rotation, Direction.Axis.X, template);
      Vec3i offsetZ = this.getRotatedOffset(rotation, Direction.Axis.Z, template);
      BlockPos pos = origin.offset(offsetX).offset(offsetZ);
      StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setRandom(random);
      return template.placeInWorld(level, pos, pos, settings, random, 3);
   }

   private Vec3i getRotatedOffset(final Rotation rotation, final Direction.Axis axis, final StructureTemplate template) {
      return rotation.rotate(axis.getNegative()).getUnitVec3i().multiply(template.getSize().get(axis) / 2);
   }

   public record TemplateEntry(Identifier template, List<Rotation> rotations) {
      public static final Codec<TemplateFeature.TemplateEntry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Identifier.CODEC.fieldOf("id").forGetter(TemplateFeature.TemplateEntry::template),
               Rotation.CODEC.listOf().optionalFieldOf("rotations", List.of(Rotation.values())).forGetter(TemplateFeature.TemplateEntry::rotations)
            )
            .apply(i, TemplateFeature.TemplateEntry::new)
      );

      public static TemplateFeature.TemplateEntry of(final Identifier template) {
         return new TemplateFeature.TemplateEntry(template, List.of(Rotation.values()));
      }
   }
}
