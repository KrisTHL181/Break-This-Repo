package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jspecify.annotations.Nullable;

public class AxeItem extends Item {
   protected static final Map<Block, Block> STRIPPABLES = new Builder()
      .put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
      .put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
      .put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
      .put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
      .put(Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD)
      .put(Blocks.PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG)
      .put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
      .put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
      .put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD)
      .put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
      .put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
      .put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
      .put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
      .put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
      .put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
      .put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
      .put(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM)
      .put(Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE)
      .put(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM)
      .put(Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE)
      .put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD)
      .put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
      .put(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK)
      .build();

   public AxeItem(ToolMaterial p_365403_, float p_363626_, float p_361899_, Item.Properties p_40524_) {
      super(p_40524_.axe(p_365403_, p_363626_, p_361899_));
   }

   @Override
   public InteractionResult useOn(UseOnContext p_40529_) {
      Level level = p_40529_.getLevel();
      BlockPos blockpos = p_40529_.getClickedPos();
      Player player = p_40529_.getPlayer();
      if (playerHasBlockingItemUseIntent(p_40529_)) {
         return InteractionResult.PASS;
      }

      Optional<BlockState> optional = this.evaluateNewBlockState(level, blockpos, player, level.getBlockState(blockpos));
      if (optional.isEmpty()) {
         return InteractionResult.PASS;
      }

      ItemStack itemstack = p_40529_.getItemInHand();
      if (player instanceof ServerPlayer) {
         CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemstack);
      }

      level.setBlock(blockpos, optional.get(), 11);
      level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, optional.get()));
      if (player != null) {
         itemstack.hurtAndBreak(1, player, p_40529_.getHand().asEquipmentSlot());
      }

      return InteractionResult.SUCCESS;
   }

   private static boolean playerHasBlockingItemUseIntent(UseOnContext p_393811_) {
      Player player = p_393811_.getPlayer();
      return p_393811_.getHand().equals(InteractionHand.MAIN_HAND)
         && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
         && !player.isSecondaryUseActive();
   }

   private Optional<BlockState> evaluateNewBlockState(Level p_312809_, BlockPos p_313114_, @Nullable Player p_312029_, BlockState p_311198_) {
      Optional<BlockState> optional = this.getStripped(p_311198_);
      if (optional.isPresent()) {
         p_312809_.playSound(p_312029_, p_313114_, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
         return optional;
      } else {
         Optional<BlockState> optional1 = WeatheringCopper.getPrevious(p_311198_);
         if (optional1.isPresent()) {
            spawnSoundAndParticle(p_312809_, p_313114_, p_312029_, p_311198_, SoundEvents.AXE_SCRAPE, 3005);
            return optional1;
         } else {
            Optional<BlockState> optional2 = Optional.ofNullable((Block)HoneycombItem.WAX_OFF_BY_BLOCK.get().get(p_311198_.getBlock()))
               .map(p_150694_ -> ((Block)p_150694_).withPropertiesOf(p_311198_));
            if (optional2.isPresent()) {
               spawnSoundAndParticle(p_312809_, p_313114_, p_312029_, p_311198_, SoundEvents.AXE_WAX_OFF, 3004);
               return optional2;
            } else {
               return Optional.empty();
            }
         }
      }
   }

   private static void spawnSoundAndParticle(
      Level p_430522_, BlockPos p_426660_, @Nullable Player p_430118_, BlockState p_423483_, SoundEvent p_431439_, int p_431501_
   ) {
      p_430522_.playSound(p_430118_, p_426660_, p_431439_, SoundSource.BLOCKS, 1.0F, 1.0F);
      p_430522_.levelEvent(p_430118_, p_431501_, p_426660_, 0);
      if (p_423483_.getBlock() instanceof ChestBlock && p_423483_.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
         BlockPos blockpos = ChestBlock.getConnectedBlockPos(p_426660_, p_423483_);
         p_430522_.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(p_430118_, p_430522_.getBlockState(blockpos)));
         p_430522_.levelEvent(p_430118_, p_431501_, blockpos, 0);
      }
   }

   private Optional<BlockState> getStripped(BlockState p_150691_) {
      return Optional.ofNullable(STRIPPABLES.get(p_150691_.getBlock()))
         .map(p_359378_ -> p_359378_.defaultBlockState().setValue(RotatedPillarBlock.AXIS, p_150691_.getValue(RotatedPillarBlock.AXIS)));
   }
}
