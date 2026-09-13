package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public record BlockItemTagId(TagKey<Block> block, TagKey<Item> item) {
   public static BlockItemTagId create(final Identifier blockId, final Identifier itemId) {
      return new BlockItemTagId(TagKey.create(Registries.BLOCK, blockId), TagKey.create(Registries.ITEM, itemId));
   }

   public static BlockItemTagId create(final String blockName, final String itemName) {
      return create(Identifier.withDefaultNamespace(blockName), Identifier.withDefaultNamespace(itemName));
   }

   public static BlockItemTagId create(final String name) {
      Identifier id = Identifier.withDefaultNamespace(name);
      return create(id, id);
   }
}
