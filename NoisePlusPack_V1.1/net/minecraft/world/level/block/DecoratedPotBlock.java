package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DecoratedPotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final MapCodec<DecoratedPotBlock> CODEC = simpleCodec(DecoratedPotBlock::new);
   public static final Identifier SHERDS_DYNAMIC_DROP_ID = Identifier.withDefaultNamespace("sherds");
   public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
   public static final BooleanProperty CRACKED = BlockStateProperties.CRACKED;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

   @Override
   public MapCodec<DecoratedPotBlock> codec() {
      return CODEC;
   }

   protected DecoratedPotBlock(BlockBehaviour.Properties p_273064_) {
      super(p_273064_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(CRACKED, false));
   }

   @Override
   protected BlockState updateShape(
      BlockState p_276307_,
      LevelReader p_363776_,
      ScheduledTickAccess p_364929_,
      BlockPos p_276270_,
      Direction p_276322_,
      BlockPos p_276312_,
      BlockState p_276280_,
      RandomSource p_361966_
   ) {
      if (p_276307_.getValue(WATERLOGGED)) {
         p_364929_.scheduleTick(p_276270_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363776_));
      }

      return super.updateShape(p_276307_, p_363776_, p_364929_, p_276270_, p_276322_, p_276312_, p_276280_, p_361966_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_272711_) {
      FluidState fluidstate = p_272711_.getLevel().getFluidState(p_272711_.getClickedPos());
      return this.defaultBlockState()
         .setValue(HORIZONTAL_FACING, p_272711_.getHorizontalDirection())
         .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER)
         .setValue(CRACKED, false);
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_335411_, BlockState p_334873_, Level p_328717_, BlockPos p_332886_, Player p_331165_, InteractionHand p_330433_, BlockHitResult p_330105_
   ) {
      if (p_328717_.getBlockEntity(p_332886_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         if (p_328717_.isClientSide()) {
            return InteractionResult.SUCCESS;
         }

         ItemStack itemstack1 = decoratedpotblockentity.getTheItem();
         if (!p_335411_.isEmpty()
            && (itemstack1.isEmpty() || ItemStack.isSameItemSameComponents(itemstack1, p_335411_) && itemstack1.getCount() < itemstack1.getMaxStackSize())) {
            decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.POSITIVE);
            p_331165_.awardStat(Stats.ITEM_USED.get(p_335411_.getItem()));
            ItemStack itemstack = p_335411_.consumeAndReturn(1, p_331165_);
            float f;
            if (decoratedpotblockentity.isEmpty()) {
               decoratedpotblockentity.setTheItem(itemstack);
               f = (float)itemstack.getCount() / itemstack.getMaxStackSize();
            } else {
               itemstack1.grow(1);
               f = (float)itemstack1.getCount() / itemstack1.getMaxStackSize();
            }

            p_328717_.playSound(null, p_332886_, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1.0F, 0.7F + 0.5F * f);
            if (p_328717_ instanceof ServerLevel serverlevel) {
               serverlevel.sendParticles(
                  ParticleTypes.DUST_PLUME, p_332886_.getX() + 0.5, p_332886_.getY() + 1.2, p_332886_.getZ() + 0.5, 7, 0.0, 0.0, 0.0, 0.0
               );
            }

            decoratedpotblockentity.setChanged();
            p_328717_.gameEvent(p_331165_, GameEvent.BLOCK_CHANGE, p_332886_);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_329061_, Level p_331143_, BlockPos p_332658_, Player p_330362_, BlockHitResult p_330700_) {
      if (p_331143_.getBlockEntity(p_332658_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         p_331143_.playSound(null, p_332658_, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F);
         decoratedpotblockentity.wobble(DecoratedPotBlockEntity.WobbleStyle.NEGATIVE);
         p_331143_.gameEvent(p_330362_, GameEvent.BLOCK_CHANGE, p_332658_);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected boolean isPathfindable(BlockState p_276295_, PathComputationType p_276303_) {
      return false;
   }

   @Override
   protected VoxelShape getShape(BlockState p_273112_, BlockGetter p_273055_, BlockPos p_273137_, CollisionContext p_273151_) {
      return SHAPE;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_273169_) {
      p_273169_.add(HORIZONTAL_FACING, WATERLOGGED, CRACKED);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_273396_, BlockState p_272674_) {
      return new DecoratedPotBlockEntity(p_273396_, p_272674_);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_391406_, ServerLevel p_392756_, BlockPos p_397588_, boolean p_397815_) {
      Containers.updateNeighboursAfterDestroy(p_391406_, p_392756_, p_397588_);
   }

   @Override
   protected List<ItemStack> getDrops(BlockState p_287683_, LootParams.Builder p_287582_) {
      BlockEntity blockentity = p_287582_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockentity instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         p_287582_.withDynamicDrop(SHERDS_DYNAMIC_DROP_ID, p_327259_ -> {
            for (Item item : decoratedpotblockentity.getDecorations().ordered()) {
               p_327259_.accept(item.getDefaultInstance());
            }
         });
      }

      return super.getDrops(p_287683_, p_287582_);
   }

   @Override
   public BlockState playerWillDestroy(Level p_273590_, BlockPos p_273343_, BlockState p_272869_, Player p_273002_) {
      ItemStack itemstack = p_273002_.getMainHandItem();
      BlockState blockstate = p_272869_;
      if (itemstack.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasTag(itemstack, EnchantmentTags.PREVENTS_DECORATED_POT_SHATTERING)) {
         blockstate = p_272869_.setValue(CRACKED, true);
         p_273590_.setBlock(p_273343_, blockstate, 260);
      }

      return super.playerWillDestroy(p_273590_, p_273343_, blockstate, p_273002_);
   }

   @Override
   protected FluidState getFluidState(BlockState p_272593_) {
      return p_272593_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_272593_);
   }

   @Override
   protected SoundType getSoundType(BlockState p_277561_) {
      return p_277561_.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
   }

   @Override
   protected void onProjectileHit(Level p_310477_, BlockState p_309479_, BlockHitResult p_309542_, Projectile p_309867_) {
      BlockPos blockpos = p_309542_.getBlockPos();
      if (p_310477_ instanceof ServerLevel serverlevel && p_309867_.mayInteract(serverlevel, blockpos) && p_309867_.mayBreak(serverlevel)) {
         p_310477_.setBlock(blockpos, p_309479_.setValue(CRACKED, true), 260);
         p_310477_.destroyBlock(blockpos, true, p_309867_);
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312375_, BlockPos p_300759_, BlockState p_297348_, boolean p_378578_) {
      if (p_312375_.getBlockEntity(p_300759_) instanceof DecoratedPotBlockEntity decoratedpotblockentity) {
         PotDecorations potdecorations = decoratedpotblockentity.getDecorations();
         return DecoratedPotBlockEntity.createDecoratedPotItem(potdecorations);
      } else {
         return super.getCloneItemStack(p_312375_, p_300759_, p_297348_, p_378578_);
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_310567_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_310830_, Level p_312569_, BlockPos p_309943_, Direction p_426852_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_312569_.getBlockEntity(p_309943_));
   }

   @Override
   protected BlockState rotate(BlockState p_335606_, Rotation p_331991_) {
      return p_335606_.setValue(HORIZONTAL_FACING, p_331991_.rotate(p_335606_.getValue(HORIZONTAL_FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_332589_, Mirror p_332235_) {
      return p_332589_.rotate(p_332235_.getRotation(p_332589_.getValue(HORIZONTAL_FACING)));
   }
}
