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

   protected abstract void onOpen(Level var1, BlockPos var2, BlockState var3);

   protected abstract void onClose(Level var1, BlockPos var2, BlockState var3);

   protected abstract void openerCountChanged(Level var1, BlockPos var2, BlockState var3, int var4, int var5);

   public abstract boolean isOwnContainer(Player var1);

   public void incrementOpeners(LivingEntity p_430652_, Level p_155454_, BlockPos p_155455_, BlockState p_155456_, double p_426289_) {
      int i = this.openCount++;
      if (i == 0) {
         this.onOpen(p_155454_, p_155455_, p_155456_);
         p_155454_.gameEvent(p_430652_, GameEvent.CONTAINER_OPEN, p_155455_);
         scheduleRecheck(p_155454_, p_155455_, p_155456_);
      }

      this.openerCountChanged(p_155454_, p_155455_, p_155456_, i, this.openCount);
      this.maxInteractionRange = Math.max(p_426289_, this.maxInteractionRange);
   }

   public void decrementOpeners(LivingEntity p_430878_, Level p_155470_, BlockPos p_155471_, BlockState p_155472_) {
      int i = this.openCount--;
      if (this.openCount == 0) {
         this.onClose(p_155470_, p_155471_, p_155472_);
         p_155470_.gameEvent(p_430878_, GameEvent.CONTAINER_CLOSE, p_155471_);
         this.maxInteractionRange = 0.0;
      }

      this.openerCountChanged(p_155470_, p_155471_, p_155472_, i, this.openCount);
   }

   public List<ContainerUser> getEntitiesWithContainerOpen(Level p_426166_, BlockPos p_424786_) {
      double d0 = this.maxInteractionRange + 4.0;
      AABB aabb = new AABB(p_424786_).inflate(d0);
      return p_426166_.getEntities((Entity)null, aabb, p_426258_ -> this.hasContainerOpen(p_426258_, p_424786_))
         .stream()
         .map(p_431386_ -> (ContainerUser)p_431386_)
         .collect(Collectors.toList());
   }

   private boolean hasContainerOpen(Entity p_431382_, BlockPos p_428254_) {
      return p_431382_ instanceof ContainerUser containeruser && !containeruser.getLivingEntity().isSpectator()
         ? containeruser.hasContainerOpen(this, p_428254_)
         : false;
   }

   public void recheckOpeners(Level p_155477_, BlockPos p_155478_, BlockState p_155479_) {
      List<ContainerUser> list = this.getEntitiesWithContainerOpen(p_155477_, p_155478_);
      this.maxInteractionRange = 0.0;

      for (ContainerUser containeruser : list) {
         this.maxInteractionRange = Math.max(containeruser.getContainerInteractionRange(), this.maxInteractionRange);
      }

      int i = list.size();
      int j = this.openCount;
      if (j != i) {
         boolean flag = i != 0;
         boolean flag1 = j != 0;
         if (flag && !flag1) {
            this.onOpen(p_155477_, p_155478_, p_155479_);
            p_155477_.gameEvent(null, GameEvent.CONTAINER_OPEN, p_155478_);
         } else if (!flag) {
            this.onClose(p_155477_, p_155478_, p_155479_);
            p_155477_.gameEvent(null, GameEvent.CONTAINER_CLOSE, p_155478_);
         }

         this.openCount = i;
      }

      this.openerCountChanged(p_155477_, p_155478_, p_155479_, j, i);
      if (i > 0) {
         scheduleRecheck(p_155477_, p_155478_, p_155479_);
      }
   }

   public int getOpenerCount() {
      return this.openCount;
   }

   private static void scheduleRecheck(Level p_155481_, BlockPos p_155482_, BlockState p_155483_) {
      p_155481_.scheduleTick(p_155482_, p_155483_.getBlock(), 5);
   }
}
