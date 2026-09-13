package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class DelegatingOps<T> implements DynamicOps<T> {
   protected final DynamicOps<T> delegate;

   protected DelegatingOps(DynamicOps<T> p_135467_) {
      this.delegate = p_135467_;
   }

   public T empty() {
      return (T)this.delegate.empty();
   }

   public T emptyMap() {
      return (T)this.delegate.emptyMap();
   }

   public T emptyList() {
      return (T)this.delegate.emptyList();
   }

   public <U> U convertTo(DynamicOps<U> p_135470_, T p_135471_) {
      return (U)(Objects.equals(p_135470_, this.delegate) ? p_135471_ : this.delegate.convertTo(p_135470_, p_135471_));
   }

   public DataResult<Number> getNumberValue(T p_135518_) {
      return this.delegate.getNumberValue(p_135518_);
   }

   public T createNumeric(Number p_135495_) {
      return (T)this.delegate.createNumeric(p_135495_);
   }

   public T createByte(byte p_135475_) {
      return (T)this.delegate.createByte(p_135475_);
   }

   public T createShort(short p_135497_) {
      return (T)this.delegate.createShort(p_135497_);
   }

   public T createInt(int p_135483_) {
      return (T)this.delegate.createInt(p_135483_);
   }

   public T createLong(long p_135489_) {
      return (T)this.delegate.createLong(p_135489_);
   }

   public T createFloat(float p_135481_) {
      return (T)this.delegate.createFloat(p_135481_);
   }

   public T createDouble(double p_135479_) {
      return (T)this.delegate.createDouble(p_135479_);
   }

   public DataResult<Boolean> getBooleanValue(T p_135502_) {
      return this.delegate.getBooleanValue(p_135502_);
   }

   public T createBoolean(boolean p_135473_) {
      return (T)this.delegate.createBoolean(p_135473_);
   }

   public DataResult<String> getStringValue(T p_135522_) {
      return this.delegate.getStringValue(p_135522_);
   }

   public T createString(String p_135499_) {
      return (T)this.delegate.createString(p_135499_);
   }

   public DataResult<T> mergeToList(T p_135526_, T p_135527_) {
      return this.delegate.mergeToList(p_135526_, p_135527_);
   }

   public DataResult<T> mergeToList(T p_135529_, List<T> p_135530_) {
      return this.delegate.mergeToList(p_135529_, p_135530_);
   }

   public DataResult<T> mergeToMap(T p_135535_, T p_135536_, T p_135537_) {
      return this.delegate.mergeToMap(p_135535_, p_135536_, p_135537_);
   }

   public DataResult<T> mergeToMap(T p_135532_, MapLike<T> p_135533_) {
      return this.delegate.mergeToMap(p_135532_, p_135533_);
   }

   public DataResult<T> mergeToMap(T p_335567_, Map<T, T> p_327772_) {
      return this.delegate.mergeToMap(p_335567_, p_327772_);
   }

   public DataResult<T> mergeToPrimitive(T p_330125_, T p_335137_) {
      return this.delegate.mergeToPrimitive(p_330125_, p_335137_);
   }

   public DataResult<Stream<Pair<T, T>>> getMapValues(T p_135516_) {
      return this.delegate.getMapValues(p_135516_);
   }

   public DataResult<Consumer<BiConsumer<T, T>>> getMapEntries(T p_135514_) {
      return this.delegate.getMapEntries(p_135514_);
   }

   public T createMap(Map<T, T> p_336013_) {
      return (T)this.delegate.createMap(p_336013_);
   }

   public T createMap(Stream<Pair<T, T>> p_135493_) {
      return (T)this.delegate.createMap(p_135493_);
   }

   public DataResult<MapLike<T>> getMap(T p_135512_) {
      return this.delegate.getMap(p_135512_);
   }

   public DataResult<Stream<T>> getStream(T p_135520_) {
      return this.delegate.getStream(p_135520_);
   }

   public DataResult<Consumer<Consumer<T>>> getList(T p_135508_) {
      return this.delegate.getList(p_135508_);
   }

   public T createList(Stream<T> p_135487_) {
      return (T)this.delegate.createList(p_135487_);
   }

   public DataResult<ByteBuffer> getByteBuffer(T p_135504_) {
      return this.delegate.getByteBuffer(p_135504_);
   }

   public T createByteList(ByteBuffer p_135477_) {
      return (T)this.delegate.createByteList(p_135477_);
   }

   public DataResult<IntStream> getIntStream(T p_135506_) {
      return this.delegate.getIntStream(p_135506_);
   }

   public T createIntList(IntStream p_135485_) {
      return (T)this.delegate.createIntList(p_135485_);
   }

   public DataResult<LongStream> getLongStream(T p_135510_) {
      return this.delegate.getLongStream(p_135510_);
   }

   public T createLongList(LongStream p_135491_) {
      return (T)this.delegate.createLongList(p_135491_);
   }

   public T remove(T p_135539_, String p_135540_) {
      return (T)this.delegate.remove(p_135539_, p_135540_);
   }

   public boolean compressMaps() {
      return this.delegate.compressMaps();
   }

   public ListBuilder<T> listBuilder() {
      return new DelegatingOps.DelegateListBuilder(this.delegate.listBuilder());
   }

   public RecordBuilder<T> mapBuilder() {
      return new DelegatingOps.DelegateRecordBuilder(this.delegate.mapBuilder());
   }

   protected class DelegateListBuilder implements ListBuilder<T> {
      private final ListBuilder<T> original;

      protected DelegateListBuilder(final ListBuilder<T> p_395521_) {
         this.original = p_395521_;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public DataResult<T> build(T p_393497_) {
         return this.original.build(p_393497_);
      }

      public ListBuilder<T> add(T p_397871_) {
         this.original.add(p_397871_);
         return this;
      }

      public ListBuilder<T> add(DataResult<T> p_393700_) {
         this.original.add(p_393700_);
         return this;
      }

      public <E> ListBuilder<T> add(E p_393952_, Encoder<E> p_393440_) {
         this.original.add(p_393440_.encodeStart(this.ops(), p_393952_));
         return this;
      }

      public <E> ListBuilder<T> addAll(Iterable<E> p_396633_, Encoder<E> p_396833_) {
         p_396633_.forEach(p_395457_ -> this.original.add(p_396833_.encode(p_395457_, this.ops(), this.ops().empty())));
         return this;
      }

      public ListBuilder<T> withErrorsFrom(DataResult<?> p_393779_) {
         this.original.withErrorsFrom(p_393779_);
         return this;
      }

      public ListBuilder<T> mapError(UnaryOperator<String> p_394496_) {
         this.original.mapError(p_394496_);
         return this;
      }

      public DataResult<T> build(DataResult<T> p_392507_) {
         return this.original.build(p_392507_);
      }
   }

   protected class DelegateRecordBuilder implements RecordBuilder<T> {
      private final RecordBuilder<T> original;

      protected DelegateRecordBuilder(final RecordBuilder<T> p_397773_) {
         this.original = p_397773_;
      }

      public DynamicOps<T> ops() {
         return DelegatingOps.this;
      }

      public RecordBuilder<T> add(T p_391780_, T p_393356_) {
         this.original.add(p_391780_, p_393356_);
         return this;
      }

      public RecordBuilder<T> add(T p_391456_, DataResult<T> p_395058_) {
         this.original.add(p_391456_, p_395058_);
         return this;
      }

      public RecordBuilder<T> add(DataResult<T> p_394017_, DataResult<T> p_393031_) {
         this.original.add(p_394017_, p_393031_);
         return this;
      }

      public RecordBuilder<T> add(String p_393508_, T p_391722_) {
         this.original.add(p_393508_, p_391722_);
         return this;
      }

      public RecordBuilder<T> add(String p_396615_, DataResult<T> p_397821_) {
         this.original.add(p_396615_, p_397821_);
         return this;
      }

      public <E> RecordBuilder<T> add(String p_397286_, E p_391361_, Encoder<E> p_397772_) {
         return this.original.add(p_397286_, p_397772_.encodeStart(this.ops(), p_391361_));
      }

      public RecordBuilder<T> withErrorsFrom(DataResult<?> p_391664_) {
         this.original.withErrorsFrom(p_391664_);
         return this;
      }

      public RecordBuilder<T> setLifecycle(Lifecycle p_392189_) {
         this.original.setLifecycle(p_392189_);
         return this;
      }

      public RecordBuilder<T> mapError(UnaryOperator<String> p_391221_) {
         this.original.mapError(p_391221_);
         return this;
      }

      public DataResult<T> build(T p_397011_) {
         return this.original.build(p_397011_);
      }

      public DataResult<T> build(DataResult<T> p_392992_) {
         return this.original.build(p_392992_);
      }
   }
}
