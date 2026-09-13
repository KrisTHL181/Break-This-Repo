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

   public FriendlyByteBuf(final ByteBuf source) {
      this.source = source;
   }

   @Deprecated
   public <T> T readWithCodecTrusted(final DynamicOps<Tag> ops, final Codec<T> codec) {
      return this.readWithCodec(ops, codec, NbtAccounter.unlimitedHeap());
   }

   @Deprecated
   public <T> T readWithCodec(final DynamicOps<Tag> ops, final Codec<T> codec, final NbtAccounter accounter) {
      Tag tag = this.readNbt(accounter);
      return (T)codec.parse(ops, tag).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + tag));
   }

   @Deprecated
   public <T> FriendlyByteBuf writeWithCodec(final DynamicOps<Tag> ops, final Codec<T> codec, final T value) {
      Tag tag = (Tag)codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + value));
      this.writeNbt(tag);
      return this;
   }

   public <T> T readLenientJsonWithCodec(final Codec<T> codec) {
      JsonElement json = LenientJsonParser.parse(this.readUtf());
      DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
      return (T)result.getOrThrow(error -> new DecoderException("Failed to decode JSON: " + error));
   }

   public <T> void writeJsonWithCodec(final Codec<T> codec, final T value) {
      DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, value);
      this.writeUtf(GSON.toJson((JsonElement)result.getOrThrow(error -> new EncoderException("Failed to encode: " + error + " " + value))));
   }

   public static <T> IntFunction<T> limitValue(final IntFunction<T> original, final int limit) {
      return value -> {
         if (value > limit) {
            throw new DecoderException("Value " + value + " is larger than limit " + limit);
         } else {
            return original.apply(value);
         }
      };
   }

   public <T, C extends Collection<T>> C readCollection(final IntFunction<C> ctor, final StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
      int count = this.readVarInt();
      C result = (C)ctor.apply(count);

      for (int i = 0; i < count; i++) {
         result.add(elementDecoder.decode(this));
      }

      return result;
   }

   public <T> void writeCollection(final Collection<T> collection, final StreamEncoder<? super FriendlyByteBuf, T> encoder) {
      this.writeVarInt(collection.size());

      for (T element : collection) {
         encoder.encode(this, element);
      }
   }

   public <T> List<T> readList(final StreamDecoder<? super FriendlyByteBuf, T> elementDecoder) {
      return this.readCollection(Lists::newArrayListWithCapacity, elementDecoder);
   }

   public IntList readIntIdList() {
      int count = this.readVarInt();
      IntList result = new IntArrayList();

      for (int i = 0; i < count; i++) {
         result.add(this.readVarInt());
      }

      return result;
   }

   public void writeIntIdList(final IntList ids) {
      this.writeVarInt(ids.size());
      ids.forEach(this::writeVarInt);
   }

   public <K, V, M extends Map<K, V>> M readMap(
      final IntFunction<M> ctor, final StreamDecoder<? super FriendlyByteBuf, K> keyDecoder, final StreamDecoder<? super FriendlyByteBuf, V> valueDecoder
   ) {
      int count = this.readVarInt();
      M result = (M)ctor.apply(count);

      for (int i = 0; i < count; i++) {
         K key = keyDecoder.decode(this);
         V value = valueDecoder.decode(this);
         result.put(key, value);
      }

      return result;
   }

   public <K, V> Map<K, V> readMap(final StreamDecoder<? super FriendlyByteBuf, K> keyDecoder, final StreamDecoder<? super FriendlyByteBuf, V> valueDecoder) {
      return this.readMap(Maps::newHashMapWithExpectedSize, keyDecoder, valueDecoder);
   }

   public <K, V> void writeMap(
      final Map<K, V> map, final StreamEncoder<? super FriendlyByteBuf, K> keyEncoder, final StreamEncoder<? super FriendlyByteBuf, V> valueEncoder
   ) {
      this.writeVarInt(map.size());
      map.forEach((k, v) -> {
         keyEncoder.encode(this, (K)k);
         valueEncoder.encode(this, (V)v);
      });
   }

   public void readWithCount(final Consumer<FriendlyByteBuf> reader) {
      int count = this.readVarInt();

      for (int i = 0; i < count; i++) {
         reader.accept(this);
      }
   }

   public <E extends Enum<E>> void writeEnumSet(final EnumSet<E> set, final Class<E> clazz) {
      E[] values = (E[])clazz.getEnumConstants();
      BitSet mask = new BitSet(values.length);

      for (int i = 0; i < values.length; i++) {
         mask.set(i, set.contains(values[i]));
      }

      this.writeFixedBitSet(mask, values.length);
   }

   public <E extends Enum<E>> EnumSet<E> readEnumSet(final Class<E> clazz) {
      E[] values = (E[])clazz.getEnumConstants();
      BitSet mask = this.readFixedBitSet(values.length);
      EnumSet<E> result = EnumSet.noneOf(clazz);

      for (int i = 0; i < values.length; i++) {
         if (mask.get(i)) {
            result.add(values[i]);
         }
      }

      return result;
   }

   public <T> void writeOptional(final Optional<T> value, final StreamEncoder<? super FriendlyByteBuf, T> valueWriter) {
      if (value.isPresent()) {
         this.writeBoolean(true);
         valueWriter.encode(this, value.get());
      } else {
         this.writeBoolean(false);
      }
   }

   public <T> Optional<T> readOptional(final StreamDecoder<? super FriendlyByteBuf, T> valueReader) {
      return this.readBoolean() ? Optional.of(valueReader.decode(this)) : Optional.empty();
   }

   public <L, R> void writeEither(
      final Either<L, R> value, final StreamEncoder<? super FriendlyByteBuf, L> leftWriter, final StreamEncoder<? super FriendlyByteBuf, R> rightWriter
   ) {
      value.ifLeft(left -> {
         this.writeBoolean(true);
         leftWriter.encode(this, (L)left);
      }).ifRight(right -> {
         this.writeBoolean(false);
         rightWriter.encode(this, (R)right);
      });
   }

   public <L, R> Either<L, R> readEither(
      final StreamDecoder<? super FriendlyByteBuf, L> leftReader, final StreamDecoder<? super FriendlyByteBuf, R> rightReader
   ) {
      return this.readBoolean() ? Either.left(leftReader.decode(this)) : Either.right(rightReader.decode(this));
   }

   public <T> @Nullable T readNullable(final StreamDecoder<? super FriendlyByteBuf, T> valueDecoder) {
      return readNullable(this, valueDecoder);
   }

   public static <T, B extends ByteBuf> @Nullable T readNullable(final B input, final StreamDecoder<? super B, T> valueDecoder) {
      return input.readBoolean() ? valueDecoder.decode(input) : null;
   }

   public <T> void writeNullable(final @Nullable T value, final StreamEncoder<? super FriendlyByteBuf, T> valueEncoder) {
      writeNullable(this, value, valueEncoder);
   }

   public static <T, B extends ByteBuf> void writeNullable(final B output, final @Nullable T value, final StreamEncoder<? super B, T> valueEncoder) {
      if (value != null) {
         output.writeBoolean(true);
         valueEncoder.encode(output, value);
      } else {
         output.writeBoolean(false);
      }
   }

   public byte[] readByteArray() {
      return readByteArray(this);
   }

   public static byte[] readByteArray(final ByteBuf input) {
      return readByteArray(input, input.readableBytes());
   }

   public FriendlyByteBuf writeByteArray(final byte[] bytes) {
      writeByteArray(this, bytes);
      return this;
   }

   public static void writeByteArray(final ByteBuf output, final byte[] bytes) {
      VarInt.write(output, bytes.length);
      output.writeBytes(bytes);
   }

   public byte[] readByteArray(final int maxSize) {
      return readByteArray(this, maxSize);
   }

   public static byte[] readByteArray(final ByteBuf input, final int maxSize) {
      int size = VarInt.read(input);
      if (size > maxSize) {
         throw new DecoderException("ByteArray with size " + size + " is bigger than allowed " + maxSize);
      }

      byte[] bytes = new byte[size];
      input.readBytes(bytes);
      return bytes;
   }

   public FriendlyByteBuf writeVarIntArray(final int[] ints) {
      this.writeVarInt(ints.length);

      for (int i : ints) {
         this.writeVarInt(i);
      }

      return this;
   }

   public int[] readVarIntArray() {
      return this.readVarIntArray(this.readableBytes());
   }

   public int[] readVarIntArray(final int maxSize) {
      int size = this.readVarInt();
      if (size > maxSize) {
         throw new DecoderException("VarIntArray with size " + size + " is bigger than allowed " + maxSize);
      }

      int[] ints = new int[size];

      for (int i = 0; i < ints.length; i++) {
         ints[i] = this.readVarInt();
      }

      return ints;
   }

   public FriendlyByteBuf writeLongArray(final long[] longs) {
      writeLongArray(this, longs);
      return this;
   }

   public static void writeLongArray(final ByteBuf output, final long[] longs) {
      VarInt.write(output, longs.length);
      writeFixedSizeLongArray(output, longs);
   }

   public FriendlyByteBuf writeFixedSizeLongArray(final long[] longs) {
      writeFixedSizeLongArray(this, longs);
      return this;
   }

   public static void writeFixedSizeLongArray(final ByteBuf output, final long[] longs) {
      for (long l : longs) {
         output.writeLong(l);
      }
   }

   public long[] readLongArray() {
      return readLongArray(this);
   }

   public long[] readFixedSizeLongArray(final long[] output) {
      return readFixedSizeLongArray(this, output);
   }

   public static long[] readLongArray(final ByteBuf input) {
      int size = VarInt.read(input);
      int maxSize = input.readableBytes() / 8;
      if (size > maxSize) {
         throw new DecoderException("LongArray with size " + size + " is bigger than allowed " + maxSize);
      } else {
         return readFixedSizeLongArray(input, new long[size]);
      }
   }

   public static long[] readFixedSizeLongArray(final ByteBuf input, final long[] output) {
      for (int i = 0; i < output.length; i++) {
         output[i] = input.readLong();
      }

      return output;
   }

   public BlockPos readBlockPos() {
      return readBlockPos(this);
   }

   public static BlockPos readBlockPos(final ByteBuf input) {
      return BlockPos.of(input.readLong());
   }

   public FriendlyByteBuf writeBlockPos(final BlockPos pos) {
      writeBlockPos(this, pos);
      return this;
   }

   public static void writeBlockPos(final ByteBuf output, final BlockPos pos) {
      output.writeLong(pos.asLong());
   }

   public ChunkPos readChunkPos() {
      return ChunkPos.unpack(this.readLong());
   }

   public FriendlyByteBuf writeChunkPos(final ChunkPos pos) {
      this.writeLong(pos.pack());
      return this;
   }

   public static ChunkPos readChunkPos(final ByteBuf input) {
      return ChunkPos.unpack(input.readLong());
   }

   public static void writeChunkPos(final ByteBuf output, final ChunkPos chunkPos) {
      output.writeLong(chunkPos.pack());
   }

   public GlobalPos readGlobalPos() {
      ResourceKey<Level> dimension = this.readResourceKey(Registries.DIMENSION);
      BlockPos pos = this.readBlockPos();
      return GlobalPos.of(dimension, pos);
   }

   public void writeGlobalPos(final GlobalPos globalPos) {
      this.writeResourceKey(globalPos.dimension());
      this.writeBlockPos(globalPos.pos());
   }

   public Vector3f readVector3f() {
      return readVector3f(this);
   }

   public static Vector3f readVector3f(final ByteBuf input) {
      return new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
   }

   public void writeVector3f(final Vector3f v) {
      writeVector3f(this, v);
   }

   public static void writeVector3f(final ByteBuf output, final Vector3fc v) {
      output.writeFloat(v.x());
      output.writeFloat(v.y());
      output.writeFloat(v.z());
   }

   public Quaternionf readQuaternion() {
      return readQuaternion(this);
   }

   public static Quaternionf readQuaternion(final ByteBuf input) {
      return new Quaternionf(input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
   }

   public void writeQuaternion(final Quaternionf q) {
      writeQuaternion(this, q);
   }

   public static void writeQuaternion(final ByteBuf output, final Quaternionfc value) {
      output.writeFloat(value.x());
      output.writeFloat(value.y());
      output.writeFloat(value.z());
      output.writeFloat(value.w());
   }

   public <T extends Enum<T>> T readEnum(final Class<T> clazz) {
      return clazz.getEnumConstants()[this.readVarInt()];
   }

   public FriendlyByteBuf writeEnum(final Enum<?> value) {
      return this.writeVarInt(value.ordinal());
   }

   public <T> T readById(final IntFunction<T> converter) {
      int id = this.readVarInt();
      return converter.apply(id);
   }

   public <T> FriendlyByteBuf writeById(final ToIntFunction<T> converter, final T value) {
      int id = converter.applyAsInt(value);
      return this.writeVarInt(id);
   }

   public int readVarInt() {
      return VarInt.read(this.source);
   }

   public long readVarLong() {
      return VarLong.read(this.source);
   }

   public FriendlyByteBuf writeUUID(final UUID uuid) {
      writeUUID(this, uuid);
      return this;
   }

   public static void writeUUID(final ByteBuf output, final UUID uuid) {
      output.writeLong(uuid.getMostSignificantBits());
      output.writeLong(uuid.getLeastSignificantBits());
   }

   public UUID readUUID() {
      return readUUID(this);
   }

   public static UUID readUUID(final ByteBuf input) {
      return new UUID(input.readLong(), input.readLong());
   }

   public FriendlyByteBuf writeVarInt(final int value) {
      VarInt.write(this.source, value);
      return this;
   }

   public FriendlyByteBuf writeVarLong(final long value) {
      VarLong.write(this.source, value);
      return this;
   }

   public FriendlyByteBuf writeNbt(final @Nullable Tag tag) {
      writeNbt(this, tag);
      return this;
   }

   public static void writeNbt(final ByteBuf output, @Nullable Tag tag) {
      if (tag == null) {
         tag = EndTag.INSTANCE;
      }

      try {
         NbtIo.writeAnyTag(tag, new ByteBufOutputStream(output));
      } catch (IOException e) {
         throw new EncoderException(e);
      }
   }

   public @Nullable CompoundTag readNbt() {
      return readNbt(this);
   }

   public static @Nullable CompoundTag readNbt(final ByteBuf input) {
      Tag result = readNbt(input, NbtAccounter.defaultQuota());
      if (result != null && !(result instanceof CompoundTag)) {
         throw new DecoderException("Not a compound tag: " + result);
      } else {
         return (CompoundTag)result;
      }
   }

   public static @Nullable Tag readNbt(final ByteBuf input, final NbtAccounter accounter) {
      try {
         Tag tag = NbtIo.readAnyTag(new ByteBufInputStream(input), accounter);
         return tag.getId() == 0 ? null : tag;
      } catch (IOException e) {
         throw new EncoderException(e);
      }
   }

   public @Nullable Tag readNbt(final NbtAccounter accounter) {
      return readNbt(this, accounter);
   }

   public String readUtf() {
      return this.readUtf(32767);
   }

   public String readUtf(final int maxLength) {
      return Utf8String.read(this.source, maxLength);
   }

   public FriendlyByteBuf writeUtf(final String value) {
      return this.writeUtf(value, 32767);
   }

   public FriendlyByteBuf writeUtf(final String value, final int maxLength) {
      Utf8String.write(this.source, value, maxLength);
      return this;
   }

   public Identifier readIdentifier() {
      return Identifier.parse(this.readUtf(32767));
   }

   public FriendlyByteBuf writeIdentifier(final Identifier identifier) {
      this.writeUtf(identifier.toString());
      return this;
   }

   public <T> ResourceKey<T> readResourceKey(final ResourceKey<? extends Registry<T>> registry) {
      Identifier id = this.readIdentifier();
      return ResourceKey.create(registry, id);
   }

   public void writeResourceKey(final ResourceKey<?> key) {
      this.writeIdentifier(key.identifier());
   }

   public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
      Identifier id = this.readIdentifier();
      return ResourceKey.createRegistryKey(id);
   }

   public Instant readInstant() {
      return Instant.ofEpochMilli(this.readLong());
   }

   public void writeInstant(final Instant value) {
      this.writeLong(value.toEpochMilli());
   }

   public PublicKey readPublicKey() {
      try {
         return Crypt.byteToPublicKey(this.readByteArray(512));
      } catch (CryptException e) {
         throw new DecoderException("Malformed public key bytes", e);
      }
   }

   public FriendlyByteBuf writePublicKey(final PublicKey publicKey) {
      this.writeByteArray(publicKey.getEncoded());
      return this;
   }

   public BlockHitResult readBlockHitResult() {
      BlockPos pos = this.readBlockPos();
      Direction face = this.readEnum(Direction.class);
      float clickX = this.readFloat();
      float clickY = this.readFloat();
      float clickZ = this.readFloat();
      boolean inside = this.readBoolean();
      boolean worldBorder = this.readBoolean();
      return new BlockHitResult(new Vec3((double)pos.getX() + clickX, (double)pos.getY() + clickY, (double)pos.getZ() + clickZ), face, pos, inside, worldBorder);
   }

   public void writeBlockHitResult(final BlockHitResult blockHit) {
      BlockPos blockPos = blockHit.getBlockPos();
      this.writeBlockPos(blockPos);
      this.writeEnum(blockHit.getDirection());
      Vec3 location = blockHit.getLocation();
      this.writeFloat((float)(location.x - blockPos.getX()));
      this.writeFloat((float)(location.y - blockPos.getY()));
      this.writeFloat((float)(location.z - blockPos.getZ()));
      this.writeBoolean(blockHit.isInside());
      this.writeBoolean(blockHit.isWorldBorderHit());
   }

   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   public void writeBitSet(final BitSet bitSet) {
      this.writeLongArray(bitSet.toLongArray());
   }

   public BitSet readFixedBitSet(final int size) {
      byte[] bytes = new byte[Mth.positiveCeilDiv(size, 8)];
      this.readBytes(bytes);
      return BitSet.valueOf(bytes);
   }

   public void writeFixedBitSet(final BitSet bitSet, final int size) {
      if (bitSet.length() > size) {
         throw new EncoderException("BitSet is larger than expected size (" + bitSet.length() + ">" + size + ")");
      }

      byte[] bytes = bitSet.toByteArray();
      this.writeBytes(Arrays.copyOf(bytes, Mth.positiveCeilDiv(size, 8)));
   }

   public static int readContainerId(final ByteBuf input) {
      return VarInt.read(input);
   }

   public int readContainerId() {
      return readContainerId(this.source);
   }

   public static void writeContainerId(final ByteBuf output, final int id) {
      VarInt.write(output, id);
   }

   public void writeContainerId(final int id) {
      writeContainerId(this.source, id);
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

   public FriendlyByteBuf capacity(final int newCapacity) {
      this.source.capacity(newCapacity);
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

   public ByteBuf order(final ByteOrder endianness) {
      return this.source.order(endianness);
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

   public FriendlyByteBuf readerIndex(final int readerIndex) {
      this.source.readerIndex(readerIndex);
      return this;
   }

   public int writerIndex() {
      return this.source.writerIndex();
   }

   public FriendlyByteBuf writerIndex(final int writerIndex) {
      this.source.writerIndex(writerIndex);
      return this;
   }

   public FriendlyByteBuf setIndex(final int readerIndex, final int writerIndex) {
      this.source.setIndex(readerIndex, writerIndex);
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

   public boolean isReadable(final int size) {
      return this.source.isReadable(size);
   }

   public boolean isWritable() {
      return this.source.isWritable();
   }

   public boolean isWritable(final int size) {
      return this.source.isWritable(size);
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

   public FriendlyByteBuf ensureWritable(final int minWritableBytes) {
      this.source.ensureWritable(minWritableBytes);
      return this;
   }

   public int ensureWritable(final int minWritableBytes, final boolean force) {
      return this.source.ensureWritable(minWritableBytes, force);
   }

   public boolean getBoolean(final int index) {
      return this.source.getBoolean(index);
   }

   public byte getByte(final int index) {
      return this.source.getByte(index);
   }

   public short getUnsignedByte(final int index) {
      return this.source.getUnsignedByte(index);
   }

   public short getShort(final int index) {
      return this.source.getShort(index);
   }

   public short getShortLE(final int index) {
      return this.source.getShortLE(index);
   }

   public int getUnsignedShort(final int index) {
      return this.source.getUnsignedShort(index);
   }

   public int getUnsignedShortLE(final int index) {
      return this.source.getUnsignedShortLE(index);
   }

   public int getMedium(final int index) {
      return this.source.getMedium(index);
   }

   public int getMediumLE(final int index) {
      return this.source.getMediumLE(index);
   }

   public int getUnsignedMedium(final int index) {
      return this.source.getUnsignedMedium(index);
   }

   public int getUnsignedMediumLE(final int index) {
      return this.source.getUnsignedMediumLE(index);
   }

   public int getInt(final int index) {
      return this.source.getInt(index);
   }

   public int getIntLE(final int index) {
      return this.source.getIntLE(index);
   }

   public long getUnsignedInt(final int index) {
      return this.source.getUnsignedInt(index);
   }

   public long getUnsignedIntLE(final int index) {
      return this.source.getUnsignedIntLE(index);
   }

   public long getLong(final int index) {
      return this.source.getLong(index);
   }

   public long getLongLE(final int index) {
      return this.source.getLongLE(index);
   }

   public char getChar(final int index) {
      return this.source.getChar(index);
   }

   public float getFloat(final int index) {
      return this.source.getFloat(index);
   }

   public double getDouble(final int index) {
      return this.source.getDouble(index);
   }

   public FriendlyByteBuf getBytes(final int index, final ByteBuf dst) {
      this.source.getBytes(index, dst);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final ByteBuf dst, final int length) {
      this.source.getBytes(index, dst, length);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final ByteBuf dst, final int dstIndex, final int length) {
      this.source.getBytes(index, dst, dstIndex, length);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final byte[] dst) {
      this.source.getBytes(index, dst);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final byte[] dst, final int dstIndex, final int length) {
      this.source.getBytes(index, dst, dstIndex, length);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final ByteBuffer dst) {
      this.source.getBytes(index, dst);
      return this;
   }

   public FriendlyByteBuf getBytes(final int index, final OutputStream out, final int length) throws IOException {
      this.source.getBytes(index, out, length);
      return this;
   }

   public int getBytes(final int index, final GatheringByteChannel out, final int length) throws IOException {
      return this.source.getBytes(index, out, length);
   }

   public int getBytes(final int index, final FileChannel out, final long position, final int length) throws IOException {
      return this.source.getBytes(index, out, position, length);
   }

   public CharSequence getCharSequence(final int index, final int length, final Charset charset) {
      return this.source.getCharSequence(index, length, charset);
   }

   public FriendlyByteBuf setBoolean(final int index, final boolean value) {
      this.source.setBoolean(index, value);
      return this;
   }

   public FriendlyByteBuf setByte(final int index, final int value) {
      this.source.setByte(index, value);
      return this;
   }

   public FriendlyByteBuf setShort(final int index, final int value) {
      this.source.setShort(index, value);
      return this;
   }

   public FriendlyByteBuf setShortLE(final int index, final int value) {
      this.source.setShortLE(index, value);
      return this;
   }

   public FriendlyByteBuf setMedium(final int index, final int value) {
      this.source.setMedium(index, value);
      return this;
   }

   public FriendlyByteBuf setMediumLE(final int index, final int value) {
      this.source.setMediumLE(index, value);
      return this;
   }

   public FriendlyByteBuf setInt(final int index, final int value) {
      this.source.setInt(index, value);
      return this;
   }

   public FriendlyByteBuf setIntLE(final int index, final int value) {
      this.source.setIntLE(index, value);
      return this;
   }

   public FriendlyByteBuf setLong(final int index, final long value) {
      this.source.setLong(index, value);
      return this;
   }

   public FriendlyByteBuf setLongLE(final int index, final long value) {
      this.source.setLongLE(index, value);
      return this;
   }

   public FriendlyByteBuf setChar(final int index, final int value) {
      this.source.setChar(index, value);
      return this;
   }

   public FriendlyByteBuf setFloat(final int index, final float value) {
      this.source.setFloat(index, value);
      return this;
   }

   public FriendlyByteBuf setDouble(final int index, final double value) {
      this.source.setDouble(index, value);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final ByteBuf src) {
      this.source.setBytes(index, src);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final ByteBuf src, final int length) {
      this.source.setBytes(index, src, length);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final ByteBuf src, final int srcIndex, final int length) {
      this.source.setBytes(index, src, srcIndex, length);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final byte[] src) {
      this.source.setBytes(index, src);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final byte[] src, final int srcIndex, final int length) {
      this.source.setBytes(index, src, srcIndex, length);
      return this;
   }

   public FriendlyByteBuf setBytes(final int index, final ByteBuffer src) {
      this.source.setBytes(index, src);
      return this;
   }

   public int setBytes(final int index, final InputStream in, final int length) throws IOException {
      return this.source.setBytes(index, in, length);
   }

   public int setBytes(final int index, final ScatteringByteChannel in, final int length) throws IOException {
      return this.source.setBytes(index, in, length);
   }

   public int setBytes(final int index, final FileChannel in, final long position, final int length) throws IOException {
      return this.source.setBytes(index, in, position, length);
   }

   public FriendlyByteBuf setZero(final int index, final int length) {
      this.source.setZero(index, length);
      return this;
   }

   public int setCharSequence(final int index, final CharSequence sequence, final Charset charset) {
      return this.source.setCharSequence(index, sequence, charset);
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

   public ByteBuf readBytes(final int length) {
      return this.source.readBytes(length);
   }

   public ByteBuf readSlice(final int length) {
      return this.source.readSlice(length);
   }

   public ByteBuf readRetainedSlice(final int length) {
      return this.source.readRetainedSlice(length);
   }

   public FriendlyByteBuf readBytes(final ByteBuf dst) {
      this.source.readBytes(dst);
      return this;
   }

   public FriendlyByteBuf readBytes(final ByteBuf dst, final int length) {
      this.source.readBytes(dst, length);
      return this;
   }

   public FriendlyByteBuf readBytes(final ByteBuf dst, final int dstIndex, final int length) {
      this.source.readBytes(dst, dstIndex, length);
      return this;
   }

   public FriendlyByteBuf readBytes(final byte[] dst) {
      this.source.readBytes(dst);
      return this;
   }

   public FriendlyByteBuf readBytes(final byte[] dst, final int dstIndex, final int length) {
      this.source.readBytes(dst, dstIndex, length);
      return this;
   }

   public FriendlyByteBuf readBytes(final ByteBuffer dst) {
      this.source.readBytes(dst);
      return this;
   }

   public FriendlyByteBuf readBytes(final OutputStream out, final int length) throws IOException {
      this.source.readBytes(out, length);
      return this;
   }

   public int readBytes(final GatheringByteChannel out, final int length) throws IOException {
      return this.source.readBytes(out, length);
   }

   public CharSequence readCharSequence(final int length, final Charset charset) {
      return this.source.readCharSequence(length, charset);
   }

   public String readString(final int length, final Charset charset) {
      return this.source.readString(length, charset);
   }

   public int readBytes(final FileChannel out, final long position, final int length) throws IOException {
      return this.source.readBytes(out, position, length);
   }

   public FriendlyByteBuf skipBytes(final int length) {
      this.source.skipBytes(length);
      return this;
   }

   public FriendlyByteBuf writeBoolean(final boolean value) {
      this.source.writeBoolean(value);
      return this;
   }

   public FriendlyByteBuf writeByte(final int value) {
      this.source.writeByte(value);
      return this;
   }

   public FriendlyByteBuf writeShort(final int value) {
      this.source.writeShort(value);
      return this;
   }

   public FriendlyByteBuf writeShortLE(final int value) {
      this.source.writeShortLE(value);
      return this;
   }

   public FriendlyByteBuf writeMedium(final int value) {
      this.source.writeMedium(value);
      return this;
   }

   public FriendlyByteBuf writeMediumLE(final int value) {
      this.source.writeMediumLE(value);
      return this;
   }

   public FriendlyByteBuf writeInt(final int value) {
      this.source.writeInt(value);
      return this;
   }

   public FriendlyByteBuf writeIntLE(final int value) {
      this.source.writeIntLE(value);
      return this;
   }

   public FriendlyByteBuf writeLong(final long value) {
      this.source.writeLong(value);
      return this;
   }

   public FriendlyByteBuf writeLongLE(final long value) {
      this.source.writeLongLE(value);
      return this;
   }

   public FriendlyByteBuf writeChar(final int value) {
      this.source.writeChar(value);
      return this;
   }

   public FriendlyByteBuf writeFloat(final float value) {
      this.source.writeFloat(value);
      return this;
   }

   public FriendlyByteBuf writeDouble(final double value) {
      this.source.writeDouble(value);
      return this;
   }

   public FriendlyByteBuf writeBytes(final ByteBuf src) {
      this.source.writeBytes(src);
      return this;
   }

   public FriendlyByteBuf writeBytes(final ByteBuf src, final int length) {
      this.source.writeBytes(src, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(final ByteBuf src, final int srcIndex, final int length) {
      this.source.writeBytes(src, srcIndex, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(final byte[] src) {
      this.source.writeBytes(src);
      return this;
   }

   public FriendlyByteBuf writeBytes(final byte[] src, final int srcIndex, final int length) {
      this.source.writeBytes(src, srcIndex, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(final ByteBuffer src) {
      this.source.writeBytes(src);
      return this;
   }

   public int writeBytes(final InputStream in, final int length) throws IOException {
      return this.source.writeBytes(in, length);
   }

   public int writeBytes(final ScatteringByteChannel in, final int length) throws IOException {
      return this.source.writeBytes(in, length);
   }

   public int writeBytes(final FileChannel in, final long position, final int length) throws IOException {
      return this.source.writeBytes(in, position, length);
   }

   public FriendlyByteBuf writeZero(final int length) {
      this.source.writeZero(length);
      return this;
   }

   public int writeCharSequence(final CharSequence sequence, final Charset charset) {
      return this.source.writeCharSequence(sequence, charset);
   }

   public int indexOf(final int fromIndex, final int toIndex, final byte value) {
      return this.source.indexOf(fromIndex, toIndex, value);
   }

   public int bytesBefore(final byte value) {
      return this.source.bytesBefore(value);
   }

   public int bytesBefore(final int length, final byte value) {
      return this.source.bytesBefore(length, value);
   }

   public int bytesBefore(final int index, final int length, final byte value) {
      return this.source.bytesBefore(index, length, value);
   }

   public int forEachByte(final ByteProcessor processor) {
      return this.source.forEachByte(processor);
   }

   public int forEachByte(final int index, final int length, final ByteProcessor processor) {
      return this.source.forEachByte(index, length, processor);
   }

   public int forEachByteDesc(final ByteProcessor processor) {
      return this.source.forEachByteDesc(processor);
   }

   public int forEachByteDesc(final int index, final int length, final ByteProcessor processor) {
      return this.source.forEachByteDesc(index, length, processor);
   }

   public ByteBuf copy() {
      return this.source.copy();
   }

   public ByteBuf copy(final int index, final int length) {
      return this.source.copy(index, length);
   }

   public ByteBuf slice() {
      return this.source.slice();
   }

   public ByteBuf retainedSlice() {
      return this.source.retainedSlice();
   }

   public ByteBuf slice(final int index, final int length) {
      return this.source.slice(index, length);
   }

   public ByteBuf retainedSlice(final int index, final int length) {
      return this.source.retainedSlice(index, length);
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

   public ByteBuffer nioBuffer(final int index, final int length) {
      return this.source.nioBuffer(index, length);
   }

   public ByteBuffer internalNioBuffer(final int index, final int length) {
      return this.source.internalNioBuffer(index, length);
   }

   public ByteBuffer[] nioBuffers() {
      return this.source.nioBuffers();
   }

   public ByteBuffer[] nioBuffers(final int index, final int length) {
      return this.source.nioBuffers(index, length);
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

   public String toString(final Charset charset) {
      return this.source.toString(charset);
   }

   public String toString(final int index, final int length, final Charset charset) {
      return this.source.toString(index, length, charset);
   }

   public int hashCode() {
      return this.source.hashCode();
   }

   public boolean equals(final Object obj) {
      return this.source.equals(obj);
   }

   public int compareTo(final ByteBuf buffer) {
      return this.source.compareTo(buffer);
   }

   public String toString() {
      return this.source.toString();
   }

   public FriendlyByteBuf retain(final int increment) {
      this.source.retain(increment);
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

   public FriendlyByteBuf touch(final Object hint) {
      this.source.touch(hint);
      return this;
   }

   public int refCnt() {
      return this.source.refCnt();
   }

   public boolean release() {
      return this.source.release();
   }

   public boolean release(final int decrement) {
      return this.source.release(decrement);
   }
}
