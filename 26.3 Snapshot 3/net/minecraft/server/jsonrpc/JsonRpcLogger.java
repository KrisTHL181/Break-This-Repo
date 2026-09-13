package net.minecraft.server.jsonrpc;

import com.mojang.logging.LogUtils;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

public class JsonRpcLogger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = "RPC Connection #{}: ";

   public void log(final ClientInfo clientInfo, final String message, final Object... args) {
      LoggingEventBuilder builder = LOGGER.atInfo().setMessage("RPC Connection #{}: " + message).addArgument(clientInfo.connectionId());

      for (Object arg : args) {
         builder = builder.addArgument(arg);
      }

      builder.log();
   }
}
