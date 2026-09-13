package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;

public record ArmorModelSet<T>(T head, T chest, T legs, T feet) {
   public T get(final EquipmentSlot slot) {
      return (T)(switch (slot) {
         case HEAD -> this.head;
         case CHEST -> this.chest;
         case LEGS -> this.legs;
         case FEET -> this.feet;
         default -> throw new IllegalStateException("No model for slot: " + slot);
      });
   }

   public <U> ArmorModelSet<U> map(final Function<? super T, ? extends U> mapper) {
      return (ArmorModelSet<U>)(new ArmorModelSet<>(mapper.apply(this.head), mapper.apply(this.chest), mapper.apply(this.legs), mapper.apply(this.feet)));
   }

   public void putFrom(final ArmorModelSet<LayerDefinition> values, final Builder<T, LayerDefinition> output) {
      output.put(this.head, values.head);
      output.put(this.chest, values.chest);
      output.put(this.legs, values.legs);
      output.put(this.feet, values.feet);
   }

   public static <M extends HumanoidModel<?>> ArmorModelSet<M> bake(
      final ArmorModelSet<ModelLayerLocation> locations, final EntityModelSet modelSet, final Function<ModelPart, M> factory
   ) {
      return locations.map(id -> factory.apply(modelSet.bakeLayer(id)));
   }
}
