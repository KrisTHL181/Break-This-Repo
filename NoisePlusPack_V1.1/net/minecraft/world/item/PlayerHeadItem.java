package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;

public class PlayerHeadItem extends StandingAndWallBlockItem {
   public PlayerHeadItem(Block p_42971_, Block p_42972_, Item.Properties p_42973_) {
      super(p_42971_, p_42972_, Direction.DOWN, p_42973_);
   }

   @Override
   public Component getName(ItemStack p_42977_) {
      ResolvableProfile resolvableprofile = p_42977_.get(DataComponents.PROFILE);
      return resolvableprofile != null && resolvableprofile.name().isPresent()
         ? Component.translatable(this.descriptionId + ".named", resolvableprofile.name().get())
         : super.getName(p_42977_);
   }
}
