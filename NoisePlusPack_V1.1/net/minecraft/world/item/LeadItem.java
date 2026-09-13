package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class LeadItem extends Item {
   public LeadItem(Item.Properties p_42828_) {
      super(p_42828_);
   }

   @Override
   public InteractionResult useOn(UseOnContext p_42834_) {
      Level level = p_42834_.getLevel();
      BlockPos blockpos = p_42834_.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (blockstate.is(BlockTags.FENCES)) {
         Player player = p_42834_.getPlayer();
         if (!level.isClientSide() && player != null) {
            return bindPlayerMobs(player, level, blockpos);
         }
      }

      return InteractionResult.PASS;
   }

   public static InteractionResult bindPlayerMobs(Player p_42830_, Level p_42831_, BlockPos p_42832_) {
      LeashFenceKnotEntity leashfenceknotentity = null;
      List<Leashable> list = Leashable.leashableInArea(p_42831_, Vec3.atCenterOf(p_42832_), p_341570_ -> p_341570_.getLeashHolder() == p_42830_);
      boolean flag = false;

      for (Leashable leashable : list) {
         if (leashfenceknotentity == null) {
            leashfenceknotentity = LeashFenceKnotEntity.getOrCreateKnot(p_42831_, p_42832_);
            leashfenceknotentity.playPlacementSound();
         }

         if (leashable.canHaveALeashAttachedTo(leashfenceknotentity)) {
            leashable.setLeashedTo(leashfenceknotentity, true);
            flag = true;
         }
      }

      if (flag) {
         p_42831_.gameEvent(GameEvent.BLOCK_ATTACH, p_42832_, GameEvent.Context.of(p_42830_));
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }
}
