package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class DoubleArgumentInfo implements ArgumentTypeInfo<DoubleArgumentType, DoubleArgumentInfo.Template> {
   public void serializeToNetwork(DoubleArgumentInfo.Template p_235485_, FriendlyByteBuf p_235486_) {
      boolean flag = p_235485_.min != -Double.MAX_VALUE;
      boolean flag1 = p_235485_.max != Double.MAX_VALUE;
      p_235486_.writeByte(ArgumentUtils.createNumberFlags(flag, flag1));
      if (flag) {
         p_235486_.writeDouble(p_235485_.min);
      }

      if (flag1) {
         p_235486_.writeDouble(p_235485_.max);
      }
   }

   public DoubleArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf p_235488_) {
      byte b0 = p_235488_.readByte();
      double d0 = ArgumentUtils.numberHasMin(b0) ? p_235488_.readDouble() : -Double.MAX_VALUE;
      double d1 = ArgumentUtils.numberHasMax(b0) ? p_235488_.readDouble() : Double.MAX_VALUE;
      return new DoubleArgumentInfo.Template(d0, d1);
   }

   public void serializeToJson(DoubleArgumentInfo.Template p_235482_, JsonObject p_235483_) {
      if (p_235482_.min != -Double.MAX_VALUE) {
         p_235483_.addProperty("min", p_235482_.min);
      }

      if (p_235482_.max != Double.MAX_VALUE) {
         p_235483_.addProperty("max", p_235482_.max);
      }
   }

   public DoubleArgumentInfo.Template unpack(DoubleArgumentType p_235474_) {
      return new DoubleArgumentInfo.Template(p_235474_.getMinimum(), p_235474_.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<DoubleArgumentType> {
      final double min;
      final double max;

      Template(final double p_235496_, final double p_235497_) {
         this.min = p_235496_;
         this.max = p_235497_;
      }

      public DoubleArgumentType instantiate(CommandBuildContext p_235500_) {
         return DoubleArgumentType.doubleArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<DoubleArgumentType, ?> type() {
         return DoubleArgumentInfo.this;
      }
   }
}
