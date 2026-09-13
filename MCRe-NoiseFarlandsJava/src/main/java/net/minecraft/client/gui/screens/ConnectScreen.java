package net.minecraft.client.gui.screens;

import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.quickplay.QuickPlay;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ConnectScreen extends Screen {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long NARRATION_DELAY_MS = 2000L;
    public static final Component ABORT_CONNECTION = Component.translatable("connect.aborted");
    public static final Component UNKNOWN_HOST_MESSAGE = Component.translatable("disconnect.genericReason", Component.translatable("disconnect.unknownHost"));
    private volatile @Nullable Connection connection;
    private @Nullable ChannelFuture channelFuture;
    private volatile boolean aborted;
    private final Screen parent;
    private Component status = Component.translatable("connect.connecting");
    private long lastNarration = -1L;
    private final Component connectFailedTitle;

    private ConnectScreen(final Screen parent, final Component connectFailedTitle) {
        super(GameNarrator.NO_TITLE);
        this.parent = parent;
        this.connectFailedTitle = connectFailedTitle;
    }

    public static void startConnecting(
        final Screen parent,
        final Minecraft minecraft,
        final ServerAddress hostAndPort,
        final ServerData data,
        final boolean isQuickPlay,
        final @Nullable TransferState transferState
    ) {
        if (minecraft.gui.screen() instanceof ConnectScreen) {
            LOGGER.error("Attempt to connect while already connecting");
        } else {
            Component connectFailedTitle;
            if (transferState != null) {
                connectFailedTitle = CommonComponents.TRANSFER_CONNECT_FAILED;
            } else if (isQuickPlay) {
                connectFailedTitle = QuickPlay.ERROR_TITLE;
            } else {
                connectFailedTitle = CommonComponents.CONNECT_FAILED;
            }

            ConnectScreen screen = new ConnectScreen(parent, connectFailedTitle);
            if (transferState != null) {
                screen.updateStatus(Component.translatable("connect.transferring"));
            }

            minecraft.disconnectWithProgressScreen(false);
            minecraft.prepareForMultiplayer();
            minecraft.updateReportEnvironment(ReportEnvironment.thirdParty(data.ip));
            minecraft.quickPlayLog().setWorldData(QuickPlayLog.Type.MULTIPLAYER, data.ip, data.name);
            minecraft.gui.setScreen(screen);
            screen.connect(minecraft, hostAndPort, data, transferState);
        }
    }

    private void connect(final Minecraft minecraft, final ServerAddress hostAndPort, final ServerData server, final @Nullable TransferState transferState) {
        LOGGER.info("Connecting to {}, {}", hostAndPort.getHost(), hostAndPort.getPort());
        Thread thread = new Thread("Server Connector #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                InetSocketAddress address = null;

                try {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    Optional<InetSocketAddress> resolvedAddress = ServerNameResolver.DEFAULT
                        .resolveAddress(hostAndPort)
                        .map(ResolvedServerAddress::asInetSocketAddress);
                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    if (resolvedAddress.isEmpty()) {
                        minecraft.execute(
                            () -> minecraft.gui
                                .setScreen(
                                    new DisconnectedScreen(ConnectScreen.this.parent, ConnectScreen.this.connectFailedTitle, ConnectScreen.UNKNOWN_HOST_MESSAGE)
                                )
                        );
                        return;
                    }

                    address = resolvedAddress.get();
                    Connection pendingConnection;
                    synchronized (ConnectScreen.this) {
                        if (ConnectScreen.this.aborted) {
                            return;
                        }

                        pendingConnection = new Connection(PacketFlow.CLIENTBOUND);
                        pendingConnection.setBandwidthLogger(minecraft.getDebugOverlay().getBandwidthLogger());
                        ConnectScreen.this.channelFuture = Connection.connect(
                            address, EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()), pendingConnection
                        );
                    }

                    ConnectScreen.this.channelFuture.syncUninterruptibly();
                    synchronized (ConnectScreen.this) {
                        if (ConnectScreen.this.aborted) {
                            pendingConnection.disconnect(ConnectScreen.ABORT_CONNECTION);
                            return;
                        }

                        ConnectScreen.this.connection = pendingConnection;
                        minecraft.getDownloadedPackSource().configureForServerControl(pendingConnection, convertPackStatus(server.getResourcePackStatus()));
                    }

                    ConnectScreen.this.connection
                        .initiateServerboundPlayConnection(
                            address.getHostName(),
                            address.getPort(),
                            LoginProtocols.SERVERBOUND,
                            LoginProtocols.CLIENTBOUND,
                            new ClientHandshakePacketListenerImpl(
                                ConnectScreen.this.connection,
                                minecraft,
                                server,
                                ConnectScreen.this.parent,
                                false,
                                null,
                                ConnectScreen.this::updateStatus,
                                new LevelLoadTracker(),
                                transferState
                            ),
                            transferState != null
                        );
                    ConnectScreen.this.connection.send(new ServerboundHelloPacket(minecraft.getUser().getName(), minecraft.getUser().getProfileId()));
                } catch (Exception exception) {
                    if (ConnectScreen.this.aborted) {
                        return;
                    }

                    Exception cause;
                    if (exception.getCause() instanceof Exception originalCause) {
                        cause = originalCause;
                    } else {
                        cause = exception;
                    }

                    ConnectScreen.LOGGER.error("Couldn't connect to server", exception);
                    String message = address == null
                        ? cause.getMessage()
                        : cause.getMessage().replaceAll(address.getHostName() + ":" + address.getPort(), "").replaceAll(address.toString(), "");
                    minecraft.execute(
                        () -> minecraft.gui
                            .setScreen(
                                new DisconnectedScreen(
                                    ConnectScreen.this.parent,
                                    ConnectScreen.this.connectFailedTitle,
                                    Component.translatable("disconnect.genericReason", message)
                                )
                            )
                    );
                }
            }

            private static ServerPackManager.PackPromptStatus convertPackStatus(final ServerData.ServerPackStatus resourcePackStatus) {
                return switch (resourcePackStatus) {
                    case ENABLED -> ServerPackManager.PackPromptStatus.ALLOWED;
                    case DISABLED -> ServerPackManager.PackPromptStatus.DECLINED;
                    case PROMPT -> ServerPackManager.PackPromptStatus.PENDING;
                };
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    private void updateStatus(final Component status) {
        this.status = status;
    }

    @Override
    public void tick() {
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                this.connection.tick();
            } else {
                this.connection.handleDisconnection();
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            synchronized (this) {
                this.aborted = true;
                if (this.channelFuture != null) {
                    this.channelFuture.cancel(true);
                    this.channelFuture = null;
                }

                if (this.connection != null) {
                    this.connection.disconnect(ABORT_CONNECTION);
                }
            }

            this.minecraft.gui.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20).build());
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        long current = Util.getMillis();
        if (current - this.lastNarration > 2000L) {
            this.lastNarration = current;
            this.minecraft.getNarrator().saySystemNow(Component.translatable("narrator.joining"));
        }

        graphics.centeredText(this.font, this.status, this.width / 2, this.height / 2 - 50, -1);
    }
}