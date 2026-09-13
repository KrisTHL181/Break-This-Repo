package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
   private static final int DEFAULT_ACCURACY = 6;

   @Override
   public final ItemStack dispense(BlockSource p_123391_, ItemStack p_123392_) {
      ItemStack itemstack = this.execute(p_123391_, p_123392_);
      this.playSound(p_123391_);
      this.playAnimation(p_123391_, p_123391_.state().getValue(DispenserBlock.FACING));
      return itemstack;
   }

   protected ItemStack execute(BlockSource p_301824_, ItemStack p_123386_) {
      Direction direction = p_301824_.state().getValue(DispenserBlock.FACING);
      Position position = DispenserBlock.getDispensePosition(p_301824_);
      ItemStack itemstack = p_123386_.split(1);
      spawnItem(p_301824_.level(), itemstack, 6, direction, position);
      return p_123386_;
   }

   public static void spawnItem(Level p_123379_, ItemStack p_123380_, int p_123381_, Direction p_123382_, Position p_123383_) {
      double d0 = p_123383_.x();
      double d1 = p_123383_.y();
      double d2 = p_123383_.z();
      if (p_123382_.getAxis() == Direction.Axis.Y) {
         d1 -= 0.125;
      } else {
         d1 -= 0.15625;
      }

      ItemEntity itementity = new ItemEntity(p_123379_, d0, d1, d2, p_123380_);
      double d3 = p_123379_.random.nextDouble() * 0.1 + 0.2;
      itementity.setDeltaMovement(
         p_123379_.random.triangle(p_123382_.getStepX() * d3, 0.0172275 * p_123381_),
         p_123379_.random.triangle(0.2, 0.0172275 * p_123381_),
         p_123379_.random.triangle(p_123382_.getStepZ() * d3, 0.0172275 * p_123381_)
      );
      p_123379_.addFreshEntity(itementity);
   }

   protected void playSound(BlockSource p_123384_) {
      playDefaultSound(p_123384_);
   }

   protected void playAnimation(BlockSource p_123388_, Direction p_123389_) {
      playDefaultAnimation(p_123388_, p_123389_);
   }

   private static void playDefaultSound(BlockSource p_343539_) {
      p_343539_.level().levelEvent(1000, p_343539_.pos(), 0);
   }

   private static void playDefaultAnimation(BlockSource p_342920_, Direction p_343526_) {
      p_342920_.level().levelEvent(2000, p_342920_.pos(), p_343526_.get3DDataValue());
   }

   protected ItemStack consumeWithRemainder(BlockSource p_344354_, ItemStack p_343730_, ItemStack p_344011_) {
      p_343730_.shrink(1);
      if (p_343730_.isEmpty()) {
         return p_344011_;
      }

      this.addToInventoryOrDispense(p_344354_, p_344011_);
      return p_343730_;
   }

   private void addToInventoryOrDispense(BlockSource p_343398_, ItemStack p_344816_) {
      ItemStack itemstack = p_343398_.blockEntity().insertItem(p_344816_);
      if (!itemstack.isEmpty()) {
         Direction direction = p_343398_.state().getValue(DispenserBlock.FACING);
         spawnItem(p_343398_.level(), itemstack, 6, direction, DispenserBlock.getDispensePosition(p_343398_));
         playDefaultSound(p_343398_);
         playDefaultAnimation(p_343398_, direction);
      }
   }
}
