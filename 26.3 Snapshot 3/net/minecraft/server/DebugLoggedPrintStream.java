package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class DebugLoggedPrintStream extends LoggedPrintStream {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DebugLoggedPrintStream(final String name, final OutputStream out) {
      super(name, out);
   }

   @Override
   protected void logLine(final @Nullable String out) {
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      StackTraceElement stackTraceElement = stackTrace[Math.min(3, stackTrace.length)];
      LOGGER.info("[{}]@.({}:{}): {}", new Object[]{this.name, stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), out});
   }
}
