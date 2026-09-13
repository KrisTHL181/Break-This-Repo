package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public class GameTestSequence {
   final GameTestInfo parent;
   private final List<GameTestEvent> events = Lists.newArrayList();
   private int lastTick;

   GameTestSequence(GameTestInfo p_177542_) {
      this.parent = p_177542_;
      this.lastTick = p_177542_.getTick();
   }

   public GameTestSequence thenWaitUntil(Runnable p_177553_) {
      this.events.add(GameTestEvent.create(p_177553_));
      return this;
   }

   public GameTestSequence thenWaitUntil(long p_177550_, Runnable p_177551_) {
      this.events.add(GameTestEvent.create(p_177550_, p_177551_));
      return this;
   }

   public GameTestSequence thenIdle(int p_177545_) {
      return this.thenExecuteAfter(p_177545_, () -> {});
   }

   public GameTestSequence thenExecute(Runnable p_177563_) {
      this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(p_177563_)));
      return this;
   }

   public GameTestSequence thenExecuteAfter(int p_177547_, Runnable p_177548_) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + p_177547_) {
            throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
         }

         this.executeWithoutFail(p_177548_);
      }));
      return this;
   }

   public GameTestSequence thenExecuteFor(int p_177560_, Runnable p_177561_) {
      this.events.add(GameTestEvent.create(() -> {
         if (this.parent.getTick() < this.lastTick + p_177560_) {
            this.executeWithoutFail(p_177561_);
            throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
         }
      }));
      return this;
   }

   public void thenSucceed() {
      this.events.add(GameTestEvent.create(this.parent::succeed));
   }

   public void thenFail(Supplier<GameTestException> p_177555_) {
      this.events.add(GameTestEvent.create(() -> this.parent.fail(p_177555_.get())));
   }

   public GameTestSequence.Condition thenTrigger() {
      GameTestSequence.Condition gametestsequence$condition = new GameTestSequence.Condition();
      this.events.add(GameTestEvent.create(() -> gametestsequence$condition.trigger(this.parent.getTick())));
      return gametestsequence$condition;
   }

   public void tickAndContinue(int p_396814_) {
      try {
         this.tick(p_396814_);
      } catch (GameTestAssertException var3) {
      }
   }

   public void tickAndFailIfNotComplete(int p_393498_) {
      try {
         this.tick(p_393498_);
      } catch (GameTestAssertException gametestassertexception) {
         this.parent.fail(gametestassertexception);
      }
   }

   private void executeWithoutFail(Runnable p_177571_) {
      try {
         p_177571_.run();
      } catch (GameTestAssertException gametestassertexception) {
         this.parent.fail(gametestassertexception);
      }
   }

   private void tick(int p_391876_) {
      Iterator<GameTestEvent> iterator = this.events.iterator();

      while (iterator.hasNext()) {
         GameTestEvent gametestevent = iterator.next();
         gametestevent.assertion.run();
         iterator.remove();
         int i = p_391876_ - this.lastTick;
         int j = this.lastTick;
         this.lastTick = p_391876_;
         if (gametestevent.expectedDelay != null && gametestevent.expectedDelay != i) {
            this.parent
               .fail(new GameTestAssertException(Component.translatable("test.error.sequence.invalid_tick", j + gametestevent.expectedDelay), p_391876_));
            break;
         }
      }
   }

   public class Condition {
      private static final int NOT_TRIGGERED = -1;
      private int triggerTime = -1;

      void trigger(int p_396403_) {
         if (this.triggerTime != -1) {
            throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
         }

         this.triggerTime = p_396403_;
      }

      public void assertTriggeredThisTick() {
         int i = GameTestSequence.this.parent.getTick();
         if (this.triggerTime != i) {
            if (this.triggerTime == -1) {
               throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_not_triggered"), i);
            } else {
               throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_already_triggered", this.triggerTime), i);
            }
         }
      }
   }
}
