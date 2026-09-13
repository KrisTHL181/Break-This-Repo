package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SlotLoot extends LootPoolSingletonContainer {
   public static final MapCodec<SlotLoot> CODEC = RecordCodecBuilder.mapCodec(
      p_453432_ -> p_453432_.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(p_457365_ -> p_457365_.slotSource))
         .and(singletonFields(p_453432_))
         .apply(p_453432_, SlotLoot::new)
   );
   private final SlotSource slotSource;

   private SlotLoot(SlotSource p_458135_, int p_451342_, int p_459427_, List<LootItemCondition> p_452826_, List<LootItemFunction> p_458558_) {
      super(p_451342_, p_459427_, p_452826_, p_458558_);
      this.slotSource = p_458135_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.SLOTS;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_458633_, LootContext p_455679_) {
      this.slotSource.provide(p_455679_).itemCopies().filter(p_455693_ -> !p_455693_.isEmpty()).forEach(p_458633_);
   }

   @Override
   public void validate(ValidationContext p_456722_) {
      super.validate(p_456722_);
      this.slotSource.validate(p_456722_.forChild(new ProblemReporter.FieldPathElement("slot_source")));
   }
}
