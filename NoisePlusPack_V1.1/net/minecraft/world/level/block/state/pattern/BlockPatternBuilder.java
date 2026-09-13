package net.minecraft.world.level.block.state.pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class BlockPatternBuilder {
   private final List<String[]> pattern = Lists.newArrayList();
   private final Map<Character, Predicate<@Nullable BlockInWorld>> lookup = Maps.newHashMap();
   private int height;
   private int width;
   private final CharSet unknownCharacters = new CharOpenHashSet();

   private BlockPatternBuilder() {
      this.lookup.put(' ', p_187549_ -> true);
   }

   public BlockPatternBuilder aisle(String... p_61248_) {
      if (!ArrayUtils.isEmpty(p_61248_) && !StringUtils.isEmpty(p_61248_[0])) {
         if (this.pattern.isEmpty()) {
            this.height = p_61248_.length;
            this.width = p_61248_[0].length();
         }

         if (p_61248_.length != this.height) {
            throw new IllegalArgumentException("Expected aisle with height of " + this.height + ", but was given one with a height of " + p_61248_.length + ")");
         }

         for (String s : p_61248_) {
            if (s.length() != this.width) {
               throw new IllegalArgumentException(
                  "Not all rows in the given aisle are the correct width (expected " + this.width + ", found one with " + s.length() + ")"
               );
            }

            for (char c0 : s.toCharArray()) {
               if (!this.lookup.containsKey(c0)) {
                  this.unknownCharacters.add(c0);
               }
            }
         }

         this.pattern.add(p_61248_);
         return this;
      } else {
         throw new IllegalArgumentException("Empty pattern for aisle");
      }
   }

   public static BlockPatternBuilder start() {
      return new BlockPatternBuilder();
   }

   public BlockPatternBuilder where(char p_61245_, Predicate<@Nullable BlockInWorld> p_61246_) {
      this.lookup.put(p_61245_, p_61246_);
      this.unknownCharacters.remove(p_61245_);
      return this;
   }

   public BlockPattern build() {
      return new BlockPattern(this.createPattern());
   }

   private Predicate<BlockInWorld>[][][] createPattern() {
      if (!this.unknownCharacters.isEmpty()) {
         throw new IllegalStateException("Predicates for character(s) " + this.unknownCharacters + " are missing");
      }

      Predicate<BlockInWorld>[][][] predicate = (Predicate<BlockInWorld>[][][])Array.newInstance(Predicate.class, this.pattern.size(), this.height, this.width);

      for (int i = 0; i < this.pattern.size(); i++) {
         for (int j = 0; j < this.height; j++) {
            for (int k = 0; k < this.width; k++) {
               predicate[i][j][k] = this.lookup.get(this.pattern.get(i)[j].charAt(k));
            }
         }
      }

      return predicate;
   }
}
