package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SculkSpreader {
   public static final int MAX_GROWTH_RATE_RADIUS = 24;
   public static final int MAX_CHARGE = 1000;
   public static final float MAX_DECAY_FACTOR = 0.5F;
   private static final int MAX_CURSORS = 32;
   public static final int SHRIEKER_PLACEMENT_RATE = 11;
   public static final int MAX_CURSOR_DISTANCE = 1024;
   private final boolean isWorldGeneration;
   private final TagKey<Block> replaceableBlocks;
   private final int growthSpawnCost;
   private final int noGrowthRadius;
   private final int chargeDecayRate;
   private final int additionalDecayRate;
   private List<SculkSpreader.ChargeCursor> cursors = new ArrayList<>();

   public SculkSpreader(
      final boolean isWorldGeneration,
      final TagKey<Block> replaceableBlocks,
      final int growthSpawnCost,
      final int noGrowthRadius,
      final int chargeDecayRate,
      final int additionalDecayRate
   ) {
      this.isWorldGeneration = isWorldGeneration;
      this.replaceableBlocks = replaceableBlocks;
      this.growthSpawnCost = growthSpawnCost;
      this.noGrowthRadius = noGrowthRadius;
      this.chargeDecayRate = chargeDecayRate;
      this.additionalDecayRate = additionalDecayRate;
   }

   public static SculkSpreader createLevelSpreader() {
      return new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
   }

   public static SculkSpreader createWorldGenSpreader() {
      return new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
   }

   public TagKey<Block> replaceableBlocks() {
      return this.replaceableBlocks;
   }

   public int growthSpawnCost() {
      return this.growthSpawnCost;
   }

   public int noGrowthRadius() {
      return this.noGrowthRadius;
   }

   public int chargeDecayRate() {
      return this.chargeDecayRate;
   }

   public int additionalDecayRate() {
      return this.additionalDecayRate;
   }

   public boolean isWorldGeneration() {
      return this.isWorldGeneration;
   }

   @VisibleForTesting
   public List<SculkSpreader.ChargeCursor> getCursors() {
      return this.cursors;
   }

   public void clear() {
      this.cursors.clear();
   }

   public void load(final ValueInput input) {
      this.cursors.clear();
      input.<List>read("cursors", SculkSpreader.ChargeCursor.CODEC.sizeLimitedListOf(32)).orElse(List.of()).forEach(this::addCursor);
   }

   public void save(final ValueOutput output) {
      output.store("cursors", SculkSpreader.ChargeCursor.CODEC.listOf(), this.cursors);
      if (SharedConstants.DEBUG_SCULK_CATALYST) {
         int charge = this.getCursors().stream().map(SculkSpreader.ChargeCursor::getCharge).reduce(0, Integer::sum);
         int charges = this.getCursors().stream().map(c -> 1).reduce(0, Integer::sum);
         int max = this.getCursors().stream().map(SculkSpreader.ChargeCursor::getCharge).reduce(0, Math::max);
         output.putInt("stats.total", charge);
         output.putInt("stats.count", charges);
         output.putInt("stats.max", max);
         output.putInt("stats.avg", charge / (charges + 1));
      }
   }

   public void addCursors(final BlockPos startPos, int charge) {
      while (charge > 0) {
         int currentCharge = Math.min(charge, 1000);
         this.addCursor(new SculkSpreader.ChargeCursor(startPos, currentCharge));
         charge -= currentCharge;
      }
   }

   private void addCursor(final SculkSpreader.ChargeCursor cursor) {
      if (this.cursors.size() < 32) {
         this.cursors.add(cursor);
      }
   }

   public void updateCursors(final LevelAccessor level, final BlockPos originPos, final RandomSource random, final boolean spreadVeins) {
      if (!this.cursors.isEmpty()) {
         List<SculkSpreader.ChargeCursor> processedCursors = new ArrayList<>();
         Map<BlockPos, SculkSpreader.ChargeCursor> mergeableCursors = new HashMap<>();
         Object2IntMap<BlockPos> chargeMap = new Object2IntOpenHashMap();

         for (SculkSpreader.ChargeCursor cursor : this.cursors) {
            if (!cursor.isPosUnreasonable(originPos)) {
               cursor.update(level, originPos, random, this, spreadVeins);
               if (cursor.charge <= 0) {
                  level.levelEvent(3006, cursor.getPos(), 0);
               } else {
                  BlockPos pos = cursor.getPos();
                  chargeMap.computeInt(pos, (k, count) -> (count == null ? 0 : count) + cursor.charge);
                  SculkSpreader.ChargeCursor existing = mergeableCursors.get(pos);
                  if (existing == null) {
                     mergeableCursors.put(pos, cursor);
                     processedCursors.add(cursor);
                  } else if (!this.isWorldGeneration() && cursor.charge + existing.charge <= 1000) {
                     existing.mergeWith(cursor);
                  } else {
                     processedCursors.add(cursor);
                     if (cursor.charge < existing.charge) {
                        mergeableCursors.put(pos, cursor);
                     }
                  }
               }
            }
         }

         ObjectIterator var16 = chargeMap.object2IntEntrySet().iterator();

         while (var16.hasNext()) {
            Entry<BlockPos> entry = (Entry<BlockPos>)var16.next();
            BlockPos pos = (BlockPos)entry.getKey();
            int charge = entry.getIntValue();
            SculkSpreader.ChargeCursor cursor = mergeableCursors.get(pos);
            Collection<Direction> faces = cursor == null ? null : cursor.getFacingData();
            if (charge > 0 && faces != null) {
               int numParticles = (int)(Math.log1p(charge) / 2.3F) + 1;
               int data = (numParticles << 6) + MultifaceBlock.pack(faces);
               level.levelEvent(3006, pos, data);
            }
         }

         this.cursors = processedCursors;
      }
   }

   public static class ChargeCursor {
      private static final ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS = Util.make(
         new ObjectArrayList(18),
         list -> BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
            .filter(position -> (position.getX() == 0 || position.getY() == 0 || position.getZ() == 0) && !position.equals(BlockPos.ZERO))
            .map(BlockPos::immutable)
            .forEach(list::add)
      );
      public static final int MAX_CURSOR_DECAY_DELAY = 1;
      private BlockPos pos;
      private int charge;
      private int updateDelay;
      private int decayDelay;
      private @Nullable Set<Direction> facings;
      private static final Codec<Set<Direction>> DIRECTION_SET = Direction.CODEC.listOf().xmap(l -> Sets.newEnumSet(l, Direction.class), Lists::newArrayList);
      public static final Codec<SculkSpreader.ChargeCursor> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(SculkSpreader.ChargeCursor::getPos),
               Codec.intRange(0, 1000).optionalFieldOf("charge", 0).forGetter(SculkSpreader.ChargeCursor::getCharge),
               Codec.intRange(0, 1).optionalFieldOf("decay_delay", 1).forGetter(SculkSpreader.ChargeCursor::getDecayDelay),
               Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("update_delay", 0).forGetter(o -> o.updateDelay),
               DIRECTION_SET.lenientOptionalFieldOf("facings").forGetter(o -> Optional.ofNullable(o.getFacingData()))
            )
            .apply(i, SculkSpreader.ChargeCursor::new)
      );

      private ChargeCursor(final BlockPos pos, final int charge, final int decayDelay, final int updateDelay, final Optional<Set<Direction>> facings) {
         this.pos = pos;
         this.charge = charge;
         this.decayDelay = decayDelay;
         this.updateDelay = updateDelay;
         this.facings = facings.orElse(null);
      }

      public ChargeCursor(final BlockPos pos, final int charge) {
         this(pos, charge, 1, 0, Optional.empty());
      }

      public BlockPos getPos() {
         return this.pos;
      }

      private boolean isPosUnreasonable(final BlockPos originPos) {
         return this.pos.distChessboard(originPos) > 1024;
      }

      public int getCharge() {
         return this.charge;
      }

      public int getDecayDelay() {
         return this.decayDelay;
      }

      public @Nullable Set<Direction> getFacingData() {
         return this.facings;
      }

      private boolean shouldUpdate(final LevelAccessor level, final BlockPos pos, final boolean isWorldGen) {
         if (this.charge <= 0) {
            return false;
         } else if (isWorldGen) {
            return true;
         } else {
            return level instanceof ServerLevel serverLevel ? serverLevel.shouldTickBlocksAt(pos) : false;
         }
      }

      public void update(
         final LevelAccessor level, final BlockPos originPos, final RandomSource random, final SculkSpreader spreader, final boolean spreadVeins
      ) {
         if (this.shouldUpdate(level, originPos, spreader.isWorldGeneration)) {
            if (this.updateDelay > 0) {
               this.updateDelay--;
            } else {
               BlockState currentState = level.getBlockState(this.pos);
               SculkBehaviour sculkBehaviour = getBlockBehaviour(currentState);
               if (spreadVeins && sculkBehaviour.attemptSpreadVein(level, this.pos, currentState, this.facings, spreader.isWorldGeneration())) {
                  if (sculkBehaviour.canChangeBlockStateOnSpread()) {
                     currentState = level.getBlockState(this.pos);
                     sculkBehaviour = getBlockBehaviour(currentState);
                  }

                  level.playSound(null, this.pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               this.charge = sculkBehaviour.attemptUseCharge(this, level, originPos, random, spreader, spreadVeins);
               if (this.charge <= 0) {
                  sculkBehaviour.onDischarged(level, currentState, this.pos, random);
               } else {
                  BlockPos transferPos = getValidMovementPos(level, this.pos, random);
                  if (transferPos != null) {
                     sculkBehaviour.onDischarged(level, currentState, this.pos, random);
                     this.pos = transferPos.immutable();
                     if (spreader.isWorldGeneration() && !this.pos.closerThan(new Vec3i(originPos.getX(), this.pos.getY(), originPos.getZ()), 15.0)) {
                        this.charge = 0;
                        return;
                     }

                     currentState = level.getBlockState(transferPos);
                  }

                  if (currentState.getBlock() instanceof SculkBehaviour) {
                     this.facings = MultifaceBlock.availableFaces(currentState);
                  }

                  this.decayDelay = sculkBehaviour.updateDecayDelay(this.decayDelay);
                  this.updateDelay = sculkBehaviour.getSculkSpreadDelay();
               }
            }
         }
      }

      private void mergeWith(final SculkSpreader.ChargeCursor other) {
         this.charge = this.charge + other.charge;
         other.charge = 0;
         this.updateDelay = Math.min(this.updateDelay, other.updateDelay);
      }

      private static SculkBehaviour getBlockBehaviour(final BlockState state) {
         return state.getBlock() instanceof SculkBehaviour behaviour ? behaviour : SculkBehaviour.DEFAULT;
      }

      private static List<Vec3i> getRandomizedNonCornerNeighbourOffsets(final RandomSource random) {
         return Util.shuffledCopy(NON_CORNER_NEIGHBOURS, random);
      }

      private static @Nullable BlockPos getValidMovementPos(final LevelAccessor level, final BlockPos pos, final RandomSource random) {
         BlockPos.MutableBlockPos sculkPosition = pos.mutable();
         BlockPos.MutableBlockPos neighbour = pos.mutable();

         for (Vec3i offset : getRandomizedNonCornerNeighbourOffsets(random)) {
            neighbour.setWithOffset(pos, offset);
            BlockState transferee = level.getBlockState(neighbour);
            if (transferee.getBlock() instanceof SculkBehaviour && isMovementUnobstructed(level, pos, neighbour)) {
               sculkPosition.set(neighbour);
               if (SculkVeinBlock.hasSubstrateAccess(level, transferee, neighbour)) {
                  break;
               }
            }
         }

         return sculkPosition.equals(pos) ? null : sculkPosition;
      }

      private static boolean isMovementUnobstructed(final LevelAccessor level, final BlockPos from, final BlockPos to) {
         if (from.distManhattan(to) == 1) {
            return true;
         } else {
            BlockPos delta = to.subtract(from);
            Direction directionX = Direction.fromAxisAndDirection(
               Direction.Axis.X, delta.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction directionY = Direction.fromAxisAndDirection(
               Direction.Axis.Y, delta.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            Direction directionZ = Direction.fromAxisAndDirection(
               Direction.Axis.Z, delta.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE
            );
            if (delta.getX() == 0) {
               return isUnobstructed(level, from, directionY) || isUnobstructed(level, from, directionZ);
            } else {
               return delta.getY() == 0
                  ? isUnobstructed(level, from, directionX) || isUnobstructed(level, from, directionZ)
                  : isUnobstructed(level, from, directionX) || isUnobstructed(level, from, directionY);
            }
         }
      }

      private static boolean isUnobstructed(final LevelAccessor level, final BlockPos from, final Direction direction) {
         BlockPos testPos = from.relative(direction);
         return !level.getBlockState(testPos).isFaceSturdy(level, testPos, direction.getOpposite());
      }
   }
}
