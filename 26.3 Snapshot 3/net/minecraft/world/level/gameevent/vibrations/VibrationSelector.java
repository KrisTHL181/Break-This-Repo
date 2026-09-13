package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public class VibrationSelector {
   public static final Codec<VibrationSelector> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(o -> o.currentVibrationData.map(VibrationSelector.VibrationEvent::event)),
            Codec.LONG.fieldOf("tick").forGetter(o -> o.currentVibrationData.map(VibrationSelector.VibrationEvent::tick).orElse(-1L))
         )
         .apply(i, VibrationSelector::new)
   );
   private Optional<VibrationSelector.VibrationEvent> currentVibrationData;

   public VibrationSelector(final Optional<VibrationInfo> currentVibration, final long tick) {
      this.currentVibrationData = currentVibration.map(vibrationInfo -> new VibrationSelector.VibrationEvent(vibrationInfo, tick));
   }

   public VibrationSelector() {
      this.currentVibrationData = Optional.empty();
   }

   public void addCandidate(final VibrationInfo newVibration, final long tickTime) {
      if (this.shouldReplaceVibration(newVibration, tickTime)) {
         this.currentVibrationData = Optional.of(new VibrationSelector.VibrationEvent(newVibration, tickTime));
      }
   }

   private boolean shouldReplaceVibration(final VibrationInfo newVibration, final long tickTime) {
      if (this.currentVibrationData.isEmpty()) {
         return true;
      } else {
         VibrationSelector.VibrationEvent previousData = this.currentVibrationData.get();
         long previousTick = previousData.tick();
         if (tickTime != previousTick) {
            return false;
         } else {
            VibrationInfo previousVibration = previousData.event();
            if (newVibration.distance() < previousVibration.distance()) {
               return true;
            } else {
               return newVibration.distance() > previousVibration.distance()
                  ? false
                  : VibrationSystem.getGameEventFrequency(newVibration.gameEvent()) > VibrationSystem.getGameEventFrequency(previousVibration.gameEvent());
            }
         }
      }
   }

   public Optional<VibrationInfo> chosenCandidate(final long time) {
      if (this.currentVibrationData.isEmpty()) {
         return Optional.empty();
      } else {
         return this.currentVibrationData.get().tick() < time ? Optional.of(this.currentVibrationData.get().event()) : Optional.empty();
      }
   }

   public void startOver() {
      this.currentVibrationData = Optional.empty();
   }

   private record VibrationEvent(VibrationInfo event, long tick) {
   }
}
