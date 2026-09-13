package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record PendingInvite(String invitationId, String realmName, String realmOwnerName, UUID realmOwnerUuid, Instant date) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static @Nullable PendingInvite parse(final JsonObject json) {
      try {
         return new PendingInvite(
            JsonUtils.getStringOr("invitationId", json, ""),
            JsonUtils.getStringOr("worldName", json, ""),
            JsonUtils.getStringOr("worldOwnerName", json, ""),
            JsonUtils.getUuidOr("worldOwnerUuid", json, Util.NIL_UUID),
            JsonUtils.getDateOr("date", json)
         );
      } catch (Exception e) {
         LOGGER.error("Could not parse PendingInvite", e);
         return null;
      }
   }
}
