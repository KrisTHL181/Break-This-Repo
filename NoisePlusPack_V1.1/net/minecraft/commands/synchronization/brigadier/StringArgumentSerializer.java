package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class StringArgumentSerializer implements ArgumentTypeInfo<StringArgumentType, StringArgumentSerializer.Template> {
   public void serializeToNetwork(StringArgumentSerializer.Template p_235616_, FriendlyByteBuf p_235617_) {
      p_235617_.writeEnum(p_235616_.type);
   }

   public StringArgumentSerializer.Template deserializeFromNetwork(FriendlyByteBuf p_235619_) {
      StringType stringtype = p_235619_.readEnum(StringType.class);
      return new StringArgumentSerializer.Template(stringtype);
   }

   public void serializeToJson(StringArgumentSerializer.Template p_235613_, JsonObject p_235614_) {
      p_235614_.addProperty("type", switch (p_235613_.type) {
         case SINGLE_WORD -> "word";
         case QUOTABLE_PHRASE -> "phrase";
         case GREEDY_PHRASE -> "greedy";
         default -> throw new MatchException(null, null);
      });
   }

   public StringArgumentSerializer.Template unpack(StringArgumentType p_235605_) {
      return new StringArgumentSerializer.Template(p_235605_.getType());
   }

   public final class Template implements ArgumentTypeInfo.Template<StringArgumentType> {
      final StringType type;

      public Template(final StringType p_235626_) {
         this.type = p_235626_;
      }

      public StringArgumentType instantiate(CommandBuildContext p_235629_) {
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
