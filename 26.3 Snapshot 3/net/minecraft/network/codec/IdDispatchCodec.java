package net.minecraft.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.VarInt;

public class IdDispatchCodec<B extends ByteBuf, V, T> implements StreamCodec<B, V> {
   private static final int UNKNOWN_TYPE = -1;
   private final Function<V, ? extends T> typeGetter;
   private final List<IdDispatchCodec.Entry<B, V, T>> byId;
   private final Object2IntMap<T> toId;

   private IdDispatchCodec(final Function<V, ? extends T> typeGetter, final List<IdDispatchCodec.Entry<B, V, T>> byId, final Object2IntMap<T> toId) {
      this.typeGetter = typeGetter;
      this.byId = byId;
      this.toId = toId;
   }

   public V decode(final B input) {
      int id = VarInt.read(input);
      if (id >= 0 && id < this.byId.size()) {
         IdDispatchCodec.Entry<B, V, T> entry = this.byId.get(id);

         try {
            return (V)entry.serializer.decode(input);
         } catch (Exception e) {
            if (e instanceof IdDispatchCodec.DontDecorateException) {
               throw e;
            } else {
               throw new DecoderException("Failed to decode packet '" + entry.type + "'", e);
            }
         }
      } else {
         throw new DecoderException("Received unknown packet id " + id);
      }
   }

   public void encode(final B output, final V value) {
      T type = (T)this.typeGetter.apply(value);
      int id = this.toId.getOrDefault(type, -1);
      if (id == -1) {
         throw new EncoderException("Sending unknown packet '" + type + "'");
      }

      VarInt.write(output, id);
      IdDispatchCodec.Entry<B, V, T> entry = this.byId.get(id);

      try {
         StreamCodec<? super B, V> codec = (StreamCodec<? super B, V>)entry.serializer;
         codec.encode(output, value);
      } catch (Exception e) {
         if (e instanceof IdDispatchCodec.DontDecorateException) {
            throw e;
         } else {
            throw new EncoderException("Failed to encode packet '" + type + "'", e);
         }
      }
   }

   public static <B extends ByteBuf, V, T> IdDispatchCodec.Builder<B, V, T> builder(final Function<V, ? extends T> typeGetter) {
      return new IdDispatchCodec.Builder<>(typeGetter);
   }

   public static class Builder<B extends ByteBuf, V, T> {
      private final List<IdDispatchCodec.Entry<B, V, T>> entries = new ArrayList<>();
      private final Function<V, ? extends T> typeGetter;

      private Builder(final Function<V, ? extends T> typeGetter) {
         this.typeGetter = typeGetter;
      }

      public IdDispatchCodec.Builder<B, V, T> add(final T type, final StreamCodec<? super B, ? extends V> serializer) {
         this.entries.add(new IdDispatchCodec.Entry<>(serializer, type));
         return this;
      }

      public IdDispatchCodec<B, V, T> build() {
         Object2IntOpenHashMap<T> toId = new Object2IntOpenHashMap();
         toId.defaultReturnValue(-2);

         for (IdDispatchCodec.Entry<B, V, T> entry : this.entries) {
            int id = toId.size();
            int previous = toId.putIfAbsent(entry.type, id);
            if (previous != -2) {
               throw new IllegalStateException("Duplicate registration for type " + entry.type);
            }
         }

         return new IdDispatchCodec<>(this.typeGetter, List.copyOf(this.entries), toId);
      }
   }

   public interface DontDecorateException {
   }

   private record Entry<B, V, T>(StreamCodec<? super B, ? extends V> serializer, T type) {
   }
}
