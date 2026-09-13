package net.minecraft.world.level.timers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class TimerQueue<T> extends SavedData {
   public static final Codec<TimerQueue<MinecraftServer>> CODEC = codec(TimerCallbacks.SERVER_CALLBACKS);
   public static final SavedDataType<TimerQueue<MinecraftServer>> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("scheduled_events"), TimerQueue::new, CODEC, DataFixTypes.SAVED_DATA_SCHEDULED_EVENTS
   );
   private final Queue<TimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
   private UnsignedLong sequentialId = UnsignedLong.ZERO;
   private final Table<String, Long, TimerQueue.Event<T>> events = HashBasedTable.create();

   @VisibleForTesting
   protected static <T> Codec<TimerQueue<T>> codec(final TimerCallbacks<T> callbacks) {
      return TimerQueue.Packed.codec(callbacks.codec()).xmap(TimerQueue::new, TimerQueue::pack);
   }

   private static <T> Comparator<TimerQueue.Event<T>> createComparator() {
      return Comparator.<TimerQueue.Event<T>>comparingLong(l -> l.triggerTime).thenComparing(l -> l.sequentialId);
   }

   public TimerQueue(final TimerQueue.Packed<T> packedEvents) {
      this();
      this.queue.clear();
      this.events.clear();
      this.sequentialId = UnsignedLong.ZERO;
      packedEvents.events.forEach(event -> this.schedule(event.id, event.triggerTime, event.callback));
   }

   public TimerQueue() {
   }

   public void tick(final T context, final long currentTick) {
      while (true) {
         TimerQueue.Event<T> event = this.queue.peek();
         if (event == null || event.triggerTime > currentTick) {
            return;
         }

         this.queue.remove();
         this.events.remove(event.id, currentTick);
         this.setDirty();
         event.callback.handle(context, this, currentTick);
      }
   }

   public void schedule(final String id, final long time, final TimerCallback<T> callback) {
      if (!this.events.contains(id, time)) {
         this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
         TimerQueue.Event<T> newEvent = new TimerQueue.Event<>(time, this.sequentialId, id, callback);
         this.events.put(id, time, newEvent);
         this.queue.add(newEvent);
         this.setDirty();
      }
   }

   public int remove(final String id) {
      Collection<TimerQueue.Event<T>> eventsToRemove = this.events.row(id).values();
      eventsToRemove.forEach(this.queue::remove);
      int size = eventsToRemove.size();
      eventsToRemove.clear();
      this.setDirty();
      return size;
   }

   public Set<String> getEventsIds() {
      return Collections.unmodifiableSet(this.events.rowKeySet());
   }

   @VisibleForTesting
   protected TimerQueue.Packed<T> pack() {
      return new TimerQueue.Packed<>(
         this.queue.stream().sorted(createComparator()).map(event -> new TimerQueue.Event.Packed<>(event.triggerTime, event.id, event.callback)).toList()
      );
   }

   public record Event<T>(long triggerTime, UnsignedLong sequentialId, String id, TimerCallback<T> callback) {
      public record Packed<T>(long triggerTime, String id, TimerCallback<T> callback) {
         public static <T> Codec<TimerQueue.Event.Packed<T>> codec(final Codec<TimerCallback<T>> callbackCodec) {
            return RecordCodecBuilder.create(
               i -> i.group(
                     Codec.LONG.fieldOf("trigger_time").forGetter(TimerQueue.Event.Packed::triggerTime),
                     Codec.STRING.fieldOf("id").forGetter(TimerQueue.Event.Packed::id),
                     callbackCodec.fieldOf("callback").forGetter(TimerQueue.Event.Packed::callback)
                  )
                  .apply(i, TimerQueue.Event.Packed::new)
            );
         }
      }
   }

   public record Packed<T>(List<TimerQueue.Event.Packed<T>> events) {
      public static <T> Codec<TimerQueue.Packed<T>> codec(final Codec<TimerCallback<T>> callbackCodec) {
         return RecordCodecBuilder.create(
            i -> i.group(TimerQueue.Event.Packed.codec(callbackCodec).listOf().fieldOf("events").forGetter(TimerQueue.Packed::events))
               .apply(i, TimerQueue.Packed::new)
         );
      }
   }
}
