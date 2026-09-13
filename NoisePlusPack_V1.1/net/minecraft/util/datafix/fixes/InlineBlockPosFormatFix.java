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
   public InlineBlockPosFormatFix(Schema p_391535_) {
      super(p_391535_, false);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<?> opticfinder = this.entityFinder("minecraft:vex");
      OpticFinder<?> opticfinder1 = this.entityFinder("minecraft:phantom");
      OpticFinder<?> opticfinder2 = this.entityFinder("minecraft:turtle");
      List<OpticFinder<?>> list = List.of(
         this.entityFinder("minecraft:item_frame"),
         this.entityFinder("minecraft:glow_item_frame"),
         this.entityFinder("minecraft:painting"),
         this.entityFinder("minecraft:leash_knot")
      );
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - player",
            this.getInputSchema().getType(References.PLAYER),
            p_391680_ -> p_391680_.update(DSL.remainderFinder(), this::fixPlayer)
         ),
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - entity",
            this.getInputSchema().getType(References.ENTITY),
            p_397287_ -> {
               p_397287_ = p_397287_.update(DSL.remainderFinder(), this::fixLivingEntity)
                  .updateTyped(opticfinder, p_393874_ -> p_393874_.update(DSL.remainderFinder(), this::fixVex))
                  .updateTyped(opticfinder1, p_392186_ -> p_392186_.update(DSL.remainderFinder(), this::fixPhantom))
                  .updateTyped(opticfinder2, p_391416_ -> p_391416_.update(DSL.remainderFinder(), this::fixTurtle));

               for (OpticFinder<?> opticfinder3 : list) {
                  p_397287_ = p_397287_.updateTyped(opticfinder3, p_391296_ -> p_391296_.update(DSL.remainderFinder(), this::fixBlockAttached));
               }

               return p_397287_;
            }
         )
      );
   }

   private OpticFinder<?> entityFinder(String p_391479_) {
      return DSL.namedChoice(p_391479_, this.getInputSchema().getChoiceType(References.ENTITY, p_391479_));
   }

   private Dynamic<?> fixPlayer(Dynamic<?> p_393275_) {
      p_393275_ = this.fixLivingEntity(p_393275_);
      Optional<Number> optional = p_393275_.get("SpawnX").asNumber().result();
      Optional<Number> optional1 = p_393275_.get("SpawnY").asNumber().result();
      Optional<Number> optional2 = p_393275_.get("SpawnZ").asNumber().result();
      if (optional.isPresent() && optional1.isPresent() && optional2.isPresent()) {
         Dynamic<?> dynamic = p_393275_.createMap(
            Map.of(
               p_393275_.createString("pos"),
               ExtraDataFixUtils.createBlockPos(p_393275_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
            )
         );
         dynamic = Dynamic.copyField(p_393275_, "SpawnAngle", dynamic, "angle");
         dynamic = Dynamic.copyField(p_393275_, "SpawnDimension", dynamic, "dimension");
         dynamic = Dynamic.copyField(p_393275_, "SpawnForced", dynamic, "forced");
         p_393275_ = p_393275_.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
         p_393275_ = p_393275_.set("respawn", dynamic);
      }

      Optional<? extends Dynamic<?>> optional3 = p_393275_.get("enteredNetherPosition").result();
      if (optional3.isPresent()) {
         p_393275_ = p_393275_.remove("enteredNetherPosition")
            .set(
               "entered_nether_pos",
               p_393275_.createList(
                  Stream.of(
                     p_393275_.createDouble(optional3.get().get("x").asDouble(0.0)),
                     p_393275_.createDouble(optional3.get().get("y").asDouble(0.0)),
                     p_393275_.createDouble(optional3.get().get("z").asDouble(0.0))
                  )
               )
            );
      }

      return p_393275_;
   }

   private Dynamic<?> fixLivingEntity(Dynamic<?> p_394790_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_394790_, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
   }

   private Dynamic<?> fixVex(Dynamic<?> p_393116_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_393116_.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
   }

   private Dynamic<?> fixPhantom(Dynamic<?> p_397511_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_397511_.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
   }

   private Dynamic<?> fixTurtle(Dynamic<?> p_392400_) {
      p_392400_ = p_392400_.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
      p_392400_ = ExtraDataFixUtils.fixInlineBlockPos(p_392400_, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
      return p_392400_.renameField("HasEgg", "has_egg");
   }

   private Dynamic<?> fixBlockAttached(Dynamic<?> p_392055_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_392055_, "TileX", "TileY", "TileZ", "block_pos");
   }
}
