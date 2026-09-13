package net.minecraft.advancements.criterion;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends SimpleCriterionTrigger.SimpleInstance> implements CriterionTrigger<T> {
   private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

   @Override
   public final void addPlayerListener(PlayerAdvancements p_454131_, CriterionTrigger.Listener<T> p_450538_) {
      this.players.computeIfAbsent(p_454131_, p_458047_ -> Sets.newHashSet()).add(p_450538_);
   }

   @Override
   public final void removePlayerListener(PlayerAdvancements p_455543_, CriterionTrigger.Listener<T> p_455386_) {
      Set<CriterionTrigger.Listener<T>> set = this.players.get(p_455543_);
      if (set != null) {
         set.remove(p_455386_);
         if (set.isEmpty()) {
            this.players.remove(p_455543_);
         }
      }
   }

   @Override
   public final void removePlayerListeners(PlayerAdvancements p_457963_) {
      this.players.remove(p_457963_);
   }

   protected void trigger(ServerPlayer p_459834_, Predicate<T> p_454123_) {
      PlayerAdvancements playeradvancements = p_459834_.getAdvancements();
      Set<CriterionTrigger.Listener<T>> set = this.players.get(playeradvancements);
      if (set != null && !set.isEmpty()) {
         LootContext lootcontext = EntityPredicate.createContext(p_459834_, p_459834_);
         List<CriterionTrigger.Listener<T>> list = null;

         for (CriterionTrigger.Listener<T> listener : set) {
            T t = listener.trigger();
            if (p_454123_.test(t)) {
               Optional<ContextAwarePredicate> optional = t.player();
               if (optional.isEmpty() || optional.get().matches(lootcontext)) {
                  if (list == null) {
                     list = Lists.newArrayList();
                  }

                  list.add(listener);
               }
            }
         }

         if (list != null) {
            for (CriterionTrigger.Listener<T> listener1 : list) {
               listener1.run(playeradvancements);
            }
         }
      }
   }

   public interface SimpleInstance extends CriterionTriggerInstance {
      @Override
      default void validate(CriterionValidator p_459776_) {
         p_459776_.validateEntity(this.player(), "player");
      }

      Optional<ContextAwarePredicate> player();
   }
}
