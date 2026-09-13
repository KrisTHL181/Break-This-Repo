package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface LootContextArg<R> {
    Codec<LootContextArg<Object>> ENTITY_OR_BLOCK = createArgCodec(
        builder -> builder.anyOf(LootContext.EntityTarget.values()).anyOf(LootContext.BlockEntityTarget.values())
    );

    @Nullable R get(LootContext context);

    ContextKey<?> contextParam();

    static <U> LootContextArg<U> cast(final LootContextArg<? extends U> original) {
        return (LootContextArg<U>)original;
    }

    static <R> Codec<LootContextArg<R>> createArgCodec(final UnaryOperator<LootContextArg.ArgCodecBuilder<R>> consumer) {
        return consumer.apply(new LootContextArg.ArgCodecBuilder<>()).build();
    }

    final class ArgCodecBuilder<R> {
        private final ExtraCodecs.LateBoundIdMapper<String, LootContextArg<R>> sources = new ExtraCodecs.LateBoundIdMapper<>();

        private ArgCodecBuilder() {
        }

        public <T> LootContextArg.ArgCodecBuilder<R> anyOf(
            final T[] targets, final Function<T, String> nameGetter, final Function<T, ? extends LootContextArg<R>> argFactory
        ) {
            for (T target : targets) {
                this.sources.put(nameGetter.apply(target), (LootContextArg<R>)argFactory.apply(target));
            }

            return this;
        }

        public <T extends StringRepresentable> LootContextArg.ArgCodecBuilder<R> anyOf(
            final T[] targets, final Function<T, ? extends LootContextArg<R>> argFactory
        ) {
            return this.anyOf(targets, StringRepresentable::getSerializedName, argFactory);
        }

        public <T extends StringRepresentable & LootContextArg<? extends R>> LootContextArg.ArgCodecBuilder<R> anyOf(final T[] targets) {
            return this.anyOf(targets, x$0 -> LootContextArg.cast((LootContextArg<? extends R>)x$0));
        }

        public LootContextArg.ArgCodecBuilder<R> anyEntity(final Function<? super ContextKey<? extends Entity>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.EntityTarget.values(), target -> function.apply(target.contextParam()));
        }

        public LootContextArg.ArgCodecBuilder<R> anyBlockEntity(final Function<? super ContextKey<? extends BlockEntity>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.BlockEntityTarget.values(), target -> function.apply(target.contextParam()));
        }

        public LootContextArg.ArgCodecBuilder<R> anyItemStack(final Function<? super ContextKey<? extends ItemInstance>, ? extends LootContextArg<R>> function) {
            return this.anyOf(LootContext.ItemStackTarget.values(), target -> function.apply(target.contextParam()));
        }

        private Codec<LootContextArg<R>> build() {
            return this.sources.codec(Codec.STRING);
        }
    }

    interface Getter<T, R> extends LootContextArg<R> {
        @Nullable R get(T value);

        @Override
        ContextKey<? extends T> contextParam();

        @Override
        default @Nullable R get(final LootContext context) {
            T value = context.getOptionalParameter((ContextKey<T>)this.contextParam());
            return value != null ? this.get(value) : null;
        }
    }

    interface SimpleGetter<T> extends LootContextArg<T> {
        @Override
        ContextKey<? extends T> contextParam();

        @Override
        default @Nullable T get(final LootContext context) {
            return context.getOptionalParameter((ContextKey<T>)this.contextParam());
        }
    }
}