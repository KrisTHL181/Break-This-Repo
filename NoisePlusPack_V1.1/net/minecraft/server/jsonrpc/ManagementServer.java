package net.minecraft.server.jsonrpc;

import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.jsonrpc.security.AuthenticationHandler;
import net.minecraft.server.jsonrpc.websocket.JsonToWebSocketEncoder;
import net.minecraft.server.jsonrpc.websocket.WebSocketToJsonCodec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ManagementServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final HostAndPort hostAndPort;
   final AuthenticationHandler authenticationHandler;
   private @Nullable Channel serverChannel;
   private final NioEventLoopGroup nioEventLoopGroup;
   private final Set<Connection> connections = Sets.newIdentityHashSet();

   public ManagementServer(HostAndPort p_426593_, AuthenticationHandler p_424598_) {
      this.hostAndPort = p_426593_;
      this.authenticationHandler = p_424598_;
      this.nioEventLoopGroup = new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Management server IO #%d").setDaemon(true).build());
   }

   public ManagementServer(HostAndPort p_422908_, AuthenticationHandler p_430019_, NioEventLoopGroup p_431378_) {
      this.hostAndPort = p_422908_;
      this.authenticationHandler = p_430019_;
      this.nioEventLoopGroup = p_431378_;
   }

   public void onConnected(Connection p_427033_) {
      synchronized (this.connections) {
         this.connections.add(p_427033_);
      }
   }

   public void onDisconnected(Connection p_429054_) {
      synchronized (this.connections) {
         this.connections.remove(p_429054_);
      }
   }

   public void startWithoutTls(MinecraftApi p_423287_) {
      this.start(p_423287_, null);
   }

   public void startWithTls(MinecraftApi p_426868_, SslContext p_423207_) {
      this.start(p_426868_, p_423207_);
   }

   private void start(final MinecraftApi p_423781_, final @Nullable SslContext p_425912_) {
      final JsonRpcLogger jsonrpclogger = new JsonRpcLogger();
      ChannelFuture channelfuture = ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)new ServerBootstrap().handler(new LoggingHandler(LogLevel.DEBUG)))
               .channel(NioServerSocketChannel.class))
            .childHandler(
               new ChannelInitializer<Channel>() {
                  protected void initChannel(Channel p_429263_) {
                     try {
                        p_429263_.config().setOption(ChannelOption.TCP_NODELAY, true);
                     } catch (ChannelException var3) {
                     }

                     ChannelPipeline channelpipeline = p_429263_.pipeline();
                     if (p_425912_ != null) {
                        channelpipeline.addLast(new ChannelHandler[]{p_425912_.newHandler(p_429263_.alloc())});
                     }

                     channelpipeline.addLast(new ChannelHandler[]{new HttpServerCodec()})
                        .addLast(new ChannelHandler[]{new HttpObjectAggregator(65536)})
                        .addLast(new ChannelHandler[]{ManagementServer.this.authenticationHandler})
                        .addLast(new ChannelHandler[]{new WebSocketServerProtocolHandler("/")})
                        .addLast(new ChannelHandler[]{new WebSocketToJsonCodec()})
                        .addLast(new ChannelHandler[]{new JsonToWebSocketEncoder()})
                        .addLast(new ChannelHandler[]{new Connection(p_429263_, ManagementServer.this, p_423781_, jsonrpclogger)});
                  }
               }
            )
            .group(this.nioEventLoopGroup)
            .localAddress(this.hostAndPort.getHost(), this.hostAndPort.getPort()))
         .bind();
      this.serverChannel = channelfuture.channel();
      channelfuture.syncUninterruptibly();
      LOGGER.info("Json-RPC Management connection listening on {}:{}", this.hostAndPort.getHost(), this.getPort());
   }

   public void stop(boolean p_422749_) throws InterruptedException {
      if (this.serverChannel != null) {
         this.serverChannel.close().sync();
         this.serverChannel = null;
      }

      this.connections.clear();
      if (p_422749_) {
         this.nioEventLoopGroup.shutdownGracefully().sync();
      }
   }

   public void tick() {
      this.forEachConnection(Connection::tick);
   }

   public int getPort() {
      return this.serverChannel != null ? ((InetSocketAddress)this.serverChannel.localAddress()).getPort() : this.hostAndPort.getPort();
   }

   void forEachConnection(Consumer<Connection> p_429471_) {
      synchronized (this.connections) {
         this.connections.forEach(p_429471_);
      }
   }
}
