package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class LogTestReporter implements TestReporter {
   private static final Logger LOGGER = LogUtils.getLogger();

   @Override
   public void onTestFailed(final GameTestInfo testInfo) {
      String testPosition = testInfo.getTestBlockPos().toShortString();
      if (testInfo.isRequired()) {
         LOGGER.error("{} failed at {}! {}", new Object[]{testInfo.id(), testPosition, Util.describeError(testInfo.getError())});
      } else {
         LOGGER.warn("(optional) {} failed at {}. {}", new Object[]{testInfo.id(), testPosition, Util.describeError(testInfo.getError())});
      }
   }

   @Override
   public void onTestSuccess(final GameTestInfo testInfo) {
   }
}
