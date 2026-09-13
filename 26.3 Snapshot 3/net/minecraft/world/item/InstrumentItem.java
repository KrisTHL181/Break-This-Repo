package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
   public InstrumentItem(final Item.Properties properties) {
      super(properties);
   }

   public static ItemStack create(final Item item, final Holder<Instrument> instrument) {
      ItemStack itemStack = new ItemStack(item);
      itemStack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));
      return itemStack;
   }

   @Override
   public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
      ItemStack itemStack = player.getItemInHand(hand);
      Optional<? extends Holder<Instrument>> instrumentHolder = getInstrument(itemStack);
      if (instrumentHolder.isPresent()) {
         Instrument instrument = instrumentHolder.get().value();
         player.startUsingItem(hand);
         play(level, player, instrument);
         int durabilityDamage = instrument.durabilityDamage();
         if (durabilityDamage > 0) {
            itemStack.hurtAndBreak(durabilityDamage, player, hand.asEquipmentSlot());
         }

         float instrumentCooldown = instrument.useDuration();
         if (instrumentCooldown > 0.0F) {
            player.getCooldowns().addCooldown(itemStack, Mth.floor(instrumentCooldown * 20.0F));
         }

         player.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.FAIL;
      }
   }

   @Override
   public int getUseDuration(final ItemStack itemStack, final LivingEntity user) {
      Optional<Holder<Instrument>> instrument = getInstrument(itemStack);
      return instrument.<Integer>map(instrumentHolder -> Mth.floor(instrumentHolder.value().useDuration() * 20.0F)).orElse(0);
   }

   private static Optional<Holder<Instrument>> getInstrument(final ItemStack itemStack) {
      InstrumentComponent instrument = itemStack.get(DataComponents.INSTRUMENT);
      return instrument != null ? Optional.of(instrument.instrument()) : Optional.empty();
   }

   @Override
   public ItemUseAnimation getUseAnimation(final ItemStack itemStack) {
      return ItemUseAnimation.TOOT_HORN;
   }

   private static void play(final Level level, final Player player, final Instrument instrument) {
      SoundEvent soundEvent = instrument.soundEvent().value();
      float volume = instrument.range() / 16.0F;
      level.playSound(player, player, soundEvent, SoundSource.RECORDS, volume, 1.0F);
      level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
   }
}
