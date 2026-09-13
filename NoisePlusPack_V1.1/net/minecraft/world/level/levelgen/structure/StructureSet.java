package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public record StructureSet(List<StructureSet.StructureSelectionEntry> structures, StructurePlacement placement) {
   public static final Codec<StructureSet> DIRECT_CODEC = RecordCodecBuilder.create(
      p_210014_ -> p_210014_.group(
            StructureSet.StructureSelectionEntry.CODEC.listOf().fieldOf("structures").forGetter(StructureSet::structures),
            StructurePlacement.CODEC.fieldOf("placement").forGetter(StructureSet::placement)
         )
         .apply(p_210014_, StructureSet::new)
   );
   public static final Codec<Holder<StructureSet>> CODEC = RegistryFileCodec.create(Registries.STRUCTURE_SET, DIRECT_CODEC);

   public StructureSet(Holder<Structure> p_210007_, StructurePlacement p_210008_) {
      this(List.of(new StructureSet.StructureSelectionEntry(p_210007_, 1)), p_210008_);
   }

   public static StructureSet.StructureSelectionEntry entry(Holder<Structure> p_210018_, int p_210019_) {
      return new StructureSet.StructureSelectionEntry(p_210018_, p_210019_);
   }

   public static StructureSet.StructureSelectionEntry entry(Holder<Structure> p_210016_) {
      return new StructureSet.StructureSelectionEntry(p_210016_, 1);
   }

   public record StructureSelectionEntry(Holder<Structure> structure, int weight) {
      public static final Codec<StructureSet.StructureSelectionEntry> CODEC = RecordCodecBuilder.create(
         p_210034_ -> p_210034_.group(
               Structure.CODEC.fieldOf("structure").forGetter(StructureSet.StructureSelectionEntry::structure),
               ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(StructureSet.StructureSelectionEntry::weight)
            )
            .apply(p_210034_, StructureSet.StructureSelectionEntry::new)
      );
   }
}
