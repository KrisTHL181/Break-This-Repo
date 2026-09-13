package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.util.Util;

public class AllOfCondition extends CompositeLootItemCondition {
   public static final MapCodec<AllOfCondition> MAP_CODEC = createCodec(AllOfCondition::new);
   public static final Codec<AllOfCondition> INLINE_CODEC = createInlineCodec(AllOfCondition::new);

   private AllOfCondition(final List<LootItemCondition> terms) {
      super(terms, Util.allOf(terms));
   }

   public static AllOfCondition allOf(final List<LootItemCondition> terms) {
      return new AllOfCondition(List.copyOf(terms));
   }

   @Override
   public MapCodec<AllOfCondition> codec() {
      return MAP_CODEC;
   }

   public static AllOfCondition.Builder allOf(final LootItemCondition.Builder... terms) {
      return new AllOfCondition.Builder(terms);
   }

   public static class Builder extends CompositeLootItemCondition.Builder {
      public Builder(final LootItemCondition.Builder... terms) {
         super(terms);
      }

      @Override
      public AllOfCondition.Builder and(final LootItemCondition.Builder term) {
         this.addTerm(term);
         return this;
      }

      @Override
      protected LootItemCondition create(final List<LootItemCondition> terms) {
         return new AllOfCondition(terms);
      }
   }
}
