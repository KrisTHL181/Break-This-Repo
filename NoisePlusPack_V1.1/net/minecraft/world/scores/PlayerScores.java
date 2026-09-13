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

   public @Nullable Score get(Objective p_310183_) {
      return (Score)this.scores.get(p_310183_);
   }

   public Score getOrCreate(Objective p_310156_, Consumer<Score> p_310669_) {
      return (Score)this.scores.computeIfAbsent(p_310156_, p_312480_ -> {
         Score score = new Score();
         p_310669_.accept(score);
         return score;
      });
   }

   public boolean remove(Objective p_312444_) {
      return this.scores.remove(p_312444_) != null;
   }

   public boolean hasScores() {
      return !this.scores.isEmpty();
   }

   public Object2IntMap<Objective> listScores() {
      Object2IntMap<Objective> object2intmap = new Object2IntOpenHashMap();
      this.scores.forEach((p_309981_, p_312246_) -> object2intmap.put(p_309981_, p_312246_.value()));
      return object2intmap;
   }

   void setScore(Objective p_312005_, Score p_312306_) {
      this.scores.put(p_312005_, p_312306_);
   }

   Map<Objective, Score> listRawScores() {
      return Collections.unmodifiableMap(this.scores);
   }
}
