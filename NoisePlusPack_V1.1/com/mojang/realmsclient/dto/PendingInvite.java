package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record PendingInvite(String invitationId, String realmName, String realmOwnerName, UUID realmOwnerUuid, Instant date) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static @Nullable PendingInvite parse(JsonObject p_87431_) {
      try {
         return new PendingInvite(
            JsonUtils.getStringOr("invitationId", p_87431_, ""),
            JsonUtils.getStringOr("worldName", p_87431_, ""),
            JsonUtils.getStringOr("worldOwnerName", p_87431_, ""),
            JsonUtils.getUuidOr("worldOwnerUuid", p_87431_, Util.NIL_UUID),
            JsonUtils.getDateOr("date", p_87431_)
         );
      } catch (Exception exception) {
         LOGGER.error("Could not parse PendingInvite", exception);
         return null;
      }
   }
}
