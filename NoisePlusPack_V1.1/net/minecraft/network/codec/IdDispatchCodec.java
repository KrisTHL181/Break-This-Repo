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

   IdDispatchCodec(Function<V, ? extends T> p_330610_, List<IdDispatchCodec.Entry<B, V, T>> p_334834_, Object2IntMap<T> p_327784_) {
      this.typeGetter = p_330610_;
      this.byId = p_334834_;
      this.toId = p_327784_;
   }

   public V decode(B p_327793_) {
      int i = VarInt.read(p_327793_);
      if (i >= 0 && i < this.byId.size()) {
         IdDispatchCodec.Entry<B, V, T> entry = this.byId.get(i);

         try {
            return (V)entry.serializer.decode(p_327793_);
         } catch (Exception exception) {
            if (exception instanceof IdDispatchCodec.DontDecorateException) {
               throw exception;
            } else {
               throw new DecoderException("Failed to decode packet '" + entry.type + "'", exception);
            }
         }
      } else {
         throw new DecoderException("Received unknown packet id " + i);
      }
   }

   public void encode(B p_336072_, V p_327912_) {
      T t = (T)this.typeGetter.apply(p_327912_);
      int i = this.toId.getOrDefault(t, -1);
      if (i == -1) {
         throw new EncoderException("Sending unknown packet '" + t + "'");
      }

      VarInt.write(p_336072_, i);
      IdDispatchCodec.Entry<B, V, T> entry = this.byId.get(i);

      try {
         StreamCodec<? super B, V> streamcodec = (StreamCodec<? super B, V>)entry.serializer;
         streamcodec.encode(p_336072_, p_327912_);
      } catch (Exception exception) {
         if (exception instanceof IdDispatchCodec.DontDecorateException) {
            throw exception;
         } else {
            throw new EncoderException("Failed to encode packet '" + t + "'", exception);
         }
      }
   }

   public static <B extends ByteBuf, V, T> IdDispatchCodec.Builder<B, V, T> builder(Function<V, ? extends T> p_331962_) {
      return new IdDispatchCodec.Builder<>(p_331962_);
   }

   public static class Builder<B extends ByteBuf, V, T> {
      private final List<IdDispatchCodec.Entry<B, V, T>> entries = new ArrayList<>();
      private final Function<V, ? extends T> typeGetter;

      Builder(Function<V, ? extends T> p_330341_) {
         this.typeGetter = p_330341_;
      }

      public IdDispatchCodec.Builder<B, V, T> add(T p_333313_, StreamCodec<? super B, ? extends V> p_330239_) {
         this.entries.add(new IdDispatchCodec.Entry<>(p_330239_, p_333313_));
         return this;
      }

      public IdDispatchCodec<B, V, T> build() {
         Object2IntOpenHashMap<T> object2intopenhashmap = new Object2IntOpenHashMap();
         object2intopenhashmap.defaultReturnValue(-2);

         for (IdDispatchCodec.Entry<B, V, T> entry : this.entries) {
            int i = object2intopenhashmap.size();
            int j = object2intopenhashmap.putIfAbsent(entry.type, i);
            if (j != -2) {
               throw new IllegalStateException("Duplicate registration for type " + entry.type);
            }
         }

         return new IdDispatchCodec<>(this.typeGetter, List.copyOf(this.entries), object2intopenhashmap);
      }
   }

   public interface DontDecorateException {
   }

   record Entry<B, V, T>(StreamCodec<? super B, ? extends V> serializer, T type) {
   }
}
