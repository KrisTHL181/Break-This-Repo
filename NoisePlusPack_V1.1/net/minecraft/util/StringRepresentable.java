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

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> p_216440_) {
      return fromEnumWithMapping(p_216440_, p_312201_ -> p_312201_);
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(
      Supplier<E[]> p_275615_, Function<String, String> p_275259_
   ) {
      E[] ae = (E[])p_275615_.get();
      Function<String, E> function = createNameLookup(ae, p_421540_ -> p_275259_.apply(p_421540_.getSerializedName()));
      return new StringRepresentable.EnumCodec<>(ae, function);
   }

   static <T extends StringRepresentable> Codec<T> fromValues(Supplier<T[]> p_311788_) {
      T[] at = (T[])p_311788_.get();
      Function<String, T> function = createNameLookup(at);
      ToIntFunction<T> tointfunction = Util.createIndexLookup(Arrays.asList(at));
      return new StringRepresentable.StringRepresentableCodec<>(at, function, tointfunction);
   }

   static <T extends StringRepresentable> Function<String, @Nullable T> createNameLookup(T[] p_430289_) {
      return createNameLookup(p_430289_, StringRepresentable::getSerializedName);
   }

   static <T> Function<String, @Nullable T> createNameLookup(T[] p_424390_, Function<T, String> p_313109_) {
      if (p_424390_.length > 16) {
         Map<String, T> map = Arrays.<T>stream(p_424390_).collect(Collectors.toMap(p_313109_, p_426400_ -> (T)p_426400_));
         return map::get;
      } else {
         return p_421543_ -> {
            for (T t : p_424390_) {
               if (p_313109_.apply(t).equals(p_421543_)) {
                  return t;
               }
            }

            return null;
         };
      }
   }

   static Keyable keys(final StringRepresentable[] p_14358_) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
            return Arrays.stream(p_14358_).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
         }
      };
   }

   class EnumCodec<E extends Enum<E> & StringRepresentable> extends StringRepresentable.StringRepresentableCodec<E> {
      private final Function<String, @Nullable E> resolver;

      public EnumCodec(E[] p_216447_, Function<String, E> p_216448_) {
         super(p_216447_, p_216448_, p_216454_ -> p_216454_.ordinal());
         this.resolver = p_216448_;
      }

      public @Nullable E byName(String p_216456_) {
         return this.resolver.apply(p_216456_);
      }

      public E byName(String p_263077_, E p_263115_) {
         return Objects.requireNonNullElse(this.byName(p_263077_), p_263115_);
      }

      public E byName(String p_367164_, Supplier<? extends E> p_363447_) {
         return Objects.requireNonNullElseGet(this.byName(p_367164_), p_363447_);
      }
   }

   class StringRepresentableCodec<S extends StringRepresentable> implements Codec<S> {
      private final Codec<S> codec;

      public StringRepresentableCodec(S[] p_309730_, Function<String, @Nullable S> p_311107_, ToIntFunction<S> p_312549_) {
         this.codec = ExtraCodecs.orCompressed(
            Codec.stringResolver(StringRepresentable::getSerializedName, p_311107_),
            ExtraCodecs.idResolverCodec(p_312549_, p_312747_ -> p_312747_ >= 0 && p_312747_ < p_309730_.length ? p_309730_[p_312747_] : null, -1)
         );
      }

      public <T> DataResult<Pair<S, T>> decode(DynamicOps<T> p_310491_, T p_312317_) {
         return this.codec.decode(p_310491_, p_312317_);
      }

      public <T> DataResult<T> encode(S p_312413_, DynamicOps<T> p_310685_, T p_312430_) {
         return this.codec.encode(p_312413_, p_310685_, p_312430_);
      }
   }
}
