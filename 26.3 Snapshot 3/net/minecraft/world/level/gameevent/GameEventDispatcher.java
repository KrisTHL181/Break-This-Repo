package net.minecraft.world.level.gameevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugGameEventInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.Vec3;

public class GameEventDispatcher {
   private final ServerLevel level;

   public GameEventDispatcher(final ServerLevel level) {
      this.level = level;
   }

   public void post(final Holder<GameEvent> gameEvent, final Vec3 position, final GameEvent.Context context) {
      int radius = gameEvent.value().notificationRadius();
      BlockPos center = BlockPos.containing(position);
      int sectionMinX = SectionPos.blockToSectionCoord(center.getX() - radius);
      int sectionMinY = SectionPos.blockToSectionCoord(center.getY() - radius);
      int sectionMinZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
      int sectionMaxX = SectionPos.blockToSectionCoord(center.getX() + radius);
      int sectionMaxY = SectionPos.blockToSectionCoord(center.getY() + radius);
      int sectionMaxZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
      List<GameEvent.ListenerInfo> toHandleByDistance = new ArrayList<>();
      GameEventListenerRegistry.ListenerVisitor visitListeners = (listener, pos) -> {
         if (listener.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE) {
            toHandleByDistance.add(new GameEvent.ListenerInfo(gameEvent, position, context, listener, pos));
         } else {
            listener.handleGameEvent(this.level, gameEvent, context, position);
         }
      };
      boolean applicable = false;

      for (int chunkX = sectionMinX; chunkX <= sectionMaxX; chunkX++) {
         for (int chunkZ = sectionMinZ; chunkZ <= sectionMaxZ; chunkZ++) {
            ChunkAccess chunk = this.level.getChunkSource().getChunkNow(chunkX, chunkZ);
            if (chunk != null) {
               for (int section = sectionMinY; section <= sectionMaxY; section++) {
                  applicable |= chunk.getListenerRegistry(section).visitInRangeListeners(gameEvent, position, context, visitListeners);
               }
            }
         }
      }

      if (!toHandleByDistance.isEmpty()) {
         this.handleGameEventMessagesInQueue(toHandleByDistance);
      }

      if (applicable) {
         this.level
            .debugSynchronizers()
            .broadcastEventToTracking(BlockPos.containing(position), DebugSubscriptions.GAME_EVENTS, new DebugGameEventInfo(gameEvent, position));
      }
   }

   private void handleGameEventMessagesInQueue(final List<GameEvent.ListenerInfo> listenerInfos) {
      Collections.sort(listenerInfos);

      for (GameEvent.ListenerInfo listenerInfo : listenerInfos) {
         GameEventListener listener = listenerInfo.recipient();
         listener.handleGameEvent(this.level, listenerInfo.gameEvent(), listenerInfo.context(), listenerInfo.source());
      }
   }
}
