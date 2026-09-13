package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PopupScreen extends Screen {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("popup/background");
    private static final int SPACING = 12;
    private static final int BG_BORDER_WITH_SPACING = 18;
    private static final int BUTTON_SPACING = 6;
    private static final int IMAGE_SIZE_X = 130;
    private static final int IMAGE_SIZE_Y = 64;
    private static final int POPUP_DEFAULT_WIDTH = 250;
    private final @Nullable Screen backgroundScreen;
    private final @Nullable Identifier image;
    private final List<Component> messages;
    private final List<PopupScreen.ButtonOption> buttons;
    private final @Nullable Runnable onClose;
    private final int contentWidth;
    private final LinearLayout layout = LinearLayout.vertical();

    private PopupScreen(
        final @Nullable Screen backgroundScreen,
        final int backgroundWidth,
        final @Nullable Identifier image,
        final Component title,
        final List<Component> messages,
        final List<PopupScreen.ButtonOption> buttons,
        final @Nullable Runnable onClose
    ) {
        super(title);
        this.backgroundScreen = backgroundScreen;
        this.image = image;
        this.messages = messages;
        this.buttons = buttons;
        this.onClose = onClose;
        this.contentWidth = backgroundWidth - 36;
    }

    @Override
    public void added() {
        super.added();
        if (this.backgroundScreen != null) {
            this.backgroundScreen.clearFocus();
        }
    }

    @Override
    protected void init() {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.init(this.width, this.height);
        }

        this.layout.spacing(12).defaultCellSetting().alignHorizontallyCenter();
        this.layout
            .addChild(new MultiLineTextWidget(this.title.copy().withStyle(ChatFormatting.BOLD), this.font).setMaxWidth(this.contentWidth).setCentered(true));
        if (this.image != null) {
            this.layout.addChild(ImageWidget.texture(130, 64, this.image, 130, 64));
        }

        this.messages.forEach(message -> this.layout.addChild(new MultiLineTextWidget(message, this.font).setMaxWidth(this.contentWidth).setCentered(true)));
        this.layout.addChild(this.buildButtonRow());
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    private LinearLayout buildButtonRow() {
        int totalSpacing = 6 * (this.buttons.size() - 1);
        int buttonWidth = Math.min((this.contentWidth - totalSpacing) / this.buttons.size(), 150);
        LinearLayout row = LinearLayout.horizontal();
        row.spacing(6);

        for (PopupScreen.ButtonOption button : this.buttons) {
            row.addChild(Button.builder(button.message(), b -> button.action().accept(this)).width(buttonWidth).build());
        }

        return row;
    }

    @Override
    protected void repositionElements() {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.resize(this.width, this.height);
        }

        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.backgroundScreen != null) {
            this.backgroundScreen.extractBackground(graphics, mouseX, mouseY, a);
            graphics.nextStratum();
            this.backgroundScreen.extractRenderState(graphics, -1, -1, a);
            graphics.nextStratum();
            this.extractTransparentBackground(graphics);
        } else {
            super.extractBackground(graphics, mouseX, mouseY, a);
        }

        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND_SPRITE,
            this.layout.getX() - 18,
            this.layout.getY() - 18,
            this.layout.getWidth() + 36,
            this.layout.getHeight() + 36
        );
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.title, CommonComponents.joinLines(this.messages));
    }

    @Override
    public void onClose() {
        if (this.onClose != null) {
            this.onClose.run();
        }

        this.minecraft.gui.setScreen(this.backgroundScreen);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final @Nullable Screen backgroundScreen;
        private final Component title;
        private final List<Component> messages = new ArrayList<>();
        private int width = 250;
        private @Nullable Identifier image;
        private final List<PopupScreen.ButtonOption> buttons = new ArrayList<>();
        private @Nullable Runnable onClose = null;

        public Builder(final @Nullable Screen backgroundScreen, final Component title) {
            this.backgroundScreen = backgroundScreen;
            this.title = title;
        }

        public PopupScreen.Builder setWidth(final int width) {
            this.width = width;
            return this;
        }

        public PopupScreen.Builder setImage(final Identifier image) {
            this.image = image;
            return this;
        }

        public PopupScreen.Builder addMessage(final Component message) {
            this.messages.add(message);
            return this;
        }

        public PopupScreen.Builder addButton(final Component message, final Consumer<PopupScreen> action) {
            this.buttons.add(new PopupScreen.ButtonOption(message, action));
            return this;
        }

        public PopupScreen.Builder onClose(final Runnable onClose) {
            this.onClose = onClose;
            return this;
        }

        public PopupScreen build() {
            if (this.buttons.isEmpty()) {
                throw new IllegalStateException("Popup must have at least one button");
            } else {
                return new PopupScreen(this.backgroundScreen, this.width, this.image, this.title, this.messages, List.copyOf(this.buttons), this.onClose);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record ButtonOption(Component message, Consumer<PopupScreen> action) {
    }
}