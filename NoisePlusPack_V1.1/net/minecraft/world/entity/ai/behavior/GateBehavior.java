package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GateBehavior<E extends LivingEntity> implements BehaviorControl<E> {
   private final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
   private final Set<MemoryModuleType<?>> exitErasedMemories;
   private final GateBehavior.OrderPolicy orderPolicy;
   private final GateBehavior.RunningPolicy runningPolicy;
   private final ShufflingList<BehaviorControl<? super E>> behaviors = new ShufflingList<>();
   private Behavior.Status status = Behavior.Status.STOPPED;

   public GateBehavior(
      Map<MemoryModuleType<?>, MemoryStatus> p_22873_,
      Set<MemoryModuleType<?>> p_22874_,
      GateBehavior.OrderPolicy p_22875_,
      GateBehavior.RunningPolicy p_22876_,
      List<Pair<? extends BehaviorControl<? super E>, Integer>> p_22877_
   ) {
      this.entryCondition = p_22873_;
      this.exitErasedMemories = p_22874_;
      this.orderPolicy = p_22875_;
      this.runningPolicy = p_22876_;
      p_22877_.forEach(p_258332_ -> this.behaviors.add((BehaviorControl)p_258332_.getFirst(), (Integer)p_258332_.getSecond()));
   }

   @Override
   public Behavior.Status getStatus() {
      return this.status;
   }

   private boolean hasRequiredMemories(E p_259419_) {
      for (Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
         MemoryModuleType<?> memorymoduletype = entry.getKey();
         MemoryStatus memorystatus = entry.getValue();
         if (!p_259419_.getBrain().checkMemory(memorymoduletype, memorystatus)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public final boolean tryStart(ServerLevel p_259362_, E p_259746_, long p_259560_) {
      if (this.hasRequiredMemories(p_259746_)) {
         this.status = Behavior.Status.RUNNING;
         this.orderPolicy.apply(this.behaviors);
         this.runningPolicy.apply(this.behaviors.stream(), p_259362_, p_259746_, p_259560_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void tickOrStop(ServerLevel p_259934_, E p_259790_, long p_260259_) {
      this.behaviors
         .stream()
         .filter(p_258342_ -> p_258342_.getStatus() == Behavior.Status.RUNNING)
         .forEach(p_258336_ -> p_258336_.tickOrStop(p_259934_, p_259790_, p_260259_));
      if (this.behaviors.stream().noneMatch(p_258344_ -> p_258344_.getStatus() == Behavior.Status.RUNNING)) {
         this.doStop(p_259934_, p_259790_, p_260259_);
      }
   }

   @Override
   public final void doStop(ServerLevel p_259962_, E p_260250_, long p_259847_) {
      this.status = Behavior.Status.STOPPED;
      this.behaviors
         .stream()
         .filter(p_258337_ -> p_258337_.getStatus() == Behavior.Status.RUNNING)
         .forEach(p_258341_ -> p_258341_.doStop(p_259962_, p_260250_, p_259847_));
      this.exitErasedMemories.forEach(p_260250_.getBrain()::eraseMemory);
   }

   @Override
   public String debugString() {
      return this.getClass().getSimpleName();
   }

   @Override
   public String toString() {
      Set<? extends BehaviorControl<? super E>> set = this.behaviors
         .stream()
         .filter(p_258343_ -> p_258343_.getStatus() == Behavior.Status.RUNNING)
         .collect(Collectors.toSet());
      return "(" + this.getClass().getSimpleName() + "): " + set;
   }

   public enum OrderPolicy {
      ORDERED(p_147530_ -> {}),
      SHUFFLED(ShufflingList::shuffle);

      private final Consumer<ShufflingList<?>> consumer;

      OrderPolicy(final Consumer<ShufflingList<?>> p_22930_) {
         this.consumer = p_22930_;
      }

      public void apply(ShufflingList<?> p_147528_) {
         this.consumer.accept(p_147528_);
      }
   }

   public enum RunningPolicy {
      RUN_ONE {
         @Override
         public <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> p_147537_, ServerLevel p_147538_, E p_147539_, long p_147540_) {
            p_147537_.filter(p_258349_ -> p_258349_.getStatus() == Behavior.Status.STOPPED)
               .filter(p_258348_ -> p_258348_.tryStart(p_147538_, p_147539_, p_147540_))
               .findFirst();
         }
      },
      TRY_ALL {
         @Override
         public <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> p_147542_, ServerLevel p_147543_, E p_147544_, long p_147545_) {
            p_147542_.filter(p_258350_ -> p_258350_.getStatus() == Behavior.Status.STOPPED)
               .forEach(p_258354_ -> p_258354_.tryStart(p_147543_, p_147544_, p_147545_));
         }
      };

      public abstract <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> var1, ServerLevel var2, E var3, long var4);
   }
}
