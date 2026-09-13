package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
   public BannerItem(Block p_40534_, Block p_40535_, Item.Properties p_40536_) {
      super(p_40534_, p_40535_, Direction.DOWN, p_40536_);
      Validate.isInstanceOf(AbstractBannerBlock.class, p_40534_);
      Validate.isInstanceOf(AbstractBannerBlock.class, p_40535_);
   }

   public DyeColor getColor() {
      return ((AbstractBannerBlock)this.getBlock()).getColor();
   }
}
