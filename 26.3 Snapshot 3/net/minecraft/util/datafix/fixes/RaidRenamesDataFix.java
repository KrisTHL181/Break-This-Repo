package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class RaidRenamesDataFix extends DataFix {
   public RaidRenamesDataFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "RaidRenamesDataFix",
         this.getInputSchema().getType(References.SAVED_DATA_RAIDS),
         input -> input.update(DSL.remainderFinder(), container -> container.update("data", RaidRenamesDataFix::fix))
      );
   }

   private static Dynamic<?> fix(final Dynamic<?> tag) {
      return tag.renameAndFixField("Raids", "raids", raids -> raids.createList(raids.asStream().map(RaidRenamesDataFix::fixRaid)))
         .renameField("Tick", "tick")
         .renameField("NextAvailableID", "next_id");
   }

   private static Dynamic<?> fixRaid(final Dynamic<?> raid) {
      return ExtraDataFixUtils.fixInlineBlockPos(raid, "CX", "CY", "CZ", "center")
         .renameField("Id", "id")
         .renameField("Started", "started")
         .renameField("Active", "active")
         .renameField("TicksActive", "ticks_active")
         .renameField("BadOmenLevel", "raid_omen_level")
         .renameField("GroupsSpawned", "groups_spawned")
         .renameField("PreRaidTicks", "cooldown_ticks")
         .renameField("PostRaidTicks", "post_raid_ticks")
         .renameField("TotalHealth", "total_health")
         .renameField("NumGroups", "group_count")
         .renameField("Status", "status")
         .renameField("HeroesOfTheVillage", "heroes_of_the_village");
   }
}
