package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) implements ItemInstance {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<ItemStackTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Item.CODEC.fieldOf("id").forGetter(ItemStackTemplate::item),
                ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStackTemplate::components)
            )
            .apply(i, ItemStackTemplate::new)
    );
    public static final Codec<ItemStackTemplate> CODEC = Codec.withAlternative(MAP_CODEC.codec(), Item.CODEC, item -> new ItemStackTemplate(item.value()));
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC = StreamCodec.composite(
        Item.STREAM_CODEC,
        ItemStackTemplate::item,
        ByteBufCodecs.VAR_INT,
        ItemStackTemplate::count,
        DataComponentPatch.STREAM_CODEC,
        ItemStackTemplate::components,
        ItemStackTemplate::new
    );

    public ItemStackTemplate(final Item item) {
        this(item.builtInRegistryHolder(), 1, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(final Item item, final int count) {
        this(item.builtInRegistryHolder(), count, DataComponentPatch.EMPTY);
    }

    public ItemStackTemplate(final Item item, final DataComponentPatch patch) {
        this(item.builtInRegistryHolder(), 1, patch);
    }

    public ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) {
        if (count != 0 && !item.is(Items.AIR.builtInRegistryHolder())) {
            this.item = item;
            this.count = count;
            this.components = components;
        } else {
            throw new IllegalStateException("Item must be non-empty");
        }
    }

    public static ItemStackTemplate fromNonEmptyStack(final ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            throw new IllegalStateException("Stack must be non-empty");
        } else {
            return fromStack(itemStack);
        }
    }

    public static ItemStackTemplate fromStack(final ItemStack itemStack) {
        return new ItemStackTemplate(itemStack.typeHolder(), itemStack.getCount(), itemStack.getComponentsPatch());
    }

    public ItemStackTemplate withCount(final int count) {
        return this.count == count ? this : new ItemStackTemplate(this.item, count, this.components);
    }

    public ItemStack create() {
        return this.validate(new ItemStack(this.item, this.count, this.components));
    }

    private ItemStack validate(final ItemStack result) {
        Optional<Error<ItemStack>> error = ItemStack.validateStrict(result).error();
        if (error.isPresent()) {
            LOGGER.warn("Can't create item stack with properties {}, error: {}", this, error.get().message());
            return ItemStack.EMPTY;
        } else {
            return result;
        }
    }

    public ItemStack apply(final DataComponentPatch additionalPatch) {
        return this.apply(this.count, additionalPatch);
    }

    public ItemStack apply(final int count, final DataComponentPatch additionalPatch) {
        ItemStack result = new ItemStack(this.item, count, additionalPatch);
        result.applyComponents(this.components);
        return this.validate(result);
    }

    @Override
    public Holder<Item> typeHolder() {
        return this.item;
    }

    @Override
    public <T> @Nullable T get(final DataComponentType<? extends T> type) {
        return this.components.get(this.item.components(), type);
    }
}