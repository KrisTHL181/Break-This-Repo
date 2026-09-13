package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(final Supplier<E[]> values) {
      return fromEnumWithMapping(values, s -> s);
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
      final Supplier<E[]> values, final Function<String, String> converter
   ) {
      E[] valueArray = (E[])values.get();
      Function<String, E> lookupFunction = createNameLookup(valueArray, e -> converter.apply(e.getSerializedName()));
      return new StringRepresentable.EnumCodec<>(valueArray, lookupFunction);
   }

   static <T extends StringRepresentable> Codec<T> fromValues(final Supplier<T[]> values) {
      T[] valueArray = (T[])values.get();
      Function<String, T> lookupFunction = createNameLookup(valueArray);
      ToIntFunction<T> indexLookup = Util.createIndexLookup(Arrays.asList(valueArray));
      return new StringRepresentable.StringRepresentableCodec<>(valueArray, lookupFunction, indexLookup);
   }

   static <T extends StringRepresentable> Function<String, @Nullable T> createNameLookup(final T[] valueArray) {
      return createNameLookup(valueArray, StringRepresentable::getSerializedName);
   }

   static <T> Function<String, @Nullable T> createNameLookup(final T[] valueArray, final Function<T, String> converter) {
      if (valueArray.length > 16) {
         Map<String, T> byName = Arrays.<T>stream(valueArray).collect(Collectors.toMap(converter, d -> (T)d));
         return byName::get;
      } else {
         return id -> {
            for (T value : valueArray) {
               if (converter.apply(value).equals(id)) {
                  return value;
               }
            }

            return null;
         };
      }
   }

   static Keyable keys(final StringRepresentable[] values) {
      return new Keyable() {
         public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return Arrays.stream(values).map(StringRepresentable::getSerializedName).map(ops::createString);
         }
      };
   }

   class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
      private final Function<String, @Nullable E> resolver;

      public EnumCodec(final E[] valueArray, final Function<String, E> nameResolver) {
         super(valueArray, nameResolver, rec$ -> rec$.ordinal());
         this.resolver = nameResolver;
      }

      public @Nullable E byName(final String name) {
         return this.resolver.apply(name);
      }

      public E byName(final String name, final E _default) {
         return Objects.requireNonNullElse(this.byName(name), _default);
      }

      public E byName(final String name, final Supplier<? extends E> defaultSupplier) {
         return Objects.requireNonNullElseGet(this.byName(name), defaultSupplier);
      }
   }

   class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
      private final Codec<S> codec;

      public StringRepresentableCodec(final S[] valueArray, final Function<String, @Nullable S> nameResolver, final ToIntFunction<S> idResolver) {
         this.codec = ExtraCodecs.orCompressed(
            Codec.stringResolver(StringRepresentable::getSerializedName, nameResolver),
            ExtraCodecs.idResolverCodec(idResolver, i -> i >= 0 && i < valueArray.length ? valueArray[i] : null, -1)
         );
      }

      public <T> DataResult<Pair<S, T>> decode(final DynamicOps<T> ops, final T input) {
         return this.codec.decode(ops, input);
      }

      public <T> DataResult<T> encode(final S input, final DynamicOps<T> ops, final T prefix) {
         return this.codec.encode(input, ops, prefix);
      }
   }
}
