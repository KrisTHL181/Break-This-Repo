package net.minecraft.world.entity.ai.memory;

import net.minecraft.world.entity.ai.Brain;
import org.jspecify.annotations.Nullable;

public class MemorySlot<T> {
   private static final long NEVER_EXPIRE = Long.MAX_VALUE;
   private @Nullable T value;
   private long timeToLive;

   private MemorySlot(final @Nullable T value, final long timeToLive) {
      this.value = value;
      this.timeToLive = timeToLive;
   }

   public void tick() {
      if (this.hasValue() && this.canExpire()) {
         if (this.hasExpired()) {
            this.clear();
         } else {
            this.timeToLive--;
         }
      }
   }

   public static <T> MemorySlot<T> create() {
      return new MemorySlot<>(null, Long.MAX_VALUE);
   }

   public void set(final T value, final long timeToLive) {
      this.value = value;
      this.timeToLive = timeToLive;
   }

   public void set(final T value) {
      this.set(value, Long.MAX_VALUE);
   }

   public void clear() {
      this.value = null;
      this.timeToLive = Long.MAX_VALUE;
   }

   public boolean hasValue() {
      return this.value != null;
   }

   public @Nullable T value() {
      return this.value;
   }

   public boolean canExpire() {
      return this.timeToLive != Long.MAX_VALUE;
   }

   public boolean hasExpired() {
      return this.timeToLive <= 0L;
   }

   public long timeToLive() {
      return this.timeToLive;
   }

   @Override
   public String toString() {
      return this.value == null ? "<empty>" : this.value + (this.canExpire() ? " (ttl: " + this.timeToLive + ")" : "");
   }

   public void visit(final MemoryModuleType<T> type, final Brain.Visitor visitor) {
      if (this.value != null) {
         if (this.canExpire()) {
            visitor.accept(type, this.value, this.timeToLive);
         } else {
            visitor.accept(type, this.value);
         }
      } else {
         visitor.acceptEmpty(type);
      }
   }
}
