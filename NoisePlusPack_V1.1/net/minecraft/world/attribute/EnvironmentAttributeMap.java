package net.minecraft.world.attribute;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import org.jspecify.annotations.Nullable;

public final class EnvironmentAttributeMap {
   public static final EnvironmentAttributeMap EMPTY = new EnvironmentAttributeMap(Map.of());
   public static final Codec<EnvironmentAttributeMap> CODEC = Codec.lazyInitialized(
      () -> Codec.dispatchedMap(EnvironmentAttributes.CODEC, Util.memoize(EnvironmentAttributeMap.Entry::createCodec))
         .xmap(EnvironmentAttributeMap::new, p_459564_ -> p_459564_.entries)
   );
   public static final Codec<EnvironmentAttributeMap> NETWORK_CODEC = CODEC.xmap(
      EnvironmentAttributeMap::filterSyncable, EnvironmentAttributeMap::filterSyncable
   );
   public static final Codec<EnvironmentAttributeMap> CODEC_ONLY_POSITIONAL = CODEC.validate(p_458429_ -> {
      List<EnvironmentAttribute<?>> list = p_458429_.keySet().stream().filter(p_460981_ -> !p_460981_.isPositional()).toList();
      return !list.isEmpty() ? DataResult.error(() -> "The following attributes cannot be positional: " + list) : DataResult.success(p_458429_);
   });
   final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries;

   private static EnvironmentAttributeMap filterSyncable(EnvironmentAttributeMap p_455729_) {
      return new EnvironmentAttributeMap(Map.copyOf(Maps.filterKeys(p_455729_.entries, EnvironmentAttribute::isSyncable)));
   }

   EnvironmentAttributeMap(Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> p_458826_) {
      this.entries = p_458826_;
   }

   public static EnvironmentAttributeMap.Builder builder() {
      return new EnvironmentAttributeMap.Builder();
   }

   public <Value> EnvironmentAttributeMap.@Nullable Entry<Value, ?> get(EnvironmentAttribute<Value> p_454437_) {
      return (EnvironmentAttributeMap.Entry<Value, ?>)this.entries.get(p_454437_);
   }

   public <Value> Value applyModifier(EnvironmentAttribute<Value> p_457253_, Value p_459398_) {
      EnvironmentAttributeMap.Entry<Value, ?> entry = this.get(p_457253_);
      return entry != null ? entry.applyModifier(p_459398_) : p_459398_;
   }

   public boolean contains(EnvironmentAttribute<?> p_455230_) {
      return this.entries.containsKey(p_455230_);
   }

   public Set<EnvironmentAttribute<?>> keySet() {
      return this.entries.keySet();
   }

   @Override
   public boolean equals(Object p_456131_) {
      return p_456131_ == this
         ? true
         : p_456131_ instanceof EnvironmentAttributeMap environmentattributemap && this.entries.equals(environmentattributemap.entries);
   }

   @Override
   public int hashCode() {
      return this.entries.hashCode();
   }

   @Override
   public String toString() {
      return this.entries.toString();
   }

   public static class Builder {
      private final Map<EnvironmentAttribute<?>, EnvironmentAttributeMap.Entry<?, ?>> entries = new HashMap<>();

      Builder() {
      }

      public EnvironmentAttributeMap.Builder putAll(EnvironmentAttributeMap p_456688_) {
         this.entries.putAll(p_456688_.entries);
         return this;
      }

      public <Value, Parameter> EnvironmentAttributeMap.Builder modify(
         EnvironmentAttribute<Value> p_460669_, AttributeModifier<Value, Parameter> p_456757_, Parameter p_451198_
      ) {
         p_460669_.type().checkAllowedModifier(p_456757_);
         this.entries.put(p_460669_, new EnvironmentAttributeMap.Entry<>(p_451198_, p_456757_));
         return this;
      }

      public <Value> EnvironmentAttributeMap.Builder set(EnvironmentAttribute<Value> p_453213_, Value p_453666_) {
         return this.modify(p_453213_, AttributeModifier.override(), p_453666_);
      }

      public EnvironmentAttributeMap build() {
         return this.entries.isEmpty() ? EnvironmentAttributeMap.EMPTY : new EnvironmentAttributeMap(Map.copyOf(this.entries));
      }
   }

   public record Entry<Value, Argument>(Argument argument, AttributeModifier<Value, Argument> modifier) {
      private static <Value> Codec<EnvironmentAttributeMap.Entry<Value, ?>> createCodec(EnvironmentAttribute<Value> p_453007_) {
         Codec<EnvironmentAttributeMap.Entry<Value, ?>> codec = p_453007_.type()
            .modifierCodec()
            .dispatch("modifier", EnvironmentAttributeMap.Entry::modifier, Util.memoize(p_452795_ -> createFullCodec(p_453007_, p_452795_)));
         return Codec.either(p_453007_.valueCodec(), codec)
            .xmap(
               p_456378_ -> (EnvironmentAttributeMap.Entry)p_456378_.map(
                  p_460354_ -> new EnvironmentAttributeMap.Entry<>(p_460354_, AttributeModifier.override()), p_460971_ -> p_460971_
               ),
               p_456225_ -> p_456225_.modifier == AttributeModifier.override() ? Either.left(p_456225_.argument()) : Either.right(p_456225_)
            );
      }

      private static <Value, Argument> MapCodec<EnvironmentAttributeMap.Entry<Value, Argument>> createFullCodec(
         EnvironmentAttribute<Value> p_454522_, AttributeModifier<Value, Argument> p_454983_
      ) {
         return RecordCodecBuilder.mapCodec(
            p_450346_ -> p_450346_.group(p_454983_.argumentCodec(p_454522_).fieldOf("argument").forGetter(EnvironmentAttributeMap.Entry::argument))
               .apply(p_450346_, p_452710_ -> new EnvironmentAttributeMap.Entry<>(p_452710_, p_454983_))
         );
      }

      public Value applyModifier(Value p_453869_) {
         return this.modifier.apply(p_453869_, this.argument);
      }
   }
}
