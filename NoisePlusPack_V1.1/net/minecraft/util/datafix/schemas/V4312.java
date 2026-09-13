package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4312 extends NamespacedSchema {
   public V4312(int p_393232_, Schema p_397205_) {
      super(p_393232_, p_397205_);
   }

   public void registerTypes(Schema p_392814_, Map<String, Supplier<TypeTemplate>> p_395221_, Map<String, Supplier<TypeTemplate>> p_396729_) {
      super.registerTypes(p_392814_, p_395221_, p_396729_);
      p_392814_.registerType(
         false,
         References.PLAYER,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_392814_),
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(p_392814_))),
                  Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(p_392814_))),
                  Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(p_392814_))),
                  Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(p_392814_))),
                  Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(p_392814_)),
                  Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(p_392814_)),
                  Pair.of(
                     "recipeBook",
                     DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(p_392814_)), "toBeDisplayed", DSL.list(References.RECIPE.in(p_392814_)))
                  )
               }
            )
         )
      );
   }
}
