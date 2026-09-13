package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SweetBerryBushBlock extends VegetationBlock implements BonemealableBlock {
   public static final MapCodec<SweetBerryBushBlock> CODEC = simpleCodec(SweetBerryBushBlock::new);
   private static final float HURT_SPEED_THRESHOLD = 0.003F;
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final VoxelShape SHAPE_SAPLING = Block.column(10.0, 0.0, 8.0);
   private static final VoxelShape SHAPE_GROWING = Block.column(14.0, 0.0, 16.0);

   @Override
   public MapCodec<SweetBerryBushBlock> codec() {
      return CODEC;
   }

   public SweetBerryBushBlock(BlockBehaviour.Properties p_57249_) {
      super(p_57249_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312054_, BlockPos p_57257_, BlockState p_57258_, boolean p_376908_) {
      return new ItemStack(Items.SWEET_BERRIES);
   }

   @Override
   protected VoxelShape getShape(BlockState p_57291_, BlockGetter p_57292_, BlockPos p_57293_, CollisionContext p_57294_) {
      return switch (p_57291_.getValue(AGE)) {
         case 0 -> SHAPE_SAPLING;
         case 3 -> Shapes.block();
         default -> SHAPE_GROWING;
      };
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_57284_) {
      return p_57284_.getValue(AGE) < 3;
   }

   @Override
   protected void randomTick(BlockState p_222563_, ServerLevel p_222564_, BlockPos p_222565_, RandomSource p_222566_) {
      int i = p_222563_.getValue(AGE);
      if (i < 3 && p_222566_.nextInt(5) == 0 && p_222564_.getRawBrightness(p_222565_.above(), 0) >= 9) {
         BlockState blockstate = p_222563_.setValue(AGE, i + 1);
         p_222564_.setBlock(p_222565_, blockstate, 2);
         p_222564_.gameEvent(GameEvent.BLOCK_CHANGE, p_222565_, GameEvent.Context.of(blockstate));
      }
   }

   @Override
   protected void entityInside(BlockState p_57270_, Level p_57271_, BlockPos p_57272_, Entity p_57273_, InsideBlockEffectApplier p_393689_, boolean p_432056_) {
      if (p_57273_ instanceof LivingEntity && p_57273_.getType() != EntityType.FOX && p_57273_.getType() != EntityType.BEE) {
         p_57273_.makeStuckInBlock(p_57270_, new Vec3(0.8F, 0.75, 0.8F));
         if (p_57271_ instanceof ServerLevel serverlevel && p_57270_.getValue(AGE) != 0) {
            Vec3 vec3 = p_57273_.isClientAuthoritative() ? p_57273_.getKnownMovement() : p_57273_.oldPosition().subtract(p_57273_.position());
            if (vec3.horizontalDistanceSqr() > 0.0) {
               double d0 = Math.abs(vec3.x());
               double d1 = Math.abs(vec3.z());
               if (d0 >= 0.003F || d1 >= 0.003F) {
                  p_57273_.hurtServer(serverlevel, p_57271_.damageSources().sweetBerryBush(), 1.0F);
               }
            }
         }
      }
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_333126_, BlockState p_333435_, Level p_336209_, BlockPos p_329457_, Player p_336064_, InteractionHand p_336388_, BlockHitResult p_334205_
   ) {
      int i = p_333435_.getValue(AGE);
      boolean flag = i == 3;
      return !flag && p_333126_.is(Items.BONE_MEAL)
         ? InteractionResult.PASS
         : super.useItemOn(p_333126_, p_333435_, p_336209_, p_329457_, p_336064_, p_336388_, p_334205_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_330186_, Level p_334365_, BlockPos p_328580_, Player p_332233_, BlockHitResult p_329481_) {
      if (p_330186_.getValue(AGE) > 1) {
         if (p_334365_ instanceof ServerLevel serverlevel) {
            Block.dropFromBlockInteractLootTable(
               serverlevel,
               BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
               p_330186_,
               p_334365_.getBlockEntity(p_328580_),
               null,
               p_332233_,
               (p_429247_, p_430784_) -> Block.popResource(p_429247_, p_328580_, p_430784_)
            );
            serverlevel.playSound(
               null, p_328580_, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverlevel.random.nextFloat() * 0.4F
            );
            BlockState blockstate = p_330186_.setValue(AGE, 1);
            serverlevel.setBlock(p_328580_, blockstate, 2);
            serverlevel.gameEvent(GameEvent.BLOCK_CHANGE, p_328580_, GameEvent.Context.of(p_332233_, blockstate));
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useWithoutItem(p_330186_, p_334365_, p_328580_, p_332233_, p_329481_);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57282_) {
      p_57282_.add(AGE);
   }

   @Override
   public boolean isValidBonemealTarget(LevelReader p_256056_, BlockPos p_57261_, BlockState p_57262_) {
      return p_57262_.getValue(AGE) < 3;
   }

   @Override
   public boolean isBonemealSuccess(Level p_222558_, RandomSource p_222559_, BlockPos p_222560_, BlockState p_222561_) {
      return true;
   }

   @Override
   public void performBonemeal(ServerLevel p_222553_, RandomSource p_222554_, BlockPos p_222555_, BlockState p_222556_) {
      int i = Math.min(3, p_222556_.getValue(AGE) + 1);
      p_222553_.setBlock(p_222555_, p_222556_.setValue(AGE, i), 2);
   }
}
