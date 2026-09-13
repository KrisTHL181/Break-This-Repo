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

   static <C, T> Stream<T> select(final Stream<T> entries, final Function<T, PriorityProvider<C, ?>> extractor, final C context) {
      List<PriorityProvider.UnpackedEntry<C, T>> unpackedEntries = new ArrayList<>();
      entries.forEach(
         entryx -> {
            PriorityProvider<C, ?> provider = extractor.apply((T)entryx);

            for (PriorityProvider.Selector<C, ?> selector : provider.selectors()) {
               unpackedEntries.add(
                  new PriorityProvider.UnpackedEntry<>(
                     (T)entryx,
                     selector.priority(),
                     (PriorityProvider.SelectorCondition<C>)DataFixUtils.orElseGet(selector.condition(), PriorityProvider.SelectorCondition::alwaysTrue)
                  )
               );
            }
         }
      );
      unpackedEntries.sort(PriorityProvider.UnpackedEntry.HIGHEST_PRIORITY_FIRST);
      Iterator<PriorityProvider.UnpackedEntry<C, T>> iterator = unpackedEntries.iterator();
      int highestMatchedPriority = Integer.MIN_VALUE;

      while (iterator.hasNext()) {
         PriorityProvider.UnpackedEntry<C, T> entry = iterator.next();
         if (entry.priority < highestMatchedPriority) {
            iterator.remove();
         } else if (entry.condition.test(context)) {
            highestMatchedPriority = entry.priority;
         } else {
            iterator.remove();
         }
      }

      return unpackedEntries.stream().map(PriorityProvider.UnpackedEntry::entry);
   }

   static <C, T> Optional<T> pick(
      final Stream<T> entries, final Function<T, PriorityProvider<C, ?>> extractor, final RandomSource randomSource, final C context
   ) {
      List<T> selected = select(entries, extractor, context).toList();
      return Util.getRandomSafe(selected, randomSource);
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> single(
      final Condition check, final int priority
   ) {
      return List.of(new PriorityProvider.Selector<>(check, priority));
   }

   static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> alwaysTrue(
      final int priority
   ) {
      return List.of(new PriorityProvider.Selector<>(Optional.empty(), priority));
   }

   record Selector<Context, Condition extends PriorityProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
      public Selector(final Condition condition, final int priority) {
         this(Optional.of(condition), priority);
      }

      public Selector(final int priority) {
         this(Optional.empty(), priority);
      }

      public static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> Codec<PriorityProvider.Selector<Context, Condition>> codec(
         final Codec<Condition> conditionCodec
      ) {
         return RecordCodecBuilder.create(
            i -> i.group(
                  conditionCodec.optionalFieldOf("condition").forGetter(PriorityProvider.Selector::condition),
                  Codec.INT.fieldOf("priority").forGetter(PriorityProvider.Selector::priority)
               )
               .apply(i, PriorityProvider.Selector::new)
         );
      }
   }

   @FunctionalInterface
   interface SelectorCondition<C> extends Predicate<C> {
      static <C> PriorityProvider.SelectorCondition<C> alwaysTrue() {
         return context -> true;
      }
   }

   record UnpackedEntry<C, T>(T entry, int priority, PriorityProvider.SelectorCondition<C> condition) {
      public static final Comparator<PriorityProvider.UnpackedEntry<?, ?>> HIGHEST_PRIORITY_FIRST = Comparator.comparingInt(
            PriorityProvider.UnpackedEntry::priority
         )
         .reversed();
   }
}
