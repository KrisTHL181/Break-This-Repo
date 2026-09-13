package net.minecraft.world.item.context;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockPlaceContext extends UseOnContext {
   private final BlockPos relativePos;
   protected boolean replaceClicked = true;

   public BlockPlaceContext(final Player player, final InteractionHand hand, final ItemStack itemInHand, final BlockHitResult hitResult) {
      this(player.level(), player, hand, itemInHand, hitResult);
   }

   public BlockPlaceContext(final UseOnContext context) {
      this(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), context.getHitResult());
   }

   protected BlockPlaceContext(
      final Level level, final @Nullable Player player, final InteractionHand hand, final ItemStack itemStackInHand, final BlockHitResult hitResult
   ) {
      super(level, player, hand, itemStackInHand, hitResult);
      this.relativePos = hitResult.getBlockPos().relative(hitResult.getDirection());
      this.replaceClicked = level.getBlockState(hitResult.getBlockPos()).canBeReplaced(this);
   }

   public static BlockPlaceContext at(final BlockPlaceContext context, final BlockPos pos, final Direction direction) {
      return new BlockPlaceContext(
         context.getLevel(),
         context.getPlayer(),
         context.getHand(),
         context.getItemInHand(),
         new BlockHitResult(
            new Vec3(
               pos.getX() + 0.5 + direction.getStepX() * 0.5, pos.getY() + 0.5 + direction.getStepY() * 0.5, pos.getZ() + 0.5 + direction.getStepZ() * 0.5
            ),
            direction,
            pos,
            false
         )
      );
   }

   @Override
   public BlockPos getClickedPos() {
      return this.replaceClicked ? super.getClickedPos() : this.relativePos;
   }

   public boolean canPlace() {
      return this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
   }

   public boolean replacingClickedOnBlock() {
      return this.replaceClicked;
   }

   public Direction getNearestLookingDirection() {
      return Direction.orderedByNearest(this.getPlayer())[0];
   }

   public Direction getNearestLookingVerticalDirection() {
      return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
   }

   public Direction[] getNearestLookingDirections() {
      Direction[] directions = Direction.orderedByNearest(this.getPlayer());
      if (this.replaceClicked) {
         return directions;
      }

      Direction clickedFace = this.getClickedFace();
      int index = 0;

      while (index < directions.length && directions[index] != clickedFace.getOpposite()) {
         index++;
      }

      if (index > 0) {
         System.arraycopy(directions, 0, directions, 1, index);
         directions[0] = clickedFace.getOpposite();
      }

      return directions;
   }
}
