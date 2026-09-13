package net.minecraft.world.item.slot;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class TransformedSlotSource implements SlotSource {
   protected final SlotSource slotSource;

   protected TransformedSlotSource(final SlotSource slotSource) {
      this.slotSource = slotSource;
   }

   @Override
   public abstract MapCodec<? extends TransformedSlotSource> codec();

   protected static <T extends TransformedSlotSource> P1<Mu<T>, SlotSource> commonFields(final Instance<T> i) {
      return i.group(SlotSources.DIRECT_CODEC.fieldOf("slot_source").forGetter(t -> t.slotSource));
   }

   protected abstract SlotCollection transform(SlotCollection slots);

   @Override
   public final SlotCollection provide(final LootContext context) {
      return this.transform(this.slotSource.provide(context));
   }

   @Override
   public void validate(final ValidationContext context) {
      SlotSource.super.validate(context);
      Validatable.validate(context, "slot_source", this.slotSource);
   }
}
