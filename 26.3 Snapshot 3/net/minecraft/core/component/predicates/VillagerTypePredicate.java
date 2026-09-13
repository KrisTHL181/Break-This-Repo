package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.VillagerType;

public record VillagerTypePredicate(HolderSet<VillagerType> villagerTypes) implements SingleComponentItemPredicate<Holder<VillagerType>> {
   public static final Codec<VillagerTypePredicate> CODEC = RegistryCodecs.homogeneousList(Registries.VILLAGER_TYPE)
      .xmap(VillagerTypePredicate::new, VillagerTypePredicate::villagerTypes);

   @Override
   public DataComponentType<Holder<VillagerType>> componentType() {
      return DataComponents.VILLAGER_VARIANT;
   }

   public boolean matches(final Holder<VillagerType> villagerType) {
      return this.villagerTypes.contains(villagerType);
   }

   public static VillagerTypePredicate villagerTypes(final HolderSet<VillagerType> villagerTypes) {
      return new VillagerTypePredicate(villagerTypes);
   }
}
