package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class EntityEquipmentToArmorAndHandFix extends DataFix {
   public EntityEquipmentToArmorAndHandFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      return this.cap(this.getInputSchema().getTypeRaw(References.ITEM_STACK), this.getOutputSchema().getTypeRaw(References.ITEM_STACK));
   }

   private <ItemStackOld, ItemStackNew> TypeRewriteRule cap(final Type<ItemStackOld> oldItemStackType, final Type<ItemStackNew> newItemStackType) {
      Type<Pair<String, Either<List<ItemStackOld>, Unit>>> oldEquipmentType = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(), DSL.optional(DSL.field("Equipment", DSL.list(oldItemStackType)))
      );
      Type<Pair<String, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<ItemStackNew, Unit>, Either<ItemStackNew, Unit>>>>>> newEquipmentType = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(newItemStackType))),
            DSL.optional(DSL.field("HandItems", DSL.list(newItemStackType))),
            DSL.optional(DSL.field("body_armor_item", newItemStackType)),
            DSL.optional(DSL.field("saddle", newItemStackType))
         )
      );
      if (!oldEquipmentType.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Input entity_equipment type does not match expected");
      } else if (!newEquipmentType.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Output entity_equipment type does not match expected");
      } else {
         return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(
               "EntityEquipmentToArmorAndHandFix - drop chances",
               this.getInputSchema().getType(References.ENTITY),
               typed -> typed.update(DSL.remainderFinder(), EntityEquipmentToArmorAndHandFix::fixDropChances)
            ),
            this.fixTypeEverywhere(
               "EntityEquipmentToArmorAndHandFix - equipment",
               oldEquipmentType,
               newEquipmentType,
               ops -> {
                  ItemStackNew emptyStack = (ItemStackNew)((Pair)newItemStackType.read(new Dynamic(ops).emptyMap())
                        .result()
                        .orElseThrow(() -> new IllegalStateException("Could not parse newly created empty itemstack.")))
                     .getFirst();
                  Either<ItemStackNew, Unit> noItem = Either.right(DSL.unit());
                  return named -> named.mapSecond(equipmentField -> {
                     List<ItemStackOld> items = (List<ItemStackOld>)equipmentField.map(Function.identity(), ignored -> List.of());
                     Either<List<ItemStackNew>, Unit> handItems = Either.right(DSL.unit());
                     Either<List<ItemStackNew>, Unit> armorItems = Either.right(DSL.unit());
                     if (!items.isEmpty()) {
                        handItems = Either.left(Lists.newArrayList(new Object[]{items.getFirst(), emptyStack}));
                     }

                     if (items.size() > 1) {
                        List<ItemStackNew> armor = Lists.newArrayList(new Object[]{emptyStack, emptyStack, emptyStack, emptyStack});

                        for (int i = 1; i < Math.min(items.size(), 5); i++) {
                           armor.set(i - 1, (ItemStackNew)items.get(i));
                        }

                        armorItems = Either.left(armor);
                     }

                     return Pair.of(armorItems, Pair.of(handItems, Pair.of(noItem, noItem)));
                  });
               }
            )
         );
      }
   }

   private static Dynamic<?> fixDropChances(Dynamic<?> tag) {
      Optional<? extends Stream<? extends Dynamic<?>>> dropChances = tag.get("DropChances").asStreamOpt().result();
      tag = tag.remove("DropChances");
      if (dropChances.isPresent()) {
         Iterator<Float> chances = Stream.concat(dropChances.get().map(value -> value.asFloat(0.0F)), Stream.generate(() -> 0.0F)).iterator();
         float handChance = chances.next();
         if (tag.get("HandDropChances").result().isEmpty()) {
            tag = tag.set("HandDropChances", tag.createList(Stream.of(handChance, 0.0F).map(tag::createFloat)));
         }

         if (tag.get("ArmorDropChances").result().isEmpty()) {
            tag = tag.set("ArmorDropChances", tag.createList(Stream.of(chances.next(), chances.next(), chances.next(), chances.next()).map(tag::createFloat)));
         }
      }

      return tag;
   }
}
