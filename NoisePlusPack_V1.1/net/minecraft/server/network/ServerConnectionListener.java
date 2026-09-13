package net.minecraft.server.network;

import com.google.common.collect.Lists;
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
   final MinecraftServer server;
   public volatile boolean running;
   private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
   final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public ServerConnectionListener(MinecraftServer p_9707_) {
      this.server = p_9707_;
      this.running = true;
   }

   public void startTcpServerListener(@Nullable InetAddress p_9712_, int p_9713_) throws IOException {
      synchronized (this.channels) {
         EventLoopGroupHolder eventloopgroupholder = EventLoopGroupHolder.remote(this.server.useNativeTransport());
         this.channels
            .add(
               ((ServerBootstrap)((ServerBootstrap)new ServerBootstrap().channel(eventloopgroupholder.serverChannelCls()))
                     .childHandler(new ChannelInitializer<Channel>() {
                        protected void initChannel(Channel p_9729_) {
                           try {
                              p_9729_.config().setOption(ChannelOption.TCP_NODELAY, true);
                           } catch (ChannelException var5) {
                           }

                           ChannelPipeline channelpipeline = p_9729_.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                           if (ServerConnectionListener.this.server.repliesToStatus()) {
                              channelpipeline.addLast("legacy_query", new LegacyQueryHandler(ServerConnectionListener.this.getServer()));
                           }

                           Connection.configureSerialization(channelpipeline, PacketFlow.SERVERBOUND, false, null);
                           int i = ServerConnectionListener.this.server.getRateLimitPacketsPerSecond();
                           Connection connection = i > 0 ? new RateKickingConnection(i) : new Connection(PacketFlow.SERVERBOUND);
                           ServerConnectionListener.this.connections.add(connection);
                           connection.configurePacketHandler(channelpipeline);
                           connection.setListenerForServerboundHandshake(
                              new ServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, connection)
                           );
                        }
                     })
                     .group(eventloopgroupholder.eventLoopGroup())
                     .localAddress(p_9712_, p_9713_))
                  .bind()
                  .syncUninterruptibly()
            );
      }
   }

   public SocketAddress startMemoryChannel() {
      ChannelFuture channelfuture;
      synchronized (this.channels) {
         channelfuture = ((ServerBootstrap)((ServerBootstrap)new ServerBootstrap().channel(EventLoopGroupHolder.local().serverChannelCls()))
               .childHandler(
                  new ChannelInitializer<Channel>() {
                     protected void initChannel(Channel p_9734_) {
                        Connection connection = new Connection(PacketFlow.SERVERBOUND);
                        connection.setListenerForServerboundHandshake(
                           new MemoryServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, connection)
                        );
                        ServerConnectionListener.this.connections.add(connection);
                        ChannelPipeline channelpipeline = p_9734_.pipeline();
                        Connection.configureInMemoryPipeline(channelpipeline, PacketFlow.SERVERBOUND);
                        if (SharedConstants.DEBUG_FAKE_LATENCY_MS > 0) {
                           channelpipeline.addLast(
                              "latency",
                              new ServerConnectionListener.LatencySimulator(SharedConstants.DEBUG_FAKE_LATENCY_MS, SharedConstants.DEBUG_FAKE_JITTER_MS)
                           );
                        }

                        connection.configurePacketHandler(channelpipeline);
                     }
                  }
               )
               .group(EventLoopGroupHolder.local().eventLoopGroup())
               .localAddress(LocalAddress.ANY))
            .bind()
            .syncUninterruptibly();
         this.channels.add(channelfuture);
      }

      return channelfuture.channel().localAddress();
   }

   public void stop() {
      this.running = false;

      for (ChannelFuture channelfuture : this.channels) {
         try {
            channelfuture.channel().close().sync();
         } catch (InterruptedException interruptedexception) {
            LOGGER.error("Interrupted whilst closing channel");
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
                  } catch (Exception exception) {
                     if (connection.isMemoryConnection()) {
                        throw new ReportedException(CrashReport.forThrowable(exception, "Ticking memory connection"));
                     }

                     LOGGER.warn("Failed to handle packet for {}", connection.getLoggableAddress(this.server.logIPs()), exception);
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
      }
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public List<Connection> getConnections() {
      return this.connections;
   }

   static class LatencySimulator extends ChannelInboundHandlerAdapter {
      private static final Timer TIMER = new HashedWheelTimer();
      private final int delay;
      private final int jitter;
      private final List<ServerConnectionListener.LatencySimulator.DelayedMessage> queuedMessages = Lists.newArrayList();

      public LatencySimulator(int p_143593_, int p_143594_) {
         this.delay = p_143593_;
         this.jitter = p_143594_;
      }

      public void channelRead(ChannelHandlerContext p_143601_, Object p_143602_) {
         this.delayDownstream(p_143601_, p_143602_);
      }

      private void delayDownstream(ChannelHandlerContext p_143596_, Object p_143597_) {
         int i = this.delay + (int)(Math.random() * this.jitter);
         this.queuedMessages.add(new ServerConnectionListener.LatencySimulator.DelayedMessage(p_143596_, p_143597_));
         TIMER.newTimeout(this::onTimeout, i, TimeUnit.MILLISECONDS);
      }

      private void onTimeout(Timeout p_143599_) {
         ServerConnectionListener.LatencySimulator.DelayedMessage serverconnectionlistener$latencysimulator$delayedmessage = this.queuedMessages.remove(0);
         serverconnectionlistener$latencysimulator$delayedmessage.ctx.fireChannelRead(serverconnectionlistener$latencysimulator$delayedmessage.msg);
      }

      static class DelayedMessage {
         public final ChannelHandlerContext ctx;
         public final Object msg;

         public DelayedMessage(ChannelHandlerContext p_143606_, Object p_143607_) {
            this.ctx = p_143606_;
            this.msg = p_143607_;
         }
      }
   }
}
