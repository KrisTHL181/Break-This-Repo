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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record RealmsServerPlayerLists(Map<Long, List<ResolvableProfile>> servers) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static RealmsServerPlayerLists parse(String p_87597_) {
      Builder<Long, List<ResolvableProfile>> builder = ImmutableMap.builder();

      try {
         JsonObject jsonobject = GsonHelper.parse(p_87597_);
         if (GsonHelper.isArrayNode(jsonobject, "lists")) {
            for (JsonElement jsonelement : jsonobject.getAsJsonArray("lists")) {
               JsonObject jsonobject1 = jsonelement.getAsJsonObject();
               String s = JsonUtils.getStringOr("playerList", jsonobject1, null);
               List<ResolvableProfile> list;
               if (s != null) {
                  JsonElement jsonelement1 = LenientJsonParser.parse(s);
                  if (jsonelement1.isJsonArray()) {
                     list = parsePlayers(jsonelement1.getAsJsonArray());
                  } else {
                     list = Lists.newArrayList();
                  }
               } else {
                  list = Lists.newArrayList();
               }

               builder.put(JsonUtils.getLongOr("serverId", jsonobject1, -1L), list);
            }
         }
      } catch (Exception exception) {
         LOGGER.error("Could not parse RealmsServerPlayerLists", exception);
      }

      return new RealmsServerPlayerLists(builder.build());
   }

   private static List<ResolvableProfile> parsePlayers(JsonArray p_342185_) {
      List<ResolvableProfile> list = new ArrayList<>(p_342185_.size());

      for (JsonElement jsonelement : p_342185_) {
         if (jsonelement.isJsonObject()) {
            UUID uuid = JsonUtils.getUuidOr("playerId", jsonelement.getAsJsonObject(), null);
            if (uuid != null && !Minecraft.getInstance().isLocalPlayer(uuid)) {
               list.add(ResolvableProfile.createUnresolved(uuid));
            }
         }
      }

      return list;
   }

   public List<ResolvableProfile> getProfileResultsFor(long p_343284_) {
      List<ResolvableProfile> list = this.servers.get(p_343284_);
      return list != null ? list : List.of();
   }
}
