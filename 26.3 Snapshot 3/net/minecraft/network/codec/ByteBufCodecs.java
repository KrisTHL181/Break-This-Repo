package net.minecraft.network.codec;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.VarLong;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface ByteBufCodecs {
   int MAX_INITIAL_COLLECTION_SIZE = 65536;
   StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<ByteBuf, Boolean>() {
      public Boolean decode(final ByteBuf input) {
         return input.readBoolean();
      }

      public void encode(final ByteBuf output, final Boolean value) {
         output.writeBoolean(value);
      }
   };
   StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>() {
      public Byte decode(final ByteBuf input) {
         return input.readByte();
      }

      public void encode(final ByteBuf output, final Byte value) {
         output.writeByte(value);
      }
   };
   StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
   StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>() {
      public Short decode(final ByteBuf input) {
         return input.readShort();
      }

      public void encode(final ByteBuf output, final Short value) {
         output.writeShort(value);
      }
   };
   StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(final ByteBuf input) {
         return input.readUnsignedShort();
      }

      public void encode(final ByteBuf output, final Integer value) {
         output.writeShort(value);
      }
   };
   StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(final ByteBuf input) {
         return input.readInt();
      }

      public void encode(final ByteBuf output, final Integer value) {
         output.writeInt(value);
      }
   };
   StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(final ByteBuf input) {
         return VarInt.read(input);
      }

      public void encode(final ByteBuf output, final Integer value) {
         VarInt.write(output, value);
      }
   };
   StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
      i -> i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1), o -> o.isPresent() ? o.getAsInt() + 1 : 0
   );
   StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(final ByteBuf input) {
         return input.readLong();
      }

      public void encode(final ByteBuf output, final Long value) {
         output.writeLong(value);
      }
   };
   StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(final ByteBuf input) {
         return VarLong.read(input);
      }

      public void encode(final ByteBuf output, final Long value) {
         VarLong.write(output, value);
      }
   };
   StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>() {
      public Float decode(final ByteBuf input) {
         return input.readFloat();
      }

      public void encode(final ByteBuf output, final Float value) {
         output.writeFloat(value);
      }
   };
   StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>() {
      public Double decode(final ByteBuf input) {
         return input.readDouble();
      }

      public void encode(final ByteBuf output, final Double value) {
         output.writeDouble(value);
      }
   };
   StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>() {
      public byte[] decode(final ByteBuf input) {
         return FriendlyByteBuf.readByteArray(input);
      }

      public void encode(final ByteBuf output, final byte[] value) {
         FriendlyByteBuf.writeByteArray(output, value);
      }
   };
   StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>() {
      public long[] decode(final ByteBuf input) {
         return FriendlyByteBuf.readLongArray(input);
      }

      public void encode(final ByteBuf output, final long[] value) {
         FriendlyByteBuf.writeLongArray(output, value);
      }
   };
   StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
   StreamCodec<ByteBuf, Tag> TAG = tagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>() {
      public Optional<CompoundTag> decode(final ByteBuf input) {
         return Optional.ofNullable(FriendlyByteBuf.readNbt(input));
      }

      public void encode(final ByteBuf output, final Optional<CompoundTag> value) {
         FriendlyByteBuf.writeNbt(output, value.orElse(null));
      }
   };
   StreamCodec<ByteBuf, Vector3fc> VECTOR3F = new StreamCodec<ByteBuf, Vector3fc>() {
      public Vector3fc decode(final ByteBuf input) {
         return FriendlyByteBuf.readVector3f(input);
      }

      public void encode(final ByteBuf output, final Vector3fc value) {
         FriendlyByteBuf.writeVector3f(output, value);
      }
   };
   StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionfc>() {
      public Quaternionfc decode(final ByteBuf input) {
         return FriendlyByteBuf.readQuaternion(input);
      }

      public void encode(final ByteBuf output, final Quaternionfc value) {
         FriendlyByteBuf.writeQuaternion(output, value);
      }
   };
   StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(final ByteBuf input) {
         return FriendlyByteBuf.readContainerId(input);
      }

      public void encode(final ByteBuf output, final Integer value) {
         FriendlyByteBuf.writeContainerId(output, value);
      }
   };
   StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>() {
      public PropertyMap decode(final ByteBuf input) {
         int propertyCount = ByteBufCodecs.readCount(input, 16);
         Builder<String, Property> result = ImmutableMultimap.builder();

         for (int i = 0; i < propertyCount; i++) {
            String name = Utf8String.read(input, 64);
            String value = Utf8String.read(input, 32767);
            String signature = FriendlyByteBuf.readNullable(input, in -> Utf8String.read(in, 1024));
            Property property = new Property(name, value, signature);
            result.put(property.name(), property);
         }

         return new PropertyMap(result.build());
      }

      public void encode(final ByteBuf output, final PropertyMap properties) {
         ByteBufCodecs.writeCount(output, properties.size(), 16);

         for (Property property : properties.values()) {
            Utf8String.write(output, property.name(), 64);
            Utf8String.write(output, property.value(), 32767);
            FriendlyByteBuf.writeNullable(output, property.signature(), (out, signature) -> Utf8String.write(out, signature, 1024));
         }
      }
   };
   StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
   StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new
   );
   StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(final ByteBuf input) {
         return ARGB.color(input.readByte() & 0xFF, input.readByte() & 0xFF, input.readByte() & 0xFF);
      }

      public void encode(final ByteBuf output, final Integer value) {
         output.writeByte(ARGB.red(value));
         output.writeByte(ARGB.green(value));
         output.writeByte(ARGB.blue(value));
      }
   };

   static StreamCodec<ByteBuf, byte[]> byteArray(final int maxSize) {
      return new StreamCodec<ByteBuf, byte[]>() {
         public byte[] decode(final ByteBuf input) {
            return FriendlyByteBuf.readByteArray(input, maxSize);
         }

         public void encode(final ByteBuf output, final byte[] value) {
            if (value.length > maxSize) {
               throw new EncoderException("ByteArray with size " + value.length + " is bigger than allowed " + maxSize);
            }

            FriendlyByteBuf.writeByteArray(output, value);
         }
      };
   }

   static StreamCodec<ByteBuf, String> stringUtf8(final int maxStringLength) {
      return new StreamCodec<ByteBuf, String>() {
         public String decode(final ByteBuf input) {
            return Utf8String.read(input, maxStringLength);
         }

         public void encode(final ByteBuf output, final String value) {
            Utf8String.write(output, value, maxStringLength);
         }
      };
   }

   static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(final Supplier<NbtAccounter> accounter) {
      return new StreamCodec<ByteBuf, Optional<Tag>>() {
         public Optional<Tag> decode(final ByteBuf input) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(input, accounter.get()));
         }

         public void encode(final ByteBuf output, final Optional<Tag> value) {
            FriendlyByteBuf.writeNbt(output, value.orElse(null));
         }
      };
   }

   static StreamCodec<ByteBuf, Tag> tagCodec(final Supplier<NbtAccounter> accounter) {
      return new StreamCodec<ByteBuf, Tag>() {
         public Tag decode(final ByteBuf input) {
            Tag result = FriendlyByteBuf.readNbt(input, accounter.get());
            if (result == null) {
               throw new DecoderException("Expected non-null compound tag");
            } else {
               return result;
            }
         }

         public void encode(final ByteBuf output, final Tag value) {
            if (value == EndTag.INSTANCE) {
               throw new EncoderException("Expected non-null compound tag");
            }

            FriendlyByteBuf.writeNbt(output, value);
         }
      };
   }

   static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(final Supplier<NbtAccounter> accounter) {
      return tagCodec(accounter).map(tag -> {
         if (tag instanceof CompoundTag compoundTag) {
            return compoundTag;
         } else {
            throw new DecoderException("Not a compound tag: " + tag);
         }
      }, compoundTag -> (Tag)compoundTag);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(final Codec<T> codec) {
      return fromCodec(codec, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(final Codec<T> codec) {
      return fromCodec(codec, NbtAccounter::defaultQuota);
   }

   static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(final DynamicOps<T> ops, final Codec<V> codec) {
      return original -> new StreamCodec<B, V>() {
         public V decode(final B input) {
            T payload = (T)original.decode(input);
            return (V)codec.parse(ops, payload).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + payload));
         }

         public void encode(final B output, final V value) {
            T payload = (T)codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + value));
            original.encode(output, payload);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(final Codec<T> codec, final Supplier<NbtAccounter> accounter) {
      return tagCodec(accounter).apply(fromCodec(NbtOps.INSTANCE, codec));
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(final Codec<T> codec) {
      return fromCodecWithRegistries(codec, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> codec) {
      return fromCodecWithRegistries(codec, NbtAccounter::defaultQuota);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> codec, final Supplier<NbtAccounter> accounter) {
      final StreamCodec<ByteBuf, Tag> tagCodec = tagCodec(accounter);
      return new StreamCodec<RegistryFriendlyByteBuf, T>() {
         public T decode(final RegistryFriendlyByteBuf input) {
            Tag tag = tagCodec.decode(input);
            RegistryOps<Tag> ops = input.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return (T)codec.parse(ops, tag).getOrThrow(msg -> new DecoderException("Failed to decode: " + msg + " " + tag));
         }

         public void encode(final RegistryFriendlyByteBuf output, final T value) {
            RegistryOps<Tag> ops = output.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = (Tag)codec.encodeStart(ops, value).getOrThrow(msg -> new EncoderException("Failed to encode: " + msg + " " + value));
            tagCodec.encode(output, tag);
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> original) {
      return new StreamCodec<B, Optional<V>>() {
         public Optional<V> decode(final B input) {
            return input.readBoolean() ? Optional.of(original.decode(input)) : Optional.empty();
         }

         public void encode(final B output, final Optional<V> value) {
            if (value.isPresent()) {
               output.writeBoolean(true);
               original.encode(output, value.get());
            } else {
               output.writeBoolean(false);
            }
         }
      };
   }

   static int readCount(final ByteBuf input, final int maxSize) {
      int count = VarInt.read(input);
      if (count > maxSize) {
         throw new DecoderException(count + " elements exceeded max size of: " + maxSize);
      } else {
         return count;
      }
   }

   static void writeCount(final ByteBuf output, final int count, final int maxSize) {
      if (count > maxSize) {
         throw new EncoderException(count + " elements exceeded max size of: " + maxSize);
      }

      VarInt.write(output, count);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
      final IntFunction<C> constructor, final StreamCodec<? super B, V> elementCodec
   ) {
      return collection(constructor, elementCodec, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
      final IntFunction<C> constructor, final StreamCodec<? super B, V> elementCodec, final int maxSize
   ) {
      return new StreamCodec<B, C>() {
         public C decode(final B input) {
            int count = ByteBufCodecs.readCount(input, maxSize);
            C result = constructor.apply(Math.min(count, 65536));

            for (int i = 0; i < count; i++) {
               result.add(elementCodec.decode(input));
            }

            return result;
         }

         public void encode(final B output, final C value) {
            ByteBufCodecs.writeCount(output, value.size(), maxSize);

            for (V element : value) {
               elementCodec.encode(output, element);
            }
         }
      };
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(final IntFunction<C> constructor) {
      return original -> collection(constructor, original);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(final IntFunction<C> constructor, final int maxSize) {
      return original -> collection(constructor, original, maxSize);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
      return original -> collection(ArrayList::new, original);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(final int maxSize) {
      return original -> collection(ArrayList::new, original, maxSize);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      final IntFunction<? extends M> constructor, final StreamCodec<? super B, K> keyCodec, final StreamCodec<? super B, V> valueCodec
   ) {
      return map(constructor, keyCodec, valueCodec, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      final IntFunction<? extends M> constructor, final StreamCodec<? super B, K> keyCodec, final StreamCodec<? super B, V> valueCodec, final int maxSize
   ) {
      return new StreamCodec<B, M>() {
         public void encode(final B output, final M map) {
            ByteBufCodecs.writeCount(output, map.size(), maxSize);
            map.forEach((k, v) -> {
               keyCodec.encode(output, (K)k);
               valueCodec.encode(output, (V)v);
            });
         }

         public M decode(final B input) {
            int count = ByteBufCodecs.readCount(input, maxSize);
            M result = (M)constructor.apply(Math.min(count, 65536));

            for (int i = 0; i < count; i++) {
               K key = keyCodec.decode(input);
               V value = valueCodec.decode(input);
               result.put(key, value);
            }

            return result;
         }
      };
   }

   static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> leftCodec, final StreamCodec<? super B, R> rightCodec) {
      return new StreamCodec<B, Either<L, R>>() {
         public Either<L, R> decode(final B input) {
            return input.readBoolean() ? Either.left(leftCodec.decode(input)) : Either.right(rightCodec.decode(input));
         }

         public void encode(final B output, final Either<L, R> value) {
            value.ifLeft(left -> {
               output.writeBoolean(true);
               leftCodec.encode(output, (L)left);
            }).ifRight(right -> {
               output.writeBoolean(false);
               rightCodec.encode(output, (R)right);
            });
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(final int maxSize, final BiFunction<B, ByteBuf, B> decorator) {
      return original -> new StreamCodec<B, V>() {
         public V decode(final B input) {
            int size = VarInt.read(input);
            if (size > maxSize) {
               throw new DecoderException("Buffer size " + size + " is larger than allowed limit of " + maxSize);
            }

            int index = input.readerIndex();
            B limitedSlice = (B)((ByteBuf)decorator.apply(input, input.slice(index, size)));
            input.readerIndex(index + size);
            return (V)original.decode(limitedSlice);
         }

         public void encode(final B output, final V value) {
            B scratchBuffer = (B)((ByteBuf)decorator.apply(output, output.alloc().buffer()));

            try {
               original.encode(scratchBuffer, value);
               int size = scratchBuffer.readableBytes();
               if (size > maxSize) {
                  throw new EncoderException("Buffer size " + size + " is  larger than allowed limit of " + maxSize);
               }

               VarInt.write(output, size);
               output.writeBytes(scratchBuffer);
            } finally {
               scratchBuffer.release();
            }
         }
      };
   }

   static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(final int maxSize) {
      return lengthPrefixed(maxSize, (parent, child) -> child);
   }

   static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(final int maxSize) {
      return lengthPrefixed(maxSize, (parent, child) -> new RegistryFriendlyByteBuf(child, parent.registryAccess()));
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> byId, final ToIntFunction<T> toId) {
      return new StreamCodec<ByteBuf, T>() {
         public T decode(final ByteBuf input) {
            int id = VarInt.read(input);
            return byId.apply(id);
         }

         public void encode(final ByteBuf output, final T value) {
            int id = toId.applyAsInt(value);
            VarInt.write(output, id);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(final IdMap<T> mapper) {
      return idMapper(mapper::byIdOrThrow, mapper::getIdOrThrow);
   }

   private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(
      final ResourceKey<? extends Registry<T>> registryKey, final Function<Registry<T>, IdMap<R>> mapExtractor
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, R>() {
         private IdMap<R> getRegistryOrThrow(final RegistryFriendlyByteBuf input) {
            return mapExtractor.apply(input.registryAccess().lookupOrThrow(registryKey));
         }

         public R decode(final RegistryFriendlyByteBuf input) {
            int id = VarInt.read(input);
            return (R)this.getRegistryOrThrow(input).byIdOrThrow(id);
         }

         public void encode(final RegistryFriendlyByteBuf output, final R value) {
            int id = this.getRegistryOrThrow(output).getIdOrThrow(value);
            VarInt.write(output, id);
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(final ResourceKey<? extends Registry<T>> registryKey) {
      return registry(registryKey, r -> r);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(final ResourceKey<? extends Registry<T>> registryKey) {
      return registry(registryKey, Registry::asHolderIdMap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(
      final ResourceKey<? extends Registry<T>> registryKey, final StreamCodec<? super RegistryFriendlyByteBuf, T> directCodec
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>() {
         private static final int DIRECT_HOLDER_ID = 0;

         private IdMap<Holder<T>> getRegistryOrThrow(final RegistryFriendlyByteBuf input) {
            return input.registryAccess().lookupOrThrow(registryKey).asHolderIdMap();
         }

         public Holder<T> decode(final RegistryFriendlyByteBuf input) {
            int id = VarInt.read(input);
            return id == 0 ? Holder.direct(directCodec.decode(input)) : (Holder)this.getRegistryOrThrow(input).byIdOrThrow(id - 1);
         }

         public void encode(final RegistryFriendlyByteBuf output, final Holder<T> holder) {
            switch (holder.kind()) {
               case REFERENCE:
                  int id = this.getRegistryOrThrow(output).getIdOrThrow(holder);
                  VarInt.write(output, id + 1);
                  break;
               case DIRECT:
                  VarInt.write(output, 0);
                  directCodec.encode(output, holder.value());
            }
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(final ResourceKey<? extends Registry<T>> registryKey) {
      return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>() {
         private static final int NAMED_SET = -1;
         private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = ByteBufCodecs.holderRegistry(registryKey);

         public HolderSet<T> decode(final RegistryFriendlyByteBuf input) {
            int count = VarInt.read(input) - 1;
            if (count == -1) {
               Registry<T> registry = input.registryAccess().lookupOrThrow(registryKey);
               return registry.get(TagKey.create(registryKey, Identifier.STREAM_CODEC.decode(input))).orElseThrow();
            }

            List<Holder<T>> holders = new ArrayList<>(Math.min(count, 65536));

            for (int i = 0; i < count; i++) {
               holders.add(this.holderCodec.decode(input));
            }

            return HolderSet.direct(holders);
         }

         public void encode(final RegistryFriendlyByteBuf output, final HolderSet<T> value) {
            Optional<TagKey<T>> key = value.unwrapKey();
            if (key.isPresent()) {
               VarInt.write(output, 0);
               Identifier.STREAM_CODEC.encode(output, key.get().location());
            } else {
               VarInt.write(output, value.size() + 1);

               for (Holder<T> holder : value) {
                  this.holderCodec.encode(output, holder);
               }
            }
         }
      };
   }

   static StreamCodec<ByteBuf, JsonElement> lenientJson(final int maxStringLength) {
      return new StreamCodec<ByteBuf, JsonElement>() {
         private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

         public JsonElement decode(final ByteBuf input) {
            String payload = Utf8String.read(input, maxStringLength);

            try {
               return LenientJsonParser.parse(payload);
            } catch (JsonSyntaxException e) {
               throw new DecoderException("Failed to parse JSON", e);
            }
         }

         public void encode(final ByteBuf output, final JsonElement value) {
            String payload = GSON.toJson(value);
            Utf8String.write(output, payload, maxStringLength);
         }
      };
   }
}
