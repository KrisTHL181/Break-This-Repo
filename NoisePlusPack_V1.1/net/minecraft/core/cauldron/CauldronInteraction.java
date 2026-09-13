package net.minecraft.core.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;

public interface CauldronInteraction {
   Map<String, CauldronInteraction.InteractionMap> INTERACTIONS = new Object2ObjectArrayMap();
   Codec<CauldronInteraction.InteractionMap> CODEC = Codec.stringResolver(CauldronInteraction.InteractionMap::name, INTERACTIONS::get);
   CauldronInteraction.InteractionMap EMPTY = newInteractionMap("empty");
   CauldronInteraction.InteractionMap WATER = newInteractionMap("water");
   CauldronInteraction.InteractionMap LAVA = newInteractionMap("lava");
   CauldronInteraction.InteractionMap POWDER_SNOW = newInteractionMap("powder_snow");

   static CauldronInteraction.InteractionMap newInteractionMap(String p_311265_) {
      Object2ObjectOpenHashMap<Item, CauldronInteraction> object2objectopenhashmap = new Object2ObjectOpenHashMap();
      object2objectopenhashmap.defaultReturnValue(
         (CauldronInteraction)(p_358117_, p_358118_, p_358119_, p_358120_, p_358121_, p_358122_) -> InteractionResult.TRY_WITH_EMPTY_HAND
      );
      CauldronInteraction.InteractionMap cauldroninteraction$interactionmap = new CauldronInteraction.InteractionMap(p_311265_, object2objectopenhashmap);
      INTERACTIONS.put(p_311265_, cauldroninteraction$interactionmap);
      return cauldroninteraction$interactionmap;
   }

   InteractionResult interact(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, ItemStack var6);

   static void bootStrap() {
      Map<Item, CauldronInteraction> map = EMPTY.map();
      addDefaultInteractions(map);
      map.put(Items.POTION, (p_175732_, p_175733_, p_175734_, p_175735_, p_175736_, p_175737_) -> {
         PotionContents potioncontents = p_175737_.get(DataComponents.POTION_CONTENTS);
         if (potioncontents != null && potioncontents.is(Potions.WATER)) {
            if (!p_175733_.isClientSide()) {
               Item item = p_175737_.getItem();
               p_175735_.setItemInHand(p_175736_, ItemUtils.createFilledResult(p_175737_, p_175735_, new ItemStack(Items.GLASS_BOTTLE)));
               p_175735_.awardStat(Stats.USE_CAULDRON);
               p_175735_.awardStat(Stats.ITEM_USED.get(item));
               p_175733_.setBlockAndUpdate(p_175734_, Blocks.WATER_CAULDRON.defaultBlockState());
               p_175733_.playSound(null, p_175734_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_175733_.gameEvent(null, GameEvent.FLUID_PLACE, p_175734_);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      });
      Map<Item, CauldronInteraction> map1 = WATER.map();
      addDefaultInteractions(map1);
      map1.put(
         Items.BUCKET,
         (p_358111_, p_358112_, p_358113_, p_358114_, p_358115_, p_358116_) -> fillBucket(
            p_358111_,
            p_358112_,
            p_358113_,
            p_358114_,
            p_358115_,
            p_358116_,
            new ItemStack(Items.WATER_BUCKET),
            p_175660_ -> p_175660_.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL
         )
      );
      map1.put(Items.GLASS_BOTTLE, (p_325758_, p_325759_, p_325760_, p_325761_, p_325762_, p_325763_) -> {
         if (!p_325759_.isClientSide()) {
            Item item = p_325763_.getItem();
            p_325761_.setItemInHand(p_325762_, ItemUtils.createFilledResult(p_325763_, p_325761_, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
            p_325761_.awardStat(Stats.USE_CAULDRON);
            p_325761_.awardStat(Stats.ITEM_USED.get(item));
            LayeredCauldronBlock.lowerFillLevel(p_325758_, p_325759_, p_325760_);
            p_325759_.playSound(null, p_325760_, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_325759_.gameEvent(null, GameEvent.FLUID_PICKUP, p_325760_);
         }

         return InteractionResult.SUCCESS;
      });
      map1.put(Items.POTION, (p_175704_, p_175705_, p_175706_, p_175707_, p_175708_, p_175709_) -> {
         if (p_175704_.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }

         PotionContents potioncontents = p_175709_.get(DataComponents.POTION_CONTENTS);
         if (potioncontents != null && potioncontents.is(Potions.WATER)) {
            if (!p_175705_.isClientSide()) {
               p_175707_.setItemInHand(p_175708_, ItemUtils.createFilledResult(p_175709_, p_175707_, new ItemStack(Items.GLASS_BOTTLE)));
               p_175707_.awardStat(Stats.USE_CAULDRON);
               p_175707_.awardStat(Stats.ITEM_USED.get(p_175709_.getItem()));
               p_175705_.setBlockAndUpdate(p_175706_, p_175704_.cycle(LayeredCauldronBlock.LEVEL));
               p_175705_.playSound(null, p_175706_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_175705_.gameEvent(null, GameEvent.FLUID_PLACE, p_175706_);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      });
      map1.put(Items.LEATHER_BOOTS, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_LEGGINGS, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_CHESTPLATE, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_HELMET, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_HORSE_ARMOR, CauldronInteraction::dyedItemIteration);
      map1.put(Items.WOLF_ARMOR, CauldronInteraction::dyedItemIteration);
      map1.put(Items.WHITE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.GRAY_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BLACK_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BLUE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BROWN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.CYAN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.GREEN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIGHT_BLUE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIGHT_GRAY_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIME_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.MAGENTA_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.ORANGE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.PINK_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.PURPLE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.RED_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.YELLOW_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.WHITE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.GRAY_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BLACK_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BLUE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BROWN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.CYAN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.GREEN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIGHT_BLUE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIGHT_GRAY_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIME_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.MAGENTA_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.ORANGE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.PINK_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.PURPLE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.RED_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.YELLOW_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      Map<Item, CauldronInteraction> map2 = LAVA.map();
      map2.put(
         Items.BUCKET,
         (p_358123_, p_358124_, p_358125_, p_358126_, p_358127_, p_358128_) -> fillBucket(
            p_358123_, p_358124_, p_358125_, p_358126_, p_358127_, p_358128_, new ItemStack(Items.LAVA_BUCKET), p_175651_ -> true, SoundEvents.BUCKET_FILL_LAVA
         )
      );
      addDefaultInteractions(map2);
      Map<Item, CauldronInteraction> map3 = POWDER_SNOW.map();
      map3.put(
         Items.BUCKET,
         (p_358105_, p_358106_, p_358107_, p_358108_, p_358109_, p_358110_) -> fillBucket(
            p_358105_,
            p_358106_,
            p_358107_,
            p_358108_,
            p_358109_,
            p_358110_,
            new ItemStack(Items.POWDER_SNOW_BUCKET),
            p_175627_ -> p_175627_.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL_POWDER_SNOW
         )
      );
      addDefaultInteractions(map3);
   }

   static void addDefaultInteractions(Map<Item, CauldronInteraction> p_175648_) {
      p_175648_.put(Items.LAVA_BUCKET, CauldronInteraction::fillLavaInteraction);
      p_175648_.put(Items.WATER_BUCKET, CauldronInteraction::fillWaterInteraction);
      p_175648_.put(Items.POWDER_SNOW_BUCKET, CauldronInteraction::fillPowderSnowInteraction);
   }

   static InteractionResult fillBucket(
      BlockState p_175636_,
      Level p_175637_,
      BlockPos p_175638_,
      Player p_175639_,
      InteractionHand p_175640_,
      ItemStack p_175641_,
      ItemStack p_175642_,
      Predicate<BlockState> p_175643_,
      SoundEvent p_175644_
   ) {
      if (!p_175643_.test(p_175636_)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!p_175637_.isClientSide()) {
         Item item = p_175641_.getItem();
         p_175639_.setItemInHand(p_175640_, ItemUtils.createFilledResult(p_175641_, p_175639_, p_175642_));
         p_175639_.awardStat(Stats.USE_CAULDRON);
         p_175639_.awardStat(Stats.ITEM_USED.get(item));
         p_175637_.setBlockAndUpdate(p_175638_, Blocks.CAULDRON.defaultBlockState());
         p_175637_.playSound(null, p_175638_, p_175644_, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_175637_.gameEvent(null, GameEvent.FLUID_PICKUP, p_175638_);
      }

      return InteractionResult.SUCCESS;
   }

   static InteractionResult emptyBucket(
      Level p_175619_, BlockPos p_175620_, Player p_175621_, InteractionHand p_175622_, ItemStack p_175623_, BlockState p_175624_, SoundEvent p_175625_
   ) {
      if (!p_175619_.isClientSide()) {
         Item item = p_175623_.getItem();
         p_175621_.setItemInHand(p_175622_, ItemUtils.createFilledResult(p_175623_, p_175621_, new ItemStack(Items.BUCKET)));
         p_175621_.awardStat(Stats.FILL_CAULDRON);
         p_175621_.awardStat(Stats.ITEM_USED.get(item));
         p_175619_.setBlockAndUpdate(p_175620_, p_175624_);
         p_175619_.playSound(null, p_175620_, p_175625_, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_175619_.gameEvent(null, GameEvent.FLUID_PLACE, p_175620_);
      }

      return InteractionResult.SUCCESS;
   }

   private static InteractionResult fillWaterInteraction(
      BlockState p_363465_, Level p_369690_, BlockPos p_365994_, Player p_361538_, InteractionHand p_363296_, ItemStack p_369551_
   ) {
      return emptyBucket(
         p_369690_,
         p_365994_,
         p_361538_,
         p_363296_,
         p_369551_,
         Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
         SoundEvents.BUCKET_EMPTY
      );
   }

   private static InteractionResult fillLavaInteraction(
      BlockState p_365957_, Level p_368892_, BlockPos p_365280_, Player p_368758_, InteractionHand p_369203_, ItemStack p_369309_
   ) {
      return isUnderWater(p_368892_, p_365280_)
         ? InteractionResult.CONSUME
         : emptyBucket(p_368892_, p_365280_, p_368758_, p_369203_, p_369309_, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA);
   }

   private static InteractionResult fillPowderSnowInteraction(
      BlockState p_367322_, Level p_368177_, BlockPos p_369168_, Player p_362349_, InteractionHand p_363299_, ItemStack p_365742_
   ) {
      return isUnderWater(p_368177_, p_369168_)
         ? InteractionResult.CONSUME
         : emptyBucket(
            p_368177_,
            p_369168_,
            p_362349_,
            p_363299_,
            p_365742_,
            Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
            SoundEvents.BUCKET_EMPTY_POWDER_SNOW
         );
   }

   private static InteractionResult shulkerBoxInteraction(
      BlockState p_361616_, Level p_361740_, BlockPos p_363368_, Player p_365016_, InteractionHand p_367201_, ItemStack p_364495_
   ) {
      Block block = Block.byItem(p_364495_.getItem());
      if (!(block instanceof ShulkerBoxBlock)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!p_361740_.isClientSide()) {
         ItemStack itemstack = p_364495_.transmuteCopy(Blocks.SHULKER_BOX, 1);
         p_365016_.setItemInHand(p_367201_, ItemUtils.createFilledResult(p_364495_, p_365016_, itemstack, false));
         p_365016_.awardStat(Stats.CLEAN_SHULKER_BOX);
         LayeredCauldronBlock.lowerFillLevel(p_361616_, p_361740_, p_363368_);
      }

      return InteractionResult.SUCCESS;
   }

   private static InteractionResult bannerInteraction(
      BlockState p_367762_, Level p_366713_, BlockPos p_368348_, Player p_365632_, InteractionHand p_369503_, ItemStack p_363311_
   ) {
      BannerPatternLayers bannerpatternlayers = p_363311_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      if (bannerpatternlayers.layers().isEmpty()) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!p_366713_.isClientSide()) {
         ItemStack itemstack = p_363311_.copyWithCount(1);
         itemstack.set(DataComponents.BANNER_PATTERNS, bannerpatternlayers.removeLast());
         p_365632_.setItemInHand(p_369503_, ItemUtils.createFilledResult(p_363311_, p_365632_, itemstack, false));
         p_365632_.awardStat(Stats.CLEAN_BANNER);
         LayeredCauldronBlock.lowerFillLevel(p_367762_, p_366713_, p_368348_);
      }

      return InteractionResult.SUCCESS;
   }

   private static InteractionResult dyedItemIteration(
      BlockState p_367064_, Level p_365282_, BlockPos p_365414_, Player p_364718_, InteractionHand p_362544_, ItemStack p_368695_
   ) {
      if (!p_368695_.is(ItemTags.DYEABLE)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!p_368695_.has(DataComponents.DYED_COLOR)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }

      if (!p_365282_.isClientSide()) {
         p_368695_.remove(DataComponents.DYED_COLOR);
         p_364718_.awardStat(Stats.CLEAN_ARMOR);
         LayeredCauldronBlock.lowerFillLevel(p_367064_, p_365282_, p_365414_);
      }

      return InteractionResult.SUCCESS;
   }

   private static boolean isUnderWater(Level p_362699_, BlockPos p_366592_) {
      FluidState fluidstate = p_362699_.getFluidState(p_366592_.above());
      return fluidstate.is(FluidTags.WATER);
   }

   record InteractionMap(String name, Map<Item, CauldronInteraction> map) {
   }
}
