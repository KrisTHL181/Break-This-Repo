package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
   public BannerItem(final Block block, final Block wallBlock, final Item.Properties properties) {
      super(block, wallBlock, Direction.DOWN, properties);
      Validate.isInstanceOf(AbstractBannerBlock.class, block);
      Validate.isInstanceOf(AbstractBannerBlock.class, wallBlock);
   }

   public DyeColor getColor() {
      return ((AbstractBannerBlock)this.getBlock()).getColor();
   }
}
