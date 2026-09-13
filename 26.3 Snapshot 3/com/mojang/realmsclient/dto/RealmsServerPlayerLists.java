package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;

public record RealmsServerPlayerLists(Map<Long, List<ResolvableProfile>> servers) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsServerPlayerLists parse(final String json) {
      Builder<Long, List<ResolvableProfile>> elements = ImmutableMap.builder();

      try {
         JsonObject object = GsonHelper.parse(json);
         if (GsonHelper.isArrayNode(object, "lists")) {
            for (JsonElement jsonElement : object.getAsJsonArray("lists")) {
               JsonObject node = jsonElement.getAsJsonObject();
               String playerListString = JsonUtils.getStringOr("playerList", node, null);
               List<ResolvableProfile> players;
               if (playerListString != null) {
                  JsonElement element = LenientJsonParser.parse(playerListString);
                  if (element.isJsonArray()) {
                     players = parsePlayers(element.getAsJsonArray());
                  } else {
                     players = Lists.newArrayList();
                  }
               } else {
                  players = Lists.newArrayList();
               }

               elements.put(JsonUtils.getLongOr("serverId", node, -1L), players);
            }
         }
      } catch (Exception e) {
         LOGGER.error("Could not parse RealmsServerPlayerLists", e);
      }

      return new RealmsServerPlayerLists(elements.build());
   }

   private static List<ResolvableProfile> parsePlayers(final JsonArray array) {
      List<ResolvableProfile> profiles = new ArrayList<>(array.size());

      for (JsonElement element : array) {
         if (element.isJsonObject()) {
            UUID playerId = JsonUtils.getUuidOr("playerId", element.getAsJsonObject(), null);
            if (playerId != null && !Minecraft.getInstance().isLocalPlayer(playerId)) {
               profiles.add(ResolvableProfile.createUnresolved(playerId));
            }
         }
      }

      return profiles;
   }

   public List<ResolvableProfile> getProfileResultsFor(final long serverId) {
      List<ResolvableProfile> profileResults = this.servers.get(serverId);
      return profileResults != null ? profileResults : List.of();
   }
}
