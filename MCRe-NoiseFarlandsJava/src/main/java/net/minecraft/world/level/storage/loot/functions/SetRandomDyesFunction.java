package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetRandomDyesFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetRandomDyesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> commonFields(i).and(NumberProviders.CODEC.fieldOf("number_of_dyes").forGetter(f -> f.numberOfDyes)).apply(i, SetRandomDyesFunction::new)
    );
    private final NumberProvider numberOfDyes;

    private SetRandomDyesFunction(final List<LootItemCondition> predicates, final NumberProvider numberOfDyes) {
        super(predicates);
        this.numberOfDyes = numberOfDyes;
    }

    @Override
    public MapCodec<SetRandomDyesFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(final ItemStack itemStack, final LootContext context) {
        RandomSource random = context.getRandom();
        int rolls = this.numberOfDyes.getInt(context);
        if (rolls <= 0) {
            return itemStack;
        }

        List<DyeColor> dyes = new ArrayList<>(rolls);

        for (int i = 0; i < rolls; i++) {
            dyes.add(Util.getRandom(DyeColor.VALUES, random));
        }

        return DyedItemColor.applyDyes(itemStack, dyes);
    }

    public static LootItemConditionalFunction.Builder<?> withCount(final NumberProvider numberOfDyes) {
        return simpleBuilder(conditions -> new SetRandomDyesFunction(conditions, numberOfDyes));
    }
}