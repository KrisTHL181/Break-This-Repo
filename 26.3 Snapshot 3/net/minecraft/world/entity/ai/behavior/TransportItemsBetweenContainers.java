package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public class TransportItemsBetweenContainers extends Behavior<PathfinderMob> {
   public static final int TARGET_INTERACTION_TIME = 60;
   private static final int VISITED_POSITIONS_MEMORY_TIME = 6000;
   private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
   private static final int MAX_VISITED_POSITIONS = 10;
   private static final int MAX_UNREACHABLE_POSITIONS = 50;
   private static final int PASSENGER_MOB_TARGET_SEARCH_DISTANCE = 1;
   private static final int IDLE_COOLDOWN = 140;
   private static final double CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE = 3.0;
   private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_DISTANCE = 0.5;
   private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_PATH_END_DISTANCE = 1.0;
   private static final double CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET = 2.0;
   private final float speedModifier;
   private final int horizontalSearchDistance;
   private final int verticalSearchDistance;
   private final Predicate<BlockState> sourceBlockType;
   private final Predicate<BlockState> destinationBlockType;
   private final Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget;
   private final Consumer<PathfinderMob> onStartTravelling;
   private final Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions;
   private TransportItemsBetweenContainers.@Nullable TransportItemTarget target = null;
   private TransportItemsBetweenContainers.TransportItemState state;
   private TransportItemsBetweenContainers.@Nullable ContainerInteractionState interactionState;
   private int ticksSinceReachingTarget;

   public TransportItemsBetweenContainers(
      final float speedModifier,
      final Predicate<BlockState> sourceBlockType,
      final Predicate<BlockState> destinationBlockType,
      final int horizontalSearchDistance,
      final int verticalSearchDistance,
      final Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> onTargetInteractionActions,
      final Consumer<PathfinderMob> onStartTravelling,
      final Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.VISITED_BLOCK_POSITIONS,
            MemoryStatus.REGISTERED,
            MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS,
            MemoryStatus.REGISTERED,
            MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.IS_PANICKING,
            MemoryStatus.VALUE_ABSENT
         )
      );
      this.speedModifier = speedModifier;
      this.sourceBlockType = sourceBlockType;
      this.destinationBlockType = destinationBlockType;
      this.horizontalSearchDistance = horizontalSearchDistance;
      this.verticalSearchDistance = verticalSearchDistance;
      this.onStartTravelling = onStartTravelling;
      this.shouldQueueForTarget = shouldQueueForTarget;
      this.onTargetInteractionActions = onTargetInteractionActions;
      this.state = TransportItemsBetweenContainers.TransportItemState.TRAVELLING;
   }

   protected void start(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      if (body.getNavigation() instanceof GroundPathNavigation pathNavigation) {
         pathNavigation.setCanPathToTargetsBelowSurface(true);
      }
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final PathfinderMob body) {
      return !body.isLeashed();
   }

   protected boolean canStillUse(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      return body.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !body.isPanicking() && !body.isLeashed();
   }

   @Override
   protected boolean timedOut(final long timestamp) {
      return false;
   }

   protected void tick(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      boolean updatedInvalidTarget = this.updateInvalidTarget(level, body);
      if (this.target == null) {
         this.stop(level, body, timestamp);
      } else if (!updatedInvalidTarget) {
         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.QUEUING)) {
            this.onQueuingForTarget(this.target, level, body);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            this.onTravelToTarget(this.target, level, body);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.INTERACTING)) {
            this.onReachedTarget(this.target, level, body);
         }
      }
   }

   private boolean updateInvalidTarget(final ServerLevel level, final PathfinderMob body) {
      if (!this.hasValidTarget(level, body)) {
         this.stopTargetingCurrentTarget(body);
         Optional<TransportItemsBetweenContainers.TransportItemTarget> targetBlockPosition = this.getTransportTarget(level, body);
         if (targetBlockPosition.isPresent()) {
            this.target = targetBlockPosition.get();
            this.onStartTravelling(body);
            this.setVisitedBlockPos(body, level, this.target.pos);
            return true;
         } else {
            this.enterCooldownAfterNoMatchingTargetFound(body);
            return true;
         }
      } else {
         return false;
      }
   }

   private void onQueuingForTarget(final TransportItemsBetweenContainers.TransportItemTarget target, final Level level, final PathfinderMob body) {
      if (!this.isAnotherMobInteractingWithTarget(target, level)) {
         this.resumeTravelling(body);
      }
   }

   protected void onTravelToTarget(final TransportItemsBetweenContainers.TransportItemTarget target, final Level level, final PathfinderMob body) {
      if (this.isWithinTargetDistance(3.0, target, level, body, this.getCenterPos(body)) && this.isAnotherMobInteractingWithTarget(target, level)) {
         this.startQueuing(body);
      } else if (this.isWithinTargetDistance(getInteractionRange(body), target, level, body, this.getCenterPos(body))) {
         this.startOnReachedTargetInteraction(target, body);
      } else {
         this.walkTowardsTarget(body);
      }
   }

   private Vec3 getCenterPos(final PathfinderMob body) {
      return this.setMiddleYPosition(body, body.position());
   }

   protected void onReachedTarget(final TransportItemsBetweenContainers.TransportItemTarget target, final Level level, final PathfinderMob body) {
      if (!this.isWithinTargetDistance(2.0, target, level, body, this.getCenterPos(body))) {
         this.onStartTravelling(body);
      } else {
         this.ticksSinceReachingTarget++;
         this.onTargetInteraction(target, body);
         if (this.ticksSinceReachingTarget >= 60) {
            this.doReachedTargetInteraction(
               body,
               target.container,
               this::pickUpItems,
               (mob, container) -> this.stopTargetingCurrentTarget(body),
               this::putDownItem,
               (mob, container) -> this.stopTargetingCurrentTarget(body)
            );
            this.onStartTravelling(body);
         }
      }
   }

   private void startQueuing(final PathfinderMob body) {
      this.stopInPlace(body);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.QUEUING);
   }

   private void resumeTravelling(final PathfinderMob body) {
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.walkTowardsTarget(body);
   }

   private void walkTowardsTarget(final PathfinderMob body) {
      if (this.target != null) {
         BehaviorUtils.setWalkAndLookTargetMemories(body, this.target.pos, this.speedModifier, 0);
      }
   }

   private void startOnReachedTargetInteraction(final TransportItemsBetweenContainers.TransportItemTarget target, final PathfinderMob body) {
      this.doReachedTargetInteraction(
         body,
         target.container,
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM)
      );
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.INTERACTING);
   }

   private void onStartTravelling(final PathfinderMob body) {
      this.onStartTravelling.accept(body);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.interactionState = null;
      this.ticksSinceReachingTarget = 0;
   }

   private BiConsumer<PathfinderMob, Container> onReachedInteraction(final TransportItemsBetweenContainers.ContainerInteractionState state) {
      return (mob, container) -> this.setInteractionState(state);
   }

   private void setTransportingState(final TransportItemsBetweenContainers.TransportItemState state) {
      this.state = state;
   }

   private void setInteractionState(final TransportItemsBetweenContainers.ContainerInteractionState state) {
      this.interactionState = state;
   }

   private void onTargetInteraction(final TransportItemsBetweenContainers.TransportItemTarget target, final PathfinderMob body) {
      body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos));
      this.stopInPlace(body);
      if (this.interactionState != null) {
         Optional.ofNullable(this.onTargetInteractionActions.get(this.interactionState))
            .ifPresent(action -> action.accept(body, target, this.ticksSinceReachingTarget));
      }
   }

   private void doReachedTargetInteraction(
      final PathfinderMob body,
      final Container container,
      final BiConsumer<PathfinderMob, Container> onPickupSuccess,
      final BiConsumer<PathfinderMob, Container> onPickupFailure,
      final BiConsumer<PathfinderMob, Container> onPlaceSuccess,
      final BiConsumer<PathfinderMob, Container> onPlaceFailure
   ) {
      if (isPickingUpItems(body)) {
         if (matchesGettingItemsRequirement(container)) {
            onPickupSuccess.accept(body, container);
         } else {
            onPickupFailure.accept(body, container);
         }
      } else if (matchesLeavingItemsRequirement(body, container)) {
         onPlaceSuccess.accept(body, container);
      } else {
         onPlaceFailure.accept(body, container);
      }
   }

   private Optional<TransportItemsBetweenContainers.TransportItemTarget> getTransportTarget(final ServerLevel level, final PathfinderMob body) {
      AABB targetBlockSearchArea = this.getTargetSearchArea(body);
      Set<GlobalPos> visitedPositions = getVisitedPositions(body);
      Set<GlobalPos> unreachablePositions = getUnreachablePositions(body);
      List<ChunkPos> list = ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(body), 16) + 1)
         .toList();
      TransportItemsBetweenContainers.TransportItemTarget target = null;
      double closestDistance = Float.MAX_VALUE;

      for (ChunkPos chunkPos : list) {
         LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
         if (levelChunk != null) {
            for (BlockEntity potentialTarget : levelChunk.getBlockEntities().values()) {
               if (potentialTarget instanceof ChestBlockEntity chestBlockEntity) {
                  double distance = chestBlockEntity.getBlockPos().distToCenterSqr(body.position());
                  if (distance < closestDistance) {
                     TransportItemsBetweenContainers.TransportItemTarget targetValidToPick = this.isTargetValidToPick(
                        body, level, chestBlockEntity, visitedPositions, unreachablePositions, targetBlockSearchArea
                     );
                     if (targetValidToPick != null) {
                        target = targetValidToPick;
                        closestDistance = distance;
                     }
                  }
               }
            }
         }
      }

      return target == null ? Optional.empty() : Optional.of(target);
   }

   private TransportItemsBetweenContainers.@Nullable TransportItemTarget isTargetValidToPick(
      final PathfinderMob body,
      final Level level,
      final BlockEntity blockEntity,
      final Set<GlobalPos> visitedPositions,
      final Set<GlobalPos> unreachablePositions,
      final AABB targetBlockSearchArea
   ) {
      BlockPos blockPos = blockEntity.getBlockPos();
      boolean isWithinSearchArea = targetBlockSearchArea.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      if (!isWithinSearchArea) {
         return null;
      }

      TransportItemsBetweenContainers.TransportItemTarget transportItemTarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
         blockEntity, level
      );
      if (transportItemTarget == null) {
         return null;
      }

      boolean isValidTarget = this.isWantedBlock(body, transportItemTarget.state)
         && !this.isPositionAlreadyVisited(visitedPositions, unreachablePositions, transportItemTarget, level)
         && !this.isContainerLocked(transportItemTarget);
      return isValidTarget ? transportItemTarget : null;
   }

   private boolean isContainerLocked(final TransportItemsBetweenContainers.TransportItemTarget transportItemTarget) {
      return transportItemTarget.blockEntity instanceof BaseContainerBlockEntity blockEntity && blockEntity.isLocked();
   }

   private boolean hasValidTarget(final Level level, final PathfinderMob body) {
      boolean targetIsOfValidType = this.target != null && this.isWantedBlock(body, this.target.state) && this.targetHasNotChanged(level, this.target);
      if (targetIsOfValidType && !this.isTargetBlocked(level, this.target)) {
         if (!this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            return true;
         }

         if (this.hasValidTravellingPath(level, this.target, body)) {
            return true;
         }

         this.markVisitedBlockPosAsUnreachable(body, level, this.target.pos);
      }

      return false;
   }

   private boolean hasValidTravellingPath(final Level level, final TransportItemsBetweenContainers.TransportItemTarget target, final PathfinderMob body) {
      Path path = body.getNavigation().getPath() == null ? body.getNavigation().createPath(target.pos, 0) : body.getNavigation().getPath();
      Vec3 posFromWhichToReachTarget = this.getPositionToReachTargetFrom(path, body);
      boolean canReachTarget = this.isWithinTargetDistance(getInteractionRange(body), target, level, body, posFromWhichToReachTarget);
      boolean hasNotYetCreatedPathToTarget = path == null && !canReachTarget;
      return hasNotYetCreatedPathToTarget || this.targetIsReachableFromPosition(level, canReachTarget, posFromWhichToReachTarget, target, body);
   }

   private Vec3 getPositionToReachTargetFrom(final @Nullable Path path, final PathfinderMob body) {
      boolean haveNoValidPath = path == null || path.getEndNode() == null;
      Vec3 bottomCenter = haveNoValidPath ? body.position() : Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
      return this.setMiddleYPosition(body, bottomCenter);
   }

   private Vec3 setMiddleYPosition(final PathfinderMob body, final Vec3 pos) {
      return pos.add(0.0, body.getBoundingBox().getYsize() / 2.0, 0.0);
   }

   private boolean isTargetBlocked(final Level level, final TransportItemsBetweenContainers.TransportItemTarget target) {
      return ChestBlock.isChestBlockedAt(level, target.pos);
   }

   private boolean targetHasNotChanged(final Level level, final TransportItemsBetweenContainers.TransportItemTarget target) {
      return target.blockEntity.equals(level.getBlockEntity(target.pos));
   }

   private Stream<TransportItemsBetweenContainers.TransportItemTarget> getConnectedTargets(
      final TransportItemsBetweenContainers.TransportItemTarget target, final Level level
   ) {
      if (target.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
         TransportItemsBetweenContainers.TransportItemTarget connectedTarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            ChestBlock.getConnectedBlockPos(target.pos, target.state), level
         );
         return connectedTarget != null ? Stream.of(target, connectedTarget) : Stream.of(target);
      } else {
         return Stream.of(target);
      }
   }

   private AABB getTargetSearchArea(final PathfinderMob mob) {
      int horizontalSearchDistance = this.getHorizontalSearchDistance(mob);
      return new AABB(mob.blockPosition()).inflate(horizontalSearchDistance, this.getVerticalSearchDistance(mob), horizontalSearchDistance);
   }

   private int getHorizontalSearchDistance(final PathfinderMob mob) {
      return mob.isPassenger() ? 1 : this.horizontalSearchDistance;
   }

   private int getVerticalSearchDistance(final PathfinderMob mob) {
      return mob.isPassenger() ? 1 : this.verticalSearchDistance;
   }

   private static Set<GlobalPos> getVisitedPositions(final PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
   }

   private static Set<GlobalPos> getUnreachablePositions(final PathfinderMob mob) {
      return mob.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
   }

   private boolean isPositionAlreadyVisited(
      final Set<GlobalPos> visitedPositions,
      final Set<GlobalPos> unreachablePositions,
      final TransportItemsBetweenContainers.TransportItemTarget target,
      final Level level
   ) {
      return this.getConnectedTargets(target, level)
         .map(transportItemTarget -> new GlobalPos(level.dimension(), transportItemTarget.pos))
         .anyMatch(pos -> visitedPositions.contains(pos) || unreachablePositions.contains(pos));
   }

   private static boolean hasFinishedPath(final PathfinderMob body) {
      return body.getNavigation().getPath() != null && body.getNavigation().getPath().isDone();
   }

   protected void setVisitedBlockPos(final PathfinderMob body, final Level level, final BlockPos target) {
      Set<GlobalPos> visitedPositions = new HashSet<>(getVisitedPositions(body));
      visitedPositions.add(new GlobalPos(level.dimension(), target));
      if (visitedPositions.size() > 10) {
         this.enterCooldownAfterNoMatchingTargetFound(body);
      } else {
         body.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, visitedPositions, 6000L);
      }
   }

   protected void markVisitedBlockPosAsUnreachable(final PathfinderMob body, final Level level, final BlockPos target) {
      Set<GlobalPos> visitedPositions = new HashSet<>(getVisitedPositions(body));
      visitedPositions.remove(new GlobalPos(level.dimension(), target));
      Set<GlobalPos> unreachablePositions = new HashSet<>(getUnreachablePositions(body));
      unreachablePositions.add(new GlobalPos(level.dimension(), target));
      if (unreachablePositions.size() > 50) {
         this.enterCooldownAfterNoMatchingTargetFound(body);
      } else {
         body.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, visitedPositions, 6000L);
         body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, unreachablePositions, 6000L);
      }
   }

   private boolean isWantedBlock(final PathfinderMob mob, final BlockState block) {
      return isPickingUpItems(mob) ? this.sourceBlockType.test(block) : this.destinationBlockType.test(block);
   }

   private static double getInteractionRange(final PathfinderMob body) {
      return hasFinishedPath(body) ? 1.0 : 0.5;
   }

   private boolean isWithinTargetDistance(
      final double distance, final TransportItemsBetweenContainers.TransportItemTarget target, final Level level, final PathfinderMob body, final Vec3 fromPos
   ) {
      AABB boundingBox = body.getBoundingBox();
      AABB movedBoundBox = AABB.ofSize(fromPos, boundingBox.getXsize(), boundingBox.getYsize(), boundingBox.getZsize());
      return target.state.getCollisionShape(level, target.pos).bounds().inflate(distance, 0.5, distance).move(target.pos).intersects(movedBoundBox);
   }

   private boolean targetIsReachableFromPosition(
      final Level level,
      final boolean canReachTarget,
      final Vec3 pos,
      final TransportItemsBetweenContainers.TransportItemTarget target,
      final PathfinderMob body
   ) {
      return canReachTarget && this.canSeeAnyTargetSide(target, level, body, pos);
   }

   private boolean canSeeAnyTargetSide(
      final TransportItemsBetweenContainers.TransportItemTarget target, final Level level, final PathfinderMob body, final Vec3 eyePosition
   ) {
      Vec3 center = Vec3.atCenterOf(target.pos);
      return Direction.stream()
         .map(direction -> center.add(0.5 * direction.getStepX(), 0.5 * direction.getStepY(), 0.5 * direction.getStepZ()))
         .map(hitTarget -> level.clip(new ClipContext(eyePosition, hitTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body)))
         .anyMatch(hitResult -> hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(target.pos));
   }

   private boolean isAnotherMobInteractingWithTarget(final TransportItemsBetweenContainers.TransportItemTarget target, final Level level) {
      return this.getConnectedTargets(target, level).anyMatch(this.shouldQueueForTarget);
   }

   private static boolean isPickingUpItems(final PathfinderMob body) {
      return body.getMainHandItem().isEmpty();
   }

   private static boolean matchesGettingItemsRequirement(final Container container) {
      return !container.isEmpty();
   }

   private static boolean matchesLeavingItemsRequirement(final PathfinderMob body, final Container container) {
      return container.isEmpty() || hasItemMatchingHandItem(body, container);
   }

   private static boolean hasItemMatchingHandItem(final PathfinderMob body, final Container container) {
      ItemStack mainHandItem = body.getMainHandItem();

      for (ItemStack itemStack : container) {
         if (ItemStack.isSameItem(itemStack, mainHandItem)) {
            return true;
         }
      }

      return false;
   }

   private void pickUpItems(final PathfinderMob body, final Container container) {
      body.setItemSlot(EquipmentSlot.MAINHAND, pickupItemFromContainer(container));
      body.setGuaranteedDrop(EquipmentSlot.MAINHAND);
      container.setChanged();
      this.clearMemoriesAfterMatchingTargetFound(body);
   }

   private void putDownItem(final PathfinderMob body, final Container container) {
      ItemStack itemsLeftAfterVisitingChest = addItemsToContainer(body, container);
      container.setChanged();
      body.setItemSlot(EquipmentSlot.MAINHAND, itemsLeftAfterVisitingChest);
      if (itemsLeftAfterVisitingChest.isEmpty()) {
         this.clearMemoriesAfterMatchingTargetFound(body);
      } else {
         this.stopTargetingCurrentTarget(body);
      }
   }

   private static ItemStack pickupItemFromContainer(final Container container) {
      int slot = 0;

      for (ItemStack itemStack : container) {
         if (!itemStack.isEmpty()) {
            int itemCount = Math.min(itemStack.getCount(), 16);
            return container.removeItem(slot, itemCount);
         }

         slot++;
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack addItemsToContainer(final PathfinderMob body, final Container container) {
      int slot = 0;
      ItemStack itemStack = body.getMainHandItem();

      for (ItemStack containerItemStack : container) {
         if (containerItemStack.isEmpty()) {
            container.setItem(slot, itemStack);
            return ItemStack.EMPTY;
         }

         if (ItemStack.isSameItemSameComponents(containerItemStack, itemStack) && containerItemStack.getCount() < containerItemStack.getMaxStackSize()) {
            int countThatCanBeAdded = containerItemStack.getMaxStackSize() - containerItemStack.getCount();
            int countToAdd = Math.min(countThatCanBeAdded, itemStack.getCount());
            containerItemStack.setCount(containerItemStack.getCount() + countToAdd);
            itemStack.setCount(itemStack.getCount() - countThatCanBeAdded);
            container.setItem(slot, containerItemStack);
            if (itemStack.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }

         slot++;
      }

      return itemStack;
   }

   protected void stopTargetingCurrentTarget(final PathfinderMob body) {
      this.ticksSinceReachingTarget = 0;
      this.target = null;
      body.getNavigation().stop();
      body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   protected void clearMemoriesAfterMatchingTargetFound(final PathfinderMob body) {
      this.stopTargetingCurrentTarget(body);
      body.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      body.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   private void enterCooldownAfterNoMatchingTargetFound(final PathfinderMob body) {
      this.stopTargetingCurrentTarget(body);
      body.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
      body.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      body.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   protected void stop(final ServerLevel level, final PathfinderMob body, final long timestamp) {
      this.onStartTravelling(body);
      if (body.getNavigation() instanceof GroundPathNavigation pathNavigation) {
         pathNavigation.setCanPathToTargetsBelowSurface(false);
      }
   }

   private void stopInPlace(final PathfinderMob mob) {
      mob.getNavigation().stop();
      mob.setXxa(0.0F);
      mob.setYya(0.0F);
      mob.setSpeed(0.0F);
      mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
   }

   public enum ContainerInteractionState {
      PICKUP_ITEM,
      PICKUP_NO_ITEM,
      PLACE_ITEM,
      PLACE_NO_ITEM;
   }

   @FunctionalInterface
   public interface OnTargetReachedInteraction extends TriConsumer<PathfinderMob, TransportItemsBetweenContainers.TransportItemTarget, Integer> {
   }

   public enum TransportItemState {
      TRAVELLING,
      QUEUING,
      INTERACTING;
   }

   public record TransportItemTarget(BlockPos pos, Container container, BlockEntity blockEntity, BlockState state) {
      public static TransportItemsBetweenContainers.@Nullable TransportItemTarget tryCreatePossibleTarget(final BlockEntity blockEntity, final Level level) {
         BlockPos blockPos = blockEntity.getBlockPos();
         BlockState blockState = blockEntity.getBlockState();
         Container container = getBlockEntityContainer(blockEntity, blockState, level, blockPos);
         return container != null ? new TransportItemsBetweenContainers.TransportItemTarget(blockPos, container, blockEntity, blockState) : null;
      }

      public static TransportItemsBetweenContainers.@Nullable TransportItemTarget tryCreatePossibleTarget(final BlockPos blockPos, final Level level) {
         BlockEntity blockEntity = level.getBlockEntity(blockPos);
         return blockEntity == null ? null : tryCreatePossibleTarget(blockEntity, level);
      }

      private static @Nullable Container getBlockEntityContainer(
         final BlockEntity blockEntity, final BlockState blockState, final Level level, final BlockPos blockPos
      ) {
         if (blockState.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, blockState, level, blockPos, false);
         } else {
            return blockEntity instanceof Container container ? container : null;
         }
      }
   }
}
