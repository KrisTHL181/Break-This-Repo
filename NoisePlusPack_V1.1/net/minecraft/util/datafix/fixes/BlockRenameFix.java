package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class BlockRenameFix extends DataFix {
   private final String name;

   public BlockRenameFix(Schema p_14910_, String p_14911_) {
      super(p_14910_, false);
      this.name = p_14911_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_NAME);
      Type<Pair<String, String>> type1 = DSL.named(References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(type, type1)) {
         throw new IllegalStateException("block type is not what was expected.");
      }

      TypeRewriteRule typerewriterule = this.fixTypeEverywhere(this.name + " for block", type1, p_14923_ -> p_145145_ -> p_145145_.mapSecond(this::renameBlock));
      TypeRewriteRule typerewriterule1 = this.fixTypeEverywhereTyped(
         this.name + " for block_state",
         this.getInputSchema().getType(References.BLOCK_STATE),
         p_14913_ -> p_14913_.update(DSL.remainderFinder(), this::fixBlockState)
      );
      TypeRewriteRule typerewriterule2 = this.fixTypeEverywhereTyped(
         this.name + " for flat_block_state",
         this.getInputSchema().getType(References.FLAT_BLOCK_STATE),
         p_326554_ -> p_326554_.update(
            DSL.remainderFinder(),
            p_326555_ -> (Dynamic)DataFixUtils.orElse(p_326555_.asString().result().map(this::fixFlatBlockState).map(p_326555_::createString), p_326555_)
         )
      );
      return TypeRewriteRule.seq(typerewriterule, new TypeRewriteRule[]{typerewriterule1, typerewriterule2});
   }

   private Dynamic<?> fixBlockState(Dynamic<?> p_330740_) {
      Optional<String> optional = p_330740_.get("Name").asString().result();
      return optional.isPresent() ? p_330740_.set("Name", p_330740_.createString(this.renameBlock(optional.get()))) : p_330740_;
   }

   private String fixFlatBlockState(String p_332510_) {
      int i = p_332510_.indexOf(91);
      int j = p_332510_.indexOf(123);
      int k = p_332510_.length();
      if (i > 0) {
         k = i;
      }

      if (j > 0) {
         k = Math.min(k, j);
      }

      String s = p_332510_.substring(0, k);
      String s1 = this.renameBlock(s);
      return s1 + p_332510_.substring(k);
   }

   protected abstract String renameBlock(String var1);

   public static DataFix create(Schema p_14915_, String p_14916_, final Function<String, String> p_14917_) {
      return new BlockRenameFix(p_14915_, p_14916_) {
         @Override
         protected String renameBlock(String p_333306_) {
            return p_14917_.apply(p_333306_);
         }
      };
   }
}
