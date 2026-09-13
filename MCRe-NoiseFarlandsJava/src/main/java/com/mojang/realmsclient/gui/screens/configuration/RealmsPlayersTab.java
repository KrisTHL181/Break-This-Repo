package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.screens.RealmsConfirmScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPlayersTab extends GridLayoutTab implements RealmsConfigurationTab {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component TITLE = Component.translatable("mco.configure.world.players.title");
    private static final Component QUESTION_TITLE = Component.translatable("mco.question");
    private static final int PADDING = 8;
    private final RealmsConfigureWorldScreen configurationScreen;
    private final Minecraft minecraft;
    private final Font font;
    private RealmsServer serverData;
    private final RealmsPlayersTab.InvitedObjectSelectionList invitedList;

    public RealmsPlayersTab(final RealmsConfigureWorldScreen configurationScreen, final Minecraft minecraft, final RealmsServer serverData) {
        super(TITLE);
        this.configurationScreen = configurationScreen;
        this.minecraft = minecraft;
        this.font = configurationScreen.getFont();
        this.serverData = serverData;
        GridLayout.RowHelper helper = this.layout.spacing(8).createRowHelper(1);
        this.invitedList = helper.addChild(
            new RealmsPlayersTab.InvitedObjectSelectionList(configurationScreen.width, this.calculateListHeight()),
            LayoutSettings.defaults().alignVerticallyTop().alignHorizontallyCenter()
        );
        helper.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.invite"),
                    var3 -> minecraft.gui.setScreen(new RealmsInviteScreen(configurationScreen, serverData))
                )
                .build(),
            LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter()
        );
        this.updateData(serverData);
    }

    public int calculateListHeight() {
        return this.configurationScreen.getContentHeight() - 20 - 16;
    }

    @Override
    public void doLayout(final ScreenRectangle screenRectangle) {
        super.doLayout(screenRectangle);
        this.invitedList.updateSizeAndPosition(this.configurationScreen.width, this.calculateListHeight(), this.invitedList.getX(), this.invitedList.getY());
    }

    @Override
    public void updateData(final RealmsServer serverData) {
        this.serverData = serverData;
        this.invitedList.updateList(serverData);
    }

    @OnlyIn(Dist.CLIENT)
    private abstract static class Entry extends ContainerObjectSelectionList.Entry<RealmsPlayersTab.Entry> {
    }

    @OnlyIn(Dist.CLIENT)
    private class HeaderEntry extends RealmsPlayersTab.Entry {
        private String cachedNumberOfInvites = "";
        private final FocusableTextWidget invitedWidget;

        public HeaderEntry() {
            Component invitedText = Component.translatable("mco.configure.world.invited.number", "").withStyle(ChatFormatting.UNDERLINE);
            this.invitedWidget = FocusableTextWidget.builder(invitedText, RealmsPlayersTab.this.font)
                .alwaysShowBorder(false)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                .build();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            String numberOfInvites = RealmsPlayersTab.this.serverData.players != null ? Integer.toString(RealmsPlayersTab.this.serverData.players.size()) : "0";
            if (!numberOfInvites.equals(this.cachedNumberOfInvites)) {
                this.cachedNumberOfInvites = numberOfInvites;
                Component invitedComponent = Component.translatable("mco.configure.world.invited.number", numberOfInvites).withStyle(ChatFormatting.UNDERLINE);
                this.invitedWidget.setMessage(invitedComponent);
            }

            this.invitedWidget
                .setPosition(
                    RealmsPlayersTab.this.invitedList.getRowLeft() + RealmsPlayersTab.this.invitedList.getRowWidth() / 2 - this.invitedWidget.getWidth() / 2,
                    this.getY() + this.getHeight() / 2 - this.invitedWidget.getHeight() / 2
                );
            this.invitedWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        private int height(final int lineHeight) {
            return lineHeight + this.invitedWidget.getPadding() * 2;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.invitedWidget);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.invitedWidget);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class InvitedObjectSelectionList extends ContainerObjectSelectionList<RealmsPlayersTab.Entry> {
        private static final int PLAYER_ENTRY_HEIGHT = 36;

        public InvitedObjectSelectionList(final int width, final int height) {
            super(Minecraft.getInstance(), width, height, RealmsPlayersTab.this.configurationScreen.getHeaderHeight(), 36);
        }

        private void updateList(final RealmsServer serverData) {
            this.clearEntries();
            this.populateList(serverData);
        }

        private void populateList(final RealmsServer serverData) {
            RealmsPlayersTab.HeaderEntry entry = RealmsPlayersTab.this.new HeaderEntry();
            this.addEntry(entry, entry.height(9));

            for (RealmsPlayersTab.PlayerEntry newChild : serverData.players.stream().map(x$0 -> RealmsPlayersTab.this.new PlayerEntry(x$0)).toList()) {
                this.addEntry(newChild);
            }
        }

        @Override
        protected void extractListBackground(final GuiGraphicsExtractor graphics) {
        }

        @Override
        protected void extractListSeparators(final GuiGraphicsExtractor graphics) {
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class PlayerEntry extends RealmsPlayersTab.Entry {
        protected static final int SKIN_FACE_SIZE = 32;
        private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
        private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
        private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
        private static final Identifier MAKE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/make_operator");
        private static final Identifier REMOVE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/remove_operator");
        private static final Identifier REMOVE_PLAYER_SPRITE = Identifier.withDefaultNamespace("player_list/remove_player");
        private static final int ICON_WIDTH = 8;
        private static final int ICON_HEIGHT = 7;
        private final PlayerInfo playerInfo;
        private final Button removeButton;
        private final Button makeOpButton;
        private final Button removeOpButton;

        public PlayerEntry(final PlayerInfo playerInfo) {
            this.playerInfo = playerInfo;
            int index = RealmsPlayersTab.this.serverData.players.indexOf(this.playerInfo);
            this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, button -> this.op(index), false)
                .sprite(MAKE_OP_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(NORMAL_USER_TEXT))
                .narration(
                    defaultNarrationSupplier -> CommonComponents.joinForNarration(
                        Component.translatable("mco.invited.player.narration", playerInfo.name),
                        defaultNarrationSupplier.get(),
                        Component.translatable("narration.cycle_button.usage.focused", OP_TEXT)
                    )
                )
                .build();
            this.removeOpButton = SpriteIconButton.builder(OP_TEXT, button -> this.deop(index), false)
                .sprite(REMOVE_OP_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(OP_TEXT))
                .narration(
                    defaultNarrationSupplier -> CommonComponents.joinForNarration(
                        Component.translatable("mco.invited.player.narration", playerInfo.name),
                        defaultNarrationSupplier.get(),
                        Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT)
                    )
                )
                .build();
            this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, button -> this.uninvite(index), false)
                .sprite(REMOVE_PLAYER_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(REMOVE_TEXT))
                .narration(
                    defaultNarrationSupplier -> CommonComponents.joinForNarration(
                        Component.translatable("mco.invited.player.narration", playerInfo.name), defaultNarrationSupplier.get()
                    )
                )
                .build();
            this.updateOpButtons();
        }

        private void op(final int index) {
            UUID selectedInvite = RealmsPlayersTab.this.serverData.players.get(index).uuid;
            RealmsUtil.<Ops>supplyAsync(
                    client -> client.op(RealmsPlayersTab.this.serverData.id, selectedInvite), e -> RealmsPlayersTab.LOGGER.error("Couldn't op the user", e)
                )
                .thenAcceptAsync(ops -> {
                    this.updateOps(ops);
                    this.updateOpButtons();
                    this.setFocused(this.removeOpButton);
                }, RealmsPlayersTab.this.minecraft);
        }

        private void deop(final int index) {
            UUID selectedInvite = RealmsPlayersTab.this.serverData.players.get(index).uuid;
            RealmsUtil.<Ops>supplyAsync(
                    client -> client.deop(RealmsPlayersTab.this.serverData.id, selectedInvite), e -> RealmsPlayersTab.LOGGER.error("Couldn't deop the user", e)
                )
                .thenAcceptAsync(ops -> {
                    this.updateOps(ops);
                    this.updateOpButtons();
                    this.setFocused(this.makeOpButton);
                }, RealmsPlayersTab.this.minecraft);
        }

        private void uninvite(final int index) {
            if (index >= 0 && index < RealmsPlayersTab.this.serverData.players.size()) {
                PlayerInfo playerInfo = RealmsPlayersTab.this.serverData.players.get(index);
                RealmsConfirmScreen confirmScreen = new RealmsConfirmScreen(
                    result -> {
                        if (result) {
                            RealmsUtil.runAsync(
                                client -> client.uninvite(RealmsPlayersTab.this.serverData.id, playerInfo.uuid),
                                e -> RealmsPlayersTab.LOGGER.error("Couldn't uninvite user", e)
                            );
                            RealmsPlayersTab.this.serverData.players.remove(index);
                            RealmsPlayersTab.this.updateData(RealmsPlayersTab.this.serverData);
                        }

                        RealmsPlayersTab.this.minecraft.gui.setScreen(RealmsPlayersTab.this.configurationScreen);
                    },
                    RealmsPlayersTab.QUESTION_TITLE,
                    Component.translatable("mco.configure.world.uninvite.player", playerInfo.name)
                );
                RealmsPlayersTab.this.minecraft.gui.setScreen(confirmScreen);
            }
        }

        private void updateOps(final Ops ops) {
            for (PlayerInfo playerInfo : RealmsPlayersTab.this.serverData.players) {
                playerInfo.operator = ops.ops().contains(playerInfo.name);
            }
        }

        private void updateOpButtons() {
            this.makeOpButton.visible = !this.playerInfo.operator;
            this.removeOpButton.visible = !this.makeOpButton.visible;
        }

        private Button activeOpButton() {
            return this.makeOpButton.visible ? this.makeOpButton : this.removeOpButton;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.activeOpButton(), this.removeButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.activeOpButton(), this.removeButton);
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            int inviteColor;
            if (!this.playerInfo.accepted) {
                inviteColor = -6250336;
            } else if (this.playerInfo.online) {
                inviteColor = -16711936;
            } else {
                inviteColor = -1;
            }

            int skinYPos = this.getContentYMiddle() - 16;
            RealmsUtil.extractPlayerFace(graphics, this.getContentX(), skinYPos, 32, this.playerInfo.uuid);
            int textYPos = this.getContentYMiddle() - 9 / 2;
            graphics.text(RealmsPlayersTab.this.font, this.playerInfo.name, this.getContentX() + 8 + 32, textYPos, inviteColor);
            int iconYPos = this.getContentYMiddle() - 10;
            int removeButtonXPos = this.getContentRight() - this.removeButton.getWidth();
            this.removeButton.setPosition(removeButtonXPos, iconYPos);
            this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
            int opButtonXPos = removeButtonXPos - this.activeOpButton().getWidth() - 8;
            this.makeOpButton.setPosition(opButtonXPos, iconYPos);
            this.makeOpButton.extractRenderState(graphics, mouseX, mouseY, a);
            this.removeOpButton.setPosition(opButtonXPos, iconYPos);
            this.removeOpButton.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }
}