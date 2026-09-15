package net.minecraft.gametest.framework;

public interface TestReporter {
   void onTestFailed(GameTestInfo var1);

   void onTestSuccess(GameTestInfo var1);

   default void finish() {
   }
}
