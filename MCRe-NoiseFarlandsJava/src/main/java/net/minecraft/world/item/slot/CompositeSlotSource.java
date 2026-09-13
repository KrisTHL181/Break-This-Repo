package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeSlotSource implements SlotSource {
    protected final List<SlotSource> terms;
    private final Function<LootContext, SlotCollection> compositeSlotSource;

    protected CompositeSlotSource(final List<SlotSource> terms) {
        this.terms = terms;
        this.compositeSlotSource = SlotSources.group(terms);
    }

    protected static <T extends CompositeSlotSource> MapCodec<T> createCodec(final Function<List<SlotSource>, T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group(SlotSources.CODEC.listOf().fieldOf("terms").forGetter(t -> t.terms)).apply(i, factory));
    }

    protected static <T extends CompositeSlotSource> Codec<T> createInlineCodec(final Function<List<SlotSource>, T> factory) {
        return SlotSources.CODEC.listOf().xmap(factory, t -> t.terms);
    }

    @Override
    public abstract MapCodec<? extends CompositeSlotSource> codec();

    @Override
    public SlotCollection provide(final LootContext context) {
        return this.compositeSlotSource.apply(context);
    }

    @Override
    public void validate(final ValidationContext context) {
        SlotSource.super.validate(context);
        Validatable.validate(context, "terms", this.terms);
    }
}