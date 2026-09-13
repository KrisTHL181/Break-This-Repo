package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import org.jspecify.annotations.Nullable;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf, C> {
   final ConnectionProtocol protocol;
   final PacketFlow flow;
   private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = new ArrayList<>();
   private @Nullable BundlerInfo bundlerInfo;

   public ProtocolInfoBuilder(ConnectionProtocol p_334175_, PacketFlow p_335651_) {
      this.protocol = p_334175_;
      this.flow = p_335651_;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(PacketType<P> p_335373_, StreamCodec<? super B, P> p_333531_) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(p_335373_, p_333531_, null));
      return this;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(
      PacketType<P> p_391568_, StreamCodec<? super B, P> p_394417_, CodecModifier<B, P, C> p_397994_
   ) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(p_391568_, p_394417_, p_397994_));
      return this;
   }

   public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(
      PacketType<P> p_336277_, Function<Iterable<Packet<? super T>>, P> p_331716_, D p_328432_
   ) {
      StreamCodec<ByteBuf, D> streamcodec = StreamCodec.unit(p_328432_);
      PacketType<D> packettype = (PacketType<D>)p_328432_.type();
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(packettype, streamcodec, null));
      this.bundlerInfo = BundlerInfo.createForPacket(p_336277_, p_331716_, p_328432_);
      return this;
   }

   StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(
      Function<ByteBuf, B> p_331741_, List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> p_329135_, C p_392862_
   ) {
      ProtocolCodecBuilder<ByteBuf, T> protocolcodecbuilder = new ProtocolCodecBuilder<>(this.flow);

      for (ProtocolInfoBuilder.CodecEntry<T, ?, B, C> codecentry : p_329135_) {
         codecentry.addToBuilder(protocolcodecbuilder, p_331741_, p_392862_);
      }

      return protocolcodecbuilder.build();
   }

   private static ProtocolInfo.Details buildDetails(
      final ConnectionProtocol p_395405_, final PacketFlow p_393141_, final List<? extends ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>> p_393857_
   ) {
      return new ProtocolInfo.Details() {
         @Override
         public ConnectionProtocol id() {
            return p_395405_;
         }

         @Override
         public PacketFlow flow() {
            return p_393141_;
         }

         @Override
         public void listPackets(ProtocolInfo.Details.PacketVisitor p_397253_) {
            for (int i = 0; i < p_393857_.size(); i++) {
               ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?> codecentry = (ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>)p_393857_.get(i);
               p_397253_.accept(codecentry.type, i);
            }
         }
      };
   }

   public SimpleUnboundProtocol<T, B> buildUnbound(final C p_392518_) {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
      final BundlerInfo bundlerinfo = this.bundlerInfo;
      final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
      return new SimpleUnboundProtocol<T, B>() {
         @Override
         public ProtocolInfo<T> bind(Function<ByteBuf, B> p_391671_) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(p_391671_, list, p_392518_),
               bundlerinfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return protocolinfo$details;
         }
      };
   }

   public UnboundProtocol<T, B, C> buildUnbound() {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> list = List.copyOf(this.codecs);
      final BundlerInfo bundlerinfo = this.bundlerInfo;
      final ProtocolInfo.Details protocolinfo$details = buildDetails(this.protocol, this.flow, list);
      return new UnboundProtocol<T, B, C>() {
         @Override
         public ProtocolInfo<T> bind(Function<ByteBuf, B> p_391590_, C p_391890_) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(p_391590_, list, p_391890_),
               bundlerinfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return protocolinfo$details;
         }
      };
   }

   private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(
      ConnectionProtocol p_330235_, PacketFlow p_335045_, Consumer<ProtocolInfoBuilder<L, B, Unit>> p_329753_
   ) {
      ProtocolInfoBuilder<L, B, Unit> protocolinfobuilder = new ProtocolInfoBuilder<>(p_330235_, p_335045_);
      p_329753_.accept(protocolinfobuilder);
      return protocolinfobuilder.buildUnbound(Unit.INSTANCE);
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(
      ConnectionProtocol p_331618_, Consumer<ProtocolInfoBuilder<T, B, Unit>> p_330318_
   ) {
      return protocol(p_331618_, PacketFlow.SERVERBOUND, p_330318_);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(
      ConnectionProtocol p_329688_, Consumer<ProtocolInfoBuilder<T, B, Unit>> p_332900_
   ) {
      return protocol(p_329688_, PacketFlow.CLIENTBOUND, p_332900_);
   }

   private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(
      ConnectionProtocol p_396066_, PacketFlow p_392584_, Consumer<ProtocolInfoBuilder<L, B, C>> p_393675_
   ) {
      ProtocolInfoBuilder<L, B, C> protocolinfobuilder = new ProtocolInfoBuilder<>(p_396066_, p_392584_);
      p_393675_.accept(protocolinfobuilder);
      return protocolinfobuilder.buildUnbound();
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(
      ConnectionProtocol p_391713_, Consumer<ProtocolInfoBuilder<T, B, C>> p_394900_
   ) {
      return contextProtocol(p_391713_, PacketFlow.SERVERBOUND, p_394900_);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(
      ConnectionProtocol p_396941_, Consumer<ProtocolInfoBuilder<T, B, C>> p_394206_
   ) {
      return contextProtocol(p_396941_, PacketFlow.CLIENTBOUND, p_394206_);
   }

   record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
      PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier
   ) {
      public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> p_328095_, Function<ByteBuf, B> p_333803_, C p_392980_) {
         StreamCodec<? super B, P> streamcodec;
         if (this.modifier != null) {
            streamcodec = this.modifier.apply(this.serializer, p_392980_);
         } else {
            streamcodec = this.serializer;
         }

         StreamCodec<ByteBuf, P> streamcodec1 = streamcodec.mapStream(p_333803_);
         p_328095_.add(this.type, streamcodec1);
      }
   }

   record Implementation<L extends PacketListener>(
      ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
   ) implements ProtocolInfo<L> {
   }
}
