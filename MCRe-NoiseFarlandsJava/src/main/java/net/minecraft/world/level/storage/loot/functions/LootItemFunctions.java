package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class LootItemFunctions {
    public static final BiFunction<ItemStack, LootContext, ItemStack> IDENTITY = (stack, context) -> stack;
    public static final Codec<LootItemFunction> TYPED_CODEC = BuiltInRegistries.LOOT_FUNCTION_TYPE
        .byNameCodec()
        .dispatch("function", LootItemFunction::codec, c -> c);
    public static final Codec<LootItemFunction> ROOT_CODEC = Codec.lazyInitialized(() -> Codec.withAlternative(TYPED_CODEC, SequenceFunction.INLINE_CODEC));
    public static final Codec<Holder<LootItemFunction>> CODEC = RegistryFileCodec.create(Registries.ITEM_MODIFIER, ROOT_CODEC);

    public static MapCodec<? extends LootItemFunction> bootstrap(final Registry<MapCodec<? extends LootItemFunction>> registry) {
        Registry.register(registry, "set_count", SetItemCountFunction.MAP_CODEC);
        Registry.register(registry, "set_item", SetItemFunction.MAP_CODEC);
        Registry.register(registry, "enchant_with_levels", EnchantWithLevelsFunction.MAP_CODEC);
        Registry.register(registry, "enchant_randomly", EnchantRandomlyFunction.MAP_CODEC);
        Registry.register(registry, "set_enchantments", SetEnchantmentsFunction.MAP_CODEC);
        Registry.register(registry, "set_custom_data", SetCustomDataFunction.MAP_CODEC);
        Registry.register(registry, "set_components", SetComponentsFunction.MAP_CODEC);
        Registry.register(registry, "furnace_smelt", SmeltItemFunction.MAP_CODEC);
        Registry.register(registry, "enchanted_count_increase", EnchantedCountIncreaseFunction.MAP_CODEC);
        Registry.register(registry, "set_damage", SetItemDamageFunction.MAP_CODEC);
        Registry.register(registry, "set_attributes", SetAttributesFunction.MAP_CODEC);
        Registry.register(registry, "set_name", SetNameFunction.MAP_CODEC);
        Registry.register(registry, "exploration_map", ExplorationMapFunction.MAP_CODEC);
        Registry.register(registry, "set_stew_effect", SetStewEffectFunction.MAP_CODEC);
        Registry.register(registry, "copy_name", CopyNameFunction.MAP_CODEC);
        Registry.register(registry, "set_contents", SetContainerContents.MAP_CODEC);
        Registry.register(registry, "modify_contents", ModifyContainerContents.MAP_CODEC);
        Registry.register(registry, "filtered", FilteredFunction.MAP_CODEC);
        Registry.register(registry, "limit_count", LimitCount.MAP_CODEC);
        Registry.register(registry, "apply_bonus", ApplyBonusCount.MAP_CODEC);
        Registry.register(registry, "set_loot_table", SetContainerLootTable.MAP_CODEC);
        Registry.register(registry, "explosion_decay", ApplyExplosionDecay.MAP_CODEC);
        Registry.register(registry, "set_lore", SetLoreFunction.MAP_CODEC);
        Registry.register(registry, "fill_player_head", FillPlayerHead.MAP_CODEC);
        Registry.register(registry, "copy_custom_data", CopyCustomDataFunction.MAP_CODEC);
        Registry.register(registry, "copy_state", CopyBlockState.MAP_CODEC);
        Registry.register(registry, "set_banner_pattern", SetBannerPatternFunction.MAP_CODEC);
        Registry.register(registry, "set_potion", SetPotionFunction.MAP_CODEC);
        Registry.register(registry, "set_random_dyes", SetRandomDyesFunction.MAP_CODEC);
        Registry.register(registry, "set_random_potion", SetRandomPotionFunction.MAP_CODEC);
        Registry.register(registry, "set_instrument", SetInstrumentFunction.MAP_CODEC);
        Registry.register(registry, "reference", FunctionReference.MAP_CODEC);
        Registry.register(registry, "sequence", SequenceFunction.MAP_CODEC);
        Registry.register(registry, "copy_components", CopyComponentsFunction.MAP_CODEC);
        Registry.register(registry, "set_fireworks", SetFireworksFunction.MAP_CODEC);
        Registry.register(registry, "set_firework_explosion", SetFireworkExplosionFunction.MAP_CODEC);
        Registry.register(registry, "set_book_cover", SetBookCoverFunction.MAP_CODEC);
        Registry.register(registry, "set_written_book_pages", SetWrittenBookPagesFunction.MAP_CODEC);
        Registry.register(registry, "set_writable_book_pages", SetWritableBookPagesFunction.MAP_CODEC);
        Registry.register(registry, "toggle_tooltips", ToggleTooltips.MAP_CODEC);
        Registry.register(registry, "set_ominous_bottle_amplifier", SetOminousBottleAmplifierFunction.MAP_CODEC);
        Registry.register(registry, "set_custom_model_data", SetCustomModelDataFunction.MAP_CODEC);
        return Registry.register(registry, "discard", DiscardItem.MAP_CODEC);
    }

    public static BiFunction<ItemStack, LootContext, ItemStack> compose(final List<? extends BiFunction<ItemStack, LootContext, ItemStack>> functions) {
        List<BiFunction<ItemStack, LootContext, ItemStack>> terms = List.copyOf(functions);

        return switch (terms.size()) {
            case 0 -> IDENTITY;
            case 1 -> (BiFunction)terms.get(0);
            case 2 -> {
                BiFunction<ItemStack, LootContext, ItemStack> first = terms.get(0);
                BiFunction<ItemStack, LootContext, ItemStack> second = terms.get(1);
                yield (itemStack, context) -> second.apply(first.apply(itemStack, context), context);
            }
            default -> (itemStack, context) -> {
                for (BiFunction<ItemStack, LootContext, ItemStack> function : terms) {
                    itemStack = function.apply(itemStack, context);
                }

                return itemStack;
            };
        };
    }
}