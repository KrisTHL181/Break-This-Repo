package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityAttributeBaseValueFix extends NamedEntityFix {
   private final String attributeId;
   private final DoubleUnaryOperator valueFixer;

   public EntityAttributeBaseValueFix(Schema p_376018_, String p_377145_, String p_377550_, String p_378675_, DoubleUnaryOperator p_376118_) {
      super(p_376018_, false, p_377145_, References.ENTITY, p_377550_);
      this.attributeId = p_378675_;
      this.valueFixer = p_376118_;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_378195_) {
      return p_378195_.update(DSL.remainderFinder(), this::fixValue);
   }

   private Dynamic<?> fixValue(Dynamic<?> p_377977_) {
      return p_377977_.update("attributes", p_376705_ -> p_377977_.createList(p_376705_.asStream().map(p_378716_ -> {
         String s = NamespacedSchema.ensureNamespaced(p_378716_.get("id").asString(""));
         if (!s.equals(this.attributeId)) {
            return (Dynamic)p_378716_;
         }

         double d0 = p_378716_.get("base").asDouble(0.0);
         return p_378716_.set("base", p_378716_.createDouble(this.valueFixer.applyAsDouble(d0)));
      })));
   }
}
