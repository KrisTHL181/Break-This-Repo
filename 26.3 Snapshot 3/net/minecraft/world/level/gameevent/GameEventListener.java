package net.minecraft.world.level.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface GameEventListener {
   PositionSource getListenerSource();

   int getListenerRadius();

   boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 sourcePosition);

   default GameEventListener.DeliveryMode getDeliveryMode() {
      return GameEventListener.DeliveryMode.UNSPECIFIED;
   }

   enum DeliveryMode {
      UNSPECIFIED,
      BY_DISTANCE;
   }

   interface Provider<T extends GameEventListener> {
      T getListener();
   }
}
