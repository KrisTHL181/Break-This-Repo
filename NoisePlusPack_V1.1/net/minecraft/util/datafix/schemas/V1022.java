package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1022 extends Schema {
   public V1022(int p_17365_, Schema p_17366_) {
      super(p_17365_, p_17366_);
   }

   public void registerTypes(Schema p_17373_, Map<String, Supplier<TypeTemplate>> p_17374_, Map<String, Supplier<TypeTemplate>> p_17375_) {
      super.registerTypes(p_17373_, p_17374_, p_17375_);
      p_17373_.registerType(false, References.RECIPE, () -> DSL.constType(NamespacedSchema.namespacedString()));
      p_17373_.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields(
            new Pair[]{
               Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(p_17373_))),
               Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(p_17373_))),
               Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(p_17373_))),
               Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(p_17373_))),
               Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(p_17373_)),
               Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(p_17373_)),
               Pair.of(
                  "recipeBook",
                  DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(p_17373_)), "toBeDisplayed", DSL.list(References.RECIPE.in(p_17373_)))
               )
            }
         )
      );
      p_17373_.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(p_17373_))));
   }
}
