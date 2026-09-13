package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityEquipmentPredicate(
   Optional<ItemPredicate> head,
   Optional<ItemPredicate> chest,
   Optional<ItemPredicate> legs,
   Optional<ItemPredicate> feet,
   Optional<ItemPredicate> body,
   Optional<ItemPredicate> mainhand,
   Optional<ItemPredicate> offhand
) implements EntitySubPredicate {
   public static final Codec<EntityEquipmentPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ItemPredicate.CODEC.optionalFieldOf("head").forGetter(EntityEquipmentPredicate::head),
            ItemPredicate.CODEC.optionalFieldOf("chest").forGetter(EntityEquipmentPredicate::chest),
            ItemPredicate.CODEC.optionalFieldOf("legs").forGetter(EntityEquipmentPredicate::legs),
            ItemPredicate.CODEC.optionalFieldOf("feet").forGetter(EntityEquipmentPredicate::feet),
            ItemPredicate.CODEC.optionalFieldOf("body").forGetter(EntityEquipmentPredicate::body),
            ItemPredicate.CODEC.optionalFieldOf("mainhand").forGetter(EntityEquipmentPredicate::mainhand),
            ItemPredicate.CODEC.optionalFieldOf("offhand").forGetter(EntityEquipmentPredicate::offhand)
         )
         .apply(i, EntityEquipmentPredicate::new)
   );

   public static EntityEquipmentPredicate captainPredicate(final HolderGetter<Item> items, final HolderGetter<BannerPattern> patternGetter) {
      return EntityEquipmentPredicate.Builder.equipment()
         .head(
            ItemPredicate.Builder.item()
               .of(items, Items.BANNER.white())
               .withComponents(
                  DataComponentMatchers.Builder.components()
                     .exact(
                        DataComponentExactPredicate.someOf(
                           Raid.getBannerComponentPatch(patternGetter).split().added(), DataComponents.BANNER_PATTERNS, DataComponents.ITEM_NAME
                        )
                     )
                     .build()
               )
         )
         .build();
   }

   public boolean matches(final @Nullable Entity entity) {
      if (entity instanceof LivingEntity livingEntity) {
         if (this.head.isPresent() && !this.head.get().test(livingEntity.getItemBySlot(EquipmentSlot.HEAD))) {
            return false;
         } else if (this.chest.isPresent() && !this.chest.get().test(livingEntity.getItemBySlot(EquipmentSlot.CHEST))) {
            return false;
         } else if (this.legs.isPresent() && !this.legs.get().test(livingEntity.getItemBySlot(EquipmentSlot.LEGS))) {
            return false;
         } else if (this.feet.isPresent() && !this.feet.get().test(livingEntity.getItemBySlot(EquipmentSlot.FEET))) {
            return false;
         } else if (this.body.isPresent() && !this.body.get().test(livingEntity.getItemBySlot(EquipmentSlot.BODY))) {
            return false;
         } else {
            return this.mainhand.isPresent() && !this.mainhand.get().test(livingEntity.getItemBySlot(EquipmentSlot.MAINHAND))
               ? false
               : !this.offhand.isPresent() || this.offhand.get().test(livingEntity.getItemBySlot(EquipmentSlot.OFFHAND));
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.matches(entity);
   }

   public static class Builder {
      private Optional<ItemPredicate> head = Optional.empty();
      private Optional<ItemPredicate> chest = Optional.empty();
      private Optional<ItemPredicate> legs = Optional.empty();
      private Optional<ItemPredicate> feet = Optional.empty();
      private Optional<ItemPredicate> body = Optional.empty();
      private Optional<ItemPredicate> mainhand = Optional.empty();
      private Optional<ItemPredicate> offhand = Optional.empty();

      public static EntityEquipmentPredicate.Builder equipment() {
         return new EntityEquipmentPredicate.Builder();
      }

      public EntityEquipmentPredicate.Builder head(final ItemPredicate.Builder head) {
         this.head = Optional.of(head.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder chest(final ItemPredicate.Builder chest) {
         this.chest = Optional.of(chest.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder legs(final ItemPredicate.Builder legs) {
         this.legs = Optional.of(legs.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder feet(final ItemPredicate.Builder feet) {
         this.feet = Optional.of(feet.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder body(final ItemPredicate.Builder body) {
         this.body = Optional.of(body.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder mainhand(final ItemPredicate.Builder mainhand) {
         this.mainhand = Optional.of(mainhand.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder offhand(final ItemPredicate.Builder offhand) {
         this.offhand = Optional.of(offhand.build());
         return this;
      }

      public EntityEquipmentPredicate build() {
         return new EntityEquipmentPredicate(this.head, this.chest, this.legs, this.feet, this.body, this.mainhand, this.offhand);
      }
   }
}
