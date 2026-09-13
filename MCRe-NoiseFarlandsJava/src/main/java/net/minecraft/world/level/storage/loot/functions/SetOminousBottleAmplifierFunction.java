package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetOminousBottleAmplifierFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetOminousBottleAmplifierFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> commonFields(i).and(NumberProviders.CODEC.fieldOf("amplifier").forGetter(f -> f.amplifier)).apply(i, SetOminousBottleAmplifierFunction::new)
    );
    private final NumberProvider amplifier;

    private SetOminousBottleAmplifierFunction(final List<LootItemCondition> predicates, final NumberProvider amplifier) {
        super(predicates);
        this.amplifier = amplifier;
    }

    @Override
    public void validate(final ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "amplifier", this.amplifier);
    }

    @Override
    public MapCodec<SetOminousBottleAmplifierFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(final ItemStack itemStack, final LootContext context) {
        int amplifierValue = Mth.clamp(this.amplifier.getInt(context), 0, 4);
        itemStack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(amplifierValue));
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setAmplifier(final NumberProvider amplifier) {
        return simpleBuilder(conditions -> new SetOminousBottleAmplifierFunction(conditions, amplifier));
    }
}