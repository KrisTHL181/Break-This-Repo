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

   public GameEventDispatcher(ServerLevel p_251921_) {
      this.level = p_251921_;
   }

   public void post(Holder<GameEvent> p_335078_, Vec3 p_250613_, GameEvent.Context p_251777_) {
      int i = p_335078_.value().notificationRadius();
      BlockPos blockpos = BlockPos.containing(p_250613_);
      int j = SectionPos.blockToSectionCoord(blockpos.getX() - i);
      int k = SectionPos.blockToSectionCoord(blockpos.getY() - i);
      int l = SectionPos.blockToSectionCoord(blockpos.getZ() - i);
      int i1 = SectionPos.blockToSectionCoord(blockpos.getX() + i);
      int j1 = SectionPos.blockToSectionCoord(blockpos.getY() + i);
      int k1 = SectionPos.blockToSectionCoord(blockpos.getZ() + i);
      List<GameEvent.ListenerInfo> list = new ArrayList<>();
      GameEventListenerRegistry.ListenerVisitor gameeventlistenerregistry$listenervisitor = (p_327435_, p_327436_) -> {
         if (p_327435_.getDeliveryMode() == GameEventListener.DeliveryMode.BY_DISTANCE) {
            list.add(new GameEvent.ListenerInfo(p_335078_, p_250613_, p_251777_, p_327435_, p_327436_));
         } else {
            p_327435_.handleGameEvent(this.level, p_335078_, p_251777_, p_250613_);
         }
      };
      boolean flag = false;

      for (int l1 = j; l1 <= i1; l1++) {
         for (int i2 = l; i2 <= k1; i2++) {
            ChunkAccess chunkaccess = this.level.getChunkSource().getChunkNow(l1, i2);
            if (chunkaccess != null) {
               for (int j2 = k; j2 <= j1; j2++) {
                  flag |= chunkaccess.getListenerRegistry(j2).visitInRangeListeners(p_335078_, p_250613_, p_251777_, gameeventlistenerregistry$listenervisitor);
               }
            }
         }
      }

      if (!list.isEmpty()) {
         this.handleGameEventMessagesInQueue(list);
      }

      if (flag) {
         this.level
            .debugSynchronizers()
            .broadcastEventToTracking(BlockPos.containing(p_250613_), DebugSubscriptions.GAME_EVENTS, new DebugGameEventInfo(p_335078_, p_250613_));
      }
   }

   private void handleGameEventMessagesInQueue(List<GameEvent.ListenerInfo> p_251433_) {
      Collections.sort(p_251433_);

      for (GameEvent.ListenerInfo gameevent$listenerinfo : p_251433_) {
         GameEventListener gameeventlistener = gameevent$listenerinfo.recipient();
         gameeventlistener.handleGameEvent(this.level, gameevent$listenerinfo.gameEvent(), gameevent$listenerinfo.context(), gameevent$listenerinfo.source());
      }
   }
}
