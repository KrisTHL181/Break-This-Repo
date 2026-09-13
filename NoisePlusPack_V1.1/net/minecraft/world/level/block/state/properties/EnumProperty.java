package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;

public final class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
   private final List<T> values;
   private final Map<String, T> names;
   private final int[] ordinalToIndex;

   private EnumProperty(String p_61579_, Class<T> p_61580_, List<T> p_368786_) {
      super(p_61579_, p_61580_);
      if (p_368786_.isEmpty()) {
         throw new IllegalArgumentException("Trying to make empty EnumProperty '" + p_61579_ + "'");
      }

      this.values = List.copyOf(p_368786_);
      T[] at = p_61580_.getEnumConstants();
      this.ordinalToIndex = new int[at.length];

      for (T t : at) {
         this.ordinalToIndex[t.ordinal()] = p_368786_.indexOf(t);
      }

      Builder<String, T> builder = ImmutableMap.builder();

      for (T t1 : p_368786_) {
         String s = t1.getSerializedName();
         builder.put(s, t1);
      }

      this.names = builder.buildOrThrow();
   }

   @Override
   public List<T> getPossibleValues() {
      return this.values;
   }

   @Override
   public Optional<T> getValue(String p_61604_) {
      return Optional.ofNullable(this.names.get(p_61604_));
   }

   public String getName(T p_61586_) {
      return p_61586_.getSerializedName();
   }

   public int getInternalIndex(T p_363721_) {
      return this.ordinalToIndex[p_363721_.ordinal()];
   }

   @Override
   public boolean equals(Object p_61606_) {
      if (this == p_61606_) {
         return true;
      } else {
         return p_61606_ instanceof EnumProperty<?> enumproperty && super.equals(p_61606_) ? this.values.equals(enumproperty.values) : false;
      }
   }

   @Override
   public int generateHashCode() {
      int i = super.generateHashCode();
      return 31 * i + this.values.hashCode();
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String p_61588_, Class<T> p_61589_) {
      return create(p_61588_, p_61589_, p_187560_ -> true);
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String p_61595_, Class<T> p_61596_, Predicate<T> p_61597_) {
      return create(p_61595_, p_61596_, Arrays.<T>stream(p_61596_.getEnumConstants()).filter(p_61597_).collect(Collectors.toList()));
   }

   @SafeVarargs
   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String p_61599_, Class<T> p_61600_, T... p_61601_) {
      return create(p_61599_, p_61600_, List.of(p_61601_));
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String p_61591_, Class<T> p_61592_, List<T> p_367534_) {
      return new EnumProperty<>(p_61591_, p_61592_, p_367534_);
   }
}
