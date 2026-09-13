package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.util.Locale;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public interface RealmsError {
   Component NO_MESSAGE = Component.translatable("mco.errorMessage.noDetails");
   Logger LOGGER = LogUtils.getLogger();

   int errorCode();

   Component errorMessage();

   String logMessage();

   static RealmsError parse(int p_298954_, String p_87304_) {
      if (p_298954_ == 429) {
         return RealmsError.CustomError.SERVICE_BUSY;
      }

      if (Strings.isNullOrEmpty(p_87304_)) {
         return RealmsError.CustomError.noPayload(p_298954_);
      }

      try {
         JsonObject jsonobject = LenientJsonParser.parse(p_87304_).getAsJsonObject();
         String s = GsonHelper.getAsString(jsonobject, "reason", null);
         String s1 = GsonHelper.getAsString(jsonobject, "errorMsg", null);
         int i = GsonHelper.getAsInt(jsonobject, "errorCode", -1);
         if (s1 != null || s != null || i != -1) {
            return new RealmsError.ErrorWithJsonPayload(p_298954_, i != -1 ? i : p_298954_, s, s1);
         }
      } catch (Exception exception) {
         LOGGER.error("Could not parse RealmsError", exception);
      }

      return new RealmsError.ErrorWithRawPayload(p_298954_, p_87304_);
   }

   @OnlyIn(Dist.CLIENT)
   record AuthenticationError(String message) implements RealmsError {
      public static final int ERROR_CODE = 401;

      @Override
      public int errorCode() {
         return 401;
      }

      @Override
      public Component errorMessage() {
         return Component.literal(this.message);
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms authentication error with message '%s'", this.message);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record CustomError(int httpCode, @Nullable Component payload) implements RealmsError {
      public static final RealmsError.CustomError SERVICE_BUSY = new RealmsError.CustomError(429, Component.translatable("mco.errorMessage.serviceBusy"));
      public static final Component RETRY_MESSAGE = Component.translatable("mco.errorMessage.retry");
      public static final String BODY_TAG = "<body>";
      public static final String CLOSING_BODY_TAG = "</body>";

      public static RealmsError.CustomError unknownCompatibilityResponse(String p_300024_) {
         return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.unknownCompatibility", p_300024_));
      }

      public static RealmsError.CustomError configurationError() {
         return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.configurationError"));
      }

      public static RealmsError.CustomError connectivityError(RealmsHttpException p_298467_) {
         return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.connectivity", p_298467_.getMessage()));
      }

      public static RealmsError.CustomError retry(int p_297862_) {
         return new RealmsError.CustomError(p_297862_, RETRY_MESSAGE);
      }

      public static RealmsError.CustomError noPayload(int p_298598_) {
         return new RealmsError.CustomError(p_298598_, null);
      }

      public static RealmsError.CustomError htmlPayload(int p_409016_, String p_406229_) {
         int i = p_406229_.indexOf("<body>");
         int j = p_406229_.indexOf("</body>");
         if (i >= 0 && j > i) {
            return new RealmsError.CustomError(p_409016_, Component.literal(p_406229_.substring(i + "<body>".length(), j).trim()));
         }

         LOGGER.error("Got an error with an unreadable html body {}", p_406229_);
         return new RealmsError.CustomError(p_409016_, null);
      }

      @Override
      public int errorCode() {
         return this.httpCode;
      }

      @Override
      public Component errorMessage() {
         return this.payload != null ? this.payload : NO_MESSAGE;
      }

      @Override
      public String logMessage() {
         return this.payload != null
            ? String.format(Locale.ROOT, "Realms service error (%d) with message '%s'", this.httpCode, this.payload.getString())
            : String.format(Locale.ROOT, "Realms service error (%d) with no payload", this.httpCode);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ErrorWithJsonPayload(int httpCode, int code, @Nullable String reason, @Nullable String message) implements RealmsError {
      @Override
      public int errorCode() {
         return this.code;
      }

      @Override
      public Component errorMessage() {
         String s = "mco.errorMessage." + this.code;
         if (I18n.exists(s)) {
            return Component.translatable(s);
         }

         if (this.reason != null) {
            String s1 = "mco.errorReason." + this.reason;
            if (I18n.exists(s1)) {
               return Component.translatable(s1);
            }
         }

         return this.message != null ? Component.literal(this.message) : NO_MESSAGE;
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms service error (%d/%d/%s) with message '%s'", this.httpCode, this.code, this.reason, this.message);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ErrorWithRawPayload(int httpCode, String payload) implements RealmsError {
      @Override
      public int errorCode() {
         return this.httpCode;
      }

      @Override
      public Component errorMessage() {
         return Component.literal(this.payload);
      }

      @Override
      public String logMessage() {
         return String.format(Locale.ROOT, "Realms service error (%d) with raw payload '%s'", this.httpCode, this.payload);
      }
   }
}
