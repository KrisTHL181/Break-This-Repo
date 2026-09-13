package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap.Builder;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ArmorModelSet<T>(T head, T chest, T legs, T feet) {
   public T get(EquipmentSlot p_422804_) {
      return (T)(switch (p_422804_) {
         case HEAD -> this.head;
         case CHEST -> this.chest;
         case LEGS -> this.legs;
         case FEET -> this.feet;
         default -> throw new IllegalStateException("No model for slot: " + p_422804_);
      });
   }

   public <U> ArmorModelSet<U> map(Function<? super T, ? extends U> p_425698_) {
      return (ArmorModelSet<U>)(new ArmorModelSet<>(
         p_425698_.apply(this.head), p_425698_.apply(this.chest), p_425698_.apply(this.legs), p_425698_.apply(this.feet)
      ));
   }

   public void putFrom(ArmorModelSet<LayerDefinition> p_429096_, Builder<T, LayerDefinition> p_428434_) {
      p_428434_.put(this.head, p_429096_.head);
      p_428434_.put(this.chest, p_429096_.chest);
      p_428434_.put(this.legs, p_429096_.legs);
      p_428434_.put(this.feet, p_429096_.feet);
   }

   public static <M extends HumanoidModel<?>> ArmorModelSet<M> bake(
      ArmorModelSet<ModelLayerLocation> p_429972_, EntityModelSet p_428584_, Function<ModelPart, M> p_431581_
   ) {
      return p_429972_.map(p_430094_ -> p_431581_.apply(p_428584_.bakeLayer(p_430094_)));
   }
}
