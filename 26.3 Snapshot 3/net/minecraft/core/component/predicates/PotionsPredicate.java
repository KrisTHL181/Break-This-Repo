package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.MobEffectsPredicate;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public record PotionsPredicate(Optional<HolderSet<Potion>> potions, Optional<CollectionPredicate<MobEffectInstance, MobEffectsPredicate>> effects)
   implements SingleComponentItemPredicate<PotionContents> {
   public static final Codec<PotionsPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.POTION).optionalFieldOf("potions").forGetter(PotionsPredicate::potions),
            CollectionPredicate.codec(MobEffectsPredicate.CODEC).optionalFieldOf("effects").forGetter(PotionsPredicate::effects)
         )
         .apply(i, PotionsPredicate::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, PotionsPredicate> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.POTION)),
      PotionsPredicate::potions,
      ByteBufCodecs.optional(ByteBufCodecs.fromCodecTrusted(CollectionPredicate.codec(MobEffectsPredicate.CODEC))),
      PotionsPredicate::effects,
      PotionsPredicate::new
   );

   @Override
   public DataComponentType<PotionContents> componentType() {
      return DataComponents.POTION_CONTENTS;
   }

   public boolean matches(final PotionContents potionContents) {
      Optional<Holder<Potion>> potion = potionContents.potion();
      if (!this.potions.isPresent() || !potion.isEmpty() && this.potions.get().contains(potion.get())) {
         return this.effects.isPresent() ? this.effects.get().test(potionContents.getAllEffects()) : true;
      } else {
         return false;
      }
   }

   public static PotionsPredicate ofPotions(final HolderSet<Potion> potions) {
      return new PotionsPredicate(Optional.of(potions), Optional.empty());
   }

   public static PotionsPredicate ofPotion(final Holder<Potion> potion) {
      return ofPotions(HolderSet.direct(potion));
   }
}
