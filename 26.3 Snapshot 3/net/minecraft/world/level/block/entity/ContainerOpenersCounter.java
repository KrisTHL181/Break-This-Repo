package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public abstract class ContainerOpenersCounter {
   private static final int CHECK_TICK_DELAY = 5;
   private int openCount;
   private double maxInteractionRange;

   protected abstract void onOpen(final Level level, final BlockPos pos, final BlockState blockState);

   protected abstract void onClose(final Level level, final BlockPos pos, final BlockState blockState);

   protected abstract void openerCountChanged(final Level level, final BlockPos pos, final BlockState blockState, int previous, int current);

   public abstract boolean isOwnContainer(final Player player);

   public void incrementOpeners(final LivingEntity entity, final Level level, final BlockPos pos, final BlockState blockState, final double maxInteractionRange) {
      int previous = this.openCount++;
      if (previous == 0) {
         this.onOpen(level, pos, blockState);
         level.gameEvent(entity, GameEvent.CONTAINER_OPEN, pos);
         scheduleRecheck(level, pos, blockState);
      }

      this.openerCountChanged(level, pos, blockState, previous, this.openCount);
      this.maxInteractionRange = Math.max(maxInteractionRange, this.maxInteractionRange);
   }

   public void decrementOpeners(final LivingEntity entity, final Level level, final BlockPos pos, final BlockState blockState) {
      int previous = this.openCount--;
      if (this.openCount == 0) {
         this.onClose(level, pos, blockState);
         level.gameEvent(entity, GameEvent.CONTAINER_CLOSE, pos);
         this.maxInteractionRange = 0.0;
      }

      this.openerCountChanged(level, pos, blockState, previous, this.openCount);
   }

   public List<ContainerUser> getEntitiesWithContainerOpen(final Level level, final BlockPos pos) {
      double range = this.maxInteractionRange + 4.0;
      AABB searchBox = new AABB(pos).inflate(range);
      return level.getEntities((Entity)null, searchBox, entity -> this.hasContainerOpen(entity, pos))
         .stream()
         .map(entity -> (ContainerUser)entity)
         .collect(Collectors.toList());
   }

   private boolean hasContainerOpen(final Entity entity, final BlockPos blockPos) {
      return entity instanceof ContainerUser containerUser && !containerUser.getLivingEntity().isSpectator()
         ? containerUser.hasContainerOpen(this, blockPos)
         : false;
   }

   public void recheckOpeners(final Level level, final BlockPos pos, final BlockState blockState) {
      List<ContainerUser> containerUsers = this.getEntitiesWithContainerOpen(level, pos);
      this.maxInteractionRange = 0.0;

      for (ContainerUser containerUser : containerUsers) {
         this.maxInteractionRange = Math.max(containerUser.getContainerInteractionRange(), this.maxInteractionRange);
      }

      int openCount = containerUsers.size();
      int prevCount = this.openCount;
      if (prevCount != openCount) {
         boolean isOpen = openCount != 0;
         boolean wasOpen = prevCount != 0;
         if (isOpen && !wasOpen) {
            this.onOpen(level, pos, blockState);
            level.gameEvent(null, GameEvent.CONTAINER_OPEN, pos);
         } else if (!isOpen) {
            this.onClose(level, pos, blockState);
            level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
         }

         this.openCount = openCount;
      }

      this.openerCountChanged(level, pos, blockState, prevCount, openCount);
      if (openCount > 0) {
         scheduleRecheck(level, pos, blockState);
      }
   }

   public int getOpenerCount() {
      return this.openCount;
   }

   private static void scheduleRecheck(final Level level, final BlockPos blockPos, final BlockState blockState) {
      level.scheduleTick(blockPos, blockState.getBlock(), 5);
   }
}
