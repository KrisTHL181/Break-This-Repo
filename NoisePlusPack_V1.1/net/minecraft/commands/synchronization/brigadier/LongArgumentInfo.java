package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class LongArgumentInfo implements ArgumentTypeInfo<LongArgumentType, LongArgumentInfo.Template> {
   public void serializeToNetwork(LongArgumentInfo.Template p_235584_, FriendlyByteBuf p_235585_) {
      boolean flag = p_235584_.min != Long.MIN_VALUE;
      boolean flag1 = p_235584_.max != Long.MAX_VALUE;
      p_235585_.writeByte(ArgumentUtils.createNumberFlags(flag, flag1));
      if (flag) {
         p_235585_.writeLong(p_235584_.min);
      }

      if (flag1) {
         p_235585_.writeLong(p_235584_.max);
      }
   }

   public LongArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf p_235587_) {
      byte b0 = p_235587_.readByte();
      long i = ArgumentUtils.numberHasMin(b0) ? p_235587_.readLong() : Long.MIN_VALUE;
      long j = ArgumentUtils.numberHasMax(b0) ? p_235587_.readLong() : Long.MAX_VALUE;
      return new LongArgumentInfo.Template(i, j);
   }

   public void serializeToJson(LongArgumentInfo.Template p_235581_, JsonObject p_235582_) {
      if (p_235581_.min != Long.MIN_VALUE) {
         p_235582_.addProperty("min", p_235581_.min);
      }

      if (p_235581_.max != Long.MAX_VALUE) {
         p_235582_.addProperty("max", p_235581_.max);
      }
   }

   public LongArgumentInfo.Template unpack(LongArgumentType p_235573_) {
      return new LongArgumentInfo.Template(p_235573_.getMinimum(), p_235573_.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<LongArgumentType> {
      final long min;
      final long max;

      Template(final long p_235595_, final long p_235596_) {
         this.min = p_235595_;
         this.max = p_235596_;
      }

      public LongArgumentType instantiate(CommandBuildContext p_235599_) {
         return LongArgumentType.longArg(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<LongArgumentType, ?> type() {
         return LongArgumentInfo.this;
      }
   }
}
