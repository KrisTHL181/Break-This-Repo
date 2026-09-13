package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentInfo implements ArgumentTypeInfo<IntegerArgumentType, IntegerArgumentInfo.Template> {
   public void serializeToNetwork(final IntegerArgumentInfo.Template template, final FriendlyByteBuf out) {
      boolean hasMin = template.min != Integer.MIN_VALUE;
      boolean hasMax = template.max != Integer.MAX_VALUE;
      out.writeByte(ArgumentUtils.createNumberFlags(hasMin, hasMax));
      if (hasMin) {
         out.writeInt(template.min);
      }

      if (hasMax) {
         out.writeInt(template.max);
      }
   }

   public IntegerArgumentInfo.Template deserializeFromNetwork(final FriendlyByteBuf in) {
      byte flags = in.readByte();
      int min = ArgumentUtils.numberHasMin(flags) ? in.readInt() : Integer.MIN_VALUE;
      int max = ArgumentUtils.numberHasMax(flags) ? in.readInt() : Integer.MAX_VALUE;
      return new IntegerArgumentInfo.Template(min, max);
   }

   public void serializeToJson(final IntegerArgumentInfo.Template template, final JsonObject out) {
      if (template.min != Integer.MIN_VALUE) {
         out.addProperty("min", template.min);
      }

      if (template.max != Integer.MAX_VALUE) {
         out.addProperty("max", template.max);
      }
   }

   public IntegerArgumentInfo.Template unpack(final IntegerArgumentType argument) {
      return new IntegerArgumentInfo.Template(argument.getMinimum(), argument.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<IntegerArgumentType> {
      private final int min;
      private final int max;

      private Template(final int min, final int max) {
         this.min = min;
         this.max = max;
      }

      public IntegerArgumentType instantiate(final CommandBuildContext context) {
         return IntegerArgumentType.integer(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<IntegerArgumentType, ?> type() {
         return IntegerArgumentInfo.this;
      }
   }
}
