package net.minecraft.world.item.slot;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class TransformedSlotSource implements SlotSource {
   protected final SlotSource slotSource;

   protected TransformedSlotSource(SlotSource p_453950_) {
      this.slotSource = p_453950_;
   }

   @Override
   public abstract MapCodec<? extends TransformedSlotSource> codec();

   protected static <T extends TransformedSlotSource> P1<Mu<T>, SlotSource> commonFields(Instance<T> p_460146_) {
      return p_460146_.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(p_459709_ -> p_459709_.slotSource));
   }

   protected abstract SlotCollection transform(SlotCollection var1);

   @Override
   public final SlotCollection provide(LootContext p_455202_) {
      return this.transform(this.slotSource.provide(p_455202_));
   }

   @Override
   public void validate(ValidationContext p_460284_) {
      SlotSource.super.validate(p_460284_);
      this.slotSource.validate(p_460284_.forChild(new ProblemReporter.FieldPathElement("slot_source")));
   }
}
