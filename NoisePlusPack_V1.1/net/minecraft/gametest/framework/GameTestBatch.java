package net.minecraft.gametest.framework;

import java.util.Collection;
import net.minecraft.core.Holder;

public record GameTestBatch(int index, Collection<GameTestInfo> gameTestInfos, Holder<TestEnvironmentDefinition> environment) {
   public GameTestBatch {
      if (gameTestInfos.isEmpty()) {
         throw new IllegalArgumentException("A GameTestBatch must include at least one GameTestInfo!");
      }
   }
}
