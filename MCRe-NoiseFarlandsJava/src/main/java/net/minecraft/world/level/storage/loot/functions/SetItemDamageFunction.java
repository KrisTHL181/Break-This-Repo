package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.slf4j.Logger;

public class SetItemDamageFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<SetItemDamageFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
        i -> commonFields(i)
            .and(i.group(NumberProviders.CODEC.fieldOf("damage").forGetter(f -> f.damage), Codec.BOOL.optionalFieldOf("add", false).forGetter(f -> f.add)))
            .apply(i, SetItemDamageFunction::new)
    );
    private final NumberProvider damage;
    private final boolean add;

    private SetItemDamageFunction(final List<LootItemCondition> predicates, final NumberProvider damage, final boolean add) {
        super(predicates);
        this.damage = damage;
        this.add = add;
    }

    @Override
    public MapCodec<SetItemDamageFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(final ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "damage", this.damage);
    }

    @Override
    public ItemStack run(final ItemStack itemStack, final LootContext context) {
        if (itemStack.isDamageableItem()) {
            int maxDamage = itemStack.getMaxDamage();
            float base = this.add ? 1.0F - (float)itemStack.getDamageValue() / maxDamage : 0.0F;
            float pct = 1.0F - Mth.clamp(this.damage.getFloat(context) + base, 0.0F, 1.0F);
            itemStack.setDamageValue(Mth.floor(pct * maxDamage));
        } else {
            LOGGER.warn("Couldn't set damage of loot item {}", itemStack);
        }

        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setDamage(final NumberProvider value) {
        return simpleBuilder(conditions -> new SetItemDamageFunction(conditions, value, false));
    }

    public static LootItemConditionalFunction.Builder<?> setDamage(final NumberProvider value, final boolean add) {
        return simpleBuilder(conditions -> new SetItemDamageFunction(conditions, value, add));
    }
}