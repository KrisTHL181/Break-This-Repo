package net.minecraft.world.item;

import java.util.List;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BottleItem extends Item {
   public BottleItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      List<AreaEffectCloud> clouds = level.getEntitiesOfClass(
         AreaEffectCloud.class, player.getBoundingBox().inflate(2.0), input -> input.isAlive() && input.getOwner() instanceof EnderDragon
      );
      ItemStack itemStack = player.getItemInHand(hand);
      if (!clouds.isEmpty()) {
         AreaEffectCloud cloud = clouds.get(0);
         cloud.setRadius(cloud.getRadius() - 0.5F);
         level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
         level.gameEvent(player, GameEvent.FLUID_PICKUP, player.position());
         if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(serverPlayer, itemStack, cloud);
         }

         return InteractionResult.SUCCESS.heldItemTransformedTo(this.turnBottleIntoItem(itemStack, player, new ItemStack(Items.DRAGON_BREATH)));
      } else {
         BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
         if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
         }

         if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            if (!level.mayInteract(player, pos)) {
               return InteractionResult.PASS;
            }

            if (level.getFluidState(pos).is(FluidTags.WATER)) {
               level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
               level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
               return InteractionResult.SUCCESS
                  .heldItemTransformedTo(this.turnBottleIntoItem(itemStack, player, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
            }
         }

         return InteractionResult.PASS;
      }
   }

   protected ItemStack turnBottleIntoItem(final ItemStack itemStack, final Player player, final ItemStack itemStackToTurnInto) {
      player.awardStat(Stats.ITEM_USED.get(this));
      return ItemUtils.createFilledResult(itemStack, player, itemStackToTurnInto);
   }
}
