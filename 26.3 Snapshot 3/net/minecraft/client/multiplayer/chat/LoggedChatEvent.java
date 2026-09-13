package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;

public interface LoggedChatEvent {
   Codec<LoggedChatEvent> CODEC = StringRepresentable.fromEnum(LoggedChatEvent.Type::values).dispatch(LoggedChatEvent::type, LoggedChatEvent.Type::codec);

   LoggedChatEvent.Type type();

   enum Type implements StringRepresentable {
      PLAYER("player", () -> LoggedChatMessage.Player.CODEC),
      SYSTEM("system", () -> LoggedChatMessage.System.CODEC);

      private final String serializedName;
      private final Supplier<MapCodec<? extends LoggedChatEvent>> codec;

      Type(final String serializedName, final Supplier<MapCodec<? extends LoggedChatEvent>> codec) {
         this.serializedName = serializedName;
         this.codec = codec;
      }

      private MapCodec<? extends LoggedChatEvent> codec() {
         return this.codec.get();
      }

      @Override
      public String getSerializedName() {
         return this.serializedName;
      }
   }
}
