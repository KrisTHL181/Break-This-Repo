package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentTypeInfo<StringArgumentType, StringArgumentSerializer.Template> {
   public void serializeToNetwork(final StringArgumentSerializer.Template template, final FriendlyByteBuf out) {
      out.writeEnum(template.type);
   }

   public StringArgumentSerializer.Template deserializeFromNetwork(final FriendlyByteBuf in) {
      StringType type = in.readEnum(StringType.class);
      return new StringArgumentSerializer.Template(type);
   }

   public void serializeToJson(final StringArgumentSerializer.Template template, final JsonObject out) {
      out.addProperty("type", switch (template.type) {
         case SINGLE_WORD -> "word";
         case QUOTABLE_PHRASE -> "phrase";
         case GREEDY_PHRASE -> "greedy";
         default -> throw new MatchException(null, null);
      });
   }

   public StringArgumentSerializer.Template unpack(final StringArgumentType argument) {
      return new StringArgumentSerializer.Template(argument.getType());
   }

   public final class Template implements ArgumentTypeInfo.Template<StringArgumentType> {
      private final StringType type;

      public Template(final StringType type) {
         this.type = type;
      }

      public StringArgumentType instantiate(final CommandBuildContext context) {
         return switch (this.type) {
            case SINGLE_WORD -> StringArgumentType.word();
            case QUOTABLE_PHRASE -> StringArgumentType.string();
            case GREEDY_PHRASE -> StringArgumentType.greedyString();
            default -> throw new MatchException(null, null);
         };
      }

      @Override
      public ArgumentTypeInfo<StringArgumentType, ?> type() {
         return StringArgumentSerializer.this;
      }
   }
}
