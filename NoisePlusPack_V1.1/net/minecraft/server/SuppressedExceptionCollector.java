package net.minecraft.server;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Queue;
import net.minecraft.util.ArrayListDeque;

public class SuppressedExceptionCollector {
   private static final int LATEST_ENTRY_COUNT = 8;
   private final Queue<SuppressedExceptionCollector.LongEntry> latestEntries = new ArrayListDeque<>();
   private final Object2IntLinkedOpenHashMap<SuppressedExceptionCollector.ShortEntry> entryCounts = new Object2IntLinkedOpenHashMap();

   private static long currentTimeMs() {
      return System.currentTimeMillis();
   }

   public synchronized void addEntry(String p_368067_, Throwable p_370087_) {
      long i = currentTimeMs();
      String s = p_370087_.getMessage();
      this.latestEntries.add(new SuppressedExceptionCollector.LongEntry(i, p_368067_, (Class<? extends Throwable>)p_370087_.getClass(), s));

      while (this.latestEntries.size() > 8) {
         this.latestEntries.remove();
      }

      SuppressedExceptionCollector.ShortEntry suppressedexceptioncollector$shortentry = new SuppressedExceptionCollector.ShortEntry(
         p_368067_, (Class<? extends Throwable>)p_370087_.getClass()
      );
      int j = this.entryCounts.getInt(suppressedexceptioncollector$shortentry);
      this.entryCounts.putAndMoveToFirst(suppressedexceptioncollector$shortentry, j + 1);
   }

   public synchronized String dump() {
      long i = currentTimeMs();
      StringBuilder stringbuilder = new StringBuilder();
      if (!this.latestEntries.isEmpty()) {
         stringbuilder.append("\n\t\tLatest entries:\n");

         for (SuppressedExceptionCollector.LongEntry suppressedexceptioncollector$longentry : this.latestEntries) {
            stringbuilder.append("\t\t\t")
               .append(suppressedexceptioncollector$longentry.location)
               .append(":")
               .append(suppressedexceptioncollector$longentry.cls)
               .append(": ")
               .append(suppressedexceptioncollector$longentry.message)
               .append(" (")
               .append(i - suppressedexceptioncollector$longentry.timestampMs)
               .append("ms ago)")
               .append("\n");
         }
      }

      if (!this.entryCounts.isEmpty()) {
         if (stringbuilder.isEmpty()) {
            stringbuilder.append("\n");
         }

         stringbuilder.append("\t\tEntry counts:\n");
         ObjectIterator var6 = Object2IntMaps.fastIterable(this.entryCounts).iterator();

         while (var6.hasNext()) {
            Entry<SuppressedExceptionCollector.ShortEntry> entry = (Entry<SuppressedExceptionCollector.ShortEntry>)var6.next();
            stringbuilder.append("\t\t\t")
               .append(((SuppressedExceptionCollector.ShortEntry)entry.getKey()).location)
               .append(":")
               .append(((SuppressedExceptionCollector.ShortEntry)entry.getKey()).cls)
               .append(" x ")
               .append(entry.getIntValue())
               .append("\n");
         }
      }

      return stringbuilder.isEmpty() ? "~~NONE~~" : stringbuilder.toString();
   }

   record LongEntry(long timestampMs, String location, Class<? extends Throwable> cls, String message) {
   }

   record ShortEntry(String location, Class<? extends Throwable> cls) {
   }
}
