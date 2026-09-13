package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class CompassItem extends Item {
   private static final Component LODESTONE_COMPASS_NAME = Component.translatable("item.minecraft.lodestone_compass");

   public CompassItem(Item.Properties p_40718_) {
      super(p_40718_);
   }

   @Override
   public boolean isFoil(ItemStack p_40739_) {
      return p_40739_.has(DataComponents.LODESTONE_TRACKER) || super.isFoil(p_40739_);
   }

   @Override
   public void inventoryTick(ItemStack p_40720_, ServerLevel p_392994_, Entity p_40722_, @Nullable EquipmentSlot p_395318_) {
      LodestoneTracker lodestonetracker = p_40720_.get(DataComponents.LODESTONE_TRACKER);
      if (lodestonetracker != null) {
         LodestoneTracker lodestonetracker1 = lodestonetracker.tick(p_392994_);
         if (lodestonetracker1 != lodestonetracker) {
            p_40720_.set(DataComponents.LODESTONE_TRACKER, lodestonetracker1);
         }
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40726_) {
      BlockPos blockpos = p_40726_.getClickedPos();
      Level level = p_40726_.getLevel();
      if (!level.getBlockState(blockpos).is(Blocks.LODESTONE)) {
         return super.useOn(p_40726_);
      }

      level.playSound(null, blockpos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
      Player player = p_40726_.getPlayer();
      ItemStack itemstack = p_40726_.getItemInHand();
      boolean flag = !player.hasInfiniteMaterials() && itemstack.getCount() == 1;
      LodestoneTracker lodestonetracker = new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), blockpos)), true);
      if (flag) {
         itemstack.set(DataComponents.LODESTONE_TRACKER, lodestonetracker);
      } else {
         ItemStack itemstack1 = itemstack.transmuteCopy(Items.COMPASS, 1);
         itemstack.consume(1, player);
         itemstack1.set(DataComponents.LODESTONE_TRACKER, lodestonetracker);
         if (!player.getInventory().add(itemstack1)) {
            player.drop(itemstack1, false);
         }
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public Component getName(ItemStack p_368976_) {
      return p_368976_.has(DataComponents.LODESTONE_TRACKER) ? LODESTONE_COMPASS_NAME : super.getName(p_368976_);
   }
}
