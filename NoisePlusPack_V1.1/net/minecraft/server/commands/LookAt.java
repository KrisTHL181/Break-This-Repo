package net.minecraft.server.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@FunctionalInterface
public interface LookAt {
   void perform(CommandSourceStack var1, Entity var2);

   record LookAtEntity(Entity entity, EntityAnchorArgument.Anchor anchor) implements LookAt {
      @Override
      public void perform(CommandSourceStack p_367204_, Entity p_361589_) {
         if (p_361589_ instanceof ServerPlayer serverplayer) {
            serverplayer.lookAt(p_367204_.getAnchor(), this.entity, this.anchor);
         } else {
            p_361589_.lookAt(p_367204_.getAnchor(), this.anchor.apply(this.entity));
         }
      }
   }

   record LookAtPosition(Vec3 position) implements LookAt {
      @Override
      public void perform(CommandSourceStack p_367941_, Entity p_366583_) {
         p_366583_.lookAt(p_367941_.getAnchor(), this.position);
      }
   }
}
