package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public record ScoreboardValue(ScoreboardNameProvider target, String score, float scale) implements NumberProvider {
   public static final MapCodec<ScoreboardValue> CODEC = RecordCodecBuilder.mapCodec(
      p_297867_ -> p_297867_.group(
            ScoreboardNameProviders.CODEC.fieldOf("target").forGetter(ScoreboardValue::target),
            Codec.STRING.fieldOf("score").forGetter(ScoreboardValue::score),
            Codec.FLOAT.fieldOf("scale").orElse(1.0F).forGetter(ScoreboardValue::scale)
         )
         .apply(p_297867_, ScoreboardValue::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.SCORE;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return this.target.getReferencedContextParams();
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget p_165750_, String p_165751_) {
      return fromScoreboard(p_165750_, p_165751_, 1.0F);
   }

   public static ScoreboardValue fromScoreboard(LootContext.EntityTarget p_165753_, String p_165754_, float p_165755_) {
      return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(p_165753_), p_165754_, p_165755_);
   }

   @Override
   public float getFloat(LootContext p_165758_) {
      ScoreHolder scoreholder = this.target.getScoreHolder(p_165758_);
      if (scoreholder == null) {
         return 0.0F;
      }

      Scoreboard scoreboard = p_165758_.getLevel().getScoreboard();
      Objective objective = scoreboard.getObjective(this.score);
      if (objective == null) {
         return 0.0F;
      }

      ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
      return readonlyscoreinfo == null ? 0.0F : readonlyscoreinfo.value() * this.scale;
   }
}
