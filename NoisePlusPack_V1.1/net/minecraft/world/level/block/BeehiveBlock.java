package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BeehiveBlock extends BaseEntityBlock {
   public static final MapCodec<BeehiveBlock> CODEC = simpleCodec(BeehiveBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty HONEY_LEVEL = BlockStateProperties.LEVEL_HONEY;
   public static final int MAX_HONEY_LEVELS = 5;

   @Override
   public MapCodec<BeehiveBlock> codec() {
      return CODEC;
   }

   public BeehiveBlock(BlockBehaviour.Properties p_49568_) {
      super(p_49568_);
      this.registerDefaultState(this.stateDefinition.any().setValue(HONEY_LEVEL, 0).setValue(FACING, Direction.NORTH));
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_49618_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_49620_, Level p_49621_, BlockPos p_49622_, Direction p_424276_) {
      return p_49620_.getValue(HONEY_LEVEL);
   }

   @Override
   public void playerDestroy(Level p_49584_, Player p_49585_, BlockPos p_49586_, BlockState p_49587_, @Nullable BlockEntity p_49588_, ItemStack p_49589_) {
      super.playerDestroy(p_49584_, p_49585_, p_49586_, p_49587_, p_49588_, p_49589_);
      if (!p_49584_.isClientSide() && p_49588_ instanceof BeehiveBlockEntity beehiveblockentity) {
         if (!EnchantmentHelper.hasTag(p_49589_, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {
            beehiveblockentity.emptyAllLivingFromHive(p_49585_, p_49587_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            Containers.updateNeighboursAfterDestroy(p_49587_, p_49584_, p_49586_);
            this.angerNearbyBees(p_49584_, p_49586_);
         }

         CriteriaTriggers.BEE_NEST_DESTROYED.trigger((ServerPlayer)p_49585_, p_49587_, p_49589_, beehiveblockentity.getOccupantCount());
      }
   }

   @Override
   protected void onExplosionHit(
      BlockState p_365385_, ServerLevel p_361160_, BlockPos p_363432_, Explosion p_364892_, BiConsumer<ItemStack, BlockPos> p_361789_
   ) {
      super.onExplosionHit(p_365385_, p_361160_, p_363432_, p_364892_, p_361789_);
      this.angerNearbyBees(p_361160_, p_363432_);
   }

   private void angerNearbyBees(Level p_49650_, BlockPos p_49651_) {
      AABB aabb = new AABB(p_49651_).inflate(8.0, 6.0, 8.0);
      List<Bee> list = p_49650_.getEntitiesOfClass(Bee.class, aabb);
      if (!list.isEmpty()) {
         List<Player> list1 = p_49650_.getEntitiesOfClass(Player.class, aabb);
         if (list1.isEmpty()) {
            return;
         }

         for (Bee bee : list) {
            if (bee.getTarget() == null) {
               Player player = Util.getRandom(list1, p_49650_.random);
               bee.setTarget(player);
            }
         }
      }
   }

   public static void dropHoneycomb(
      ServerLevel p_428187_, ItemStack p_426484_, BlockState p_431478_, @Nullable BlockEntity p_426315_, @Nullable Entity p_427616_, BlockPos p_49602_
   ) {
      dropFromBlockInteractLootTable(
         p_428187_,
         BuiltInLootTables.HARVEST_BEEHIVE,
         p_431478_,
         p_426315_,
         p_426484_,
         p_427616_,
         (p_425634_, p_425750_) -> popResource(p_425634_, p_49602_, p_425750_)
      );
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_331014_, BlockState p_328141_, Level p_329187_, BlockPos p_335985_, Player p_336201_, InteractionHand p_333071_, BlockHitResult p_331246_
   ) {
      int i = p_328141_.getValue(HONEY_LEVEL);
      boolean flag = false;
      if (i >= 5) {
         Item item = p_331014_.getItem();
         if (p_329187_ instanceof ServerLevel serverlevel && p_331014_.is(Items.SHEARS)) {
            dropHoneycomb(serverlevel, p_331014_, p_328141_, p_329187_.getBlockEntity(p_335985_), p_336201_, p_335985_);
            p_329187_.playSound(null, p_336201_.getX(), p_336201_.getY(), p_336201_.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_331014_.hurtAndBreak(1, p_336201_, p_333071_.asEquipmentSlot());
            flag = true;
            p_329187_.gameEvent(p_336201_, GameEvent.SHEAR, p_335985_);
         } else if (p_331014_.is(Items.GLASS_BOTTLE)) {
            p_331014_.shrink(1);
            p_329187_.playSound(p_336201_, p_336201_.getX(), p_336201_.getY(), p_336201_.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (p_331014_.isEmpty()) {
               p_336201_.setItemInHand(p_333071_, new ItemStack(Items.HONEY_BOTTLE));
            } else if (!p_336201_.getInventory().add(new ItemStack(Items.HONEY_BOTTLE))) {
               p_336201_.drop(new ItemStack(Items.HONEY_BOTTLE), false);
            }

            flag = true;
            p_329187_.gameEvent(p_336201_, GameEvent.FLUID_PICKUP, p_335985_);
         }

         if (!p_329187_.isClientSide() && flag) {
            p_336201_.awardStat(Stats.ITEM_USED.get(item));
         }
      }

      if (flag) {
         if (!CampfireBlock.isSmokeyPos(p_329187_, p_335985_)) {
            if (this.hiveContainsBees(p_329187_, p_335985_)) {
               this.angerNearbyBees(p_329187_, p_335985_);
            }

            this.releaseBeesAndResetHoneyLevel(p_329187_, p_328141_, p_335985_, p_336201_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
         } else {
            this.resetHoneyLevel(p_329187_, p_328141_, p_335985_);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useItemOn(p_331014_, p_328141_, p_329187_, p_335985_, p_336201_, p_333071_, p_331246_);
      }
   }

   private boolean hiveContainsBees(Level p_49655_, BlockPos p_49656_) {
      return p_49655_.getBlockEntity(p_49656_) instanceof BeehiveBlockEntity beehiveblockentity ? !beehiveblockentity.isEmpty() : false;
   }

   public void releaseBeesAndResetHoneyLevel(
      Level p_49595_, BlockState p_49596_, BlockPos p_49597_, @Nullable Player p_49598_, BeehiveBlockEntity.BeeReleaseStatus p_49599_
   ) {
      this.resetHoneyLevel(p_49595_, p_49596_, p_49597_);
      if (p_49595_.getBlockEntity(p_49597_) instanceof BeehiveBlockEntity beehiveblockentity) {
         beehiveblockentity.emptyAllLivingFromHive(p_49598_, p_49596_, p_49599_);
      }
   }

   public void resetHoneyLevel(Level p_49591_, BlockState p_49592_, BlockPos p_49593_) {
      p_49591_.setBlock(p_49593_, p_49592_.setValue(HONEY_LEVEL, 0), 3);
   }

   @Override
   public void animateTick(BlockState p_220773_, Level p_220774_, BlockPos p_220775_, RandomSource p_220776_) {
      if (p_220773_.getValue(HONEY_LEVEL) >= 5) {
         for (int i = 0; i < p_220776_.nextInt(1) + 1; i++) {
            this.trySpawnDripParticles(p_220774_, p_220775_, p_220773_);
         }
      }
   }

   private void trySpawnDripParticles(Level p_49604_, BlockPos p_49605_, BlockState p_49606_) {
      if (p_49606_.getFluidState().isEmpty() && !(p_49604_.random.nextFloat() < 0.3F)) {
         VoxelShape voxelshape = p_49606_.getCollisionShape(p_49604_, p_49605_);
         double d0 = voxelshape.max(Direction.Axis.Y);
         if (d0 >= 1.0 && !p_49606_.is(BlockTags.IMPERMEABLE)) {
            double d1 = voxelshape.min(Direction.Axis.Y);
            if (d1 > 0.0) {
               this.spawnParticle(p_49604_, p_49605_, voxelshape, p_49605_.getY() + d1 - 0.05);
            } else {
               BlockPos blockpos = p_49605_.below();
               BlockState blockstate = p_49604_.getBlockState(blockpos);
               VoxelShape voxelshape1 = blockstate.getCollisionShape(p_49604_, blockpos);
               double d2 = voxelshape1.max(Direction.Axis.Y);
               if ((d2 < 1.0 || !blockstate.isCollisionShapeFullBlock(p_49604_, blockpos)) && blockstate.getFluidState().isEmpty()) {
                  this.spawnParticle(p_49604_, p_49605_, voxelshape, p_49605_.getY() - 0.05);
               }
            }
         }
      }
   }

   private void spawnParticle(Level p_49613_, BlockPos p_49614_, VoxelShape p_49615_, double p_49616_) {
      this.spawnFluidParticle(
         p_49613_,
         p_49614_.getX() + p_49615_.min(Direction.Axis.X),
         p_49614_.getX() + p_49615_.max(Direction.Axis.X),
         p_49614_.getZ() + p_49615_.min(Direction.Axis.Z),
         p_49614_.getZ() + p_49615_.max(Direction.Axis.Z),
         p_49616_
      );
   }

   private void spawnFluidParticle(Level p_49577_, double p_49578_, double p_49579_, double p_49580_, double p_49581_, double p_49582_) {
      p_49577_.addParticle(
         ParticleTypes.DRIPPING_HONEY,
         Mth.lerp(p_49577_.random.nextDouble(), p_49578_, p_49579_),
         p_49582_,
         Mth.lerp(p_49577_.random.nextDouble(), p_49580_, p_49581_),
         0.0,
         0.0,
         0.0
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_49573_) {
      return this.defaultBlockState().setValue(FACING, p_49573_.getHorizontalDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_49646_) {
      p_49646_.add(HONEY_LEVEL, FACING);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos p_152184_, BlockState p_152185_) {
      return new BeehiveBlockEntity(p_152184_, p_152185_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_152180_, BlockState p_152181_, BlockEntityType<T> p_152182_) {
      return p_152180_.isClientSide() ? null : createTickerHelper(p_152182_, BlockEntityType.BEEHIVE, BeehiveBlockEntity::serverTick);
   }

   @Override
   public BlockState playerWillDestroy(Level p_49608_, BlockPos p_49609_, BlockState p_49610_, Player p_49611_) {
      if (p_49608_ instanceof ServerLevel serverlevel
         && p_49611_.preventsBlockDrops()
         && serverlevel.getGameRules().get(GameRules.BLOCK_DROPS)
         && p_49608_.getBlockEntity(p_49609_) instanceof BeehiveBlockEntity beehiveblockentity) {
         int i = p_49610_.getValue(HONEY_LEVEL);
         boolean flag = !beehiveblockentity.isEmpty();
         if (flag || i > 0) {
            ItemStack itemstack = new ItemStack(this);
            itemstack.applyComponents(beehiveblockentity.collectComponents());
            itemstack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, i));
            ItemEntity itementity = new ItemEntity(p_49608_, p_49609_.getX(), p_49609_.getY(), p_49609_.getZ(), itemstack);
            itementity.setDefaultPickUpDelay();
            p_49608_.addFreshEntity(itementity);
         }
      }

      return super.playerWillDestroy(p_49608_, p_49609_, p_49610_, p_49611_);
   }

   @Override
   protected List<ItemStack> getDrops(BlockState p_49636_, LootParams.Builder p_287581_) {
      Entity entity = p_287581_.getOptionalParameter(LootContextParams.THIS_ENTITY);
      if (entity instanceof PrimedTnt
         || entity instanceof Creeper
         || entity instanceof WitherSkull
         || entity instanceof WitherBoss
         || entity instanceof MinecartTNT) {
         BlockEntity blockentity = p_287581_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
         if (blockentity instanceof BeehiveBlockEntity beehiveblockentity) {
            beehiveblockentity.emptyAllLivingFromHive(null, p_49636_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
         }
      }

      return super.getDrops(p_49636_, p_287581_);
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_375896_, BlockPos p_377793_, BlockState p_377589_, boolean p_376286_) {
      ItemStack itemstack = super.getCloneItemStack(p_375896_, p_377793_, p_377589_, p_376286_);
      if (p_376286_) {
         itemstack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, p_377589_.getValue(HONEY_LEVEL)));
      }

      return itemstack;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_49639_,
      LevelReader p_364129_,
      ScheduledTickAccess p_367483_,
      BlockPos p_49643_,
      Direction p_49640_,
      BlockPos p_49644_,
      BlockState p_49641_,
      RandomSource p_365194_
   ) {
      if (p_364129_.getBlockState(p_49644_).getBlock() instanceof FireBlock
         && p_364129_.getBlockEntity(p_49643_) instanceof BeehiveBlockEntity beehiveblockentity) {
         beehiveblockentity.emptyAllLivingFromHive(null, p_49639_, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
      }

      return super.updateShape(p_49639_, p_364129_, p_367483_, p_49643_, p_49640_, p_49644_, p_49641_, p_365194_);
   }

   @Override
   public BlockState rotate(BlockState p_309863_, Rotation p_310613_) {
      return p_309863_.setValue(FACING, p_310613_.rotate(p_309863_.getValue(FACING)));
   }

   @Override
   public BlockState mirror(BlockState p_311137_, Mirror p_310463_) {
      return p_311137_.rotate(p_310463_.getRotation(p_311137_.getValue(FACING)));
   }
}
