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
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface HoverEvent {
   Codec<HoverEvent> CODEC = HoverEvent.Action.CODEC.dispatch("action", HoverEvent::action, p_389915_ -> p_389915_.codec);

   HoverEvent.Action action();

   enum Action implements StringRepresentable {
      SHOW_TEXT("show_text", true, HoverEvent.ShowText.CODEC),
      SHOW_ITEM("show_item", true, HoverEvent.ShowItem.CODEC),
      SHOW_ENTITY("show_entity", true, HoverEvent.ShowEntity.CODEC);

      public static final Codec<HoverEvent.Action> UNSAFE_CODEC = StringRepresentable.fromValues(HoverEvent.Action::values);
      public static final Codec<HoverEvent.Action> CODEC = UNSAFE_CODEC.validate(HoverEvent.Action::filterForSerialization);
      private final String name;
      private final boolean allowFromServer;
      final MapCodec<? extends HoverEvent> codec;

      Action(final String p_396071_, final boolean p_130843_, final MapCodec<? extends HoverEvent> p_396381_) {
         this.name = p_396071_;
         this.allowFromServer = p_130843_;
         this.codec = p_396381_;
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

      private static DataResult<HoverEvent.Action> filterForSerialization(HoverEvent.Action p_311888_) {
         return !p_311888_.isAllowedFromServer()
            ? DataResult.error(() -> "Action not allowed: " + p_311888_)
            : DataResult.success(p_311888_, Lifecycle.stable());
      }
   }

   class EntityTooltipInfo {
      public static final MapCodec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.mapCodec(
         p_389916_ -> p_389916_.group(
               BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id").forGetter(p_309982_ -> p_309982_.type),
               UUIDUtil.LENIENT_CODEC.fieldOf("uuid").forGetter(p_389917_ -> p_389917_.uuid),
               ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(p_310270_ -> p_310270_.name)
            )
            .apply(p_389916_, HoverEvent.EntityTooltipInfo::new)
      );
      public final EntityType<?> type;
      public final UUID uuid;
      public final Optional<Component> name;
      private @Nullable List<Component> linesCache;

      public EntityTooltipInfo(EntityType<?> p_130876_, UUID p_130877_, @Nullable Component p_130878_) {
         this(p_130876_, p_130877_, Optional.ofNullable(p_130878_));
      }

      public EntityTooltipInfo(EntityType<?> p_312321_, UUID p_312750_, Optional<Component> p_312078_) {
         this.type = p_312321_;
         this.uuid = p_312750_;
         this.name = p_312078_;
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
      public boolean equals(Object p_130886_) {
         if (this == p_130886_) {
            return true;
         } else if (p_130886_ != null && this.getClass() == p_130886_.getClass()) {
            HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = (HoverEvent.EntityTooltipInfo)p_130886_;
            return this.type.equals(hoverevent$entitytooltipinfo.type)
               && this.uuid.equals(hoverevent$entitytooltipinfo.uuid)
               && this.name.equals(hoverevent$entitytooltipinfo.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int i = this.type.hashCode();
         i = 31 * i + this.uuid.hashCode();
         return 31 * i + this.name.hashCode();
      }
   }

   record ShowEntity(HoverEvent.EntityTooltipInfo entity) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowEntity> CODEC = RecordCodecBuilder.mapCodec(
         p_395635_ -> p_395635_.group(HoverEvent.EntityTooltipInfo.CODEC.forGetter(HoverEvent.ShowEntity::entity)).apply(p_395635_, HoverEvent.ShowEntity::new)
      );

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_ENTITY;
      }
   }

   record ShowItem(ItemStack item) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowItem> CODEC = ItemStack.MAP_CODEC.xmap(HoverEvent.ShowItem::new, HoverEvent.ShowItem::item);

      public ShowItem {
         item = item.copy();
      }

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_ITEM;
      }

      @Override
      public boolean equals(Object p_391825_) {
         return p_391825_ instanceof HoverEvent.ShowItem hoverevent$showitem && ItemStack.matches(this.item, hoverevent$showitem.item);
      }

      @Override
      public int hashCode() {
         return ItemStack.hashItemAndComponents(this.item);
      }
   }

   record ShowText(Component value) implements HoverEvent {
      public static final MapCodec<HoverEvent.ShowText> CODEC = RecordCodecBuilder.mapCodec(
         p_392348_ -> p_392348_.group(ComponentSerialization.CODEC.fieldOf("value").forGetter(HoverEvent.ShowText::value))
            .apply(p_392348_, HoverEvent.ShowText::new)
      );

      @Override
      public HoverEvent.Action action() {
         return HoverEvent.Action.SHOW_TEXT;
      }
   }
}
