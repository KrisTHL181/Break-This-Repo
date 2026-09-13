package net.minecraft.server.jsonrpc;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import org.slf4j.Logger;

public class JsonRpcLogger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = "RPC Connection #{}: ";

   public void log(ClientInfo p_428112_, String p_423252_, Object... p_429537_) {
      if (p_429537_.length == 0) {
         LOGGER.info("RPC Connection #{}: " + p_423252_, p_428112_.connectionId());
      } else {
         List<Object> list = new ArrayList<>(Arrays.asList(p_429537_));
         list.addFirst(p_428112_.connectionId());
         LOGGER.info("RPC Connection #{}: " + p_423252_, list.toArray());
      }
   }
}
