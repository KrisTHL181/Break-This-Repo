package net.minecraft.world.entity.variant;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

public interface PriorityProvider<Context, Condition extends PriorityProvider.SelectorCondition<Context>> {
   List<PriorityProvider.Selector<Context, Condition>> selectors();

   static <C, T> Stream<T> select(Stream<T> p_396955_, Function<T, PriorityProvider<C, ?>> p_397812_, C p_397224_) {
      List<PriorityProvider.UnpackedEntry<C, T>> list = new ArrayList<>();
      p_396955_.forEach(
         p_393783_ -> {
            PriorityProvider<C, ?> priorityprovider = p_397812_.apply((T)p_393783_);

            for (PriorityProvider.Selector<C, ?> selector : priorityprovider.selectors()) {
               list.add(
                  new PriorityProvider.UnpackedEntry<>(
                     (T)p_393783_,
                     selector.priority(),
                     (PriorityProvider.SelectorCondition<C>)DataFixUtils.orElseGet(selector.condition(), PriorityProvider.SelectorCondition::alwaysTrue)
                  )
               );
            }
         }
      );
      list.sort(PriorityProvider.UnpackedEntry.HIGHEST_PRIORITY_FIRST);
      Iterator<PriorityProvider.UnpackedEntry<C, T>> iterator = list.iterator();
      int i = Integer.MIN_VALUE;

      while (iterator.hasNext()) {
         PriorityProvider.UnpackedEntry<C, T> unpackedentry = iterator.next();
         if (unpackedentry.priority < i) {
            iterator.remove();
         } else if (unpackedentry.condition.test(p_397224_)) {
            i = unpackedentry.priority;
         } else {
            iterator.remove();
         }
      }

      return list.stream().map(PriorityProvider.UnpackedEntry::entry);
   }

   static <C, T> Optional<T> pick(Stream<T> p_396747_, Function<T, PriorityProvider<C, ?>> p_391185_, RandomSource p_393478_, C p_393720_) {
      List<T> list = select(p_396747_, p_391185_, p_393720_).toList();
      return Util.getRandomSafe(list, p_393478_);
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> single(
      Condition p_396716_, int p_397144_
   ) {
      return List.of(new PriorityProvider.Selector<>(p_396716_, p_397144_));
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> alwaysTrue(int p_393120_) {
      return List.of(new PriorityProvider.Selector<>(Optional.empty(), p_393120_));
   }

   record Selector<Context, Condition extends PriorityProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
      public Selector(Condition p_391905_, int p_391164_) {
         this(Optional.of(p_391905_), p_391164_);
      }

      public Selector(int p_397244_) {
         this(Optional.empty(), p_397244_);
      }

      public static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> Codec<PriorityProvider.Selector<Context, Condition>> codec(
         Codec<Condition> p_395907_
      ) {
         return RecordCodecBuilder.create(
            p_394411_ -> p_394411_.group(
                  p_395907_.optionalFieldOf("condition").forGetter(PriorityProvider.Selector::condition),
                  Codec.INT.fieldOf("priority").forGetter(PriorityProvider.Selector::priority)
               )
               .apply(p_394411_, PriorityProvider.Selector::new)
         );
      }
   }

   @FunctionalInterface
   interface SelectorCondition<C> extends Predicate<C> {
      static <C> PriorityProvider.SelectorCondition<C> alwaysTrue() {
         return p_397254_ -> true;
      }
   }

   record UnpackedEntry<C, T>(T entry, int priority, PriorityProvider.SelectorCondition<C> condition) {
      public static final Comparator<PriorityProvider.UnpackedEntry<?, ?>> HIGHEST_PRIORITY_FIRST = Comparator.comparingInt(
            PriorityProvider.UnpackedEntry::priority
         )
         .reversed();
   }
}
