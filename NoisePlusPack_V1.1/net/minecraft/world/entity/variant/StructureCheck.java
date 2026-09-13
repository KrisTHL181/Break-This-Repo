package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;

public record StructureCheck(HolderSet<Structure> requiredStructures) implements SpawnCondition {
   public static final MapCodec<StructureCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_396325_ -> p_396325_.group(RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(StructureCheck::requiredStructures))
         .apply(p_396325_, StructureCheck::new)
   );

   public boolean test(SpawnContext p_397969_) {
      return p_397969_.level().getLevel().structureManager().getStructureWithPieceAt(p_397969_.pos(), this.requiredStructures).isValid();
   }

   @Override
   public MapCodec<StructureCheck> codec() {
      return MAP_CODEC;
   }
}
