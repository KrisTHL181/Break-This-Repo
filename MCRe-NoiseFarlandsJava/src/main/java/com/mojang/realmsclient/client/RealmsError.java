package com.mojang.realmsclient.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.exception.RealmsHttpException;
import java.util.Locale;
import net.minecraft.locale.Language;
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

    static RealmsError parse(final int httpCode, final String payload) {
        if (httpCode == 429) {
            return RealmsError.CustomError.SERVICE_BUSY;
        }

        if (Strings.isNullOrEmpty(payload)) {
            return RealmsError.CustomError.noPayload(httpCode);
        }

        try {
            JsonObject object = LenientJsonParser.parse(payload).getAsJsonObject();
            String errorReason = GsonHelper.getAsString(object, "reason", null);
            String errorMessage = GsonHelper.getAsString(object, "errorMsg", null);
            int errorCode = GsonHelper.getAsInt(object, "errorCode", -1);
            if (errorMessage != null || errorReason != null || errorCode != -1) {
                return new RealmsError.ErrorWithJsonPayload(httpCode, errorCode != -1 ? errorCode : httpCode, errorReason, errorMessage);
            }
        } catch (Exception e) {
            LOGGER.error("Could not parse RealmsError", e);
        }

        return new RealmsError.ErrorWithRawPayload(httpCode, payload);
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

        public static RealmsError.CustomError unknownCompatibilityResponse(final String response) {
            return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.unknownCompatibility", response));
        }

        public static RealmsError.CustomError configurationError() {
            return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.configurationError"));
        }

        public static RealmsError.CustomError connectivityError(final RealmsHttpException exception) {
            return new RealmsError.CustomError(500, Component.translatable("mco.errorMessage.realmsService.connectivity", exception.getMessage()));
        }

        public static RealmsError.CustomError retry(final int statusCode) {
            return new RealmsError.CustomError(statusCode, RETRY_MESSAGE);
        }

        public static RealmsError.CustomError noPayload(final int statusCode) {
            return new RealmsError.CustomError(statusCode, null);
        }

        public static RealmsError.CustomError htmlPayload(final int statusCode, final String payload) {
            int bodyStart = payload.indexOf("<body>");
            int bodyEnd = payload.indexOf("</body>");
            if (bodyStart >= 0 && bodyEnd > bodyStart) {
                return new RealmsError.CustomError(statusCode, Component.literal(payload.substring(bodyStart + "<body>".length(), bodyEnd).trim()));
            }

            LOGGER.error("Got an error with an unreadable html body {}", payload);
            return new RealmsError.CustomError(statusCode, null);
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
            String codeTranslationKey = "mco.errorMessage." + this.code;
            if (Language.getInstance().has(codeTranslationKey)) {
                return Component.translatable(codeTranslationKey);
            }

            if (this.reason != null) {
                String reasonTranslationKey = "mco.errorReason." + this.reason;
                if (Language.getInstance().has(reasonTranslationKey)) {
                    return Component.translatable(reasonTranslationKey);
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