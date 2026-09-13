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
   private final @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec;
   private final int expireAfterTicks;

   public DebugSubscription(final @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec, final int expireAfterTicks) {
      this.valueStreamCodec = valueStreamCodec;
      this.expireAfterTicks = expireAfterTicks;
   }

   public DebugSubscription(final @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec) {
      this(valueStreamCodec, 0);
   }

   public DebugSubscription.Update<T> packUpdate(final @Nullable T value) {
      return new DebugSubscription.Update<>(this, Optional.ofNullable(value));
   }

   public DebugSubscription.Update<T> emptyUpdate() {
      return new DebugSubscription.Update<>(this, Optional.empty());
   }

   public DebugSubscription.Event<T> packEvent(final T value) {
      return new DebugSubscription.Event<>(this, value);
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

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Event<T>> streamCodec(final DebugSubscription<T> subscription) {
         return Objects.requireNonNull(subscription.valueStreamCodec)
            .map(value -> new DebugSubscription.Event<>(subscription, (T)value), DebugSubscription.Event::value);
      }
   }

   public record Update<T>(DebugSubscription<T> subscription, Optional<T> value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Update<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION)
         .dispatch(DebugSubscription.Update::subscription, DebugSubscription.Update::streamCodec);

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Update<T>> streamCodec(final DebugSubscription<T> subscription) {
         return ByteBufCodecs.optional(Objects.requireNonNull(subscription.valueStreamCodec))
            .map(value -> new DebugSubscription.Update<>(subscription, (Optional<T>)value), DebugSubscription.Update::value);
      }
   }
}
