package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")
    );
    public static final int BACKWARDS = -1;
    public static final int FORWARDS = 1;
    public static final int DEFAULT_TEXT_COLOR = -2039584;
    public static final Style DEFAULT_HINT_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    public static final Style SEARCH_HINT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    private final Font font;
    private String value = "";
    private int maxLength = 32;
    private boolean bordered = true;
    private boolean canLoseFocus = true;
    private boolean isEditable = true;
    private boolean centered = false;
    private boolean textShadow = true;
    private boolean invertHighlightedTextColor = true;
    private int displayPos;
    private int cursorPos;
    private int highlightPos;
    private int textColor = -2039584;
    private int textColorUneditable = -9408400;
    private @Nullable String suggestion;
    private @Nullable Consumer<String> responder;
    private final List<EditBox.TextFormatter> formatters = new ArrayList<>();
    private @Nullable Component hint;
    private @Nullable IMEPreeditOverlay preeditOverlay;
    private long focusedTime = Util.getMillis();
    private int textX;
    private int textY;

    public EditBox(final Font font, final Component narration) {
        this(font, 150, 20, narration);
    }

    public EditBox(final Font font, final int width, final int height, final Component narration) {
        this(font, 0, 0, width, height, narration);
    }

    public EditBox(final Font font, final int x, final int y, final int width, final int height, final Component narration) {
        this(font, x, y, width, height, null, narration);
    }

    public EditBox(final Font font, final int x, final int y, final int width, final int height, final @Nullable EditBox oldBox, final Component narration) {
        super(x, y, width, height, narration);
        this.font = font;
        if (oldBox != null) {
            this.setValue(oldBox.getValue());
        }

        this.updateTextPosition();
    }

    public void setResponder(final Consumer<String> responder) {
        this.responder = responder;
    }

    public void addFormatter(final EditBox.TextFormatter formatter) {
        this.formatters.add(formatter);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        Component message = this.getMessage();
        return Component.translatable("gui.narrate.editBox", message, this.value);
    }

    public void setValue(final String value) {
        if (value.length() > this.maxLength) {
            this.value = value.substring(0, this.maxLength);
        } else {
            this.value = value;
        }

        this.moveCursorToEnd(false);
        this.setHighlightPos(this.cursorPos);
        this.onValueChange(value);
    }

    public String getValue() {
        return this.value;
    }

    public String getHighlighted() {
        int start = Math.min(this.cursorPos, this.highlightPos);
        int end = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(start, end);
    }

    @Override
    public void setX(final int x) {
        super.setX(x);
        this.updateTextPosition();
    }

    @Override
    public void setY(final int y) {
        super.setY(y);
        this.updateTextPosition();
    }

    public void insertText(final String input) {
        int start = Math.min(this.cursorPos, this.highlightPos);
        int end = Math.max(this.cursorPos, this.highlightPos);
        int maxInsertionLength = this.maxLength - this.value.length() - (start - end);
        if (maxInsertionLength > 0) {
            String text = StringUtil.filterText(input);
            int insertionLength = text.length();
            if (maxInsertionLength < insertionLength) {
                if (Character.isHighSurrogate(text.charAt(maxInsertionLength - 1))) {
                    maxInsertionLength--;
                }

                text = text.substring(0, maxInsertionLength);
                insertionLength = maxInsertionLength;
            }

            this.value = new StringBuilder(this.value).replace(start, end, text).toString();
            this.setCursorPosition(start + insertionLength);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(this.value);
        }
    }

    private void onValueChange(final String value) {
        if (this.responder != null) {
            this.responder.accept(value);
        }

        this.updateTextPosition();
    }

    private void deleteText(final int dir, final boolean wholeWord) {
        if (wholeWord) {
            this.deleteWords(dir);
        } else {
            this.deleteChars(dir);
        }
    }

    public void deleteWords(final int dir) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                this.deleteCharsToPos(this.getWordPosition(dir));
            }
        }
    }

    public void deleteChars(final int dir) {
        this.deleteCharsToPos(this.getCursorPos(dir));
    }

    public void deleteCharsToPos(final int pos) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                int start = Math.min(pos, this.cursorPos);
                int end = Math.max(pos, this.cursorPos);
                if (start != end) {
                    this.value = new StringBuilder(this.value).delete(start, end).toString();
                    this.setCursorPosition(start);
                    this.onValueChange(this.value);
                    this.moveCursorTo(start, false);
                }
            }
        }
    }

    public int getWordPosition(final int dir) {
        return this.getWordPosition(dir, this.getCursorPosition());
    }

    private int getWordPosition(final int dir, final int from) {
        return this.getWordPosition(dir, from, true);
    }

    private int getWordPosition(final int dir, final int from, final boolean stripSpaces) {
        int result = from;
        boolean reverse = dir < 0;
        int abs = Math.abs(dir);

        for (int i = 0; i < abs; i++) {
            if (!reverse) {
                int length = this.value.length();
                result = this.value.indexOf(32, result);
                if (result == -1) {
                    result = length;
                } else {
                    while (stripSpaces && result < length && this.value.charAt(result) == ' ') {
                        result++;
                    }
                }
            } else {
                while (stripSpaces && result > 0 && this.value.charAt(result - 1) == ' ') {
                    result--;
                }

                while (result > 0 && this.value.charAt(result - 1) != ' ') {
                    result--;
                }
            }
        }

        return result;
    }

    public void moveCursor(final int dir, final boolean hasShiftDown) {
        this.moveCursorTo(this.getCursorPos(dir), hasShiftDown);
    }

    private int getCursorPos(final int dir) {
        return Util.offsetByCodepoints(this.value, this.cursorPos, dir);
    }

    public void moveCursorTo(final int dir, final boolean extendSelection) {
        this.setCursorPosition(dir);
        if (!extendSelection) {
            this.setHighlightPos(this.cursorPos);
        }

        this.updateTextPosition();
    }

    public void setCursorPosition(final int pos) {
        this.cursorPos = Mth.clamp(pos, 0, this.value.length());
        this.scrollTo(this.cursorPos);
    }

    public void moveCursorToStart(final boolean hasShiftDown) {
        this.moveCursorTo(0, hasShiftDown);
    }

    public void moveCursorToEnd(final boolean hasShiftDown) {
        this.moveCursorTo(this.value.length(), hasShiftDown);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.isActive() && this.isFocused()) {
            switch (event.key()) {
                case 259:
                    if (this.isEditable) {
                        this.deleteText(-1, event.hasControlDownWithQuirk());
                    }

                    return true;
                case 260:
                case 264:
                case 265:
                case 266:
                case 267:
                default:
                    if (event.isSelectAll()) {
                        this.moveCursorToEnd(false);
                        this.setHighlightPos(0);
                        return true;
                    } else if (event.isCopy()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                        return true;
                    } else if (event.isPaste()) {
                        if (this.isEditable()) {
                            this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                        }

                        return true;
                    } else {
                        if (event.isCut()) {
                            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                            if (this.isEditable()) {
                                this.insertText("");
                            }

                            return true;
                        }

                        return false;
                    }
                case 261:
                    if (this.isEditable) {
                        this.deleteText(1, event.hasControlDownWithQuirk());
                    }

                    return true;
                case 262:
                    if (event.hasControlDownWithQuirk()) {
                        this.moveCursorTo(this.getWordPosition(1), event.hasShiftDown());
                    } else {
                        this.moveCursor(1, event.hasShiftDown());
                    }

                    return true;
                case 263:
                    if (event.hasControlDownWithQuirk()) {
                        this.moveCursorTo(this.getWordPosition(-1), event.hasShiftDown());
                    } else {
                        this.moveCursor(-1, event.hasShiftDown());
                    }

                    return true;
                case 268:
                    this.moveCursorToStart(event.hasShiftDown());
                    return true;
                case 269:
                    this.moveCursorToEnd(event.hasShiftDown());
                    return true;
            }
        } else {
            return false;
        }
    }

    public boolean canConsumeInput() {
        return this.isActive() && this.isFocused() && this.isEditable();
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        if (!this.canConsumeInput()) {
            return false;
        }

        if (event.isAllowedChatCharacter()) {
            if (this.isEditable) {
                this.insertText(event.codepointAsString());
            }

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

    private int findClickedPositionInText(final MouseButtonEvent event) {
        int positionInText = Math.min(Mth.floor(event.x()) - this.textX, this.getInnerWidth());
        String displayed = this.value.substring(this.displayPos);
        return this.displayPos + this.font.plainSubstrByWidth(displayed, positionInText).length();
    }

    private void selectWord(final MouseButtonEvent event) {
        int clickedPosition = this.findClickedPositionInText(event);
        int wordStart = this.getWordPosition(-1, clickedPosition);
        int wordEnd = this.getWordPosition(1, clickedPosition);
        this.moveCursorTo(wordStart, false);
        this.moveCursorTo(wordEnd, true);
    }

    @Override
    public void onClick(final MouseButtonEvent event, final boolean doubleClick) {
        if (doubleClick) {
            this.selectWord(event);
        } else {
            this.moveCursorTo(this.findClickedPositionInText(event), event.hasShiftDown());
        }
    }

    @Override
    protected void onDrag(final MouseButtonEvent event, final double dx, final double dy) {
        this.moveCursorTo(this.findClickedPositionInText(event), true);
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.isVisible()) {
            if (this.isBordered()) {
                Identifier sprite = SPRITES.get(this.isActive(), this.isFocused());
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            }

            int color = this.isEditable ? this.textColor : this.textColorUneditable;
            int relCursorPos = this.cursorPos - this.displayPos;
            String displayed = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            boolean cursorOnScreen = relCursorPos >= 0 && relCursorPos <= displayed.length();
            boolean showCursor = this.isFocused() && TextCursorUtils.isCursorVisible(Util.getMillis() - this.focusedTime) && cursorOnScreen;
            int drawX = this.textX;
            int relHighlightPos = Mth.clamp(this.highlightPos - this.displayPos, 0, displayed.length());
            if (!displayed.isEmpty()) {
                String half = cursorOnScreen ? displayed.substring(0, relCursorPos) : displayed;
                FormattedCharSequence charSequence = this.applyFormat(half, this.displayPos);
                graphics.text(this.font, charSequence, drawX, this.textY, color, this.textShadow);
                drawX += this.font.width(charSequence) + 1;
            }

            boolean insert = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
            int cursorX = drawX;
            if (!cursorOnScreen) {
                cursorX = relCursorPos > 0 ? this.textX + this.width : this.textX;
            } else if (insert) {
                cursorX--;
                drawX--;
            }

            if (!displayed.isEmpty() && cursorOnScreen && relCursorPos < displayed.length()) {
                graphics.text(this.font, this.applyFormat(displayed.substring(relCursorPos), this.cursorPos), drawX, this.textY, color, this.textShadow);
            }

            if (this.hint != null && displayed.isEmpty() && !this.isFocused()) {
                graphics.text(this.font, this.hint, drawX, this.textY, color);
            }

            if (!insert && this.suggestion != null) {
                graphics.text(this.font, this.suggestion, cursorX - 1, this.textY, -8355712, this.textShadow);
            }

            if (relHighlightPos != relCursorPos) {
                int highlightX = this.textX + this.font.width(displayed.substring(0, relHighlightPos));
                graphics.textHighlight(
                    Math.min(cursorX, this.getX() + this.width),
                    this.textY - 1,
                    Math.min(highlightX - 1, this.getX() + this.width),
                    this.textY + 1 + 9,
                    this.invertHighlightedTextColor
                );
            }

            if (showCursor) {
                if (insert) {
                    TextCursorUtils.extractInsertCursor(graphics, cursorX, this.textY, color, 9 + 1);
                } else {
                    TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, this.textY, color, this.textShadow);
                }
            }

            if (this.isHovered()) {
                graphics.requestCursor(this.isEditable() ? CursorTypes.IBEAM : CursorTypes.NOT_ALLOWED);
            }

            if (this.preeditOverlay != null) {
                this.preeditOverlay.updateInputPosition(cursorX, this.textY);
                graphics.setPreeditOverlay(this.preeditOverlay);
            }
        }
    }

    private FormattedCharSequence applyFormat(final String text, final int offset) {
        for (EditBox.TextFormatter formatter : this.formatters) {
            FormattedCharSequence formattedCharSequence = formatter.format(text, offset);
            if (formattedCharSequence != null) {
                return formattedCharSequence;
            }
        }

        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    private void updateTextPosition() {
        if (this.font != null) {
            String displayed = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
            this.textX = this.getX() + (this.isCentered() ? (this.getWidth() - this.font.width(displayed)) / 2 : (this.bordered ? 4 : 0));
            this.textY = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
        }
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
        if (this.value.length() > maxLength) {
            this.value = this.value.substring(0, maxLength);
            this.onValueChange(this.value);
        }
    }

    private int getMaxLength() {
        return this.maxLength;
    }

    public int getCursorPosition() {
        return this.cursorPos;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    public void setBordered(final boolean bordered) {
        this.bordered = bordered;
        this.updateTextPosition();
    }

    public void setTextColor(final int textColor) {
        this.textColor = textColor;
    }

    public void setTextColorUneditable(final int textColorUneditable) {
        this.textColorUneditable = textColorUneditable;
    }

    @Override
    public void setFocused(final boolean focused) {
        if (this.canLoseFocus || focused) {
            super.setFocused(focused);
            if (focused) {
                this.focusedTime = Util.getMillis();
            }

            if (this.isEditable()) {
                Minecraft.getInstance().onTextInputFocusChange(this, focused);
            }
        }
    }

    private boolean isEditable() {
        return this.isEditable;
    }

    public void setEditable(final boolean isEditable) {
        if (this.isFocused()) {
            Minecraft.getInstance().onTextInputFocusChange(this, isEditable);
        }

        this.isEditable = isEditable;
    }

    private boolean isCentered() {
        return this.centered;
    }

    public void setCentered(final boolean centered) {
        this.centered = centered;
        this.updateTextPosition();
    }

    public void setTextShadow(final boolean textShadow) {
        this.textShadow = textShadow;
    }

    public void setInvertHighlightedTextColor(final boolean invertHighlightedTextColor) {
        this.invertHighlightedTextColor = invertHighlightedTextColor;
    }

    public int getInnerWidth() {
        return this.isBordered() ? this.width - 8 : this.width;
    }

    public void setHighlightPos(final int pos) {
        this.highlightPos = Mth.clamp(pos, 0, this.value.length());
        this.scrollTo(this.highlightPos);
    }

    private void scrollTo(final int pos) {
        if (this.font != null) {
            this.displayPos = Math.min(this.displayPos, this.value.length());
            int innerWidth = this.getInnerWidth();
            String displayed = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), innerWidth);
            int lastPos = displayed.length() + this.displayPos;
            if (pos == this.displayPos) {
                this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, innerWidth, true).length();
            }

            if (pos > lastPos) {
                this.displayPos += pos - lastPos;
            } else if (pos <= this.displayPos) {
                this.displayPos = this.displayPos - (this.displayPos - pos);
            }

            this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
        }
    }

    public void setCanLoseFocus(final boolean canLoseFocus) {
        this.canLoseFocus = canLoseFocus;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public void setSuggestion(final @Nullable String suggestion) {
        this.suggestion = suggestion;
    }

    public int getScreenX(final int charIndex) {
        return charIndex > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, charIndex));
    }

    @Override
    public void updateWidgetNarration(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    public void setHint(final Component hint) {
        boolean hasNoStyle = hint.getStyle().equals(Style.EMPTY);
        this.hint = hasNoStyle ? hint.copy().withStyle(DEFAULT_HINT_STYLE) : hint;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface TextFormatter {
        @Nullable FormattedCharSequence format(final String text, final int offset);
    }
}