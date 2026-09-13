package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.ItemContainerContents;

public record ContainerPredicate(Optional<CollectionPredicate<ItemInstance, ItemPredicate>> items)
   implements SingleComponentItemPredicate<ItemContainerContents> {
   public static final Codec<ContainerPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(CollectionPredicate.codec(ItemPredicate.CODEC).optionalFieldOf("items").forGetter(ContainerPredicate::items))
         .apply(i, ContainerPredicate::new)
   );

   @Override
   public DataComponentType<ItemContainerContents> componentType() {
      return DataComponents.CONTAINER;
   }

   public boolean matches(final ItemContainerContents value) {
      return !this.items.isPresent() || this.items.get().test(value.nonEmptyItems());
   }
}
