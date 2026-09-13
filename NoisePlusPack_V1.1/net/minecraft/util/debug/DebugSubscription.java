package net.minecraft.util.debug;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class DebugSubscription<T> {
   public static final int DOES_NOT_EXPIRE = 0;
   final @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec;
   private final int expireAfterTicks;

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> p_427894_, int p_425280_) {
      this.valueStreamCodec = p_427894_;
      this.expireAfterTicks = p_425280_;
   }

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> p_424974_) {
      this(p_424974_, 0);
   }

   public DebugSubscription.Update<T> packUpdate(@Nullable T p_423294_) {
      return new DebugSubscription.Update<>(this, Optional.ofNullable(p_423294_));
   }

   public DebugSubscription.Update<T> emptyUpdate() {
      return new DebugSubscription.Update<>(this, Optional.empty());
   }

   public DebugSubscription.Event<T> packEvent(T p_426666_) {
      return new DebugSubscription.Event<>(this, p_426666_);
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.DEBUG_SUBSCRIPTION, this);
   }

   public @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec() {
      return this.valueStreamCodec;
   }

   public int expireAfterTicks() {
      return this.expireAfterTicks;
   }

   public record Event<T>(DebugSubscription<T> subscription, T value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Event<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION)
         .dispatch(DebugSubscription.Event::subscription, DebugSubscription.Event::streamCodec);

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Event<T>> streamCodec(DebugSubscription<T> p_424032_) {
         return Objects.requireNonNull(p_424032_.valueStreamCodec)
            .map(p_428476_ -> new DebugSubscription.Event<>(p_424032_, (T)p_428476_), DebugSubscription.Event::value);
      }
   }

   public record Update<T>(DebugSubscription<T> subscription, Optional<T> value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Update<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION)
         .dispatch(DebugSubscription.Update::subscription, DebugSubscription.Update::streamCodec);

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Update<T>> streamCodec(DebugSubscription<T> p_424223_) {
         return ByteBufCodecs.optional(Objects.requireNonNull(p_424223_.valueStreamCodec))
            .map(p_430676_ -> new DebugSubscription.Update<>(p_424223_, (Optional<T>)p_430676_), DebugSubscription.Update::value);
      }
   }
}
