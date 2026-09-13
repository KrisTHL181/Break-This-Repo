package net.minecraft.world.item;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
   private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
   private final EntityType<? extends HangingEntity> type;

   public HangingEntityItem(EntityType<? extends HangingEntity> p_41324_, Item.Properties p_41325_) {
      super(p_41325_);
      this.type = p_41324_;
   }

   @Override
   public InteractionResult useOn(UseOnContext p_41331_) {
      BlockPos blockpos = p_41331_.getClickedPos();
      Direction direction = p_41331_.getClickedFace();
      BlockPos blockpos1 = blockpos.relative(direction);
      Player player = p_41331_.getPlayer();
      ItemStack itemstack = p_41331_.getItemInHand();
      if (player != null && !this.mayPlace(player, direction, itemstack, blockpos1)) {
         return InteractionResult.FAIL;
      }

      Level level = p_41331_.getLevel();
      HangingEntity hangingentity;
      if (this.type == EntityType.PAINTING) {
         Optional<Painting> optional = Painting.create(level, blockpos1, direction);
         if (optional.isEmpty()) {
            return InteractionResult.CONSUME;
         }

         hangingentity = optional.get();
      } else if (this.type == EntityType.ITEM_FRAME) {
         hangingentity = new ItemFrame(level, blockpos1, direction);
      } else {
         if (this.type != EntityType.GLOW_ITEM_FRAME) {
            return InteractionResult.SUCCESS;
         }

         hangingentity = new GlowItemFrame(level, blockpos1, direction);
      }

      EntityType.<HangingEntity>createDefaultStackConfig(level, itemstack, player).accept(hangingentity);
      if (hangingentity.survives()) {
         if (!level.isClientSide()) {
            hangingentity.playPlacementSound();
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hangingentity.position());
            level.addFreshEntity(hangingentity);
         }

         itemstack.shrink(1);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   protected boolean mayPlace(Player p_41326_, Direction p_41327_, ItemStack p_41328_, BlockPos p_41329_) {
      return !p_41327_.getAxis().isVertical() && p_41326_.mayUseItemAt(p_41329_, p_41327_, p_41328_);
   }

   @Override
   public void appendHoverText(
      ItemStack p_270235_, Item.TooltipContext p_336046_, TooltipDisplay p_394274_, Consumer<Component> p_392825_, TooltipFlag p_270170_
   ) {
      if (this.type == EntityType.PAINTING && p_394274_.shows(DataComponents.PAINTING_VARIANT)) {
         Holder<PaintingVariant> holder = p_270235_.get(DataComponents.PAINTING_VARIANT);
         if (holder != null) {
            holder.value().title().ifPresent(p_392825_);
            holder.value().author().ifPresent(p_392825_);
            p_392825_.accept(Component.translatable("painting.dimensions", holder.value().width(), holder.value().height()));
         } else if (p_270170_.isCreative()) {
            p_392825_.accept(TOOLTIP_RANDOM_VARIANT);
         }
      }
   }
}
