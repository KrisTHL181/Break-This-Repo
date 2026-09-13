package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestTicker {
   public static final GameTestTicker SINGLETON = new GameTestTicker();
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Collection<GameTestInfo> testInfos = Lists.newCopyOnWriteArrayList();
   private @Nullable GameTestRunner runner;
   private GameTestTicker.State state = GameTestTicker.State.IDLE;

   private GameTestTicker() {
   }

   public void add(final GameTestInfo testInfo) {
      this.testInfos.add(testInfo);
   }

   public void clear() {
      if (this.state != GameTestTicker.State.IDLE) {
         this.state = GameTestTicker.State.HALTING;
      } else {
         this.testInfos.clear();
         if (this.runner != null) {
            this.runner.stop();
            this.runner = null;
         }
      }
   }

   public void setRunner(final GameTestRunner runner) {
      if (this.runner != null) {
         Util.logAndPauseIfInIde("The runner was already set in GameTestTicker");
      }

      this.runner = runner;
   }

   public void tick() {
      if (this.runner != null) {
         this.state = GameTestTicker.State.RUNNING;
         this.testInfos.forEach(i -> i.tick(this.runner));
         this.testInfos.removeIf(GameTestInfo::isDone);
         GameTestTicker.State finishingState = this.state;
         this.state = GameTestTicker.State.IDLE;
         if (finishingState == GameTestTicker.State.HALTING) {
            this.clear();
         }
      }
   }

   private enum State {
      IDLE,
      RUNNING,
      HALTING;
   }
}
