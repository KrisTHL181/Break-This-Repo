package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.util.Util;

public interface InsideBlockEffectApplier {
   InsideBlockEffectApplier NOOP = new InsideBlockEffectApplier() {
      @Override
      public void apply(InsideBlockEffectType p_396510_) {
      }

      @Override
      public void runBefore(InsideBlockEffectType p_394339_, Consumer<Entity> p_396774_) {
      }

      @Override
      public void runAfter(InsideBlockEffectType p_393215_, Consumer<Entity> p_396216_) {
      }
   };

   void apply(InsideBlockEffectType var1);

   void runBefore(InsideBlockEffectType var1, Consumer<Entity> var2);

   void runAfter(InsideBlockEffectType var1, Consumer<Entity> var2);

   class StepBasedCollector implements InsideBlockEffectApplier {
      private static final InsideBlockEffectType[] APPLY_ORDER = InsideBlockEffectType.values();
      private static final int NO_STEP = -1;
      private final Set<InsideBlockEffectType> effectsInStep = EnumSet.noneOf(InsideBlockEffectType.class);
      private final Map<InsideBlockEffectType, List<Consumer<Entity>>> beforeEffectsInStep = Util.makeEnumMap(
         InsideBlockEffectType.class, p_392356_ -> new ArrayList<>()
      );
      private final Map<InsideBlockEffectType, List<Consumer<Entity>>> afterEffectsInStep = Util.makeEnumMap(
         InsideBlockEffectType.class, p_396160_ -> new ArrayList<>()
      );
      private final List<Consumer<Entity>> finalEffects = new ArrayList<>();
      private int lastStep = -1;

      public void advanceStep(int p_393208_) {
         if (this.lastStep != p_393208_) {
            this.lastStep = p_393208_;
            this.flushStep();
         }
      }

      public void applyAndClear(Entity p_393060_) {
         this.flushStep();

         for (Consumer<Entity> consumer : this.finalEffects) {
            if (!p_393060_.isAlive()) {
               break;
            }

            consumer.accept(p_393060_);
         }

         this.finalEffects.clear();
         this.lastStep = -1;
      }

      private void flushStep() {
         for (InsideBlockEffectType insideblockeffecttype : APPLY_ORDER) {
            List<Consumer<Entity>> list = this.beforeEffectsInStep.get(insideblockeffecttype);
            this.finalEffects.addAll(list);
            list.clear();
            if (this.effectsInStep.remove(insideblockeffecttype)) {
               this.finalEffects.add(insideblockeffecttype.effect());
            }

            List<Consumer<Entity>> list1 = this.afterEffectsInStep.get(insideblockeffecttype);
            this.finalEffects.addAll(list1);
            list1.clear();
         }
      }

      @Override
      public void apply(InsideBlockEffectType p_395478_) {
         this.effectsInStep.add(p_395478_);
      }

      @Override
      public void runBefore(InsideBlockEffectType p_397546_, Consumer<Entity> p_397676_) {
         this.beforeEffectsInStep.get(p_397546_).add(p_397676_);
      }

      @Override
      public void runAfter(InsideBlockEffectType p_391286_, Consumer<Entity> p_395455_) {
         this.afterEffectsInStep.get(p_391286_).add(p_395455_);
      }
   }
}
