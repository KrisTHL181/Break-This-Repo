package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public record PotionsPredicate(HolderSet<Potion> potions) implements SingleComponentItemPredicate<PotionContents> {
   public static final Codec<PotionsPredicate> CODEC = RegistryCodecs.homogeneousList(Registries.POTION).xmap(PotionsPredicate::new, PotionsPredicate::potions);

   @Override
   public DataComponentType<PotionContents> componentType() {
      return DataComponents.POTION_CONTENTS;
   }

   public boolean matches(PotionContents p_391208_) {
      Optional<Holder<Potion>> optional = p_391208_.potion();
      return !optional.isEmpty() && this.potions.contains(optional.get());
   }

   public static DataComponentPredicate potions(HolderSet<Potion> p_394576_) {
      return new PotionsPredicate(p_394576_);
   }
}
