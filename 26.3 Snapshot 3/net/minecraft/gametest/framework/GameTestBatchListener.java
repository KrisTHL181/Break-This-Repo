package net.minecraft.gametest.framework;

public interface GameTestBatchListener {
   void testBatchStarting(final GameTestBatch batch);

   void testBatchFinished(final GameTestBatch batch);
}
