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
      public void apply(final InsideBlockEffectType type) {
      }

      @Override
      public void runBefore(final InsideBlockEffectType type, final Consumer<Entity> effect) {
      }

      @Override
      public void runAfter(final InsideBlockEffectType type, final Consumer<Entity> effect) {
      }
   };

   void apply(InsideBlockEffectType type);

   void runBefore(InsideBlockEffectType type, Consumer<Entity> effect);

   void runAfter(InsideBlockEffectType type, Consumer<Entity> effect);

   class StepBasedCollector implements InsideBlockEffectApplier {
      private static final InsideBlockEffectType[] APPLY_ORDER = InsideBlockEffectType.values();
      private static final int NO_STEP = -1;
      private final Set<InsideBlockEffectType> effectsInStep = EnumSet.noneOf(InsideBlockEffectType.class);
      private final Map<InsideBlockEffectType, List<Consumer<Entity>>> beforeEffectsInStep = Util.makeEnumMap(
         InsideBlockEffectType.class, type -> new ArrayList<>()
      );
      private final Map<InsideBlockEffectType, List<Consumer<Entity>>> afterEffectsInStep = Util.makeEnumMap(
         InsideBlockEffectType.class, type -> new ArrayList<>()
      );
      private final List<Consumer<Entity>> finalEffects = new ArrayList<>();
      private int lastStep = -1;

      public void advanceStep(final int step) {
         if (this.lastStep != step) {
            this.lastStep = step;
            this.flushStep();
         }
      }

      public void applyAndClear(final Entity entity) {
         this.flushStep();

         for (Consumer<Entity> effect : this.finalEffects) {
            if (!entity.isAlive()) {
               break;
            }

            effect.accept(entity);
         }

         this.finalEffects.clear();
         this.lastStep = -1;
      }

      private void flushStep() {
         for (InsideBlockEffectType type : APPLY_ORDER) {
            List<Consumer<Entity>> beforeEffects = this.beforeEffectsInStep.get(type);
            this.finalEffects.addAll(beforeEffects);
            beforeEffects.clear();
            if (this.effectsInStep.remove(type)) {
               this.finalEffects.add(type.effect());
            }

            List<Consumer<Entity>> afterEffects = this.afterEffectsInStep.get(type);
            this.finalEffects.addAll(afterEffects);
            afterEffects.clear();
         }
      }

      @Override
      public void apply(final InsideBlockEffectType type) {
         this.effectsInStep.add(type);
      }

      @Override
      public void runBefore(final InsideBlockEffectType type, final Consumer<Entity> effect) {
         this.beforeEffectsInStep.get(type).add(effect);
      }

      @Override
      public void runAfter(final InsideBlockEffectType type, final Consumer<Entity> effect) {
         this.afterEffectsInStep.get(type).add(effect);
      }
   }
}
