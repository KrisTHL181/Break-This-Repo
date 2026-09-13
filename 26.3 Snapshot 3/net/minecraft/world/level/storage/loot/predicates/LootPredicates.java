package net.minecraft.world.level.storage.loot.predicates;

import java.util.List;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class LootPredicates {
   public static final ResourceKey<LootItemCondition> TOOL_CAN_SILK_TOUCH = createKey("tool/can_silk_touch");
   public static final ResourceKey<LootItemCondition> TOOL_CAN_SHEAR = createKey("tool/can_shear");

   private static ResourceKey<LootItemCondition> createKey(final String name) {
      return ResourceKey.create(Registries.PREDICATE, Identifier.withDefaultNamespace(name));
   }

   public static void bootstrap(final BootstrapContext<LootItemCondition> context) {
      HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
      HolderGetter<Item> items = context.lookup(Registries.ITEM);
      context.register(
         TOOL_CAN_SILK_TOUCH,
         MatchTool.toolMatches(
               ItemPredicate.Builder.item()
                  .withComponents(
                     DataComponentMatchers.Builder.components()
                        .partial(
                           DataComponentPredicates.ENCHANTMENTS,
                           EnchantmentsPredicate.enchantments(
                              List.of(new EnchantmentPredicate(enchantments.getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)))
                           )
                        )
                        .build()
                  )
            )
            .build()
      );
      context.register(TOOL_CAN_SHEAR, MatchTool.toolMatches(ItemPredicate.Builder.item().of(items, Items.SHEARS)).build());
   }
}
