package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LoggedPrintStream extends PrintStream {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final String name;

   public LoggedPrintStream(String p_135951_, OutputStream p_135952_) {
      super(p_135952_, false, StandardCharsets.UTF_8);
      this.name = p_135951_;
   }

   @Override
   public void println(@Nullable String p_135957_) {
      this.logLine(p_135957_);
   }

   @Override
   public void println(@Nullable Object p_135955_) {
      this.logLine(String.valueOf(p_135955_));
   }

   protected void logLine(@Nullable String p_135953_) {
      LOGGER.info("[{}]: {}", this.name, p_135953_);
   }
}
