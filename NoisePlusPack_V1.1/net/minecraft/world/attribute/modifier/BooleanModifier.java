package net.minecraft.world.attribute.modifier;

import com.mojang.serialization.Codec;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public enum BooleanModifier implements AttributeModifier<Boolean, Boolean> {
   AND,
   NAND,
   OR,
   NOR,
   XOR,
   XNOR;

   public Boolean apply(Boolean p_454599_, Boolean p_455845_) {
      return switch (this) {
         case AND -> p_455845_ && p_454599_;
         case NAND -> !p_455845_ || !p_454599_;
         case OR -> p_455845_ || p_454599_;
         case NOR -> !p_455845_ && !p_454599_;
         case XOR -> p_455845_ ^ p_454599_;
         case XNOR -> p_455845_ == p_454599_;
      };
   }

   @Override
   public Codec<Boolean> argumentCodec(EnvironmentAttribute<Boolean> p_457217_) {
      return Codec.BOOL;
   }

   @Override
   public LerpFunction<Boolean> argumentKeyframeLerp(EnvironmentAttribute<Boolean> p_454024_) {
      return LerpFunction.ofConstant();
   }
}
