package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class FriendlyByteBuf extends ByteBuf {
   private final ByteBuf source;
   public static final short MAX_STRING_LENGTH = 32767;
   public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
   private static final int PUBLIC_KEY_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_LENGTH = 512;
   private static final Gson GSON = new Gson();

   public FriendlyByteBuf(ByteBuf p_130051_) {
      this.source = p_130051_;
   }

   @Deprecated
   public <T> T readWithCodecTrusted(DynamicOps<Tag> p_300704_, Codec<T> p_298353_) {
      return this.readWithCodec(p_300704_, p_298353_, NbtAccounter.unlimitedHeap());
   }

   @Deprecated
   public <T> T readWithCodec(DynamicOps<Tag> p_266903_, Codec<T> p_267107_, NbtAccounter p_300072_) {
      Tag tag = this.readNbt(p_300072_);
      return (T)p_267107_.parse(p_266903_, tag).getOrThrow(p_326054_ -> new DecoderException("Failed to decode: " + p_326054_ + " " + tag));
   }

   @Deprecated
   public <T> FriendlyByteBuf writeWithCodec(DynamicOps<Tag> p_266702_, Codec<T> p_267245_, T p_266783_) {
      Tag tag = (Tag)p_267245_.encodeStart(p_266702_, p_266783_)
         .getOrThrow(p_326056_ -> new EncoderException("Failed to encode: " + p_326056_ + " " + p_266783_));
      this.writeNbt(tag);
      return this;
   }

   public <T> T readLenientJsonWithCodec(Codec<T> p_409100_) {
      JsonElement jsonelement = LenientJsonParser.parse(this.readUtf());
      DataResult<T> dataresult = p_409100_.parse(JsonOps.INSTANCE, jsonelement);
      return (T)dataresult.getOrThrow(p_272382_ -> new DecoderException("Failed to decode JSON: " + p_272382_));
   }

   public <T> void writeJsonWithCodec(Codec<T> p_273285_, T p_272770_) {
      DataResult<JsonElement> dataresult = p_273285_.encodeStart(JsonOps.INSTANCE, p_272770_);
      this.writeUtf(GSON.toJson((JsonElement)dataresult.getOrThrow(p_326058_ -> new EncoderException("Failed to encode: " + p_326058_ + " " + p_272770_))));
   }

   public static <T> IntFunction<T> limitValue(IntFunction<T> p_182696_, int p_182697_) {
      return p_182686_ -> {
         if (p_182686_ > p_182697_) {
            throw new DecoderException("Value " + p_182686_ + " is larger than limit " + p_182697_);
         } else {
            return p_182696_.apply(p_182686_);
         }
      };
   }

   public <T, C extends Collection<T>> C readCollection(IntFunction<C> p_236839_, StreamDecoder<? super FriendlyByteBuf, T> p_332288_) {
      int i = this.readVarInt();
      C c = (C)p_236839_.apply(i);

      for (int j = 0; j < i; j++) {
         c.add(p_332288_.decode(this));
      }

      return c;
   }

   public <T> void writeCollection(Collection<T> p_236829_, StreamEncoder<? super FriendlyByteBuf, T> p_330754_) {
      this.writeVarInt(p_236829_.size());

      for (T t : p_236829_) {
         p_330754_.encode(this, t);
      }
   }

   public <T> List<T> readList(StreamDecoder<? super FriendlyByteBuf, T> p_335526_) {
      return this.readCollection(Lists::newArrayListWithCapacity, p_335526_);
   }

   public IntList readIntIdList() {
      int i = this.readVarInt();
      IntList intlist = new IntArrayList();

      for (int j = 0; j < i; j++) {
         intlist.add(this.readVarInt());
      }

      return intlist;
   }

   public void writeIntIdList(IntList p_178346_) {
      this.writeVarInt(p_178346_.size());
      p_178346_.forEach(this::writeVarInt);
   }

   public <K, V, M extends Map<K, V>> M readMap(
      IntFunction<M> p_236842_, StreamDecoder<? super FriendlyByteBuf, K> p_331258_, StreamDecoder<? super FriendlyByteBuf, V> p_332787_
   ) {
      int i = this.readVarInt();
      M m = (M)p_236842_.apply(i);

      for (int j = 0; j < i; j++) {
         K k = p_331258_.decode(this);
         V v = p_332787_.decode(this);
         m.put(k, v);
      }

      return m;
   }

   public <K, V> Map<K, V> readMap(StreamDecoder<? super FriendlyByteBuf, K> p_331841_, StreamDecoder<? super FriendlyByteBuf, V> p_328220_) {
      return this.readMap(Maps::newHashMapWithExpectedSize, p_331841_, p_328220_);
   }

   public <K, V> void writeMap(Map<K, V> p_236832_, StreamEncoder<? super FriendlyByteBuf, K> p_332360_, StreamEncoder<? super FriendlyByteBuf, V> p_331674_) {
      this.writeVarInt(p_236832_.size());
      p_236832_.forEach((p_326051_, p_326052_) -> {
         p_332360_.encode(this, (K)p_326051_);
         p_331674_.encode(this, (V)p_326052_);
      });
   }

   public void readWithCount(Consumer<FriendlyByteBuf> p_178365_) {
      int i = this.readVarInt();

      for (int j = 0; j < i; j++) {
         p_178365_.accept(this);
      }
   }

   public <E extends Enum<E>> void writeEnumSet(EnumSet<E> p_250400_, Class<E> p_250673_) {
      E[] ae = (E[])p_250673_.getEnumConstants();
      BitSet bitset = new BitSet(ae.length);

      for (int i = 0; i < ae.length; i++) {
         bitset.set(i, p_250400_.contains(ae[i]));
      }

      this.writeFixedBitSet(bitset, ae.length);
   }

   public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> p_251289_) {
      E[] ae = (E[])p_251289_.getEnumConstants();
      BitSet bitset = this.readFixedBitSet(ae.length);
      EnumSet<E> enumset = EnumSet.noneOf(p_251289_);

      for (int i = 0; i < ae.length; i++) {
         if (bitset.get(i)) {
            enumset.add(ae[i]);
         }
      }

      return enumset;
   }

   public <T> void writeOptional(Optional<T> p_236836_, StreamEncoder<? super FriendlyByteBuf, T> p_327886_) {
      if (p_236836_.isPresent()) {
         this.writeBoolean(true);
         p_327886_.encode(this, p_236836_.get());
      } else {
         this.writeBoolean(false);
      }
   }

   public <T> Optional<T> readOptional(StreamDecoder<? super FriendlyByteBuf, T> p_334254_) {
      return this.readBoolean() ? Optional.of(p_334254_.decode(this)) : Optional.empty();
   }

   public <L, R> void writeEither(
      Either<L, R> p_407428_, StreamEncoder<? super FriendlyByteBuf, L> p_410578_, StreamEncoder<? super FriendlyByteBuf, R> p_408994_
   ) {
      p_407428_.ifLeft(p_405098_ -> {
         this.writeBoolean(true);
         p_410578_.encode(this, (L)p_405098_);
      }).ifRight(p_405100_ -> {
         this.writeBoolean(false);
         p_408994_.encode(this, (R)p_405100_);
      });
   }

   public <L, R> Either<L, R> readEither(StreamDecoder<? super FriendlyByteBuf, L> p_407500_, StreamDecoder<? super FriendlyByteBuf, R> p_407062_) {
      return this.readBoolean() ? Either.left(p_407500_.decode(this)) : Either.right(p_407062_.decode(this));
   }

   public <T> @Nullable T readNullable(StreamDecoder<? super FriendlyByteBuf, T> p_334486_) {
      return readNullable(this, p_334486_);
   }

   public static <T, B extends ByteBuf> @Nullable T readNullable(B p_332661_, StreamDecoder<? super B, T> p_332281_) {
      return p_332661_.readBoolean() ? p_332281_.decode(p_332661_) : null;
   }

   public <T> void writeNullable(@Nullable T p_329857_, StreamEncoder<? super FriendlyByteBuf, T> p_330309_) {
      writeNullable(this, p_329857_, p_330309_);
   }

   public static <T, B extends ByteBuf> void writeNullable(B p_334011_, @Nullable T p_236822_, StreamEncoder<? super B, T> p_335434_) {
      if (p_236822_ != null) {
         p_334011_.writeBoolean(true);
         p_335434_.encode(p_334011_, p_236822_);
      } else {
         p_334011_.writeBoolean(false);
      }
   }

   public byte[] readByteArray() {
      return readByteArray(this);
   }

   public static byte[] readByteArray(ByteBuf p_328205_) {
      return readByteArray(p_328205_, p_328205_.readableBytes());
   }

   public FriendlyByteBuf writeByteArray(byte[] p_130088_) {
      writeByteArray(this, p_130088_);
      return this;
   }

   public static void writeByteArray(ByteBuf p_331989_, byte[] p_332475_) {
      VarInt.write(p_331989_, p_332475_.length);
      p_331989_.writeBytes(p_332475_);
   }

   public byte[] readByteArray(int p_130102_) {
      return readByteArray(this, p_130102_);
   }

   public static byte[] readByteArray(ByteBuf p_329949_, int p_330919_) {
      int i = VarInt.read(p_329949_);
      if (i > p_330919_) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + p_330919_);
      }

      byte[] abyte = new byte[i];
      p_329949_.readBytes(abyte);
      return abyte;
   }

   public FriendlyByteBuf writeVarIntArray(int[] p_130090_) {
      this.writeVarInt(p_130090_.length);

      for (int i : p_130090_) {
         this.writeVarInt(i);
      }

      return this;
   }

   public int[] readVarIntArray() {
      return this.readVarIntArray(this.readableBytes());
   }

   public int[] readVarIntArray(int p_130117_) {
      int i = this.readVarInt();
      if (i > p_130117_) {
         throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + p_130117_);
      }

      int[] aint = new int[i];

      for (int j = 0; j < aint.length; j++) {
         aint[j] = this.readVarInt();
      }

      return aint;
   }

   public FriendlyByteBuf writeLongArray(long[] p_130092_) {
      writeLongArray(this, p_130092_);
      return this;
   }

   public static void writeLongArray(ByteBuf p_396275_, long[] p_396585_) {
      VarInt.write(p_396275_, p_396585_.length);
      writeFixedSizeLongArray(p_396275_, p_396585_);
   }

   public FriendlyByteBuf writeFixedSizeLongArray(long[] p_395429_) {
      writeFixedSizeLongArray(this, p_395429_);
      return this;
   }

   public static void writeFixedSizeLongArray(ByteBuf p_392907_, long[] p_394030_) {
      for (long i : p_394030_) {
         p_392907_.writeLong(i);
      }
   }

   public long[] readLongArray() {
      return readLongArray(this);
   }

   public long[] readFixedSizeLongArray(long[] p_397361_) {
      return readFixedSizeLongArray(this, p_397361_);
   }

   public static long[] readLongArray(ByteBuf p_391797_) {
      int i = VarInt.read(p_391797_);
      int j = p_391797_.readableBytes() / 8;
      if (i > j) {
         throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + j);
      } else {
         return readFixedSizeLongArray(p_391797_, new long[i]);
      }
   }

   public static long[] readFixedSizeLongArray(ByteBuf p_394499_, long[] p_395381_) {
      for (int i = 0; i < p_395381_.length; i++) {
         p_395381_[i] = p_394499_.readLong();
      }

      return p_395381_;
   }

   public BlockPos readBlockPos() {
      return readBlockPos(this);
   }

   public static BlockPos readBlockPos(ByteBuf p_329034_) {
      return BlockPos.of(p_329034_.readLong());
   }

   public FriendlyByteBuf writeBlockPos(BlockPos p_130065_) {
      writeBlockPos(this, p_130065_);
      return this;
   }

   public static void writeBlockPos(ByteBuf p_329209_, BlockPos p_331076_) {
      p_329209_.writeLong(p_331076_.asLong());
   }

   public ChunkPos readChunkPos() {
      return new ChunkPos(this.readLong());
   }

   public FriendlyByteBuf writeChunkPos(ChunkPos p_178342_) {
      this.writeLong(p_178342_.toLong());
      return this;
   }

   public static ChunkPos readChunkPos(ByteBuf p_366320_) {
      return new ChunkPos(p_366320_.readLong());
   }

   public static void writeChunkPos(ByteBuf p_362018_, ChunkPos p_368384_) {
      p_362018_.writeLong(p_368384_.toLong());
   }

   public GlobalPos readGlobalPos() {
      ResourceKey<Level> resourcekey = this.readResourceKey(Registries.DIMENSION);
      BlockPos blockpos = this.readBlockPos();
      return GlobalPos.of(resourcekey, blockpos);
   }

   public void writeGlobalPos(GlobalPos p_236815_) {
      this.writeResourceKey(p_236815_.dimension());
      this.writeBlockPos(p_236815_.pos());
   }

   public Vector3f readVector3f() {
      return readVector3f(this);
   }

   public static Vector3f readVector3f(ByteBuf p_330751_) {
      return new Vector3f(p_330751_.readFloat(), p_330751_.readFloat(), p_330751_.readFloat());
   }

   public void writeVector3f(Vector3f p_270985_) {
      writeVector3f(this, p_270985_);
   }

   public static void writeVector3f(ByteBuf p_334705_, Vector3fc p_452948_) {
      p_334705_.writeFloat(p_452948_.x());
      p_334705_.writeFloat(p_452948_.y());
      p_334705_.writeFloat(p_452948_.z());
   }

   public Quaternionf readQuaternion() {
      return readQuaternion(this);
   }

   public static Quaternionf readQuaternion(ByteBuf p_328264_) {
      return new Quaternionf(p_328264_.readFloat(), p_328264_.readFloat(), p_328264_.readFloat(), p_328264_.readFloat());
   }

   public void writeQuaternion(Quaternionf p_270141_) {
      writeQuaternion(this, p_270141_);
   }

   public static void writeQuaternion(ByteBuf p_327848_, Quaternionfc p_458885_) {
      p_327848_.writeFloat(p_458885_.x());
      p_327848_.writeFloat(p_458885_.y());
      p_327848_.writeFloat(p_458885_.z());
      p_327848_.writeFloat(p_458885_.w());
   }

   public static Vec3 readVec3(ByteBuf p_363125_) {
      return new Vec3(p_363125_.readDouble(), p_363125_.readDouble(), p_363125_.readDouble());
   }

   public Vec3 readVec3() {
      return readVec3(this);
   }

   public static void writeVec3(ByteBuf p_369821_, Vec3 p_369185_) {
      p_369821_.writeDouble(p_369185_.x());
      p_369821_.writeDouble(p_369185_.y());
      p_369821_.writeDouble(p_369185_.z());
   }

   public void writeVec3(Vec3 p_300768_) {
      writeVec3(this, p_300768_);
   }

   public Vec3 readLpVec3() {
      return LpVec3.read(this);
   }

   public void writeLpVec3(Vec3 p_423686_) {
      LpVec3.write(this, p_423686_);
   }

   public <T extends Enum<T>> T readEnum(Class<T> p_130067_) {
      return p_130067_.getEnumConstants()[this.readVarInt()];
   }

   public FriendlyByteBuf writeEnum(Enum<?> p_130069_) {
      return this.writeVarInt(p_130069_.ordinal());
   }

   public <T> T readById(IntFunction<T> p_300981_) {
      int i = this.readVarInt();
      return p_300981_.apply(i);
   }

   public <T> FriendlyByteBuf writeById(ToIntFunction<T> p_297872_, T p_300123_) {
      int i = p_297872_.applyAsInt(p_300123_);
      return this.writeVarInt(i);
   }

   public int readVarInt() {
      return VarInt.read(this.source);
   }

   public long readVarLong() {
      return VarLong.read(this.source);
   }

   public FriendlyByteBuf writeUUID(UUID p_130078_) {
      writeUUID(this, p_130078_);
      return this;
   }

   public static void writeUUID(ByteBuf p_331039_, UUID p_331372_) {
      p_331039_.writeLong(p_331372_.getMostSignificantBits());
      p_331039_.writeLong(p_331372_.getLeastSignificantBits());
   }

   public UUID readUUID() {
      return readUUID(this);
   }

   public static UUID readUUID(ByteBuf p_330717_) {
      return new UUID(p_330717_.readLong(), p_330717_.readLong());
   }

   public FriendlyByteBuf writeVarInt(int p_130131_) {
      VarInt.write(this.source, p_130131_);
      return this;
   }

   public FriendlyByteBuf writeVarLong(long p_130104_) {
      VarLong.write(this.source, p_130104_);
      return this;
   }

   public FriendlyByteBuf writeNbt(@Nullable Tag p_300580_) {
      writeNbt(this, p_300580_);
      return this;
   }

   public static void writeNbt(ByteBuf p_335073_, @Nullable Tag p_330157_) {
      if (p_330157_ == null) {
         p_330157_ = EndTag.INSTANCE;
      }

      try {
         NbtIo.writeAnyTag(p_330157_, new ByteBufOutputStream(p_335073_));
      } catch (IOException ioexception) {
         throw new EncoderException(ioexception);
      }
   }

   public @Nullable CompoundTag readNbt() {
      return readNbt(this);
   }

   public static @Nullable CompoundTag readNbt(ByteBuf p_328524_) {
      Tag tag = readNbt(p_328524_, NbtAccounter.defaultQuota());
      if (tag != null && !(tag instanceof CompoundTag)) {
         throw new DecoderException("Not a compound tag: " + tag);
      } else {
         return (CompoundTag)tag;
      }
   }

   public static @Nullable Tag readNbt(ByteBuf p_334505_, NbtAccounter p_333893_) {
      try {
         Tag tag = NbtIo.readAnyTag(new ByteBufInputStream(p_334505_), p_333893_);
         return tag.getId() == 0 ? null : tag;
      } catch (IOException ioexception) {
         throw new EncoderException(ioexception);
      }
   }

   public @Nullable Tag readNbt(NbtAccounter p_130082_) {
      return readNbt(this, p_130082_);
   }

   public String readUtf() {
      return this.readUtf(32767);
   }

   public String readUtf(int p_130137_) {
      return Utf8String.read(this.source, p_130137_);
   }

   public FriendlyByteBuf writeUtf(String p_130071_) {
      return this.writeUtf(p_130071_, 32767);
   }

   public FriendlyByteBuf writeUtf(String p_130073_, int p_130074_) {
      Utf8String.write(this.source, p_130073_, p_130074_);
      return this;
   }

   public Identifier readIdentifier() {
      return Identifier.parse(this.readUtf(32767));
   }

   public FriendlyByteBuf writeIdentifier(Identifier p_460940_) {
      this.writeUtf(p_460940_.toString());
      return this;
   }

   public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> p_236802_) {
      Identifier identifier = this.readIdentifier();
      return ResourceKey.create(p_236802_, identifier);
   }

   public void writeResourceKey(ResourceKey<?> p_236859_) {
      this.writeIdentifier(p_236859_.identifier());
   }

   public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
      Identifier identifier = this.readIdentifier();
      return ResourceKey.createRegistryKey(identifier);
   }

   public Instant readInstant() {
      return Instant.ofEpochMilli(this.readLong());
   }

   public void writeInstant(Instant p_236827_) {
      this.writeLong(p_236827_.toEpochMilli());
   }

   public PublicKey readPublicKey() {
      try {
         return Crypt.byteToPublicKey(this.readByteArray(512));
      } catch (CryptException cryptexception) {
         throw new DecoderException("Malformed public key bytes", cryptexception);
      }
   }

   public FriendlyByteBuf writePublicKey(PublicKey p_236825_) {
      this.writeByteArray(p_236825_.getEncoded());
      return this;
   }

   public BlockHitResult readBlockHitResult() {
      BlockPos blockpos = this.readBlockPos();
      Direction direction = this.readEnum(Direction.class);
      float f = this.readFloat();
      float f1 = this.readFloat();
      float f2 = this.readFloat();
      boolean flag = this.readBoolean();
      boolean flag1 = this.readBoolean();
      return new BlockHitResult(
         new Vec3((double)blockpos.getX() + f, (double)blockpos.getY() + f1, (double)blockpos.getZ() + f2), direction, blockpos, flag, flag1
      );
   }

   public void writeBlockHitResult(BlockHitResult p_130063_) {
      BlockPos blockpos = p_130063_.getBlockPos();
      this.writeBlockPos(blockpos);
      this.writeEnum(p_130063_.getDirection());
      Vec3 vec3 = p_130063_.getLocation();
      this.writeFloat((float)(vec3.x - blockpos.getX()));
      this.writeFloat((float)(vec3.y - blockpos.getY()));
      this.writeFloat((float)(vec3.z - blockpos.getZ()));
      this.writeBoolean(p_130063_.isInside());
      this.writeBoolean(p_130063_.isWorldBorderHit());
   }

   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   public void writeBitSet(BitSet p_178351_) {
      this.writeLongArray(p_178351_.toLongArray());
   }

   public BitSet readFixedBitSet(int p_249113_) {
      byte[] abyte = new byte[Mth.positiveCeilDiv(p_249113_, 8)];
      this.readBytes(abyte);
      return BitSet.valueOf(abyte);
   }

   public void writeFixedBitSet(BitSet p_248698_, int p_248869_) {
      if (p_248698_.length() > p_248869_) {
         throw new EncoderException("BitSet is larger than expected size (" + p_248698_.length() + ">" + p_248869_ + ")");
      }

      byte[] abyte = p_248698_.toByteArray();
      this.writeBytes(Arrays.copyOf(abyte, Mth.positiveCeilDiv(p_248869_, 8)));
   }

   public static int readContainerId(ByteBuf p_360986_) {
      return VarInt.read(p_360986_);
   }

   public int readContainerId() {
      return readContainerId(this.source);
   }

   public static void writeContainerId(ByteBuf p_364943_, int p_361713_) {
      VarInt.write(p_364943_, p_361713_);
   }

   public void writeContainerId(int p_367499_) {
      writeContainerId(this.source, p_367499_);
   }

   public boolean isContiguous() {
      return this.source.isContiguous();
   }

   public int maxFastWritableBytes() {
      return this.source.maxFastWritableBytes();
   }

   public int capacity() {
      return this.source.capacity();
   }

   public FriendlyByteBuf capacity(int p_300133_) {
      this.source.capacity(p_300133_);
      return this;
   }

   public int maxCapacity() {
      return this.source.maxCapacity();
   }

   public ByteBufAllocator alloc() {
      return this.source.alloc();
   }

   public ByteOrder order() {
      return this.source.order();
   }

   public ByteBuf order(ByteOrder p_130280_) {
      return this.source.order(p_130280_);
   }

   public ByteBuf unwrap() {
      return this.source;
   }

   public boolean isDirect() {
      return this.source.isDirect();
   }

   public boolean isReadOnly() {
      return this.source.isReadOnly();
   }

   public ByteBuf asReadOnly() {
      return this.source.asReadOnly();
   }

   public int readerIndex() {
      return this.source.readerIndex();
   }

   public FriendlyByteBuf readerIndex(int p_300300_) {
      this.source.readerIndex(p_300300_);
      return this;
   }

   public int writerIndex() {
      return this.source.writerIndex();
   }

   public FriendlyByteBuf writerIndex(int p_298940_) {
      this.source.writerIndex(p_298940_);
      return this;
   }

   public FriendlyByteBuf setIndex(int p_298280_, int p_301012_) {
      this.source.setIndex(p_298280_, p_301012_);
      return this;
   }

   public int readableBytes() {
      return this.source.readableBytes();
   }

   public int writableBytes() {
      return this.source.writableBytes();
   }

   public int maxWritableBytes() {
      return this.source.maxWritableBytes();
   }

   public boolean isReadable() {
      return this.source.isReadable();
   }

   public boolean isReadable(int p_130254_) {
      return this.source.isReadable(p_130254_);
   }

   public boolean isWritable() {
      return this.source.isWritable();
   }

   public boolean isWritable(int p_130257_) {
      return this.source.isWritable(p_130257_);
   }

   public FriendlyByteBuf clear() {
      this.source.clear();
      return this;
   }

   public FriendlyByteBuf markReaderIndex() {
      this.source.markReaderIndex();
      return this;
   }

   public FriendlyByteBuf resetReaderIndex() {
      this.source.resetReaderIndex();
      return this;
   }

   public FriendlyByteBuf markWriterIndex() {
      this.source.markWriterIndex();
      return this;
   }

   public FriendlyByteBuf resetWriterIndex() {
      this.source.resetWriterIndex();
      return this;
   }

   public FriendlyByteBuf discardReadBytes() {
      this.source.discardReadBytes();
      return this;
   }

   public FriendlyByteBuf discardSomeReadBytes() {
      this.source.discardSomeReadBytes();
      return this;
   }

   public FriendlyByteBuf ensureWritable(int p_301044_) {
      this.source.ensureWritable(p_301044_);
      return this;
   }

   public int ensureWritable(int p_130141_, boolean p_130142_) {
      return this.source.ensureWritable(p_130141_, p_130142_);
   }

   public boolean getBoolean(int p_130159_) {
      return this.source.getBoolean(p_130159_);
   }

   public byte getByte(int p_130161_) {
      return this.source.getByte(p_130161_);
   }

   public short getUnsignedByte(int p_130225_) {
      return this.source.getUnsignedByte(p_130225_);
   }

   public short getShort(int p_130221_) {
      return this.source.getShort(p_130221_);
   }

   public short getShortLE(int p_130223_) {
      return this.source.getShortLE(p_130223_);
   }

   public int getUnsignedShort(int p_130235_) {
      return this.source.getUnsignedShort(p_130235_);
   }

   public int getUnsignedShortLE(int p_130237_) {
      return this.source.getUnsignedShortLE(p_130237_);
   }

   public int getMedium(int p_130217_) {
      return this.source.getMedium(p_130217_);
   }

   public int getMediumLE(int p_130219_) {
      return this.source.getMediumLE(p_130219_);
   }

   public int getUnsignedMedium(int p_130231_) {
      return this.source.getUnsignedMedium(p_130231_);
   }

   public int getUnsignedMediumLE(int p_130233_) {
      return this.source.getUnsignedMediumLE(p_130233_);
   }

   public int getInt(int p_130209_) {
      return this.source.getInt(p_130209_);
   }

   public int getIntLE(int p_130211_) {
      return this.source.getIntLE(p_130211_);
   }

   public long getUnsignedInt(int p_130227_) {
      return this.source.getUnsignedInt(p_130227_);
   }

   public long getUnsignedIntLE(int p_130229_) {
      return this.source.getUnsignedIntLE(p_130229_);
   }

   public long getLong(int p_130213_) {
      return this.source.getLong(p_130213_);
   }

   public long getLongLE(int p_130215_) {
      return this.source.getLongLE(p_130215_);
   }

   public char getChar(int p_130199_) {
      return this.source.getChar(p_130199_);
   }

   public float getFloat(int p_130207_) {
      return this.source.getFloat(p_130207_);
   }

   public double getDouble(int p_130205_) {
      return this.source.getDouble(p_130205_);
   }

   public FriendlyByteBuf getBytes(int p_299985_, ByteBuf p_298214_) {
      this.source.getBytes(p_299985_, p_298214_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_300246_, ByteBuf p_301111_, int p_300978_) {
      this.source.getBytes(p_300246_, p_301111_, p_300978_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_300430_, ByteBuf p_297846_, int p_300610_, int p_299363_) {
      this.source.getBytes(p_300430_, p_297846_, p_300610_, p_299363_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_300928_, byte[] p_297630_) {
      this.source.getBytes(p_300928_, p_297630_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_299455_, byte[] p_300069_, int p_300847_, int p_299513_) {
      this.source.getBytes(p_299455_, p_300069_, p_300847_, p_299513_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_299731_, ByteBuffer p_299164_) {
      this.source.getBytes(p_299731_, p_299164_);
      return this;
   }

   public FriendlyByteBuf getBytes(int p_298241_, OutputStream p_301151_, int p_299913_) throws IOException {
      this.source.getBytes(p_298241_, p_301151_, p_299913_);
      return this;
   }

   public int getBytes(int p_130187_, GatheringByteChannel p_130188_, int p_130189_) throws IOException {
      return this.source.getBytes(p_130187_, p_130188_, p_130189_);
   }

   public int getBytes(int p_130182_, FileChannel p_130183_, long p_130184_, int p_130185_) throws IOException {
      return this.source.getBytes(p_130182_, p_130183_, p_130184_, p_130185_);
   }

   public CharSequence getCharSequence(int p_130201_, int p_130202_, Charset p_130203_) {
      return this.source.getCharSequence(p_130201_, p_130202_, p_130203_);
   }

   public FriendlyByteBuf setBoolean(int p_299892_, boolean p_297333_) {
      this.source.setBoolean(p_299892_, p_297333_);
      return this;
   }

   public FriendlyByteBuf setByte(int p_297325_, int p_300406_) {
      this.source.setByte(p_297325_, p_300406_);
      return this;
   }

   public FriendlyByteBuf setShort(int p_297216_, int p_298749_) {
      this.source.setShort(p_297216_, p_298749_);
      return this;
   }

   public FriendlyByteBuf setShortLE(int p_299646_, int p_298038_) {
      this.source.setShortLE(p_299646_, p_298038_);
      return this;
   }

   public FriendlyByteBuf setMedium(int p_300490_, int p_299067_) {
      this.source.setMedium(p_300490_, p_299067_);
      return this;
   }

   public FriendlyByteBuf setMediumLE(int p_300630_, int p_299351_) {
      this.source.setMediumLE(p_300630_, p_299351_);
      return this;
   }

   public FriendlyByteBuf setInt(int p_299176_, int p_301413_) {
      this.source.setInt(p_299176_, p_301413_);
      return this;
   }

   public FriendlyByteBuf setIntLE(int p_300111_, int p_297978_) {
      this.source.setIntLE(p_300111_, p_297978_);
      return this;
   }

   public FriendlyByteBuf setLong(int p_298039_, long p_298360_) {
      this.source.setLong(p_298039_, p_298360_);
      return this;
   }

   public FriendlyByteBuf setLongLE(int p_300929_, long p_299282_) {
      this.source.setLongLE(p_300929_, p_299282_);
      return this;
   }

   public FriendlyByteBuf setChar(int p_297413_, int p_297953_) {
      this.source.setChar(p_297413_, p_297953_);
      return this;
   }

   public FriendlyByteBuf setFloat(int p_297779_, float p_297840_) {
      this.source.setFloat(p_297779_, p_297840_);
      return this;
   }

   public FriendlyByteBuf setDouble(int p_301027_, double p_299551_) {
      this.source.setDouble(p_301027_, p_299551_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_300769_, ByteBuf p_301342_) {
      this.source.setBytes(p_300769_, p_301342_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_300924_, ByteBuf p_301233_, int p_299359_) {
      this.source.setBytes(p_300924_, p_301233_, p_299359_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_299338_, ByteBuf p_299810_, int p_301059_, int p_297827_) {
      this.source.setBytes(p_299338_, p_299810_, p_301059_, p_297827_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_297553_, byte[] p_300329_) {
      this.source.setBytes(p_297553_, p_300329_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_297451_, byte[] p_300466_, int p_297825_, int p_299499_) {
      this.source.setBytes(p_297451_, p_300466_, p_297825_, p_299499_);
      return this;
   }

   public FriendlyByteBuf setBytes(int p_297596_, ByteBuffer p_299096_) {
      this.source.setBytes(p_297596_, p_299096_);
      return this;
   }

   public int setBytes(int p_130380_, InputStream p_130381_, int p_130382_) throws IOException {
      return this.source.setBytes(p_130380_, p_130381_, p_130382_);
   }

   public int setBytes(int p_130392_, ScatteringByteChannel p_130393_, int p_130394_) throws IOException {
      return this.source.setBytes(p_130392_, p_130393_, p_130394_);
   }

   public int setBytes(int p_130387_, FileChannel p_130388_, long p_130389_, int p_130390_) throws IOException {
      return this.source.setBytes(p_130387_, p_130388_, p_130389_, p_130390_);
   }

   public FriendlyByteBuf setZero(int p_297586_, int p_299960_) {
      this.source.setZero(p_297586_, p_299960_);
      return this;
   }

   public int setCharSequence(int p_130407_, CharSequence p_130408_, Charset p_130409_) {
      return this.source.setCharSequence(p_130407_, p_130408_, p_130409_);
   }

   public boolean readBoolean() {
      return this.source.readBoolean();
   }

   public byte readByte() {
      return this.source.readByte();
   }

   public short readUnsignedByte() {
      return this.source.readUnsignedByte();
   }

   public short readShort() {
      return this.source.readShort();
   }

   public short readShortLE() {
      return this.source.readShortLE();
   }

   public int readUnsignedShort() {
      return this.source.readUnsignedShort();
   }

   public int readUnsignedShortLE() {
      return this.source.readUnsignedShortLE();
   }

   public int readMedium() {
      return this.source.readMedium();
   }

   public int readMediumLE() {
      return this.source.readMediumLE();
   }

   public int readUnsignedMedium() {
      return this.source.readUnsignedMedium();
   }

   public int readUnsignedMediumLE() {
      return this.source.readUnsignedMediumLE();
   }

   public int readInt() {
      return this.source.readInt();
   }

   public int readIntLE() {
      return this.source.readIntLE();
   }

   public long readUnsignedInt() {
      return this.source.readUnsignedInt();
   }

   public long readUnsignedIntLE() {
      return this.source.readUnsignedIntLE();
   }

   public long readLong() {
      return this.source.readLong();
   }

   public long readLongLE() {
      return this.source.readLongLE();
   }

   public char readChar() {
      return this.source.readChar();
   }

   public float readFloat() {
      return this.source.readFloat();
   }

   public double readDouble() {
      return this.source.readDouble();
   }

   public ByteBuf readBytes(int p_130287_) {
      return this.source.readBytes(p_130287_);
   }

   public ByteBuf readSlice(int p_130332_) {
      return this.source.readSlice(p_130332_);
   }

   public ByteBuf readRetainedSlice(int p_130328_) {
      return this.source.readRetainedSlice(p_130328_);
   }

   public FriendlyByteBuf readBytes(ByteBuf p_300560_) {
      this.source.readBytes(p_300560_);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf p_299224_, int p_300166_) {
      this.source.readBytes(p_299224_, p_300166_);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf p_301382_, int p_300030_, int p_300211_) {
      this.source.readBytes(p_301382_, p_300030_, p_300211_);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] p_299454_) {
      this.source.readBytes(p_299454_);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] p_299845_, int p_297363_, int p_299384_) {
      this.source.readBytes(p_299845_, p_297363_, p_299384_);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuffer p_297688_) {
      this.source.readBytes(p_297688_);
      return this;
   }

   public FriendlyByteBuf readBytes(OutputStream p_300218_, int p_298001_) throws IOException {
      this.source.readBytes(p_300218_, p_298001_);
      return this;
   }

   public int readBytes(GatheringByteChannel p_130307_, int p_130308_) throws IOException {
      return this.source.readBytes(p_130307_, p_130308_);
   }

   public CharSequence readCharSequence(int p_130317_, Charset p_130318_) {
      return this.source.readCharSequence(p_130317_, p_130318_);
   }

   public String readString(int p_458112_, Charset p_457149_) {
      return this.source.readString(p_458112_, p_457149_);
   }

   public int readBytes(FileChannel p_130303_, long p_130304_, int p_130305_) throws IOException {
      return this.source.readBytes(p_130303_, p_130304_, p_130305_);
   }

   public FriendlyByteBuf skipBytes(int p_300784_) {
      this.source.skipBytes(p_300784_);
      return this;
   }

   public FriendlyByteBuf writeBoolean(boolean p_300653_) {
      this.source.writeBoolean(p_300653_);
      return this;
   }

   public FriendlyByteBuf writeByte(int p_299498_) {
      this.source.writeByte(p_299498_);
      return this;
   }

   public FriendlyByteBuf writeShort(int p_299519_) {
      this.source.writeShort(p_299519_);
      return this;
   }

   public FriendlyByteBuf writeShortLE(int p_297214_) {
      this.source.writeShortLE(p_297214_);
      return this;
   }

   public FriendlyByteBuf writeMedium(int p_299802_) {
      this.source.writeMedium(p_299802_);
      return this;
   }

   public FriendlyByteBuf writeMediumLE(int p_301291_) {
      this.source.writeMediumLE(p_301291_);
      return this;
   }

   public FriendlyByteBuf writeInt(int p_301066_) {
      this.source.writeInt(p_301066_);
      return this;
   }

   public FriendlyByteBuf writeIntLE(int p_299068_) {
      this.source.writeIntLE(p_299068_);
      return this;
   }

   public FriendlyByteBuf writeLong(long p_300584_) {
      this.source.writeLong(p_300584_);
      return this;
   }

   public FriendlyByteBuf writeLongLE(long p_298747_) {
      this.source.writeLongLE(p_298747_);
      return this;
   }

   public FriendlyByteBuf writeChar(int p_300374_) {
      this.source.writeChar(p_300374_);
      return this;
   }

   public FriendlyByteBuf writeFloat(float p_299476_) {
      this.source.writeFloat(p_299476_);
      return this;
   }

   public FriendlyByteBuf writeDouble(double p_301246_) {
      this.source.writeDouble(p_301246_);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf p_300943_) {
      this.source.writeBytes(p_300943_);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf p_298105_, int p_299600_) {
      this.source.writeBytes(p_298105_, p_299600_);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf p_299075_, int p_301207_, int p_299710_) {
      this.source.writeBytes(p_299075_, p_301207_, p_299710_);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] p_299214_) {
      this.source.writeBytes(p_299214_);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] p_298410_, int p_297608_, int p_300690_) {
      this.source.writeBytes(p_298410_, p_297608_, p_300690_);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuffer p_300889_) {
      this.source.writeBytes(p_300889_);
      return this;
   }

   public int writeBytes(InputStream p_130481_, int p_130482_) throws IOException {
      return this.source.writeBytes(p_130481_, p_130482_);
   }

   public int writeBytes(ScatteringByteChannel p_130490_, int p_130491_) throws IOException {
      return this.source.writeBytes(p_130490_, p_130491_);
   }

   public int writeBytes(FileChannel p_130486_, long p_130487_, int p_130488_) throws IOException {
      return this.source.writeBytes(p_130486_, p_130487_, p_130488_);
   }

   public FriendlyByteBuf writeZero(int p_298160_) {
      this.source.writeZero(p_298160_);
      return this;
   }

   public int writeCharSequence(CharSequence p_130501_, Charset p_130502_) {
      return this.source.writeCharSequence(p_130501_, p_130502_);
   }

   public int indexOf(int p_130244_, int p_130245_, byte p_130246_) {
      return this.source.indexOf(p_130244_, p_130245_, p_130246_);
   }

   public int bytesBefore(byte p_130108_) {
      return this.source.bytesBefore(p_130108_);
   }

   public int bytesBefore(int p_130110_, byte p_130111_) {
      return this.source.bytesBefore(p_130110_, p_130111_);
   }

   public int bytesBefore(int p_130113_, int p_130114_, byte p_130115_) {
      return this.source.bytesBefore(p_130113_, p_130114_, p_130115_);
   }

   public int forEachByte(ByteProcessor p_130150_) {
      return this.source.forEachByte(p_130150_);
   }

   public int forEachByte(int p_130146_, int p_130147_, ByteProcessor p_130148_) {
      return this.source.forEachByte(p_130146_, p_130147_, p_130148_);
   }

   public int forEachByteDesc(ByteProcessor p_130156_) {
      return this.source.forEachByteDesc(p_130156_);
   }

   public int forEachByteDesc(int p_130152_, int p_130153_, ByteProcessor p_130154_) {
      return this.source.forEachByteDesc(p_130152_, p_130153_, p_130154_);
   }

   public ByteBuf copy() {
      return this.source.copy();
   }

   public ByteBuf copy(int p_130128_, int p_130129_) {
      return this.source.copy(p_130128_, p_130129_);
   }

   public ByteBuf slice() {
      return this.source.slice();
   }

   public ByteBuf retainedSlice() {
      return this.source.retainedSlice();
   }

   public ByteBuf slice(int p_130450_, int p_130451_) {
      return this.source.slice(p_130450_, p_130451_);
   }

   public ByteBuf retainedSlice(int p_130359_, int p_130360_) {
      return this.source.retainedSlice(p_130359_, p_130360_);
   }

   public ByteBuf duplicate() {
      return this.source.duplicate();
   }

   public ByteBuf retainedDuplicate() {
      return this.source.retainedDuplicate();
   }

   public int nioBufferCount() {
      return this.source.nioBufferCount();
   }

   public ByteBuffer nioBuffer() {
      return this.source.nioBuffer();
   }

   public ByteBuffer nioBuffer(int p_130270_, int p_130271_) {
      return this.source.nioBuffer(p_130270_, p_130271_);
   }

   public ByteBuffer internalNioBuffer(int p_130248_, int p_130249_) {
      return this.source.internalNioBuffer(p_130248_, p_130249_);
   }

   public ByteBuffer[] nioBuffers() {
      return this.source.nioBuffers();
   }

   public ByteBuffer[] nioBuffers(int p_130275_, int p_130276_) {
      return this.source.nioBuffers(p_130275_, p_130276_);
   }

   public boolean hasArray() {
      return this.source.hasArray();
   }

   public byte[] array() {
      return this.source.array();
   }

   public int arrayOffset() {
      return this.source.arrayOffset();
   }

   public boolean hasMemoryAddress() {
      return this.source.hasMemoryAddress();
   }

   public long memoryAddress() {
      return this.source.memoryAddress();
   }

   public String toString(Charset p_130458_) {
      return this.source.toString(p_130458_);
   }

   public String toString(int p_130454_, int p_130455_, Charset p_130456_) {
      return this.source.toString(p_130454_, p_130455_, p_130456_);
   }

   public int hashCode() {
      return this.source.hashCode();
   }

   public boolean equals(Object p_130144_) {
      return this.source.equals(p_130144_);
   }

   public int compareTo(ByteBuf p_130123_) {
      return this.source.compareTo(p_130123_);
   }

   public String toString() {
      return this.source.toString();
   }

   public FriendlyByteBuf retain(int p_299349_) {
      this.source.retain(p_299349_);
      return this;
   }

   public FriendlyByteBuf retain() {
      this.source.retain();
      return this;
   }

   public FriendlyByteBuf touch() {
      this.source.touch();
      return this;
   }

   public FriendlyByteBuf touch(Object p_299243_) {
      this.source.touch(p_299243_);
      return this;
   }

   public int refCnt() {
      return this.source.refCnt();
   }

   public boolean release() {
      return this.source.release();
   }

   public boolean release(int p_130347_) {
      return this.source.release(p_130347_);
   }
}
