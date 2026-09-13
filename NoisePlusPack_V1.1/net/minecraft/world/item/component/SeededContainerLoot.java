package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootTable;

public record SeededContainerLoot(ResourceKey<LootTable> lootTable, long seed) implements TooltipProvider {
   private static final Component UNKNOWN_CONTENTS = Component.translatable("item.container.loot_table.unknown");
   public static final Codec<SeededContainerLoot> CODEC = RecordCodecBuilder.create(
      p_390822_ -> p_390822_.group(
            LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(SeededContainerLoot::lootTable),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(SeededContainerLoot::seed)
         )
         .apply(p_390822_, SeededContainerLoot::new)
   );

   @Override
   public void addToTooltip(Item.TooltipContext p_393479_, Consumer<Component> p_391736_, TooltipFlag p_396577_, DataComponentGetter p_397801_) {
      p_391736_.accept(UNKNOWN_CONTENTS);
   }
}
