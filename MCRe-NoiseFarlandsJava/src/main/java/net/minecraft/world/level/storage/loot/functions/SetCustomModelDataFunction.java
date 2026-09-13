package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetCustomModelDataFunction extends LootItemConditionalFunction {
    private static final Codec<NumberProvider> COLOR_PROVIDER_CODEC = Codec.withAlternative(
        NumberProviders.CODEC, ExtraCodecs.RGB_COLOR_CODEC, value -> new ConstantValue(value)
    );
    public static final MapCodec<SetCustomModelDataFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> commonFields(i)
            .and(
                i.group(
                    ListOperation.StandAlone.codec(NumberProviders.CODEC, Integer.MAX_VALUE).optionalFieldOf("floats").forGetter(o -> o.floats),
                    ListOperation.StandAlone.codec(Codec.BOOL, Integer.MAX_VALUE).optionalFieldOf("flags").forGetter(o -> o.flags),
                    ListOperation.StandAlone.codec(Codec.STRING, Integer.MAX_VALUE).optionalFieldOf("strings").forGetter(o -> o.strings),
                    ListOperation.StandAlone.codec(COLOR_PROVIDER_CODEC, Integer.MAX_VALUE).optionalFieldOf("colors").forGetter(o -> o.colors)
                )
            )
            .apply(i, SetCustomModelDataFunction::new)
    );
    private final Optional<ListOperation.StandAlone<NumberProvider>> floats;
    private final Optional<ListOperation.StandAlone<Boolean>> flags;
    private final Optional<ListOperation.StandAlone<String>> strings;
    private final Optional<ListOperation.StandAlone<NumberProvider>> colors;

    public SetCustomModelDataFunction(
        final List<LootItemCondition> predicates,
        final Optional<ListOperation.StandAlone<NumberProvider>> floats,
        final Optional<ListOperation.StandAlone<Boolean>> flags,
        final Optional<ListOperation.StandAlone<String>> strings,
        final Optional<ListOperation.StandAlone<NumberProvider>> colors
    ) {
        super(predicates);
        this.floats = floats;
        this.flags = flags;
        this.strings = strings;
        this.colors = colors;
    }

    @Override
    public void validate(final ValidationContext context) {
        super.validate(context);
        this.floats.ifPresent(f -> Validatable.validate(context, "floats", f.value()));
        this.colors.ifPresent(c -> Validatable.validate(context, "colors", c.value()));
    }

    @Override
    public MapCodec<SetCustomModelDataFunction> codec() {
        return MAP_CODEC;
    }

    private static <T> List<T> apply(final Optional<ListOperation.StandAlone<T>> operation, final List<T> current) {
        return operation.<List<T>>map(o -> o.apply(current)).orElse(current);
    }

    private static <T, E> List<E> apply(final Optional<ListOperation.StandAlone<T>> operation, final List<E> current, final Function<T, E> mapper) {
        return operation.<List<E>>map(o -> {
            List<E> transformedReplacement = o.value().stream().map(mapper).toList();
            return o.operation().apply(current, transformedReplacement);
        }).orElse(current);
    }

    @Override
    public ItemStack run(final ItemStack itemStack, final LootContext context) {
        CustomModelData component = itemStack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        itemStack.set(
            DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(
                apply(this.floats, component.floats(), provider -> provider.getFloat(context)),
                apply(this.flags, component.flags()),
                apply(this.strings, component.strings()),
                apply(this.colors, component.colors(), provider -> provider.getInt(context))
            )
        );
        return itemStack;
    }
}
