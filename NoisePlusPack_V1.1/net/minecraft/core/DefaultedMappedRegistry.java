package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
   private final Identifier defaultKey;
   private Holder.Reference<T> defaultValue;

   public DefaultedMappedRegistry(String p_260196_, ResourceKey<? extends Registry<T>> p_259440_, Lifecycle p_260260_, boolean p_259808_) {
      super(p_259440_, p_260260_, p_259808_);
      this.defaultKey = Identifier.parse(p_260196_);
   }

   @Override
   public Holder.Reference<T> register(ResourceKey<T> p_332872_, T p_328327_, RegistrationInfo p_331941_) {
      Holder.Reference<T> reference = super.register(p_332872_, p_328327_, p_331941_);
      if (this.defaultKey.equals(p_332872_.identifier())) {
         this.defaultValue = reference;
      }

      return reference;
   }

   @Override
   public int getId(@Nullable T p_260033_) {
      int i = super.getId(p_260033_);
      return i == -1 ? super.getId(this.defaultValue.value()) : i;
   }

   @Override
   public Identifier getKey(T p_259233_) {
      Identifier identifier = super.getKey(p_259233_);
      return identifier == null ? this.defaultKey : identifier;
   }

   @Override
   public T getValue(@Nullable Identifier p_452628_) {
      T t = super.getValue(p_452628_);
      return t == null ? this.defaultValue.value() : t;
   }

   @Override
   public Optional<T> getOptional(@Nullable Identifier p_455803_) {
      return Optional.ofNullable(super.getValue(p_455803_));
   }

   @Override
   public Optional<Holder.Reference<T>> getAny() {
      return Optional.ofNullable(this.defaultValue);
   }

   @Override
   public T byId(int p_259534_) {
      T t = super.byId(p_259534_);
      return t == null ? this.defaultValue.value() : t;
   }

   @Override
   public Optional<Holder.Reference<T>> getRandom(RandomSource p_260255_) {
      return super.getRandom(p_260255_).or(() -> Optional.of(this.defaultValue));
   }

   @Override
   public Identifier getDefaultKey() {
      return this.defaultKey;
   }
}
