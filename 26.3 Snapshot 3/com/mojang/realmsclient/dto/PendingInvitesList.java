package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import org.slf4j.Logger;

public record PendingInvitesList(List<PendingInvite> pendingInvites) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static PendingInvitesList parse(final String json) {
      List<PendingInvite> pendingInvites = new ArrayList<>();

      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         if (jsonObject.get("invites").isJsonArray()) {
            for (JsonElement element : jsonObject.get("invites").getAsJsonArray()) {
               PendingInvite entry = PendingInvite.parse(element.getAsJsonObject());
               if (entry != null) {
                  pendingInvites.add(entry);
               }
            }
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse PendingInvitesList", e);
      }

      return new PendingInvitesList(pendingInvites);
   }
}
