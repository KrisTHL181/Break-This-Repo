package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeSlotSource implements SlotSource {
   protected final List<SlotSource> terms;
   private final Function<LootContext, SlotCollection> compositeSlotSource;

   protected CompositeSlotSource(List<SlotSource> p_456540_) {
      this.terms = p_456540_;
      this.compositeSlotSource = SlotSources.group(p_456540_);
   }

   protected static <T extends CompositeSlotSource> MapCodec<T> createCodec(Function<List<SlotSource>, T> p_460254_) {
      return RecordCodecBuilder.mapCodec(
         p_459142_ -> p_459142_.group(SlotSources.CODEC.listOf().fieldOf("terms").forGetter(p_452369_ -> p_452369_.terms)).apply(p_459142_, p_460254_)
      );
   }

   protected static <T extends CompositeSlotSource> Codec<T> createInlineCodec(Function<List<SlotSource>, T> p_460759_) {
      return SlotSources.CODEC.listOf().xmap(p_460759_, p_453383_ -> p_453383_.terms);
   }

   @Override
   public abstract MapCodec<? extends CompositeSlotSource> codec();

   @Override
   public SlotCollection provide(LootContext p_457470_) {
      return this.compositeSlotSource.apply(p_457470_);
   }

   @Override
   public void validate(ValidationContext p_459527_) {
      SlotSource.super.validate(p_459527_);

      for (int i = 0; i < this.terms.size(); i++) {
         this.terms.get(i).validate(p_459527_.forChild(new ProblemReporter.IndexedFieldPathElement("terms", i)));
      }
   }
}
