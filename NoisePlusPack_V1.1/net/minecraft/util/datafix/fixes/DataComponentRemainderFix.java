package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public abstract class DataComponentRemainderFix extends DataFix {
   private final String name;
   private final String componentId;
   private final String newComponentId;

   public DataComponentRemainderFix(Schema p_376620_, String p_377270_, String p_377955_) {
      this(p_376620_, p_377270_, p_377955_, p_377955_);
   }

   public DataComponentRemainderFix(Schema p_377436_, String p_376489_, String p_375602_, String p_378725_) {
      super(p_377436_, false);
      this.name = p_376489_;
      this.componentId = p_375602_;
      this.newComponentId = p_378725_;
   }

   public final TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
      return this.fixTypeEverywhereTyped(this.name, type, p_375807_ -> p_375807_.update(DSL.remainderFinder(), p_377360_ -> {
         Optional<? extends Dynamic<?>> optional = p_377360_.get(this.componentId).result();
         if (optional.isEmpty()) {
            return p_377360_;
         }

         Dynamic<?> dynamic = this.fixComponent(optional.get());
         return p_377360_.remove(this.componentId).setFieldIfPresent(this.newComponentId, Optional.ofNullable(dynamic));
      }));
   }

   protected abstract <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> var1);
}
