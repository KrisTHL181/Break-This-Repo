package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.time.Instant;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record Subscription(Instant startDate, int daysLeft, Subscription.SubscriptionType type) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static Subscription parse(String p_87673_) {
      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87673_).getAsJsonObject();
         return new Subscription(
            JsonUtils.getDateOr("startDate", jsonobject),
            JsonUtils.getIntOr("daysLeft", jsonobject, 0),
            typeFrom(JsonUtils.getStringOr("subscriptionType", jsonobject, null))
         );
      } catch (Exception exception) {
         LOGGER.error("Could not parse Subscription", exception);
         return new Subscription(Instant.EPOCH, 0, Subscription.SubscriptionType.NORMAL);
      }
   }

   private static Subscription.SubscriptionType typeFrom(@Nullable String p_87675_) {
      try {
         if (p_87675_ != null) {
            return Subscription.SubscriptionType.valueOf(p_87675_);
         }
      } catch (Exception var2) {
      }

      return Subscription.SubscriptionType.NORMAL;
   }

   @OnlyIn(Dist.CLIENT)
   public enum SubscriptionType {
      NORMAL,
      RECURRING;
   }
}
