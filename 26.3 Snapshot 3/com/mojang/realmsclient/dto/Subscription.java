package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import net.minecraft.util.LenientJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record Subscription(Instant startDate, int daysLeft, Subscription.SubscriptionType type) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Subscription parse(final String json) {
      try {
         JsonObject jsonObject = LenientJsonParser.parse(json).getAsJsonObject();
         return new Subscription(
            JsonUtils.getDateOr("startDate", jsonObject),
            JsonUtils.getIntOr("daysLeft", jsonObject, 0),
            typeFrom(JsonUtils.getStringOr("subscriptionType", jsonObject, null))
         );
      } catch (Exception e) {
         LOGGER.error("Could not parse Subscription", e);
         return new Subscription(Instant.EPOCH, 0, Subscription.SubscriptionType.NORMAL);
      }
   }

   private static Subscription.SubscriptionType typeFrom(final @Nullable String subscriptionType) {
      try {
         if (subscriptionType != null) {
            return Subscription.SubscriptionType.valueOf(subscriptionType);
         }
      } catch (Exception var2) {
      }

      return Subscription.SubscriptionType.NORMAL;
   }

   public enum SubscriptionType {
      NORMAL,
      RECURRING;
   }
}
