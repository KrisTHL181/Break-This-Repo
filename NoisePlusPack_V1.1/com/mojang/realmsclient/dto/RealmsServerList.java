package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsServerList(@SerializedName("servers") List<RealmsServer> servers) implements ReflectionBasedSerialization {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsServerList parse(GuardedSerializer p_408512_, String p_87578_) {
      try {
         RealmsServerList realmsserverlist = p_408512_.fromJson(p_87578_, RealmsServerList.class);
         if (realmsserverlist != null) {
            realmsserverlist.servers.forEach(RealmsServer::finalize);
            return realmsserverlist;
         }

         LOGGER.error("Could not parse McoServerList: {}", p_87578_);
      } catch (Exception exception) {
         LOGGER.error("Could not parse McoServerList", exception);
      }

      return new RealmsServerList(List.of());
   }
}
