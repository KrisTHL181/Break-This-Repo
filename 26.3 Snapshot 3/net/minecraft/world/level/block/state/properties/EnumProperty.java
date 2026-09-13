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

   private EnumProperty(final String name, final Class<T> clazz, final List<T> values) {
      super(name, clazz);
      if (values.isEmpty()) {
         throw new IllegalArgumentException("Trying to make empty EnumProperty '" + name + "'");
      }

      this.values = List.copyOf(values);
      T[] allEnumValues = clazz.getEnumConstants();
      this.ordinalToIndex = new int[allEnumValues.length];

      for (T value : allEnumValues) {
         this.ordinalToIndex[value.ordinal()] = values.indexOf(value);
      }

      Builder<String, T> names = ImmutableMap.builder();

      for (T value : values) {
         String key = value.getSerializedName();
         names.put(key, value);
      }

      this.names = names.buildOrThrow();
   }

   @Override
   public List<T> getPossibleValues() {
      return this.values;
   }

   @Override
   public Optional<T> getValue(final String name) {
      return Optional.ofNullable(this.names.get(name));
   }

   public String getName(final T value) {
      return value.getSerializedName();
   }

   public int getInternalIndex(final T value) {
      return this.ordinalToIndex[value.ordinal()];
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof EnumProperty<?> that && super.equals(o) ? this.values.equals(that.values) : false;
      }
   }

   @Override
   public int generateHashCode() {
      int result = super.generateHashCode();
      return 31 * result + this.values.hashCode();
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(final String name, final Class<T> clazz) {
      return create(name, clazz, t -> true);
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(final String name, final Class<T> clazz, final Predicate<T> filter) {
      return create(name, clazz, Arrays.<T>stream(clazz.getEnumConstants()).filter(filter).collect(Collectors.toList()));
   }

   @SafeVarargs
   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(final String name, final Class<T> clazz, final T... values) {
      return create(name, clazz, List.of(values));
   }

   public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(final String name, final Class<T> clazz, final List<T> values) {
      return new EnumProperty<>(name, clazz, values);
   }
}
