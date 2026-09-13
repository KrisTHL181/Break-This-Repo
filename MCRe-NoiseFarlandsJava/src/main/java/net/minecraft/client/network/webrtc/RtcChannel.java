package net.minecraft.client.network.webrtc;

import com.mojang.logging.LogUtils;
import dev.onvoid.webrtc.RTCDataChannel;
import dev.onvoid.webrtc.RTCDataChannelBuffer;
import dev.onvoid.webrtc.RTCDataChannelObserver;
import dev.onvoid.webrtc.RTCDataChannelState;
import dev.onvoid.webrtc.RTCPeerConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.channel.unix.Errors;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class RtcChannel extends AbstractChannel {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ChannelMetadata METADATA = new ChannelMetadata(false);
   private static final int MAX_CHUNK_SIZE = 262144;
   private static final long HIGH_WATER_MARK = 1048576L;
   private static final long LOW_WATER_MARK = 262144L;
   private static final int BACKPRESSURE_FLAG = 1;
   private final RtcHandshake.HandshakeResult handshakeResult;
   private final ChannelConfig config = new DefaultChannelConfig(this);
   private volatile boolean closed;
   private volatile boolean activated;
   private boolean writeStalled;

   public RtcChannel(final RtcHandshake.HandshakeResult handshakeResult) {
      super(null);
      this.handshakeResult = handshakeResult;
   }

   public ChannelMetadata metadata() {
      return METADATA;
   }

   public ChannelConfig config() {
      return this.config;
   }

   protected AbstractChannel.AbstractUnsafe newUnsafe() {
      return new RtcUnsafe();
   }

   protected boolean isCompatible(final EventLoop loop) {
      return loop instanceof SingleThreadEventLoop;
   }

   public boolean isOpen() {
      return !this.closed;
   }

   public boolean isActive() {
      return this.activated && !this.closed;
   }

   protected SocketAddress localAddress0() {
      return new InetSocketAddress("rtc-local", 0);
   }

   protected SocketAddress remoteAddress0() {
      return new InetSocketAddress("rtc-remote", 0);
   }

   protected void doRegister(final ChannelPromise promise) {
      LOGGER.debug("RtcChannel registered (DataChannel state={})", this.handshakeResult.dataChannel().getState());
      RTCDataChannelState initial = this.handshakeResult.dataChannel().getState();
      this.eventLoop().execute(() -> {
         this.handleStateChange(initial);
         this.handshakeResult.dataChannel().registerObserver(new RTCDataChannelObserver() {
            public void onMessage(final RTCDataChannelBuffer buffer) {
               ByteBuf copy = Unpooled.copiedBuffer(buffer.data);
               RtcChannel.this.eventLoop().execute(() -> RtcChannel.this.handleMessage(copy));
            }

            public void onStateChange() {
               RTCDataChannelState state = RtcChannel.this.handshakeResult.dataChannel().getState();
               RtcChannel.LOGGER.debug("DataChannel state={}", state);
               RtcChannel.this.eventLoop().execute(() -> RtcChannel.this.handleStateChange(state));
            }

            public void onBufferedAmountChange(final long previousAmount) {
               if (RtcChannel.this.handshakeResult.dataChannel().getBufferedAmount() <= 262144L) {
                  RtcChannel.this.eventLoop().execute(() -> RtcChannel.this.setWriteStalled(false));
               }
            }
         });
         promise.setSuccess();
      });
   }

   protected void doBind(final SocketAddress localAddress) {
      throw new UnsupportedOperationException("RtcChannel cannot be bound");
   }

   protected void doDisconnect() {
      this.closeFromTransport();
   }

   protected void doClose() {
      if (!this.closed) {
         this.closed = true;
         dispose(this.handshakeResult);
      }
   }

   public static void dispose(final RtcHandshake.HandshakeResult handshakeResult) {
      dispose(handshakeResult.peerConnection(), handshakeResult.dataChannel());
   }

   public static void dispose(final RTCPeerConnection peerConnection, final @Nullable RTCDataChannel dataChannel) {
      if (dataChannel != null) {
         try {
            dataChannel.unregisterObserver();
         } catch (RuntimeException e) {
            LOGGER.debug("DataChannel unregisterObserver threw", e);
         }

         try {
            dataChannel.close();
         } catch (RuntimeException e) {
            LOGGER.debug("DataChannel close threw", e);
         }

         try {
            dataChannel.dispose();
         } catch (RuntimeException e) {
            LOGGER.debug("DataChannel dispose threw", e);
         }
      }

      try {
         peerConnection.close();
      } catch (RuntimeException e) {
         LOGGER.debug("Peer connection close threw", e);
      }
   }

   protected void doBeginRead() {
   }

   protected void doWrite(final ChannelOutboundBuffer in) throws Exception {
      Object msg;
      while ((msg = in.current()) != null) {
         if (msg instanceof ByteBuf buf) {
            this.writeByteBuf(buf);
         }

         in.remove();
         if (this.handshakeResult.dataChannel().getBufferedAmount() >= 1048576L) {
            this.setWriteStalled(true);
            return;
         }
      }
   }

   private void writeByteBuf(final ByteBuf buf) throws Exception {
      int remaining = buf.readableBytes();
      int idx = buf.readerIndex();

      while (remaining > 0) {
         int chunk = Math.min(remaining, 262144);
         byte[] bytes = new byte[chunk];
         buf.getBytes(idx, bytes);

         try {
            this.handshakeResult.dataChannel().send(new RTCDataChannelBuffer(ByteBuffer.wrap(bytes), true));
         } catch (Exception e) {
            LOGGER.debug("Failed to send DataChannel message", e);
            throw e;
         }

         idx += chunk;
         remaining -= chunk;
      }
   }

   private void setWriteStalled(final boolean stalled) {
      if (!this.closed && stalled != this.writeStalled) {
         this.writeStalled = stalled;
         ChannelOutboundBuffer outbound = this.unsafe().outboundBuffer();
         if (outbound != null) {
            outbound.setUserDefinedWritability(1, !stalled);
         }

         if (!stalled) {
            this.unsafe().flush();
         }
      }
   }

   private void handleMessage(final ByteBuf buf) {
      if (!this.closed && this.activated && this.config.isAutoRead()) {
         this.pipeline().fireChannelRead(buf);
         this.pipeline().fireChannelReadComplete();
      } else {
         buf.release();
      }
   }

   private void handleStateChange(final RTCDataChannelState state) {
      if (!this.closed) {
         switch (state) {
            case OPEN:
               if (!this.activated) {
                  LOGGER.debug("DataChannel OPEN, activating channel");
                  this.activated = true;
                  this.pipeline().fireChannelActive();
               }
               break;
            case CLOSING:
            case CLOSED:
               this.closeFromTransport();
         }
      }
   }

   private void closeFromTransport() {
      if (!this.closed) {
         LOGGER.debug("Closing RtcChannel from transport");
         this.unsafe().close(this.voidPromise());
      }
   }

   private final class RtcUnsafe extends AbstractChannel.AbstractUnsafe {
      private RtcUnsafe() {
         super();
      }

      public void connect(final SocketAddress remote, final SocketAddress local, final ChannelPromise promise) {
         promise.setFailure(new UnsupportedOperationException("RtcChannel is already connected to its RTCDataChannel"));
      }
   }
}
