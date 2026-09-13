package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ButtonBlock extends FaceAttachedHorizontalDirectionalBlock {
   public static final MapCodec<ButtonBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422077_ -> p_422077_.group(
            BlockSetType.CODEC.fieldOf("block_set_type").forGetter(p_312681_ -> p_312681_.type),
            Codec.intRange(1, 1024).fieldOf("ticks_to_stay_pressed").forGetter(p_312686_ -> p_312686_.ticksToStayPressed),
            propertiesCodec()
         )
         .apply(p_422077_, ButtonBlock::new)
   );
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private final BlockSetType type;
   private final int ticksToStayPressed;
   private final Function<BlockState, VoxelShape> shapes;

   @Override
   public MapCodec<ButtonBlock> codec() {
      return CODEC;
   }

   protected ButtonBlock(BlockSetType p_273462_, int p_273212_, BlockBehaviour.Properties p_273290_) {
      super(p_273290_.sound(p_273462_.soundType()));
      this.type = p_273462_;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(FACE, AttachFace.WALL));
      this.ticksToStayPressed = p_273212_;
      this.shapes = this.makeShapes();
   }

   private Function<BlockState, VoxelShape> makeShapes() {
      VoxelShape voxelshape = Block.cube(14.0);
      VoxelShape voxelshape1 = Block.cube(12.0);
      Map<AttachFace, Map<Direction, VoxelShape>> map = Shapes.rotateAttachFace(Block.boxZ(6.0, 4.0, 8.0, 16.0));
      return this.getShapeForEachState(
         p_390924_ -> Shapes.join(
            map.get(p_390924_.getValue(FACE)).get(p_390924_.getValue(FACING)), p_390924_.getValue(POWERED) ? voxelshape : voxelshape1, BooleanOp.ONLY_FIRST
         )
      );
   }

   @Override
   protected VoxelShape getShape(BlockState p_51104_, BlockGetter p_51105_, BlockPos p_51106_, CollisionContext p_51107_) {
      return this.shapes.apply(p_51104_);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_329418_, Level p_334611_, BlockPos p_332004_, Player p_330636_, BlockHitResult p_327724_) {
      if (p_329418_.getValue(POWERED)) {
         return InteractionResult.CONSUME;
      }

      this.press(p_329418_, p_334611_, p_332004_, p_330636_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected void onExplosionHit(
      BlockState p_310762_, ServerLevel p_363623_, BlockPos p_312982_, Explosion p_311820_, BiConsumer<ItemStack, BlockPos> p_312672_
   ) {
      if (p_311820_.canTriggerBlocks() && !p_310762_.getValue(POWERED)) {
         this.press(p_310762_, p_363623_, p_312982_, null);
      }

      super.onExplosionHit(p_310762_, p_363623_, p_312982_, p_311820_, p_312672_);
   }

   public void press(BlockState p_51117_, Level p_51118_, BlockPos p_51119_, @Nullable Player p_343045_) {
      p_51118_.setBlock(p_51119_, p_51117_.setValue(POWERED, true), 3);
      this.updateNeighbours(p_51117_, p_51118_, p_51119_);
      p_51118_.scheduleTick(p_51119_, this, this.ticksToStayPressed);
      this.playSound(p_343045_, p_51118_, p_51119_, true);
      p_51118_.gameEvent(p_343045_, GameEvent.BLOCK_ACTIVATE, p_51119_);
   }

   protected void playSound(@Nullable Player p_51068_, LevelAccessor p_51069_, BlockPos p_51070_, boolean p_51071_) {
      p_51069_.playSound(p_51071_ ? p_51068_ : null, p_51070_, this.getSound(p_51071_), SoundSource.BLOCKS);
   }

   protected SoundEvent getSound(boolean p_51102_) {
      return p_51102_ ? this.type.buttonClickOn() : this.type.buttonClickOff();
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_391932_, ServerLevel p_395947_, BlockPos p_394950_, boolean p_394747_) {
      if (!p_394747_ && p_391932_.getValue(POWERED)) {
         this.updateNeighbours(p_391932_, p_395947_, p_394950_);
      }
   }

   @Override
   protected int getSignal(BlockState p_51078_, BlockGetter p_51079_, BlockPos p_51080_, Direction p_51081_) {
      return p_51078_.getValue(POWERED) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState p_51109_, BlockGetter p_51110_, BlockPos p_51111_, Direction p_51112_) {
      return p_51109_.getValue(POWERED) && getConnectedDirection(p_51109_) == p_51112_ ? 15 : 0;
   }

   @Override
   protected boolean isSignalSource(BlockState p_51114_) {
      return true;
   }

   @Override
   protected void tick(BlockState p_220903_, ServerLevel p_220904_, BlockPos p_220905_, RandomSource p_220906_) {
      if (p_220903_.getValue(POWERED)) {
         this.checkPressed(p_220903_, p_220904_, p_220905_);
      }
   }

   @Override
   protected void entityInside(BlockState p_51083_, Level p_51084_, BlockPos p_51085_, Entity p_51086_, InsideBlockEffectApplier p_394334_, boolean p_432047_) {
      if (!p_51084_.isClientSide() && this.type.canButtonBeActivatedByArrows() && !p_51083_.getValue(POWERED)) {
         this.checkPressed(p_51083_, p_51084_, p_51085_);
      }
   }

   protected void checkPressed(BlockState p_51121_, Level p_51122_, BlockPos p_51123_) {
      AbstractArrow abstractarrow = this.type.canButtonBeActivatedByArrows()
         ? p_51122_.getEntitiesOfClass(AbstractArrow.class, p_51121_.getShape(p_51122_, p_51123_).bounds().move(p_51123_)).stream().findFirst().orElse(null)
         : null;
      boolean flag = abstractarrow != null;
      boolean flag1 = p_51121_.getValue(POWERED);
      if (flag != flag1) {
         p_51122_.setBlock(p_51123_, p_51121_.setValue(POWERED, flag), 3);
         this.updateNeighbours(p_51121_, p_51122_, p_51123_);
         this.playSound(null, p_51122_, p_51123_, flag);
         p_51122_.gameEvent(abstractarrow, flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, p_51123_);
      }

      if (flag) {
         p_51122_.scheduleTick(new BlockPos(p_51123_), this, this.ticksToStayPressed);
      }
   }

   private void updateNeighbours(BlockState p_51125_, Level p_51126_, BlockPos p_51127_) {
      Direction direction = getConnectedDirection(p_51125_).getOpposite();
      Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
         p_51126_, direction, direction.getAxis().isHorizontal() ? Direction.UP : p_51125_.getValue(FACING)
      );
      p_51126_.updateNeighborsAt(p_51127_, this, orientation);
      p_51126_.updateNeighborsAt(p_51127_.relative(direction), this, orientation);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51101_) {
      p_51101_.add(FACING, POWERED, FACE);
   }
}
