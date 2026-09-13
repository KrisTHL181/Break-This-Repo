package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

public record EntityHasScoreCondition(Map<String, IntRange> scores, LootContext.EntityTarget entityTarget) implements LootItemCondition {
   public static final MapCodec<EntityHasScoreCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_297188_ -> p_297188_.group(
            Codec.unboundedMap(Codec.STRING, IntRange.CODEC).fieldOf("scores").forGetter(EntityHasScoreCondition::scores),
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(EntityHasScoreCondition::entityTarget)
         )
         .apply(p_297188_, EntityHasScoreCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.ENTITY_SCORES;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Stream.concat(
            Stream.of(this.entityTarget.contextParam()), this.scores.values().stream().flatMap(p_165487_ -> p_165487_.getReferencedContextParams().stream())
         )
         .collect(ImmutableSet.toImmutableSet());
   }

   public boolean test(LootContext p_81631_) {
      Entity entity = p_81631_.getOptionalParameter(this.entityTarget.contextParam());
      if (entity == null) {
         return false;
      }

      Scoreboard scoreboard = p_81631_.getLevel().getScoreboard();

      for (Entry<String, IntRange> entry : this.scores.entrySet()) {
         if (!this.hasScore(p_81631_, entity, scoreboard, entry.getKey(), entry.getValue())) {
            return false;
         }
      }

      return true;
   }

   protected boolean hasScore(LootContext p_165491_, Entity p_165492_, Scoreboard p_165493_, String p_165494_, IntRange p_165495_) {
      Objective objective = p_165493_.getObjective(p_165494_);
      if (objective == null) {
         return false;
      }

      ReadOnlyScoreInfo readonlyscoreinfo = p_165493_.getPlayerScoreInfo(p_165492_, objective);
      return readonlyscoreinfo == null ? false : p_165495_.test(p_165491_, readonlyscoreinfo.value());
   }

   public static EntityHasScoreCondition.Builder hasScores(LootContext.EntityTarget p_165489_) {
      return new EntityHasScoreCondition.Builder(p_165489_);
   }

   public static class Builder implements LootItemCondition.Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, IntRange> scores = ImmutableMap.builder();
      private final LootContext.EntityTarget entityTarget;

      public Builder(LootContext.EntityTarget p_165499_) {
         this.entityTarget = p_165499_;
      }

      public EntityHasScoreCondition.Builder withScore(String p_165501_, IntRange p_165502_) {
         this.scores.put(p_165501_, p_165502_);
         return this;
      }

      @Override
      public LootItemCondition build() {
         return new EntityHasScoreCondition(this.scores.build(), this.entityTarget);
      }
   }
}
