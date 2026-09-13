package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

public class LookAndFollowTradingPlayerSink extends Behavior<Villager> {
   private final float speedModifier;

   public LookAndFollowTradingPlayerSink(float p_23434_) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
      this.speedModifier = p_23434_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_23445_, Villager p_450961_) {
      Player player = p_450961_.getTradingPlayer();
      return p_450961_.isAlive() && player != null && !p_450961_.isInWater() && !p_450961_.hurtMarked && p_450961_.distanceToSqr(player) <= 16.0;
   }

   protected boolean canStillUse(ServerLevel p_23448_, Villager p_453695_, long p_23450_) {
      return this.checkExtraStartConditions(p_23448_, p_453695_);
   }

   protected void start(ServerLevel p_23458_, Villager p_454877_, long p_23460_) {
      this.followPlayer(p_454877_);
   }

   protected void stop(ServerLevel p_23466_, Villager p_451069_, long p_23468_) {
      Brain<?> brain = p_451069_.getBrain();
      brain.eraseMemory(MemoryModuleType.WALK_TARGET);
      brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(ServerLevel p_23474_, Villager p_457753_, long p_23476_) {
      this.followPlayer(p_457753_);
   }

   @Override
   protected boolean timedOut(long p_23436_) {
      return false;
   }

   private void followPlayer(Villager p_458447_) {
      Brain<?> brain = p_458447_.getBrain();
      brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(p_458447_.getTradingPlayer(), false), this.speedModifier, 2));
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p_458447_.getTradingPlayer(), true));
   }
}
