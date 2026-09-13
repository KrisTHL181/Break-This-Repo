package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelLocationUtils {
   @Deprecated
   public static Identifier decorateBlockModelLocation(String p_376541_) {
      return Identifier.withDefaultNamespace("block/" + p_376541_);
   }

   public static Identifier decorateItemModelLocation(String p_376094_) {
      return Identifier.withDefaultNamespace("item/" + p_376094_);
   }

   public static Identifier getModelLocation(Block p_450796_, String p_375834_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_450796_);
      return identifier.withPath(p_375700_ -> "block/" + p_375700_ + p_375834_);
   }

   public static Identifier getModelLocation(Block p_378693_) {
      Identifier identifier = BuiltInRegistries.BLOCK.getKey(p_378693_);
      return identifier.withPrefix("block/");
   }

   public static Identifier getModelLocation(Item p_452444_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_452444_);
      return identifier.withPrefix("item/");
   }

   public static Identifier getModelLocation(Item p_378416_, String p_459374_) {
      Identifier identifier = BuiltInRegistries.ITEM.getKey(p_378416_);
      return identifier.withPath(p_376725_ -> "item/" + p_376725_ + p_459374_);
   }
}
