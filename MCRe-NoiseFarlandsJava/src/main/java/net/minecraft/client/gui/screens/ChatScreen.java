package net.minecraft.client.gui.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.client.LocalCommandExecutor;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.multiplayer.RestrictionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
    public static final double MOUSE_SCROLL_SPEED = 7.0;
    private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
    private static final Component RESTRICTED_NARRATION_TEXT = Component.translatable("chat_screen.restricted.narration");
    public static final int USAGE_BACKGROUND_COLOR = -805306368;
    private final boolean closeOnSubmit;
    private String historyBuffer = "";
    private int historyPos = -1;
    protected EditBox input;
    protected String initial;
    protected boolean isDraft;
    private ChatComponent.DisplayMode displayMode = ChatComponent.DisplayMode.FOREGROUND;
    protected ChatScreen.ExitReason exitReason = ChatScreen.ExitReason.INTERRUPTED;
    private CommandSuggestions commandSuggestions;

    public ChatScreen(final String initial, final boolean isDraft) {
        this(initial, isDraft, true);
    }

    public ChatScreen(final String initial, final boolean isDraft, final boolean closeOnSubmit) {
        super(Component.translatable("chat_screen.title"));
        this.closeOnSubmit = closeOnSubmit;
        this.initial = initial;
        this.isDraft = isDraft;
    }

    @Override
    protected void init() {
        this.historyPos = this.minecraft.gui.hud.getChat().getRecentChat().size();
        this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.input.setMaxLength(256);
        this.input.setBordered(false);
        this.input.setValue(this.initial);
        this.input.setResponder(this::onEdited);
        this.input.addFormatter(this::formatChat);
        this.input.setCanLoseFocus(false);
        this.addRenderableWidget(this.input);
        this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestions.setAllowHiding(false);
        this.commandSuggestions.setAllowSuggestions(false);
        ChatAbilities chatAbilities = this.minecraft.player != null
            ? this.minecraft.player.chatAbilities()
            : ChatAbilities.NO_RESTRICTIONS;
        this.displayMode = chatAbilities.hasAnyRestrictions() ? ChatComponent.DisplayMode.FOREGROUND_RESTRICTED : ChatComponent.DisplayMode.FOREGROUND;
        this.commandSuggestions.setRestrictions(chatAbilities.canSendMessages(), chatAbilities.canSendCommands());
        this.commandSuggestions.updateCommandInfo();
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.input);
    }

    @Override
    public void resize(final int width, final int height) {
        this.initial = this.input.getValue();
        this.init(width, height);
    }

    @Override
    public void onClose() {
        this.exitReason = ChatScreen.ExitReason.INTENTIONAL;
        super.onClose();
    }

    @Override
    public void removed() {
        this.minecraft.gui.hud.getChat().resetChatScroll();
        this.initial = this.input.getValue();
        if (this.shouldDiscardDraft() || StringUtils.isBlank(this.initial)) {
            this.minecraft.gui.hud.getChat().discardDraft();
        } else if (!this.isDraft) {
            this.minecraft.gui.hud.getChat().saveAsDraft(this.initial);
        }
    }

    protected boolean shouldDiscardDraft() {
        return this.exitReason != ChatScreen.ExitReason.INTERRUPTED
            && (this.exitReason != ChatScreen.ExitReason.INTENTIONAL || !this.minecraft.options.saveChatDrafts().get());
    }

    private void onEdited(final String value) {
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.isDraft = false;
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        if (this.commandSuggestions.keyPressed(event)) {
            return true;
        }

        if (this.isDraft && event.key() == 259) {
            this.input.setValue("");
            this.isDraft = false;
            return true;
        }

        if (super.keyPressed(event)) {
            return true;
        }

        if (event.isConfirmation()) {
            if (!this.commandSuggestions.hasAllowedInput()) {
                return true;
            }

            this.handleChatInput(this.input.getValue(), true);
            // MCRe：主界面（player null，本地命令模式）Enter 不关闭聊天框，用 ESC 退出；
            // 世界内保持原版行为（Enter 提交并关闭）
            if (this.closeOnSubmit && this.minecraft.player != null) {
                this.exitReason = ChatScreen.ExitReason.DONE;
                this.minecraft.gui.setScreen(null);
            } else {
                this.input.setValue("");
                this.minecraft.gui.hud.getChat().resetChatScroll();
            }

            return true;
        } else {
            switch (event.key()) {
                case 264:
                    this.moveInHistory(1);
                    break;
                case 265:
                    this.moveInHistory(-1);
                    break;
                case 266:
                    this.minecraft.gui.hud.getChat().scrollChat(this.minecraft.gui.hud.getChat().getLinesPerPage() - 1);
                    break;
                case 267:
                    this.minecraft.gui.hud.getChat().scrollChat(-this.minecraft.gui.hud.getChat().getLinesPerPage() + 1);
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, double scrollY) {
        scrollY = Mth.clamp(scrollY, -1.0, 1.0);
        if (this.commandSuggestions.mouseScrolled(scrollY)) {
            return true;
        }

        if (!this.minecraft.hasShiftDown()) {
            scrollY *= 7.0;
        }

        this.minecraft.gui.hud.getChat().scrollChat((int)scrollY);
        return true;
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.commandSuggestions.mouseClicked(event)) {
            return true;
        }

        if (event.button() == 0) {
            int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
            ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.getFont(), (int)event.x(), (int)event.y())
                .includeInsertions(this.insertionClickMode());
            this.minecraft.gui.hud.getChat().captureClickableText(finder, screenHeight, this.minecraft.gui.hud.getGuiTicks(), this.displayMode);
            Style clicked = finder.result();
            if (clicked != null && this.handleComponentClicked(clicked, this.insertionClickMode())) {
                this.initial = this.input.getValue();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean insertionClickMode() {
        return this.minecraft.hasShiftDown();
    }

    private boolean handleComponentClicked(final Style clicked, final boolean allowInsertions) {
        ClickEvent event = clicked.getClickEvent();
        if (allowInsertions) {
            if (clicked.getInsertion() != null) {
                this.insertText(clicked.getInsertion(), false);
            }
        } else if (event != null) {
            switch (event) {
                case ClickEvent.Custom customEvent when customEvent.id().equals(ChatComponent.QUEUE_EXPAND_ID):
                    ChatListener chatListener = this.minecraft.gui.chatListener();
                    if (chatListener.queueSize() != 0L) {
                        chatListener.acceptNextDelayedMessage();
                    }
                    break;
                case ClickEvent.Custom customEvent when customEvent.id().equals(ChatComponent.GO_TO_RESTRICTIONS_SCREEN):
                    if (this.minecraft.player != null) {
                        this.minecraft.gui.setScreen(new RestrictionsScreen(this, this.minecraft.player.chatAbilities()));
                    }
                    break;
                default:
                    defaultHandleGameClickEvent(event, this.minecraft, this);
            }

            return true;
        }

        return false;
    }

    @Override
    public void insertText(final String text, final boolean replace) {
        if (replace) {
            this.input.setValue(text);
        } else {
            this.input.insertText(text);
        }
    }

    public void moveInHistory(final int dir) {
        int newPos = this.historyPos + dir;
        int max = this.minecraft.gui.hud.getChat().getRecentChat().size();
        newPos = Mth.clamp(newPos, 0, max);
        if (newPos != this.historyPos) {
            if (newPos == max) {
                this.historyPos = max;
                this.input.setValue(this.historyBuffer);
            } else {
                if (this.historyPos == max) {
                    this.historyBuffer = this.input.getValue();
                }

                this.input.setValue(this.minecraft.gui.hud.getChat().getRecentChat().get(newPos));
                this.commandSuggestions.setAllowSuggestions(false);
                this.historyPos = newPos;
            }
        }
    }

    private @Nullable FormattedCharSequence formatChat(final String text, final int offset) {
        return this.isDraft ? FormattedCharSequence.forward(text, Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true)) : null;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        // MCRe：聊天框透明化——不绘制输入框黑色背景
        this.minecraft
            .gui
            .hud
            .getChat()
            .extractRenderState(graphics, this.font, this.minecraft.gui.hud.getGuiTicks(), mouseX, mouseY, this.displayMode, this.insertionClickMode());
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.commandSuggestions.extractRenderState(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        // MCRe：主界面打开聊天——走原版主菜单标准背景路径（3D 滚动全景 + 模糊 + 暗色叠加层）
        // 修复：原实现只画暗色 MENU_BACKGROUND → 全景没人画 → 纯黑背景
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a);
            this.extractBlurredBackground(graphics);
            this.extractMenuBackground(graphics);
        }
        // 世界内：不绘制背景，保持透明看到世界
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    protected void updateNarrationState(final NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getTitle());
        if (this.displayMode.showRestrictedPrompt) {
            output.add(NarratedElementType.USAGE, CommonComponents.joinForNarration(USAGE_TEXT, RESTRICTED_NARRATION_TEXT));
        } else {
            output.add(NarratedElementType.USAGE, USAGE_TEXT);
        }

        String value = this.input.getValue();
        if (!value.isEmpty()) {
            output.nest().add(NarratedElementType.TITLE, Component.translatable("chat_screen.message", value));
        }
    }

    public void handleChatInput(String msg, final boolean addToRecent) {
        msg = this.normalizeChatMessage(msg);
        if (!msg.isEmpty()) {
            if (addToRecent) {
                this.minecraft.gui.hud.getChat().addRecentChat(msg);
            }

            if (this.minecraft.player != null && this.minecraft.player.connection != null) {
                if (msg.startsWith("/")) {
                    this.minecraft.player.connection.sendCommand(msg.substring(1));
                } else {
                    this.minecraft.player.connection.sendChat(msg);
                }
            } else {
                // MCRe：主界面/未连接世界 → 本地命令执行 / 本地消息显示
                if (msg.startsWith("/")) {
                    LocalCommandExecutor.execute(this.minecraft, msg.substring(1));
                } else {
                    this.minecraft.gui.hud.getChat().addClientSystemMessage(
                        Component.literal("§7[本地] " + msg)
                    );
                }
            }
        }
    }

    public String normalizeChatMessage(final String message) {
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(message.trim()));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface ChatConstructor<T extends ChatScreen> {
        T create(String initial, boolean isDraft);
    }

    @OnlyIn(Dist.CLIENT)
    protected enum ExitReason {
        INTENTIONAL,
        INTERRUPTED,
        DONE;
    }
}