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
         p_392899_ -> p_392899_.stream().collect(Collectors.toMap(DataComponentPredicate.Single::type, DataComponentPredicate.Single::predicate)),
         p_393256_ -> p_393256_.entrySet().stream().map(DataComponentPredicate.Single::fromEntry).toList()
      );

   static MapCodec<DataComponentPredicate.Single<?>> singleCodec(String p_392018_) {
      return DataComponentPredicate.Type.CODEC.dispatchMap(p_392018_, DataComponentPredicate.Single::type, DataComponentPredicate.Type::wrappedCodec);
   }

   boolean matches(DataComponentGetter var1);

   final class AnyValueType extends DataComponentPredicate.TypeBase<AnyValue> {
      private final AnyValue predicate;

      public AnyValueType(AnyValue p_450984_) {
         super(MapCodec.unitCodec(p_450984_));
         this.predicate = p_450984_;
      }

      public AnyValue predicate() {
         return this.predicate;
      }

      public DataComponentType<?> componentType() {
         return this.predicate.type();
      }

      public static DataComponentPredicate.AnyValueType create(DataComponentType<?> p_452132_) {
         return new DataComponentPredicate.AnyValueType(new AnyValue(p_452132_));
      }
   }

   final class ConcreteType<T extends DataComponentPredicate> extends DataComponentPredicate.TypeBase<T> {
      public ConcreteType(Codec<T> p_454461_) {
         super(p_454461_);
      }
   }

   record Single<T extends DataComponentPredicate>(DataComponentPredicate.Type<T> type, T predicate) {
      static <T extends DataComponentPredicate> MapCodec<DataComponentPredicate.Single<T>> wrapCodec(
         DataComponentPredicate.Type<T> p_450567_, Codec<T> p_451791_
      ) {
         return RecordCodecBuilder.mapCodec(
            p_457431_ -> p_457431_.group(p_451791_.fieldOf("value").forGetter(DataComponentPredicate.Single::predicate))
               .apply(p_457431_, p_459775_ -> new DataComponentPredicate.Single<>(p_450567_, (T)p_459775_))
         );
      }

      private static <T extends DataComponentPredicate> DataComponentPredicate.Single<T> fromEntry(Entry<DataComponentPredicate.Type<?>, T> p_394497_) {
         return new DataComponentPredicate.Single<>((DataComponentPredicate.Type<T>)p_394497_.getKey(), p_394497_.getValue());
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

      private static <T extends DataComponentPredicate.Type<?>> Either<T, DataComponentType<?>> unpackType(T p_455554_) {
         return p_455554_ instanceof DataComponentPredicate.AnyValueType datacomponentpredicate$anyvaluetype
            ? Either.right(datacomponentpredicate$anyvaluetype.componentType())
            : Either.left(p_455554_);
      }

      private static DataComponentPredicate.Type<?> copyOrCreateType(Either<DataComponentPredicate.Type<?>, DataComponentType<?>> p_450160_) {
         return (DataComponentPredicate.Type<?>)p_450160_.map(p_448615_ -> p_448615_, DataComponentPredicate.AnyValueType::create);
      }

      Codec<T> codec();

      MapCodec<DataComponentPredicate.Single<T>> wrappedCodec();

      StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec();
   }

   abstract class TypeBase<T extends DataComponentPredicate> implements DataComponentPredicate.Type<T> {
      private final Codec<T> codec;
      private final MapCodec<DataComponentPredicate.Single<T>> wrappedCodec;
      private final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec;

      public TypeBase(Codec<T> p_451417_) {
         this.codec = p_451417_;
         this.wrappedCodec = DataComponentPredicate.Single.wrapCodec(this, p_451417_);
         this.singleStreamCodec = ByteBufCodecs.<DataComponentPredicate>fromCodecWithRegistries(p_451417_)
            .map(p_452487_ -> new DataComponentPredicate.Single<>(this, (T)p_452487_), DataComponentPredicate.Single::predicate);
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
