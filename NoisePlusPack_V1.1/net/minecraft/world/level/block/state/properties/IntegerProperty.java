package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class IntegerProperty extends Property<Integer> {
   private final IntImmutableList values;
   private final int min;
   private final int max;

   private IntegerProperty(String p_61623_, int p_61624_, int p_61625_) {
      super(p_61623_, Integer.class);
      if (p_61624_ < 0) {
         throw new IllegalArgumentException("Min value of " + p_61623_ + " must be 0 or greater");
      }

      if (p_61625_ <= p_61624_) {
         throw new IllegalArgumentException("Max value of " + p_61623_ + " must be greater than min (" + p_61624_ + ")");
      }

      this.min = p_61624_;
      this.max = p_61625_;
      this.values = IntImmutableList.toList(IntStream.range(p_61624_, p_61625_ + 1));
   }

   @Override
   public List<Integer> getPossibleValues() {
      return this.values;
   }

   @Override
   public boolean equals(Object p_61639_) {
      if (this == p_61639_) {
         return true;
      } else {
         return p_61639_ instanceof IntegerProperty integerproperty && super.equals(p_61639_) ? this.values.equals(integerproperty.values) : false;
      }
   }

   @Override
   public int generateHashCode() {
      return 31 * super.generateHashCode() + this.values.hashCode();
   }

   public static IntegerProperty create(String p_61632_, int p_61633_, int p_61634_) {
      return new IntegerProperty(p_61632_, p_61633_, p_61634_);
   }

   @Override
   public Optional<Integer> getValue(String p_61637_) {
      try {
         int i = Integer.parseInt(p_61637_);
         return i >= this.min && i <= this.max ? Optional.of(i) : Optional.empty();
      } catch (NumberFormatException numberformatexception) {
         return Optional.empty();
      }
   }

   public String getName(Integer p_61630_) {
      return p_61630_.toString();
   }

   public int getInternalIndex(Integer p_369529_) {
      return p_369529_ <= this.max ? p_369529_ - this.min : -1;
   }
}
