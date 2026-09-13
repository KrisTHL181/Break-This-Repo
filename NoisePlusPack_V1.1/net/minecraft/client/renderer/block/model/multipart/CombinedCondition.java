package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CombinedCondition(CombinedCondition.Operation operation, List<Condition> terms) implements Condition {
   @Override
   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> p_394518_) {
      return this.operation.apply(Lists.transform(this.terms, p_397724_ -> p_397724_.instantiate(p_394518_)));
   }

   @OnlyIn(Dist.CLIENT)
   public enum Operation implements StringRepresentable {
      AND("AND") {
         @Override
         public <V> Predicate<V> apply(List<Predicate<V>> p_396002_) {
            return Util.allOf(p_396002_);
         }
      },
      OR("OR") {
         @Override
         public <V> Predicate<V> apply(List<Predicate<V>> p_393603_) {
            return Util.anyOf(p_393603_);
         }
      };

      public static final Codec<CombinedCondition.Operation> CODEC = StringRepresentable.fromEnum(CombinedCondition.Operation::values);
      private final String name;

      Operation(final String p_396752_) {
         this.name = p_396752_;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public abstract <V> Predicate<V> apply(List<Predicate<V>> var1);
   }
}
