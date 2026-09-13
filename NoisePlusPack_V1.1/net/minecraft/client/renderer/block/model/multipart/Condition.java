package net.minecraft.client.renderer.block.model.multipart;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface Condition {
   Codec<Condition> CODEC = Codec.recursive(
      "condition",
      p_389483_ -> {
         Codec<CombinedCondition> codec = Codec.simpleMap(
               CombinedCondition.Operation.CODEC, p_389483_.listOf(), StringRepresentable.keys(CombinedCondition.Operation.values())
            )
            .codec()
            .comapFlatMap(
               p_389485_ -> {
                  if (p_389485_.size() != 1) {
                     return DataResult.error(() -> "Invalid map size for combiner condition, expected exactly one element");
                  }

                  Entry<CombinedCondition.Operation, List<Condition>> entry = (Entry<CombinedCondition.Operation, List<Condition>>)p_389485_.entrySet()
                     .iterator()
                     .next();
                  return DataResult.success(new CombinedCondition(entry.getKey(), entry.getValue()));
               },
               p_389482_ -> Map.of(p_389482_.operation(), p_389482_.terms())
            );
         return Codec.either(codec, KeyValueCondition.CODEC)
            .flatComapMap(p_389486_ -> (Condition)p_389486_.map(p_389480_ -> p_389480_, p_389484_ -> p_389484_), p_389481_ -> {
               return switch (p_389481_) {
                  case CombinedCondition combinedcondition -> DataResult.success(Either.left(combinedcondition));
                  case KeyValueCondition keyvaluecondition -> DataResult.success(Either.right(keyvaluecondition));
                  default -> DataResult.error(() -> "Unrecognized condition");
               };
            });
      }
   );

   <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> var1);
}
