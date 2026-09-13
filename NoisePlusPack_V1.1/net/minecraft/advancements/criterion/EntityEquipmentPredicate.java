package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jspecify.annotations.Nullable;

public record EntityEquipmentPredicate(
   Optional<ItemPredicate> head,
   Optional<ItemPredicate> chest,
   Optional<ItemPredicate> legs,
   Optional<ItemPredicate> feet,
   Optional<ItemPredicate> body,
   Optional<ItemPredicate> mainhand,
   Optional<ItemPredicate> offhand
) {
   public static final Codec<EntityEquipmentPredicate> CODEC = RecordCodecBuilder.create(
      p_451677_ -> p_451677_.group(
            ItemPredicate.CODEC.optionalFieldOf("head").forGetter(EntityEquipmentPredicate::head),
            ItemPredicate.CODEC.optionalFieldOf("chest").forGetter(EntityEquipmentPredicate::chest),
            ItemPredicate.CODEC.optionalFieldOf("legs").forGetter(EntityEquipmentPredicate::legs),
            ItemPredicate.CODEC.optionalFieldOf("feet").forGetter(EntityEquipmentPredicate::feet),
            ItemPredicate.CODEC.optionalFieldOf("body").forGetter(EntityEquipmentPredicate::body),
            ItemPredicate.CODEC.optionalFieldOf("mainhand").forGetter(EntityEquipmentPredicate::mainhand),
            ItemPredicate.CODEC.optionalFieldOf("offhand").forGetter(EntityEquipmentPredicate::offhand)
         )
         .apply(p_451677_, EntityEquipmentPredicate::new)
   );

   public static EntityEquipmentPredicate captainPredicate(HolderGetter<Item> p_460385_, HolderGetter<BannerPattern> p_451231_) {
      return EntityEquipmentPredicate.Builder.equipment()
         .head(
            ItemPredicate.Builder.item()
               .of(p_460385_, Items.WHITE_BANNER)
               .withComponents(
                  DataComponentMatchers.Builder.components()
                     .exact(
                        DataComponentExactPredicate.someOf(
                           Raid.getOminousBannerInstance(p_451231_).getComponents(), DataComponents.BANNER_PATTERNS, DataComponents.ITEM_NAME
                        )
                     )
                     .build()
               )
         )
         .build();
   }

   public boolean matches(@Nullable Entity p_454897_) {
      if (p_454897_ instanceof LivingEntity livingentity) {
         if (this.head.isPresent() && !this.head.get().test(livingentity.getItemBySlot(EquipmentSlot.HEAD))) {
            return false;
         } else if (this.chest.isPresent() && !this.chest.get().test(livingentity.getItemBySlot(EquipmentSlot.CHEST))) {
            return false;
         } else if (this.legs.isPresent() && !this.legs.get().test(livingentity.getItemBySlot(EquipmentSlot.LEGS))) {
            return false;
         } else if (this.feet.isPresent() && !this.feet.get().test(livingentity.getItemBySlot(EquipmentSlot.FEET))) {
            return false;
         } else if (this.body.isPresent() && !this.body.get().test(livingentity.getItemBySlot(EquipmentSlot.BODY))) {
            return false;
         } else {
            return this.mainhand.isPresent() && !this.mainhand.get().test(livingentity.getItemBySlot(EquipmentSlot.MAINHAND))
               ? false
               : !this.offhand.isPresent() || this.offhand.get().test(livingentity.getItemBySlot(EquipmentSlot.OFFHAND));
         }
      } else {
         return false;
      }
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

      public EntityEquipmentPredicate.Builder head(ItemPredicate.Builder p_457502_) {
         this.head = Optional.of(p_457502_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder chest(ItemPredicate.Builder p_460680_) {
         this.chest = Optional.of(p_460680_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder legs(ItemPredicate.Builder p_450554_) {
         this.legs = Optional.of(p_450554_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder feet(ItemPredicate.Builder p_460373_) {
         this.feet = Optional.of(p_460373_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder body(ItemPredicate.Builder p_450972_) {
         this.body = Optional.of(p_450972_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder mainhand(ItemPredicate.Builder p_451232_) {
         this.mainhand = Optional.of(p_451232_.build());
         return this;
      }

      public EntityEquipmentPredicate.Builder offhand(ItemPredicate.Builder p_457033_) {
         this.offhand = Optional.of(p_457033_.build());
         return this;
      }

      public EntityEquipmentPredicate build() {
         return new EntityEquipmentPredicate(this.head, this.chest, this.legs, this.feet, this.body, this.mainhand, this.offhand);
      }
   }
}
