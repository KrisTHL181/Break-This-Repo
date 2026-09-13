package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetContainerLootTable extends LootItemConditionalFunction {
   public static final MapCodec<SetContainerLootTable> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(
            i.group(
               LootTable.KEY_CODEC.fieldOf("name").forGetter(f -> f.name),
               Codec.LONG.optionalFieldOf("seed", 0L).forGetter(f -> f.seed),
               BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(f -> f.type)
            )
         )
         .apply(i, SetContainerLootTable::new)
   );
   private final ResourceKey<LootTable> name;
   private final long seed;
   private final Holder<BlockEntityType<?>> type;

   private SetContainerLootTable(
      final List<LootItemCondition> predicates, final ResourceKey<LootTable> name, final long seed, final Holder<BlockEntityType<?>> type
   ) {
      super(predicates);
      this.name = name;
      this.seed = seed;
      this.type = type;
   }

   @Override
   public MapCodec<SetContainerLootTable> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (itemStack.isEmpty()) {
         return itemStack;
      }

      itemStack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.name, this.seed));
      return itemStack;
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      if (!context.allowsReferences()) {
         context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
      } else {
         if (context.resolver().get(this.name).isEmpty()) {
            context.reportProblem(new ValidationContext.MissingReferenceProblem(this.name));
         }
      }
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(final BlockEntityType<?> type, final ResourceKey<LootTable> value) {
      return simpleBuilder(conditions -> new SetContainerLootTable(conditions, value, 0L, type.builtInRegistryHolder()));
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(final BlockEntityType<?> type, final ResourceKey<LootTable> value, final long seed) {
      return simpleBuilder(conditions -> new SetContainerLootTable(conditions, value, seed, type.builtInRegistryHolder()));
   }
}
