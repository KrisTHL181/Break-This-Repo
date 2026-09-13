package net.minecraft.network;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Connection extends SimpleChannelInboundHandler<Packet<?>> {
   private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Marker ROOT_MARKER = MarkerFactory.getMarker("NETWORK");
   public static final Marker PACKET_MARKER = Util.make(MarkerFactory.getMarker("NETWORK_PACKETS"), m -> m.add(ROOT_MARKER));
   public static final Marker PACKET_RECEIVED_MARKER = Util.make(MarkerFactory.getMarker("PACKET_RECEIVED"), m -> m.add(PACKET_MARKER));
   public static final Marker PACKET_SENT_MARKER = Util.make(MarkerFactory.getMarker("PACKET_SENT"), m -> m.add(PACKET_MARKER));
   private static final ProtocolInfo<ServerHandshakePacketListener> INITIAL_PROTOCOL = HandshakeProtocols.SERVERBOUND;
   private final PacketFlow receiving;
   private volatile boolean sendLoginDisconnect = true;
   private final Queue<Consumer<Connection>> pendingActions = Queues.newConcurrentLinkedQueue();
   private Channel channel;
   private SocketAddress address;
   private volatile @Nullable PacketListener disconnectListener;
   private volatile @Nullable PacketListener packetListener;
   private @Nullable DisconnectionDetails disconnectionDetails;
   private boolean disconnectionHandled;
   private int receivedPackets;
   private int sentPackets;
   private float averageReceivedPackets;
   private float averageSentPackets;
   private int tickCount;
   private boolean handlingFault;
   private volatile @Nullable DisconnectionDetails delayedDisconnect;
   private @Nullable BandwidthDebugMonitor bandwidthDebugMonitor;

   public Connection(final PacketFlow receiving) {
      this.receiving = receiving;
   }

   public void channelActive(final ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      this.channel = ctx.channel();
      this.address = this.channel.remoteAddress();
      if (this.delayedDisconnect != null) {
         this.disconnect(this.delayedDisconnect);
      }
   }

   public void channelInactive(final ChannelHandlerContext ctx) {
      this.disconnect(Component.translatable("disconnect.endOfStream"));
   }

   public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      if (cause instanceof SkipPacketException) {
         LOGGER.debug("Skipping packet due to errors", cause.getCause());
      } else {
         boolean isFirstFault = !this.handlingFault;
         this.handlingFault = true;
         if (this.channel.isOpen()) {
            if (cause instanceof TimeoutException) {
               LOGGER.debug("Timeout", cause);
               this.disconnect(Component.translatable("disconnect.timeout"));
            } else {
               Component reason = Component.translatable("disconnect.genericReason", "Internal Exception: " + cause);
               PacketListener listener = this.packetListener;
               DisconnectionDetails details;
               if (listener != null) {
                  details = listener.createDisconnectionInfo(reason, cause);
               } else {
                  details = new DisconnectionDetails(reason);
               }

               if (isFirstFault) {
                  LOGGER.debug("Failed to sent packet", cause);
                  if (this.getSending() == PacketFlow.CLIENTBOUND) {
                     Packet<?> packet = this.sendLoginDisconnect ? new ClientboundLoginDisconnectPacket(reason) : new ClientboundDisconnectPacket(reason);
                     this.send(packet, PacketSendListener.thenRun(() -> this.disconnect(details)));
                  } else {
                     this.disconnect(details);
                  }

                  this.setReadOnly();
               } else {
                  LOGGER.debug("Double fault", cause);
                  this.disconnect(details);
               }
            }
         }
      }
   }

   protected void channelRead0(final ChannelHandlerContext ctx, final Packet<?> packet) {
      if (this.channel.isOpen()) {
         PacketListener packetListener = this.packetListener;
         if (packetListener == null) {
            throw new IllegalStateException("Received a packet before the packet listener was initialized");
         }

         if (packetListener.shouldHandleMessage(packet)) {
            try {
               genericsFtw(packet, packetListener);
            } catch (RunningOnDifferentThreadException var5) {
            } catch (RejectedExecutionException ignored) {
               this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
            } catch (ClassCastException exception) {
               LOGGER.error("Received {} that couldn't be processed", packet.getClass(), exception);
               this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
            }

            this.receivedPackets++;
         }
      }
   }

   private static <T extends PacketListener> void genericsFtw(final Packet<T> packet, final PacketListener listener) {
      packet.handle((T)listener);
   }

   private void validateListener(final ProtocolInfo<?> protocol, final PacketListener packetListener) {
      Objects.requireNonNull(packetListener, "packetListener");
      PacketFlow listenerFlow = packetListener.flow();
      if (listenerFlow != this.receiving) {
         throw new IllegalStateException("Trying to set listener for wrong side: connection is " + this.receiving + ", but listener is " + listenerFlow);
      }

      ConnectionProtocol listenerProtocol = packetListener.protocol();
      if (protocol.id() != listenerProtocol) {
         throw new IllegalStateException("Listener protocol (" + listenerProtocol + ") does not match requested one " + protocol);
      }
   }

   private static void syncAfterConfigurationChange(final ChannelFuture future) {
      try {
         future.syncUninterruptibly();
      } catch (Exception e) {
         if (e instanceof ClosedChannelException) {
            LOGGER.info("Connection closed during protocol change");
         } else {
            throw e;
         }
      }
   }

   public <T extends PacketListener> void setupInboundProtocol(final ProtocolInfo<T> protocol, final T packetListener) {
      this.validateListener(protocol, packetListener);
      if (protocol.flow() != this.getReceiving()) {
         throw new IllegalStateException("Invalid inbound protocol: " + protocol.id());
      }

      this.packetListener = packetListener;
      this.disconnectListener = null;
      UnconfiguredPipelineHandler.InboundConfigurationTask configMessage = UnconfiguredPipelineHandler.setupInboundProtocol(protocol);
      BundlerInfo bundlerInfo = protocol.bundlerInfo();
      if (bundlerInfo != null) {
         PacketBundlePacker newBundler = new PacketBundlePacker(bundlerInfo);
         configMessage = configMessage.andThen(ctx -> ctx.pipeline().addAfter("decoder", "bundler", newBundler));
      }

      syncAfterConfigurationChange(this.channel.writeAndFlush(configMessage));
   }

   public void setupOutboundProtocol(final ProtocolInfo<?> protocol) {
      if (protocol.flow() != this.getSending()) {
         throw new IllegalStateException("Invalid outbound protocol: " + protocol.id());
      }

      UnconfiguredPipelineHandler.OutboundConfigurationTask configMessage = UnconfiguredPipelineHandler.setupOutboundProtocol(protocol);
      BundlerInfo bundlerInfo = protocol.bundlerInfo();
      if (bundlerInfo != null) {
         PacketBundleUnpacker newUnbundler = new PacketBundleUnpacker(bundlerInfo);
         configMessage = configMessage.andThen(ctx -> ctx.pipeline().addAfter("encoder", "unbundler", newUnbundler));
      }

      boolean isLoginProtocol = protocol.id() == ConnectionProtocol.LOGIN;
      syncAfterConfigurationChange(this.channel.writeAndFlush(configMessage.andThen(ctx -> this.sendLoginDisconnect = isLoginProtocol)));
   }

   public void setListenerForServerboundHandshake(final PacketListener packetListener) {
      if (this.packetListener != null) {
         throw new IllegalStateException("Listener already set");
      }

      if (this.receiving == PacketFlow.SERVERBOUND && packetListener.flow() == PacketFlow.SERVERBOUND && packetListener.protocol() == INITIAL_PROTOCOL.id()) {
         this.packetListener = packetListener;
      } else {
         throw new IllegalStateException("Invalid initial listener");
      }
   }

   public void initiateServerboundStatusConnection(final String hostName, final int port, final ClientStatusPacketListener listener) {
      this.initiateServerboundConnection(hostName, port, StatusProtocols.SERVERBOUND, StatusProtocols.CLIENTBOUND, listener, ClientIntent.STATUS);
   }

   public void initiateServerboundPlayConnection(final String hostName, final int port, final ClientLoginPacketListener listener) {
      this.initiateServerboundConnection(hostName, port, LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND, listener, ClientIntent.LOGIN);
   }

   public <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundPlayConnection(
      final String hostName, final int port, final ProtocolInfo<S> outbound, final ProtocolInfo<C> inbound, final C listener, final boolean transfer
   ) {
      this.initiateServerboundConnection(hostName, port, outbound, inbound, listener, transfer ? ClientIntent.TRANSFER : ClientIntent.LOGIN);
   }

   private <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundConnection(
      final String hostName, final int port, final ProtocolInfo<S> outbound, final ProtocolInfo<C> inbound, final C listener, final ClientIntent intent
   ) {
      if (outbound.id() != inbound.id()) {
         throw new IllegalStateException("Mismatched initial protocols");
      }

      this.disconnectListener = listener;
      this.runOnceConnected(connection -> {
         this.setupInboundProtocol(inbound, listener);
         connection.sendPacket(new ClientIntentionPacket(SharedConstants.getCurrentVersion().protocolVersion(), hostName, port, intent), null, true);
         this.setupOutboundProtocol(outbound);
      });
   }

   public void send(final Packet<?> packet) {
      this.send(packet, null);
   }

   public void send(final Packet<?> packet, final @Nullable ChannelFutureListener listener) {
      this.send(packet, listener, true);
   }

   public void send(final Packet<?> packet, final @Nullable ChannelFutureListener listener, final boolean flush) {
      if (this.isConnected()) {
         this.flushQueue();
         this.sendPacket(packet, listener, flush);
      } else {
         this.pendingActions.add(connection -> connection.sendPacket(packet, listener, flush));
      }
   }

   public void runOnceConnected(final Consumer<Connection> action) {
      if (this.isConnected()) {
         this.flushQueue();
         action.accept(this);
      } else {
         this.pendingActions.add(action);
      }
   }

   private void sendPacket(final Packet<?> packet, final @Nullable ChannelFutureListener listener, final boolean flush) {
      this.sentPackets++;
      if (this.channel.eventLoop().inEventLoop()) {
         this.doSendPacket(packet, listener, flush);
      } else {
         this.channel.eventLoop().execute(() -> this.doSendPacket(packet, listener, flush));
      }
   }

   private void doSendPacket(final Packet<?> packet, final @Nullable ChannelFutureListener listener, final boolean flush) {
      if (listener != null) {
         ChannelFuture future = flush ? this.channel.writeAndFlush(packet) : this.channel.write(packet);
         future.addListener(listener);
      } else if (flush) {
         this.channel.writeAndFlush(packet, this.channel.voidPromise());
      } else {
         this.channel.write(packet, this.channel.voidPromise());
      }
   }

   public void flushChannel() {
      if (this.isConnected()) {
         this.flush();
      } else {
         this.pendingActions.add(Connection::flush);
      }
   }

   private void flush() {
      if (this.channel.eventLoop().inEventLoop()) {
         this.channel.flush();
      } else {
         this.channel.eventLoop().execute(() -> this.channel.flush());
      }
   }

   private void flushQueue() {
      if (this.channel != null && this.channel.isOpen()) {
         synchronized (this.pendingActions) {
            Consumer<Connection> pendingAction;
            while ((pendingAction = this.pendingActions.poll()) != null) {
               pendingAction.accept(this);
            }
         }
      }
   }

   public void tick() {
      this.flushQueue();
      if (this.packetListener instanceof TickablePacketListener tickable) {
         tickable.tick();
      }

      if (!this.isConnected() && !this.disconnectionHandled) {
         this.handleDisconnection();
      }

      if (this.channel != null) {
         this.channel.flush();
      }

      if (this.tickCount++ % 20 == 0) {
         this.tickSecond();
      }

      if (this.bandwidthDebugMonitor != null) {
         this.bandwidthDebugMonitor.tick();
      }
   }

   protected void tickSecond() {
      this.averageSentPackets = Mth.lerp(0.75F, this.sentPackets, this.averageSentPackets);
      this.averageReceivedPackets = Mth.lerp(0.75F, this.receivedPackets, this.averageReceivedPackets);
      this.sentPackets = 0;
      this.receivedPackets = 0;
   }

   public SocketAddress getRemoteAddress() {
      return this.address;
   }

   public String getLoggableAddress(final boolean logIPs) {
      if (this.address == null) {
         return "local";
      } else {
         return logIPs ? this.address.toString() : "IP hidden";
      }
   }

   public void disconnect(final Component reason) {
      this.disconnect(new DisconnectionDetails(reason));
   }

   public void disconnect(final DisconnectionDetails details) {
      if (this.channel == null) {
         this.delayedDisconnect = details;
      }

      if (this.isConnected()) {
         this.channel.close().awaitUninterruptibly();
         this.disconnectionDetails = details;
      }
   }

   public boolean isMemoryConnection() {
      return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
   }

   public PacketFlow getReceiving() {
      return this.receiving;
   }

   public PacketFlow getSending() {
      return this.receiving.getOpposite();
   }

   public static Connection connectToServer(
      final InetSocketAddress address, final EventLoopGroupHolder eventLoopGroupHolder, final @Nullable LocalSampleLogger bandwidthLogger
   ) {
      Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      if (bandwidthLogger != null) {
         connection.setBandwidthLogger(bandwidthLogger);
      }

      ChannelFuture connect = connect(address, eventLoopGroupHolder, connection);
      connect.syncUninterruptibly();
      return connection;
   }

   public static ChannelFuture connect(final InetSocketAddress address, final EventLoopGroupHolder eventLoopGroupHolder, final Connection connection) {
      return ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(eventLoopGroupHolder.eventLoopGroup())).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(final Channel channel) {
            try {
               channel.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException var3) {
            }

            ChannelPipeline pipeline = channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
            Connection.configureSerialization(pipeline, PacketFlow.CLIENTBOUND, false, connection.bandwidthDebugMonitor);
            connection.configurePacketHandler(pipeline);
         }
      })).channel(eventLoopGroupHolder.channelCls())).connect(address.getAddress(), address.getPort());
   }

   private static String outboundHandlerName(final boolean configureOutbound) {
      return configureOutbound ? "encoder" : "outbound_config";
   }

   private static String inboundHandlerName(final boolean configureInbound) {
      return configureInbound ? "decoder" : "inbound_config";
   }

   public void configurePacketHandler(final ChannelPipeline pipeline) {
      pipeline.addLast("hackfix", new ChannelOutboundHandlerAdapter() {
         public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
            super.write(ctx, msg, promise);
         }
      }).addLast("packet_handler", this);
   }

   public static void configureSerialization(
      final ChannelPipeline pipeline, final PacketFlow inboundDirection, final boolean local, final @Nullable BandwidthDebugMonitor monitor
   ) {
      PacketFlow outboundDirection = inboundDirection.getOpposite();
      boolean configureInbound = inboundDirection == PacketFlow.SERVERBOUND;
      boolean configureOutbound = outboundDirection == PacketFlow.SERVERBOUND;
      pipeline.addLast("splitter", createFrameDecoder(monitor, local))
         .addLast(new ChannelHandler[]{new FlowControlHandler()})
         .addLast(
            inboundHandlerName(configureInbound),
            (ChannelHandler)(configureInbound ? new PacketDecoder<ServerHandshakePacketListener>(INITIAL_PROTOCOL) : new UnconfiguredPipelineHandler.Inbound())
         )
         .addLast("prepender", createFrameEncoder(local))
         .addLast(
            outboundHandlerName(configureOutbound),
            (ChannelHandler)(configureOutbound
               ? new PacketEncoder<ServerHandshakePacketListener>(INITIAL_PROTOCOL)
               : new UnconfiguredPipelineHandler.Outbound())
         );
   }

   private static ChannelOutboundHandler createFrameEncoder(final boolean local) {
      return (ChannelOutboundHandler)(local ? new LocalFrameEncoder() : new Varint21LengthFieldPrepender());
   }

   private static ChannelInboundHandler createFrameDecoder(final @Nullable BandwidthDebugMonitor monitor, final boolean local) {
      if (!local) {
         return new Varint21FrameDecoder(monitor);
      } else {
         return (ChannelInboundHandler)(monitor != null ? new MonitoredLocalFrameDecoder(monitor) : new LocalFrameDecoder());
      }
   }

   public static void configureInMemoryPipeline(final ChannelPipeline pipeline, final PacketFlow packetFlow) {
      configureSerialization(pipeline, packetFlow, true, null);
   }

   public static Connection connectToLocalServer(final SocketAddress address) {
      final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(EventLoopGroupHolder.local().eventLoopGroup())).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(final Channel channel) {
            ChannelPipeline pipeline = channel.pipeline();
            Connection.configureInMemoryPipeline(pipeline, PacketFlow.CLIENTBOUND);
            connection.configurePacketHandler(pipeline);
         }
      })).channel(EventLoopGroupHolder.local().channelCls())).connect(address).syncUninterruptibly();
      return connection;
   }

   public static Connection fromChannel(final Channel channel, final PacketFlow flow, final @Nullable LocalSampleLogger bandwidthLogger) {
      Connection connection = new Connection(flow);
      if (bandwidthLogger != null) {
         connection.setBandwidthLogger(bandwidthLogger);
      }

      ChannelPipeline pipeline = channel.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
      configureSerialization(pipeline, flow, false, connection.bandwidthDebugMonitor);
      connection.configurePacketHandler(pipeline);
      EventLoopGroupHolder.local().eventLoopGroup().register(channel).syncUninterruptibly();
      return connection;
   }

   public void setEncryptionKey(final Cipher decryptCipher, final Cipher encryptCipher) {
      this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(decryptCipher));
      this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(encryptCipher));
   }

   public boolean isConnected() {
      return this.channel != null && this.channel.isOpen();
   }

   public boolean isConnecting() {
      return this.channel == null;
   }

   public @Nullable PacketListener getPacketListener() {
      return this.packetListener;
   }

   public @Nullable DisconnectionDetails getDisconnectionDetails() {
      return this.disconnectionDetails;
   }

   public void setReadOnly() {
      if (this.channel != null) {
         this.channel.config().setAutoRead(false);
      }
   }

   public void setupCompression(final int threshold, final boolean validateDecompressed) {
      if (threshold >= 0) {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder compressionDecoder) {
            compressionDecoder.setThreshold(threshold, validateDecompressed);
         } else {
            this.channel.pipeline().addAfter("splitter", "decompress", new CompressionDecoder(threshold, validateDecompressed));
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder compressionEncoder) {
            compressionEncoder.setThreshold(threshold);
         } else {
            this.channel.pipeline().addAfter("prepender", "compress", new CompressionEncoder(threshold));
         }
      } else {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
            this.channel.pipeline().remove("decompress");
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
            this.channel.pipeline().remove("compress");
         }
      }
   }

   public void handleDisconnection() {
      if (this.channel != null && !this.channel.isOpen()) {
         if (this.disconnectionHandled) {
            LOGGER.warn("handleDisconnection() called twice");
         } else {
            this.disconnectionHandled = true;
            PacketListener packetListener = this.getPacketListener();
            PacketListener disconnectListener = packetListener != null ? packetListener : this.disconnectListener;
            if (disconnectListener != null) {
               DisconnectionDetails details = Objects.requireNonNullElseGet(
                  this.getDisconnectionDetails(), () -> new DisconnectionDetails(Component.translatable("multiplayer.disconnect.generic"))
               );
               disconnectListener.onDisconnect(details);
            }
         }
      }
   }

   public float getAverageReceivedPackets() {
      return this.averageReceivedPackets;
   }

   public float getAverageSentPackets() {
      return this.averageSentPackets;
   }

   public void setBandwidthLogger(final LocalSampleLogger bandwidthLogger) {
      this.bandwidthDebugMonitor = new BandwidthDebugMonitor(bandwidthLogger);
   }
}
