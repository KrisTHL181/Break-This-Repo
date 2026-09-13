package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public interface Portal {
   default int getPortalTransitionTime(ServerLevel p_345098_, Entity p_345452_) {
      return 0;
   }

   @Nullable TeleportTransition getPortalDestination(ServerLevel var1, Entity var2, BlockPos var3);

   default Portal.Transition getLocalTransition() {
      return Portal.Transition.NONE;
   }

   enum Transition {
      CONFUSION,
      NONE;
   }
}
