package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ChestBlock extends AbstractChestBlock<ChestBlockEntity> implements SimpleWaterloggedBlock {
   public static final MapCodec<ChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422095_ -> p_422095_.group(
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
            propertiesCodec()
         )
         .apply(p_422095_, (p_422092_, p_422093_, p_422094_) -> new ChestBlock(() -> BlockEntityType.CHEST, p_422092_, p_422093_, p_422094_))
   );
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final int EVENT_SET_OPEN_COUNT = 1;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 14.0);
   private static final Map<Direction, VoxelShape> HALF_SHAPES = Shapes.rotateHorizontal(Block.boxZ(14.0, 0.0, 14.0, 0.0, 15.0));
   private final SoundEvent openSound;
   private final SoundEvent closeSound;
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>> CHEST_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<Container>>() {
      public Optional<Container> acceptDouble(ChestBlockEntity p_51591_, ChestBlockEntity p_51592_) {
         return Optional.of(new CompoundContainer(p_51591_, p_51592_));
      }

      public Optional<Container> acceptSingle(ChestBlockEntity p_51589_) {
         return Optional.of(p_51589_);
      }

      public Optional<Container> acceptNone() {
         return Optional.empty();
      }
   };
   private static final DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER = new DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>>() {
      public Optional<MenuProvider> acceptDouble(final ChestBlockEntity p_51604_, final ChestBlockEntity p_51605_) {
         final Container container = new CompoundContainer(p_51604_, p_51605_);
         return Optional.of(new MenuProvider() {
            @Override
            public @Nullable AbstractContainerMenu createMenu(int p_51622_, Inventory p_51623_, Player p_51624_) {
               if (p_51604_.canOpen(p_51624_) && p_51605_.canOpen(p_51624_)) {
                  p_51604_.unpackLootTable(p_51623_.player);
                  p_51605_.unpackLootTable(p_51623_.player);
                  return ChestMenu.sixRows(p_51622_, p_51623_, container);
               } else {
                  Direction direction = ChestBlock.getConnectedDirection(p_51604_.getBlockState());
                  Vec3 vec3 = p_51604_.getBlockPos().getCenter();
                  Vec3 vec31 = vec3.add(direction.getStepX() / 2.0, 0.0, direction.getStepZ() / 2.0);
                  BaseContainerBlockEntity.sendChestLockedNotifications(vec31, p_51624_, this.getDisplayName());
                  return null;
               }
            }

            @Override
            public Component getDisplayName() {
               if (p_51604_.hasCustomName()) {
                  return p_51604_.getDisplayName();
               } else {
                  return p_51605_.hasCustomName() ? p_51605_.getDisplayName() : Component.translatable("container.chestDouble");
               }
            }
         });
      }

      public Optional<MenuProvider> acceptSingle(ChestBlockEntity p_51602_) {
         return Optional.of(p_51602_);
      }

      public Optional<MenuProvider> acceptNone() {
         return Optional.empty();
      }
   };

   @Override
   public MapCodec<? extends ChestBlock> codec() {
      return CODEC;
   }

   protected ChestBlock(
      Supplier<BlockEntityType<? extends ChestBlockEntity>> p_51491_, SoundEvent p_428979_, SoundEvent p_427906_, BlockBehaviour.Properties p_51490_
   ) {
      super(p_51490_, p_51491_);
      this.openSound = p_428979_;
      this.closeSound = p_427906_;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, ChestType.SINGLE).setValue(WATERLOGGED, false));
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState p_51583_) {
      ChestType chesttype = p_51583_.getValue(TYPE);
      if (chesttype == ChestType.SINGLE) {
         return DoubleBlockCombiner.BlockType.SINGLE;
      } else {
         return chesttype == ChestType.RIGHT ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51555_,
      LevelReader p_362695_,
      ScheduledTickAccess p_362061_,
      BlockPos p_51559_,
      Direction p_51556_,
      BlockPos p_51560_,
      BlockState p_51557_,
      RandomSource p_368899_
   ) {
      if (p_51555_.getValue(WATERLOGGED)) {
         p_362061_.scheduleTick(p_51559_, Fluids.WATER, Fluids.WATER.getTickDelay(p_362695_));
      }

      if (this.chestCanConnectTo(p_51557_) && p_51556_.getAxis().isHorizontal()) {
         ChestType chesttype = p_51557_.getValue(TYPE);
         if (p_51555_.getValue(TYPE) == ChestType.SINGLE
            && chesttype != ChestType.SINGLE
            && p_51555_.getValue(FACING) == p_51557_.getValue(FACING)
            && getConnectedDirection(p_51557_) == p_51556_.getOpposite()) {
            return p_51555_.setValue(TYPE, chesttype.getOpposite());
         }
      } else if (getConnectedDirection(p_51555_) == p_51556_) {
         return p_51555_.setValue(TYPE, ChestType.SINGLE);
      }

      return super.updateShape(p_51555_, p_362695_, p_362061_, p_51559_, p_51556_, p_51560_, p_51557_, p_368899_);
   }

   public boolean chestCanConnectTo(BlockState p_426203_) {
      return p_426203_.is(this);
   }

   @Override
   protected VoxelShape getShape(BlockState p_51569_, BlockGetter p_51570_, BlockPos p_51571_, CollisionContext p_51572_) {
      return switch ((ChestType)p_51569_.getValue(TYPE)) {
         case SINGLE -> SHAPE;
         case LEFT, RIGHT -> (VoxelShape)HALF_SHAPES.get(getConnectedDirection(p_51569_));
      };
   }

   public static Direction getConnectedDirection(BlockState p_51585_) {
      Direction direction = p_51585_.getValue(FACING);
      return p_51585_.getValue(TYPE) == ChestType.LEFT ? direction.getClockWise() : direction.getCounterClockWise();
   }

   public static BlockPos getConnectedBlockPos(BlockPos p_422294_, BlockState p_431255_) {
      Direction direction = getConnectedDirection(p_431255_);
      return p_422294_.relative(direction);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51493_) {
      ChestType chesttype = ChestType.SINGLE;
      Direction direction = p_51493_.getHorizontalDirection().getOpposite();
      FluidState fluidstate = p_51493_.getLevel().getFluidState(p_51493_.getClickedPos());
      boolean flag = p_51493_.isSecondaryUseActive();
      Direction direction1 = p_51493_.getClickedFace();
      if (direction1.getAxis().isHorizontal() && flag) {
         Direction direction2 = this.candidatePartnerFacing(p_51493_.getLevel(), p_51493_.getClickedPos(), direction1.getOpposite());
         if (direction2 != null && direction2.getAxis() != direction1.getAxis()) {
            direction = direction2;
            chesttype = direction2.getCounterClockWise() == direction1.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
         }
      }

      if (chesttype == ChestType.SINGLE && !flag) {
         chesttype = this.getChestType(p_51493_.getLevel(), p_51493_.getClickedPos(), direction);
      }

      return this.defaultBlockState().setValue(FACING, direction).setValue(TYPE, chesttype).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
   }

   protected ChestType getChestType(Level p_429175_, BlockPos p_425036_, Direction p_431565_) {
      if (p_431565_ == this.candidatePartnerFacing(p_429175_, p_425036_, p_431565_.getClockWise())) {
         return ChestType.LEFT;
      } else {
         return p_431565_ == this.candidatePartnerFacing(p_429175_, p_425036_, p_431565_.getCounterClockWise()) ? ChestType.RIGHT : ChestType.SINGLE;
      }
   }

   @Override
   protected FluidState getFluidState(BlockState p_51581_) {
      return p_51581_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_51581_);
   }

   private @Nullable Direction candidatePartnerFacing(Level p_430310_, BlockPos p_423687_, Direction p_51496_) {
      BlockState blockstate = p_430310_.getBlockState(p_423687_.relative(p_51496_));
      return this.chestCanConnectTo(blockstate) && blockstate.getValue(TYPE) == ChestType.SINGLE ? blockstate.getValue(FACING) : null;
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_397064_, ServerLevel p_396255_, BlockPos p_393735_, boolean p_392386_) {
      Containers.updateNeighboursAfterDestroy(p_397064_, p_396255_, p_393735_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_51531_, Level p_51532_, BlockPos p_51533_, Player p_51534_, BlockHitResult p_51536_) {
      if (p_51532_ instanceof ServerLevel serverlevel) {
         MenuProvider menuprovider = this.getMenuProvider(p_51531_, p_51532_, p_51533_);
         if (menuprovider != null) {
            p_51534_.openMenu(menuprovider);
            p_51534_.awardStat(this.getOpenChestStat());
            PiglinAi.angerNearbyPiglins(serverlevel, p_51534_, true);
         }
      }

      return InteractionResult.SUCCESS;
   }

   protected Stat<Identifier> getOpenChestStat() {
      return Stats.CUSTOM.get(Stats.OPEN_CHEST);
   }

   public BlockEntityType<? extends ChestBlockEntity> blockEntityType() {
      return this.blockEntityType.get();
   }

   public static @Nullable Container getContainer(ChestBlock p_51512_, BlockState p_51513_, Level p_51514_, BlockPos p_51515_, boolean p_51516_) {
      return p_51512_.combine(p_51513_, p_51514_, p_51515_, p_51516_).apply(CHEST_COMBINER).orElse(null);
   }

   @Override
   public DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(
      BlockState p_51544_, Level p_51545_, BlockPos p_51546_, boolean p_51547_
   ) {
      BiPredicate<LevelAccessor, BlockPos> bipredicate;
      if (p_51547_) {
         bipredicate = (p_51578_, p_51579_) -> false;
      } else {
         bipredicate = ChestBlock::isChestBlockedAt;
      }

      return DoubleBlockCombiner.combineWithNeigbour(
         this.blockEntityType.get(), ChestBlock::getBlockType, ChestBlock::getConnectedDirection, FACING, p_51544_, p_51545_, p_51546_, bipredicate
      );
   }

   @Override
   protected @Nullable MenuProvider getMenuProvider(BlockState p_51574_, Level p_51575_, BlockPos p_51576_) {
      return this.combine(p_51574_, p_51575_, p_51576_, false).apply(MENU_PROVIDER_COMBINER).orElse(null);
   }

   public static DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction> opennessCombiner(final LidBlockEntity p_51518_) {
      return new DoubleBlockCombiner.Combiner<ChestBlockEntity, Float2FloatFunction>() {
         public Float2FloatFunction acceptDouble(ChestBlockEntity p_51633_, ChestBlockEntity p_51634_) {
            return p_51638_ -> Math.max(p_51633_.getOpenNess(p_51638_), p_51634_.getOpenNess(p_51638_));
         }

         public Float2FloatFunction acceptSingle(ChestBlockEntity p_51631_) {
            return p_51631_::getOpenNess;
         }

         public Float2FloatFunction acceptNone() {
            return p_51518_::getOpenNess;
         }
      };
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153064_, BlockState p_153065_) {
      return new ChestBlockEntity(p_153064_, p_153065_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_153055_, BlockState p_153056_, BlockEntityType<T> p_153057_) {
      return p_153055_.isClientSide() ? createTickerHelper(p_153057_, this.blockEntityType(), ChestBlockEntity::lidAnimateTick) : null;
   }

   public static boolean isChestBlockedAt(LevelAccessor p_51509_, BlockPos p_51510_) {
      return isBlockedChestByBlock(p_51509_, p_51510_) || isCatSittingOnChest(p_51509_, p_51510_);
   }

   private static boolean isBlockedChestByBlock(BlockGetter p_51500_, BlockPos p_51501_) {
      BlockPos blockpos = p_51501_.above();
      return p_51500_.getBlockState(blockpos).isRedstoneConductor(p_51500_, blockpos);
   }

   private static boolean isCatSittingOnChest(LevelAccessor p_51564_, BlockPos p_51565_) {
      List<Cat> list = p_51564_.getEntitiesOfClass(
         Cat.class, new AABB(p_51565_.getX(), p_51565_.getY() + 1, p_51565_.getZ(), p_51565_.getX() + 1, p_51565_.getY() + 2, p_51565_.getZ() + 1)
      );
      if (!list.isEmpty()) {
         for (Cat cat : list) {
            if (cat.isInSittingPose()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_51520_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_51527_, Level p_51528_, BlockPos p_51529_, Direction p_430492_) {
      return AbstractContainerMenu.getRedstoneSignalFromContainer(getContainer(this, p_51527_, p_51528_, p_51529_, false));
   }

   @Override
   protected BlockState rotate(BlockState p_51552_, Rotation p_51553_) {
      return p_51552_.setValue(FACING, p_51553_.rotate(p_51552_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_51549_, Mirror p_51550_) {
      return p_51549_.rotate(p_51550_.getRotation(p_51549_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51562_) {
      p_51562_.add(FACING, TYPE, WATERLOGGED);
   }

   @Override
   protected boolean isPathfindable(BlockState p_51522_, PathComputationType p_51525_) {
      return false;
   }

   @Override
   protected void tick(BlockState p_220958_, ServerLevel p_220959_, BlockPos p_220960_, RandomSource p_220961_) {
      BlockEntity blockentity = p_220959_.getBlockEntity(p_220960_);
      if (blockentity instanceof ChestBlockEntity) {
         ((ChestBlockEntity)blockentity).recheckOpen();
      }
   }

   public SoundEvent getOpenChestSound() {
      return this.openSound;
   }

   public SoundEvent getCloseChestSound() {
      return this.closeSound;
   }
}
