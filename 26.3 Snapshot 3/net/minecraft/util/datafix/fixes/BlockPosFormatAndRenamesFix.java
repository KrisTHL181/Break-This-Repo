package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BlockPosFormatAndRenamesFix extends DataFix {
   private static final List<String> PATROLLING_MOBS = List.of(
      "minecraft:witch", "minecraft:ravager", "minecraft:pillager", "minecraft:illusioner", "minecraft:evoker", "minecraft:vindicator"
   );

   public BlockPosFormatAndRenamesFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   private Typed<?> fixFields(final Typed<?> typed, final Map<String, String> fields) {
      return typed.update(DSL.remainderFinder(), tag -> {
         for (Entry<String, String> entry : fields.entrySet()) {
            tag = tag.renameAndFixField(entry.getKey(), entry.getValue(), ExtraDataFixUtils::fixBlockPos);
         }

         return tag;
      });
   }

   private <T> Dynamic<T> fixMapSavedData(final Dynamic<T> data) {
      return data.update("frames", frames -> frames.createList(frames.asStream().map(frame -> {
         frame = frame.renameAndFixField("Pos", "pos", ExtraDataFixUtils::fixBlockPos);
         frame = frame.renameField("Rotation", "rotation");
         return frame.renameField("EntityId", "entity_id");
      }))).update("banners", banners -> banners.createList(banners.asStream().map(banner -> {
         banner = banner.renameField("Pos", "pos");
         banner = banner.renameField("Color", "color");
         return banner.renameField("Name", "name");
      })));
   }

   public TypeRewriteRule makeRule() {
      List<TypeRewriteRule> rules = new ArrayList<>();
      this.addEntityRules(rules);
      this.addBlockEntityRules(rules);
      rules.add(
         this.writeFixAndRead(
            "BlockPos format for map frames",
            this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA),
            this.getOutputSchema().getType(References.SAVED_DATA_MAP_DATA),
            input -> input.update("data", this::fixMapSavedData)
         )
      );
      Type<?> itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
      rules.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format for compass target",
            itemStackType,
            ItemStackTagFix.createFixer(
               itemStackType,
               "minecraft:compass"::equals,
               typed -> typed.update(DSL.remainderFinder(), tag -> tag.update("LodestonePos", ExtraDataFixUtils::fixBlockPos))
            )
         )
      );
      return TypeRewriteRule.seq(rules);
   }

   private void addEntityRules(final List<TypeRewriteRule> rules) {
      rules.add(this.createEntityFixer(References.ENTITY, "minecraft:bee", Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")));
      rules.add(this.createEntityFixer(References.ENTITY, "minecraft:end_crystal", Map.of("BeamTarget", "beam_target")));
      rules.add(this.createEntityFixer(References.ENTITY, "minecraft:wandering_trader", Map.of("WanderTarget", "wander_target")));

      for (String patrollingMob : PATROLLING_MOBS) {
         rules.add(this.createEntityFixer(References.ENTITY, patrollingMob, Map.of("PatrolTarget", "patrol_target")));
      }

      rules.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format in Leash for mobs",
            this.getInputSchema().getType(References.ENTITY),
            input -> input.update(DSL.remainderFinder(), tag -> tag.renameAndFixField("Leash", "leash", ExtraDataFixUtils::fixBlockPos))
         )
      );
   }

   private void addBlockEntityRules(final List<TypeRewriteRule> rules) {
      rules.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:beehive", Map.of("FlowerPos", "flower_pos")));
      rules.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:end_gateway", Map.of("ExitPortal", "exit_portal")));
   }

   private TypeRewriteRule createEntityFixer(final TypeReference type, final String entityName, final Map<String, String> fields) {
      String name = "BlockPos format in " + fields.keySet() + " for " + entityName + " (" + type.typeName() + ")";
      OpticFinder<?> entityF = DSL.namedChoice(entityName, this.getInputSchema().getChoiceType(type, entityName));
      return this.fixTypeEverywhereTyped(
         name, this.getInputSchema().getType(type), input -> input.updateTyped(entityF, entity -> this.fixFields(entity, fields))
      );
   }
}
