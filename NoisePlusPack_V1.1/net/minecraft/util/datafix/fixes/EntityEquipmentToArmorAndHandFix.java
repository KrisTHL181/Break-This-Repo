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
   public EntityEquipmentToArmorAndHandFix(Schema p_15417_) {
      super(p_15417_, true);
   }

   public TypeRewriteRule makeRule() {
      return this.cap(this.getInputSchema().getTypeRaw(References.ITEM_STACK), this.getOutputSchema().getTypeRaw(References.ITEM_STACK));
   }

   private <ItemStackOld, ItemStackNew> TypeRewriteRule cap(Type<ItemStackOld> p_15427_, Type<ItemStackNew> p_392382_) {
      Type<Pair<String, Either<List<ItemStackOld>, Unit>>> type = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(), DSL.optional(DSL.field("Equipment", DSL.list(p_15427_)))
      );
      Type<Pair<String, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<List<ItemStackNew>, Unit>, Pair<Either<ItemStackNew, Unit>, Either<ItemStackNew, Unit>>>>>> type1 = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(p_392382_))),
            DSL.optional(DSL.field("HandItems", DSL.list(p_392382_))),
            DSL.optional(DSL.field("body_armor_item", p_392382_)),
            DSL.optional(DSL.field("saddle", p_392382_))
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Input entity_equipment type does not match expected");
      } else if (!type1.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Output entity_equipment type does not match expected");
      } else {
         return TypeRewriteRule.seq(
            this.fixTypeEverywhereTyped(
               "EntityEquipmentToArmorAndHandFix - drop chances",
               this.getInputSchema().getType(References.ENTITY),
               p_390245_ -> p_390245_.update(DSL.remainderFinder(), EntityEquipmentToArmorAndHandFix::fixDropChances)
            ),
            this.fixTypeEverywhere(
               "EntityEquipmentToArmorAndHandFix - equipment",
               type,
               type1,
               p_390243_ -> {
                  ItemStackNew itemstacknew = (ItemStackNew)((Pair)p_392382_.read(new Dynamic(p_390243_).emptyMap())
                        .result()
                        .orElseThrow(() -> new IllegalStateException("Could not parse newly created empty itemstack.")))
                     .getFirst();
                  Either<ItemStackNew, Unit> either = Either.right(DSL.unit());
                  return p_390252_ -> p_390252_.mapSecond(p_390248_ -> {
                     List<ItemStackOld> list = (List<ItemStackOld>)p_390248_.map(Function.identity(), p_390244_ -> List.of());
                     Either<List<ItemStackNew>, Unit> either1 = Either.right(DSL.unit());
                     Either<List<ItemStackNew>, Unit> either2 = Either.right(DSL.unit());
                     if (!list.isEmpty()) {
                        either1 = Either.left(Lists.newArrayList(new Object[]{list.getFirst(), itemstacknew}));
                     }

                     if (list.size() > 1) {
                        List<ItemStackNew> list1 = Lists.newArrayList(new Object[]{itemstacknew, itemstacknew, itemstacknew, itemstacknew});

                        for (int i = 1; i < Math.min(list.size(), 5); i++) {
                           list1.set(i - 1, (ItemStackNew)list.get(i));
                        }

                        either2 = Either.left(list1);
                     }

                     return Pair.of(either2, Pair.of(either1, Pair.of(either, either)));
                  });
               }
            )
         );
      }
   }

   private static Dynamic<?> fixDropChances(Dynamic<?> p_393604_) {
      Optional<? extends Stream<? extends Dynamic<?>>> optional = p_393604_.get("DropChances").asStreamOpt().result();
      p_393604_ = p_393604_.remove("DropChances");
      if (optional.isPresent()) {
         Iterator<Float> iterator = Stream.concat(optional.get().map(p_390249_ -> p_390249_.asFloat(0.0F)), Stream.generate(() -> 0.0F)).iterator();
         float f = iterator.next();
         if (p_393604_.get("HandDropChances").result().isEmpty()) {
            p_393604_ = p_393604_.set("HandDropChances", p_393604_.createList(Stream.of(f, 0.0F).map(p_393604_::createFloat)));
         }

         if (p_393604_.get("ArmorDropChances").result().isEmpty()) {
            p_393604_ = p_393604_.set(
               "ArmorDropChances",
               p_393604_.createList(Stream.of(iterator.next(), iterator.next(), iterator.next(), iterator.next()).map(p_393604_::createFloat))
            );
         }
      }

      return p_393604_;
   }
}
