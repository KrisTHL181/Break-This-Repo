package com.mojang.realmsclient.dto;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsNotification {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NOTIFICATION_UUID = "notificationUuid";
    private static final String DISMISSABLE = "dismissable";
    private static final String SEEN = "seen";
    private static final String TYPE = "type";
    private static final String VISIT_URL = "visitUrl";
    private static final String INFO_POPUP = "infoPopup";
    private static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
    private final UUID uuid;
    private final boolean dismissable;
    private final boolean seen;
    private final String type;

    private RealmsNotification(final UUID uuid, final boolean dismissable, final boolean seen, final String type) {
        this.uuid = uuid;
        this.dismissable = dismissable;
        this.seen = seen;
        this.type = type;
    }

    public boolean seen() {
        return this.seen;
    }

    public boolean dismissable() {
        return this.dismissable;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public static List<RealmsNotification> parseList(final String json) {
        List<RealmsNotification> result = new ArrayList<>();

        try {
            for (JsonElement element : LenientJsonParser.parse(json).getAsJsonObject().get("notifications").getAsJsonArray()) {
                result.add(parse(element.getAsJsonObject()));
            }
        } catch (Exception e) {
            LOGGER.error("Could not parse list of RealmsNotifications", e);
        }

        return result;
    }

    private static RealmsNotification parse(final JsonObject jsonObject) {
        UUID uuid = JsonUtils.getUuidOr("notificationUuid", jsonObject, null);
        if (uuid == null) {
            throw new IllegalStateException("Missing required property notificationUuid");
        }

        boolean dismissable = JsonUtils.getBooleanOr("dismissable", jsonObject, true);
        boolean seen = JsonUtils.getBooleanOr("seen", jsonObject, false);
        String type = JsonUtils.getRequiredString("type", jsonObject);
        RealmsNotification base = new RealmsNotification(uuid, dismissable, seen, type);

        return switch (type) {
            case "visitUrl" -> RealmsNotification.VisitUrl.parse(base, jsonObject);
            case "infoPopup" -> RealmsNotification.InfoPopup.parse(base, jsonObject);
            default -> base;
        };
    }

    @OnlyIn(Dist.CLIENT)
    public static class InfoPopup extends RealmsNotification {
        private static final String TITLE = "title";
        private static final String MESSAGE = "message";
        private static final String IMAGE = "image";
        private static final String URL_BUTTON = "urlButton";
        private final RealmsText title;
        private final RealmsText message;
        private final Identifier image;
        private final RealmsNotification.@Nullable UrlButton urlButton;

        private InfoPopup(
            final RealmsNotification base,
            final RealmsText title,
            final RealmsText message,
            final Identifier image,
            final RealmsNotification.@Nullable UrlButton urlButton
        ) {
            super(base.uuid, base.dismissable, base.seen, base.type);
            this.title = title;
            this.message = message;
            this.image = image;
            this.urlButton = urlButton;
        }

        public static RealmsNotification.InfoPopup parse(final RealmsNotification base, final JsonObject object) {
            RealmsText title = JsonUtils.getRequired("title", object, RealmsText::parse);
            RealmsText message = JsonUtils.getRequired("message", object, RealmsText::parse);
            Identifier image = Identifier.parse(JsonUtils.getRequiredString("image", object));
            RealmsNotification.UrlButton urlButton = JsonUtils.getOptional("urlButton", object, RealmsNotification.UrlButton::parse);
            return new RealmsNotification.InfoPopup(base, title, message, image, urlButton);
        }

        public @Nullable PopupScreen buildScreen(final Screen parentScreen, final Consumer<UUID> dismiss) {
            Component title = this.title.createComponent();
            if (title == null) {
                RealmsNotification.LOGGER.warn("Realms info popup had title with no available translation: {}", this.title);
                return null;
            }

            PopupScreen.Builder builder = new PopupScreen.Builder(parentScreen, title)
                .setImage(this.image)
                .addMessage(this.message.createComponent(CommonComponents.EMPTY));
            if (this.urlButton != null) {
                builder.addButton(this.urlButton.urlText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK), popup -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.gui.setScreen(new ConfirmLinkScreen(result -> {
                        if (result) {
                            Util.getPlatform().openUri(this.urlButton.url);
                            minecraft.gui.setScreen(parentScreen);
                        } else {
                            minecraft.gui.setScreen(popup);
                        }
                    }, this.urlButton.url, true));
                    dismiss.accept(this.uuid());
                });
            }

            builder.addButton(CommonComponents.GUI_OK, popup -> {
                popup.onClose();
                dismiss.accept(this.uuid());
            });
            builder.onClose(() -> dismiss.accept(this.uuid()));
            return builder.build();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record UrlButton(String url, RealmsText urlText) {
        private static final String URL = "url";
        private static final String URL_TEXT = "urlText";

        public static RealmsNotification.UrlButton parse(final JsonObject jsonObject) {
            String url = JsonUtils.getRequiredString("url", jsonObject);
            RealmsText urlText = JsonUtils.getRequired("urlText", jsonObject, RealmsText::parse);
            return new RealmsNotification.UrlButton(url, urlText);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class VisitUrl extends RealmsNotification {
        private static final String URL = "url";
        private static final String BUTTON_TEXT = "buttonText";
        private static final String MESSAGE = "message";
        private final String url;
        private final RealmsText buttonText;
        private final RealmsText message;

        private VisitUrl(final RealmsNotification base, final String url, final RealmsText buttonText, final RealmsText message) {
            super(base.uuid, base.dismissable, base.seen, base.type);
            this.url = url;
            this.buttonText = buttonText;
            this.message = message;
        }

        public static RealmsNotification.VisitUrl parse(final RealmsNotification base, final JsonObject jsonObject) {
            String url = JsonUtils.getRequiredString("url", jsonObject);
            RealmsText buttonText = JsonUtils.getRequired("buttonText", jsonObject, RealmsText::parse);
            RealmsText message = JsonUtils.getRequired("message", jsonObject, RealmsText::parse);
            return new RealmsNotification.VisitUrl(base, url, buttonText, message);
        }

        public Component getMessage() {
            return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
        }

        public Button buildOpenLinkButton(final Screen parentScreen) {
            Component buttonLabel = this.buttonText.createComponent(RealmsNotification.BUTTON_TEXT_FALLBACK);
            return Button.builder(buttonLabel, ConfirmLinkScreen.confirmLink(parentScreen, this.url)).build();
        }
    }
}