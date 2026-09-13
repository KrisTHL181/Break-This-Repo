package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jspecify.annotations.Nullable;

public class VillagerTrade implements Validatable {
    public static final Codec<VillagerTrade> CODEC = RecordCodecBuilder.<VillagerTrade>create(
            i -> i.group(
                    TradeCost.CODEC.fieldOf("wants").forGetter(villagerTrade -> villagerTrade.wants),
                    TradeCost.CODEC.optionalFieldOf("additional_wants").forGetter(villagerTrade -> villagerTrade.additionalWants),
                    ItemStackTemplate.CODEC.fieldOf("gives").forGetter(villagerTrade -> villagerTrade.gives),
                    NumberProviders.CODEC.lenientOptionalFieldOf("max_uses", ConstantValue.exactly(4.0F)).forGetter(villagerTrade -> villagerTrade.maxUses),
                    NumberProviders.CODEC
                        .lenientOptionalFieldOf("reputation_discount", ConstantValue.exactly(0.0F))
                        .forGetter(villagerTrade -> villagerTrade.reputationDiscount),
                    NumberProviders.CODEC.lenientOptionalFieldOf("xp", ConstantValue.exactly(1.0F)).forGetter(villagerTrade -> villagerTrade.xp),
                    LootItemCondition.DIRECT_CODEC.optionalFieldOf("merchant_predicate").forGetter(villagerTrade -> villagerTrade.merchantPredicate),
                    LootItemFunctions.ROOT_CODEC
                        .listOf()
                        .optionalFieldOf("given_item_modifiers", List.of())
                        .forGetter(villagerTrade -> villagerTrade.givenItemModifiers),
                    RegistryCodecs.homogeneousList(Registries.ENCHANTMENT)
                        .optionalFieldOf("double_trade_price_enchantments")
                        .forGetter(villagerTrade -> villagerTrade.doubleTradePriceEnchantments)
                )
                .apply(i, VillagerTrade::new)
        )
        .validate(Validatable.validatorForContext(LootContextParamSets.VILLAGER_TRADE));
    private final TradeCost wants;
    private final Optional<TradeCost> additionalWants;
    private final ItemStackTemplate gives;
    private final Optional<LootItemCondition> merchantPredicate;
    private final List<LootItemFunction> givenItemModifiers;
    private final NumberProvider maxUses;
    private final NumberProvider reputationDiscount;
    private final NumberProvider xp;
    private final Optional<HolderSet<Enchantment>> doubleTradePriceEnchantments;

    private VillagerTrade(
        final TradeCost wants,
        final Optional<TradeCost> additionalWants,
        final ItemStackTemplate gives,
        final NumberProvider maxUses,
        final NumberProvider reputationDiscount,
        final NumberProvider xp,
        final Optional<LootItemCondition> merchantPredicate,
        final List<LootItemFunction> givenItemModifiers,
        final Optional<HolderSet<Enchantment>> doubleTradePriceEnchantments
    ) {
        this.wants = wants;
        this.additionalWants = additionalWants;
        this.gives = gives;
        this.maxUses = maxUses;
        this.reputationDiscount = reputationDiscount;
        this.xp = xp;
        this.merchantPredicate = merchantPredicate;
        this.givenItemModifiers = givenItemModifiers;
        this.doubleTradePriceEnchantments = doubleTradePriceEnchantments;
    }

    public VillagerTrade(
        final TradeCost wants,
        final Optional<TradeCost> additionalWants,
        final ItemStackTemplate gives,
        final int maxUses,
        final int xp,
        final float reputationDiscount,
        final Optional<LootItemCondition> merchantPredicate,
        final List<LootItemFunction> givenItemModifiers,
        final HolderSet<Enchantment> doubleTradePriceEnchantments
    ) {
        this(
            wants,
            additionalWants,
            gives,
            ConstantValue.exactly(maxUses),
            ConstantValue.exactly(reputationDiscount),
            ConstantValue.exactly(xp),
            merchantPredicate,
            givenItemModifiers,
            Optional.of(doubleTradePriceEnchantments)
        );
    }

    public VillagerTrade(
        final TradeCost wants,
        final Optional<TradeCost> additionalWants,
        final ItemStackTemplate gives,
        final int maxUses,
        final int xp,
        final float reputationDiscount,
        final Optional<LootItemCondition> merchantPredicate,
        final List<LootItemFunction> givenItemModifiers
    ) {
        this(
            wants,
            additionalWants,
            gives,
            ConstantValue.exactly(maxUses),
            ConstantValue.exactly(reputationDiscount),
            ConstantValue.exactly(xp),
            merchantPredicate,
            givenItemModifiers,
            Optional.empty()
        );
    }

    public VillagerTrade(
        final TradeCost wants,
        final ItemStackTemplate gives,
        final int maxUses,
        final int xp,
        final float reputationDiscount,
        final Optional<LootItemCondition> merchantPredicate,
        final List<LootItemFunction> givenItemModifiers
    ) {
        this(
            wants,
            Optional.empty(),
            gives,
            ConstantValue.exactly(maxUses),
            ConstantValue.exactly(reputationDiscount),
            ConstantValue.exactly(xp),
            merchantPredicate,
            givenItemModifiers,
            Optional.empty()
        );
    }

    @Override
    public void validate(final ValidationContext context) {
        Validatable.validate(context, "wants", this.wants);
        Validatable.validate(context, "additional_wants", this.additionalWants);
        Validatable.validate(context, "max_uses", this.maxUses);
        Validatable.validate(context, "reputation_discount", this.reputationDiscount);
        Validatable.validate(context, "xp", this.xp);
        Validatable.validate(context, "merchant_predicate", this.merchantPredicate);
        Validatable.validate(context, "given_item_modifiers", this.givenItemModifiers);
    }

    public @Nullable MerchantOffer getOffer(final LootContext lootContext) {
        if (this.merchantPredicate.isPresent() && !this.merchantPredicate.get().test(lootContext)) {
            return null;
        }

        ItemStack result = this.gives.create();
        int additionalCost = 0;

        for (LootItemFunction outputItemModifier : this.givenItemModifiers) {
            result = outputItemModifier.apply(result, lootContext);
            if (result.isEmpty()) {
                return null;
            }
        }

        Integer additionalTradeCost = result.remove(DataComponents.ADDITIONAL_TRADE_COST);
        if (additionalTradeCost != null) {
            additionalCost += additionalTradeCost;
        }

        if (this.doubleTradePriceEnchantments.isPresent()) {
            HolderSet<Enchantment> enchantments = this.doubleTradePriceEnchantments.get();
            ItemEnchantments itemEnchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (itemEnchantments != null && itemEnchantments.keySet().stream().anyMatch(enchantments::contains)) {
                additionalCost *= 2;
            }
        }

        ItemCost itemCost = this.wants.toItemCost(lootContext, additionalCost);
        if (itemCost.count() < 1) {
            return null;
        }

        Optional<ItemCost> additionalItemCost = this.additionalWants.map(tradeCost -> tradeCost.toItemCost(lootContext, 0));
        return additionalItemCost.isPresent() && additionalItemCost.get().count() < 1
            ? null
            : new MerchantOffer(
                itemCost,
                additionalItemCost,
                result,
                Math.max(this.maxUses.getInt(lootContext), 1),
                Math.max(this.xp.getInt(lootContext), 0),
                Math.max(this.reputationDiscount.getFloat(lootContext), 0.0F)
            );
    }
}