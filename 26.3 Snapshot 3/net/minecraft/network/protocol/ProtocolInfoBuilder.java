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
   private final ConnectionProtocol protocol;
   private final PacketFlow flow;
   private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = new ArrayList<>();
   private @Nullable BundlerInfo bundlerInfo;

   public ProtocolInfoBuilder(final ConnectionProtocol protocol, final PacketFlow flow) {
      this.protocol = protocol;
      this.flow = flow;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(final PacketType<P> type, final StreamCodec<? super B, P> serializer) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(type, serializer, null));
      return this;
   }

   public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B, C> addPacket(
      final PacketType<P> type, final StreamCodec<? super B, P> serializer, final CodecModifier<B, P, C> modifier
   ) {
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(type, serializer, modifier));
      return this;
   }

   public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B, C> withBundlePacket(
      final PacketType<P> bundlerPacket, final Function<Iterable<Packet<? super T>>, P> constructor, final D delimiterPacket
   ) {
      StreamCodec<ByteBuf, D> delimitedCodec = StreamCodec.unit(delimiterPacket);
      PacketType<D> delimiterType = (PacketType<D>)delimiterPacket.type();
      this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(delimiterType, delimitedCodec, null));
      this.bundlerInfo = BundlerInfo.createForPacket(bundlerPacket, constructor, delimiterPacket);
      return this;
   }

   private StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(
      final Function<ByteBuf, B> contextWrapper, final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs, final C context
   ) {
      ProtocolCodecBuilder<ByteBuf, T> codecBuilder = new ProtocolCodecBuilder<>(this.flow);

      for (ProtocolInfoBuilder.CodecEntry<T, ?, B, C> codec : codecs) {
         codec.addToBuilder(codecBuilder, contextWrapper, context);
      }

      return codecBuilder.build();
   }

   private static ProtocolInfo.Details buildDetails(
      final ConnectionProtocol protocol, final PacketFlow flow, final List<? extends ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>> codecs
   ) {
      return new ProtocolInfo.Details() {
         @Override
         public ConnectionProtocol id() {
            return protocol;
         }

         @Override
         public PacketFlow flow() {
            return flow;
         }

         @Override
         public void listPackets(final ProtocolInfo.Details.PacketVisitor output) {
            for (int i = 0; i < codecs.size(); i++) {
               ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?> entry = (ProtocolInfoBuilder.CodecEntry<?, ?, ?, ?>)codecs.get(i);
               output.accept(entry.type, i);
            }
         }
      };
   }

   public SimpleUnboundProtocol<T, B> buildUnbound(final C context) {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = List.copyOf(this.codecs);
      final BundlerInfo bundlerInfo = this.bundlerInfo;
      final ProtocolInfo.Details details = buildDetails(this.protocol, this.flow, codecs);
      return new SimpleUnboundProtocol<T, B>() {
         @Override
         public ProtocolInfo<T> bind(final Function<ByteBuf, B> contextWrapper) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(contextWrapper, codecs, context),
               bundlerInfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return details;
         }
      };
   }

   public UnboundProtocol<T, B, C> buildUnbound() {
      final List<ProtocolInfoBuilder.CodecEntry<T, ?, B, C>> codecs = List.copyOf(this.codecs);
      final BundlerInfo bundlerInfo = this.bundlerInfo;
      final ProtocolInfo.Details details = buildDetails(this.protocol, this.flow, codecs);
      return new UnboundProtocol<T, B, C>() {
         @Override
         public ProtocolInfo<T> bind(final Function<ByteBuf, B> contextWrapper, final C context) {
            return new ProtocolInfoBuilder.Implementation<>(
               ProtocolInfoBuilder.this.protocol,
               ProtocolInfoBuilder.this.flow,
               ProtocolInfoBuilder.this.buildPacketCodec(contextWrapper, codecs, context),
               bundlerInfo
            );
         }

         @Override
         public ProtocolInfo.Details details() {
            return details;
         }
      };
   }

   private static <L extends PacketListener, B extends ByteBuf> SimpleUnboundProtocol<L, B> protocol(
      final ConnectionProtocol id, final PacketFlow flow, final Consumer<ProtocolInfoBuilder<L, B, Unit>> config
   ) {
      ProtocolInfoBuilder<L, B, Unit> builder = new ProtocolInfoBuilder<>(id, flow);
      config.accept(builder);
      return builder.buildUnbound(Unit.INSTANCE);
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> serverboundProtocol(
      final ConnectionProtocol id, final Consumer<ProtocolInfoBuilder<T, B, Unit>> config
   ) {
      return protocol(id, PacketFlow.SERVERBOUND, config);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf> SimpleUnboundProtocol<T, B> clientboundProtocol(
      final ConnectionProtocol id, final Consumer<ProtocolInfoBuilder<T, B, Unit>> config
   ) {
      return protocol(id, PacketFlow.CLIENTBOUND, config);
   }

   private static <L extends PacketListener, B extends ByteBuf, C> UnboundProtocol<L, B, C> contextProtocol(
      final ConnectionProtocol id, final PacketFlow flow, final Consumer<ProtocolInfoBuilder<L, B, C>> config
   ) {
      ProtocolInfoBuilder<L, B, C> builder = new ProtocolInfoBuilder<>(id, flow);
      config.accept(builder);
      return builder.buildUnbound();
   }

   public static <T extends ServerboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextServerboundProtocol(
      final ConnectionProtocol id, final Consumer<ProtocolInfoBuilder<T, B, C>> config
   ) {
      return contextProtocol(id, PacketFlow.SERVERBOUND, config);
   }

   public static <T extends ClientboundPacketListener, B extends ByteBuf, C> UnboundProtocol<T, B, C> contextClientboundProtocol(
      final ConnectionProtocol id, final Consumer<ProtocolInfoBuilder<T, B, C>> config
   ) {
      return contextProtocol(id, PacketFlow.CLIENTBOUND, config);
   }

   private record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf, C>(
      PacketType<P> type, StreamCodec<? super B, P> serializer, @Nullable CodecModifier<B, P, C> modifier
   ) {
      public void addToBuilder(final ProtocolCodecBuilder<ByteBuf, T> codecBuilder, final Function<ByteBuf, B> contextWrapper, final C context) {
         StreamCodec<? super B, P> finalSerializer;
         if (this.modifier != null) {
            finalSerializer = this.modifier.apply(this.serializer, context);
         } else {
            finalSerializer = this.serializer;
         }

         StreamCodec<ByteBuf, P> baseCodec = finalSerializer.mapStream(contextWrapper);
         codecBuilder.add(this.type, baseCodec);
      }
   }

   private record Implementation<L extends PacketListener>(
      ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
   ) implements ProtocolInfo<L> {
   }
}
