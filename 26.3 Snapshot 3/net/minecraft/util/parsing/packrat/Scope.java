package net.minecraft.util.parsing.packrat;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public final class Scope {
   private static final int NOT_FOUND = -1;
   private static final Object FRAME_START_MARKER = new Object() {
      @Override
      public String toString() {
         return "frame";
      }
   };
   private static final int ENTRY_STRIDE = 2;
   private @Nullable Object[] stack = new Object[128];
   private int topEntryKeyIndex = 0;
   private int topMarkerKeyIndex = 0;

   public Scope() {
      this.stack[0] = FRAME_START_MARKER;
      this.stack[1] = null;
   }

   private int valueIndex(final Atom<?> atom) {
      for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
         Object key = this.stack[i];
         assert key instanceof Atom;
         if (key == atom) {
            return i + 1;
         }
      }

      return -1;
   }

   public int valueIndexForAny(final Atom<?>... atoms) {
      for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
         Object key = this.stack[i];
         assert key instanceof Atom;

         for (Atom<?> atom : atoms) {
            if (atom == key) {
               return i + 1;
            }
         }
      }

      return -1;
   }

   private void ensureCapacity(final int additionalEntryCount) {
      int currentSize = this.stack.length;
      int currentLastValueIndex = this.topEntryKeyIndex + 1;
      int newLastValueIndex = currentLastValueIndex + additionalEntryCount * 2;
      if (newLastValueIndex >= currentSize) {
         int newSize = Util.growByHalf(currentSize, newLastValueIndex + 1);
         Object[] newStack = new Object[newSize];
         System.arraycopy(this.stack, 0, newStack, 0, currentSize);
         this.stack = newStack;
      }

      assert this.validateStructure();
   }

   private void setupNewFrame() {
      this.topEntryKeyIndex += 2;
      this.stack[this.topEntryKeyIndex] = FRAME_START_MARKER;
      this.stack[this.topEntryKeyIndex + 1] = this.topMarkerKeyIndex;
      this.topMarkerKeyIndex = this.topEntryKeyIndex;
   }

   public void pushFrame() {
      this.ensureCapacity(1);
      this.setupNewFrame();
      assert this.validateStructure();
   }

   private int getPreviousMarkerIndex(final int markerKeyIndex) {
      return (Integer)this.stack[markerKeyIndex + 1];
   }

   public void popFrame() {
      assert this.topMarkerKeyIndex != 0;
      this.topEntryKeyIndex = this.topMarkerKeyIndex - 2;
      this.topMarkerKeyIndex = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);
      assert this.validateStructure();
   }

   public void splitFrame() {
      int currentFrameMarkerIndex = this.topMarkerKeyIndex;
      int nonMarkerEntriesInFrame = (this.topEntryKeyIndex - this.topMarkerKeyIndex) / 2;
      this.ensureCapacity(nonMarkerEntriesInFrame + 1);
      this.setupNewFrame();
      int sourceCursor = currentFrameMarkerIndex + 2;
      int targetCursor = this.topEntryKeyIndex;

      for (int i = 0; i < nonMarkerEntriesInFrame; i++) {
         targetCursor += 2;
         Object key = this.stack[sourceCursor];
         assert key != null;
         this.stack[targetCursor] = key;
         this.stack[targetCursor + 1] = null;
         sourceCursor += 2;
      }

      this.topEntryKeyIndex = targetCursor;
      assert this.validateStructure();
   }

   public void clearFrameValues() {
      for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
         assert this.stack[i] instanceof Atom;
         this.stack[i + 1] = null;
      }

      assert this.validateStructure();
   }

   public void mergeFrame() {
      int previousMarkerIndex = this.getPreviousMarkerIndex(this.topMarkerKeyIndex);
      int previousFrameCursor = previousMarkerIndex;
      int currentFrameCursor = this.topMarkerKeyIndex;

      while (currentFrameCursor < this.topEntryKeyIndex) {
         previousFrameCursor += 2;
         currentFrameCursor += 2;
         Object newKey = this.stack[currentFrameCursor];
         assert newKey instanceof Atom;
         Object newValue = this.stack[currentFrameCursor + 1];
         Object oldKey = this.stack[previousFrameCursor];
         if (oldKey != newKey) {
            this.stack[previousFrameCursor] = newKey;
            this.stack[previousFrameCursor + 1] = newValue;
         } else if (newValue != null) {
            this.stack[previousFrameCursor + 1] = newValue;
         }
      }

      this.topEntryKeyIndex = previousFrameCursor;
      this.topMarkerKeyIndex = previousMarkerIndex;
      assert this.validateStructure();
   }

   public <T> void put(final Atom<T> name, final @Nullable T value) {
      int valueIndex = this.valueIndex(name);
      if (valueIndex != -1) {
         this.stack[valueIndex] = value;
      } else {
         this.ensureCapacity(1);
         this.topEntryKeyIndex += 2;
         this.stack[this.topEntryKeyIndex] = name;
         this.stack[this.topEntryKeyIndex + 1] = value;
      }

      assert this.validateStructure();
   }

   public <T> @Nullable T get(final Atom<T> name) {
      int valueIndex = this.valueIndex(name);
      return (T)(valueIndex != -1 ? this.stack[valueIndex] : null);
   }

   public <T> T getOrThrow(final Atom<T> name) {
      int valueIndex = this.valueIndex(name);
      if (valueIndex == -1) {
         throw new IllegalArgumentException("No value for atom " + name);
      } else {
         return (T)this.stack[valueIndex];
      }
   }

   public <T> T getOrDefault(final Atom<T> name, final T fallback) {
      int valueIndex = this.valueIndex(name);
      return (T)(valueIndex != -1 ? this.stack[valueIndex] : fallback);
   }

   @SafeVarargs
   public final <T> @Nullable T getAny(final Atom<? extends T>... names) {
      int valueIndex = this.valueIndexForAny(names);
      return (T)(valueIndex != -1 ? this.stack[valueIndex] : null);
   }

   @SafeVarargs
   public final <T> T getAnyOrThrow(final Atom<? extends T>... names) {
      int valueIndex = this.valueIndexForAny(names);
      if (valueIndex == -1) {
         throw new IllegalArgumentException("No value for atoms " + Arrays.toString(names));
      } else {
         return (T)this.stack[valueIndex];
      }
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      boolean afterFrame = true;

      for (int i = 0; i <= this.topEntryKeyIndex; i += 2) {
         Object key = this.stack[i];
         Object value = this.stack[i + 1];
         if (key == FRAME_START_MARKER) {
            result.append('|');
            afterFrame = true;
         } else {
            if (!afterFrame) {
               result.append(',');
            }

            afterFrame = false;
            result.append(key).append(':').append(value);
         }
      }

      return result.toString();
   }

   @VisibleForTesting
   public Map<Atom<?>, ?> lastFrame() {
      HashMap<Atom<?>, Object> result = new HashMap<>();

      for (int i = this.topEntryKeyIndex; i > this.topMarkerKeyIndex; i -= 2) {
         Object key = this.stack[i];
         Object value = this.stack[i + 1];
         result.put((Atom<?>)key, value);
      }

      return result;
   }

   public boolean hasOnlySingleFrame() {
      for (int i = this.topEntryKeyIndex; i > 0; i--) {
         if (this.stack[i] == FRAME_START_MARKER) {
            return false;
         }
      }

      if (this.stack[0] != FRAME_START_MARKER) {
         throw new IllegalStateException("Corrupted stack");
      } else {
         return true;
      }
   }

   private boolean validateStructure() {
      assert this.topMarkerKeyIndex >= 0;
      assert this.topEntryKeyIndex >= this.topMarkerKeyIndex;

      for (int i = 0; i <= this.topEntryKeyIndex; i += 2) {
         Object key = this.stack[i];
         if (key != FRAME_START_MARKER && !(key instanceof Atom)) {
            return false;
         }
      }

      for (int marker = this.topMarkerKeyIndex; marker != 0; marker = this.getPreviousMarkerIndex(marker)) {
         Object key = this.stack[marker];
         if (key != FRAME_START_MARKER) {
            return false;
         }
      }

      return true;
   }
}
