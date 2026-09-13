package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record ToolMaterial(
   TagKey<Block> incorrectBlocksForDrops, int durability, float speed, float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems
) {
   public static final ToolMaterial WOOD = new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 59, 2.0F, 0.0F, 15, ItemTags.WOODEN_TOOL_MATERIALS);
   public static final ToolMaterial STONE = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, 131, 4.0F, 1.0F, 5, ItemTags.STONE_TOOL_MATERIALS);
   public static final ToolMaterial COPPER = new ToolMaterial(BlockTags.INCORRECT_FOR_COPPER_TOOL, 190, 5.0F, 1.0F, 13, ItemTags.COPPER_TOOL_MATERIALS);
   public static final ToolMaterial IRON = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0F, 2.0F, 14, ItemTags.IRON_TOOL_MATERIALS);
   public static final ToolMaterial DIAMOND = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 8.0F, 3.0F, 10, ItemTags.DIAMOND_TOOL_MATERIALS);
   public static final ToolMaterial GOLD = new ToolMaterial(BlockTags.INCORRECT_FOR_GOLD_TOOL, 32, 12.0F, 0.0F, 22, ItemTags.GOLD_TOOL_MATERIALS);
   public static final ToolMaterial NETHERITE = new ToolMaterial(
      BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0F, 4.0F, 15, ItemTags.NETHERITE_TOOL_MATERIALS
   );

   private Item.Properties applyCommonProperties(Item.Properties p_367813_) {
      return p_367813_.durability(this.durability).repairable(this.repairItems).enchantable(this.enchantmentValue);
   }

   public Item.Properties applyToolProperties(Item.Properties p_364561_, TagKey<Block> p_363347_, float p_365658_, float p_368042_, float p_395053_) {
      HolderGetter<Block> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
      return this.applyCommonProperties(p_364561_)
         .component(
            DataComponents.TOOL,
            new Tool(
               List.of(
                  Tool.Rule.deniesDrops(holdergetter.getOrThrow(this.incorrectBlocksForDrops)),
                  Tool.Rule.minesAndDrops(holdergetter.getOrThrow(p_363347_), this.speed)
               ),
               1.0F,
               1,
               true
            )
         )
         .attributes(this.createToolAttributes(p_365658_, p_368042_))
         .component(DataComponents.WEAPON, new Weapon(2, p_395053_));
   }

   private ItemAttributeModifiers createToolAttributes(float p_365132_, float p_360987_) {
      return ItemAttributeModifiers.builder()
         .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, p_365132_ + this.attackDamageBonus, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, p_360987_, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .build();
   }

   public Item.Properties applySwordProperties(Item.Properties p_365507_, float p_363140_, float p_366527_) {
      HolderGetter<Block> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
      return this.applyCommonProperties(p_365507_)
         .component(
            DataComponents.TOOL,
            new Tool(
               List.of(
                  Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
                  Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                  Tool.Rule.overrideSpeed(holdergetter.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)
               ),
               1.0F,
               2,
               false
            )
         )
         .attributes(this.createSwordAttributes(p_363140_, p_366527_))
         .component(DataComponents.WEAPON, new Weapon(1));
   }

   private ItemAttributeModifiers createSwordAttributes(float p_367704_, float p_369087_) {
      return ItemAttributeModifiers.builder()
         .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, p_367704_ + this.attackDamageBonus, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, p_369087_, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
         )
         .build();
   }
}
