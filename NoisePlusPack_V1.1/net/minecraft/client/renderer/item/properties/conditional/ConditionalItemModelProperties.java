package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModelProperties {
   private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();
   public static final MapCodec<ConditionalItemModelProperty> MAP_CODEC = ID_MAPPER.codec(Identifier.CODEC)
      .dispatchMap("property", ConditionalItemModelProperty::type, p_376487_ -> p_376487_);

   public static void bootstrap() {
      ID_MAPPER.put(Identifier.withDefaultNamespace("custom_model_data"), CustomModelDataProperty.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("using_item"), IsUsingItem.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("broken"), Broken.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("damaged"), Damaged.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("fishing_rod/cast"), FishingRodCast.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("has_component"), HasComponent.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("bundle/has_selected_item"), BundleHasSelectedItem.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("selected"), IsSelected.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("carried"), IsCarried.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("extended_view"), ExtendedView.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("keybind_down"), IsKeybindDown.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("view_entity"), IsViewEntity.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("component"), ComponentMatches.MAP_CODEC);
   }
}
