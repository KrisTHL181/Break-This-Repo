package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CarvedPumpkinBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<CarvedPumpkinBlock> CODEC = simpleCodec(CarvedPumpkinBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   private @Nullable BlockPattern snowGolemBase;
   private @Nullable BlockPattern snowGolemFull;
   private @Nullable BlockPattern ironGolemBase;
   private @Nullable BlockPattern ironGolemFull;
   private @Nullable BlockPattern copperGolemBase;
   private @Nullable BlockPattern copperGolemFull;
   private static final Predicate<BlockState> PUMPKINS_PREDICATE = p_449891_ -> p_449891_.is(Blocks.CARVED_PUMPKIN) || p_449891_.is(Blocks.JACK_O_LANTERN);

   @Override
   public MapCodec<? extends CarvedPumpkinBlock> codec() {
      return CODEC;
   }

   protected CarvedPumpkinBlock(BlockBehaviour.Properties p_51375_) {
      super(p_51375_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   protected void onPlace(BlockState p_51387_, Level p_51388_, BlockPos p_51389_, BlockState p_51390_, boolean p_51391_) {
      if (!p_51390_.is(p_51387_.getBlock())) {
         this.trySpawnGolem(p_51388_, p_51389_);
      }
   }

   public boolean canSpawnGolem(LevelReader p_51382_, BlockPos p_51383_) {
      return this.getOrCreateSnowGolemBase().find(p_51382_, p_51383_) != null
         || this.getOrCreateIronGolemBase().find(p_51382_, p_51383_) != null
         || this.getOrCreateCopperGolemBase().find(p_51382_, p_51383_) != null;
   }

   private void trySpawnGolem(Level p_51379_, BlockPos p_51380_) {
      BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch = this.getOrCreateSnowGolemFull().find(p_51379_, p_51380_);
      if (blockpattern$blockpatternmatch != null) {
         SnowGolem snowgolem = EntityType.SNOW_GOLEM.create(p_51379_, EntitySpawnReason.TRIGGERED);
         if (snowgolem != null) {
            spawnGolemInWorld(p_51379_, blockpattern$blockpatternmatch, snowgolem, blockpattern$blockpatternmatch.getBlock(0, 2, 0).getPos());
            return;
         }
      }

      BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch1 = this.getOrCreateIronGolemFull().find(p_51379_, p_51380_);
      if (blockpattern$blockpatternmatch1 != null) {
         IronGolem irongolem = EntityType.IRON_GOLEM.create(p_51379_, EntitySpawnReason.TRIGGERED);
         if (irongolem != null) {
            irongolem.setPlayerCreated(true);
            spawnGolemInWorld(p_51379_, blockpattern$blockpatternmatch1, irongolem, blockpattern$blockpatternmatch1.getBlock(1, 2, 0).getPos());
            return;
         }
      }

      BlockPattern.BlockPatternMatch blockpattern$blockpatternmatch2 = this.getOrCreateCopperGolemFull().find(p_51379_, p_51380_);
      if (blockpattern$blockpatternmatch2 != null) {
         CopperGolem coppergolem = EntityType.COPPER_GOLEM.create(p_51379_, EntitySpawnReason.TRIGGERED);
         if (coppergolem != null) {
            spawnGolemInWorld(p_51379_, blockpattern$blockpatternmatch2, coppergolem, blockpattern$blockpatternmatch2.getBlock(0, 0, 0).getPos());
            this.replaceCopperBlockWithChest(p_51379_, blockpattern$blockpatternmatch2);
            coppergolem.spawn(this.getWeatherStateFromPattern(blockpattern$blockpatternmatch2));
         }
      }
   }

   private WeatheringCopper.WeatherState getWeatherStateFromPattern(BlockPattern.BlockPatternMatch p_431546_) {
      BlockState blockstate = p_431546_.getBlock(0, 1, 0).getState();
      return blockstate.getBlock() instanceof WeatheringCopper weatheringcopper
         ? weatheringcopper.getAge()
         : Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(blockstate.getBlock()))
            .filter(p_422086_ -> p_422086_ instanceof WeatheringCopper)
            .map(p_422084_ -> (WeatheringCopper)p_422084_)
            .orElse((WeatheringCopper)Blocks.COPPER_BLOCK)
            .getAge();
   }

   private static void spawnGolemInWorld(Level p_249110_, BlockPattern.BlockPatternMatch p_251293_, Entity p_251251_, BlockPos p_251189_) {
      clearPatternBlocks(p_249110_, p_251293_);
      p_251251_.snapTo(p_251189_.getX() + 0.5, p_251189_.getY() + 0.05, p_251189_.getZ() + 0.5, 0.0F, 0.0F);
      p_249110_.addFreshEntity(p_251251_);

      for (ServerPlayer serverplayer : p_249110_.getEntitiesOfClass(ServerPlayer.class, p_251251_.getBoundingBox().inflate(5.0))) {
         CriteriaTriggers.SUMMONED_ENTITY.trigger(serverplayer, p_251251_);
      }

      updatePatternBlocks(p_249110_, p_251293_);
   }

   public static void clearPatternBlocks(Level p_249604_, BlockPattern.BlockPatternMatch p_251190_) {
      for (int i = 0; i < p_251190_.getWidth(); i++) {
         for (int j = 0; j < p_251190_.getHeight(); j++) {
            BlockInWorld blockinworld = p_251190_.getBlock(i, j, 0);
            p_249604_.setBlock(blockinworld.getPos(), Blocks.AIR.defaultBlockState(), 2);
            p_249604_.levelEvent(2001, blockinworld.getPos(), Block.getId(blockinworld.getState()));
         }
      }
   }

   public static void updatePatternBlocks(Level p_248711_, BlockPattern.BlockPatternMatch p_251935_) {
      for (int i = 0; i < p_251935_.getWidth(); i++) {
         for (int j = 0; j < p_251935_.getHeight(); j++) {
            BlockInWorld blockinworld = p_251935_.getBlock(i, j, 0);
            p_248711_.updateNeighborsAt(blockinworld.getPos(), Blocks.AIR);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51377_) {
      return this.defaultBlockState().setValue(FACING, p_51377_.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51385_) {
      p_51385_.add(FACING);
   }

   private BlockPattern getOrCreateSnowGolemBase() {
      if (this.snowGolemBase == null) {
         this.snowGolemBase = BlockPatternBuilder.start()
            .aisle(" ", "#", "#")
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
            .build();
      }

      return this.snowGolemBase;
   }

   private BlockPattern getOrCreateSnowGolemFull() {
      if (this.snowGolemFull == null) {
         this.snowGolemFull = BlockPatternBuilder.start()
            .aisle("^", "#", "#")
            .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SNOW_BLOCK)))
            .build();
      }

      return this.snowGolemFull;
   }

   private BlockPattern getOrCreateIronGolemBase() {
      if (this.ironGolemBase == null) {
         this.ironGolemBase = BlockPatternBuilder.start()
            .aisle("~ ~", "###", "~#~")
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
            .where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
            .build();
      }

      return this.ironGolemBase;
   }

   private BlockPattern getOrCreateIronGolemFull() {
      if (this.ironGolemFull == null) {
         this.ironGolemFull = BlockPatternBuilder.start()
            .aisle("~^~", "###", "~#~")
            .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
            .where('#', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
            .where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
            .build();
      }

      return this.ironGolemFull;
   }

   private BlockPattern getOrCreateCopperGolemBase() {
      if (this.copperGolemBase == null) {
         this.copperGolemBase = BlockPatternBuilder.start()
            .aisle(" ", "#")
            .where('#', BlockInWorld.hasState(p_422085_ -> p_422085_.is(BlockTags.COPPER)))
            .build();
      }

      return this.copperGolemBase;
   }

   private BlockPattern getOrCreateCopperGolemFull() {
      if (this.copperGolemFull == null) {
         this.copperGolemFull = BlockPatternBuilder.start()
            .aisle("^", "#")
            .where('^', BlockInWorld.hasState(PUMPKINS_PREDICATE))
            .where('#', BlockInWorld.hasState(p_422087_ -> p_422087_.is(BlockTags.COPPER)))
            .build();
      }

      return this.copperGolemFull;
   }

   public void replaceCopperBlockWithChest(Level p_426310_, BlockPattern.BlockPatternMatch p_428424_) {
      BlockInWorld blockinworld = p_428424_.getBlock(0, 1, 0);
      BlockInWorld blockinworld1 = p_428424_.getBlock(0, 0, 0);
      Direction direction = blockinworld1.getState().getValue(FACING);
      BlockState blockstate = CopperChestBlock.getFromCopperBlock(blockinworld.getState().getBlock(), direction, p_426310_, blockinworld.getPos());
      p_426310_.setBlock(blockinworld.getPos(), blockstate, 2);
   }
}
