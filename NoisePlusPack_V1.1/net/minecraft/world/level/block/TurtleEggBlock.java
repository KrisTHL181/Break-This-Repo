package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TurtleEggBlock extends Block {
   public static final MapCodec<TurtleEggBlock> CODEC = simpleCodec(TurtleEggBlock::new);
   public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
   public static final IntegerProperty EGGS = BlockStateProperties.EGGS;
   public static final int MAX_HATCH_LEVEL = 2;
   public static final int MIN_EGGS = 1;
   public static final int MAX_EGGS = 4;
   private static final VoxelShape SHAPE_SINGLE = Block.box(3.0, 0.0, 3.0, 12.0, 7.0, 12.0);
   private static final VoxelShape SHAPE_MULTIPLE = Block.column(14.0, 0.0, 7.0);

   @Override
   public MapCodec<TurtleEggBlock> codec() {
      return CODEC;
   }

   public TurtleEggBlock(BlockBehaviour.Properties p_57759_) {
      super(p_57759_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0).setValue(EGGS, 1));
   }

   @Override
   public void stepOn(Level p_154857_, BlockPos p_154858_, BlockState p_154859_, Entity p_154860_) {
      if (!p_154860_.isSteppingCarefully()) {
         this.destroyEgg(p_154857_, p_154859_, p_154858_, p_154860_, 100);
      }

      super.stepOn(p_154857_, p_154858_, p_154859_, p_154860_);
   }

   @Override
   public void fallOn(Level p_154845_, BlockState p_154846_, BlockPos p_154847_, Entity p_154848_, double p_392306_) {
      if (!(p_154848_ instanceof Zombie)) {
         this.destroyEgg(p_154845_, p_154846_, p_154847_, p_154848_, 3);
      }

      super.fallOn(p_154845_, p_154846_, p_154847_, p_154848_, p_392306_);
   }

   private void destroyEgg(Level p_154851_, BlockState p_154852_, BlockPos p_154853_, Entity p_154854_, int p_154855_) {
      if (p_154852_.is(Blocks.TURTLE_EGG)
         && p_154851_ instanceof ServerLevel serverlevel
         && this.canDestroyEgg(serverlevel, p_154854_)
         && p_154851_.random.nextInt(p_154855_) == 0) {
         this.decreaseEggs(serverlevel, p_154853_, p_154852_);
      }
   }

   private void decreaseEggs(Level p_57792_, BlockPos p_57793_, BlockState p_57794_) {
      p_57792_.playSound(null, p_57793_, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + p_57792_.random.nextFloat() * 0.2F);
      int i = p_57794_.getValue(EGGS);
      if (i <= 1) {
         p_57792_.destroyBlock(p_57793_, false);
      } else {
         p_57792_.setBlock(p_57793_, p_57794_.setValue(EGGS, i - 1), 2);
         p_57792_.gameEvent(GameEvent.BLOCK_DESTROY, p_57793_, GameEvent.Context.of(p_57794_));
         p_57792_.levelEvent(2001, p_57793_, Block.getId(p_57794_));
      }
   }

   @Override
   protected void randomTick(BlockState p_222644_, ServerLevel p_222645_, BlockPos p_222646_, RandomSource p_222647_) {
      if (this.shouldUpdateHatchLevel(p_222645_, p_222646_) && onSand(p_222645_, p_222646_)) {
         int i = p_222644_.getValue(HATCH);
         if (i < 2) {
            p_222645_.playSound(null, p_222646_, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7F, 0.9F + p_222647_.nextFloat() * 0.2F);
            p_222645_.setBlock(p_222646_, p_222644_.setValue(HATCH, i + 1), 2);
            p_222645_.gameEvent(GameEvent.BLOCK_CHANGE, p_222646_, GameEvent.Context.of(p_222644_));
         } else {
            p_222645_.playSound(null, p_222646_, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.7F, 0.9F + p_222647_.nextFloat() * 0.2F);
            p_222645_.removeBlock(p_222646_, false);
            p_222645_.gameEvent(GameEvent.BLOCK_DESTROY, p_222646_, GameEvent.Context.of(p_222644_));

            for (int j = 0; j < p_222644_.getValue(EGGS); j++) {
               p_222645_.levelEvent(2001, p_222646_, Block.getId(p_222644_));
               Turtle turtle = EntityType.TURTLE.create(p_222645_, EntitySpawnReason.BREEDING);
               if (turtle != null) {
                  turtle.setAge(-24000);
                  turtle.setHomePos(p_222646_);
                  turtle.snapTo(p_222646_.getX() + 0.3 + j * 0.2, p_222646_.getY(), p_222646_.getZ() + 0.3, 0.0F, 0.0F);
                  p_222645_.addFreshEntity(turtle);
               }
            }
         }
      }
   }

   public static boolean onSand(BlockGetter p_57763_, BlockPos p_57764_) {
      return isSand(p_57763_, p_57764_.below());
   }

   public static boolean isSand(BlockGetter p_57801_, BlockPos p_57802_) {
      return p_57801_.getBlockState(p_57802_).is(BlockTags.SAND);
   }

   @Override
   protected void onPlace(BlockState p_57814_, Level p_57815_, BlockPos p_57816_, BlockState p_57817_, boolean p_57818_) {
      if (onSand(p_57815_, p_57816_) && !p_57815_.isClientSide()) {
         p_57815_.levelEvent(2012, p_57816_, 15);
      }
   }

   private boolean shouldUpdateHatchLevel(Level p_57766_, BlockPos p_451096_) {
      float f = p_57766_.environmentAttributes().getValue(EnvironmentAttributes.TURTLE_EGG_HATCH_CHANCE, p_451096_);
      return f > 0.0F && p_57766_.random.nextFloat() < f;
   }

   @Override
   public void playerDestroy(Level p_57771_, Player p_57772_, BlockPos p_57773_, BlockState p_57774_, @Nullable BlockEntity p_57775_, ItemStack p_57776_) {
      super.playerDestroy(p_57771_, p_57772_, p_57773_, p_57774_, p_57775_, p_57776_);
      this.decreaseEggs(p_57771_, p_57773_, p_57774_);
   }

   @Override
   protected boolean canBeReplaced(BlockState p_57796_, BlockPlaceContext p_57797_) {
      return !p_57797_.isSecondaryUseActive() && p_57797_.getItemInHand().is(this.asItem()) && p_57796_.getValue(EGGS) < 4
         ? true
         : super.canBeReplaced(p_57796_, p_57797_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_57761_) {
      BlockState blockstate = p_57761_.getLevel().getBlockState(p_57761_.getClickedPos());
      return blockstate.is(this) ? blockstate.setValue(EGGS, Math.min(4, blockstate.getValue(EGGS) + 1)) : super.getStateForPlacement(p_57761_);
   }

   @Override
   protected VoxelShape getShape(BlockState p_57809_, BlockGetter p_57810_, BlockPos p_57811_, CollisionContext p_57812_) {
      return p_57809_.getValue(EGGS) == 1 ? SHAPE_SINGLE : SHAPE_MULTIPLE;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57799_) {
      p_57799_.add(HATCH, EGGS);
   }

   private boolean canDestroyEgg(ServerLevel p_366354_, Entity p_57769_) {
      if (!(p_57769_ instanceof Turtle) && !(p_57769_ instanceof Bat)) {
         return !(p_57769_ instanceof LivingEntity) ? false : p_57769_ instanceof Player || p_366354_.getGameRules().get(GameRules.MOB_GRIEFING);
      } else {
         return false;
      }
   }
}
