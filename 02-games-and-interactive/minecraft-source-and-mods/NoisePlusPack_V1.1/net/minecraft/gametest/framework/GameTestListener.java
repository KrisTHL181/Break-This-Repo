package net.minecraft.gametest.framework;

public interface GameTestListener {
   void testStructureLoaded(GameTestInfo var1);

   void testPassed(GameTestInfo var1, GameTestRunner var2);

   void testFailed(GameTestInfo var1, GameTestRunner var2);

   void testAddedForRerun(GameTestInfo var1, GameTestInfo var2, GameTestRunner var3);
}
