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
      public Boolean decode(ByteBuf p_332480_) {
         return p_332480_.readBoolean();
      }

      public void encode(ByteBuf p_332710_, Boolean p_330535_) {
         p_332710_.writeBoolean(p_330535_);
      }
   };
   StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<ByteBuf, Byte>() {
      public Byte decode(ByteBuf p_332150_) {
         return p_332150_.readByte();
      }

      public void encode(ByteBuf p_328538_, Byte p_327835_) {
         p_328538_.writeByte(p_327835_);
      }
   };
   StreamCodec<ByteBuf, Float> ROTATION_BYTE = BYTE.map(Mth::unpackDegrees, Mth::packDegrees);
   StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<ByteBuf, Short>() {
      public Short decode(ByteBuf p_331682_) {
         return p_331682_.readShort();
      }

      public void encode(ByteBuf p_329734_, Short p_332862_) {
         p_329734_.writeShort(p_332862_);
      }
   };
   StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_333416_) {
         return p_333416_.readUnsignedShort();
      }

      public void encode(ByteBuf p_334768_, Integer p_335195_) {
         p_334768_.writeShort(p_335195_);
      }
   };
   StreamCodec<ByteBuf, Integer> INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_334363_) {
         return p_334363_.readInt();
      }

      public void encode(ByteBuf p_328174_, Integer p_329350_) {
         p_328174_.writeInt(p_329350_);
      }
   };
   StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_334861_) {
         return VarInt.read(p_334861_);
      }

      public void encode(ByteBuf p_333121_, Integer p_329976_) {
         VarInt.write(p_333121_, p_329976_);
      }
   };
   StreamCodec<ByteBuf, OptionalInt> OPTIONAL_VAR_INT = VAR_INT.map(
      p_358482_ -> p_358482_ == 0 ? OptionalInt.empty() : OptionalInt.of(p_358482_ - 1), p_358481_ -> p_358481_.isPresent() ? p_358481_.getAsInt() + 1 : 0
   );
   StreamCodec<ByteBuf, Long> LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf p_330259_) {
         return p_330259_.readLong();
      }

      public void encode(ByteBuf p_332625_, Long p_327681_) {
         p_332625_.writeLong(p_327681_);
      }
   };
   StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<ByteBuf, Long>() {
      public Long decode(ByteBuf p_335511_) {
         return VarLong.read(p_335511_);
      }

      public void encode(ByteBuf p_331177_, Long p_364567_) {
         VarLong.write(p_331177_, p_364567_);
      }
   };
   StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<ByteBuf, Float>() {
      public Float decode(ByteBuf p_330378_) {
         return p_330378_.readFloat();
      }

      public void encode(ByteBuf p_329698_, Float p_365105_) {
         p_329698_.writeFloat(p_365105_);
      }
   };
   StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<ByteBuf, Double>() {
      public Double decode(ByteBuf p_331124_) {
         return p_331124_.readDouble();
      }

      public void encode(ByteBuf p_327898_, Double p_363039_) {
         p_327898_.writeDouble(p_363039_);
      }
   };
   StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<ByteBuf, byte[]>() {
      public byte[] decode(ByteBuf p_333794_) {
         return FriendlyByteBuf.readByteArray(p_333794_);
      }

      public void encode(ByteBuf p_327981_, byte[] p_368580_) {
         FriendlyByteBuf.writeByteArray(p_327981_, p_368580_);
      }
   };
   StreamCodec<ByteBuf, long[]> LONG_ARRAY = new StreamCodec<ByteBuf, long[]>() {
      public long[] decode(ByteBuf p_329846_) {
         return FriendlyByteBuf.readLongArray(p_329846_);
      }

      public void encode(ByteBuf p_336297_, long[] p_397679_) {
         FriendlyByteBuf.writeLongArray(p_336297_, p_397679_);
      }
   };
   StreamCodec<ByteBuf, String> STRING_UTF8 = stringUtf8(32767);
   StreamCodec<ByteBuf, Tag> TAG = tagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, Tag> TRUSTED_TAG = tagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(NbtAccounter::defaultQuota);
   StreamCodec<ByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(NbtAccounter::unlimitedHeap);
   StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<ByteBuf, Optional<CompoundTag>>() {
      public Optional<CompoundTag> decode(ByteBuf p_331156_) {
         return Optional.ofNullable(FriendlyByteBuf.readNbt(p_331156_));
      }

      public void encode(ByteBuf p_328803_, Optional<CompoundTag> p_407819_) {
         FriendlyByteBuf.writeNbt(p_328803_, p_407819_.orElse(null));
      }
   };
   StreamCodec<ByteBuf, Vector3fc> VECTOR3F = new StreamCodec<ByteBuf, Vector3fc>() {
      public Vector3fc decode(ByteBuf p_329844_) {
         return FriendlyByteBuf.readVector3f(p_329844_);
      }

      public void encode(ByteBuf p_335209_, Vector3fc p_450377_) {
         FriendlyByteBuf.writeVector3f(p_335209_, p_450377_);
      }
   };
   StreamCodec<ByteBuf, Quaternionfc> QUATERNIONF = new StreamCodec<ByteBuf, Quaternionfc>() {
      public Quaternionfc decode(ByteBuf p_336330_) {
         return FriendlyByteBuf.readQuaternion(p_336330_);
      }

      public void encode(ByteBuf p_329166_, Quaternionfc p_451862_) {
         FriendlyByteBuf.writeQuaternion(p_329166_, p_451862_);
      }
   };
   StreamCodec<ByteBuf, Integer> CONTAINER_ID = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_328010_) {
         return FriendlyByteBuf.readContainerId(p_328010_);
      }

      public void encode(ByteBuf p_335266_, Integer p_408662_) {
         FriendlyByteBuf.writeContainerId(p_335266_, p_408662_);
      }
   };
   StreamCodec<ByteBuf, PropertyMap> GAME_PROFILE_PROPERTIES = new StreamCodec<ByteBuf, PropertyMap>() {
      public PropertyMap decode(ByteBuf p_406017_) {
         int i = ByteBufCodecs.readCount(p_406017_, 16);
         Builder<String, Property> builder = ImmutableMultimap.builder();

         for (int j = 0; j < i; j++) {
            String s = Utf8String.read(p_406017_, 64);
            String s1 = Utf8String.read(p_406017_, 32767);
            String s2 = FriendlyByteBuf.readNullable(p_406017_, p_410370_ -> Utf8String.read(p_410370_, 1024));
            Property property = new Property(s, s1, s2);
            builder.put(property.name(), property);
         }

         return new PropertyMap(builder.build());
      }

      public void encode(ByteBuf p_409260_, PropertyMap p_410667_) {
         ByteBufCodecs.writeCount(p_409260_, p_410667_.size(), 16);

         for (Property property : p_410667_.values()) {
            Utf8String.write(p_409260_, property.name(), 64);
            Utf8String.write(p_409260_, property.value(), 32767);
            FriendlyByteBuf.writeNullable(p_409260_, property.signature(), (p_410493_, p_408544_) -> Utf8String.write(p_410493_, p_408544_, 1024));
         }
      }
   };
   StreamCodec<ByteBuf, String> PLAYER_NAME = stringUtf8(16);
   StreamCodec<ByteBuf, GameProfile> GAME_PROFILE = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC, GameProfile::id, PLAYER_NAME, GameProfile::name, GAME_PROFILE_PROPERTIES, GameProfile::properties, GameProfile::new
   );
   StreamCodec<ByteBuf, Integer> RGB_COLOR = new StreamCodec<ByteBuf, Integer>() {
      public Integer decode(ByteBuf p_406122_) {
         return ARGB.color(p_406122_.readByte() & 0xFF, p_406122_.readByte() & 0xFF, p_406122_.readByte() & 0xFF);
      }

      public void encode(ByteBuf p_406948_, Integer p_424624_) {
         p_406948_.writeByte(ARGB.red(p_424624_));
         p_406948_.writeByte(ARGB.green(p_424624_));
         p_406948_.writeByte(ARGB.blue(p_424624_));
      }
   };

   static StreamCodec<ByteBuf, byte[]> byteArray(final int p_329369_) {
      return new StreamCodec<ByteBuf, byte[]>() {
         public byte[] decode(ByteBuf p_330658_) {
            return FriendlyByteBuf.readByteArray(p_330658_, p_329369_);
         }

         public void encode(ByteBuf p_332407_, byte[] p_327934_) {
            if (p_327934_.length > p_329369_) {
               throw new EncoderException("ByteArray with size " + p_327934_.length + " is bigger than allowed " + p_329369_);
            }

            FriendlyByteBuf.writeByteArray(p_332407_, p_327934_);
         }
      };
   }

   static StreamCodec<ByteBuf, String> stringUtf8(final int p_332577_) {
      return new StreamCodec<ByteBuf, String>() {
         public String decode(ByteBuf p_363937_) {
            return Utf8String.read(p_363937_, p_332577_);
         }

         public void encode(ByteBuf p_367629_, String p_392026_) {
            Utf8String.write(p_367629_, p_392026_, p_332577_);
         }
      };
   }

   static StreamCodec<ByteBuf, Optional<Tag>> optionalTagCodec(final Supplier<NbtAccounter> p_409696_) {
      return new StreamCodec<ByteBuf, Optional<Tag>>() {
         public Optional<Tag> decode(ByteBuf p_397230_) {
            return Optional.ofNullable(FriendlyByteBuf.readNbt(p_397230_, p_409696_.get()));
         }

         public void encode(ByteBuf p_397825_, Optional<Tag> p_409212_) {
            FriendlyByteBuf.writeNbt(p_397825_, p_409212_.orElse(null));
         }
      };
   }

   static StreamCodec<ByteBuf, Tag> tagCodec(final Supplier<NbtAccounter> p_334674_) {
      return new StreamCodec<ByteBuf, Tag>() {
         public Tag decode(ByteBuf p_410203_) {
            Tag tag = FriendlyByteBuf.readNbt(p_410203_, p_334674_.get());
            if (tag == null) {
               throw new DecoderException("Expected non-null compound tag");
            } else {
               return tag;
            }
         }

         public void encode(ByteBuf p_409848_, Tag p_407203_) {
            if (p_407203_ == EndTag.INSTANCE) {
               throw new EncoderException("Expected non-null compound tag");
            }

            FriendlyByteBuf.writeNbt(p_409848_, p_407203_);
         }
      };
   }

   static StreamCodec<ByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> p_334293_) {
      return tagCodec(p_334293_).map(p_329005_ -> {
         if (p_329005_ instanceof CompoundTag compoundtag) {
            return compoundtag;
         } else {
            throw new DecoderException("Not a compound tag: " + p_329005_);
         }
      }, p_331817_ -> (Tag)p_331817_);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodecTrusted(Codec<T> p_332454_) {
      return fromCodec(p_332454_, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> p_330766_) {
      return fromCodec(p_330766_, NbtAccounter::defaultQuota);
   }

   static <T, B extends ByteBuf, V> StreamCodec.CodecOperation<B, T, V> fromCodec(DynamicOps<T> p_407470_, Codec<V> p_406519_) {
      return p_405104_ -> new StreamCodec<B, V>() {
         public V decode(B p_328716_) {
            T t = (T)p_405104_.decode(p_328716_);
            return (V)p_406519_.parse(p_407470_, t).getOrThrow(p_409074_ -> new DecoderException("Failed to decode: " + p_409074_ + " " + t));
         }

         public void encode(B p_327986_, V p_405995_) {
            T t = (T)p_406519_.encodeStart(p_407470_, p_405995_)
               .getOrThrow(p_409057_ -> new EncoderException("Failed to encode: " + p_409057_ + " " + p_405995_));
            p_405104_.encode(p_327986_, t);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> p_332152_, Supplier<NbtAccounter> p_333221_) {
      return tagCodec(p_333221_).apply(fromCodec(NbtOps.INSTANCE, p_332152_));
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistriesTrusted(Codec<T> p_331690_) {
      return fromCodecWithRegistries(p_331690_, NbtAccounter::unlimitedHeap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> p_334037_) {
      return fromCodecWithRegistries(p_334037_, NbtAccounter::defaultQuota);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(final Codec<T> p_332747_, Supplier<NbtAccounter> p_329046_) {
      final StreamCodec<ByteBuf, Tag> streamcodec = tagCodec(p_329046_);
      return new StreamCodec<RegistryFriendlyByteBuf, T>() {
         public T decode(RegistryFriendlyByteBuf p_406763_) {
            Tag tag = streamcodec.decode(p_406763_);
            RegistryOps<Tag> registryops = p_406763_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return (T)p_332747_.parse(registryops, tag).getOrThrow(p_406463_ -> new DecoderException("Failed to decode: " + p_406463_ + " " + tag));
         }

         public void encode(RegistryFriendlyByteBuf p_405963_, T p_409936_) {
            RegistryOps<Tag> registryops = p_405963_.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag tag = (Tag)p_332747_.encodeStart(registryops, p_409936_)
               .getOrThrow(p_409718_ -> new EncoderException("Failed to encode: " + p_409718_ + " " + p_409936_));
            streamcodec.encode(p_405963_, tag);
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<? super B, V> p_333614_) {
      return new StreamCodec<B, Optional<V>>() {
         public Optional<V> decode(B p_365796_) {
            return p_365796_.readBoolean() ? Optional.of(p_333614_.decode(p_365796_)) : Optional.empty();
         }

         public void encode(B p_362090_, Optional<V> p_408618_) {
            if (p_408618_.isPresent()) {
               p_362090_.writeBoolean(true);
               p_333614_.encode(p_362090_, p_408618_.get());
            } else {
               p_362090_.writeBoolean(false);
            }
         }
      };
   }

   static int readCount(ByteBuf p_335948_, int p_329745_) {
      int i = VarInt.read(p_335948_);
      if (i > p_329745_) {
         throw new DecoderException(i + " elements exceeded max size of: " + p_329745_);
      } else {
         return i;
      }
   }

   static void writeCount(ByteBuf p_332743_, int p_332779_, int p_330804_) {
      if (p_332779_ > p_330804_) {
         throw new EncoderException(p_332779_ + " elements exceeded max size of: " + p_330804_);
      }

      VarInt.write(p_332743_, p_332779_);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> p_329603_, StreamCodec<? super B, V> p_335274_) {
      return collection(p_329603_, p_335274_, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(
      final IntFunction<C> p_330282_, final StreamCodec<? super B, V> p_329504_, final int p_331395_
   ) {
      return new StreamCodec<B, C>() {
         public C decode(B p_368316_) {
            int i = ByteBufCodecs.readCount(p_368316_, p_331395_);
            C c = p_330282_.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               c.add(p_329504_.decode(p_368316_));
            }

            return c;
         }

         public void encode(B p_361972_, C p_406140_) {
            ByteBufCodecs.writeCount(p_361972_, p_406140_.size(), p_331395_);

            for (V v : p_406140_) {
               p_329504_.encode(p_361972_, v);
            }
         }
      };
   }

   static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> p_333333_) {
      return p_331526_ -> collection(p_333333_, p_331526_);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
      return p_331787_ -> collection(ArrayList::new, p_331787_);
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int p_331728_) {
      return p_328420_ -> collection(ArrayList::new, p_328420_, p_331728_);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      IntFunction<? extends M> p_329613_, StreamCodec<? super B, K> p_335749_, StreamCodec<? super B, V> p_332695_
   ) {
      return map(p_329613_, p_335749_, p_332695_, Integer.MAX_VALUE);
   }

   static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(
      final IntFunction<? extends M> p_331225_, final StreamCodec<? super B, K> p_334555_, final StreamCodec<? super B, V> p_330391_, final int p_331122_
   ) {
      return new StreamCodec<B, M>() {
         public void encode(B p_391889_, M p_407795_) {
            ByteBufCodecs.writeCount(p_391889_, p_407795_.size(), p_331122_);
            p_407795_.forEach((p_407001_, p_410513_) -> {
               p_334555_.encode(p_391889_, (K)p_407001_);
               p_330391_.encode(p_391889_, (V)p_410513_);
            });
         }

         public M decode(B p_391476_) {
            int i = ByteBufCodecs.readCount(p_391476_, p_331122_);
            M m = (M)p_331225_.apply(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               K k = p_334555_.decode(p_391476_);
               V v = p_330391_.decode(p_391476_);
               m.put(k, v);
            }

            return m;
         }
      };
   }

   static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> p_330563_, final StreamCodec<? super B, R> p_328664_) {
      return new StreamCodec<B, Either<L, R>>() {
         public Either<L, R> decode(B p_393204_) {
            return p_393204_.readBoolean() ? Either.left(p_330563_.decode(p_393204_)) : Either.right(p_328664_.decode(p_393204_));
         }

         public void encode(B p_391372_, Either<L, R> p_407718_) {
            p_407718_.ifLeft(p_409560_ -> {
               p_391372_.writeBoolean(true);
               p_330563_.encode(p_391372_, (L)p_409560_);
            }).ifRight(p_409506_ -> {
               p_391372_.writeBoolean(false);
               p_328664_.encode(p_391372_, (R)p_409506_);
            });
         }
      };
   }

   static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, V> lengthPrefixed(int p_393461_, BiFunction<B, ByteBuf, B> p_397581_) {
      return p_405107_ -> new StreamCodec<B, V>() {
         public V decode(B p_409854_) {
            int i = VarInt.read(p_409854_);
            if (i > p_393461_) {
               throw new DecoderException("Buffer size " + i + " is larger than allowed limit of " + p_393461_);
            }

            int j = p_409854_.readerIndex();
            B b = (B)((ByteBuf)p_397581_.apply(p_409854_, p_409854_.slice(j, i)));
            p_409854_.readerIndex(j + i);
            return (V)p_405107_.decode(b);
         }

         public void encode(B p_408406_, V p_391276_) {
            B b = (B)((ByteBuf)p_397581_.apply(p_408406_, p_408406_.alloc().buffer()));

            try {
               p_405107_.encode(b, p_391276_);
               int i = b.readableBytes();
               if (i > p_393461_) {
                  throw new EncoderException("Buffer size " + i + " is  larger than allowed limit of " + p_393461_);
               }

               VarInt.write(p_408406_, i);
               p_408406_.writeBytes(b);
            } finally {
               b.release();
            }
         }
      };
   }

   static <V> StreamCodec.CodecOperation<ByteBuf, V, V> lengthPrefixed(int p_396380_) {
      return lengthPrefixed(p_396380_, (p_408962_, p_406825_) -> p_406825_);
   }

   static <V> StreamCodec.CodecOperation<RegistryFriendlyByteBuf, V, V> registryFriendlyLengthPrefixed(int p_406506_) {
      return lengthPrefixed(p_406506_, (p_389924_, p_389925_) -> new RegistryFriendlyByteBuf(p_389925_, p_389924_.registryAccess()));
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> p_333433_, final ToIntFunction<T> p_334959_) {
      return new StreamCodec<ByteBuf, T>() {
         public T decode(ByteBuf p_409027_) {
            int i = VarInt.read(p_409027_);
            return p_333433_.apply(i);
         }

         public void encode(ByteBuf p_405926_, T p_408387_) {
            int i = p_334959_.applyAsInt(p_408387_);
            VarInt.write(p_405926_, i);
         }
      };
   }

   static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> p_332036_) {
      return idMapper(p_332036_::byIdOrThrow, p_332036_::getIdOrThrow);
   }

   private static <T, R> StreamCodec<RegistryFriendlyByteBuf, R> registry(
      final ResourceKey<? extends Registry<T>> p_332046_, final Function<Registry<T>, IdMap<R>> p_332827_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, R>() {
         private IdMap<R> getRegistryOrThrow(RegistryFriendlyByteBuf p_408099_) {
            return p_332827_.apply(p_408099_.registryAccess().lookupOrThrow(p_332046_));
         }

         public R decode(RegistryFriendlyByteBuf p_391777_) {
            int i = VarInt.read(p_391777_);
            return (R)this.getRegistryOrThrow(p_391777_).byIdOrThrow(i);
         }

         public void encode(RegistryFriendlyByteBuf p_393392_, R p_408249_) {
            int i = this.getRegistryOrThrow(p_393392_).getIdOrThrow(p_408249_);
            VarInt.write(p_393392_, i);
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> p_332712_) {
      return registry(p_332712_, p_405101_ -> p_405101_);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> p_332639_) {
      return registry(p_332639_, Registry::asHolderIdMap);
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holder(
      final ResourceKey<? extends Registry<T>> p_335347_, final StreamCodec<? super RegistryFriendlyByteBuf, T> p_329304_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, Holder<T>>() {
         private static final int DIRECT_HOLDER_ID = 0;

         private IdMap<Holder<T>> getRegistryOrThrow(RegistryFriendlyByteBuf p_406546_) {
            return p_406546_.registryAccess().lookupOrThrow(p_335347_).asHolderIdMap();
         }

         public Holder<T> decode(RegistryFriendlyByteBuf p_410130_) {
            int i = VarInt.read(p_410130_);
            return i == 0 ? Holder.direct(p_329304_.decode(p_410130_)) : (Holder)this.getRegistryOrThrow(p_410130_).byIdOrThrow(i - 1);
         }

         public void encode(RegistryFriendlyByteBuf p_407566_, Holder<T> p_406766_) {
            switch (p_406766_.kind()) {
               case REFERENCE:
                  int i = this.getRegistryOrThrow(p_407566_).getIdOrThrow(p_406766_);
                  VarInt.write(p_407566_, i + 1);
                  break;
               case DIRECT:
                  VarInt.write(p_407566_, 0);
                  p_329304_.encode(p_407566_, p_406766_.value());
            }
         }
      };
   }

   static <T> StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>> holderSet(final ResourceKey<? extends Registry<T>> p_328506_) {
      return new StreamCodec<RegistryFriendlyByteBuf, HolderSet<T>>() {
         private static final int NAMED_SET = -1;
         private final StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderCodec = ByteBufCodecs.holderRegistry(p_328506_);

         public HolderSet<T> decode(RegistryFriendlyByteBuf p_406717_) {
            int i = VarInt.read(p_406717_) - 1;
            if (i == -1) {
               Registry<T> registry = p_406717_.registryAccess().lookupOrThrow(p_328506_);
               return registry.get(TagKey.create(p_328506_, Identifier.STREAM_CODEC.decode(p_406717_))).orElseThrow();
            }

            List<Holder<T>> list = new ArrayList<>(Math.min(i, 65536));

            for (int j = 0; j < i; j++) {
               list.add(this.holderCodec.decode(p_406717_));
            }

            return HolderSet.direct(list);
         }

         public void encode(RegistryFriendlyByteBuf p_409001_, HolderSet<T> p_409541_) {
            Optional<TagKey<T>> optional = p_409541_.unwrapKey();
            if (optional.isPresent()) {
               VarInt.write(p_409001_, 0);
               Identifier.STREAM_CODEC.encode(p_409001_, optional.get().location());
            } else {
               VarInt.write(p_409001_, p_409541_.size() + 1);

               for (Holder<T> holder : p_409541_) {
                  this.holderCodec.encode(p_409001_, holder);
               }
            }
         }
      };
   }

   static StreamCodec<ByteBuf, JsonElement> lenientJson(final int p_409586_) {
      return new StreamCodec<ByteBuf, JsonElement>() {
         private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

         public JsonElement decode(ByteBuf p_407243_) {
            String s = Utf8String.read(p_407243_, p_409586_);

            try {
               return LenientJsonParser.parse(s);
            } catch (JsonSyntaxException jsonsyntaxexception) {
               throw new DecoderException("Failed to parse JSON", jsonsyntaxexception);
            }
         }

         public void encode(ByteBuf p_407727_, JsonElement p_428661_) {
            String s = GSON.toJson(p_428661_);
            Utf8String.write(p_407727_, s, p_409586_);
         }
      };
   }
}
