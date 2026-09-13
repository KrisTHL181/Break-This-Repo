package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.util.Util;

public class AnyOfCondition extends CompositeLootItemCondition {
   public static final MapCodec<AnyOfCondition> MAP_CODEC = createCodec(AnyOfCondition::new);

   private AnyOfCondition(final List<LootItemCondition> terms) {
      super(terms, Util.anyOf(terms));
   }

   @Override
   public MapCodec<AnyOfCondition> codec() {
      return MAP_CODEC;
   }

   public static AnyOfCondition.Builder anyOf(final LootItemCondition.Builder... terms) {
      return new AnyOfCondition.Builder(terms);
   }

   public static class Builder extends CompositeLootItemCondition.Builder {
      public Builder(final LootItemCondition.Builder... terms) {
         super(terms);
      }

      @Override
      public AnyOfCondition.Builder or(final LootItemCondition.Builder term) {
         this.addTerm(term);
         return this;
      }

      @Override
      protected LootItemCondition create(final List<LootItemCondition> terms) {
         return new AnyOfCondition(terms);
      }
   }
}
