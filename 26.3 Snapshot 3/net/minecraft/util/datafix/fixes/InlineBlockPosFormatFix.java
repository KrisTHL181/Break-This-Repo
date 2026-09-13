package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class InlineBlockPosFormatFix extends DataFix {
   public InlineBlockPosFormatFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<?> vexFinder = this.entityFinder("minecraft:vex");
      OpticFinder<?> phantomFinder = this.entityFinder("minecraft:phantom");
      OpticFinder<?> turtleFinder = this.entityFinder("minecraft:turtle");
      List<OpticFinder<?>> blockAttachedFinders = List.of(
         this.entityFinder("minecraft:item_frame"),
         this.entityFinder("minecraft:glow_item_frame"),
         this.entityFinder("minecraft:painting"),
         this.entityFinder("minecraft:leash_knot")
      );
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - player",
            this.getInputSchema().getType(References.PLAYER),
            player -> player.update(DSL.remainderFinder(), this::fixPlayer)
         ),
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - entity",
            this.getInputSchema().getType(References.ENTITY),
            entity -> {
               entity = entity.update(DSL.remainderFinder(), this::fixLivingEntity)
                  .updateTyped(vexFinder, vex -> vex.update(DSL.remainderFinder(), this::fixVex))
                  .updateTyped(phantomFinder, phantom -> phantom.update(DSL.remainderFinder(), this::fixPhantom))
                  .updateTyped(turtleFinder, turtle -> turtle.update(DSL.remainderFinder(), this::fixTurtle));

               for (OpticFinder<?> blockAttachedFinder : blockAttachedFinders) {
                  entity = entity.updateTyped(blockAttachedFinder, blockAttached -> blockAttached.update(DSL.remainderFinder(), this::fixBlockAttached));
               }

               return entity;
            }
         )
      );
   }

   private OpticFinder<?> entityFinder(final String choiceName) {
      return DSL.namedChoice(choiceName, this.getInputSchema().getChoiceType(References.ENTITY, choiceName));
   }

   private Dynamic<?> fixPlayer(Dynamic<?> tag) {
      tag = this.fixLivingEntity(tag);
      Optional<Number> spawnX = tag.get("SpawnX").asNumber().result();
      Optional<Number> spawnY = tag.get("SpawnY").asNumber().result();
      Optional<Number> spawnZ = tag.get("SpawnZ").asNumber().result();
      if (spawnX.isPresent() && spawnY.isPresent() && spawnZ.isPresent()) {
         Dynamic<?> respawn = tag.createMap(
            Map.of(tag.createString("pos"), ExtraDataFixUtils.createBlockPos(tag, spawnX.get().intValue(), spawnY.get().intValue(), spawnZ.get().intValue()))
         );
         respawn = Dynamic.copyField(tag, "SpawnAngle", respawn, "angle");
         respawn = Dynamic.copyField(tag, "SpawnDimension", respawn, "dimension");
         respawn = Dynamic.copyField(tag, "SpawnForced", respawn, "forced");
         tag = tag.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
         tag = tag.set("respawn", respawn);
      }

      Optional<? extends Dynamic<?>> enteredNetherPos = tag.get("enteredNetherPosition").result();
      if (enteredNetherPos.isPresent()) {
         tag = tag.remove("enteredNetherPosition")
            .set(
               "entered_nether_pos",
               tag.createList(
                  Stream.of(
                     tag.createDouble(enteredNetherPos.get().get("x").asDouble(0.0)),
                     tag.createDouble(enteredNetherPos.get().get("y").asDouble(0.0)),
                     tag.createDouble(enteredNetherPos.get().get("z").asDouble(0.0))
                  )
               )
            );
      }

      return tag;
   }

   private Dynamic<?> fixLivingEntity(final Dynamic<?> tag) {
      return ExtraDataFixUtils.fixInlineBlockPos(tag, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
   }

   private Dynamic<?> fixVex(final Dynamic<?> tag) {
      return ExtraDataFixUtils.fixInlineBlockPos(tag.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
   }

   private Dynamic<?> fixPhantom(final Dynamic<?> tag) {
      return ExtraDataFixUtils.fixInlineBlockPos(tag.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
   }

   private Dynamic<?> fixTurtle(Dynamic<?> tag) {
      tag = tag.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
      tag = ExtraDataFixUtils.fixInlineBlockPos(tag, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
      return tag.renameField("HasEgg", "has_egg");
   }

   private Dynamic<?> fixBlockAttached(final Dynamic<?> tag) {
      return ExtraDataFixUtils.fixInlineBlockPos(tag, "TileX", "TileY", "TileZ", "block_pos");
   }
}
