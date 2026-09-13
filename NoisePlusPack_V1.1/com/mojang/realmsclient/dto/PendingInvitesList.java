package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record PendingInvitesList(List<PendingInvite> pendingInvites) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static PendingInvitesList parse(String p_87437_) {
      List<PendingInvite> list = new ArrayList<>();

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87437_).getAsJsonObject();
         if (jsonobject.get("invites").isJsonArray()) {
            for (JsonElement jsonelement : jsonobject.get("invites").getAsJsonArray()) {
               PendingInvite pendinginvite = PendingInvite.parse(jsonelement.getAsJsonObject());
               if (pendinginvite != null) {
                  list.add(pendinginvite);
               }
            }
         }
      } catch (Exception exception) {
         LOGGER.error("Could not parse PendingInvitesList", exception);
      }

      return new PendingInvitesList(list);
   }
}
