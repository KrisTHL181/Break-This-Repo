package net.minecraft.core.component.predicates;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface DataComponentPredicate {
   Codec<Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> CODEC = Codec.dispatchedMap(
      DataComponentPredicate.Type.CODEC, DataComponentPredicate.Type::codec
   );
   StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<?>> SINGLE_STREAM_CODEC = DataComponentPredicate.Type.STREAM_CODEC
      .dispatch(DataComponentPredicate.Single::type, DataComponentPredicate.Type::singleStreamCodec);
   StreamCodec<RegistryFriendlyByteBuf, Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> STREAM_CODEC = SINGLE_STREAM_CODEC.apply(
         ByteBufCodecs.list(64)
      )
      .map(
         singles -> singles.stream().collect(Collectors.toMap(DataComponentPredicate.Single::type, DataComponentPredicate.Single::predicate)),
         map -> map.entrySet().stream().map(DataComponentPredicate.Single::fromEntry).toList()
      );

   static MapCodec<DataComponentPredicate.Single<?>> singleCodec(final String name) {
      return DataComponentPredicate.Type.CODEC.dispatchMap(name, DataComponentPredicate.Single::type, DataComponentPredicate.Type::wrappedCodec);
   }

   boolean matches(DataComponentGetter components);

   final class AnyValueType extends DataComponentPredicate.TypeBase<AnyValue> {
      private final AnyValue predicate;

      public AnyValueType(final AnyValue predicate) {
         super(MapCodec.unitCodec(predicate));
         this.predicate = predicate;
      }

      public AnyValue predicate() {
         return this.predicate;
      }

      public DataComponentType<?> componentType() {
         return this.predicate.type();
      }

      public static DataComponentPredicate.AnyValueType create(final DataComponentType<?> componentType) {
         return new DataComponentPredicate.AnyValueType(new AnyValue(componentType));
      }
   }

   final class ConcreteType<T extends DataComponentPredicate> extends DataComponentPredicate.TypeBase<T> {
      public ConcreteType(final Codec<T> codec) {
         super(codec);
      }
   }

   record Single<T extends DataComponentPredicate>(DataComponentPredicate.Type<T> type, T predicate) {
      private static <T extends DataComponentPredicate> MapCodec<DataComponentPredicate.Single<T>> wrapCodec(
         final DataComponentPredicate.Type<T> type, final Codec<T> codec
      ) {
         return RecordCodecBuilder.mapCodec(
            i -> i.group(codec.fieldOf("value").forGetter(DataComponentPredicate.Single::predicate))
               .apply(i, predicate -> new DataComponentPredicate.Single<>(type, (T)predicate))
         );
      }

      private static <T extends DataComponentPredicate> DataComponentPredicate.Single<T> fromEntry(final Entry<DataComponentPredicate.Type<?>, T> e) {
         return new DataComponentPredicate.Single<>((DataComponentPredicate.Type<T>)e.getKey(), e.getValue());
      }
   }

   interface Type<T extends DataComponentPredicate> {
      Codec<DataComponentPredicate.Type<?>> CODEC = Codec.either(
            BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec(), BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec()
         )
         .xmap(DataComponentPredicate.Type::copyOrCreateType, DataComponentPredicate.Type::unpackType);
      StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Type<?>> STREAM_CODEC = ByteBufCodecs.either(
            ByteBufCodecs.registry(Registries.DATA_COMPONENT_PREDICATE_TYPE), ByteBufCodecs.registry(Registries.DATA_COMPONENT_TYPE)
         )
         .map(DataComponentPredicate.Type::copyOrCreateType, DataComponentPredicate.Type::unpackType);

      private static <T extends DataComponentPredicate.Type<?>> Either<T, DataComponentType<?>> unpackType(final T type) {
         return type instanceof DataComponentPredicate.AnyValueType anyCheck ? Either.right(anyCheck.componentType()) : Either.left(type);
      }

      private static DataComponentPredicate.Type<?> copyOrCreateType(final Either<DataComponentPredicate.Type<?>, DataComponentType<?>> concreteTypeOrComponent) {
         return (DataComponentPredicate.Type<?>)concreteTypeOrComponent.map(concrete -> concrete, DataComponentPredicate.AnyValueType::create);
      }

      Codec<T> codec();

      MapCodec<DataComponentPredicate.Single<T>> wrappedCodec();

      StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec();
   }

   abstract class TypeBase<T extends DataComponentPredicate> implements DataComponentPredicate.Type<T> {
      private final Codec<T> codec;
      private final MapCodec<DataComponentPredicate.Single<T>> wrappedCodec;
      private final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec;

      public TypeBase(final Codec<T> codec) {
         this.codec = codec;
         this.wrappedCodec = DataComponentPredicate.Single.wrapCodec(this, codec);
         this.singleStreamCodec = ByteBufCodecs.<DataComponentPredicate>fromCodecWithRegistries(codec)
            .map(v -> new DataComponentPredicate.Single<>(this, (T)v), DataComponentPredicate.Single::predicate);
      }

      @Override
      public Codec<T> codec() {
         return this.codec;
      }

      @Override
      public MapCodec<DataComponentPredicate.Single<T>> wrappedCodec() {
         return this.wrappedCodec;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec() {
         return this.singleStreamCodec;
      }
   }
}
