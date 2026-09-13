package net.minecraft.util;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class HashOps implements DynamicOps<HashCode> {
   private static final byte TAG_EMPTY = 1;
   private static final byte TAG_MAP_START = 2;
   private static final byte TAG_MAP_END = 3;
   private static final byte TAG_LIST_START = 4;
   private static final byte TAG_LIST_END = 5;
   private static final byte TAG_BYTE = 6;
   private static final byte TAG_SHORT = 7;
   private static final byte TAG_INT = 8;
   private static final byte TAG_LONG = 9;
   private static final byte TAG_FLOAT = 10;
   private static final byte TAG_DOUBLE = 11;
   private static final byte TAG_STRING = 12;
   private static final byte TAG_BOOLEAN = 13;
   private static final byte TAG_BYTE_ARRAY_START = 14;
   private static final byte TAG_BYTE_ARRAY_END = 15;
   private static final byte TAG_INT_ARRAY_START = 16;
   private static final byte TAG_INT_ARRAY_END = 17;
   private static final byte TAG_LONG_ARRAY_START = 18;
   private static final byte TAG_LONG_ARRAY_END = 19;
   private static final byte[] EMPTY_PAYLOAD = new byte[]{1};
   private static final byte[] FALSE_PAYLOAD = new byte[]{13, 0};
   private static final byte[] TRUE_PAYLOAD = new byte[]{13, 1};
   public static final byte[] EMPTY_MAP_PAYLOAD = new byte[]{2, 3};
   public static final byte[] EMPTY_LIST_PAYLOAD = new byte[]{4, 5};
   private static final DataResult<Object> UNSUPPORTED_OPERATION_ERROR = DataResult.error(() -> "Unsupported operation");
   private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
   private static final Comparator<Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Entry.<HashCode, HashCode>comparingByKey(HASH_COMPARATOR)
      .thenComparing(Entry.comparingByValue(HASH_COMPARATOR));
   private static final Comparator<Pair<HashCode, HashCode>> MAPLIKE_ENTRY_ORDER = Comparator.<Pair<HashCode, HashCode>, HashCode>comparing(
         Pair::getFirst, HASH_COMPARATOR
      )
      .thenComparing(Pair::getSecond, HASH_COMPARATOR);
   public static final HashOps CRC32C_INSTANCE = new HashOps(Hashing.crc32c());
   final HashFunction hashFunction;
   final HashCode empty;
   private final HashCode emptyMap;
   private final HashCode emptyList;
   private final HashCode trueHash;
   private final HashCode falseHash;

   public HashOps(HashFunction p_397329_) {
      this.hashFunction = p_397329_;
      this.empty = p_397329_.hashBytes(EMPTY_PAYLOAD);
      this.emptyMap = p_397329_.hashBytes(EMPTY_MAP_PAYLOAD);
      this.emptyList = p_397329_.hashBytes(EMPTY_LIST_PAYLOAD);
      this.falseHash = p_397329_.hashBytes(FALSE_PAYLOAD);
      this.trueHash = p_397329_.hashBytes(TRUE_PAYLOAD);
   }

   public HashCode empty() {
      return this.empty;
   }

   public HashCode emptyMap() {
      return this.emptyMap;
   }

   public HashCode emptyList() {
      return this.emptyList;
   }

   public HashCode createNumeric(Number p_397688_) {
      return switch (p_397688_) {
         case Byte obyte -> this.createByte(obyte);
         case Short oshort -> this.createShort(oshort);
         case Integer integer -> this.createInt(integer);
         case Long olong -> this.createLong(olong);
         case Double d0 -> this.createDouble(d0);
         case Float f -> this.createFloat(f);
         default -> this.createDouble(p_397688_.doubleValue());
      };
   }

   public HashCode createByte(byte p_396691_) {
      return this.hashFunction.newHasher(2).putByte((byte)6).putByte(p_396691_).hash();
   }

   public HashCode createShort(short p_392813_) {
      return this.hashFunction.newHasher(3).putByte((byte)7).putShort(p_392813_).hash();
   }

   public HashCode createInt(int p_395249_) {
      return this.hashFunction.newHasher(5).putByte((byte)8).putInt(p_395249_).hash();
   }

   public HashCode createLong(long p_392135_) {
      return this.hashFunction.newHasher(9).putByte((byte)9).putLong(p_392135_).hash();
   }

   public HashCode createFloat(float p_394026_) {
      return this.hashFunction.newHasher(5).putByte((byte)10).putFloat(p_394026_).hash();
   }

   public HashCode createDouble(double p_394179_) {
      return this.hashFunction.newHasher(9).putByte((byte)11).putDouble(p_394179_).hash();
   }

   public HashCode createString(String p_392797_) {
      return this.hashFunction.newHasher().putByte((byte)12).putInt(p_392797_.length()).putUnencodedChars(p_392797_).hash();
   }

   public HashCode createBoolean(boolean p_397569_) {
      return p_397569_ ? this.trueHash : this.falseHash;
   }

   private static Hasher hashMap(Hasher p_397613_, Map<HashCode, HashCode> p_396942_) {
      p_397613_.putByte((byte)2);
      p_396942_.entrySet()
         .stream()
         .sorted(MAP_ENTRY_ORDER)
         .forEach(p_392920_ -> p_397613_.putBytes(p_392920_.getKey().asBytes()).putBytes(p_392920_.getValue().asBytes()));
      p_397613_.putByte((byte)3);
      return p_397613_;
   }

   static Hasher hashMap(Hasher p_392551_, Stream<Pair<HashCode, HashCode>> p_398023_) {
      p_392551_.putByte((byte)2);
      p_398023_.sorted(MAPLIKE_ENTRY_ORDER)
         .forEach(p_392939_ -> p_392551_.putBytes(((HashCode)p_392939_.getFirst()).asBytes()).putBytes(((HashCode)p_392939_.getSecond()).asBytes()));
      p_392551_.putByte((byte)3);
      return p_392551_;
   }

   public HashCode createMap(Stream<Pair<HashCode, HashCode>> p_396656_) {
      return hashMap(this.hashFunction.newHasher(), p_396656_).hash();
   }

   public HashCode createMap(Map<HashCode, HashCode> p_393057_) {
      return hashMap(this.hashFunction.newHasher(), p_393057_).hash();
   }

   public HashCode createList(Stream<HashCode> p_398039_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)4);
      p_398039_.forEach(p_397145_ -> hasher.putBytes(p_397145_.asBytes()));
      hasher.putByte((byte)5);
      return hasher.hash();
   }

   public HashCode createByteList(ByteBuffer p_393066_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)14);
      hasher.putBytes(p_393066_);
      hasher.putByte((byte)15);
      return hasher.hash();
   }

   public HashCode createIntList(IntStream p_392378_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)16);
      p_392378_.forEach(hasher::putInt);
      hasher.putByte((byte)17);
      return hasher.hash();
   }

   public HashCode createLongList(LongStream p_395866_) {
      Hasher hasher = this.hashFunction.newHasher();
      hasher.putByte((byte)18);
      p_395866_.forEach(hasher::putLong);
      hasher.putByte((byte)19);
      return hasher.hash();
   }

   public HashCode remove(HashCode p_394547_, String p_395616_) {
      return p_394547_;
   }

   public RecordBuilder<HashCode> mapBuilder() {
      return new HashOps.MapHashBuilder();
   }

   public ListBuilder<HashCode> listBuilder() {
      return new HashOps.ListHashBuilder();
   }

   @Override
   public String toString() {
      return "Hash " + this.hashFunction;
   }

   public <U> U convertTo(DynamicOps<U> p_394392_, HashCode p_396939_) {
      throw new UnsupportedOperationException("Can't convert from this type");
   }

   public Number getNumberValue(HashCode p_396836_, Number p_396683_) {
      return p_396683_;
   }

   public HashCode set(HashCode p_392609_, String p_394861_, HashCode p_396034_) {
      return p_392609_;
   }

   public HashCode update(HashCode p_393943_, String p_397765_, Function<HashCode, HashCode> p_393638_) {
      return p_393943_;
   }

   public HashCode updateGeneric(HashCode p_394295_, HashCode p_397939_, Function<HashCode, HashCode> p_391335_) {
      return p_394295_;
   }

   private static <T> DataResult<T> unsupported() {
      return (DataResult<T>)UNSUPPORTED_OPERATION_ERROR;
   }

   public DataResult<HashCode> get(HashCode p_397791_, String p_393212_) {
      return unsupported();
   }

   public DataResult<HashCode> getGeneric(HashCode p_392165_, HashCode p_395432_) {
      return unsupported();
   }

   public DataResult<Number> getNumberValue(HashCode p_394009_) {
      return unsupported();
   }

   public DataResult<Boolean> getBooleanValue(HashCode p_397324_) {
      return unsupported();
   }

   public DataResult<String> getStringValue(HashCode p_397603_) {
      return unsupported();
   }

   boolean isEmpty(HashCode p_460752_) {
      return p_460752_.equals(this.empty);
   }

   public DataResult<HashCode> mergeToList(HashCode p_395328_, HashCode p_397519_) {
      return this.isEmpty(p_395328_) ? DataResult.success(this.createList(Stream.of(p_397519_))) : unsupported();
   }

   public DataResult<HashCode> mergeToList(HashCode p_392335_, List<HashCode> p_393705_) {
      return this.isEmpty(p_392335_) ? DataResult.success(this.createList(p_393705_.stream())) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_397103_, HashCode p_393644_, HashCode p_397786_) {
      return this.isEmpty(p_397103_) ? DataResult.success(this.createMap(Map.of(p_393644_, p_397786_))) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_394828_, Map<HashCode, HashCode> p_392230_) {
      return this.isEmpty(p_394828_) ? DataResult.success(this.createMap(p_392230_)) : unsupported();
   }

   public DataResult<HashCode> mergeToMap(HashCode p_393002_, MapLike<HashCode> p_397657_) {
      return this.isEmpty(p_393002_) ? DataResult.success(this.createMap(p_397657_.entries())) : unsupported();
   }

   public DataResult<Stream<Pair<HashCode, HashCode>>> getMapValues(HashCode p_394873_) {
      return unsupported();
   }

   public DataResult<Consumer<BiConsumer<HashCode, HashCode>>> getMapEntries(HashCode p_395753_) {
      return unsupported();
   }

   public DataResult<Stream<HashCode>> getStream(HashCode p_397363_) {
      return unsupported();
   }

   public DataResult<Consumer<Consumer<HashCode>>> getList(HashCode p_391333_) {
      return unsupported();
   }

   public DataResult<MapLike<HashCode>> getMap(HashCode p_391961_) {
      return unsupported();
   }

   public DataResult<ByteBuffer> getByteBuffer(HashCode p_393051_) {
      return unsupported();
   }

   public DataResult<IntStream> getIntStream(HashCode p_395829_) {
      return unsupported();
   }

   public DataResult<LongStream> getLongStream(HashCode p_392986_) {
      return unsupported();
   }

   class ListHashBuilder extends AbstractListBuilder<HashCode, Hasher> {
      public ListHashBuilder() {
         super(HashOps.this);
      }

      protected Hasher initBuilder() {
         return HashOps.this.hashFunction.newHasher().putByte((byte)4);
      }

      protected Hasher append(Hasher p_394930_, HashCode p_392558_) {
         return p_394930_.putBytes(p_392558_.asBytes());
      }

      protected DataResult<HashCode> build(Hasher p_394248_, HashCode p_391292_) {
         assert p_391292_.equals(HashOps.this.empty);
         p_394248_.putByte((byte)5);
         return DataResult.success(p_394248_.hash());
      }
   }

   final class MapHashBuilder extends AbstractUniversalBuilder<HashCode, List<Pair<HashCode, HashCode>>> {
      public MapHashBuilder() {
         super(HashOps.this);
      }

      protected List<Pair<HashCode, HashCode>> initBuilder() {
         return new ArrayList<>();
      }

      protected List<Pair<HashCode, HashCode>> append(HashCode p_391659_, HashCode p_392968_, List<Pair<HashCode, HashCode>> p_394869_) {
         p_394869_.add(Pair.of(p_391659_, p_392968_));
         return p_394869_;
      }

      protected DataResult<HashCode> build(List<Pair<HashCode, HashCode>> p_396177_, HashCode p_392314_) {
         assert HashOps.this.isEmpty(p_392314_);
         return DataResult.success(HashOps.hashMap(HashOps.this.hashFunction.newHasher(), p_396177_.stream()).hash());
      }
   }
}
