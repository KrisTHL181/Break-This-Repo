package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionBuilder {
   private final Builder<String, KeyValueCondition.Terms> terms = ImmutableMap.builder();

   private <T extends Comparable<T>> void putValue(Property<T> p_392543_, KeyValueCondition.Terms p_394882_) {
      this.terms.put(p_392543_.getName(), p_394882_);
   }

   public final <T extends Comparable<T>> ConditionBuilder term(Property<T> p_394088_, T p_393189_) {
      this.putValue(p_394088_, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(p_394088_.getName(p_393189_), false))));
      return this;
   }

   @SafeVarargs
   public final <T extends Comparable<T>> ConditionBuilder term(Property<T> p_392462_, T p_397351_, T... p_395115_) {
      List<KeyValueCondition.Term> list = Stream.concat(Stream.of(p_397351_), Stream.of(p_395115_))
         .map(p_392462_::getName)
         .sorted()
         .distinct()
         .map(p_393302_ -> new KeyValueCondition.Term(p_393302_, false))
         .toList();
      this.putValue(p_392462_, new KeyValueCondition.Terms(list));
      return this;
   }

   public final <T extends Comparable<T>> ConditionBuilder negatedTerm(Property<T> p_395433_, T p_393560_) {
      this.putValue(p_395433_, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(p_395433_.getName(p_393560_), true))));
      return this;
   }

   public Condition build() {
      return new KeyValueCondition(this.terms.buildOrThrow());
   }
}
