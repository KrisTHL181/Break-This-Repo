package net.minecraft.world.waypoints;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.scores.TeamColor;

public interface Waypoint {
   AttributeModifier WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER = new AttributeModifier(
      Identifier.withDefaultNamespace("waypoint_transmit_range_hide"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
   );

   static Item.Properties addHideAttribute(final Item.Properties properties) {
      return properties.component(
         DataComponents.ATTRIBUTE_MODIFIERS,
         ItemAttributeModifiers.builder()
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE, WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER, EquipmentSlotGroup.HEAD, ItemAttributeModifiers.Display.hidden())
            .build()
      );
   }

   class Icon {
      public static final Codec<Waypoint.Icon> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               ResourceKey.codec(WaypointStyleAssets.ROOT_ID).fieldOf("style").forGetter(icon -> icon.style),
               ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color").forGetter(icon -> icon.color)
            )
            .apply(i, Waypoint.Icon::new)
      );
      public static final StreamCodec<ByteBuf, Waypoint.Icon> STREAM_CODEC = StreamCodec.composite(
         ResourceKey.streamCodec(WaypointStyleAssets.ROOT_ID),
         icon -> icon.style,
         ByteBufCodecs.optional(ByteBufCodecs.RGB_COLOR),
         icon -> icon.color,
         Waypoint.Icon::new
      );
      public static final Waypoint.Icon NULL = new Waypoint.Icon();
      public ResourceKey<WaypointStyleAsset> style = WaypointStyleAssets.DEFAULT;
      public Optional<Integer> color = Optional.empty();

      public Icon() {
      }

      private Icon(final ResourceKey<WaypointStyleAsset> style, final Optional<Integer> color) {
         this.style = style;
         this.color = color;
      }

      public boolean hasData() {
         return this.style != WaypointStyleAssets.DEFAULT || this.color.isPresent();
      }

      public Waypoint.Icon cloneAndAssignStyle(final LivingEntity livingEntity) {
         ResourceKey<WaypointStyleAsset> overrideStyle = this.getOverrideStyle();
         Optional<Integer> colorOverride = this.color
            .or(
               () -> Optional.ofNullable(livingEntity.getTeam())
                  .flatMap(t -> t.getColor())
                  .map(teamColor -> teamColor == TeamColor.BLACK ? -13619152 : teamColor.rgb())
            );
         return overrideStyle == this.style && colorOverride.isEmpty() ? this : new Waypoint.Icon(overrideStyle, colorOverride);
      }

      public void copyFrom(final Waypoint.Icon other) {
         this.color = other.color;
         this.style = other.style;
      }

      private ResourceKey<WaypointStyleAsset> getOverrideStyle() {
         return this.style != WaypointStyleAssets.DEFAULT ? this.style : WaypointStyleAssets.DEFAULT;
      }
   }
}
