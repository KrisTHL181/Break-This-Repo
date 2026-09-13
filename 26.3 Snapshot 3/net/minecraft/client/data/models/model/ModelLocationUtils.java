package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModelLocationUtils {
   @Deprecated
   public static Identifier decorateBlockModelLocation(final String id) {
      return Identifier.withDefaultNamespace("block/" + id);
   }

   public static Identifier decorateItemModelLocation(final String id) {
      return Identifier.withDefaultNamespace("item/" + id);
   }

   public static Identifier getModelLocation(final Block block, final String suffix) {
      Identifier key = BuiltInRegistries.BLOCK.getKey(block);
      return key.withPath(path -> "block/" + path + suffix);
   }

   public static Identifier getModelLocation(final Block block) {
      Identifier key = BuiltInRegistries.BLOCK.getKey(block);
      return key.withPrefix("block/");
   }

   public static Identifier getModelLocation(final Item item) {
      Identifier key = BuiltInRegistries.ITEM.getKey(item);
      return key.withPrefix("item/");
   }

   public static Identifier getModelLocation(final Item item, final String suffix) {
      Identifier key = BuiltInRegistries.ITEM.getKey(item);
      return key.withPath(path -> "item/" + path + suffix);
   }
}
