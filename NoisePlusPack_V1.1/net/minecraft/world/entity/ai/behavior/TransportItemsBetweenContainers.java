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
      float p_424497_,
      Predicate<BlockState> p_430226_,
      Predicate<BlockState> p_431607_,
      int p_422794_,
      int p_426431_,
      Map<TransportItemsBetweenContainers.ContainerInteractionState, TransportItemsBetweenContainers.OnTargetReachedInteraction> p_427019_,
      Consumer<PathfinderMob> p_431304_,
      Predicate<TransportItemsBetweenContainers.TransportItemTarget> p_422647_
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
      this.speedModifier = p_424497_;
      this.sourceBlockType = p_430226_;
      this.destinationBlockType = p_431607_;
      this.horizontalSearchDistance = p_422794_;
      this.verticalSearchDistance = p_426431_;
      this.onStartTravelling = p_431304_;
      this.shouldQueueForTarget = p_422647_;
      this.onTargetInteractionActions = p_427019_;
      this.state = TransportItemsBetweenContainers.TransportItemState.TRAVELLING;
   }

   protected void start(ServerLevel p_422501_, PathfinderMob p_428990_, long p_422511_) {
      if (p_428990_.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
         groundpathnavigation.setCanPathToTargetsBelowSurface(true);
      }
   }

   protected boolean checkExtraStartConditions(ServerLevel p_427337_, PathfinderMob p_426012_) {
      return !p_426012_.isLeashed();
   }

   protected boolean canStillUse(ServerLevel p_428991_, PathfinderMob p_428695_, long p_429576_) {
      return p_428695_.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !p_428695_.isPanicking() && !p_428695_.isLeashed();
   }

   @Override
   protected boolean timedOut(long p_424301_) {
      return false;
   }

   protected void tick(ServerLevel p_430265_, PathfinderMob p_423613_, long p_430914_) {
      boolean flag = this.updateInvalidTarget(p_430265_, p_423613_);
      if (this.target == null) {
         this.stop(p_430265_, p_423613_, p_430914_);
      } else if (!flag) {
         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.QUEUING)) {
            this.onQueuingForTarget(this.target, p_430265_, p_423613_);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            this.onTravelToTarget(this.target, p_430265_, p_423613_);
         }

         if (this.state.equals(TransportItemsBetweenContainers.TransportItemState.INTERACTING)) {
            this.onReachedTarget(this.target, p_430265_, p_423613_);
         }
      }
   }

   private boolean updateInvalidTarget(ServerLevel p_429508_, PathfinderMob p_430783_) {
      if (!this.hasValidTarget(p_429508_, p_430783_)) {
         this.stopTargetingCurrentTarget(p_430783_);
         Optional<TransportItemsBetweenContainers.TransportItemTarget> optional = this.getTransportTarget(p_429508_, p_430783_);
         if (optional.isPresent()) {
            this.target = optional.get();
            this.onStartTravelling(p_430783_);
            this.setVisitedBlockPos(p_430783_, p_429508_, this.target.pos);
            return true;
         } else {
            this.enterCooldownAfterNoMatchingTargetFound(p_430783_);
            return true;
         }
      } else {
         return false;
      }
   }

   private void onQueuingForTarget(TransportItemsBetweenContainers.TransportItemTarget p_429932_, Level p_425226_, PathfinderMob p_423308_) {
      if (!this.isAnotherMobInteractingWithTarget(p_429932_, p_425226_)) {
         this.resumeTravelling(p_423308_);
      }
   }

   protected void onTravelToTarget(TransportItemsBetweenContainers.TransportItemTarget p_424055_, Level p_431702_, PathfinderMob p_424815_) {
      if (this.isWithinTargetDistance(3.0, p_424055_, p_431702_, p_424815_, this.getCenterPos(p_424815_))
         && this.isAnotherMobInteractingWithTarget(p_424055_, p_431702_)) {
         this.startQueuing(p_424815_);
      } else if (this.isWithinTargetDistance(getInteractionRange(p_424815_), p_424055_, p_431702_, p_424815_, this.getCenterPos(p_424815_))) {
         this.startOnReachedTargetInteraction(p_424055_, p_424815_);
      } else {
         this.walkTowardsTarget(p_424815_);
      }
   }

   private Vec3 getCenterPos(PathfinderMob p_429723_) {
      return this.setMiddleYPosition(p_429723_, p_429723_.position());
   }

   protected void onReachedTarget(TransportItemsBetweenContainers.TransportItemTarget p_431168_, Level p_423012_, PathfinderMob p_431642_) {
      if (!this.isWithinTargetDistance(2.0, p_431168_, p_423012_, p_431642_, this.getCenterPos(p_431642_))) {
         this.onStartTravelling(p_431642_);
      } else {
         this.ticksSinceReachingTarget++;
         this.onTargetInteraction(p_431168_, p_431642_);
         if (this.ticksSinceReachingTarget >= 60) {
            this.doReachedTargetInteraction(
               p_431642_,
               p_431168_.container,
               this::pickUpItems,
               (p_430921_, p_425673_) -> this.stopTargetingCurrentTarget(p_431642_),
               this::putDownItem,
               (p_431419_, p_423494_) -> this.stopTargetingCurrentTarget(p_431642_)
            );
            this.onStartTravelling(p_431642_);
         }
      }
   }

   private void startQueuing(PathfinderMob p_426818_) {
      this.stopInPlace(p_426818_);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.QUEUING);
   }

   private void resumeTravelling(PathfinderMob p_431513_) {
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.walkTowardsTarget(p_431513_);
   }

   private void walkTowardsTarget(PathfinderMob p_424859_) {
      if (this.target != null) {
         BehaviorUtils.setWalkAndLookTargetMemories(p_424859_, this.target.pos, this.speedModifier, 0);
      }
   }

   private void startOnReachedTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget p_427600_, PathfinderMob p_425815_) {
      this.doReachedTargetInteraction(
         p_425815_,
         p_427600_.container,
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PICKUP_NO_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_ITEM),
         this.onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState.PLACE_NO_ITEM)
      );
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.INTERACTING);
   }

   private void onStartTravelling(PathfinderMob p_430410_) {
      this.onStartTravelling.accept(p_430410_);
      this.setTransportingState(TransportItemsBetweenContainers.TransportItemState.TRAVELLING);
      this.interactionState = null;
      this.ticksSinceReachingTarget = 0;
   }

   private BiConsumer<PathfinderMob, Container> onReachedInteraction(TransportItemsBetweenContainers.ContainerInteractionState p_425192_) {
      return (p_427688_, p_425484_) -> this.setInteractionState(p_425192_);
   }

   private void setTransportingState(TransportItemsBetweenContainers.TransportItemState p_430587_) {
      this.state = p_430587_;
   }

   private void setInteractionState(TransportItemsBetweenContainers.ContainerInteractionState p_428548_) {
      this.interactionState = p_428548_;
   }

   private void onTargetInteraction(TransportItemsBetweenContainers.TransportItemTarget p_429611_, PathfinderMob p_424978_) {
      p_424978_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(p_429611_.pos));
      this.stopInPlace(p_424978_);
      if (this.interactionState != null) {
         Optional.ofNullable(this.onTargetInteractionActions.get(this.interactionState))
            .ifPresent(p_426924_ -> p_426924_.accept(p_424978_, p_429611_, this.ticksSinceReachingTarget));
      }
   }

   private void doReachedTargetInteraction(
      PathfinderMob p_429612_,
      Container p_423430_,
      BiConsumer<PathfinderMob, Container> p_422762_,
      BiConsumer<PathfinderMob, Container> p_428229_,
      BiConsumer<PathfinderMob, Container> p_423271_,
      BiConsumer<PathfinderMob, Container> p_430791_
   ) {
      if (isPickingUpItems(p_429612_)) {
         if (matchesGettingItemsRequirement(p_423430_)) {
            p_422762_.accept(p_429612_, p_423430_);
         } else {
            p_428229_.accept(p_429612_, p_423430_);
         }
      } else if (matchesLeavingItemsRequirement(p_429612_, p_423430_)) {
         p_423271_.accept(p_429612_, p_423430_);
      } else {
         p_430791_.accept(p_429612_, p_423430_);
      }
   }

   private Optional<TransportItemsBetweenContainers.TransportItemTarget> getTransportTarget(ServerLevel p_426099_, PathfinderMob p_430780_) {
      AABB aabb = this.getTargetSearchArea(p_430780_);
      Set<GlobalPos> set = getVisitedPositions(p_430780_);
      Set<GlobalPos> set1 = getUnreachablePositions(p_430780_);
      List<ChunkPos> list = ChunkPos.rangeClosed(new ChunkPos(p_430780_.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(p_430780_), 16) + 1)
         .toList();
      TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = null;
      double d0 = Float.MAX_VALUE;

      for (ChunkPos chunkpos : list) {
         LevelChunk levelchunk = p_426099_.getChunkSource().getChunkNow(chunkpos.x, chunkpos.z);
         if (levelchunk != null) {
            for (BlockEntity blockentity : levelchunk.getBlockEntities().values()) {
               if (blockentity instanceof ChestBlockEntity chestblockentity) {
                  double d1 = chestblockentity.getBlockPos().distToCenterSqr(p_430780_.position());
                  if (d1 < d0) {
                     TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget1 = this.isTargetValidToPick(
                        p_430780_, p_426099_, chestblockentity, set, set1, aabb
                     );
                     if (transportitemsbetweencontainers$transportitemtarget1 != null) {
                        transportitemsbetweencontainers$transportitemtarget = transportitemsbetweencontainers$transportitemtarget1;
                        d0 = d1;
                     }
                  }
               }
            }
         }
      }

      return transportitemsbetweencontainers$transportitemtarget == null ? Optional.empty() : Optional.of(transportitemsbetweencontainers$transportitemtarget);
   }

   private TransportItemsBetweenContainers.@Nullable TransportItemTarget isTargetValidToPick(
      PathfinderMob p_423147_, Level p_425510_, BlockEntity p_426039_, Set<GlobalPos> p_424376_, Set<GlobalPos> p_429336_, AABB p_424224_
   ) {
      BlockPos blockpos = p_426039_.getBlockPos();
      boolean flag = p_424224_.contains(blockpos.getX(), blockpos.getY(), blockpos.getZ());
      if (!flag) {
         return null;
      }

      TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
         p_426039_, p_425510_
      );
      if (transportitemsbetweencontainers$transportitemtarget == null) {
         return null;
      }

      boolean flag1 = this.isWantedBlock(p_423147_, transportitemsbetweencontainers$transportitemtarget.state)
         && !this.isPositionAlreadyVisited(p_424376_, p_429336_, transportitemsbetweencontainers$transportitemtarget, p_425510_)
         && !this.isContainerLocked(transportitemsbetweencontainers$transportitemtarget);
      return flag1 ? transportitemsbetweencontainers$transportitemtarget : null;
   }

   private boolean isContainerLocked(TransportItemsBetweenContainers.TransportItemTarget p_428315_) {
      return p_428315_.blockEntity instanceof BaseContainerBlockEntity basecontainerblockentity && basecontainerblockentity.isLocked();
   }

   private boolean hasValidTarget(Level p_423114_, PathfinderMob p_426599_) {
      boolean flag = this.target != null && this.isWantedBlock(p_426599_, this.target.state) && this.targetHasNotChanged(p_423114_, this.target);
      if (flag && !this.isTargetBlocked(p_423114_, this.target)) {
         if (!this.state.equals(TransportItemsBetweenContainers.TransportItemState.TRAVELLING)) {
            return true;
         }

         if (this.hasValidTravellingPath(p_423114_, this.target, p_426599_)) {
            return true;
         }

         this.markVisitedBlockPosAsUnreachable(p_426599_, p_423114_, this.target.pos);
      }

      return false;
   }

   private boolean hasValidTravellingPath(Level p_431874_, TransportItemsBetweenContainers.TransportItemTarget p_431898_, PathfinderMob p_431868_) {
      Path path = p_431868_.getNavigation().getPath() == null ? p_431868_.getNavigation().createPath(p_431898_.pos, 0) : p_431868_.getNavigation().getPath();
      Vec3 vec3 = this.getPositionToReachTargetFrom(path, p_431868_);
      boolean flag = this.isWithinTargetDistance(getInteractionRange(p_431868_), p_431898_, p_431874_, p_431868_, vec3);
      boolean flag1 = path == null && !flag;
      return flag1 || this.targetIsReachableFromPosition(p_431874_, flag, vec3, p_431898_, p_431868_);
   }

   private Vec3 getPositionToReachTargetFrom(@Nullable Path p_426108_, PathfinderMob p_425120_) {
      boolean flag = p_426108_ == null || p_426108_.getEndNode() == null;
      Vec3 vec3 = flag ? p_425120_.position() : p_426108_.getEndNode().asBlockPos().getBottomCenter();
      return this.setMiddleYPosition(p_425120_, vec3);
   }

   private Vec3 setMiddleYPosition(PathfinderMob p_429844_, Vec3 p_425090_) {
      return p_425090_.add(0.0, p_429844_.getBoundingBox().getYsize() / 2.0, 0.0);
   }

   private boolean isTargetBlocked(Level p_422595_, TransportItemsBetweenContainers.TransportItemTarget p_424779_) {
      return ChestBlock.isChestBlockedAt(p_422595_, p_424779_.pos);
   }

   private boolean targetHasNotChanged(Level p_430472_, TransportItemsBetweenContainers.TransportItemTarget p_431475_) {
      return p_431475_.blockEntity.equals(p_430472_.getBlockEntity(p_431475_.pos));
   }

   private Stream<TransportItemsBetweenContainers.TransportItemTarget> getConnectedTargets(
      TransportItemsBetweenContainers.TransportItemTarget p_429991_, Level p_422587_
   ) {
      if (p_429991_.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
         TransportItemsBetweenContainers.TransportItemTarget transportitemsbetweencontainers$transportitemtarget = TransportItemsBetweenContainers.TransportItemTarget.tryCreatePossibleTarget(
            ChestBlock.getConnectedBlockPos(p_429991_.pos, p_429991_.state), p_422587_
         );
         return transportitemsbetweencontainers$transportitemtarget != null
            ? Stream.of(p_429991_, transportitemsbetweencontainers$transportitemtarget)
            : Stream.of(p_429991_);
      } else {
         return Stream.of(p_429991_);
      }
   }

   private AABB getTargetSearchArea(PathfinderMob p_428330_) {
      int i = this.getHorizontalSearchDistance(p_428330_);
      return new AABB(p_428330_.blockPosition()).inflate(i, this.getVerticalSearchDistance(p_428330_), i);
   }

   private int getHorizontalSearchDistance(PathfinderMob p_431290_) {
      return p_431290_.isPassenger() ? 1 : this.horizontalSearchDistance;
   }

   private int getVerticalSearchDistance(PathfinderMob p_431476_) {
      return p_431476_.isPassenger() ? 1 : this.verticalSearchDistance;
   }

   private static Set<GlobalPos> getVisitedPositions(PathfinderMob p_423632_) {
      return p_423632_.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
   }

   private static Set<GlobalPos> getUnreachablePositions(PathfinderMob p_425740_) {
      return p_425740_.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
   }

   private boolean isPositionAlreadyVisited(
      Set<GlobalPos> p_422913_, Set<GlobalPos> p_426081_, TransportItemsBetweenContainers.TransportItemTarget p_423166_, Level p_426127_
   ) {
      return this.getConnectedTargets(p_423166_, p_426127_)
         .map(p_422480_ -> new GlobalPos(p_426127_.dimension(), p_422480_.pos))
         .anyMatch(p_430238_ -> p_422913_.contains(p_430238_) || p_426081_.contains(p_430238_));
   }

   private static boolean hasFinishedPath(PathfinderMob p_422952_) {
      return p_422952_.getNavigation().getPath() != null && p_422952_.getNavigation().getPath().isDone();
   }

   protected void setVisitedBlockPos(PathfinderMob p_428904_, Level p_424578_, BlockPos p_430799_) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(p_428904_));
      set.add(new GlobalPos(p_424578_.dimension(), p_430799_));
      if (set.size() > 10) {
         this.enterCooldownAfterNoMatchingTargetFound(p_428904_);
      } else {
         p_428904_.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
      }
   }

   protected void markVisitedBlockPosAsUnreachable(PathfinderMob p_427338_, Level p_426597_, BlockPos p_428719_) {
      Set<GlobalPos> set = new HashSet<>(getVisitedPositions(p_427338_));
      set.remove(new GlobalPos(p_426597_.dimension(), p_428719_));
      Set<GlobalPos> set1 = new HashSet<>(getUnreachablePositions(p_427338_));
      set1.add(new GlobalPos(p_426597_.dimension(), p_428719_));
      if (set1.size() > 50) {
         this.enterCooldownAfterNoMatchingTargetFound(p_427338_);
      } else {
         p_427338_.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, set, 6000L);
         p_427338_.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, set1, 6000L);
      }
   }

   private boolean isWantedBlock(PathfinderMob p_430555_, BlockState p_427764_) {
      return isPickingUpItems(p_430555_) ? this.sourceBlockType.test(p_427764_) : this.destinationBlockType.test(p_427764_);
   }

   private static double getInteractionRange(PathfinderMob p_429333_) {
      return hasFinishedPath(p_429333_) ? 1.0 : 0.5;
   }

   private boolean isWithinTargetDistance(
      double p_425352_, TransportItemsBetweenContainers.TransportItemTarget p_428034_, Level p_429222_, PathfinderMob p_430342_, Vec3 p_422690_
   ) {
      AABB aabb = p_430342_.getBoundingBox();
      AABB aabb1 = AABB.ofSize(p_422690_, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
      return p_428034_.state.getCollisionShape(p_429222_, p_428034_.pos).bounds().inflate(p_425352_, 0.5, p_425352_).move(p_428034_.pos).intersects(aabb1);
   }

   private boolean targetIsReachableFromPosition(
      Level p_430308_, boolean p_422924_, Vec3 p_428497_, TransportItemsBetweenContainers.TransportItemTarget p_423186_, PathfinderMob p_427703_
   ) {
      return p_422924_ && this.canSeeAnyTargetSide(p_423186_, p_430308_, p_427703_, p_428497_);
   }

   private boolean canSeeAnyTargetSide(TransportItemsBetweenContainers.TransportItemTarget p_426777_, Level p_428844_, PathfinderMob p_425798_, Vec3 p_424847_) {
      Vec3 vec3 = p_426777_.pos.getCenter();
      return Direction.stream()
         .map(p_425993_ -> vec3.add(0.5 * p_425993_.getStepX(), 0.5 * p_425993_.getStepY(), 0.5 * p_425993_.getStepZ()))
         .map(p_424471_ -> p_428844_.clip(new ClipContext(p_424847_, p_424471_, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p_425798_)))
         .anyMatch(p_422369_ -> p_422369_.getType() == HitResult.Type.BLOCK && p_422369_.getBlockPos().equals(p_426777_.pos));
   }

   private boolean isAnotherMobInteractingWithTarget(TransportItemsBetweenContainers.TransportItemTarget p_429626_, Level p_425245_) {
      return this.getConnectedTargets(p_429626_, p_425245_).anyMatch(this.shouldQueueForTarget);
   }

   private static boolean isPickingUpItems(PathfinderMob p_428670_) {
      return p_428670_.getMainHandItem().isEmpty();
   }

   private static boolean matchesGettingItemsRequirement(Container p_427231_) {
      return !p_427231_.isEmpty();
   }

   private static boolean matchesLeavingItemsRequirement(PathfinderMob p_429049_, Container p_426778_) {
      return p_426778_.isEmpty() || hasItemMatchingHandItem(p_429049_, p_426778_);
   }

   private static boolean hasItemMatchingHandItem(PathfinderMob p_430719_, Container p_426288_) {
      ItemStack itemstack = p_430719_.getMainHandItem();

      for (ItemStack itemstack1 : p_426288_) {
         if (ItemStack.isSameItem(itemstack1, itemstack)) {
            return true;
         }
      }

      return false;
   }

   private void pickUpItems(PathfinderMob p_429859_, Container p_428102_) {
      p_429859_.setItemSlot(EquipmentSlot.MAINHAND, pickupItemFromContainer(p_428102_));
      p_429859_.setGuaranteedDrop(EquipmentSlot.MAINHAND);
      p_428102_.setChanged();
      this.clearMemoriesAfterMatchingTargetFound(p_429859_);
   }

   private void putDownItem(PathfinderMob p_429357_, Container p_429652_) {
      ItemStack itemstack = addItemsToContainer(p_429357_, p_429652_);
      p_429652_.setChanged();
      p_429357_.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
      if (itemstack.isEmpty()) {
         this.clearMemoriesAfterMatchingTargetFound(p_429357_);
      } else {
         this.stopTargetingCurrentTarget(p_429357_);
      }
   }

   private static ItemStack pickupItemFromContainer(Container p_422876_) {
      int i = 0;

      for (ItemStack itemstack : p_422876_) {
         if (!itemstack.isEmpty()) {
            int j = Math.min(itemstack.getCount(), 16);
            return p_422876_.removeItem(i, j);
         }

         i++;
      }

      return ItemStack.EMPTY;
   }

   private static ItemStack addItemsToContainer(PathfinderMob p_423175_, Container p_423503_) {
      int i = 0;
      ItemStack itemstack = p_423175_.getMainHandItem();

      for (ItemStack itemstack1 : p_423503_) {
         if (itemstack1.isEmpty()) {
            p_423503_.setItem(i, itemstack);
            return ItemStack.EMPTY;
         }

         if (ItemStack.isSameItemSameComponents(itemstack1, itemstack) && itemstack1.getCount() < itemstack1.getMaxStackSize()) {
            int j = itemstack1.getMaxStackSize() - itemstack1.getCount();
            int k = Math.min(j, itemstack.getCount());
            itemstack1.setCount(itemstack1.getCount() + k);
            itemstack.setCount(itemstack.getCount() - j);
            p_423503_.setItem(i, itemstack1);
            if (itemstack.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }

         i++;
      }

      return itemstack;
   }

   protected void stopTargetingCurrentTarget(PathfinderMob p_427621_) {
      this.ticksSinceReachingTarget = 0;
      this.target = null;
      p_427621_.getNavigation().stop();
      p_427621_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }

   protected void clearMemoriesAfterMatchingTargetFound(PathfinderMob p_428810_) {
      this.stopTargetingCurrentTarget(p_428810_);
      p_428810_.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      p_428810_.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   private void enterCooldownAfterNoMatchingTargetFound(PathfinderMob p_428759_) {
      this.stopTargetingCurrentTarget(p_428759_);
      p_428759_.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
      p_428759_.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
      p_428759_.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
   }

   protected void stop(ServerLevel p_428411_, PathfinderMob p_430299_, long p_430531_) {
      this.onStartTravelling(p_430299_);
      if (p_430299_.getNavigation() instanceof GroundPathNavigation groundpathnavigation) {
         groundpathnavigation.setCanPathToTargetsBelowSurface(false);
      }
   }

   private void stopInPlace(PathfinderMob p_430904_) {
      p_430904_.getNavigation().stop();
      p_430904_.setXxa(0.0F);
      p_430904_.setYya(0.0F);
      p_430904_.setSpeed(0.0F);
      p_430904_.setDeltaMovement(0.0, p_430904_.getDeltaMovement().y, 0.0);
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
      public static TransportItemsBetweenContainers.@Nullable TransportItemTarget tryCreatePossibleTarget(BlockEntity p_430951_, Level p_430204_) {
         BlockPos blockpos = p_430951_.getBlockPos();
         BlockState blockstate = p_430951_.getBlockState();
         Container container = getBlockEntityContainer(p_430951_, blockstate, p_430204_, blockpos);
         return container != null ? new TransportItemsBetweenContainers.TransportItemTarget(blockpos, container, p_430951_, blockstate) : null;
      }

      public static TransportItemsBetweenContainers.@Nullable TransportItemTarget tryCreatePossibleTarget(BlockPos p_422547_, Level p_431755_) {
         BlockEntity blockentity = p_431755_.getBlockEntity(p_422547_);
         return blockentity == null ? null : tryCreatePossibleTarget(blockentity, p_431755_);
      }

      private static @Nullable Container getBlockEntityContainer(BlockEntity p_430260_, BlockState p_425178_, Level p_424291_, BlockPos p_429679_) {
         if (p_425178_.getBlock() instanceof ChestBlock chestblock) {
            return ChestBlock.getContainer(chestblock, p_425178_, p_424291_, p_429679_, false);
         } else {
            return p_430260_ instanceof Container container ? container : null;
         }
      }
   }
}
