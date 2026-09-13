package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.Property;

public record PropertyValueList(List<Property.Value<?>> values) {
   public static final PropertyValueList EMPTY = new PropertyValueList(List.of());
   private static final Comparator<Property.Value<?>> COMPARE_BY_NAME = Comparator.comparing(p -> p.property().getName());

   public PropertyValueList extend(final Property.Value<?> element) {
      return new PropertyValueList(Util.copyAndAdd(this.values, element));
   }

   public PropertyValueList extend(final PropertyValueList other) {
      return new PropertyValueList(ImmutableList.builder().addAll(this.values).addAll(other.values).build());
   }

   public static PropertyValueList of(final Property.Value<?>... values) {
      return new PropertyValueList(List.of(values));
   }

   public String getKey() {
      return this.values.stream().sorted(COMPARE_BY_NAME).map(Property.Value::toString).collect(Collectors.joining(","));
   }

   @Override
   public String toString() {
      return this.getKey();
   }
}
