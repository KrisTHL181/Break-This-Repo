package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SnifferEggBlock extends Block {
   public static final MapCodec<SnifferEggBlock> CODEC = simpleCodec(SnifferEggBlock::new);
   public static final int MAX_HATCH_LEVEL = 2;
   public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
   private static final int REGULAR_HATCH_TIME_TICKS = 24000;
   private static final int BOOSTED_HATCH_TIME_TICKS = 12000;
   private static final int RANDOM_HATCH_OFFSET_TICKS = 300;
   private static final VoxelShape SHAPE = Block.column(14.0, 12.0, 0.0, 16.0);

   @Override
   public MapCodec<SnifferEggBlock> codec() {
      return CODEC;
   }

   public SnifferEggBlock(BlockBehaviour.Properties p_277906_) {
      super(p_277906_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_277441_) {
      p_277441_.add(HATCH);
   }

   @Override
   public VoxelShape getShape(BlockState p_277872_, BlockGetter p_278090_, BlockPos p_277364_, CollisionContext p_278016_) {
      return SHAPE;
   }

   public int getHatchLevel(BlockState p_279125_) {
      return p_279125_.getValue(HATCH);
   }

   private boolean isReadyToHatch(BlockState p_278021_) {
      return this.getHatchLevel(p_278021_) == 2;
   }

   @Override
   public void tick(BlockState p_277841_, ServerLevel p_277739_, BlockPos p_277692_, RandomSource p_277973_) {
      if (!this.isReadyToHatch(p_277841_)) {
         p_277739_.playSound(null, p_277692_, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + p_277973_.nextFloat() * 0.2F);
         p_277739_.setBlock(p_277692_, p_277841_.setValue(HATCH, this.getHatchLevel(p_277841_) + 1), 2);
      } else {
         p_277739_.playSound(null, p_277692_, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + p_277973_.nextFloat() * 0.2F);
         p_277739_.destroyBlock(p_277692_, false);
         Sniffer sniffer = EntityType.SNIFFER.create(p_277739_, EntitySpawnReason.BREEDING);
         if (sniffer != null) {
            Vec3 vec3 = p_277692_.getCenter();
            sniffer.setBaby(true);
            sniffer.snapTo(vec3.x(), vec3.y(), vec3.z(), Mth.wrapDegrees(p_277739_.random.nextFloat() * 360.0F), 0.0F);
            p_277739_.addFreshEntity(sniffer);
         }
      }
   }

   @Override
   public void onPlace(BlockState p_277964_, Level p_277827_, BlockPos p_277526_, BlockState p_277618_, boolean p_277819_) {
      boolean flag = hatchBoost(p_277827_, p_277526_);
      if (!p_277827_.isClientSide() && flag) {
         p_277827_.levelEvent(3009, p_277526_, 0);
      }

      int i = flag ? 12000 : 24000;
      int j = i / 3;
      p_277827_.gameEvent(GameEvent.BLOCK_PLACE, p_277526_, GameEvent.Context.of(p_277964_));
      p_277827_.scheduleTick(p_277526_, this, j + p_277827_.random.nextInt(300));
   }

   @Override
   public boolean isPathfindable(BlockState p_279414_, PathComputationType p_279299_) {
      return false;
   }

   public static boolean hatchBoost(BlockGetter p_277485_, BlockPos p_278065_) {
      return p_277485_.getBlockState(p_278065_.below()).is(BlockTags.SNIFFER_EGG_HATCH_BOOST);
   }
}
