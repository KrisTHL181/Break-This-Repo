package net.minecraft.world.scores;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

class PlayerScores {
   private final Reference2ObjectOpenHashMap<Objective, Score> scores = new Reference2ObjectOpenHashMap(16, 0.5F);

   public @Nullable Score get(final Objective objective) {
      return (Score)this.scores.get(objective);
   }

   public Score getOrCreate(final Objective objective, final Consumer<Score> newResultCallback) {
      return (Score)this.scores.computeIfAbsent(objective, obj -> {
         Score newScore = new Score();
         newResultCallback.accept(newScore);
         return newScore;
      });
   }

   public boolean remove(final Objective objective) {
      return this.scores.remove(objective) != null;
   }

   public boolean hasScores() {
      return !this.scores.isEmpty();
   }

   public Object2IntMap<Objective> listScores() {
      Object2IntMap<Objective> result = new Object2IntOpenHashMap();
      this.scores.forEach((objective, score) -> result.put(objective, score.value()));
      return result;
   }

   void setScore(final Objective objective, final Score score) {
      this.scores.put(objective, score);
   }

   Map<Objective, Score> listRawScores() {
      return Collections.unmodifiableMap(this.scores);
   }
}
