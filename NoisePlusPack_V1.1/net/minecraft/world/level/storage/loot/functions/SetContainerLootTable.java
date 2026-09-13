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
   public static final MapCodec<SetContainerLootTable> CODEC = RecordCodecBuilder.mapCodec(
      p_391134_ -> commonFields(p_391134_)
         .and(
            p_391134_.group(
               LootTable.KEY_CODEC.fieldOf("name").forGetter(p_327592_ -> p_327592_.name),
               Codec.LONG.optionalFieldOf("seed", 0L).forGetter(p_297122_ -> p_297122_.seed),
               BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(p_297116_ -> p_297116_.type)
            )
         )
         .apply(p_391134_, SetContainerLootTable::new)
   );
   private final ResourceKey<LootTable> name;
   private final long seed;
   private final Holder<BlockEntityType<?>> type;

   private SetContainerLootTable(List<LootItemCondition> p_297857_, ResourceKey<LootTable> p_335799_, long p_193047_, Holder<BlockEntityType<?>> p_300516_) {
      super(p_297857_);
      this.name = p_335799_;
      this.seed = p_193047_;
      this.type = p_300516_;
   }

   @Override
   public LootItemFunctionType<SetContainerLootTable> getType() {
      return LootItemFunctions.SET_LOOT_TABLE;
   }

   @Override
   public ItemStack run(ItemStack p_80967_, LootContext p_80968_) {
      if (p_80967_.isEmpty()) {
         return p_80967_;
      }

      p_80967_.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(this.name, this.seed));
      return p_80967_;
   }

   @Override
   public void validate(ValidationContext p_80970_) {
      super.validate(p_80970_);
      if (!p_80970_.allowsReferences()) {
         p_80970_.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
      } else if (p_80970_.resolver().get(this.name).isEmpty()) {
         p_80970_.reportProblem(new ValidationContext.MissingReferenceProblem(this.name));
      }
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193050_, ResourceKey<LootTable> p_334597_) {
      return simpleBuilder(p_327599_ -> new SetContainerLootTable(p_327599_, p_334597_, 0L, p_193050_.builtInRegistryHolder()));
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193053_, ResourceKey<LootTable> p_332251_, long p_193055_) {
      return simpleBuilder(p_327596_ -> new SetContainerLootTable(p_327596_, p_332251_, p_193055_, p_193053_.builtInRegistryHolder()));
   }
}
