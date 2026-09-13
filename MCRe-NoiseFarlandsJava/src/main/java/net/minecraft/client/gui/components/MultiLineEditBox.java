package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MultiLineEditBox extends AbstractTextAreaWidget {
    private static final int CURSOR_COLOR = -3092272;
    private static final int PLACEHOLDER_TEXT_COLOR = ARGB.color(204, -2039584);
    private final Font font;
    private final Component placeholder;
    private final MultilineTextField textField;
    private final int textColor;
    private final boolean textShadow;
    private final int cursorColor;
    private @Nullable IMEPreeditOverlay preeditOverlay;
    private long focusedTime = Util.getMillis();

    private MultiLineEditBox(
        final Font font,
        final int x,
        final int y,
        final int width,
        final int height,
        final Component placeholder,
        final Component narration,
        final int textColor,
        final boolean textShadow,
        final int cursorColor,
        final boolean showBackground,
        final boolean showDecorations
    ) {
        super(x, y, width, height, narration, AbstractScrollArea.defaultSettings((int)(9.0 / 2.0)), showBackground, showDecorations);
        this.font = font;
        this.textShadow = textShadow;
        this.textColor = textColor;
        this.cursorColor = cursorColor;
        this.placeholder = placeholder;
        this.textField = new MultilineTextField(font, width - this.totalInnerPadding());
        this.textField.setCursorListener(this::scrollToCursor);
    }

    public void setCharacterLimit(final int characterLimit) {
        this.textField.setCharacterLimit(characterLimit);
    }

    public void setLineLimit(final int lineLimit) {
        this.textField.setLineLimit(lineLimit);
    }

    public void setValueListener(final Consumer<String> valueListener) {
        this.textField.setValueListener(valueListener);
    }

    public void setValue(final String value) {
        this.setValue(value, false);
    }

    public void setValue(final String value, final boolean allowOverflowLineLimit) {
        this.textField.setValue(value, allowOverflowLineLimit);
    }

    public String getValue() {
        return this.textField.value();
    }

    @Override
    public void updateWidgetNarration(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (doubleClick) {
            this.textField.selectWordAtCursor();
        } else {
            this.textField.setSelecting(event.hasShiftDown());
            this.seekCursorScreen(event.x(), event.y());
        }
    }

    @Override
    protected void onDrag(final MouseButtonEvent event, final double dx, final double dy) {
        this.textField.setSelecting(true);
        this.seekCursorScreen(event.x(), event.y());
        this.textField.setSelecting(event.hasShiftDown());
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        return this.textField.keyPressed(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (this.visible && this.isFocused() && event.isAllowedChatCharacter()) {
            this.textField.insertText(event.codepointAsString());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean preeditUpdated(final @Nullable PreeditEvent event) {
        this.preeditOverlay = event != null ? new IMEPreeditOverlay(event, this.font, 9 + 1) : null;
        return true;
    }

    @Override
    protected void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        String value = this.textField.value();
        if (value.isEmpty() && !this.isFocused()) {
            graphics.textWithWordWrap(
                this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), PLACEHOLDER_TEXT_COLOR
            );
        } else {
            int cursor = this.textField.cursor();
            boolean showCursor = this.isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - this.focusedTime);
            boolean needsValidCursorPos = this.preeditOverlay != null;
            boolean insertCursor = cursor < value.length();
            int cursorX = 0;
            int cursorY = 0;
            int drawTop = this.getInnerTop();
            int innerLeft = this.getInnerLeft();
            boolean hasDrawnCursor = false;

            for (MultilineTextField.StringView lineView : this.textField.iterateLines()) {
                boolean lineWithinVisibleBounds = this.withinContentAreaTopBottom(drawTop, drawTop + 9);
                if (!hasDrawnCursor && (needsValidCursorPos || showCursor) && insertCursor && cursor >= lineView.beginIndex() && cursor <= lineView.endIndex()) {
                    if (lineWithinVisibleBounds) {
                        String textBeforeCursor = value.substring(lineView.beginIndex(), cursor);
                        int textBeforeCursorPosRight = innerLeft + this.font.width(textBeforeCursor);
                        String textAfterCursor = value.substring(cursor, lineView.endIndex());
                        graphics.text(this.font, textBeforeCursor, innerLeft, drawTop, this.textColor, this.textShadow);
                        graphics.text(this.font, textAfterCursor, textBeforeCursorPosRight, drawTop, this.textColor, this.textShadow);
                        cursorX = textBeforeCursorPosRight;
                        cursorY = drawTop;
                        if (showCursor) {
                            TextCursorUtils.extractInsertCursor(graphics, cursorX, cursorY, this.cursorColor, 9 + 1);
                        }

                        hasDrawnCursor = true;
                    }
                } else if (lineWithinVisibleBounds) {
                    String substring = value.substring(lineView.beginIndex(), lineView.endIndex());
                    graphics.text(this.font, substring, innerLeft, drawTop, this.textColor, this.textShadow);
                    if ((needsValidCursorPos || showCursor) && !insertCursor) {
                        cursorX = innerLeft + this.font.width(substring);
                        cursorY = drawTop;
                    }
                }

                drawTop += 9;
            }

            if (showCursor && !insertCursor && this.withinContentAreaTopBottom(cursorY, cursorY + 9)) {
                TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, cursorY, this.cursorColor, this.textShadow);
            }

            if (this.textField.hasSelection()) {
                MultilineTextField.StringView selection = this.textField.getSelected();
                int drawX = this.getInnerLeft();
                drawTop = this.getInnerTop();

                for (MultilineTextField.StringView lineView : this.textField.iterateLines()) {
                    if (selection.beginIndex() > lineView.endIndex()) {
                        drawTop += 9;
                    } else {
                        if (lineView.beginIndex() > selection.endIndex()) {
                            break;
                        }

                        if (this.withinContentAreaTopBottom(drawTop, drawTop + 9)) {
                            int drawBegin = this.font.width(value.substring(lineView.beginIndex(), Math.max(selection.beginIndex(), lineView.beginIndex())));
                            int drawEnd;
                            if (selection.endIndex() > lineView.endIndex()) {
                                drawEnd = this.width - this.innerPadding();
                            } else {
                                drawEnd = this.font.width(value.substring(lineView.beginIndex(), selection.endIndex()));
                            }

                            graphics.textHighlight(drawX + drawBegin, drawTop, drawX + drawEnd, drawTop + 9, true);
                        }

                        drawTop += 9;
                    }
                }
            }

            if (this.isHovered()) {
                graphics.requestCursor(CursorTypes.IBEAM);
            }

            if (this.preeditOverlay != null) {
                this.preeditOverlay.updateInputPosition(cursorX, cursorY);
                graphics.setPreeditOverlay(this.preeditOverlay);
            }
        }
    }

    @Override
    protected void extractDecorations(final GuiGraphicsExtractor graphics) {
        super.extractDecorations(graphics);
        if (this.textField.hasCharacterLimit()) {
            int characterLimit = this.textField.characterLimit();
            Component countText = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), characterLimit);
            graphics.text(this.font, countText, this.getX() + this.width - this.font.width(countText), this.getY() + this.height + 4, -6250336);
        }
    }

    @Override
    public int getInnerHeight() {
        return 9 * this.textField.getLineCount();
    }

    private void scrollToCursor() {
        double scrollAmount = this.scrollAmount();
        MultilineTextField.StringView firstFullyVisibleLine = this.textField.getLineView((int)(scrollAmount / 9.0));
        if (this.textField.cursor() <= firstFullyVisibleLine.beginIndex()) {
            scrollAmount = this.textField.getLineAtCursor() * 9;
        } else {
            MultilineTextField.StringView lastFullyVisibleLine = this.textField.getLineView((int)((scrollAmount + this.height) / 9.0) - 1);
            if (this.textField.cursor() > lastFullyVisibleLine.endIndex()) {
                scrollAmount = this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding();
            }
        }

        this.setScrollAmount(scrollAmount);
    }

    private void seekCursorScreen(final double x, final double y) {
        double mouseX = x - this.getX() - this.innerPadding();
        double mouseY = y - this.getY() - this.innerPadding() + this.scrollAmount();
        this.textField.seekCursorToPoint(mouseX, mouseY);
    }

    @Override
    public void setFocused(final boolean focused) {
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }

        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    public static MultiLineEditBox.Builder builder() {
        return new MultiLineEditBox.Builder();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private int x;
        private int y;
        private Component placeholder = CommonComponents.EMPTY;
        private int textColor = -2039584;
        private boolean textShadow = true;
        private int cursorColor = -3092272;
        private boolean showBackground = true;
        private boolean showDecorations = true;

        public MultiLineEditBox.Builder setX(final int x) {
            this.x = x;
            return this;
        }

        public MultiLineEditBox.Builder setY(final int y) {
            this.y = y;
            return this;
        }

        public MultiLineEditBox.Builder setPlaceholder(final Component placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public MultiLineEditBox.Builder setTextColor(final int textColor) {
            this.textColor = textColor;
            return this;
        }

        public MultiLineEditBox.Builder setTextShadow(final boolean textShadow) {
            this.textShadow = textShadow;
            return this;
        }

        public MultiLineEditBox.Builder setCursorColor(final int cursorColor) {
            this.cursorColor = cursorColor;
            return this;
        }

        public MultiLineEditBox.Builder setShowBackground(final boolean showBackground) {
            this.showBackground = showBackground;
            return this;
        }

        public MultiLineEditBox.Builder setShowDecorations(final boolean showDecorations) {
            this.showDecorations = showDecorations;
            return this;
        }

        public MultiLineEditBox build(final Font font, final int width, final int height, final Component narration) {
            return new MultiLineEditBox(
                font,
                this.x,
                this.y,
                width,
                height,
                this.placeholder,
                narration,
                this.textColor,
                this.textShadow,
                this.cursorColor,
                this.showBackground,
                this.showDecorations
            );
        }
    }
}