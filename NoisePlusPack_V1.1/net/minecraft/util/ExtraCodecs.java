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
      .comapFlatMap(
         p_449274_ -> Util.fixedSize(p_449274_, 2).map(p_405241_ -> new Vector2f((Float)p_405241_.get(0), (Float)p_405241_.get(1))),
         p_449248_ -> List.of(p_449248_.x(), p_449248_.y())
      );
   public static final Codec<Vector3fc> VECTOR3F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_449276_ -> Util.fixedSize(p_449276_, 3).map(p_253489_ -> new Vector3f((Float)p_253489_.get(0), (Float)p_253489_.get(1), (Float)p_253489_.get(2))),
         p_449249_ -> List.of(p_449249_.x(), p_449249_.y(), p_449249_.z())
      );
   public static final Codec<Vector3ic> VECTOR3I = Codec.INT
      .listOf()
      .comapFlatMap(
         p_449253_ -> Util.fixedSize(p_449253_, 3)
            .map(p_405240_ -> new Vector3i((Integer)p_405240_.get(0), (Integer)p_405240_.get(1), (Integer)p_405240_.get(2))),
         p_449270_ -> List.of(p_449270_.x(), p_449270_.y(), p_449270_.z())
      );
   public static final Codec<Vector4fc> VECTOR4F = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_449240_ -> Util.fixedSize(p_449240_, 4)
            .map(p_326509_ -> new Vector4f((Float)p_326509_.get(0), (Float)p_326509_.get(1), (Float)p_326509_.get(2), (Float)p_326509_.get(3))),
         p_449252_ -> List.of(p_449252_.x(), p_449252_.y(), p_449252_.z(), p_449252_.w())
      );
   public static final Codec<Quaternionfc> QUATERNIONF_COMPONENTS = Codec.FLOAT
      .listOf()
      .comapFlatMap(
         p_449256_ -> Util.fixedSize(p_449256_, 4)
            .map(p_341245_ -> new Quaternionf((Float)p_341245_.get(0), (Float)p_341245_.get(1), (Float)p_341245_.get(2), (Float)p_341245_.get(3)).normalize()),
         p_449261_ -> List.of(p_449261_.x(), p_449261_.y(), p_449261_.z(), p_449261_.w())
      );
   public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
      p_269774_ -> p_269774_.group(
            Codec.FLOAT.fieldOf("angle").forGetter(p_269776_ -> p_269776_.angle),
            VECTOR3F.fieldOf("axis").forGetter(p_269778_ -> new Vector3f(p_269778_.x, p_269778_.y, p_269778_.z))
         )
         .apply(p_269774_, AxisAngle4f::new)
   );
   public static final Codec<Quaternionfc> QUATERNIONF = Codec.withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
   public static final Codec<Matrix4fc> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(p_449269_ -> Util.fixedSize(p_449269_, 16).map(p_269777_ -> {
      Matrix4f matrix4f = new Matrix4f();

      for (int i = 0; i < p_269777_.size(); i++) {
         matrix4f.setRowColumn(i >> 2, i & 3, (Float)p_269777_.get(i));
      }

      return matrix4f.determineProperties();
   }), p_390212_ -> {
      FloatList floatlist = new FloatArrayList(16);

      for (int i = 0; i < 16; i++) {
         floatlist.add(p_390212_.getRowColumn(i >> 2, i & 3));
      }

      return floatlist;
   });
   private static final String HEX_COLOR_PREFIX = "#";
   public static final Codec<Integer> RGB_COLOR_CODEC = Codec.withAlternative(
      Codec.INT, VECTOR3F, p_449262_ -> ARGB.colorFromFloat(1.0F, p_449262_.x(), p_449262_.y(), p_449262_.z())
   );
   public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.withAlternative(
      Codec.INT, VECTOR4F, p_449255_ -> ARGB.colorFromFloat(p_449255_.w(), p_449255_.x(), p_449255_.y(), p_449255_.z())
   );
   public static final Codec<Integer> STRING_RGB_COLOR = Codec.withAlternative(hexColor(6).xmap(ARGB::opaque, ARGB::transparent), RGB_COLOR_CODEC);
   public static final Codec<Integer> STRING_ARGB_COLOR = Codec.withAlternative(hexColor(8), ARGB_COLOR_CODEC);
   public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
      .flatComapMap(
         UnsignedBytes::toInt,
         p_326500_ -> p_326500_ > 255
            ? DataResult.error(() -> "Unsigned byte was too large: " + p_326500_ + " > 255")
            : DataResult.success(p_326500_.byteValue())
      );
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, p_453333_ -> "Value must be non-negative: " + p_453333_);
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, p_452514_ -> "Value must be positive: " + p_452514_);
   public static final Codec<Long> NON_NEGATIVE_LONG = longRangeWithMessage(0L, Long.MAX_VALUE, p_449264_ -> "Value must be non-negative: " + p_449264_);
   public static final Codec<Long> POSITIVE_LONG = longRangeWithMessage(1L, Long.MAX_VALUE, p_449272_ -> "Value must be positive: " + p_449272_);
   public static final Codec<Float> NON_NEGATIVE_FLOAT = floatRangeMinInclusiveWithMessage(
      0.0F, Float.MAX_VALUE, p_449275_ -> "Value must be non-negative: " + p_449275_
   );
   public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(
      0.0F, Float.MAX_VALUE, p_449254_ -> "Value must be positive: " + p_449254_
   );
   public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(p_274857_ -> {
      try {
         return DataResult.success(Pattern.compile(p_274857_));
      } catch (PatternSyntaxException patternsyntaxexception) {
         return DataResult.error(() -> "Invalid regex pattern '" + p_274857_ + "': " + patternsyntaxexception.getMessage());
      }
   }, Pattern::pattern);
   public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
   public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(p_274852_ -> {
      try {
         return DataResult.success(Base64.getDecoder().decode(p_274852_));
      } catch (IllegalArgumentException illegalargumentexception) {
         return DataResult.error(() -> "Malformed base64 string");
      }
   }, p_216180_ -> Base64.getEncoder().encodeToString(p_216180_));
   public static final Codec<String> ESCAPED_STRING = Codec.STRING
      .comapFlatMap(p_296617_ -> DataResult.success(StringEscapeUtils.unescapeJava(p_296617_)), StringEscapeUtils::escapeJava);
   public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
      .comapFlatMap(
         p_449268_ -> p_449268_.startsWith("#")
            ? Identifier.read(p_449268_.substring(1)).map(p_449242_ -> new ExtraCodecs.TagOrElementLocation(p_449242_, true))
            : Identifier.read(p_449268_).map(p_449241_ -> new ExtraCodecs.TagOrElementLocation(p_449241_, false)),
         ExtraCodecs.TagOrElementLocation::decoratedId
      );
   public static final Function<Optional<Long>, OptionalLong> toOptionalLong = p_216176_ -> p_216176_.map(OptionalLong::of).orElseGet(OptionalLong::empty);
   public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = p_216178_ -> p_216178_.isPresent()
      ? Optional.of(p_216178_.getAsLong())
      : Optional.empty();
   public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
      .xmap(p_253514_ -> BitSet.valueOf(p_253514_.toArray()), p_253493_ -> Arrays.stream(p_253493_.toLongArray()));
   public static final int MAX_PROPERTY_NAME_LENGTH = 64;
   public static final int MAX_PROPERTY_VALUE_LENGTH = 32767;
   public static final int MAX_PROPERTY_SIGNATURE_LENGTH = 1024;
   public static final int MAX_PROPERTIES = 16;
   private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
      p_449282_ -> p_449282_.group(
            Codec.sizeLimitedString(64).fieldOf("name").forGetter(Property::name),
            Codec.sizeLimitedString(32767).fieldOf("value").forGetter(Property::value),
            Codec.sizeLimitedString(1024).optionalFieldOf("signature").forGetter(p_296611_ -> Optional.ofNullable(p_296611_.signature()))
         )
         .apply(p_449282_, (p_253494_, p_253495_, p_253496_) -> new Property(p_253494_, p_253495_, (String)p_253496_.orElse(null)))
   );
   public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(
         Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
            .validate(
               p_449281_ -> p_449281_.size() > 16
                  ? DataResult.error(() -> "Cannot have more than 16 properties, but was " + p_449281_.size())
                  : DataResult.success(p_449281_)
            ),
         PROPERTY.sizeLimitedListOf(16)
      )
      .xmap(p_421534_ -> {
         Builder<String, Property> builder = ImmutableMultimap.builder();
         p_421534_.ifLeft(p_253506_ -> p_253506_.forEach((p_421532_, p_421533_) -> {
            for (String s : p_421533_) {
               builder.put(p_421532_, new Property(p_421532_, s));
            }
         })).ifRight(p_421536_ -> {
            for (Property property : p_421536_) {
               builder.put(property.name(), property);
            }
         });
         return new PropertyMap(builder.build());
      }, p_253504_ -> Either.right(p_253504_.values().stream().toList()));
   public static final Codec<String> PLAYER_NAME = Codec.string(0, 16)
      .validate(
         p_326493_ -> StringUtil.isValidPlayerName(p_326493_)
            ? DataResult.success(p_326493_)
            : DataResult.error(() -> "Player name contained disallowed characters: '" + p_326493_ + "'")
      );
   public static final Codec<GameProfile> AUTHLIB_GAME_PROFILE = gameProfileCodec(UUIDUtil.AUTHLIB_CODEC).codec();
   public static final MapCodec<GameProfile> STORED_GAME_PROFILE = gameProfileCodec(UUIDUtil.CODEC);
   public static final Codec<String> NON_EMPTY_STRING = Codec.STRING
      .validate(p_274858_ -> p_274858_.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(p_274858_));
   public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(p_284688_ -> {
      int[] aint = p_284688_.codePoints().toArray();
      return aint.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + p_284688_) : DataResult.success(aint[0]);
   }, Character::toString);
   public static final Codec<String> RESOURCE_PATH_CODEC = Codec.STRING
      .validate(
         p_449273_ -> !Identifier.isValidPath(p_449273_)
            ? DataResult.error(() -> "Invalid string to use as a resource path element: " + p_449273_)
            : DataResult.success(p_449273_)
      );
   public static final Codec<URI> UNTRUSTED_URI = Codec.STRING.comapFlatMap(p_449263_ -> {
      try {
         return DataResult.success(Util.parseAndValidateUntrustedUri(p_449263_));
      } catch (URISyntaxException urisyntaxexception) {
         return DataResult.error(urisyntaxexception::getMessage);
      }
   }, URI::toString);
   public static final Codec<String> CHAT_STRING = Codec.STRING.validate(p_421528_ -> {
      for (int i = 0; i < p_421528_.length(); i++) {
         char c0 = p_421528_.charAt(i);
         if (!StringUtil.isAllowedChatCharacter(c0)) {
            return DataResult.error(() -> "Disallowed chat character: '" + c0 + "'");
         }
      }

      return DataResult.success(p_421528_);
   });

   public static <T> Codec<T> converter(DynamicOps<T> p_310943_) {
      return Codec.PASSTHROUGH.xmap(p_308959_ -> p_308959_.convert(p_310943_).getValue(), p_308962_ -> new Dynamic(p_310943_, p_308962_));
   }

   private static Codec<Integer> hexColor(int p_459516_) {
      long i = (1L << p_459516_ * 4) - 1L;
      return Codec.STRING.comapFlatMap(p_449267_ -> {
         if (!p_449267_.startsWith("#")) {
            return DataResult.error(() -> "Hex color must begin with #");
         }

         int j = p_449267_.length() - "#".length();
         if (j != p_459516_) {
            return DataResult.error(() -> "Hex color is wrong size, expected " + p_459516_ + " digits but got " + j);
         }

         try {
            long k = HexFormat.fromHexDigitsToLong(p_449267_, "#".length(), p_449267_.length());
            return k >= 0L && k <= i ? DataResult.success((int)k) : DataResult.error(() -> "Color value out of range: " + p_449267_);
         } catch (NumberFormatException numberformatexception) {
            return DataResult.error(() -> "Invalid color value: " + p_449267_);
         }
      }, p_449247_ -> "#" + HexFormat.of().toHexDigits(p_449247_.intValue(), p_459516_));
   }

   public static <P, I> Codec<I> intervalCodec(
      Codec<P> p_184362_, String p_184363_, String p_184364_, BiFunction<P, P, DataResult<I>> p_184365_, Function<I, P> p_184366_, Function<I, P> p_184367_
   ) {
      Codec<I> codec = Codec.list(p_184362_).comapFlatMap(p_449251_ -> Util.fixedSize(p_449251_, 2).flatMap(p_184445_ -> {
         P p = (P)p_184445_.get(0);
         P p1 = (P)p_184445_.get(1);
         return p_184365_.apply(p, p1);
      }), p_184459_ -> ImmutableList.of(p_184366_.apply((I)p_184459_), p_184367_.apply((I)p_184459_)));
      Codec<I> codec1 = RecordCodecBuilder.create(
            p_184360_ -> p_184360_.group(p_184362_.fieldOf(p_184363_).forGetter(Pair::getFirst), p_184362_.fieldOf(p_184364_).forGetter(Pair::getSecond))
               .apply(p_184360_, Pair::of)
         )
         .comapFlatMap(
            p_184392_ -> p_184365_.apply((P)p_184392_.getFirst(), (P)p_184392_.getSecond()),
            p_184449_ -> Pair.of(p_184366_.apply((I)p_184449_), p_184367_.apply((I)p_184449_))
         );
      Codec<I> codec2 = Codec.withAlternative(codec, codec1);
      return Codec.either(p_184362_, codec2)
         .comapFlatMap(p_184389_ -> (DataResult)p_184389_.map(p_184395_ -> p_184365_.apply((P)p_184395_, (P)p_184395_), DataResult::success), p_184411_ -> {
            P p = p_184366_.apply((I)p_184411_);
            P p1 = p_184367_.apply((I)p_184411_);
            return Objects.equals(p, p1) ? Either.left(p) : Either.right(p_184411_);
         });
   }

   public static <A> ResultFunction<A> orElsePartial(final A p_184382_) {
      return new ResultFunction<A>() {
         public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> p_184466_, T p_184467_, DataResult<Pair<A, T>> p_184468_) {
            MutableObject<String> mutableobject = new MutableObject();
            Optional<Pair<A, T>> optional = p_184468_.resultOrPartial(mutableobject::setValue);
            return optional.isPresent()
               ? p_184468_
               : DataResult.error(() -> "(" + (String)mutableobject.get() + " -> using default)", Pair.of(p_184382_, p_184467_));
         }

         public <T> DataResult<T> coApply(DynamicOps<T> p_184470_, A p_184471_, DataResult<T> p_184472_) {
            return p_184472_;
         }

         @Override
         public String toString() {
            return "OrElsePartial[" + p_184382_ + "]";
         }
      };
   }

   public static <E> Codec<E> idResolverCodec(ToIntFunction<E> p_184422_, IntFunction<@Nullable E> p_184423_, int p_184424_) {
      return Codec.INT
         .flatXmap(
            p_184414_ -> Optional.ofNullable(p_184423_.apply(p_184414_))
               .<DataResult>map(DataResult::success)
               .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + p_184414_)),
            p_274850_ -> {
               int i = p_184422_.applyAsInt((E)p_274850_);
               return i == p_184424_ ? DataResult.error(() -> "Element with unknown id: " + p_274850_) : DataResult.success(i);
            }
         );
   }

   public static <I, E> Codec<E> idResolverCodec(Codec<I> p_376599_, Function<I, @Nullable E> p_378105_, Function<E, @Nullable I> p_376561_) {
      return p_376599_.flatXmap(p_374893_ -> {
         E e = p_378105_.apply((I)p_374893_);
         return e == null ? DataResult.error(() -> "Unknown element id: " + p_374893_) : DataResult.success(e);
      }, p_374900_ -> {
         I i = p_376561_.apply((E)p_374900_);
         return i == null ? DataResult.error(() -> "Element with unknown id: " + p_374900_) : DataResult.success(i);
      });
   }

   public static <E> Codec<E> orCompressed(final Codec<E> p_184426_, final Codec<E> p_184427_) {
      return new Codec<E>() {
         public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_) {
            return p_184484_.compressMaps() ? p_184427_.encode(p_184483_, p_184484_, p_184485_) : p_184426_.encode(p_184483_, p_184484_, p_184485_);
         }

         public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_) {
            return p_184480_.compressMaps() ? p_184427_.decode(p_184480_, p_184481_) : p_184426_.decode(p_184480_, p_184481_);
         }

         @Override
         public String toString() {
            return p_184426_ + " orCompressed " + p_184427_;
         }
      };
   }

   public static <E> MapCodec<E> orCompressed(final MapCodec<E> p_311419_, final MapCodec<E> p_312369_) {
      return new MapCodec<E>() {
         public <T> RecordBuilder<T> encode(E p_310450_, DynamicOps<T> p_312581_, RecordBuilder<T> p_310094_) {
            return p_312581_.compressMaps() ? p_312369_.encode(p_310450_, p_312581_, p_310094_) : p_311419_.encode(p_310450_, p_312581_, p_310094_);
         }

         public <T> DataResult<E> decode(DynamicOps<T> p_312833_, MapLike<T> p_309452_) {
            return p_312833_.compressMaps() ? p_312369_.decode(p_312833_, p_309452_) : p_311419_.decode(p_312833_, p_309452_);
         }

         public <T> Stream<T> keys(DynamicOps<T> p_311885_) {
            return p_312369_.keys(p_311885_);
         }

         public String toString() {
            return p_311419_ + " orCompressed " + p_312369_;
         }
      };
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> p_184369_, final Function<E, Lifecycle> p_184370_, final Function<E, Lifecycle> p_184371_) {
      return p_184369_.mapResult(new ResultFunction<E>() {
         public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> p_310634_, T p_310147_, DataResult<Pair<E, T>> p_311056_) {
            return p_311056_.result().map(p_326518_ -> p_311056_.setLifecycle(p_184370_.apply((E)p_326518_.getFirst()))).orElse(p_311056_);
         }

         public <T> DataResult<T> coApply(DynamicOps<T> p_311101_, E p_309590_, DataResult<T> p_309495_) {
            return p_309495_.setLifecycle(p_184371_.apply(p_309590_));
         }

         @Override
         public String toString() {
            return "WithLifecycle[" + p_184370_ + " " + p_184371_ + "]";
         }
      });
   }

   public static <E> Codec<E> overrideLifecycle(Codec<E> p_331277_, Function<E, Lifecycle> p_331041_) {
      return overrideLifecycle(p_331277_, p_331041_, p_331041_);
   }

   public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> p_298880_, Codec<V> p_297369_) {
      return new ExtraCodecs.StrictUnboundedMapCodec<>(p_298880_, p_297369_);
   }

   public static <E> Codec<List<E>> compactListCodec(Codec<E> p_375826_) {
      return compactListCodec(p_375826_, p_375826_.listOf());
   }

   public static <E> Codec<List<E>> compactListCodec(Codec<E> p_378656_, Codec<List<E>> p_377134_) {
      return Codec.either(p_377134_, p_378656_)
         .xmap(
            p_374901_ -> (List)p_374901_.map(p_374891_ -> p_374891_, List::of),
            p_374897_ -> p_374897_.size() == 1 ? Either.right(p_374897_.getFirst()) : Either.left(p_374897_)
         );
   }

   private static Codec<Integer> intRangeWithMessage(int p_144634_, int p_144635_, Function<Integer, String> p_144636_) {
      return Codec.INT
         .validate(
            p_274889_ -> p_274889_.compareTo(p_144634_) >= 0 && p_274889_.compareTo(p_144635_) <= 0
               ? DataResult.success(p_274889_)
               : DataResult.error(() -> p_144636_.apply(p_274889_))
         );
   }

   public static Codec<Integer> intRange(int p_270883_, int p_270323_) {
      return intRangeWithMessage(p_270883_, p_270323_, p_269784_ -> "Value must be within range [" + p_270883_ + ";" + p_270323_ + "]: " + p_269784_);
   }

   private static Codec<Long> longRangeWithMessage(long p_451065_, long p_460193_, Function<Long, String> p_461090_) {
      return Codec.LONG
         .validate(
            p_449280_ -> p_449280_.compareTo(p_451065_) >= 0L && p_449280_.compareTo(p_460193_) <= 0L
               ? DataResult.success(p_449280_)
               : DataResult.error(() -> p_461090_.apply(p_449280_))
         );
   }

   public static Codec<Long> longRange(int p_457684_, int p_456426_) {
      return longRangeWithMessage(p_457684_, p_456426_, p_449245_ -> "Value must be within range [" + p_457684_ + ";" + p_456426_ + "]: " + p_449245_);
   }

   private static Codec<Float> floatRangeMinInclusiveWithMessage(float p_367913_, float p_363196_, Function<Float, String> p_366905_) {
      return Codec.FLOAT
         .validate(
            p_358800_ -> p_358800_.compareTo(p_367913_) >= 0 && p_358800_.compareTo(p_363196_) <= 0
               ? DataResult.success(p_358800_)
               : DataResult.error(() -> p_366905_.apply(p_358800_))
         );
   }

   private static Codec<Float> floatRangeMinExclusiveWithMessage(float p_184351_, float p_184352_, Function<Float, String> p_184353_) {
      return Codec.FLOAT
         .validate(
            p_274865_ -> p_274865_.compareTo(p_184351_) > 0 && p_274865_.compareTo(p_184352_) <= 0
               ? DataResult.success(p_274865_)
               : DataResult.error(() -> p_184353_.apply(p_274865_))
         );
   }

   public static Codec<Float> floatRange(float p_375431_, float p_376877_) {
      return floatRangeMinInclusiveWithMessage(
         p_375431_, p_376877_, p_374896_ -> "Value must be within range [" + p_375431_ + ";" + p_376877_ + "]: " + p_374896_
      );
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> p_144638_) {
      return p_144638_.validate(p_274853_ -> p_274853_.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(p_274853_));
   }

   public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> p_203983_) {
      return p_203983_.validate(
         p_274860_ -> p_274860_.unwrap().right().filter(List::isEmpty).isPresent()
            ? DataResult.error(() -> "List must have contents")
            : DataResult.success(p_274860_)
      );
   }

   public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(Codec<M> p_365784_) {
      return p_365784_.validate(p_358805_ -> p_358805_.isEmpty() ? DataResult.error(() -> "Map must have contents") : DataResult.success(p_358805_));
   }

   public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> p_203977_) {
      class ContextRetrievalCodec extends MapCodec<E> {
         public <T> RecordBuilder<T> encode(E p_203993_, DynamicOps<T> p_203994_, RecordBuilder<T> p_203995_) {
            return p_203995_;
         }

         public <T> DataResult<E> decode(DynamicOps<T> p_203990_, MapLike<T> p_203991_) {
            return p_203977_.apply(p_203990_);
         }

         public String toString() {
            return "ContextRetrievalCodec[" + p_203977_ + "]";
         }

         public <T> Stream<T> keys(DynamicOps<T> p_203997_) {
            return Stream.empty();
         }
      }

      return new ContextRetrievalCodec();
   }

   public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> p_203985_) {
      return p_203980_ -> {
         Iterator<E> iterator = p_203980_.iterator();
         if (iterator.hasNext()) {
            T t = p_203985_.apply(iterator.next());

            while (iterator.hasNext()) {
               E e = iterator.next();
               T t1 = p_203985_.apply(e);
               if (t1 != t) {
                  return DataResult.error(() -> "Mixed type list: element " + e + " had type " + t1 + ", but list is of type " + t);
               }
            }
         }

         return DataResult.success(p_203980_, Lifecycle.stable());
      };
   }

   public static <A> Codec<A> catchDecoderException(final Codec<A> p_216186_) {
      return Codec.of(p_216186_, new Decoder<A>() {
         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_309963_, T p_309877_) {
            try {
               return p_216186_.decode(p_309963_, p_309877_);
            } catch (Exception exception) {
               return DataResult.error(() -> "Caught exception decoding " + p_309877_ + ": " + exception.getMessage());
            }
         }
      });
   }

   public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter p_216171_) {
      return Codec.STRING.comapFlatMap(p_296605_ -> {
         try {
            return DataResult.success(p_216171_.parse(p_296605_));
         } catch (Exception exception) {
            return DataResult.error(exception::getMessage);
         }
      }, p_216171_::format);
   }

   public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> p_216167_) {
      return p_216167_.xmap(toOptionalLong, fromOptionalLong);
   }

   private static MapCodec<GameProfile> gameProfileCodec(Codec<UUID> p_425773_) {
      return RecordCodecBuilder.mapCodec(
         p_421530_ -> p_421530_.group(
               p_425773_.fieldOf("id").forGetter(GameProfile::id),
               PLAYER_NAME.fieldOf("name").forGetter(GameProfile::name),
               PROPERTY_MAP.optionalFieldOf("properties", PropertyMap.EMPTY).forGetter(GameProfile::properties)
            )
            .apply(p_421530_, GameProfile::new)
      );
   }

   public static <K, V> Codec<Map<K, V>> sizeLimitedMap(Codec<Map<K, V>> p_331930_, int p_328430_) {
      return p_331930_.validate(
         p_326506_ -> p_326506_.size() > p_328430_
            ? DataResult.error(() -> "Map is too long: " + p_326506_.size() + ", expected range [0-" + p_328430_ + "]")
            : DataResult.success(p_326506_)
      );
   }

   public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> p_300841_) {
      return Codec.unboundedMap(p_300841_, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
   }

   @Deprecated
   public static <K, V> MapCodec<V> dispatchOptionalValue(
      final String p_311089_,
      final String p_310965_,
      final Codec<K> p_310912_,
      final Function<? super V, ? extends K> p_311760_,
      final Function<? super K, ? extends Codec<? extends V>> p_312960_
   ) {
      return new MapCodec<V>() {
         public <T> Stream<T> keys(DynamicOps<T> p_310901_) {
            return Stream.of((T[])(new Object[]{p_310901_.createString(p_311089_), p_310901_.createString(p_310965_)}));
         }

         public <T> DataResult<V> decode(DynamicOps<T> p_310472_, MapLike<T> p_310342_) {
            T t = (T)p_310342_.get(p_311089_);
            return t == null ? DataResult.error(() -> "Missing \"" + p_311089_ + "\" in: " + p_310342_) : p_310912_.decode(p_310472_, t).flatMap(p_326527_ -> {
               T t1 = Objects.requireNonNullElseGet((T)p_310342_.get(p_310965_), p_310472_::emptyMap);
               return p_312960_.apply((K)p_326527_.getFirst()).decode(p_310472_, t1).map(Pair::getFirst);
            });
         }

         public <T> RecordBuilder<T> encode(V p_309380_, DynamicOps<T> p_311460_, RecordBuilder<T> p_311592_) {
            K k = (K)p_311760_.apply(p_309380_);
            p_311592_.add(p_311089_, p_310912_.encodeStart(p_311460_, k));
            DataResult<T> dataresult = this.encode(p_312960_.apply(k), p_309380_, p_311460_);
            if (dataresult.result().isEmpty() || !Objects.equals(dataresult.result().get(), p_311460_.emptyMap())) {
               p_311592_.add(p_310965_, dataresult);
            }

            return p_311592_;
         }

         private <T, V2 extends V> DataResult<T> encode(Codec<V2> p_313212_, V p_310224_, DynamicOps<T> p_311229_) {
            return p_313212_.encodeStart(p_311229_, p_310224_);
         }
      };
   }

   public static <A> Codec<Optional<A>> optionalEmptyMap(final Codec<A> p_329455_) {
      return new Codec<Optional<A>>() {
         public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> p_331677_, T p_335846_) {
            return isEmptyMap(p_331677_, p_335846_)
               ? DataResult.success(Pair.of(Optional.empty(), p_335846_))
               : p_329455_.decode(p_331677_, p_335846_).map(p_333523_ -> p_333523_.mapFirst(Optional::of));
         }

         private static <T> boolean isEmptyMap(DynamicOps<T> p_336166_, T p_333395_) {
            Optional<MapLike<T>> optional = p_336166_.getMap(p_333395_).result();
            return optional.isPresent() && optional.get().entries().findAny().isEmpty();
         }

         public <T> DataResult<T> encode(Optional<A> p_332665_, DynamicOps<T> p_329533_, T p_335687_) {
            return p_332665_.isEmpty() ? DataResult.success(p_329533_.emptyMap()) : p_329455_.encode(p_332665_.get(), p_329533_, p_335687_);
         }
      };
   }

   @Deprecated
   public static <E extends Enum<E>> Codec<E> legacyEnum(Function<String, E> p_394935_) {
      return Codec.STRING.comapFlatMap(p_390207_ -> {
         try {
            return DataResult.success(p_394935_.apply(p_390207_));
         } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "No value with id: " + p_390207_);
         }
      }, Enum::toString);
   }

   public static class LateBoundIdMapper<I, V> {
      private final BiMap<I, V> idToValue = HashBiMap.create();

      public Codec<V> codec(Codec<I> p_375672_) {
         BiMap<V, I> bimap = this.idToValue.inverse();
         return ExtraCodecs.idResolverCodec(p_375672_, this.idToValue::get, bimap::get);
      }

      public ExtraCodecs.LateBoundIdMapper<I, V> put(I p_378774_, V p_377522_) {
         Objects.requireNonNull(p_377522_, () -> "Value for " + p_378774_ + " is null");
         this.idToValue.put(p_378774_, p_377522_);
         return this;
      }

      public Set<V> values() {
         return Collections.unmodifiableSet(this.idToValue.values());
      }
   }

   public record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
      public <T> DataResult<Map<K, V>> decode(DynamicOps<T> p_298061_, MapLike<T> p_299914_) {
         com.google.common.collect.ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

         for (Pair<T, T> pair : p_299914_.entries().toList()) {
            DataResult<K> dataresult = this.keyCodec().parse(p_298061_, pair.getFirst());
            DataResult<V> dataresult1 = this.elementCodec().parse(p_298061_, pair.getSecond());
            DataResult<Pair<K, V>> dataresult2 = dataresult.apply2stable(Pair::of, dataresult1);
            Optional<Error<Pair<K, V>>> optional = dataresult2.error();
            if (optional.isPresent()) {
               String s = optional.get().message();
               return DataResult.error(() -> dataresult.result().isPresent() ? "Map entry '" + dataresult.result().get() + "' : " + s : s);
            }

            if (!dataresult2.result().isPresent()) {
               return DataResult.error(() -> "Empty or invalid map contents are not allowed");
            }

            Pair<K, V> pair1 = (Pair<K, V>)dataresult2.result().get();
            builder.put(pair1.getFirst(), pair1.getSecond());
         }

         Map<K, V> map = builder.build();
         return DataResult.success(map);
      }

      public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> p_299262_, T p_297460_) {
         return p_299262_.getMap(p_297460_)
            .setLifecycle(Lifecycle.stable())
            .flatMap(p_297301_ -> this.decode(p_299262_, p_297301_))
            .map(p_300226_ -> Pair.of(p_300226_, p_297460_));
      }

      public <T> DataResult<T> encode(Map<K, V> p_301091_, DynamicOps<T> p_298442_, T p_300447_) {
         return this.encode(p_301091_, p_298442_, p_298442_.mapBuilder()).build(p_300447_);
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
