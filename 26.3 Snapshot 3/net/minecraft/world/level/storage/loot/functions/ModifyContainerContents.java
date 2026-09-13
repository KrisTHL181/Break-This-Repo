package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulators;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ModifyContainerContents extends LootItemConditionalFunction {
   public static final MapCodec<ModifyContainerContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               ContainerComponentManipulators.CODEC.fieldOf("component").forGetter(f -> f.component),
               LootItemFunctions.ROOT_CODEC.fieldOf("modifier").forGetter(f -> f.modifier)
            )
         )
         .apply(i, ModifyContainerContents::new)
   );
   private final ContainerComponentManipulator<?> component;
   private final LootItemFunction modifier;

   private ModifyContainerContents(final List<LootItemCondition> predicates, final ContainerComponentManipulator<?> component, final LootItemFunction modifier) {
      super(predicates);
      this.component = component;
      this.modifier = modifier;
   }

   @Override
   public MapCodec<ModifyContainerContents> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (itemStack.isEmpty()) {
         return itemStack;
      }

      this.component.modifyItems(itemStack, c -> this.modifier.apply(c, context));
      return itemStack;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "modifier", this.modifier);
   }
}
