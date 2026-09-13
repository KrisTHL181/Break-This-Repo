package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class HoeItem extends Item {
   protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.newHashMap(
      ImmutableMap.of(
         Blocks.GRASS_BLOCK,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.DIRT_PATH,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.DIRT,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())),
         Blocks.COARSE_DIRT,
         Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.DIRT.defaultBlockState())),
         Blocks.ROOTED_DIRT,
         Pair.of((Predicate<UseOnContext>)p_238242_ -> true, changeIntoStateAndDropItem(Blocks.DIRT.defaultBlockState(), Items.HANGING_ROOTS))
      )
   );

   public HoeItem(ToolMaterial p_369501_, float p_368838_, float p_361631_, Item.Properties p_41339_) {
      super(p_41339_.hoe(p_369501_, p_368838_, p_361631_));
   }

   @Override
   public InteractionResult useOn(UseOnContext p_41341_) {
      Level level = p_41341_.getLevel();
      BlockPos blockpos = p_41341_.getClickedPos();
      Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = TILLABLES.get(level.getBlockState(blockpos).getBlock());
      if (pair == null) {
         return InteractionResult.PASS;
      }

      Predicate<UseOnContext> predicate = (Predicate<UseOnContext>)pair.getFirst();
      Consumer<UseOnContext> consumer = (Consumer<UseOnContext>)pair.getSecond();
      if (predicate.test(p_41341_)) {
         Player player = p_41341_.getPlayer();
         level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!level.isClientSide()) {
            consumer.accept(p_41341_);
            if (player != null) {
               p_41341_.getItemInHand().hurtAndBreak(1, player, p_41341_.getHand().asEquipmentSlot());
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static Consumer<UseOnContext> changeIntoState(BlockState p_150859_) {
      return p_327147_ -> {
         p_327147_.getLevel().setBlock(p_327147_.getClickedPos(), p_150859_, 11);
         p_327147_.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, p_327147_.getClickedPos(), GameEvent.Context.of(p_327147_.getPlayer(), p_150859_));
      };
   }

   public static Consumer<UseOnContext> changeIntoStateAndDropItem(BlockState p_150850_, ItemLike p_150851_) {
      return p_327150_ -> {
         p_327150_.getLevel().setBlock(p_327150_.getClickedPos(), p_150850_, 11);
         p_327150_.getLevel().gameEvent(GameEvent.BLOCK_CHANGE, p_327150_.getClickedPos(), GameEvent.Context.of(p_327150_.getPlayer(), p_150850_));
         Block.popResourceFromFace(p_327150_.getLevel(), p_327150_.getClickedPos(), p_327150_.getClickedFace(), new ItemStack(p_150851_));
      };
   }

   public static boolean onlyIfAirAbove(UseOnContext p_150857_) {
      return p_150857_.getClickedFace() != Direction.DOWN && p_150857_.getLevel().getBlockState(p_150857_.getClickedPos().above()).isAir();
   }
}
