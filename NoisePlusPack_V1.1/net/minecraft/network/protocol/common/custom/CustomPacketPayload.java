package net.minecraft.network.protocol.common.custom;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.resources.Identifier;

public interface CustomPacketPayload {
   CustomPacketPayload.Type<? extends CustomPacketPayload> type();

   static <B extends ByteBuf, T extends CustomPacketPayload> StreamCodec<B, T> codec(StreamMemberEncoder<B, T> p_336135_, StreamDecoder<B, T> p_335771_) {
      return StreamCodec.ofMember(p_336135_, p_335771_);
   }

   static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> createType(String p_331650_) {
      return new CustomPacketPayload.Type<>(Identifier.withDefaultNamespace(p_331650_));
   }

   static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> codec(
      final CustomPacketPayload.FallbackProvider<B> p_329573_, List<CustomPacketPayload.TypeAndCodec<? super B, ?>> p_333081_
   ) {
      final Map<Identifier, StreamCodec<? super B, ? extends CustomPacketPayload>> map = p_333081_.stream()
         .collect(Collectors.toUnmodifiableMap(p_448779_ -> p_448779_.type().id(), CustomPacketPayload.TypeAndCodec::codec));
      return new StreamCodec<B, CustomPacketPayload>() {
         private StreamCodec<? super B, ? extends CustomPacketPayload> findCodec(Identifier p_459672_) {
            StreamCodec<? super B, ? extends CustomPacketPayload> streamcodec = map.get(p_459672_);
            return streamcodec != null ? streamcodec : p_329573_.create(p_459672_);
         }

         private <T extends CustomPacketPayload> void writeCap(B p_332252_, CustomPacketPayload.Type<T> p_334465_, CustomPacketPayload p_334290_) {
            p_332252_.writeIdentifier(p_334465_.id());
            StreamCodec<B, T> streamcodec = this.findCodec(p_334465_.id);
            streamcodec.encode(p_332252_, (T)p_334290_);
         }

         public void encode(B p_334992_, CustomPacketPayload p_329854_) {
            this.writeCap(p_334992_, p_329854_.type(), p_329854_);
         }

         public CustomPacketPayload decode(B p_334320_) {
            Identifier identifier = p_334320_.readIdentifier();
            return (CustomPacketPayload)this.findCodec(identifier).decode(p_334320_);
         }
      };
   }

   interface FallbackProvider<B extends FriendlyByteBuf> {
      StreamCodec<B, ? extends CustomPacketPayload> create(Identifier var1);
   }

   record Type<T extends CustomPacketPayload>(Identifier id) {
   }

   record TypeAndCodec<B extends FriendlyByteBuf, T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<B, T> codec) {
   }
}
