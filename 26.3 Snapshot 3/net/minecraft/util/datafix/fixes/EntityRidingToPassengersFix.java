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
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class EntityRidingToPassengersFix extends DataFix {
   public EntityRidingToPassengersFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Schema inputSchema = this.getInputSchema();
      Schema outputSchema = this.getOutputSchema();
      Type<?> oldEntityTreeType = inputSchema.getTypeRaw(References.ENTITY_TREE);
      Type<?> newEntityTreeType = outputSchema.getTypeRaw(References.ENTITY_TREE);
      Type<?> entityType = inputSchema.getTypeRaw(References.ENTITY);
      return this.cap(inputSchema, outputSchema, oldEntityTreeType, newEntityTreeType, entityType);
   }

   private <OldEntityTree, NewEntityTree, Entity> TypeRewriteRule cap(
      final Schema inputSchema,
      final Schema outputType,
      final Type<OldEntityTree> oldEntityTreeType,
      final Type<NewEntityTree> newEntityTreeType,
      final Type<Entity> entityType
   ) {
      Type<Pair<String, Pair<Either<OldEntityTree, Unit>, Entity>>> oldType = DSL.named(
         References.ENTITY_TREE.typeName(), DSL.and(DSL.optional(DSL.field("Riding", oldEntityTreeType)), entityType)
      );
      Type<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> newType = DSL.named(
         References.ENTITY_TREE.typeName(), DSL.and(DSL.optional(DSL.field("Passengers", DSL.list(newEntityTreeType))), entityType)
      );
      Type<?> oldEntityType = inputSchema.getType(References.ENTITY_TREE);
      Type<?> newEntityType = outputType.getType(References.ENTITY_TREE);
      if (!Objects.equals(oldEntityType, oldType)) {
         throw new IllegalStateException("Old entity type is not what was expected.");
      }

      if (!newEntityType.equals(newType, true, true)) {
         throw new IllegalStateException("New entity type is not what was expected.");
      }

      Type<?> patchedEntityTreeType = ExtraDataFixUtils.patchSubType(oldType, oldType, newType);
      OpticFinder<Entity> entityFinder = DSL.typeFinder(entityType);
      OpticFinder<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> newEntityTreeValueFinder = DSL.typeFinder(newType);
      OpticFinder<NewEntityTree> ridingFinder = DSL.fieldFinder("Riding", newEntityTreeType);
      Type<?> oldPlayerType = inputSchema.getType(References.PLAYER);
      Type<?> newPlayerType = outputType.getType(References.PLAYER);
      return TypeRewriteRule.seq(
         this.fixTypeEverywhere(
            "EntityRidingToPassengerFix",
            oldType,
            newType,
            ops -> badlyTypedInput -> {
               Typed<?> input = ExtraDataFixUtils.cast(patchedEntityTreeType, badlyTypedInput, ops);
               Optional<Pair<String, Pair<Either<List<NewEntityTree>, Unit>, Entity>>> maybeRiding = input.getOptionalTyped(ridingFinder)
                  .flatMap(t -> t.getOptional(newEntityTreeValueFinder));
               Entity entity = (Entity)input.getOptional(entityFinder).orElseThrow();
               if (maybeRiding.isEmpty()) {
                  Either<List<NewEntityTree>, Unit> passengers = Either.right(Unit.INSTANCE);
                  return Pair.of(References.ENTITY_TREE.typeName(), Pair.of(passengers, entity));
               } else {
                  return addPassengerToTop(maybeRiding.get(), entity, ops, newEntityTreeType, newEntityTreeValueFinder);
               }
            }
         ),
         this.writeAndRead("player RootVehicle injecter", oldPlayerType, newPlayerType)
      );
   }

   private static <Entity, EntityTree> Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> addPassengerToTop(
      final Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> root,
      final Entity passengerEntity,
      final DynamicOps<?> ops,
      final Type<EntityTree> rawEntityTreeType,
      final OpticFinder<Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>>> entityTreeFinder
   ) {
      Entity rootEntity = (Entity)((Pair)root.getSecond()).getSecond();
      Optional<List<EntityTree>> passengers = ((Either)((Pair)root.getSecond()).getFirst()).left();
      Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> newPassenger;
      if (passengers.isPresent() && !passengers.get().isEmpty()) {
         Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> unwrappedPassenger = unwrapRecursiveValue(
            passengers.get().getFirst(), ops, rawEntityTreeType, entityTreeFinder
         );
         newPassenger = addPassengerToTop(unwrappedPassenger, passengerEntity, ops, rawEntityTreeType, entityTreeFinder);
      } else {
         newPassenger = Pair.of(References.ENTITY_TREE.typeName(), Pair.of(Either.right(Unit.INSTANCE), passengerEntity));
      }

      List<EntityTree> newPassengers = List.of(wrapRecursiveValue(newPassenger, ops, rawEntityTreeType, entityTreeFinder));
      return Pair.of(References.ENTITY_TREE.typeName(), Pair.of(Either.left(newPassengers), rootEntity));
   }

   private static <Raw, Value> Value unwrapRecursiveValue(final Raw raw, final DynamicOps<?> ops, final Type<Raw> rawType, final OpticFinder<Value> valueFinder) {
      return (Value)new Typed(rawType, ops, raw).getOptional(valueFinder).orElseThrow();
   }

   private static <Raw, Value> Raw wrapRecursiveValue(final Value value, final DynamicOps<?> ops, final Type<Raw> rawType, final OpticFinder<Value> valueFinder) {
      return (Raw)((Typed)rawType.pointTyped(ops).orElseThrow()).set(valueFinder, value).getValue();
   }
}
