package net.minecraft.references;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public record BlockItemId(ResourceKey<Block> block, ResourceKey<Item> item) {
   public static BlockItemId create(final Identifier blockId, final Identifier itemId) {
      return new BlockItemId(ResourceKey.create(Registries.BLOCK, blockId), ResourceKey.create(Registries.ITEM, itemId));
   }

   public static BlockItemId create(final String blockName, final String itemName) {
      return create(Identifier.withDefaultNamespace(blockName), Identifier.withDefaultNamespace(itemName));
   }

   public static BlockItemId create(final String name) {
      Identifier id = Identifier.withDefaultNamespace(name);
      return create(id, id);
   }
}
