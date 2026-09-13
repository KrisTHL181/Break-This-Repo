package net.minecraft.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public class ExtraCodecs {
   public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
   public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
   public static final Codec<Tag> NBT = converter(NbtOps.INSTANCE);
   public static final Codec<Vector2fc> VECTOR2F = Codec.FLOAT
      .listOf()
      .comapFlatMap(input -> Util.fixedSize(input, 2).map(d -> new Vector2f((Float)d.get(0), (Float)d.get(1))), vec -> List.of(vec.x(), vec.y()));
   public static final Codec<Vector3fc> VECTOR3F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 3).map(d -> new Vector3f((Float)d.get(0), (Float)d.get(1), (Float)d.get(2))), vec -> List.of(vec.x(), vec.y(), vec.z())
      );
   public static final Codec<Vector3ic> VECTOR3I = Codec.INT
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 3).map(d -> new Vector3i((Integer)d.get(0), (Integer)d.get(1), (Integer)d.get(2))),
         vec -> List.of(vec.x(), vec.y(), vec.z())
      );
   public static final Codec<Vector4fc> VECTOR4F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 4).map(d -> new Vector4f((Float)d.get(0), (Float)d.get(1), (Float)d.get(2), (Float)d.get(3))),
         vec -> List.of(vec.x(), vec.y(), vec.z(), vec.w())
      );
   public static final Codec<Quaternionfc> QUATERNIONF_COMPONENTS = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 4).map(d -> new Quaternionf((Float)d.get(0), (Float)d.get(1), (Float)d.get(2), (Float)d.get(3)).normalize()),
         q -> List.of(q.x(), q.y(), q.z(), q.w())
      );
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
      i -> i.group(Codec.FLOAT.fieldOf("angle").forGetter(o -> o.angle), VECTOR3F.fieldOf("axis").forGetter(o -> new Vector3f(o.x, o.y, o.z)))
         .apply(i, AxisAngle4f::new)
   );
   public static final Codec<Quaternionfc> QUATERNIONF = Codec.withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
   public static final Codec<Matrix4fc> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(input -> Util.fixedSize(input, 16).map(l -> {
      Matrix4f result = new Matrix4f();

      for (int i = 0; i < l.size(); i++) {
         result.setRowColumn(i >> 2, i & 3, (Float)l.get(i));
      }

      return result.determineProperties();
   }), m -> {
      FloatList output = new FloatArrayList(16);

      for (int i = 0; i < 16; i++) {
         output.add(m.getRowColumn(i >> 2, i & 3));
      }

      return output;
   });
   private static final String HEX_COLOR_PREFIX = "#";
   public static final Codec<Integer> RGB_COLOR_CODEC = Codec.withAlternative(Codec.INT, VECTOR3F, v -> ARGB.colorFromFloat(1.0F, v.x(), v.y(), v.z()));
   public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.withAlternative(Codec.INT, VECTOR4F, v -> ARGB.colorFromFloat(v.w(), v.x(), v.y(), v.z()));
   public static final Codec<Integer> STRING_RGB_COLOR = Codec.withAlternative(hexColor(6).xmap(ARGB::opaque, ARGB::transparent), RGB_COLOR_CODEC);
   public static final Codec<Integer> STRING_ARGB_COLOR = Codec.withAlternative(hexColor(8), ARGB_COLOR_CODEC);
   public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
      .flatComapMap(
         UnsignedBytes::toInt,
         integer -> integer > 255 ? DataResult.error(() -> "Unsigned byte was too large: " + integer + " > 255") : DataResult.success(integer.byteValue())
      );
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, n -> "Value must be non-negative: " + n);
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, n -> "Value must be positive: " + n);
   public static final Codec<Long> NON_NEGATIVE_LONG = longRangeWithMessage(0L, Long.MAX_VALUE, n -> "Value must be non-negative: " + n);
   public static final Codec<Long> POSITIVE_LONG = longRangeWithMessage(1L, Long.MAX_VALUE, n -> "Value must be positive: " + n);
   public static final Codec<Float> NON_NEGATIVE_FLOAT = floatRangeMinInclusiveWithMessage(0.0F, Float.MAX_VALUE, n -> "Value must be non-negative: " + n);
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, n -> "Value must be positive: " + n);
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(pattern -> {
      try {
         return DataResult.success(Pattern.compile(pattern));
      } catch (PatternSyntaxException e) {
         return DataResult.error(() -> "Invalid regex pattern '" + pattern + "': " + e.getMessage());
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(string));
      } catch (IllegalArgumentException e) {
         return DataResult.error(() -> "Malformed base64 string");
      }
   }, bytes -> Base64.getEncoder().encodeToString(bytes));
   public static final Codec<String> ESCAPED_STRING = Codec.STRING
      .comapFlatMap(str -> DataResult.success(StringEscapeUtils.unescapeJava(str)), StringEscapeUtils::escapeJava);
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
      .comapFlatMap(
         name -> name.startsWith("#")
            ? Identifier.read(name.substring(1)).map(id -> new ExtraCodecs.TagOrElementLocation(id, true))
            : Identifier.read(name).map(id -> new ExtraCodecs.TagOrElementLocation(id, false)),
         ExtraCodecs.TagOrElementLocation::decoratedId
      );
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = o -> o.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = l -> l.isPresent() ? Optional.of(l.getAsLong()) : Optional.empty();
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
      .xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> Arrays.stream(bitSet.toLongArray()));
   public static final int MAX_PROPERTY_NAME_LENGTH = 64;
   public static final int MAX_PROPERTY_VALUE_LENGTH = 32767;
   public static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
   public static final int MAX_PROPERTIES = 16;
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
      i -> i.group(
            Codec.sizeLimitedString(64).fieldOf("name").forGetter(Property::name),
            Codec.sizeLimitedString(32767).fieldOf("value").forGetter(Property::value),
            Codec.sizeLimitedString(1024).optionalFieldOf("signature").forGetter(property -> Optional.ofNullable(property.signature()))
         )
         .apply(i, (name, value, signature) -> new Property(name, value, (String)signature.orElse(null)))
   );
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(
         Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
            .validate(map -> map.size() > 16 ? DataResult.error(() -> "Cannot have more than 16 properties, but was " + map.size()) : DataResult.success(map)),
         PROPERTY.sizeLimitedListOf(16)
      )
      .xmap(mapListEither -> {
         Builder<String, Property> result = ImmutableMultimap.builder();
         mapListEither.ifLeft(s -> s.forEach((name, properties) -> {
            for (String property : properties) {
               result.put(name, new Property(name, property));
            }
         })).ifRight(properties -> {
            for (Property property : properties) {
               result.put(property.name(), property);
            }
         });
         return new PropertyMap(result.build());
      }, propertyMap -> Either.right(propertyMap.values().stream().toList()));
   public static final Codec<String> PLAYER_NAME = Codec.string(0, 16)
      .validate(
         name -> StringUtil.isValidPlayerName(name)
            ? DataResult.success(name)
            : DataResult.error(() -> "Player name contained disallowed characters: '" + name + "'")
      );
   public static final Codec<GameProfile> AUTHLIB_GAME_PROFILE = gameProfileCodec(UUIDUtil.AUTHLIB_CODEC).codec();
   public static final MapCodec<GameProfile> STORED_GAME_PROFILE = gameProfileCodec(UUIDUtil.CODEC);
   public static final Codec<String> NON_EMPTY_STRING = Codec.STRING
      .validate(value -> value.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(value));
   public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(s -> {
      int[] codepoint = s.codePoints().toArray();
      return codepoint.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + s) : DataResult.success(codepoint[0]);
   }, Character::toString);
   public static final Codec<String> RESOURCE_PATH_CODEC = Codec.STRING
      .validate(s -> !Identifier.isValidPath(s) ? DataResult.error(() -> "Invalid string to use as a resource path element: " + s) : DataResult.success(s));
   public static final Codec<URI> UNTRUSTED_URI = Codec.STRING.comapFlatMap(string -> {
      try {
         return DataResult.success(Util.parseAndValidateUntrustedUri(string));
      } catch (URISyntaxException e) {
         return DataResult.error(e::getMessage);
      }
   }, URI::toString);
   public static final Codec<String> CHAT_STRING = Codec.STRING.validate(string -> {
      for (int i = 0; i < string.length(); i++) {
         char c = string.charAt(i);
         if (!StringUtil.isAllowedChatCharacter(c)) {
            return DataResult.error(() -> "Disallowed chat character: '" + c + "'");
         }
      }

      return DataResult.success(string);
   });

   public static <T> Codec<T> converter(final DynamicOps<T> ops) {
      return Codec.PASSTHROUGH.xmap(t -> t.convert(ops).getValue(), t -> new Dynamic(ops, t));
   }

   private static Codec<Integer> hexColor(final int expectedDigits) {
      long maxValue = (1L << expectedDigits * 4) - 1L;
      return Codec.STRING.comapFlatMap(string -> {
         if (!string.startsWith("#")) {
            return DataResult.error(() -> "Hex color must begin with #");
         }

         int digits = string.length() - "#".length();
         if (digits != expectedDigits) {
            return DataResult.error(() -> "Hex color is wrong size, expected " + expectedDigits + " digits but got " + digits);
         }

         try {
            long value = HexFormat.fromHexDigitsToLong(string, "#".length(), string.length());
            return value >= 0L && value <= maxValue ? DataResult.success((int)value) : DataResult.error(() -> "Color value out of range: " + string);
         } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color value: " + string);
         }
      }, value -> "#" + HexFormat.of().toHexDigits(value.intValue(), expectedDigits));
   }

   public static <P, I> Codec<I> intervalCodec(
      final Codec<P> pointCodec,
      final String lowerBoundName,
      final String upperBoundName,
      final BiFunction<P, P, DataResult<I>> makeInterval,
      final Function<I, P> getMin,
      final Function<I, P> getMax
   ) {
      Codec<I> arrayCodec = Codec.list(pointCodec).comapFlatMap(list -> Util.fixedSize(list, 2).flatMap(l -> {
         P min = (P)l.get(0);
         P max = (P)l.get(1);
         return makeInterval.apply(min, max);
      }), p -> ImmutableList.of(getMin.apply((I)p), getMax.apply((I)p)));
      Codec<I> objectCodec = RecordCodecBuilder.create(
            i -> i.group(pointCodec.fieldOf(lowerBoundName).forGetter(Pair::getFirst), pointCodec.fieldOf(upperBoundName).forGetter(Pair::getSecond))
               .apply(i, Pair::of)
         )
         .comapFlatMap(p -> makeInterval.apply((P)p.getFirst(), (P)p.getSecond()), i -> Pair.of(getMin.apply((I)i), getMax.apply((I)i)));
      Codec<I> arrayOrObjectCodec = Codec.withAlternative(arrayCodec, objectCodec);
      return Codec.either(pointCodec, arrayOrObjectCodec)
         .comapFlatMap(either -> (DataResult)either.map(min -> makeInterval.apply((P)min, (P)min), DataResult::success), p -> {
            P min = getMin.apply((I)p);
            P max = getMax.apply((I)p);
            return Objects.equals(min, max) ? Either.left(min) : Either.right(p);
         });
   }

   public static <A> ResultFunction<A> orElsePartial(final A value) {
      return new ResultFunction<A>() {
         public <T> DataResult<Pair<A, T>> apply(final DynamicOps<T> ops, final T input, final DataResult<Pair<A, T>> a) {
            MutableObject<String> message = new MutableObject();
            Optional<Pair<A, T>> result = a.resultOrPartial(message::setValue);
            return result.isPresent() ? a : DataResult.error(() -> "(" + (String)message.get() + " -> using default)", Pair.of(value, input));
         }

         public <T> DataResult<T> coApply(final DynamicOps<T> ops, final A input, final DataResult<T> t) {
            return t;
         }

         @Override
         public String toString() {
            return "OrElsePartial[" + value + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(final ToIntFunction<E> toInt, final IntFunction<@Nullable E> fromInt, final int unknownId) {
      return Codec.INT
         .flatXmap(
            id -> Optional.ofNullable(fromInt.apply(id))
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + id)),
            e -> {
               int id = toInt.applyAsInt((E)e);
               return id == unknownId ? DataResult.error(() -> "Element with unknown id: " + e) : DataResult.success(id);
            }
         );
   }

   public static <I, E> Codec<E> idResolverCodec(final Codec<I> value, final Function<I, @Nullable E> fromId, final Function<E, @Nullable I> toId) {
      return value.flatXmap(id -> {
         E element = fromId.apply((I)id);
         return element == null ? DataResult.error(() -> "Unknown element id: " + id) : DataResult.success(element);
      }, e -> {
         I id = toId.apply((E)e);
         return id == null ? DataResult.error(() -> "Element with unknown id: " + e) : DataResult.success(id);
      });
   }

   public static <E> Codec<E> orCompressed(final Codec<E> normal, final Codec<E> compressed) {
      return new Codec<E>() {
         public <T> DataResult<T> encode(final E input, final DynamicOps<T> ops, final T prefix) {
            return ops.compressMaps() ? compressed.encode(input, ops, prefix) : normal.encode(input, ops, prefix);
         }

         public <T> DataResult<Pair<E, T>> decode(final DynamicOps<T> ops, final T input) {
            return ops.compressMaps() ? compressed.decode(ops, input) : normal.decode(ops, input);
         }

         @Override
         public String toString() {
            return normal + " orCompressed " + compressed;
         }
      };
   }

   public static <E> MapCodec<E> orCompressed(final MapCodec<E> normal, final MapCodec<E> compressed) {
      return new MapCodec<E>() {
         public <T> RecordBuilder<T> encode(final E input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
            return ops.compressMaps() ? compressed.encode(input, ops, prefix) : normal.encode(input, ops, prefix);
         }

         public <T> DataResult<E> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            return ops.compressMaps() ? compressed.decode(ops, input) : normal.decode(ops, input);
         }

         public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return compressed.keys(ops);
         }

         public String toString() {
            return normal + " orCompressed " + compressed;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(
      final Codec<E> codec, final Function<E, Lifecycle> decodeLifecycle, final Function<E, Lifecycle> encodeLifecycle
   ) {
      return codec.mapResult(new ResultFunction<E>() {
         public <T> DataResult<Pair<E, T>> apply(final DynamicOps<T> ops, final T input, final DataResult<Pair<E, T>> a) {
            return a.result().map(r -> a.setLifecycle(decodeLifecycle.apply((E)r.getFirst()))).orElse(a);
         }

         public <T> DataResult<T> coApply(final DynamicOps<T> ops, final E input, final DataResult<T> t) {
            return t.setLifecycle(encodeLifecycle.apply(input));
         }

         @Override
         public String toString() {
            return "WithLifecycle[" + decodeLifecycle + " " + encodeLifecycle + "]";
         }
      });
   }

   public static <E> Codec<E> overrideLifecycle(final Codec<E> codec, final Function<E, Lifecycle> lifecycleGetter) {
      return overrideLifecycle(codec, lifecycleGetter, lifecycleGetter);
   }

   public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(final Codec<K> keyCodec, final Codec<V> elementCodec) {
      return new ExtraCodecs.StrictUnboundedMapCodec<>(keyCodec, elementCodec);
   }

   public static <E> Codec<List<E>> compactListCodec(final Codec<E> elementCodec) {
      return compactListCodec(elementCodec, elementCodec.listOf());
   }

   public static <E> Codec<List<E>> compactListCodec(final Codec<E> elementCodec, final Codec<List<E>> listCodec) {
      return Codec.either(listCodec, elementCodec).xmap(e -> (List)e.map(l -> l, List::of), v -> v.size() == 1 ? Either.right(v.getFirst()) : Either.left(v));
   }

   private static Codec<Integer> intRangeWithMessage(final int minInclusive, final int maxInclusive, final Function<Integer, String> error) {
      return Codec.INT
         .validate(
            value -> value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0
               ? DataResult.success(value)
               : DataResult.error(() -> error.apply(value))
         );
   }

   public static Codec<Integer> intRange(final int minInclusive, final int maxInclusive) {
      return intRangeWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
   }

   private static Codec<Long> longRangeWithMessage(final long minInclusive, final long maxInclusive, final Function<Long, String> error) {
      return Codec.LONG
         .validate(
            value -> value.compareTo(minInclusive) >= 0L && value.compareTo(maxInclusive) <= 0L
               ? DataResult.success(value)
               : DataResult.error(() -> error.apply(value))
         );
   }

   public static Codec<Long> longRange(final int minInclusive, final int maxInclusive) {
      return longRangeWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
   }

   private static Codec<Float> floatRangeMinInclusiveWithMessage(final float minInclusive, final float maxInclusive, final Function<Float, String> error) {
      return Codec.FLOAT
         .validate(
            value -> value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0
               ? DataResult.success(value)
               : DataResult.error(() -> error.apply(value))
         );
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(final float minExclusive, final float maxInclusive, final Function<Float, String> error) {
      return Codec.FLOAT
         .validate(
            value -> value.compareTo(minExclusive) > 0 && value.compareTo(maxInclusive) <= 0
               ? DataResult.success(value)
               : DataResult.error(() -> error.apply(value))
         );
   }

   public static Codec<Float> floatRange(final float minInclusive, final float maxInclusive) {
      return floatRangeMinInclusiveWithMessage(minInclusive, maxInclusive, n -> "Value must be within range [" + minInclusive + ";" + maxInclusive + "]: " + n);
   }

   public static <T> Codec<List<T>> nonEmptyList(final Codec<List<T>> listCodec) {
      return listCodec.validate(list -> list.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(list));
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(final Codec<HolderSet<T>> listCodec) {
      return listCodec.validate(
         list -> list.unwrap().right().filter(List::isEmpty).isPresent() ? DataResult.error(() -> "List must have contents") : DataResult.success(list)
      );
   }

   public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(final Codec<M> mapCodec) {
      return mapCodec.validate(map -> map.isEmpty() ? DataResult.error(() -> "Map must have contents") : DataResult.success(map));
   }

   public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> getter) {
      class ContextRetrievalCodec extends MapCodec<E> {
         public <T> RecordBuilder<T> encode(final E input, final DynamicOps<T> ops, final RecordBuilder<T> prefix) {
            return prefix;
         }

         public <T> DataResult<E> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            return getter.apply(ops);
         }

         public String toString() {
            return "ContextRetrievalCodec[" + getter + "]";
         }

         public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(final Function<E, T> typeGetter) {
      return container -> {
         Iterator<E> it = container.iterator();
         if (it.hasNext()) {
            T firstType = typeGetter.apply(it.next());

            while (it.hasNext()) {
               E next = it.next();
               T nextType = typeGetter.apply(next);
               if (nextType != firstType) {
                  return DataResult.error(() -> "Mixed type list: element " + next + " had type " + nextType + ", but list is of type " + firstType);
               }
            }
         }

         return DataResult.success(container, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(final Codec<A> codec) {
      return Codec.of(codec, new Decoder<A>() {
         public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
            try {
               return codec.decode(ops, input);
            } catch (Exception e) {
               return DataResult.error(() -> "Caught exception decoding " + input + ": " + e.getMessage());
            }
         }
      });
   }

   public static Codec<TemporalAccessor> temporalCodec(final DateTimeFormatter formatter) {
      return Codec.STRING.comapFlatMap(s -> {
         try {
            return DataResult.success(formatter.parse(s));
         } catch (Exception e) {
            return DataResult.error(e::getMessage);
         }
      }, formatter::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(final MapCodec<Optional<Long>> fieldCodec) {
      return fieldCodec.xmap(toOptionalLong, fromOptionalLong);
   }

   private static MapCodec<GameProfile> gameProfileCodec(final Codec<UUID> uuidCodec) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(
               uuidCodec.fieldOf("id").forGetter(GameProfile::id),
               PLAYER_NAME.fieldOf("name").forGetter(GameProfile::name),
               PROPERTY_MAP.optionalFieldOf("properties", PropertyMap.EMPTY).forGetter(GameProfile::properties)
            )
            .apply(i, GameProfile::new)
      );
   }

   public static <K, V> Codec<Map<K, V>> sizeLimitedMap(final Codec<Map<K, V>> codec, final int maxSizeInclusive) {
      return codec.validate(
         map -> map.size() > maxSizeInclusive
            ? DataResult.error(() -> "Map is too long: " + map.size() + ", expected range [0-" + maxSizeInclusive + "]")
            : DataResult.success(map)
      );
   }

   public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(final Codec<T> keyCodec) {
      return Codec.unboundedMap(keyCodec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
   }

   @Deprecated
   public static <K, V> MapCodec<V> dispatchOptionalValue(
      final String typeKey,
      final String valueKey,
      final Codec<K> typeCodec,
      final Function<? super V, ? extends K> typeGetter,
      final Function<? super K, ? extends Codec<? extends V>> valueCodec
   ) {
      return new MapCodec<V>() {
         public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return Stream.of((T[])(new Object[]{ops.createString(typeKey), ops.createString(valueKey)}));
         }

         public <T> DataResult<V> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            T typeName = (T)input.get(typeKey);
            return typeName == null ? DataResult.error(() -> "Missing \"" + typeKey + "\" in: " + input) : typeCodec.decode(ops, typeName).flatMap(type -> {
               T value = Objects.requireNonNullElseGet((T)input.get(valueKey), ops::emptyMap);
               return valueCodec.apply((K)type.getFirst()).decode(ops, value).map(Pair::getFirst);
            });
         }

         public <T> RecordBuilder<T> encode(final V input, final DynamicOps<T> ops, final RecordBuilder<T> builder) {
            K type = (K)typeGetter.apply(input);
            builder.add(typeKey, typeCodec.encodeStart(ops, type));
            DataResult<T> parameters = this.encode(valueCodec.apply(type), input, ops);
            if (parameters.result().isEmpty() || !Objects.equals(parameters.result().get(), ops.emptyMap())) {
               builder.add(valueKey, parameters);
            }

            return builder;
         }

         private <T, V2 extends V> DataResult<T> encode(final Codec<V2> codec, final V input, final DynamicOps<T> ops) {
            return codec.encodeStart(ops, input);
         }
      };
   }

   public static <A> Codec<Optional<A>> optionalEmptyMap(final Codec<A> codec) {
      return new Codec<Optional<A>>() {
         public <T> DataResult<Pair<Optional<A>, T>> decode(final DynamicOps<T> ops, final T input) {
            return isEmptyMap(ops, input)
               ? DataResult.success(Pair.of(Optional.empty(), input))
               : codec.decode(ops, input).map(pair -> pair.mapFirst(Optional::of));
         }

         private static <T> boolean isEmptyMap(final DynamicOps<T> ops, final T input) {
            Optional<MapLike<T>> map = ops.getMap(input).result();
            return map.isPresent() && map.get().entries().findAny().isEmpty();
         }

         public <T> DataResult<T> encode(final Optional<A> input, final DynamicOps<T> ops, final T prefix) {
            return input.isEmpty() ? DataResult.success(ops.emptyMap()) : codec.encode(input.get(), ops, prefix);
         }
      };
   }

   @Deprecated
   public static <E extends Enum<E>> Codec<E> legacyEnum(final Function<String, E> valueOf) {
      return Codec.STRING.comapFlatMap(key -> {
         try {
            return DataResult.success(valueOf.apply(key));
         } catch (IllegalArgumentException ignored) {
            return DataResult.error(() -> "No value with id: " + key);
         }
      }, Enum::toString);
   }

   public static Codec<Path> pathCodec(final Function<String, Path> pathFactory) {
      return Codec.STRING.xmap(pathFactory, path -> FilenameUtils.separatorsToUnix(path.toString()));
   }

   public static Codec<Path> relaiveNormalizedSubPathCodec(final Function<String, Path> pathFactory) {
      return pathCodec(pathFactory)
         .xmap(Path::normalize, Path::normalize)
         .validate(
            path -> {
               if (path.isAbsolute()) {
                  return DataResult.error(() -> "Illegal absolute path: " + path);
               } else {
                  return !path.startsWith("..") && !path.startsWith(".") && !FileUtil.isEmptyPath(path)
                     ? DataResult.success(path)
                     : DataResult.error(() -> "Illegal path traversal: " + path);
               }
            }
         );
   }

   public static Codec<Path> guardedPathCodec(final Path baseFolder) {
      FileSystem var1 = baseFolder.getFileSystem();
      return relaiveNormalizedSubPathCodec(x$0 -> var1.getPath(x$0)).xmap(baseFolder::resolve, baseFolder::relativize);
   }

   public static <A> MapCodec<A> optionalAlwaysPresentFieldOf(final Codec<A> elementCodec, final String name, final A defaultValue, final boolean lenient) {
      return Codec.optionalField(name, elementCodec, lenient).xmap(o -> o.orElse(defaultValue), Optional::of);
   }

   public static <A> MapCodec<A> optionalAlwaysPresentFieldOf(final Codec<A> elementCodec, final String name, final A defaultValue) {
      return optionalAlwaysPresentFieldOf(elementCodec, name, defaultValue, false);
   }

   public static class LateBoundIdMapper<I, V> {
      private final BiMap<I, V> idToValue = HashBiMap.create();

      public Codec<V> codec(final Codec<I> idCodec) {
         BiMap<V, I> valueToId = this.idToValue.inverse();
         return ExtraCodecs.idResolverCodec(idCodec, this.idToValue::get, valueToId::get);
      }

      public ExtraCodecs.LateBoundIdMapper<I, V> put(final I id, final V value) {
         Objects.requireNonNull(value, () -> "Value for " + id + " is null");
         this.idToValue.put(id, value);
         return this;
      }

      public Set<V> values() {
         return Collections.unmodifiableSet(this.idToValue.values());
      }
   }

   public record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements BaseMapCodec<K, V>, Codec<Map<K, V>> {
      public <T> DataResult<Map<K, V>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
         com.google.common.collect.ImmutableMap.Builder<K, V> read = ImmutableMap.builder();

         for (Pair<T, T> pair : input.entries().toList()) {
            DataResult<K> k = this.keyCodec().parse(ops, pair.getFirst());
            DataResult<V> v = this.elementCodec().parse(ops, pair.getSecond());
            DataResult<Pair<K, V>> entry = k.apply2stable(Pair::of, v);
            Optional<Error<Pair<K, V>>> error = entry.error();
            if (error.isPresent()) {
               String errorMessage = error.get().message();
               return DataResult.error(() -> k.result().isPresent() ? "Map entry '" + k.result().get() + "' : " + errorMessage : errorMessage);
            }

            if (!entry.result().isPresent()) {
               return DataResult.error(() -> "Empty or invalid map contents are not allowed");
            }

            Pair<K, V> kvPair = (Pair<K, V>)entry.result().get();
            read.put(kvPair.getFirst(), kvPair.getSecond());
         }

         Map<K, V> elements = read.build();
         return DataResult.success(elements);
      }

      public <T> DataResult<Pair<Map<K, V>, T>> decode(final DynamicOps<T> ops, final T input) {
         return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap(map -> this.decode(ops, map)).map(r -> Pair.of(r, input));
      }

      public <T> DataResult<T> encode(final Map<K, V> input, final DynamicOps<T> ops, final T prefix) {
         return this.encode(input, ops, ops.mapBuilder()).build(prefix);
      }

      @Override
      public String toString() {
         return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
      }
   }

   public record TagOrElementLocation(Identifier id, boolean tag) {
      @Override
      public String toString() {
         return this.decoratedId();
      }

      private String decoratedId() {
         return this.tag ? "#" + this.id : this.id.toString();
      }
   }
}
