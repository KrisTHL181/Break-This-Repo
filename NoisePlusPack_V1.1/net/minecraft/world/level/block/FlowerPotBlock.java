package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerPotBlock extends Block {
   public static final MapCodec<FlowerPotBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422111_ -> p_422111_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("potted").forGetter(p_310137_ -> p_310137_.potted), propertiesCodec())
         .apply(p_422111_, FlowerPotBlock::new)
   );
   private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
   private static final VoxelShape SHAPE = Block.column(6.0, 0.0, 6.0);
   private final Block potted;

   @Override
   public MapCodec<FlowerPotBlock> codec() {
      return CODEC;
   }

   public FlowerPotBlock(Block p_53528_, BlockBehaviour.Properties p_53529_) {
      super(p_53529_);
      this.potted = p_53528_;
      POTTED_BY_CONTENT.put(p_53528_, this);
   }

   @Override
   protected VoxelShape getShape(BlockState p_53556_, BlockGetter p_53557_, BlockPos p_53558_, CollisionContext p_53559_) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_329639_, BlockState p_328047_, Level p_328816_, BlockPos p_334572_, Player p_329206_, InteractionHand p_329142_, BlockHitResult p_330607_
   ) {
      BlockState blockstate = (p_329639_.getItem() instanceof BlockItem blockitem
            ? POTTED_BY_CONTENT.getOrDefault(blockitem.getBlock(), Blocks.AIR)
            : Blocks.AIR)
         .defaultBlockState();
      if (blockstate.isAir()) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!this.isEmpty()) {
         return InteractionResult.CONSUME;
      }

      p_328816_.setBlock(p_334572_, blockstate, 3);
      p_328816_.gameEvent(p_329206_, GameEvent.BLOCK_CHANGE, p_334572_);
      p_329206_.awardStat(Stats.POT_FLOWER);
      p_329639_.consume(1, p_329206_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_335777_, Level p_334489_, BlockPos p_330334_, Player p_333787_, BlockHitResult p_335374_) {
      if (this.isEmpty()) {
         return InteractionResult.CONSUME;
      }

      ItemStack itemstack = new ItemStack(this.potted);
      if (!p_333787_.addItem(itemstack)) {
         p_333787_.drop(itemstack, false);
      }

      p_334489_.setBlock(p_330334_, Blocks.FLOWER_POT.defaultBlockState(), 3);
      p_334489_.gameEvent(p_333787_, GameEvent.BLOCK_CHANGE, p_330334_);
      return InteractionResult.SUCCESS;
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_312345_, BlockPos p_53532_, BlockState p_53533_, boolean p_377634_) {
      return this.isEmpty() ? super.getCloneItemStack(p_312345_, p_53532_, p_53533_, p_377634_) : new ItemStack(this.potted);
   }

   private boolean isEmpty() {
      return this.potted == Blocks.AIR;
   }

   @Override
   protected BlockState updateShape(
      BlockState p_53547_,
      LevelReader p_368356_,
      ScheduledTickAccess p_362911_,
      BlockPos p_53551_,
      Direction p_53548_,
      BlockPos p_53552_,
      BlockState p_53549_,
      RandomSource p_368885_
   ) {
      return p_53548_ == Direction.DOWN && !p_53547_.canSurvive(p_368356_, p_53551_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_53547_, p_368356_, p_362911_, p_53551_, p_53548_, p_53552_, p_53549_, p_368885_);
   }

   public Block getPotted() {
      return this.potted;
   }

   @Override
   protected boolean isPathfindable(BlockState p_53535_, PathComputationType p_53538_) {
      return false;
   }

   @Override
   protected boolean isRandomlyTicking(BlockState p_376063_) {
      return p_376063_.is(Blocks.POTTED_OPEN_EYEBLOSSOM) || p_376063_.is(Blocks.POTTED_CLOSED_EYEBLOSSOM);
   }

   @Override
   protected void randomTick(BlockState p_377272_, ServerLevel p_377872_, BlockPos p_375421_, RandomSource p_376681_) {
      if (this.isRandomlyTicking(p_377272_)) {
         boolean flag = this.potted == Blocks.OPEN_EYEBLOSSOM;
         boolean flag1 = p_377872_.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, p_375421_).toBoolean(flag);
         if (flag != flag1) {
            p_377872_.setBlock(p_375421_, this.opposite(p_377272_), 3);
            EyeblossomBlock.Type eyeblossomblock$type = EyeblossomBlock.Type.fromBoolean(flag).transform();
            eyeblossomblock$type.spawnTransformParticle(p_377872_, p_375421_, p_376681_);
            p_377872_.playSound(null, p_375421_, eyeblossomblock$type.longSwitchSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

      super.randomTick(p_377272_, p_377872_, p_375421_, p_376681_);
   }

   public BlockState opposite(BlockState p_376074_) {
      if (p_376074_.is(Blocks.POTTED_OPEN_EYEBLOSSOM)) {
         return Blocks.POTTED_CLOSED_EYEBLOSSOM.defaultBlockState();
      } else {
         return p_376074_.is(Blocks.POTTED_CLOSED_EYEBLOSSOM) ? Blocks.POTTED_OPEN_EYEBLOSSOM.defaultBlockState() : p_376074_;
      }
   }
}
