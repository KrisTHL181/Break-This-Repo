package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox.Mu;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class InteractWithDoor {
   private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
   private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 3.0;
   private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0;

   public static BehaviorControl<LivingEntity> create() {
      MutableObject<Node> lastCheckedNode = new MutableObject();
      MutableInt remainingCooldown = new MutableInt(0);
      return BehaviorBuilder.create(
         i -> i.group(i.present(MemoryModuleType.PATH), i.registered(MemoryModuleType.DOORS_TO_CLOSE), i.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES))
            .apply(
               i,
               (pathMemory, doorsMemory, nearestEntities) -> (level, body, timestamp) -> {
                  Path path = i.get(pathMemory);
                  Optional<Set<GlobalPos>> doors = i.tryGet(doorsMemory);
                  if (!path.notStarted() && !path.isDone()) {
                     if (Objects.equals(lastCheckedNode.get(), path.getNextNode())) {
                        remainingCooldown.setValue(20);
                     } else if (remainingCooldown.decrementAndGet() > 0) {
                        return false;
                     }

                     lastCheckedNode.setValue(path.getNextNode());
                     Node fromNode = path.getPreviousNode();
                     Node toNode = path.getNextNode();
                     BlockPos fromPos = fromNode.asBlockPos();
                     BlockState fromState = level.getBlockState(fromPos);
                     if (fromState.is(BlockTags.MOB_INTERACTABLE_DOORS, s -> s.getBlock() instanceof DoorBlock)) {
                        DoorBlock fromBlock = (DoorBlock)fromState.getBlock();
                        if (!fromBlock.isOpen(fromState)) {
                           fromBlock.setOpen(body, level, fromState, fromPos, true);
                        }

                        doors = rememberDoorToClose(doorsMemory, doors, level, fromPos);
                     }

                     BlockPos toPos = toNode.asBlockPos();
                     BlockState toState = level.getBlockState(toPos);
                     if (toState.is(BlockTags.MOB_INTERACTABLE_DOORS, s -> s.getBlock() instanceof DoorBlock)) {
                        DoorBlock door = (DoorBlock)toState.getBlock();
                        if (!door.isOpen(toState)) {
                           door.setOpen(body, level, toState, toPos, true);
                           doors = rememberDoorToClose(doorsMemory, doors, level, toPos);
                        }
                     }

                     doors.ifPresent(
                        doorSet -> closeDoorsThatIHaveOpenedOrPassedThrough(level, body, fromNode, toNode, (Set<GlobalPos>)doorSet, i.tryGet(nearestEntities))
                     );
                     return true;
                  } else {
                     return false;
                  }
               }
            )
      );
   }

   public static void closeDoorsThatIHaveOpenedOrPassedThrough(
      final ServerLevel level,
      final LivingEntity body,
      final @Nullable Node movingFromNode,
      final @Nullable Node movingToNode,
      final Set<GlobalPos> doors,
      final Optional<List<LivingEntity>> nearestEntities
   ) {
      Iterator<GlobalPos> iterator = doors.iterator();

      while (iterator.hasNext()) {
         GlobalPos doorGlobalPos = iterator.next();
         BlockPos doorPos = doorGlobalPos.pos();
         if ((movingFromNode == null || !movingFromNode.asBlockPos().equals(doorPos)) && (movingToNode == null || !movingToNode.asBlockPos().equals(doorPos))) {
            if (isDoorTooFarAway(level, body, doorGlobalPos)) {
               iterator.remove();
            } else {
               BlockState state = level.getBlockState(doorPos);
               if (!state.is(BlockTags.MOB_INTERACTABLE_DOORS, s -> s.getBlock() instanceof DoorBlock)) {
                  iterator.remove();
               } else {
                  DoorBlock block = (DoorBlock)state.getBlock();
                  if (!block.isOpen(state)) {
                     iterator.remove();
                  } else if (areOtherMobsComingThroughDoor(body, doorPos, nearestEntities)) {
                     iterator.remove();
                  } else {
                     block.setOpen(body, level, state, doorPos, false);
                     iterator.remove();
                  }
               }
            }
         }
      }
   }

   private static boolean areOtherMobsComingThroughDoor(final LivingEntity body, final BlockPos doorPos, final Optional<List<LivingEntity>> nearestEntities) {
      return nearestEntities.isEmpty()
         ? false
         : nearestEntities.get()
            .stream()
            .filter(otherMob -> otherMob.getType() == body.getType())
            .filter(otherMob -> doorPos.closerToCenterThan(otherMob.position(), 2.0))
            .anyMatch(otherMob -> isMobComingThroughDoor(otherMob.getBrain(), doorPos));
   }

   private static boolean isMobComingThroughDoor(final Brain<?> otherBrain, final BlockPos doorPos) {
      if (!otherBrain.hasMemoryValue(MemoryModuleType.PATH)) {
         return false;
      }

      Path path = otherBrain.getMemory(MemoryModuleType.PATH).get();
      if (path.isDone()) {
         return false;
      }

      Node movingFromNode = path.getPreviousNode();
      if (movingFromNode == null) {
         return false;
      }

      Node movingToNode = path.getNextNode();
      return doorPos.equals(movingFromNode.asBlockPos()) || doorPos.equals(movingToNode.asBlockPos());
   }

   private static boolean isDoorTooFarAway(final ServerLevel level, final LivingEntity body, final GlobalPos doorGlobalPos) {
      return doorGlobalPos.dimension() != level.dimension() || !doorGlobalPos.pos().closerToCenterThan(body.position(), 3.0);
   }

   private static Optional<Set<GlobalPos>> rememberDoorToClose(
      final MemoryAccessor<Mu, Set<GlobalPos>> doorsMemory, final Optional<Set<GlobalPos>> doors, final ServerLevel level, final BlockPos doorPos
   ) {
      GlobalPos globalDoorPos = GlobalPos.of(level.dimension(), doorPos);
      return Optional.of(doors.<Set<GlobalPos>>map(set -> {
         set.add(globalDoorPos);
         return set;
      }).orElseGet(() -> {
         Set<GlobalPos> set = Sets.newHashSet(new GlobalPos[]{globalDoorPos});
         doorsMemory.set(set);
         return set;
      }));
   }
}
