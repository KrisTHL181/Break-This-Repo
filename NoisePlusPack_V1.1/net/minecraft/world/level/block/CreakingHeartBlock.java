package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

public class CreakingHeartBlock extends BaseEntityBlock {
   public static final MapCodec<CreakingHeartBlock> CODEC = simpleCodec(CreakingHeartBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
   public static final EnumProperty<CreakingHeartState> STATE = BlockStateProperties.CREAKING_HEART_STATE;
   public static final BooleanProperty NATURAL = BlockStateProperties.NATURAL;

   @Override
   public MapCodec<CreakingHeartBlock> codec() {
      return CODEC;
   }

   protected CreakingHeartBlock(BlockBehaviour.Properties p_366361_) {
      super(p_366361_);
      this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(STATE, CreakingHeartState.UPROOTED).setValue(NATURAL, false));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_361541_, BlockState p_365645_) {
      return new CreakingHeartBlockEntity(p_361541_, p_365645_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_363998_, BlockState p_362026_, BlockEntityType<T> p_362183_) {
      if (p_363998_.isClientSide()) {
         return null;
      } else {
         return p_362026_.getValue(STATE) != CreakingHeartState.UPROOTED
            ? createTickerHelper(p_362183_, BlockEntityType.CREAKING_HEART, CreakingHeartBlockEntity::serverTick)
            : null;
      }
   }

   @Override
   public void animateTick(BlockState p_363486_, Level p_367731_, BlockPos p_364380_, RandomSource p_362325_) {
      if (p_367731_.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, p_364380_)
         && p_363486_.getValue(STATE) != CreakingHeartState.UPROOTED
         && p_362325_.nextInt(16) == 0
         && isSurroundedByLogs(p_367731_, p_364380_)) {
         p_367731_.playLocalSound(p_364380_.getX(), p_364380_.getY(), p_364380_.getZ(), SoundEvents.CREAKING_HEART_IDLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_368911_,
      LevelReader p_369079_,
      ScheduledTickAccess p_361736_,
      BlockPos p_363646_,
      Direction p_364258_,
      BlockPos p_367438_,
      BlockState p_361093_,
      RandomSource p_368581_
   ) {
      p_361736_.scheduleTick(p_363646_, this, 1);
      return super.updateShape(p_368911_, p_369079_, p_361736_, p_363646_, p_364258_, p_367438_, p_361093_, p_368581_);
   }

   @Override
   protected void tick(BlockState p_396773_, ServerLevel p_396152_, BlockPos p_394279_, RandomSource p_392431_) {
      BlockState blockstate = updateState(p_396773_, p_396152_, p_394279_);
      if (blockstate != p_396773_) {
         p_396152_.setBlock(p_394279_, blockstate, 3);
      }
   }

   private static BlockState updateState(BlockState p_366979_, Level p_397672_, BlockPos p_368789_) {
      boolean flag = hasRequiredLogs(p_366979_, p_397672_, p_368789_);
      boolean flag1 = p_366979_.getValue(STATE) == CreakingHeartState.UPROOTED;
      return flag && flag1
         ? p_366979_.setValue(
            STATE,
            p_397672_.environmentAttributes().getValue(EnvironmentAttributes.CREAKING_ACTIVE, p_368789_)
               ? CreakingHeartState.AWAKE
               : CreakingHeartState.DORMANT
         )
         : p_366979_;
   }

   public static boolean hasRequiredLogs(BlockState p_363238_, LevelReader p_369227_, BlockPos p_362506_) {
      Direction.Axis direction$axis = p_363238_.getValue(AXIS);

      for (Direction direction : direction$axis.getDirections()) {
         BlockState blockstate = p_369227_.getBlockState(p_362506_.relative(direction));
         if (!blockstate.is(BlockTags.PALE_OAK_LOGS) || blockstate.getValue(AXIS) != direction$axis) {
            return false;
         }
      }

      return true;
   }

   private static boolean isSurroundedByLogs(LevelAccessor p_369449_, BlockPos p_360949_) {
      for (Direction direction : Direction.values()) {
         BlockPos blockpos = p_360949_.relative(direction);
         BlockState blockstate = p_369449_.getBlockState(blockpos);
         if (!blockstate.is(BlockTags.PALE_OAK_LOGS)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_368175_) {
      return updateState(this.defaultBlockState().setValue(AXIS, p_368175_.getClickedFace().getAxis()), p_368175_.getLevel(), p_368175_.getClickedPos());
   }

   @Override
   protected BlockState rotate(BlockState p_364749_, Rotation p_361524_) {
      return RotatedPillarBlock.rotatePillar(p_364749_, p_361524_);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_365552_) {
      p_365552_.add(AXIS, STATE, NATURAL);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_393571_, ServerLevel p_391268_, BlockPos p_396756_, boolean p_392387_) {
      Containers.updateNeighboursAfterDestroy(p_393571_, p_391268_, p_396756_);
   }

   @Override
   protected void onExplosionHit(
      BlockState p_378796_, ServerLevel p_375403_, BlockPos p_376010_, Explosion p_377799_, BiConsumer<ItemStack, BlockPos> p_378141_
   ) {
      if (p_375403_.getBlockEntity(p_376010_) instanceof CreakingHeartBlockEntity creakingheartblockentity
         && p_377799_ instanceof ServerExplosion serverexplosion
         && p_377799_.getBlockInteraction().shouldAffectBlocklikeEntities()) {
         creakingheartblockentity.removeProtector(serverexplosion.getDamageSource());
         if (p_377799_.getIndirectSourceEntity() instanceof Player player && p_377799_.getBlockInteraction().shouldAffectBlocklikeEntities()) {
            this.tryAwardExperience(player, p_378796_, p_375403_, p_376010_);
         }
      }

      super.onExplosionHit(p_378796_, p_375403_, p_376010_, p_377799_, p_378141_);
   }

   @Override
   public BlockState playerWillDestroy(Level p_361112_, BlockPos p_368479_, BlockState p_363792_, Player p_362626_) {
      if (p_361112_.getBlockEntity(p_368479_) instanceof CreakingHeartBlockEntity creakingheartblockentity) {
         creakingheartblockentity.removeProtector(p_362626_.damageSources().playerAttack(p_362626_));
         this.tryAwardExperience(p_362626_, p_363792_, p_361112_, p_368479_);
      }

      return super.playerWillDestroy(p_361112_, p_368479_, p_363792_, p_362626_);
   }

   private void tryAwardExperience(Player p_378356_, BlockState p_377297_, Level p_376854_, BlockPos p_378426_) {
      if (!p_378356_.preventsBlockDrops() && !p_378356_.isSpectator() && p_377297_.getValue(NATURAL) && p_376854_ instanceof ServerLevel serverlevel) {
         this.popExperience(serverlevel, p_378426_, p_376854_.random.nextIntBetweenInclusive(20, 24));
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_369932_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_360933_, Level p_366654_, BlockPos p_366296_, Direction p_425404_) {
      if (p_360933_.getValue(STATE) == CreakingHeartState.UPROOTED) {
         return 0;
      } else {
         return p_366654_.getBlockEntity(p_366296_) instanceof CreakingHeartBlockEntity creakingheartblockentity
            ? creakingheartblockentity.getAnalogOutputSignal()
            : 0;
      }
   }
}
