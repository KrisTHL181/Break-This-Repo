package net.minecraft.world.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class FishingRodItem extends Item {
   public FishingRodItem(final Item.Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      if (player.fishing != null) {
         if (!level.isClientSide()) {
            int dmg = player.fishing.retrieve(itemStack);
            itemStack.hurtAndBreak(dmg, player, hand.asEquipmentSlot());
         }

         level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.FISHING_BOBBER_RETRIEVE,
            SoundSource.NEUTRAL,
            1.0F,
            0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
         );
         itemStack.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);
      } else {
         level.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.FISHING_BOBBER_THROW,
            SoundSource.NEUTRAL,
            0.5F,
            0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
         );
         if (level instanceof ServerLevel serverLevel) {
            int lureSpeed = (int)(EnchantmentHelper.getFishingTimeReduction(serverLevel, itemStack, player) * 20.0F);
            int luck = EnchantmentHelper.getFishingLuckBonus(serverLevel, itemStack, player);
            Projectile.spawnProjectile(new FishingHook(player, level, luck, lureSpeed), serverLevel, itemStack);
         }

         player.awardStat(Stats.ITEM_USED.get(this));
         itemStack.causeUseVibration(player, GameEvent.ITEM_INTERACT_START);
      }

      return InteractionResult.SUCCESS;
   }
}
