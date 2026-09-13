package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

public interface Portal {
   default int getPortalTransitionTime(final ServerLevel level, final Entity entity) {
      return 0;
   }

   @Nullable TeleportTransition getPortalDestination(final ServerLevel currentLevel, final Entity entity, final BlockPos portalEntryPos);

   default Portal.Transition getLocalTransition() {
      return Portal.Transition.NONE;
   }

   enum Transition {
      CONFUSION,
      NONE;
   }
}
