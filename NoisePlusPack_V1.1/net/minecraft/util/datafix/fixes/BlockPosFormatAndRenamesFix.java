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

   public BlockPosFormatAndRenamesFix(Schema p_334085_) {
      super(p_334085_, true);
   }

   private Typed<?> fixFields(Typed<?> p_329260_, Map<String, String> p_332392_) {
      return p_329260_.update(DSL.remainderFinder(), p_333105_ -> {
         for (Entry<String, String> entry : p_332392_.entrySet()) {
            p_333105_ = p_333105_.renameAndFixField(entry.getKey(), entry.getValue(), ExtraDataFixUtils::fixBlockPos);
         }

         return p_333105_;
      });
   }

   private <T> Dynamic<T> fixMapSavedData(Dynamic<T> p_328964_) {
      return p_328964_.update("frames", p_334922_ -> p_334922_.createList(p_334922_.asStream().map(p_334081_ -> {
         p_334081_ = p_334081_.renameAndFixField("Pos", "pos", ExtraDataFixUtils::fixBlockPos);
         p_334081_ = p_334081_.renameField("Rotation", "rotation");
         return p_334081_.renameField("EntityId", "entity_id");
      }))).update("banners", p_332873_ -> p_332873_.createList(p_332873_.asStream().map(p_333148_ -> {
         p_333148_ = p_333148_.renameField("Pos", "pos");
         p_333148_ = p_333148_.renameField("Color", "color");
         return p_333148_.renameField("Name", "name");
      })));
   }

   public TypeRewriteRule makeRule() {
      List<TypeRewriteRule> list = new ArrayList<>();
      this.addEntityRules(list);
      this.addBlockEntityRules(list);
      list.add(
         this.writeFixAndRead(
            "BlockPos format for map frames",
            this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA),
            this.getOutputSchema().getType(References.SAVED_DATA_MAP_DATA),
            p_335523_ -> p_335523_.update("data", this::fixMapSavedData)
         )
      );
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      list.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format for compass target",
            type,
            ItemStackTagFix.createFixer(
               type,
               "minecraft:compass"::equals,
               p_390227_ -> p_390227_.update(DSL.remainderFinder(), p_330014_ -> p_330014_.update("LodestonePos", ExtraDataFixUtils::fixBlockPos))
            )
         )
      );
      return TypeRewriteRule.seq(list);
   }

   private void addEntityRules(List<TypeRewriteRule> p_328667_) {
      p_328667_.add(this.createEntityFixer(References.ENTITY, "minecraft:bee", Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")));
      p_328667_.add(this.createEntityFixer(References.ENTITY, "minecraft:end_crystal", Map.of("BeamTarget", "beam_target")));
      p_328667_.add(this.createEntityFixer(References.ENTITY, "minecraft:wandering_trader", Map.of("WanderTarget", "wander_target")));

      for (String s : PATROLLING_MOBS) {
         p_328667_.add(this.createEntityFixer(References.ENTITY, s, Map.of("PatrolTarget", "patrol_target")));
      }

      p_328667_.add(
         this.fixTypeEverywhereTyped(
            "BlockPos format in Leash for mobs",
            this.getInputSchema().getType(References.ENTITY),
            p_332531_ -> p_332531_.update(DSL.remainderFinder(), p_333406_ -> p_333406_.renameAndFixField("Leash", "leash", ExtraDataFixUtils::fixBlockPos))
         )
      );
   }

   private void addBlockEntityRules(List<TypeRewriteRule> p_331262_) {
      p_331262_.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:beehive", Map.of("FlowerPos", "flower_pos")));
      p_331262_.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:end_gateway", Map.of("ExitPortal", "exit_portal")));
   }

   private TypeRewriteRule createEntityFixer(TypeReference p_328651_, String p_335363_, Map<String, String> p_335843_) {
      String s = "BlockPos format in " + p_335843_.keySet() + " for " + p_335363_ + " (" + p_328651_.typeName() + ")";
      OpticFinder<?> opticfinder = DSL.namedChoice(p_335363_, this.getInputSchema().getChoiceType(p_328651_, p_335363_));
      return this.fixTypeEverywhereTyped(
         s, this.getInputSchema().getType(p_328651_), p_329758_ -> p_329758_.updateTyped(opticfinder, p_336142_ -> this.fixFields(p_336142_, p_335843_))
      );
   }
}
