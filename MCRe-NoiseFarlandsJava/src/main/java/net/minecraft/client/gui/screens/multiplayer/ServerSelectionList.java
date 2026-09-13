package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
    private static final Identifier INCOMPATIBLE_SPRITE = Identifier.withDefaultNamespace("server_list/incompatible");
    private static final Identifier UNREACHABLE_SPRITE = Identifier.withDefaultNamespace("server_list/unreachable");
    private static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("server_list/ping_1");
    private static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("server_list/ping_2");
    private static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("server_list/ping_3");
    private static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("server_list/ping_4");
    private static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("server_list/ping_5");
    private static final Identifier PINGING_1_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_1");
    private static final Identifier PINGING_2_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_2");
    private static final Identifier PINGING_3_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_3");
    private static final Identifier PINGING_4_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_4");
    private static final Identifier PINGING_5_SPRITE = Identifier.withDefaultNamespace("server_list/pinging_5");
    private static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/join_highlighted");
    private static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("server_list/join");
    private static final Identifier MOVE_UP_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_up_highlighted");
    private static final Identifier MOVE_UP_SPRITE = Identifier.withDefaultNamespace("server_list/move_up");
    private static final Identifier MOVE_DOWN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("server_list/move_down_highlighted");
    private static final Identifier MOVE_DOWN_SPRITE = Identifier.withDefaultNamespace("server_list/move_down");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(
        5,
        new ThreadFactoryBuilder()
            .setNameFormat("Server Pinger #%d")
            .setDaemon(true)
            .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
            .build()
    );
    private static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
    private static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
    private static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
    private static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
    private static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
    private static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
    private static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    private final JoinMultiplayerScreen screen;
    private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
    private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
    private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

    public ServerSelectionList(
        final JoinMultiplayerScreen screen, final Minecraft minecraft, final int width, final int height, final int y, final int itemHeight
    ) {
        super(minecraft, width, height, y, itemHeight);
        this.screen = screen;
    }

    private void refreshEntries() {
        ServerSelectionList.Entry previouslySelected = this.getSelected();
        List<ServerSelectionList.Entry> entriesToAdd = new ArrayList<>(this.onlineServers);
        entriesToAdd.add(this.lanHeader);
        entriesToAdd.addAll(this.networkServers);
        this.replaceEntries(entriesToAdd);
        if (previouslySelected != null) {
            for (ServerSelectionList.Entry entry : entriesToAdd) {
                if (entry.matches(previouslySelected)) {
                    this.setSelected(entry);
                    break;
                }
            }
        }
    }

    public void setSelected(final ServerSelectionList.@Nullable Entry selected) {
        super.setSelected(selected);
        this.screen.onSelectedChange();
    }

    public void updateOnlineServers(final ServerList servers) {
        this.onlineServers.clear();

        for (int i = 0; i < servers.size(); i++) {
            this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, servers.get(i)));
        }

        this.refreshEntries();
    }

    public void updateNetworkServers(final List<LanServer> servers) {
        int newServerCount = servers.size() - this.networkServers.size();
        this.networkServers.clear();

        for (LanServer server : servers) {
            this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, server));
        }

        this.refreshEntries();

        for (int i = this.networkServers.size() - newServerCount; i < this.networkServers.size(); i++) {
            ServerSelectionList.NetworkServerEntry newServer = this.networkServers.get(i);
            int entryIndex = i - this.networkServers.size() + this.children().size();
            int rowTop = this.getRowTop(entryIndex);
            int rowBottom = this.getRowBottom(entryIndex);
            if (rowBottom >= this.getY() && rowTop <= this.getBottom()) {
                this.minecraft.getNarrator().saySystemQueued(Component.translatable("multiplayer.lan.server_found", newServer.getServerNarration()));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return 305;
    }

    public void removed() {
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements AutoCloseable {
        @Override
        public void close() {
        }

        protected abstract boolean matches(final ServerSelectionList.Entry other);

        public abstract void join();
    }

    @OnlyIn(Dist.CLIENT)
    public static class LANHeader extends ServerSelectionList.Entry {
        private final Minecraft minecraft = Minecraft.getInstance();
        private final LoadingDotsWidget loadingDotsWidget = new LoadingDotsWidget(this.minecraft.font, ServerSelectionList.SCANNING_LABEL);

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            this.loadingDotsWidget
                .setPosition(this.getContentXMiddle() - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2, this.getContentY());
            this.loadingDotsWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public Component getNarration() {
            return ServerSelectionList.SCANNING_LABEL;
        }

        @Override
        protected boolean matches(final ServerSelectionList.Entry other) {
            return other instanceof ServerSelectionList.LANHeader;
        }

        @Override
        public void join() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NetworkServerEntry extends ServerSelectionList.Entry {
        private static final int ICON_WIDTH = 32;
        private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
        private final JoinMultiplayerScreen screen;
        protected final Minecraft minecraft;
        protected final LanServer serverData;

        protected NetworkServerEntry(final JoinMultiplayerScreen screen, final LanServer serverData) {
            this.screen = screen;
            this.serverData = serverData;
            this.minecraft = Minecraft.getInstance();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            graphics.text(this.minecraft.font, LAN_SERVER_HEADER, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
            graphics.text(this.minecraft.font, this.serverData.getMotd(), this.getContentX() + 32 + 3, this.getContentY() + 12, -8355712);
            if (this.minecraft.options.hideServerAddress) {
                graphics.text(this.minecraft.font, HIDDEN_ADDRESS_TEXT, this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
            } else {
                graphics.text(this.minecraft.font, this.serverData.getAddress(), this.getContentX() + 32 + 3, this.getContentY() + 12 + 11, -8355712);
            }
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            if (doubleClick) {
                this.join();
            }

            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (event.isSelection()) {
                this.join();
                return true;
            } else {
                return super.keyPressed(event);
            }
        }

        @Override
        public void join() {
            this.screen.join(new ServerData(this.serverData.getMotd(), this.serverData.getAddress(), ServerData.Type.LAN));
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.getServerNarration());
        }

        public Component getServerNarration() {
            return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
        }

        @Override
        protected boolean matches(final ServerSelectionList.Entry other) {
            return other instanceof ServerSelectionList.NetworkServerEntry networkServerEntry && networkServerEntry.serverData == this.serverData;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class OnlineServerEntry extends ServerSelectionList.Entry implements SelectableEntry {
        private static final int ICON_SIZE = 32;
        private static final int SPACING = 5;
        private static final int STATUS_ICON_WIDTH = 10;
        private static final int STATUS_ICON_HEIGHT = 8;
        private final JoinMultiplayerScreen screen;
        private final Minecraft minecraft;
        private final ServerData serverData;
        private final FaviconTexture icon;
        private byte @Nullable [] lastIconBytes;
        private @Nullable List<Component> onlinePlayersTooltip;
        private @Nullable Identifier statusIcon;
        private @Nullable Component statusIconTooltip;

        protected OnlineServerEntry(final JoinMultiplayerScreen screen, final ServerData serverData) {
            this.screen = screen;
            this.serverData = serverData;
            this.minecraft = Minecraft.getInstance();
            this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), serverData.ip);
            this.refreshStatus();
        }

        @Override
        public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            if (this.serverData.state() == ServerData.State.INITIAL) {
                this.serverData.setState(ServerData.State.PINGING);
                this.serverData.motd = CommonComponents.EMPTY;
                this.serverData.status = CommonComponents.EMPTY;
                ServerSelectionList.THREAD_POOL
                    .submit(
                        () -> {
                            try {
                                this.screen
                                    .getPinger()
                                    .pingServer(
                                        this.serverData,
                                        () -> this.minecraft.execute(this::updateServerList),
                                        () -> {
                                            this.serverData
                                                .setState(
                                                    this.serverData.protocol == SharedConstants.getCurrentVersion().protocolVersion()
                                                        ? ServerData.State.SUCCESSFUL
                                                        : ServerData.State.INCOMPATIBLE
                                                );
                                            this.minecraft.execute(this::refreshStatus);
                                        },
                                        EventLoopGroupHolder.remote(this.minecraft.options.useNativeTransport())
                                    );
                            } catch (UnknownHostException ignored) {
                                this.serverData.setState(ServerData.State.UNREACHABLE);
                                this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
                                this.minecraft.execute(this::refreshStatus);
                            } catch (Exception ignored) {
                                this.serverData.setState(ServerData.State.UNREACHABLE);
                                this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
                                this.minecraft.execute(this::refreshStatus);
                            }
                        }
                    );
            }

            graphics.text(this.minecraft.font, this.serverData.name, this.getContentX() + 32 + 3, this.getContentY() + 1, -1);
            List<FormattedCharSequence> lines = this.minecraft.font.split(this.serverData.motd, this.getContentWidth() - 32 - 2);

            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                graphics.text(this.minecraft.font, lines.get(i), this.getContentX() + 32 + 3, this.getContentY() + 12 + 9 * i, -8355712);
            }

            this.extractIcon(graphics, this.getContentX(), this.getContentY(), this.icon.textureLocation());
            int index = ServerSelectionList.this.children().indexOf(this);
            if (this.serverData.state() == ServerData.State.PINGING) {
                int iconIndex = (int)(Util.getMillis() / 100L + index * 2 & 7L);
                if (iconIndex > 4) {
                    iconIndex = 8 - iconIndex;
                }
                this.statusIcon = switch (iconIndex) {
                    case 1 -> ServerSelectionList.PINGING_2_SPRITE;
                    case 2 -> ServerSelectionList.PINGING_3_SPRITE;
                    case 3 -> ServerSelectionList.PINGING_4_SPRITE;
                    case 4 -> ServerSelectionList.PINGING_5_SPRITE;
                    default -> ServerSelectionList.PINGING_1_SPRITE;
                };
            }

            int statusIconX = this.getContentRight() - 10 - 5;
            if (this.statusIcon != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.statusIcon, statusIconX, this.getContentY(), 10, 8);
            }

            byte[] currentIconBytes = this.serverData.getIconBytes();
            if (!Arrays.equals(currentIconBytes, this.lastIconBytes)) {
                if (this.uploadServerIcon(currentIconBytes)) {
                    this.lastIconBytes = currentIconBytes;
                } else {
                    this.serverData.setIconBytes(null);
                    this.updateServerList();
                }
            }

            Component status = this.serverData.state() == ServerData.State.INCOMPATIBLE
                ? this.serverData.version.copy().withStyle(ChatFormatting.RED)
                : this.serverData.status;
            int statusWidth = this.minecraft.font.width(status);
            int statusX = statusIconX - statusWidth - 5;
            graphics.text(this.minecraft.font, status, statusX, this.getContentY() + 1, -8355712);
            if (this.statusIconTooltip != null
                && mouseX >= statusIconX
                && mouseX <= statusIconX + 10
                && mouseY >= this.getContentY()
                && mouseY <= this.getContentY() + 8) {
                graphics.setTooltipForNextFrame(this.statusIconTooltip, mouseX, mouseY);
            } else if (this.onlinePlayersTooltip != null
                && mouseX >= statusX
                && mouseX <= statusX + statusWidth
                && mouseY >= this.getContentY()
                && mouseY <= this.getContentY() - 1 + 9) {
                graphics.setTooltipForNextFrame(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText), mouseX, mouseY);
            }

            if (hovered) {
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                int relX = mouseX - this.getContentX();
                int relY = mouseY - this.getContentY();
                if (this.mouseOverRightHalf(relX, relY, 32)) {
                    graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED, ServerSelectionList.JOIN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                    );
                    ServerSelectionList.this.handleCursor(graphics);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.JOIN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                }

                if (index > 0) {
                    if (this.mouseOverTopLeftQuarter(relX, relY, 32)) {
                        graphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                        );
                        ServerSelectionList.this.handleCursor(graphics);
                    } else {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_UP_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                    }
                }

                if (index < this.screen.getServers().size() - 1) {
                    if (this.mouseOverBottomLeftQuarter(relX, relY, 32)) {
                        graphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, this.getContentX(), this.getContentY(), 32, 32
                        );
                        ServerSelectionList.this.handleCursor(graphics);
                    } else {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ServerSelectionList.MOVE_DOWN_SPRITE, this.getContentX(), this.getContentY(), 32, 32);
                    }
                }
            }
        }

        private void refreshStatus() {
            this.onlinePlayersTooltip = null;
            switch (this.serverData.state()) {
                case INITIAL:
                case PINGING:
                    this.statusIcon = ServerSelectionList.PING_1_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.PINGING_STATUS;
                    break;
                case INCOMPATIBLE:
                    this.statusIcon = ServerSelectionList.INCOMPATIBLE_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.INCOMPATIBLE_STATUS;
                    this.onlinePlayersTooltip = this.serverData.playerList;
                    break;
                case UNREACHABLE:
                    this.statusIcon = ServerSelectionList.UNREACHABLE_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.NO_CONNECTION_STATUS;
                    break;
                case SUCCESSFUL:
                    if (this.serverData.ping < 150L) {
                        this.statusIcon = ServerSelectionList.PING_5_SPRITE;
                    } else if (this.serverData.ping < 300L) {
                        this.statusIcon = ServerSelectionList.PING_4_SPRITE;
                    } else if (this.serverData.ping < 600L) {
                        this.statusIcon = ServerSelectionList.PING_3_SPRITE;
                    } else if (this.serverData.ping < 1000L) {
                        this.statusIcon = ServerSelectionList.PING_2_SPRITE;
                    } else {
                        this.statusIcon = ServerSelectionList.PING_1_SPRITE;
                    }

                    this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
                    this.onlinePlayersTooltip = this.serverData.playerList;
            }
        }

        public void updateServerList() {
            this.screen.getServers().save();
        }

        protected void extractIcon(final GuiGraphicsExtractor graphics, final int rowLeft, final int rowTop, final Identifier location) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, location, rowLeft, rowTop, 0.0F, 0.0F, 32, 32, 32, 32);
        }

        private boolean uploadServerIcon(final byte @Nullable [] serverIconBytes) {
            if (serverIconBytes == null) {
                this.icon.clear();
            } else {
                try {
                    this.icon.upload(NativeImage.read(serverIconBytes));
                } catch (Throwable t) {
                    ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", this.serverData.name, this.serverData.ip, t);
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (event.isSelection()) {
                this.join();
                return true;
            }

            if (event.hasShiftDown()) {
                ServerSelectionList list = this.screen.serverSelectionList;
                int currentIndex = list.children().indexOf(this);
                if (currentIndex == -1) {
                    return true;
                }

                if (event.isDown() && currentIndex < this.screen.getServers().size() - 1 || event.isUp() && currentIndex > 0) {
                    this.swap(currentIndex, event.isDown() ? currentIndex + 1 : currentIndex - 1);
                    return true;
                }
            }

            return super.keyPressed(event);
        }

        @Override
        public void join() {
            this.screen.join(this.serverData);
        }

        private void swap(final int currentIndex, final int newIndex) {
            this.screen.getServers().swap(currentIndex, newIndex);
            this.screen.serverSelectionList.swap(currentIndex, newIndex);
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
            int relX = (int)event.x() - this.getContentX();
            int relY = (int)event.y() - this.getContentY();
            if (this.mouseOverRightHalf(relX, relY, 32)) {
                this.join();
                return true;
            }

            int currentIndex = this.screen.serverSelectionList.children().indexOf(this);
            if (currentIndex > 0 && this.mouseOverTopLeftQuarter(relX, relY, 32)) {
                this.swap(currentIndex, currentIndex - 1);
                return true;
            }

            if (currentIndex < this.screen.getServers().size() - 1 && this.mouseOverBottomLeftQuarter(relX, relY, 32)) {
                this.swap(currentIndex, currentIndex + 1);
                return true;
            }

            if (doubleClick) {
                this.join();
            }

            return super.mouseClicked(event, doubleClick);
        }

        public ServerData getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            MutableComponent narrationComponent = Component.empty();
            narrationComponent.append(Component.translatable("narrator.select", this.serverData.name));
            narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
            switch (this.serverData.state()) {
                case PINGING:
                    narrationComponent.append(ServerSelectionList.PINGING_STATUS);
                    break;
                case INCOMPATIBLE:
                    narrationComponent.append(ServerSelectionList.INCOMPATIBLE_STATUS);
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    break;
                case UNREACHABLE:
                    narrationComponent.append(ServerSelectionList.NO_CONNECTION_STATUS);
                    break;
                default:
                    narrationComponent.append(ServerSelectionList.ONLINE_STATUS);
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
                    narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                    narrationComponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    if (this.serverData.players != null) {
                        narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                        narrationComponent.append(
                            Component.translatable("multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max())
                        );
                        narrationComponent.append(CommonComponents.NARRATION_SEPARATOR);
                        narrationComponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
                    }
            }

            return narrationComponent;
        }

        @Override
        public void close() {
            this.icon.close();
        }

        @Override
        protected boolean matches(final ServerSelectionList.Entry other) {
            return other instanceof ServerSelectionList.OnlineServerEntry onlineServerEntry && onlineServerEntry.serverData == this.serverData;
        }
    }
}