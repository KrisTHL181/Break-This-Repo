package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public interface DispensibleContainerItem {
   default void checkExtraContent(@Nullable LivingEntity p_391486_, Level p_150818_, ItemStack p_150819_, BlockPos p_150820_) {
   }

   boolean emptyContents(@Nullable LivingEntity var1, Level var2, BlockPos var3, @Nullable BlockHitResult var4);
}
