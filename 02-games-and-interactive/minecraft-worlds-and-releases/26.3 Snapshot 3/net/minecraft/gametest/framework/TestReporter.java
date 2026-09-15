package net.minecraft.gametest.framework;

public interface TestReporter {
   void onTestFailed(GameTestInfo testInfo);

   void onTestSuccess(GameTestInfo testInfo);

   default void finish() {
   }
}
