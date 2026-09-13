package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public record TrimPredicate(Optional<HolderSet<TrimMaterial>> material, Optional<HolderSet<TrimPattern>> pattern)
   implements SingleComponentItemPredicate<ArmorTrim> {
   public static final Codec<TrimPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.TRIM_MATERIAL).optionalFieldOf("material").forGetter(TrimPredicate::material),
            RegistryCodecs.homogeneousList(Registries.TRIM_PATTERN).optionalFieldOf("pattern").forGetter(TrimPredicate::pattern)
         )
         .apply(i, TrimPredicate::new)
   );

   @Override
   public DataComponentType<ArmorTrim> componentType() {
      return DataComponents.TRIM;
   }

   public boolean matches(final ArmorTrim value) {
      return this.material.isPresent() && !this.material.get().contains(value.material())
         ? false
         : !this.pattern.isPresent() || this.pattern.get().contains(value.pattern());
   }
}
