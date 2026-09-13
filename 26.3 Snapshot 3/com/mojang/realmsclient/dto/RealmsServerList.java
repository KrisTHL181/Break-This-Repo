package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.util.List;
import org.slf4j.Logger;

public record RealmsServerList(@SerializedName("servers") List<RealmsServer> servers) implements ReflectionBasedSerialization {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsServerList parse(final GuardedSerializer gson, final String json) {
      try {
         RealmsServerList realmsServerList = gson.fromJson(json, RealmsServerList.class);
         if (realmsServerList != null) {
            realmsServerList.servers.forEach(RealmsServer::finalize);
            return realmsServerList;
         }

         LOGGER.error("Could not parse McoServerList: {}", json);
      } catch (Exception e) {
         LOGGER.error("Could not parse McoServerList", e);
      }

      return new RealmsServerList(List.of());
   }
}
