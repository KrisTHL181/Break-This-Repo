package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.debug.DebugGameEventListenerInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EuclideanGameEventListenerRegistry implements GameEventListenerRegistry {
   private final List<GameEventListener> listeners = Lists.newArrayList();
   private final Set<GameEventListener> listenersToRemove = Sets.newHashSet();
   private final List<GameEventListener> listenersToAdd = Lists.newArrayList();
   private boolean processing;
   private final ServerLevel level;
   private final int sectionY;
   private final EuclideanGameEventListenerRegistry.OnEmptyAction onEmptyAction;

   public EuclideanGameEventListenerRegistry(final ServerLevel level, final int sectionY, final EuclideanGameEventListenerRegistry.OnEmptyAction onEmptyAction) {
      this.level = level;
      this.sectionY = sectionY;
      this.onEmptyAction = onEmptyAction;
   }

   @Override
   public boolean isEmpty() {
      return this.listeners.isEmpty();
   }

   @Override
   public void register(final GameEventListener listener) {
      if (this.processing) {
         this.listenersToAdd.add(listener);
      } else {
         this.listeners.add(listener);
      }

      sendDebugInfo(this.level, listener);
   }

   private static void sendDebugInfo(final ServerLevel level, final GameEventListener listener) {
      if (level.debugSynchronizers().hasAnySubscriberFor(DebugSubscriptions.GAME_EVENT_LISTENERS)) {
         DebugGameEventListenerInfo info = new DebugGameEventListenerInfo(listener.getListenerRadius());
         PositionSource listenerSource = listener.getListenerSource();
         if (listenerSource instanceof BlockPositionSource blockSource) {
            level.debugSynchronizers().sendBlockValue(blockSource.pos(), DebugSubscriptions.GAME_EVENT_LISTENERS, info);
         } else if (listenerSource instanceof EntityPositionSource entitySource) {
            Entity entity = level.getEntity(entitySource.getUuid());
            if (entity != null) {
               level.debugSynchronizers().sendEntityValue(entity, DebugSubscriptions.GAME_EVENT_LISTENERS, info);
            }
         }
      }
   }

   @Override
   public void unregister(final GameEventListener listener) {
      if (this.processing) {
         this.listenersToRemove.add(listener);
      } else {
         this.listeners.remove(listener);
      }

      if (this.listeners.isEmpty()) {
         this.onEmptyAction.apply(this.sectionY);
      }
   }

   @Override
   public boolean visitInRangeListeners(
      final Holder<GameEvent> event, final Vec3 sourcePosition, final GameEvent.Context context, final GameEventListenerRegistry.ListenerVisitor action
   ) {
      this.processing = true;
      boolean applicable = false;

      try {
         Iterator<GameEventListener> iterator = this.listeners.iterator();

         while (iterator.hasNext()) {
            GameEventListener listener = iterator.next();
            if (this.listenersToRemove.remove(listener)) {
               iterator.remove();
            } else {
               Optional<Vec3> optionalPosition = getPostableListenerPosition(this.level, sourcePosition, listener);
               if (optionalPosition.isPresent()) {
                  action.visit(listener, optionalPosition.get());
                  applicable = true;
               }
            }
         }
      } finally {
         this.processing = false;
      }

      if (!this.listenersToAdd.isEmpty()) {
         this.listeners.addAll(this.listenersToAdd);
         this.listenersToAdd.clear();
      }

      if (!this.listenersToRemove.isEmpty()) {
         this.listeners.removeAll(this.listenersToRemove);
         this.listenersToRemove.clear();
      }

      return applicable;
   }

   private static Optional<Vec3> getPostableListenerPosition(final ServerLevel level, final Vec3 sourcePosition, final GameEventListener listener) {
      Optional<Vec3> position = listener.getListenerSource().getPosition(level);
      if (position.isEmpty()) {
         return Optional.empty();
      }

      double distanceFromOrigin = BlockPos.containing(position.get()).distSqr(BlockPos.containing(sourcePosition));
      int radiusSqr = listener.getListenerRadius() * listener.getListenerRadius();
      return distanceFromOrigin > radiusSqr ? Optional.empty() : position;
   }

   @FunctionalInterface
   public interface OnEmptyAction {
      void apply(final int sectionY);
   }
}
