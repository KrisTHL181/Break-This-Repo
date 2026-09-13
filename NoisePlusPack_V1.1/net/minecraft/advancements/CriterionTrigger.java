package net.minecraft.advancements;

import com.mojang.serialization.Codec;
import net.minecraft.server.PlayerAdvancements;

public interface CriterionTrigger<T extends CriterionTriggerInstance> {
   void addPlayerListener(PlayerAdvancements var1, CriterionTrigger.Listener<T> var2);

   void removePlayerListener(PlayerAdvancements var1, CriterionTrigger.Listener<T> var2);

   void removePlayerListeners(PlayerAdvancements var1);

   Codec<T> codec();

   default Criterion<T> createCriterion(T p_299598_) {
      return new Criterion<>(this, p_299598_);
   }

   record Listener<T extends CriterionTriggerInstance>(T trigger, AdvancementHolder advancement, String criterion) {
      public void run(PlayerAdvancements p_13687_) {
         p_13687_.award(this.advancement, this.criterion);
      }
   }
}
