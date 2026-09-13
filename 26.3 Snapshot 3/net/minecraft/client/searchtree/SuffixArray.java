package net.minecraft.client.searchtree;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

public class SuffixArray<T> {
   private static final boolean DEBUG_COMPARISONS = Boolean.parseBoolean(System.getProperty("SuffixArray.printComparisons", "false"));
   private static final boolean DEBUG_ARRAY = Boolean.parseBoolean(System.getProperty("SuffixArray.printArray", "false"));
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int END_OF_TEXT_MARKER = -1;
   private static final int END_OF_DATA = -2;
   protected final List<T> list = Lists.newArrayList();
   private final IntList chars = new IntArrayList();
   private final IntList wordStarts = new IntArrayList();
   private IntList suffixToT = new IntArrayList();
   private IntList offsets = new IntArrayList();
   private int maxStringLength;

   public void add(final T t, final String text) {
      this.maxStringLength = Math.max(this.maxStringLength, text.length());
      int index = this.list.size();
      this.list.add(t);
      this.wordStarts.add(this.chars.size());

      for (int i = 0; i < text.length(); i++) {
         this.suffixToT.add(index);
         this.offsets.add(i);
         this.chars.add(text.charAt(i));
      }

      this.suffixToT.add(index);
      this.offsets.add(text.length());
      this.chars.add(-1);
   }

   public void generate() {
      int charCount = this.chars.size();
      int[] positions = new int[charCount];
      int[] lefts = new int[charCount];
      int[] rights = new int[charCount];
      int[] reverse = new int[charCount];
      IntComparator comparator = (a, b) -> lefts[a] == lefts[b] ? Integer.compare(rights[a], rights[b]) : Integer.compare(lefts[a], lefts[b]);
      Swapper swapper = (a, b) -> {
         if (a != b) {
            int tmp = lefts[a];
            lefts[a] = lefts[b];
            lefts[b] = tmp;
            tmp = rights[a];
            rights[a] = rights[b];
            rights[b] = tmp;
            tmp = reverse[a];
            reverse[a] = reverse[b];
            reverse[b] = tmp;
         }
      };

      for (int i = 0; i < charCount; i++) {
         positions[i] = this.chars.getInt(i);
      }

      int count = 1;

      for (int max = Math.min(charCount, this.maxStringLength); count * 2 < max; count *= 2) {
         for (int i = 0; i < charCount; reverse[i] = i++) {
            lefts[i] = positions[i];
            rights[i] = i + count < charCount ? positions[i + count] : -2;
         }

         Arrays.quickSort(0, charCount, comparator, swapper);

         for (int i = 0; i < charCount; i++) {
            if (i > 0 && lefts[i] == lefts[i - 1] && rights[i] == rights[i - 1]) {
               positions[reverse[i]] = positions[reverse[i - 1]];
            } else {
               positions[reverse[i]] = i;
            }
         }
      }

      IntList oldSuffixToT = this.suffixToT;
      IntList oldOffsets = this.offsets;
      this.suffixToT = new IntArrayList(oldSuffixToT.size());
      this.offsets = new IntArrayList(oldOffsets.size());

      for (int i = 0; i < charCount; i++) {
         int index = reverse[i];
         this.suffixToT.add(oldSuffixToT.getInt(index));
         this.offsets.add(oldOffsets.getInt(index));
      }

      if (DEBUG_ARRAY) {
         this.print();
      }
   }

   private void print() {
      for (int i = 0; i < this.suffixToT.size(); i++) {
         LOGGER.debug("{} {}", i, this.getString(i));
      }

      LOGGER.debug("");
   }

   private String getString(final int i) {
      int start = this.offsets.getInt(i);
      int offset = this.wordStarts.getInt(this.suffixToT.getInt(i));
      StringBuilder builder = new StringBuilder();

      for (int j = 0; offset + j < this.chars.size(); j++) {
         if (j == start) {
            builder.append('^');
         }

         int p = this.chars.getInt(offset + j);
         if (p == -1) {
            break;
         }

         builder.append((char)p);
      }

      return builder.toString();
   }

   private int compare(final String text, final int index) {
      int start = this.wordStarts.getInt(this.suffixToT.getInt(index));
      int offset = this.offsets.getInt(index);

      for (int i = 0; i < text.length(); i++) {
         int p = this.chars.getInt(start + offset + i);
         if (p == -1) {
            return 1;
         }

         char c = text.charAt(i);
         char c2 = (char)p;
         if (c < c2) {
            return -1;
         }

         if (c > c2) {
            return 1;
         }
      }

      return 0;
   }

   public List<T> search(final String text) {
      int suffixCount = this.suffixToT.size();
      int low = 0;
      int high = suffixCount;

      while (low < high) {
         int mid = low + (high - low) / 2;
         int c = this.compare(text, mid);
         if (DEBUG_COMPARISONS) {
            LOGGER.debug("comparing lower \"{}\" with {} \"{}\": {}", new Object[]{text, mid, this.getString(mid), c});
         }

         if (c > 0) {
            low = mid + 1;
         } else {
            high = mid;
         }
      }

      if (low >= 0 && low < suffixCount) {
         int lowerBound = low;
         high = suffixCount;

         while (low < high) {
            int mid = low + (high - low) / 2;
            int c = this.compare(text, mid);
            if (DEBUG_COMPARISONS) {
               LOGGER.debug("comparing upper \"{}\" with {} \"{}\": {}", new Object[]{text, mid, this.getString(mid), c});
            }

            if (c >= 0) {
               low = mid + 1;
            } else {
               high = mid;
            }
         }

         int upperBound = low;
         IntSet matches = new IntOpenHashSet();

         for (int i = lowerBound; i < upperBound; i++) {
            matches.add(this.suffixToT.getInt(i));
         }

         int[] ints = matches.toIntArray();
         java.util.Arrays.sort(ints);
         Set<T> result = Sets.newLinkedHashSet();

         for (int t : ints) {
            result.add(this.list.get(t));
         }

         return Lists.newArrayList(result);
      } else {
         return Collections.emptyList();
      }
   }
}
