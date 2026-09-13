package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public class UpgradeData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final UpgradeData EMPTY = new UpgradeData(EmptyBlockGetter.INSTANCE);
   private static final String TAG_INDICES = "Indices";
   private static final Direction8[] DIRECTIONS = Direction8.values();
   private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec().orElse(Blocks.AIR)).listOf();
   private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec().orElse(Fluids.EMPTY)).listOf();
   private final EnumSet<Direction8> sides = EnumSet.noneOf(Direction8.class);
   private final List<SavedTick<Block>> neighborBlockTicks = Lists.newArrayList();
   private final List<SavedTick<Fluid>> neighborFluidTicks = Lists.newArrayList();
   private final int[][] index;
   static final Map<Block, UpgradeData.BlockFixer> MAP = new IdentityHashMap<>();
   static final Set<UpgradeData.BlockFixer> CHUNKY_FIXERS = Sets.newHashSet();

   private UpgradeData(LevelHeightAccessor p_156506_) {
      this.index = new int[p_156506_.getSectionsCount()][];
   }

   public UpgradeData(CompoundTag p_156508_, LevelHeightAccessor p_156509_) {
      this(p_156509_);
      p_156508_.getCompound("Indices").ifPresent(p_391007_ -> {
         for (int j = 0; j < this.index.length; j++) {
            this.index[j] = p_391007_.getIntArray(String.valueOf(j)).orElse(null);
         }
      });
      int i = p_156508_.getIntOr("Sides", 0);

      for (Direction8 direction8 : Direction8.values()) {
         if ((i & 1 << direction8.ordinal()) != 0) {
            this.sides.add(direction8);
         }
      }

      p_156508_.<Collection<? extends E>>read("neighbor_block_ticks", BLOCK_TICKS_CODEC).ifPresent(this.neighborBlockTicks::addAll);
      p_156508_.<Collection<? extends E>>read("neighbor_fluid_ticks", FLUID_TICKS_CODEC).ifPresent(this.neighborFluidTicks::addAll);
   }

   private UpgradeData(UpgradeData p_360816_) {
      this.sides.addAll(p_360816_.sides);
      this.neighborBlockTicks.addAll(p_360816_.neighborBlockTicks);
      this.neighborFluidTicks.addAll(p_360816_.neighborFluidTicks);
      this.index = new int[p_360816_.index.length][];

      for (int i = 0; i < p_360816_.index.length; i++) {
         int[] aint = p_360816_.index[i];
         this.index[i] = aint != null ? IntArrays.copy(aint) : null;
      }
   }

   public void upgrade(LevelChunk p_63342_) {
      this.upgradeInside(p_63342_);

      for (Direction8 direction8 : DIRECTIONS) {
         upgradeSides(p_63342_, direction8);
      }

      Level level = p_63342_.getLevel();
      this.neighborBlockTicks.forEach(p_208142_ -> {
         Block block = p_208142_.type() == Blocks.AIR ? level.getBlockState(p_208142_.pos()).getBlock() : p_208142_.type();
         level.scheduleTick(p_208142_.pos(), block, p_208142_.delay(), p_208142_.priority());
      });
      this.neighborFluidTicks.forEach(p_208125_ -> {
         Fluid fluid = p_208125_.type() == Fluids.EMPTY ? level.getFluidState(p_208125_.pos()).getType() : p_208125_.type();
         level.scheduleTick(p_208125_.pos(), fluid, p_208125_.delay(), p_208125_.priority());
      });
      CHUNKY_FIXERS.forEach(p_208122_ -> p_208122_.processChunk(level));
   }

   private static void upgradeSides(LevelChunk p_63344_, Direction8 p_63345_) {
      Level level = p_63344_.getLevel();
      if (p_63344_.getUpgradeData().sides.remove(p_63345_)) {
         Set<Direction> set = p_63345_.getDirections();
         int i = 0;
         int j = 15;
         boolean flag = set.contains(Direction.EAST);
         boolean flag1 = set.contains(Direction.WEST);
         boolean flag2 = set.contains(Direction.SOUTH);
         boolean flag3 = set.contains(Direction.NORTH);
         boolean flag4 = set.size() == 1;
         ChunkPos chunkpos = p_63344_.getPos();
         int k = chunkpos.getMinBlockX() + (!flag4 || !flag3 && !flag2 ? (flag1 ? 0 : 15) : 1);
         int l = chunkpos.getMinBlockX() + (!flag4 || !flag3 && !flag2 ? (flag1 ? 0 : 15) : 14);
         int i1 = chunkpos.getMinBlockZ() + (!flag4 || !flag && !flag1 ? (flag3 ? 0 : 15) : 1);
         int j1 = chunkpos.getMinBlockZ() + (!flag4 || !flag && !flag1 ? (flag3 ? 0 : 15) : 14);
         Direction[] adirection = Direction.values();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for (BlockPos blockpos : BlockPos.betweenClosed(k, level.getMinY(), i1, l, level.getMaxY(), j1)) {
            BlockState blockstate = level.getBlockState(blockpos);
            BlockState blockstate1 = blockstate;

            for (Direction direction : adirection) {
               blockpos$mutableblockpos.setWithOffset(blockpos, direction);
               blockstate1 = updateState(blockstate1, direction, level, blockpos, blockpos$mutableblockpos);
            }

            Block.updateOrDestroy(blockstate, blockstate1, level, blockpos, 18);
         }
      }
   }

   private static BlockState updateState(BlockState p_63336_, Direction p_63337_, LevelAccessor p_63338_, BlockPos p_63339_, BlockPos p_63340_) {
      return MAP.getOrDefault(p_63336_.getBlock(), UpgradeData.BlockFixers.DEFAULT)
         .updateShape(p_63336_, p_63337_, p_63338_.getBlockState(p_63340_), p_63338_, p_63339_, p_63340_);
   }

   private void upgradeInside(LevelChunk p_63348_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
      ChunkPos chunkpos = p_63348_.getPos();
      LevelAccessor levelaccessor = p_63348_.getLevel();

      for (int i = 0; i < this.index.length; i++) {
         LevelChunkSection levelchunksection = p_63348_.getSection(i);
         int[] aint = this.index[i];
         this.index[i] = null;
         if (aint != null && aint.length > 0) {
            Direction[] adirection = Direction.values();
            PalettedContainer<BlockState> palettedcontainer = levelchunksection.getStates();
            int j = p_63348_.getSectionYFromSectionIndex(i);
            int k = SectionPos.sectionToBlockCoord(j);

            for (int l : aint) {
               int i1 = l & 15;
               int j1 = l >> 8 & 15;
               int k1 = l >> 4 & 15;
               blockpos$mutableblockpos.set(chunkpos.getMinBlockX() + i1, k + j1, chunkpos.getMinBlockZ() + k1);
               BlockState blockstate = palettedcontainer.get(l);
               BlockState blockstate1 = blockstate;

               for (Direction direction : adirection) {
                  blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, direction);
                  if (SectionPos.blockToSectionCoord(blockpos$mutableblockpos.getX()) == chunkpos.x
                     && SectionPos.blockToSectionCoord(blockpos$mutableblockpos.getZ()) == chunkpos.z) {
                     blockstate1 = updateState(blockstate1, direction, levelaccessor, blockpos$mutableblockpos, blockpos$mutableblockpos1);
                  }
               }

               Block.updateOrDestroy(blockstate, blockstate1, levelaccessor, blockpos$mutableblockpos, 18);
            }
         }
      }

      for (int l1 = 0; l1 < this.index.length; l1++) {
         if (this.index[l1] != null) {
            LOGGER.warn(
               "Discarding update data for section {} for chunk ({} {})", new Object[]{levelaccessor.getSectionYFromSectionIndex(l1), chunkpos.x, chunkpos.z}
            );
         }

         this.index[l1] = null;
      }
   }

   public boolean isEmpty() {
      for (int[] aint : this.index) {
         if (aint != null) {
            return false;
         }
      }

      return this.sides.isEmpty();
   }

   public CompoundTag write() {
      CompoundTag compoundtag = new CompoundTag();
      CompoundTag compoundtag1 = new CompoundTag();

      for (int i = 0; i < this.index.length; i++) {
         String s = String.valueOf(i);
         if (this.index[i] != null && this.index[i].length != 0) {
            compoundtag1.putIntArray(s, this.index[i]);
         }
      }

      if (!compoundtag1.isEmpty()) {
         compoundtag.put("Indices", compoundtag1);
      }

      int j = 0;

      for (Direction8 direction8 : this.sides) {
         j |= 1 << direction8.ordinal();
      }

      compoundtag.putByte("Sides", (byte)j);
      if (!this.neighborBlockTicks.isEmpty()) {
         compoundtag.store("neighbor_block_ticks", BLOCK_TICKS_CODEC, this.neighborBlockTicks);
      }

      if (!this.neighborFluidTicks.isEmpty()) {
         compoundtag.store("neighbor_fluid_ticks", FLUID_TICKS_CODEC, this.neighborFluidTicks);
      }

      return compoundtag;
   }

   public UpgradeData copy() {
      return this == EMPTY ? EMPTY : new UpgradeData(this);
   }

   public interface BlockFixer {
      BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6);

      default void processChunk(LevelAccessor p_63351_) {
      }
   }

   enum BlockFixers implements UpgradeData.BlockFixer {
      BLACKLIST(
         Blocks.OBSERVER,
         Blocks.NETHER_PORTAL,
         Blocks.WHITE_CONCRETE_POWDER,
         Blocks.ORANGE_CONCRETE_POWDER,
         Blocks.MAGENTA_CONCRETE_POWDER,
         Blocks.LIGHT_BLUE_CONCRETE_POWDER,
         Blocks.YELLOW_CONCRETE_POWDER,
         Blocks.LIME_CONCRETE_POWDER,
         Blocks.PINK_CONCRETE_POWDER,
         Blocks.GRAY_CONCRETE_POWDER,
         Blocks.LIGHT_GRAY_CONCRETE_POWDER,
         Blocks.CYAN_CONCRETE_POWDER,
         Blocks.PURPLE_CONCRETE_POWDER,
         Blocks.BLUE_CONCRETE_POWDER,
         Blocks.BROWN_CONCRETE_POWDER,
         Blocks.GREEN_CONCRETE_POWDER,
         Blocks.RED_CONCRETE_POWDER,
         Blocks.BLACK_CONCRETE_POWDER,
         Blocks.ANVIL,
         Blocks.CHIPPED_ANVIL,
         Blocks.DAMAGED_ANVIL,
         Blocks.DRAGON_EGG,
         Blocks.GRAVEL,
         Blocks.SAND,
         Blocks.RED_SAND,
         Blocks.OAK_SIGN,
         Blocks.SPRUCE_SIGN,
         Blocks.BIRCH_SIGN,
         Blocks.ACACIA_SIGN,
         Blocks.CHERRY_SIGN,
         Blocks.JUNGLE_SIGN,
         Blocks.DARK_OAK_SIGN,
         Blocks.PALE_OAK_SIGN,
         Blocks.OAK_WALL_SIGN,
         Blocks.SPRUCE_WALL_SIGN,
         Blocks.BIRCH_WALL_SIGN,
         Blocks.ACACIA_WALL_SIGN,
         Blocks.JUNGLE_WALL_SIGN,
         Blocks.DARK_OAK_WALL_SIGN,
         Blocks.PALE_OAK_WALL_SIGN,
         Blocks.OAK_HANGING_SIGN,
         Blocks.SPRUCE_HANGING_SIGN,
         Blocks.BIRCH_HANGING_SIGN,
         Blocks.ACACIA_HANGING_SIGN,
         Blocks.JUNGLE_HANGING_SIGN,
         Blocks.DARK_OAK_HANGING_SIGN,
         Blocks.PALE_OAK_HANGING_SIGN,
         Blocks.OAK_WALL_HANGING_SIGN,
         Blocks.SPRUCE_WALL_HANGING_SIGN,
         Blocks.BIRCH_WALL_HANGING_SIGN,
         Blocks.ACACIA_WALL_HANGING_SIGN,
         Blocks.JUNGLE_WALL_HANGING_SIGN,
         Blocks.DARK_OAK_WALL_HANGING_SIGN,
         Blocks.PALE_OAK_WALL_HANGING_SIGN
      ) {
         @Override
         public BlockState updateShape(
            BlockState p_63394_, Direction p_63395_, BlockState p_63396_, LevelAccessor p_63397_, BlockPos p_63398_, BlockPos p_63399_
         ) {
            return p_63394_;
         }
      },
      DEFAULT {
         @Override
         public BlockState updateShape(
            BlockState p_63405_, Direction p_63406_, BlockState p_63407_, LevelAccessor p_63408_, BlockPos p_63409_, BlockPos p_63410_
         ) {
            return p_63405_.updateShape(p_63408_, p_63408_, p_63409_, p_63406_, p_63410_, p_63408_.getBlockState(p_63410_), p_63408_.getRandom());
         }
      },
      CHEST(Blocks.CHEST, Blocks.TRAPPED_CHEST) {
         @Override
         public BlockState updateShape(
            BlockState p_63416_, Direction p_63417_, BlockState p_63418_, LevelAccessor p_63419_, BlockPos p_63420_, BlockPos p_63421_
         ) {
            if (p_63418_.is(p_63416_.getBlock())
               && p_63417_.getAxis().isHorizontal()
               && p_63416_.getValue(ChestBlock.TYPE) == ChestType.SINGLE
               && p_63418_.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
               Direction direction = p_63416_.getValue(ChestBlock.FACING);
               if (p_63417_.getAxis() != direction.getAxis() && direction == p_63418_.getValue(ChestBlock.FACING)) {
                  ChestType chesttype = p_63417_ == direction.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
                  p_63419_.setBlock(p_63421_, p_63418_.setValue(ChestBlock.TYPE, chesttype.getOpposite()), 18);
                  if (direction == Direction.NORTH || direction == Direction.EAST) {
                     BlockEntity blockentity = p_63419_.getBlockEntity(p_63420_);
                     BlockEntity blockentity1 = p_63419_.getBlockEntity(p_63421_);
                     if (blockentity instanceof ChestBlockEntity && blockentity1 instanceof ChestBlockEntity) {
                        ChestBlockEntity.swapContents((ChestBlockEntity)blockentity, (ChestBlockEntity)blockentity1);
                     }
                  }

                  return p_63416_.setValue(ChestBlock.TYPE, chesttype);
               }
            }

            return p_63416_;
         }
      },
      LEAVES(
         true,
         Blocks.ACACIA_LEAVES,
         Blocks.CHERRY_LEAVES,
         Blocks.BIRCH_LEAVES,
         Blocks.PALE_OAK_LEAVES,
         Blocks.DARK_OAK_LEAVES,
         Blocks.JUNGLE_LEAVES,
         Blocks.OAK_LEAVES,
         Blocks.SPRUCE_LEAVES
      ) {
         private final ThreadLocal<List<ObjectSet<BlockPos>>> queue = ThreadLocal.withInitial(() -> Lists.newArrayListWithCapacity(7));

         @Override
         public BlockState updateShape(
            BlockState p_63432_, Direction p_63433_, BlockState p_63434_, LevelAccessor p_63435_, BlockPos p_63436_, BlockPos p_63437_
         ) {
            BlockState blockstate = p_63432_.updateShape(
               p_63435_, p_63435_, p_63436_, p_63433_, p_63437_, p_63435_.getBlockState(p_63437_), p_63435_.getRandom()
            );
            if (p_63432_ != blockstate) {
               int i = blockstate.getValue(BlockStateProperties.DISTANCE);
               List<ObjectSet<BlockPos>> list = this.queue.get();
               if (list.isEmpty()) {
                  for (int j = 0; j < 7; j++) {
                     list.add(new ObjectOpenHashSet());
                  }
               }

               list.get(i).add(p_63436_.immutable());
            }

            return p_63432_;
         }

         @Override
         public void processChunk(LevelAccessor p_63430_) {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            List<ObjectSet<BlockPos>> list = this.queue.get();

            for (int i = 2; i < list.size(); i++) {
               int j = i - 1;
               ObjectSet<BlockPos> objectset = list.get(j);
               ObjectSet<BlockPos> objectset1 = list.get(i);
               ObjectIterator var8 = objectset.iterator();

               while (var8.hasNext()) {
                  BlockPos blockpos = (BlockPos)var8.next();
                  BlockState blockstate = p_63430_.getBlockState(blockpos);
                  if (blockstate.getValue(BlockStateProperties.DISTANCE) >= j) {
                     p_63430_.setBlock(blockpos, blockstate.setValue(BlockStateProperties.DISTANCE, j), 18);
                     if (i != 7) {
                        for (Direction direction : DIRECTIONS) {
                           blockpos$mutableblockpos.setWithOffset(blockpos, direction);
                           BlockState blockstate1 = p_63430_.getBlockState(blockpos$mutableblockpos);
                           if (blockstate1.hasProperty(BlockStateProperties.DISTANCE) && blockstate.getValue(BlockStateProperties.DISTANCE) > i) {
                              objectset1.add(blockpos$mutableblockpos.immutable());
                           }
                        }
                     }
                  }
               }
            }

            list.clear();
         }
      },
      STEM_BLOCK(Blocks.MELON_STEM, Blocks.PUMPKIN_STEM) {
         @Override
         public BlockState updateShape(
            BlockState p_63443_, Direction p_63444_, BlockState p_63445_, LevelAccessor p_63446_, BlockPos p_63447_, BlockPos p_63448_
         ) {
            if (p_63443_.getValue(StemBlock.AGE) == 7) {
               Block block = p_63443_.is(Blocks.PUMPKIN_STEM) ? Blocks.PUMPKIN : Blocks.MELON;
               if (p_63445_.is(block)) {
                  return (p_63443_.is(Blocks.PUMPKIN_STEM) ? Blocks.ATTACHED_PUMPKIN_STEM : Blocks.ATTACHED_MELON_STEM)
                     .defaultBlockState()
                     .setValue(HorizontalDirectionalBlock.FACING, p_63444_);
               }
            }

            return p_63443_;
         }
      };

      public static final Direction[] DIRECTIONS = Direction.values();

      BlockFixers(final Block... p_63380_) {
         this(false, p_63380_);
      }

      BlockFixers(final boolean p_63369_, final Block... p_63370_) {
         for (Block block : p_63370_) {
            UpgradeData.MAP.put(block, this);
         }

         if (p_63369_) {
            UpgradeData.CHUNKY_FIXERS.add(this);
         }
      }
   }
}
