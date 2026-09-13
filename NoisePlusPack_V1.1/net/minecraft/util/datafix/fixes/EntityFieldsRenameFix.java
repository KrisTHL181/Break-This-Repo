package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Map.Entry;

public class EntityFieldsRenameFix extends NamedEntityFix {
   private final Map<String, String> renames;

   public EntityFieldsRenameFix(Schema p_378547_, String p_378612_, String p_377255_, Map<String, String> p_377805_) {
      super(p_378547_, false, p_378612_, References.ENTITY, p_377255_);
      this.renames = p_377805_;
   }

   public Dynamic<?> fixTag(Dynamic<?> p_378263_) {
      for (Entry<String, String> entry : this.renames.entrySet()) {
         p_378263_ = p_378263_.renameField(entry.getKey(), entry.getValue());
      }

      return p_378263_;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_378322_) {
      return p_378322_.update(DSL.remainderFinder(), this::fixTag);
   }
}
