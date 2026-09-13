package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public interface DispensibleContainerItem {
   default void checkExtraContent(final @Nullable LivingEntity user, final Level level, final ItemStack itemStack, final BlockPos pos) {
   }

   boolean emptyContents(final @Nullable LivingEntity user, final Level level, final BlockPos pos, final @Nullable BlockHitResult hitResult);
}
