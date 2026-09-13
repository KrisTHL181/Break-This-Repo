package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class FloatArgumentInfo implements ArgumentTypeInfo<FloatArgumentType, FloatArgumentInfo.Template> {
   public void serializeToNetwork(FloatArgumentInfo.Template p_235518_, FriendlyByteBuf p_235519_) {
      boolean flag = p_235518_.min != -Float.MAX_VALUE;
      boolean flag1 = p_235518_.max != Float.MAX_VALUE;
      p_235519_.writeByte(ArgumentUtils.createNumberFlags(flag, flag1));
      if (flag) {
         p_235519_.writeFloat(p_235518_.min);
      }

      if (flag1) {
         p_235519_.writeFloat(p_235518_.max);
      }
   }

   public FloatArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf p_235521_) {
      byte b0 = p_235521_.readByte();
      float f = ArgumentUtils.numberHasMin(b0) ? p_235521_.readFloat() : -Float.MAX_VALUE;
      float f1 = ArgumentUtils.numberHasMax(b0) ? p_235521_.readFloat() : Float.MAX_VALUE;
      return new FloatArgumentInfo.Template(f, f1);
   }

   public void serializeToJson(FloatArgumentInfo.Template p_235515_, JsonObject p_235516_) {
      if (p_235515_.min != -Float.MAX_VALUE) {
         p_235516_.addProperty("min", p_235515_.min);
      }

      if (p_235515_.max != Float.MAX_VALUE) {
         p_235516_.addProperty("max", p_235515_.max);
      }
   }

   public FloatArgumentInfo.Template unpack(FloatArgumentType p_235507_) {
      return new FloatArgumentInfo.Template(p_235507_.getMinimum(), p_235507_.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<FloatArgumentType> {
      final float min;
      final float max;

      Template(final float p_235529_, final float p_235530_) {
         this.min = p_235529_;
         this.max = p_235530_;
      }

      public FloatArgumentType instantiate(CommandBuildContext p_235533_) {
         return FloatArgumentType.floatArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<FloatArgumentType, ?> type() {
         return FloatArgumentInfo.this;
      }
   }
}
