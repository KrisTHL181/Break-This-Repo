package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BlendingDataFix extends DataFix {
   private final String name;
   private static final Set<String> STATUSES_TO_SKIP_BLENDING = Set.of(
      "minecraft:empty", "minecraft:structure_starts", "minecraft:structure_references", "minecraft:biomes"
   );

   public BlendingDataFix(Schema p_216561_) {
      super(p_216561_, false);
      this.name = "Blending Data Fix v" + p_216561_.getVersionKey();
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getOutputSchema().getType(References.CHUNK);
      return this.fixTypeEverywhereTyped(
         this.name, type, p_216563_ -> p_216563_.update(DSL.remainderFinder(), p_240248_ -> updateChunkTag(p_240248_, p_240248_.get("__context")))
      );
   }

   private static Dynamic<?> updateChunkTag(Dynamic<?> p_240279_, OptionalDynamic<?> p_240280_) {
      p_240279_ = p_240279_.remove("blending_data");
      boolean flag = "minecraft:overworld".equals(p_240280_.get("dimension").asString().result().orElse(""));
      Optional<? extends Dynamic<?>> optional = p_240279_.get("Status").result();
      if (flag && optional.isPresent()) {
         String s = NamespacedSchema.ensureNamespaced(optional.get().asString("empty"));
         Optional<? extends Dynamic<?>> optional1 = p_240279_.get("below_zero_retrogen").result();
         if (!STATUSES_TO_SKIP_BLENDING.contains(s)) {
            p_240279_ = updateBlendingData(p_240279_, 384, -64);
         } else if (optional1.isPresent()) {
            Dynamic<?> dynamic = (Dynamic<?>)optional1.get();
            String s1 = NamespacedSchema.ensureNamespaced(dynamic.get("target_status").asString("empty"));
            if (!STATUSES_TO_SKIP_BLENDING.contains(s1)) {
               p_240279_ = updateBlendingData(p_240279_, 256, 0);
            }
         }
      }

      return p_240279_;
   }

   private static Dynamic<?> updateBlendingData(Dynamic<?> p_216567_, int p_216568_, int p_216569_) {
      return p_216567_.set(
         "blending_data",
         p_216567_.createMap(
            Map.of(
               p_216567_.createString("min_section"),
               p_216567_.createInt(SectionPos.blockToSectionCoord(p_216569_)),
               p_216567_.createString("max_section"),
               p_216567_.createInt(SectionPos.blockToSectionCoord(p_216569_ + p_216568_))
            )
         )
      );
   }
}
