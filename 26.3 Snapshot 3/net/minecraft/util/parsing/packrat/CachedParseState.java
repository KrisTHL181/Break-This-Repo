package net.minecraft.util.parsing.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public abstract class CachedParseState<S> implements ParseState<S> {
   private CachedParseState.@Nullable PositionCache[] positionCache = new CachedParseState.PositionCache[256];
   private final ErrorCollector<S> errorCollector;
   private final Scope scope = new Scope();
   private CachedParseState.@Nullable SimpleControl[] controlCache = new CachedParseState.SimpleControl[16];
   private int nextControlToReturn;
   private final CachedParseState<S>.Silent silent = new CachedParseState.Silent();

   protected CachedParseState(final ErrorCollector<S> errorCollector) {
      this.errorCollector = errorCollector;
   }

   @Override
   public Scope scope() {
      return this.scope;
   }

   @Override
   public ErrorCollector<S> errorCollector() {
      return this.errorCollector;
   }

   @Override
   public <T> @Nullable T parse(final NamedRule<S, T> rule) {
      int markBeforeParse = this.mark();
      CachedParseState.PositionCache positionCache = this.getCacheForPosition(markBeforeParse);
      int entryIndex = positionCache.findKeyIndex(rule.name());
      if (entryIndex != -1) {
         CachedParseState.CacheEntry<T> value = positionCache.getValue(entryIndex);
         if (value != null) {
            if (value == CachedParseState.CacheEntry.NEGATIVE) {
               return null;
            }

            this.restore(value.markAfterParse);
            return value.value;
         }
      } else {
         entryIndex = positionCache.allocateNewEntry(rule.name());
      }

      T result = rule.value().parse(this);
      CachedParseState.CacheEntry<T> entry;
      if (result == null) {
         entry = CachedParseState.CacheEntry.negativeEntry();
      } else {
         int markAfterParse = this.mark();
         entry = new CachedParseState.CacheEntry<>(result, markAfterParse);
      }

      positionCache.setValue(entryIndex, entry);
      return result;
   }

   private CachedParseState.PositionCache getCacheForPosition(final int index) {
      int currentSize = this.positionCache.length;
      if (index >= currentSize) {
         int newSize = Util.growByHalf(currentSize, index + 1);
         CachedParseState.PositionCache[] newCache = new CachedParseState.PositionCache[newSize];
         System.arraycopy(this.positionCache, 0, newCache, 0, currentSize);
         this.positionCache = newCache;
      }

      CachedParseState.PositionCache result = this.positionCache[index];
      if (result == null) {
         result = new CachedParseState.PositionCache();
         this.positionCache[index] = result;
      }

      return result;
   }

   @Override
   public Control acquireControl() {
      int currentSize = this.controlCache.length;
      if (this.nextControlToReturn >= currentSize) {
         int newSize = Util.growByHalf(currentSize, this.nextControlToReturn + 1);
         CachedParseState.SimpleControl[] newControlCache = new CachedParseState.SimpleControl[newSize];
         System.arraycopy(this.controlCache, 0, newControlCache, 0, currentSize);
         this.controlCache = newControlCache;
      }

      int controlIndex = this.nextControlToReturn++;
      CachedParseState.SimpleControl entry = this.controlCache[controlIndex];
      if (entry == null) {
         entry = new CachedParseState.SimpleControl();
         this.controlCache[controlIndex] = entry;
      } else {
         entry.reset();
      }

      return entry;
   }

   @Override
   public void releaseControl() {
      this.nextControlToReturn--;
   }

   @Override
   public ParseState<S> silent() {
      return this.silent;
   }

   private record CacheEntry<T>(@Nullable T value, int markAfterParse) {
      public static final CachedParseState.CacheEntry<?> NEGATIVE = new CachedParseState.CacheEntry(null, -1);

      public static <T> CachedParseState.CacheEntry<T> negativeEntry() {
         return (CachedParseState.CacheEntry<T>)NEGATIVE;
      }
   }

   private static class PositionCache {
      public static final int ENTRY_STRIDE = 2;
      private static final int NOT_FOUND = -1;
      private Object[] atomCache = new Object[16];
      private int nextKey;

      public int findKeyIndex(final Atom<?> key) {
         for (int i = 0; i < this.nextKey; i += 2) {
            if (this.atomCache[i] == key) {
               return i;
            }
         }

         return -1;
      }

      public int allocateNewEntry(final Atom<?> key) {
         int newKeyIndex = this.nextKey;
         this.nextKey += 2;
         int newValueIndex = newKeyIndex + 1;
         int currentSize = this.atomCache.length;
         if (newValueIndex >= currentSize) {
            int newSize = Util.growByHalf(currentSize, newValueIndex + 1);
            Object[] newCache = new Object[newSize];
            System.arraycopy(this.atomCache, 0, newCache, 0, currentSize);
            this.atomCache = newCache;
         }

         this.atomCache[newKeyIndex] = key;
         return newKeyIndex;
      }

      public <T> CachedParseState.@Nullable CacheEntry<T> getValue(final int keyIndex) {
         return (CachedParseState.CacheEntry<T>)this.atomCache[keyIndex + 1];
      }

      public void setValue(final int keyIndex, final CachedParseState.CacheEntry<?> entry) {
         this.atomCache[keyIndex + 1] = entry;
      }
   }

   private class Silent implements ParseState<S> {
      private final ErrorCollector<S> silentCollector = new ErrorCollector.Nop<>();

      @Override
      public ErrorCollector<S> errorCollector() {
         return this.silentCollector;
      }

      @Override
      public Scope scope() {
         return CachedParseState.this.scope();
      }

      @Override
      public <T> @Nullable T parse(final NamedRule<S, T> rule) {
         return CachedParseState.this.parse(rule);
      }

      @Override
      public S input() {
         return CachedParseState.this.input();
      }

      @Override
      public int mark() {
         return CachedParseState.this.mark();
      }

      @Override
      public void restore(final int mark) {
         CachedParseState.this.restore(mark);
      }

      @Override
      public Control acquireControl() {
         return CachedParseState.this.acquireControl();
      }

      @Override
      public void releaseControl() {
         CachedParseState.this.releaseControl();
      }

      @Override
      public ParseState<S> silent() {
         return this;
      }
   }

   private static class SimpleControl implements Control {
      private boolean hasCut;

      @Override
      public void cut() {
         this.hasCut = true;
      }

      @Override
      public boolean hasCut() {
         return this.hasCut;
      }

      public void reset() {
         this.hasCut = false;
      }
   }
}
