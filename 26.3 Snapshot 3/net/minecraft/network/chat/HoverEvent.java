package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public interface HoverEvent {
   Codec<HoverEvent> CODEC = HoverEvent.Action.CODEC.dispatch("action", HoverEvent::action, action -> action.codec);

   HoverEvent.Action action();

   enum Action implements StringRepresentable {
      SHOW_TEXT("show_text", true, HoverEvent.ShowText.CODEC),
      SHOW_ITEM("show_item", true, HoverEvent.ShowItem.CODEC),
      SHOW_ENTITY("show_entity", true, HoverEvent.ShowEntity.CODEC);

      public static final Codec<HoverEvent.Action> UNSAFE_CODEC = StringRepresentable.fromValues(HoverEvent.Action::values);
      public static final Codec<HoverEvent.Action> CODEC = UNSAFE_CODEC.validate(HoverEvent.Action::filterForSerialization);
      private final String name;
      private final boolean allowFromServer;
      private final MapCodec<? extends HoverEvent> codec;

      Action(final String name, final boolean allowFromServer, final MapCodec<? extends HoverEvent> codec) {
         this.name = name;
         this.allowFromServer = allowFromServer;
         this.codec = codec;
      }

      public boolean isAllowedFromServer() {
         return this.allowFromServer;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      @Override
      public String toString() {
         return "<action " + this.name + ">";
      }

      private static DataResult<HoverEvent.Action> filterForSerialization(final HoverEvent.Action action) {
         return !action.isAllowedFromServer() ? DataResult.error(() -> "Action not allowed: " + action) : DataResult.success(action, Lifecycle.stable());
      }
   }

   class EntityTooltipInfo {
      public static final MapCodec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id").forGetter(o -> o.type),
               UUIDUtil.LENIENT_CODEC.fieldOf("uuid").forGetter(o -> o.uuid),
               ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(o -> o.name)
            )
            .apply(i, HoverEvent.EntityTooltipInfo::new)
      );
      public final EntityType<?> type;
      public final UUID uuid;
      public final Optional<Component> name;
      private @Nullable List<Component> linesCache;

      public EntityTooltipInfo(final EntityType<?> type, final UUID uuid, final @Nullable Component name) {
         this(type, uuid, Optional.ofNullable(name));
      }

      public EntityTooltipInfo(final EntityType<?> type, final UUID uuid, final Optional<Component> name) {
         this.type = type;
         this.uuid = uuid;
         this.name = name;
      }

      public List<Component> getTooltipLines() {
         if (this.linesCache == null) {
            this.linesCache = new ArrayList<>();
            this.name.ifPresent(this.linesCache::add);
            this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
            this.linesCache.add(Component.literal(this.uuid.toString()));
         }

         return this.linesCache;
      }

      @Override
      public boolean equals(final Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            HoverEvent.EntityTooltipInfo that = (HoverEvent.EntityTooltipInfo)o;
            return this.type.equals(that.type) && this.uuid.equals(that.uuid) && this.name.equals(that.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.type.hashCode();
         result = 31 * result + this.uuid.hashCode();
         return 31 * result + this.name.hashCode();
      }
   }

   record ShowEntity(HoverEvent.EntityTooltipInfo entity) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowEntity> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(HoverEvent.EntityTooltipInfo.CODEC.forGetter(HoverEvent.ShowEntity::entity)).apply(i, HoverEvent.ShowEntity::new)
      );

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_ENTITY;
      }
   }

   record ShowItem(ItemStackTemplate item) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowItem> CODEC = ItemStackTemplate.MAP_CODEC.xmap(HoverEvent.ShowItem::new, HoverEvent.ShowItem::item);

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_ITEM;
      }
   }

   record ShowText(Component value) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowText> CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(HoverEvent.ShowText::value)).apply(i, HoverEvent.ShowText::new)
      );

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_TEXT;
      }
   }
}
