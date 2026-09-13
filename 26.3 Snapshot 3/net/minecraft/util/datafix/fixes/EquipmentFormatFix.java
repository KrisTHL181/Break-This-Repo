package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EquipmentFormatFix extends DataFix {
   public EquipmentFormatFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> oldItemStackType = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
      Type<?> newItemStackType = this.getOutputSchema().getTypeRaw(References.ITEM_STACK);
      OpticFinder<?> idFinder = oldItemStackType.findField("id");
      return this.fix(oldItemStackType, newItemStackType, idFinder);
   }

   private <ItemStackOld, ItemStackNew> TypeRewriteRule fix(
      final Type<ItemStackOld> oldItemStackType, final Type<ItemStackNew> newItemStackType, final OpticFinder<?> idFinder
   ) {
      Type<Pair<String, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>>> oldEquipmentType = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(oldItemStackType))),
            DSL.optional(DSL.field("HandItems", DSL.list(oldItemStackType))),
            DSL.optional(DSL.field("body_armor_item", oldItemStackType)),
            DSL.optional(DSL.field("saddle", oldItemStackType))
         )
      );
      Type<Pair<String, Either<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>>> newEquipmentType = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.optional(
            DSL.field(
               "equipment",
               DSL.and(
                  DSL.optional(DSL.field("mainhand", newItemStackType)),
                  DSL.optional(DSL.field("offhand", newItemStackType)),
                  DSL.optional(DSL.field("feet", newItemStackType)),
                  DSL.and(
                     DSL.optional(DSL.field("legs", newItemStackType)),
                     DSL.optional(DSL.field("chest", newItemStackType)),
                     DSL.optional(DSL.field("head", newItemStackType)),
                     DSL.and(DSL.optional(DSL.field("body", newItemStackType)), DSL.optional(DSL.field("saddle", newItemStackType)), DSL.remainderType())
                  )
               )
            )
         )
      );
      if (!oldEquipmentType.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Input entity_equipment type does not match expected");
      } else if (!newEquipmentType.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Output entity_equipment type does not match expected");
      } else {
         return this.fixTypeEverywhere(
            "EquipmentFormatFix",
            oldEquipmentType,
            newEquipmentType,
            ops -> {
               Predicate<ItemStackOld> isPlaceholder = itemStack -> {
                  Typed<ItemStackOld> typed = new Typed(oldItemStackType, ops, itemStack);
                  return typed.getOptional(idFinder).isEmpty();
               };
               return namedOldEquipment -> {
                  String typeName = (String)namedOldEquipment.getFirst();
                  Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>> oldEquipment = (Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>)namedOldEquipment.getSecond();
                  List<ItemStackOld> armorItems = (List<ItemStackOld>)((Either)oldEquipment.getFirst()).map(Function.identity(), ignored -> List.of());
                  List<ItemStackOld> handItems = (List<ItemStackOld>)((Either)((Pair)oldEquipment.getSecond()).getFirst())
                     .map(Function.identity(), ignored -> List.of());
                  Either<ItemStackOld, Unit> body = (Either<ItemStackOld, Unit>)((Pair)((Pair)oldEquipment.getSecond()).getSecond()).getFirst();
                  Either<ItemStackOld, Unit> saddle = (Either<ItemStackOld, Unit>)((Pair)((Pair)oldEquipment.getSecond()).getSecond()).getSecond();
                  Either<ItemStackOld, Unit> feet = getItemFromList(0, armorItems, isPlaceholder);
                  Either<ItemStackOld, Unit> legs = getItemFromList(1, armorItems, isPlaceholder);
                  Either<ItemStackOld, Unit> chest = getItemFromList(2, armorItems, isPlaceholder);
                  Either<ItemStackOld, Unit> head = getItemFromList(3, armorItems, isPlaceholder);
                  Either<ItemStackOld, Unit> mainhand = getItemFromList(0, handItems, isPlaceholder);
                  Either<ItemStackOld, Unit> offhand = getItemFromList(1, handItems, isPlaceholder);
                  return areAllEmpty(body, saddle, feet, legs, chest, head, mainhand, offhand)
                     ? Pair.of(typeName, Either.right(Unit.INSTANCE))
                     : Pair.of(
                        typeName,
                        Either.left(
                           Pair.of(
                              mainhand,
                              Pair.of(offhand, Pair.of(feet, Pair.of(legs, Pair.of(chest, Pair.of(head, Pair.of(body, Pair.of(saddle, new Dynamic(ops))))))))
                           )
                        )
                     );
               };
            }
         );
      }
   }

   @SafeVarargs
   private static boolean areAllEmpty(final Either<?, Unit>... fields) {
      for (Either<?, Unit> field : fields) {
         if (field.right().isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static <ItemStack> Either<ItemStack, Unit> getItemFromList(final int index, final List<ItemStack> items, final Predicate<ItemStack> isPlaceholder) {
      if (index >= items.size()) {
         return Either.right(Unit.INSTANCE);
      }

      ItemStack item = items.get(index);
      return isPlaceholder.test(item) ? Either.right(Unit.INSTANCE) : Either.left(item);
   }
}
