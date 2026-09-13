package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RateKickingConnection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerConnectionListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer server;
    public volatile boolean running;
    private volatile @Nullable UUID sessionId;
    private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
    private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

    public ServerConnectionListener(final MinecraftServer server) {
        this.server = server;
        this.running = true;
    }

    public void startTcpServerListener(final @Nullable InetAddress address, final int port) throws IOException {
        synchronized (this.channels) {
            EventLoopGroupHolder eventLoopGroupHolder = EventLoopGroupHolder.remote(this.server.useNativeTransport());
            this.channels
                .add(
                    new ServerBootstrap()
                        .channel(eventLoopGroupHolder.serverChannelCls())
                        .childHandler(
                            new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(final Channel channel) {
                                    try {
                                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
                                    } catch (ChannelException var5) {
                                    }

                                    ChannelPipeline pipeline = channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                                    if (ServerConnectionListener.this.server.repliesToStatus()) {
                                        pipeline.addLast("legacy_query", new LegacyQueryHandler(ServerConnectionListener.this.getServer()));
                                    }

                                    Connection.configureSerialization(pipeline, PacketFlow.SERVERBOUND, false, null);
                                    int rateLimitPacketsPerSecond = ServerConnectionListener.this.server.getRateLimitPacketsPerSecond();
                                    Connection connection = rateLimitPacketsPerSecond > 0
                                        ? new RateKickingConnection(rateLimitPacketsPerSecond)
                                        : new Connection(PacketFlow.SERVERBOUND);
                                    ServerConnectionListener.this.connections.add(connection);
                                    connection.configurePacketHandler(pipeline);
                                    connection.setListenerForServerboundHandshake(
                                        new ServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, connection)
                                    );
                                }
                            }
                        )
                        .group(eventLoopGroupHolder.eventLoopGroup())
                        .localAddress(address, port)
                        .bind()
                        .syncUninterruptibly()
                );
        }
    }

    public SocketAddress startMemoryChannel() {
        ChannelFuture newChannel;
        synchronized (this.channels) {
            newChannel = new ServerBootstrap()
                .channel(EventLoopGroupHolder.local().serverChannelCls())
                .childHandler(
                    new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(final Channel channel) {
                            Connection connection = new Connection(PacketFlow.SERVERBOUND);
                            connection.setListenerForServerboundHandshake(
                                new MemoryServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, connection)
                            );
                            ServerConnectionListener.this.connections.add(connection);
                            ChannelPipeline pipeline = channel.pipeline();
                            Connection.configureInMemoryPipeline(pipeline, PacketFlow.SERVERBOUND);
                            if (SharedConstants.DEBUG_FAKE_LATENCY_MS > 0) {
                                pipeline.addLast(
                                    "latency",
                                    new ServerConnectionListener.LatencySimulator(SharedConstants.DEBUG_FAKE_LATENCY_MS, SharedConstants.DEBUG_FAKE_JITTER_MS)
                                );
                            }

                            connection.configurePacketHandler(pipeline);
                        }
                    }
                )
                .group(EventLoopGroupHolder.local().eventLoopGroup())
                .localAddress(LocalAddress.ANY)
                .bind()
                .syncUninterruptibly();
            this.channels.add(newChannel);
        }

        return newChannel.channel().localAddress();
    }

    public void acceptChannel(final Channel channel, final UUID profileId) {
        channel.pipeline()
            .addLast(
                new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        int rateLimitPacketsPerSecond = ServerConnectionListener.this.server.getRateLimitPacketsPerSecond();
                        Connection connection = rateLimitPacketsPerSecond > 0
                            ? new RateKickingConnection(rateLimitPacketsPerSecond)
                            : new Connection(PacketFlow.SERVERBOUND);
                        ChannelPipeline pipeline = ch.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                        Connection.configureSerialization(pipeline, PacketFlow.SERVERBOUND, false, null);
                        connection.configurePacketHandler(pipeline);
                        connection.setListenerForServerboundHandshake(new ServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, connection));
                        connection.setIntendedProfileId(profileId);
                        ServerConnectionListener.this.connections.add(connection);
                    }
                }
            );
        EventLoopGroupHolder.local().eventLoopGroup().register(channel).syncUninterruptibly();
    }

    public void stop() {
        this.running = false;

        for (ChannelFuture channel : this.channels) {
            try {
                channel.channel().close().sync();
            } catch (InterruptedException ignored) {
                LOGGER.error("Interrupted whilst closing channel");
            }
        }
    }

    public void stopTcpServerListener() {
        synchronized (this.channels) {
            Iterator<ChannelFuture> iterator = this.channels.iterator();

            while (iterator.hasNext()) {
                ChannelFuture future = iterator.next();
                if (!(future.channel() instanceof LocalServerChannel)) {
                    try {
                        future.channel().close().sync();
                    } catch (InterruptedException ignored) {
                        LOGGER.error("Interrupted whilst closing TCP listener");
                    }

                    iterator.remove();
                }
            }
        }
    }

    public void tick() {
        synchronized (this.connections) {
            Iterator<Connection> iterator = this.connections.iterator();

            while (iterator.hasNext()) {
                Connection connection = iterator.next();
                if (!connection.isConnecting()) {
                    if (connection.isConnected()) {
                        try {
                            connection.tick();
                        } catch (Exception e) {
                            if (connection.isMemoryConnection()) {
                                throw new ReportedException(CrashReport.forThrowable(e, "Ticking memory connection"));
                            }

                            LOGGER.warn("Failed to handle packet for {}", connection.getLoggableAddress(this.server.logIPs()), e);
                            Component component = Component.literal("Internal server error");
                            connection.send(new ClientboundDisconnectPacket(component), PacketSendListener.thenRun(() -> connection.disconnect(component)));
                            connection.setReadOnly();
                        }
                    } else {
                        iterator.remove();
                        connection.handleDisconnection();
                    }
                }
            }

            if (this.connections.isEmpty()) {
                synchronized (this) {
                    this.sessionId = null;
                }
            }
        }
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public List<Connection> getConnections() {
        return this.connections;
    }

    public UUID getSessionId() {
        UUID uuid = this.sessionId;
        if (uuid != null) {
            return uuid;
        }

        synchronized (this) {
            uuid = this.sessionId;
            if (uuid == null) {
                uuid = UUID.randomUUID();
                this.sessionId = uuid;
            }

            return uuid;
        }
    }

    private static class LatencySimulator extends ChannelInboundHandlerAdapter {
        private static final Timer TIMER = new HashedWheelTimer(new ThreadFactoryBuilder().setNameFormat("Latency Simulator #%d").setDaemon(true).build());
        private final int delay;
        private final int jitter;
        private final List<ServerConnectionListener.LatencySimulator.DelayedMessage> queuedMessages = Lists.newArrayList();

        public LatencySimulator(final int delay, final int jitter) {
            this.delay = delay;
            this.jitter = jitter;
        }

        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
            this.delayDownstream(ctx, msg);
        }

        private void delayDownstream(final ChannelHandlerContext ctx, final Object msg) {
            int sendDelay = this.delay + (int)(Math.random() * this.jitter);
            this.queuedMessages.add(new ServerConnectionListener.LatencySimulator.DelayedMessage(ctx, msg));
            TIMER.newTimeout(this::onTimeout, sendDelay, TimeUnit.MILLISECONDS);
        }

        private void onTimeout(final Timeout timeout) {
            ServerConnectionListener.LatencySimulator.DelayedMessage next = this.queuedMessages.remove(0);
            next.ctx.fireChannelRead(next.msg);
        }

        private record DelayedMessage(ChannelHandlerContext ctx, Object msg) {
        }
    }
}