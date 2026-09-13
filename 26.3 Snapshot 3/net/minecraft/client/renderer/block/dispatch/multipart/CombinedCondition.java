package net.minecraft.client.renderer.block.dispatch.multipart;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;

public record CombinedCondition(CombinedCondition.Operation operation, List<Condition> terms) implements Condition {
   @Override
   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(final StateDefinition<O, S> definition) {
      return this.operation.apply(Lists.transform(this.terms, c -> c.instantiate(definition)));
   }

   public enum Operation implements StringRepresentable {
      AND("AND") {
         @Override
         public <V> Predicate<V> apply(final List<Predicate<V>> terms) {
            return Util.allOf(terms);
         }
      },
      OR("OR") {
         @Override
         public <V> Predicate<V> apply(final List<Predicate<V>> terms) {
            return Util.anyOf(terms);
         }
      };

      public static final Codec<CombinedCondition.Operation> CODEC = StringRepresentable.fromEnum(CombinedCondition.Operation::values);
      private final String name;

      Operation(final String name) {
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract <V> Predicate<V> apply(List<Predicate<V>> terms);
   }
}
