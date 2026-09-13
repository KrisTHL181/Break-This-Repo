package net.minecraft.client.renderer.block.dispatch.multipart;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;

@FunctionalInterface
public interface Condition {
   Codec<Condition> CODEC = Codec.recursive(
      "condition",
      self -> {
         Codec<CombinedCondition> combinerCodec = Codec.simpleMap(
               CombinedCondition.Operation.CODEC, self.listOf(), StringRepresentable.keys(CombinedCondition.Operation.values())
            )
            .codec()
            .comapFlatMap(map -> {
               if (map.size() != 1) {
                  return DataResult.error(() -> "Invalid map size for combiner condition, expected exactly one element");
               }

               Entry<CombinedCondition.Operation, List<Condition>> entry = (Entry<CombinedCondition.Operation, List<Condition>>)map.entrySet()
                  .iterator()
                  .next();
               return DataResult.success(new CombinedCondition(entry.getKey(), entry.getValue()));
            }, condition -> Map.of(condition.operation(), condition.terms()));
         return Codec.either(combinerCodec, KeyValueCondition.CODEC).flatComapMap(either -> (Condition)either.map(l -> l, r -> r), condition -> {
            return switch (condition) {
               case CombinedCondition combiner -> DataResult.success(Either.left(combiner));
               case KeyValueCondition keyValue -> DataResult.success(Either.right(keyValue));
               default -> DataResult.error(() -> "Unrecognized condition");
            };
         });
      }
   );

   <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> definition);
}
