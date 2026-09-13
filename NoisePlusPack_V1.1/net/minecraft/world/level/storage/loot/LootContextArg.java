package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface LootContextArg<R> {
   Codec<LootContextArg<Object>> ENTITY_OR_BLOCK = createArgCodec(
      p_451045_ -> p_451045_.anyOf(LootContext.EntityTarget.values()).anyOf(LootContext.BlockEntityTarget.values())
   );

   @Nullable R get(LootContext var1);

   ContextKey<?> contextParam();

   static <U> LootContextArg<U> cast(LootContextArg<? extends U> p_454137_) {
      return (LootContextArg<U>)p_454137_;
   }

   static <R> Codec<LootContextArg<R>> createArgCodec(UnaryOperator<LootContextArg.ArgCodecBuilder<R>> p_453817_) {
      return p_453817_.apply(new LootContextArg.ArgCodecBuilder<>()).build();
   }

   final class ArgCodecBuilder<R> {
      private final ExtraCodecs.LateBoundIdMapper<String, LootContextArg<R>> sources = new ExtraCodecs.LateBoundIdMapper<>();

      ArgCodecBuilder() {
      }

      public <T> LootContextArg.ArgCodecBuilder<R> anyOf(T[] p_458446_, Function<T, String> p_454035_, Function<T, ? extends LootContextArg<R>> p_457810_) {
         for (T t : p_458446_) {
            this.sources.put(p_454035_.apply(t), (LootContextArg<R>)p_457810_.apply(t));
         }

         return this;
      }

      public <T extends StringRepresentable> LootContextArg.ArgCodecBuilder<R> anyOf(T[] p_459676_, Function<T, ? extends LootContextArg<R>> p_456250_) {
         return this.anyOf(p_459676_, StringRepresentable::getSerializedName, p_456250_);
      }

      public <T extends StringRepresentable & LootContextArg<? extends R>> LootContextArg.ArgCodecBuilder<R> anyOf(T[] p_461000_) {
         return this.anyOf(p_461000_, p_460261_ -> LootContextArg.cast((LootContextArg<? extends R>)p_460261_));
      }

      public LootContextArg.ArgCodecBuilder<R> anyEntity(Function<? super ContextKey<? extends Entity>, ? extends LootContextArg<R>> p_455805_) {
         return this.anyOf(LootContext.EntityTarget.values(), p_451991_ -> p_455805_.apply(p_451991_.contextParam()));
      }

      public LootContextArg.ArgCodecBuilder<R> anyBlockEntity(Function<? super ContextKey<? extends BlockEntity>, ? extends LootContextArg<R>> p_459109_) {
         return this.anyOf(LootContext.BlockEntityTarget.values(), p_459556_ -> p_459109_.apply(p_459556_.contextParam()));
      }

      public LootContextArg.ArgCodecBuilder<R> anyItemStack(Function<? super ContextKey<? extends ItemStack>, ? extends LootContextArg<R>> p_457168_) {
         return this.anyOf(LootContext.ItemStackTarget.values(), p_455639_ -> p_457168_.apply(p_455639_.contextParam()));
      }

      Codec<LootContextArg<R>> build() {
         return this.sources.codec(Codec.STRING);
      }
   }

   interface Getter<T, R> extends LootContextArg<R> {
      @Nullable R get(T var1);

      @Override
      ContextKey<? extends T> contextParam();

      @Override
      default @Nullable R get(LootContext p_454993_) {
         T t = p_454993_.getOptionalParameter((ContextKey<T>)this.contextParam());
         return t != null ? this.get(t) : null;
      }
   }

   interface SimpleGetter<T> extends LootContextArg<T> {
      @Override
      ContextKey<? extends T> contextParam();

      @Override
      default @Nullable T get(LootContext p_454884_) {
         return p_454884_.getOptionalParameter((ContextKey<T>)this.contextParam());
      }
   }
}
