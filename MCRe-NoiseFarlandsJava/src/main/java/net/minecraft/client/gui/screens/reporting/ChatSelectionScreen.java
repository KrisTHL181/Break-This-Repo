package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
    private static final Identifier CHECKMARK_SPRITE = Identifier.withDefaultNamespace("icon/checkmark");
    private static final Component TITLE = Component.translatable("gui.chatSelection.title");
    private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
    private final @Nullable Screen lastScreen;
    private final ReportingContext reportingContext;
    private Button confirmSelectedButton;
    private MultiLineLabel contextInfoLabel;
    private ChatSelectionScreen.@Nullable ChatSelectionList chatSelectionList;
    private final ChatReport.Builder report;
    private final Consumer<ChatReport.Builder> onSelected;
    private ChatSelectionLogFiller chatLogFiller;

    public ChatSelectionScreen(
        final @Nullable Screen lastScreen,
        final ReportingContext reportingContext,
        final ChatReport.Builder report,
        final Consumer<ChatReport.Builder> onSelected
    ) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.reportingContext = reportingContext;
        this.report = report.copy();
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatSelectionList = this.addRenderableWidget(
            new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9)
        );
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build());
        this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            this.onSelected.accept(this.report);
            this.onClose();
        }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
        this.updateConfirmSelectedButton();
        this.extendLog();
        this.chatSelectionList.setScrollAmount(this.chatSelectionList.maxScrollAmount());
    }

    private boolean canReport(final LoggedChatMessage message) {
        return message.canReport(this.report.reportedProfileId());
    }

    private void extendLog() {
        int pageSize = this.chatSelectionList.getMaxVisibleEntries();
        this.chatLogFiller.fillNextPage(pageSize, this.chatSelectionList);
    }

    private void onReachedScrollTop() {
        this.extendLog();
    }

    private void updateConfirmSelectedButton() {
        this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        graphics.centeredText(this.font, this.title, this.width / 2, 10, -1);
        AbuseReportLimits reportLimits = this.reportingContext.sender().reportLimits();
        int messageCount = this.report.reportedMessages().size();
        int maxMessageCount = reportLimits.maxReportedMessageCount();
        Component selectedText = Component.translatable("gui.chatSelection.selected", messageCount, maxMessageCount);
        graphics.centeredText(this.font, selectedText, this.width / 2, 26, -1);
        int topY = this.chatSelectionList.getFooterTop();
        this.contextInfoLabel.visitLines(TextAlignment.CENTER, this.width / 2, topY, 9, textRenderer);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    @OnlyIn(Dist.CLIENT)
    public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
        public static final int ITEM_HEIGHT = 16;
        private ChatSelectionScreen.ChatSelectionList.@Nullable Heading previousHeading;

        public ChatSelectionList(final Minecraft minecraft, final int upperMargin) {
            super(minecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - upperMargin - 80, 40, 16);
        }

        @Override
        public void setScrollAmount(final double scrollAmount) {
            double prevScrollAmount = this.scrollAmount();
            super.setScrollAmount(scrollAmount);
            if (this.maxScrollAmount() > 1.0E-5F && scrollAmount <= 1.0E-5F && !Mth.equal(scrollAmount, prevScrollAmount)) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public void acceptMessage(final int id, final LoggedChatMessage.Player message) {
            boolean canReport = message.canReport(ChatSelectionScreen.this.report.reportedProfileId());
            ChatTrustLevel trustLevel = message.trustLevel();
            GuiMessageTag tag = trustLevel.createTag(message.message());
            ChatSelectionScreen.ChatSelectionList.Entry entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
                id, message.toContentComponent(), message.toNarrationComponent(), tag, canReport, true
            );
            this.addEntryToTop(entry);
            this.updateHeading(message, canReport);
        }

        private void updateHeading(final LoggedChatMessage.Player message, final boolean canReport) {
            ChatSelectionScreen.ChatSelectionList.Entry entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
                message.profile(), message.toHeadingComponent(), canReport
            );
            this.addEntryToTop(entry);
            ChatSelectionScreen.ChatSelectionList.Heading heading = new ChatSelectionScreen.ChatSelectionList.Heading(message.profileId(), entry);
            if (this.previousHeading != null && this.previousHeading.canCombine(heading)) {
                this.removeEntryFromTop(this.previousHeading.entry());
            }

            this.previousHeading = heading;
        }

        @Override
        public void acceptDivider(final Component text) {
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(text));
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.previousHeading = null;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        public int getMaxVisibleEntries() {
            return Mth.positiveCeilDiv(this.height, 16);
        }

        protected void extractItem(
            final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a, final ChatSelectionScreen.ChatSelectionList.Entry entry
        ) {
            if (this.shouldHighlightEntry(entry)) {
                boolean selected = this.getSelected() == entry;
                int outlineColor = this.isFocused() && selected ? -1 : -8355712;
                this.extractSelection(graphics, entry, outlineColor);
            }

            entry.extractContent(graphics, mouseX, mouseY, this.getHovered() == entry, a);
        }

        private boolean shouldHighlightEntry(final ChatSelectionScreen.ChatSelectionList.Entry entry) {
            if (entry.canSelect()) {
                boolean entrySelected = this.getSelected() == entry;
                boolean nothingSelected = this.getSelected() == null;
                boolean entryHovered = this.getHovered() == entry;
                return entrySelected || nothingSelected && entryHovered && entry.canReport();
            } else {
                return false;
            }
        }

        protected ChatSelectionScreen.ChatSelectionList.@Nullable Entry nextEntry(final ScreenDirection dir) {
            return this.nextEntry(dir, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
        }

        public void setSelected(final ChatSelectionScreen.ChatSelectionList.@Nullable Entry selected) {
            super.setSelected(selected);
            ChatSelectionScreen.ChatSelectionList.Entry entry = this.nextEntry(ScreenDirection.UP);
            if (entry == null) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            ChatSelectionScreen.ChatSelectionList.Entry selected = this.getSelected();
            return selected != null && selected.keyPressed(event) ? true : super.keyPressed(event);
        }

        public int getFooterTop() {
            return this.getBottom() + 9;
        }

        @OnlyIn(Dist.CLIENT)
        public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private final Component text;

            public DividerEntry(final Component text) {
                this.text = text;
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                int centerY = this.getContentYMiddle();
                int rowRight = this.getContentRight() - 8;
                int textWidth = ChatSelectionScreen.this.font.width(this.text);
                int textLeft = (this.getContentX() + rowRight - textWidth) / 2;
                int textTop = centerY - 9 / 2;
                graphics.text(ChatSelectionScreen.this.font, this.text, textLeft, textTop, -6250336);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }

        @OnlyIn(Dist.CLIENT)
        public abstract static class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
            @Override
            public Component getNarration() {
                return CommonComponents.EMPTY;
            }

            public boolean isSelected() {
                return false;
            }

            public boolean canSelect() {
                return false;
            }

            public boolean canReport() {
                return this.canSelect();
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                return this.canSelect();
            }
        }

        @OnlyIn(Dist.CLIENT)
        private record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
            public boolean canCombine(final ChatSelectionScreen.ChatSelectionList.Heading other) {
                return other.sender.equals(this.sender);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private static final int TAG_MARGIN_LEFT = 4;
            private final int chatId;
            private final FormattedText text;
            private final Component narration;
            private final @Nullable List<FormattedCharSequence> hoverText;
            private final GuiMessageTag.@Nullable Icon tagIcon;
            private final @Nullable List<FormattedCharSequence> tagHoverText;
            private final boolean canReport;
            private final boolean playerMessage;

            public MessageEntry(
                final int chatId,
                final Component text,
                final Component narration,
                final @Nullable GuiMessageTag tag,
                final boolean canReport,
                final boolean playerMessage
            ) {
                this.chatId = chatId;
                this.tagIcon = Optionull.map(tag, GuiMessageTag::icon);
                this.tagHoverText = tag != null && tag.text() != null
                    ? ChatSelectionScreen.this.font.split(tag.text(), ChatSelectionList.this.getRowWidth())
                    : null;
                this.canReport = canReport;
                this.playerMessage = playerMessage;
                FormattedText shortText = ChatSelectionScreen.this.font
                    .substrByWidth(text, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
                if (text != shortText) {
                    this.text = FormattedText.composite(shortText, CommonComponents.ELLIPSIS);
                    this.hoverText = ChatSelectionScreen.this.font.split(text, ChatSelectionList.this.getRowWidth());
                } else {
                    this.text = text;
                    this.hoverText = null;
                }

                this.narration = narration;
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                if (this.isSelected() && this.canReport) {
                    this.extractSelectedCheckmark(graphics, this.getContentY(), this.getContentX(), this.getContentHeight());
                }

                int textX = this.getContentX() + this.getTextIndent();
                int textY = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
                graphics.text(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), textX, textY, this.canReport ? -1 : -1593835521);
                if (this.hoverText != null && hovered) {
                    graphics.setTooltipForNextFrame(this.hoverText, mouseX, mouseY);
                }

                int textWidth = ChatSelectionScreen.this.font.width(this.text);
                this.extractTag(graphics, textX + textWidth + 4, this.getContentY(), this.getContentHeight(), mouseX, mouseY);
            }

            private void extractTag(
                final GuiGraphicsExtractor graphics, final int iconLeft, final int rowTop, final int rowHeight, final int mouseX, final int mouseY
            ) {
                if (this.tagIcon != null) {
                    int iconTop = rowTop + (rowHeight - this.tagIcon.height) / 2;
                    this.tagIcon.extractRenderState(graphics, iconLeft, iconTop);
                    if (this.tagHoverText != null
                        && mouseX >= iconLeft
                        && mouseX <= iconLeft + this.tagIcon.width
                        && mouseY >= iconTop
                        && mouseY <= iconTop + this.tagIcon.height) {
                        graphics.setTooltipForNextFrame(this.tagHoverText, mouseX, mouseY);
                    }
                }
            }

            private void extractSelectedCheckmark(final GuiGraphicsExtractor graphics, final int rowTop, final int rowLeft, final int rowHeight) {
                int left = rowLeft;
                int top = rowTop + (rowHeight - 8) / 2;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ChatSelectionScreen.CHECKMARK_SPRITE, left, top, 9, 8);
            }

            private int getMaximumTextWidth() {
                int tagMargin = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
                return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - tagMargin;
            }

            private int getTextIndent() {
                return this.playerMessage ? 11 : 0;
            }

            @Override
            public Component getNarration() {
                return this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration;
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                ChatSelectionList.this.setSelected((ChatSelectionScreen.ChatSelectionList.Entry)null);
                return this.toggleReport();
            }

            @Override
            public boolean keyPressed(final KeyEvent event) {
                return event.isSelection() ? this.toggleReport() : false;
            }

            @Override
            public boolean isSelected() {
                return ChatSelectionScreen.this.report.isReported(this.chatId);
            }

            @Override
            public boolean canSelect() {
                return true;
            }

            @Override
            public boolean canReport() {
                return this.canReport;
            }

            private boolean toggleReport() {
                if (this.canReport) {
                    ChatSelectionScreen.this.report.toggleReported(this.chatId);
                    ChatSelectionScreen.this.updateConfirmSelectedButton();
                    return true;
                } else {
                    return false;
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int FACE_SIZE = 12;
            private static final int PADDING = 4;
            private final Component heading;
            private final Supplier<PlayerSkin> skin;
            private final boolean canReport;

            public MessageHeadingEntry(final GameProfile profile, final Component heading, final boolean canReport) {
                this.heading = heading;
                this.canReport = canReport;
                this.skin = ChatSelectionList.this.minecraft.getSkinManager().createLookup(profile, true);
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                int faceX = this.getContentX() - 12 + 4;
                int faceY = this.getContentY() + (this.getContentHeight() - 12) / 2;
                PlayerFaceExtractor.extractRenderState(graphics, this.skin.get(), faceX, faceY, 12);
                int textY = this.getContentY() + 1 + (this.getContentHeight() - 9) / 2;
                graphics.text(ChatSelectionScreen.this.font, this.heading, faceX + 12 + 4, textY, this.canReport ? -1 : -1593835521);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public static class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            }
        }
    }
}