package net.minecraft.nbt;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractStringBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class NbtOps implements DynamicOps<Tag> {
   public static final NbtOps INSTANCE = new NbtOps();

   private NbtOps() {
   }

   public Tag empty() {
      return EndTag.INSTANCE;
   }

   public Tag emptyList() {
      return new ListTag();
   }

   public Tag emptyMap() {
      return new CompoundTag();
   }

   public <U> U convertTo(DynamicOps<U> p_128980_, Tag p_128981_) {
      return (U)(switch (p_128981_) {
         case EndTag endtag -> p_128980_.empty();
         case ByteTag(byte b0) -> p_128980_.createByte(b0);
         case ShortTag(short short1) -> p_128980_.createShort(short1);
         case IntTag(int i) -> p_128980_.createInt(i);
         case LongTag(long j) -> p_128980_.createLong(j);
         case FloatTag(float f) -> p_128980_.createFloat(f);
         case DoubleTag(double d0) -> p_128980_.createDouble(d0);
         case ByteArrayTag bytearraytag -> p_128980_.createByteList(ByteBuffer.wrap(bytearraytag.getAsByteArray()));
         case StringTag(String s) -> p_128980_.createString(s);
         case ListTag listtag -> this.convertList(p_128980_, listtag);
         case CompoundTag compoundtag -> this.convertMap(p_128980_, compoundtag);
         case IntArrayTag intarraytag -> p_128980_.createIntList(Arrays.stream(intarraytag.getAsIntArray()));
         case LongArrayTag longarraytag -> p_128980_.createLongList(Arrays.stream(longarraytag.getAsLongArray()));
         default -> throw new MatchException(null, null);
      });
   }

   public DataResult<Number> getNumberValue(Tag p_129030_) {
      return p_129030_.asNumber().<DataResult<Number>>map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Not a number"));
   }

   public Tag createNumeric(Number p_128983_) {
      return DoubleTag.valueOf(p_128983_.doubleValue());
   }

   public Tag createByte(byte p_128963_) {
      return ByteTag.valueOf(p_128963_);
   }

   public Tag createShort(short p_129048_) {
      return ShortTag.valueOf(p_129048_);
   }

   public Tag createInt(int p_128976_) {
      return IntTag.valueOf(p_128976_);
   }

   public Tag createLong(long p_128978_) {
      return LongTag.valueOf(p_128978_);
   }

   public Tag createFloat(float p_128974_) {
      return FloatTag.valueOf(p_128974_);
   }

   public Tag createDouble(double p_128972_) {
      return DoubleTag.valueOf(p_128972_);
   }

   public Tag createBoolean(boolean p_129050_) {
      return ByteTag.valueOf(p_129050_);
   }

   public DataResult<String> getStringValue(Tag p_129061_) {
      return p_129061_ instanceof StringTag(String s) ? DataResult.success(s) : DataResult.error(() -> "Not a string");
   }

   public Tag createString(String p_128985_) {
      return StringTag.valueOf(p_128985_);
   }

   public DataResult<Tag> mergeToList(Tag p_129041_, Tag p_129042_) {
      return createCollector(p_129041_)
         .map(p_248053_ -> DataResult.success(p_248053_.accept(p_129042_).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + p_129041_, p_129041_));
   }

   public DataResult<Tag> mergeToList(Tag p_129038_, List<Tag> p_129039_) {
      return createCollector(p_129038_)
         .map(p_248048_ -> DataResult.success(p_248048_.acceptAll(p_129039_).result()))
         .orElseGet(() -> DataResult.error(() -> "mergeToList called with not a list: " + p_129038_, p_129038_));
   }

   public DataResult<Tag> mergeToMap(Tag p_129044_, Tag p_129045_, Tag p_129046_) {
      if (!(p_129044_ instanceof CompoundTag) && !(p_129044_ instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + p_129044_, p_129044_);
      } else if (p_129045_ instanceof StringTag(String s1)) {
         String $$5 = s1;
         CompoundTag compoundtag = p_129044_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
         compoundtag.put($$5, p_129046_);
         return DataResult.success(compoundtag);
      } else {
         return DataResult.error(() -> "key is not a string: " + p_129045_, p_129044_);
      }
   }

   public DataResult<Tag> mergeToMap(Tag p_129032_, MapLike<Tag> p_129033_) {
      if (!(p_129032_ instanceof CompoundTag) && !(p_129032_ instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + p_129032_, p_129032_);
      }

      Iterator<Pair<Tag, Tag>> iterator = p_129033_.entries().iterator();
      if (!iterator.hasNext()) {
         return p_129032_ == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(p_129032_);
      }

      CompoundTag compoundtag = p_129032_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
      List<Tag> list = new ArrayList<>();
      iterator.forEachRemaining(p_389883_ -> {
         Tag tag = (Tag)p_389883_.getFirst();
         if (tag instanceof StringTag(String s)) {
            compoundtag.put(s, (Tag)p_389883_.getSecond());
         } else {
            list.add(tag);
         }
      });
      return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
   }

   public DataResult<Tag> mergeToMap(Tag p_336265_, Map<Tag, Tag> p_331137_) {
      if (!(p_336265_ instanceof CompoundTag) && !(p_336265_ instanceof EndTag)) {
         return DataResult.error(() -> "mergeToMap called with not a map: " + p_336265_, p_336265_);
      }

      if (p_331137_.isEmpty()) {
         return p_336265_ == this.empty() ? DataResult.success(this.emptyMap()) : DataResult.success(p_336265_);
      }

      CompoundTag compoundtag = p_336265_ instanceof CompoundTag compoundtag1 ? compoundtag1.shallowCopy() : new CompoundTag();
      List<Tag> list = new ArrayList<>();

      for (Entry<Tag, Tag> entry : p_331137_.entrySet()) {
         Tag tag = entry.getKey();
         if (tag instanceof StringTag(String s)) {
            compoundtag.put(s, entry.getValue());
         } else {
            list.add(tag);
         }
      }

      return !list.isEmpty() ? DataResult.error(() -> "some keys are not strings: " + list, compoundtag) : DataResult.success(compoundtag);
   }

   public DataResult<Stream<Pair<Tag, Tag>>> getMapValues(Tag p_129070_) {
      return p_129070_ instanceof CompoundTag compoundtag
         ? DataResult.success(compoundtag.entrySet().stream().map(p_326024_ -> Pair.of(this.createString(p_326024_.getKey()), p_326024_.getValue())))
         : DataResult.error(() -> "Not a map: " + p_129070_);
   }

   public DataResult<Consumer<BiConsumer<Tag, Tag>>> getMapEntries(Tag p_129103_) {
      return p_129103_ instanceof CompoundTag compoundtag ? DataResult.success((Consumer<BiConsumer>)p_326020_ -> {
         for (Entry<String, Tag> entry : compoundtag.entrySet()) {
            p_326020_.accept(this.createString(entry.getKey()), entry.getValue());
         }
      }) : DataResult.error(() -> "Not a map: " + p_129103_);
   }

   public DataResult<MapLike<Tag>> getMap(Tag p_129105_) {
      return p_129105_ instanceof CompoundTag compoundtag ? DataResult.success(new MapLike<Tag>() {
         public @Nullable Tag get(Tag p_129174_) {
            if (p_129174_ instanceof StringTag(String s)) {
               return compoundtag.get(s);
            } else {
               throw new UnsupportedOperationException("Cannot get map entry with non-string key: " + p_129174_);
            }
         }

         public @Nullable Tag get(String p_129169_) {
            return compoundtag.get(p_129169_);
         }

         public Stream<Pair<Tag, Tag>> entries() {
            return compoundtag.entrySet().stream().map(p_326034_ -> Pair.of(NbtOps.this.createString(p_326034_.getKey()), p_326034_.getValue()));
         }

         @Override
         public String toString() {
            return "MapLike[" + compoundtag + "]";
         }
      }) : DataResult.error(() -> "Not a map: " + p_129105_);
   }

   public Tag createMap(Stream<Pair<Tag, Tag>> p_129004_) {
      CompoundTag compoundtag = new CompoundTag();
      p_129004_.forEach(p_389880_ -> {
         Tag tag = (Tag)p_389880_.getFirst();
         Tag tag1 = (Tag)p_389880_.getSecond();
         if (tag instanceof StringTag(String s)) {
            compoundtag.put(s, tag1);
         } else {
            throw new UnsupportedOperationException("Cannot create map with non-string key: " + tag);
         }
      });
      return compoundtag;
   }

   public DataResult<Stream<Tag>> getStream(Tag p_129108_) {
      return p_129108_ instanceof CollectionTag collectiontag ? DataResult.success(collectiontag.stream()) : DataResult.error(() -> "Not a list");
   }

   public DataResult<Consumer<Consumer<Tag>>> getList(Tag p_129110_) {
      return p_129110_ instanceof CollectionTag collectiontag ? DataResult.success(collectiontag::forEach) : DataResult.error(() -> "Not a list: " + p_129110_);
   }

   public DataResult<ByteBuffer> getByteBuffer(Tag p_129132_) {
      return p_129132_ instanceof ByteArrayTag bytearraytag
         ? DataResult.success(ByteBuffer.wrap(bytearraytag.getAsByteArray()))
         : super.getByteBuffer(p_129132_);
   }

   public Tag createByteList(ByteBuffer p_128990_) {
      ByteBuffer bytebuffer = p_128990_.duplicate().clear();
      byte[] abyte = new byte[p_128990_.capacity()];
      bytebuffer.get(0, abyte, 0, abyte.length);
      return new ByteArrayTag(abyte);
   }

   public DataResult<IntStream> getIntStream(Tag p_129134_) {
      return p_129134_ instanceof IntArrayTag intarraytag ? DataResult.success(Arrays.stream(intarraytag.getAsIntArray())) : super.getIntStream(p_129134_);
   }

   public Tag createIntList(IntStream p_129000_) {
      return new IntArrayTag(p_129000_.toArray());
   }

   public DataResult<LongStream> getLongStream(Tag p_129136_) {
      return p_129136_ instanceof LongArrayTag longarraytag ? DataResult.success(Arrays.stream(longarraytag.getAsLongArray())) : super.getLongStream(p_129136_);
   }

   public Tag createLongList(LongStream p_129002_) {
      return new LongArrayTag(p_129002_.toArray());
   }

   public Tag createList(Stream<Tag> p_129052_) {
      return new ListTag(p_129052_.collect(Util.toMutableList()));
   }

   public Tag remove(Tag p_129035_, String p_129036_) {
      if (p_129035_ instanceof CompoundTag compoundtag) {
         CompoundTag compoundtag1 = compoundtag.shallowCopy();
         compoundtag1.remove(p_129036_);
         return compoundtag1;
      } else {
         return p_129035_;
      }
   }

   @Override
   public String toString() {
      return "NBT";
   }

   public RecordBuilder<Tag> mapBuilder() {
      return new NbtOps.NbtRecordBuilder();
   }

   private static Optional<NbtOps.ListCollector> createCollector(Tag p_249503_) {
      if (p_249503_ instanceof EndTag) {
         return Optional.of(new NbtOps.GenericListCollector());
      }

      if (p_249503_ instanceof CollectionTag collectiontag) {
         if (collectiontag.isEmpty()) {
            return Optional.of(new NbtOps.GenericListCollector());
         }

         return switch (collectiontag) {
            case ListTag listtag -> Optional.of(new NbtOps.GenericListCollector(listtag));
            case ByteArrayTag bytearraytag -> Optional.of(new NbtOps.ByteListCollector(bytearraytag.getAsByteArray()));
            case IntArrayTag intarraytag -> Optional.of(new NbtOps.IntListCollector(intarraytag.getAsIntArray()));
            case LongArrayTag longarraytag -> Optional.of(new NbtOps.LongListCollector(longarraytag.getAsLongArray()));
            default -> throw new MatchException(null, null);
         };
      } else {
         return Optional.empty();
      }
   }

   static class ByteListCollector implements NbtOps.ListCollector {
      private final ByteArrayList values = new ByteArrayList();

      public ByteListCollector(byte[] p_250457_) {
         this.values.addElements(0, p_250457_);
      }

      @Override
      public NbtOps.ListCollector accept(Tag p_250723_) {
         if (p_250723_ instanceof ByteTag bytetag) {
            this.values.add(bytetag.byteValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(p_250723_);
         }
      }

      @Override
      public Tag result() {
         return new ByteArrayTag(this.values.toByteArray());
      }
   }

   static class GenericListCollector implements NbtOps.ListCollector {
      private final ListTag result = new ListTag();

      GenericListCollector() {
      }

      GenericListCollector(ListTag p_397028_) {
         this.result.addAll(p_397028_);
      }

      public GenericListCollector(IntArrayList p_396566_) {
         p_396566_.forEach(p_393744_ -> this.result.add(IntTag.valueOf(p_393744_)));
      }

      public GenericListCollector(ByteArrayList p_393005_) {
         p_393005_.forEach(p_393979_ -> this.result.add(ByteTag.valueOf(p_393979_)));
      }

      public GenericListCollector(LongArrayList p_392062_) {
         p_392062_.forEach(p_395643_ -> this.result.add(LongTag.valueOf(p_395643_)));
      }

      @Override
      public NbtOps.ListCollector accept(Tag p_391617_) {
         this.result.add(p_391617_);
         return this;
      }

      @Override
      public Tag result() {
         return this.result;
      }
   }

   static class IntListCollector implements NbtOps.ListCollector {
      private final IntArrayList values = new IntArrayList();

      public IntListCollector(int[] p_249489_) {
         this.values.addElements(0, p_249489_);
      }

      @Override
      public NbtOps.ListCollector accept(Tag p_251372_) {
         if (p_251372_ instanceof IntTag inttag) {
            this.values.add(inttag.intValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(p_251372_);
         }
      }

      @Override
      public Tag result() {
         return new IntArrayTag(this.values.toIntArray());
      }
   }

   interface ListCollector {
      NbtOps.ListCollector accept(Tag var1);

      default NbtOps.ListCollector acceptAll(Iterable<Tag> p_249781_) {
         NbtOps.ListCollector nbtops$listcollector = this;

         for (Tag tag : p_249781_) {
            nbtops$listcollector = nbtops$listcollector.accept(tag);
         }

         return nbtops$listcollector;
      }

      default NbtOps.ListCollector acceptAll(Stream<Tag> p_249876_) {
         return this.acceptAll(p_249876_::iterator);
      }

      Tag result();
   }

   static class LongListCollector implements NbtOps.ListCollector {
      private final LongArrayList values = new LongArrayList();

      public LongListCollector(long[] p_251409_) {
         this.values.addElements(0, p_251409_);
      }

      @Override
      public NbtOps.ListCollector accept(Tag p_252167_) {
         if (p_252167_ instanceof LongTag longtag) {
            this.values.add(longtag.longValue());
            return this;
         } else {
            return new NbtOps.GenericListCollector(this.values).accept(p_252167_);
         }
      }

      @Override
      public Tag result() {
         return new LongArrayTag(this.values.toLongArray());
      }
   }

   class NbtRecordBuilder extends AbstractStringBuilder<Tag, CompoundTag> {
      protected NbtRecordBuilder() {
         super(NbtOps.this);
      }

      protected CompoundTag initBuilder() {
         return new CompoundTag();
      }

      protected CompoundTag append(String p_129186_, Tag p_129187_, CompoundTag p_129188_) {
         p_129188_.put(p_129186_, p_129187_);
         return p_129188_;
      }

      protected DataResult<Tag> build(CompoundTag p_129190_, Tag p_129191_) {
         if (p_129191_ == null || p_129191_ == EndTag.INSTANCE) {
            return DataResult.success(p_129190_);
         } else if (!(p_129191_ instanceof CompoundTag compoundtag)) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + p_129191_, p_129191_);
         } else {
            CompoundTag compoundtag1 = compoundtag.shallowCopy();

            for (Entry<String, Tag> entry : p_129190_.entrySet()) {
               compoundtag1.put(entry.getKey(), entry.getValue());
            }

            return DataResult.success(compoundtag1);
         }
      }
   }
}
