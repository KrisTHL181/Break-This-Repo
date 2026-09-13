package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class CopperChestBlock extends ChestBlock {
   public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_428032_ -> p_428032_.group(
            WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getState),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("open_sound").forGetter(ChestBlock::getOpenChestSound),
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("close_sound").forGetter(ChestBlock::getCloseChestSound),
            propertiesCodec()
         )
         .apply(p_428032_, CopperChestBlock::new)
   );
   private static final Map<Block, Supplier<Block>> COPPER_TO_COPPER_CHEST_MAPPING = Map.of(
      Blocks.COPPER_BLOCK,
      () -> Blocks.COPPER_CHEST,
      Blocks.EXPOSED_COPPER,
      () -> Blocks.EXPOSED_COPPER_CHEST,
      Blocks.WEATHERED_COPPER,
      () -> Blocks.WEATHERED_COPPER_CHEST,
      Blocks.OXIDIZED_COPPER,
      () -> Blocks.OXIDIZED_COPPER_CHEST,
      Blocks.WAXED_COPPER_BLOCK,
      () -> Blocks.COPPER_CHEST,
      Blocks.WAXED_EXPOSED_COPPER,
      () -> Blocks.EXPOSED_COPPER_CHEST,
      Blocks.WAXED_WEATHERED_COPPER,
      () -> Blocks.WEATHERED_COPPER_CHEST,
      Blocks.WAXED_OXIDIZED_COPPER,
      () -> Blocks.OXIDIZED_COPPER_CHEST
   );
   private final WeatheringCopper.WeatherState weatherState;

   @Override
   public MapCodec<? extends CopperChestBlock> codec() {
      return CODEC;
   }

   public CopperChestBlock(WeatheringCopper.WeatherState p_425330_, SoundEvent p_431268_, SoundEvent p_430366_, BlockBehaviour.Properties p_425818_) {
      super(() -> BlockEntityType.CHEST, p_431268_, p_430366_, p_425818_);
      this.weatherState = p_425330_;
   }

   @Override
   public boolean chestCanConnectTo(BlockState p_430798_) {
      return p_430798_.is(BlockTags.COPPER_CHESTS) && p_430798_.hasProperty(ChestBlock.TYPE);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_424674_) {
      BlockState blockstate = super.getStateForPlacement(p_424674_);
      return getLeastOxidizedChestOfConnectedBlocks(blockstate, p_424674_.getLevel(), p_424674_.getClickedPos());
   }

   private static BlockState getLeastOxidizedChestOfConnectedBlocks(BlockState p_428941_, Level p_424225_, BlockPos p_423567_) {
      BlockState blockstate = p_424225_.getBlockState(p_423567_.relative(getConnectedDirection(p_428941_)));
      if (!p_428941_.getValue(ChestBlock.TYPE).equals(ChestType.SINGLE)
         && p_428941_.getBlock() instanceof CopperChestBlock copperchestblock
         && blockstate.getBlock() instanceof CopperChestBlock copperchestblock1) {
         BlockState blockstate2 = p_428941_;
         BlockState blockstate1 = blockstate;
         if (copperchestblock.isWaxed() != copperchestblock1.isWaxed()) {
            blockstate2 = unwaxBlock(copperchestblock, p_428941_).orElse(p_428941_);
            blockstate1 = unwaxBlock(copperchestblock1, blockstate).orElse(blockstate);
         }

         Block block = copperchestblock.weatherState.ordinal() <= copperchestblock1.weatherState.ordinal() ? blockstate2.getBlock() : blockstate1.getBlock();
         return block.withPropertiesOf(blockstate2);
      } else {
         return p_428941_;
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_424785_,
      LevelReader p_427755_,
      ScheduledTickAccess p_430516_,
      BlockPos p_422490_,
      Direction p_429642_,
      BlockPos p_423254_,
      BlockState p_422810_,
      RandomSource p_424126_
   ) {
      BlockState blockstate = super.updateShape(p_424785_, p_427755_, p_430516_, p_422490_, p_429642_, p_423254_, p_422810_, p_424126_);
      if (this.chestCanConnectTo(p_422810_)) {
         ChestType chesttype = blockstate.getValue(ChestBlock.TYPE);
         if (!chesttype.equals(ChestType.SINGLE) && getConnectedDirection(blockstate) == p_429642_) {
            return p_422810_.getBlock().withPropertiesOf(blockstate);
         }
      }

      return blockstate;
   }

   private static Optional<BlockState> unwaxBlock(CopperChestBlock p_428303_, BlockState p_426608_) {
      return !p_428303_.isWaxed()
         ? Optional.of(p_426608_)
         : Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(p_426608_.getBlock()))
            .map(p_426033_ -> ((Block)p_426033_).withPropertiesOf(p_426608_));
   }

   public WeatheringCopper.WeatherState getState() {
      return this.weatherState;
   }

   public static BlockState getFromCopperBlock(Block p_430017_, Direction p_427385_, Level p_422943_, BlockPos p_425003_) {
      CopperChestBlock copperchestblock = (CopperChestBlock)COPPER_TO_COPPER_CHEST_MAPPING.getOrDefault(p_430017_, Blocks.COPPER_CHEST::asBlock).get();
      ChestType chesttype = copperchestblock.getChestType(p_422943_, p_425003_, p_427385_);
      BlockState blockstate = copperchestblock.defaultBlockState().setValue(FACING, p_427385_).setValue(TYPE, chesttype);
      return getLeastOxidizedChestOfConnectedBlocks(blockstate, p_422943_, p_425003_);
   }

   public boolean isWaxed() {
      return true;
   }

   @Override
   public boolean shouldChangedStateKeepBlockEntity(BlockState p_427179_) {
      return p_427179_.is(BlockTags.COPPER_CHESTS);
   }
}
