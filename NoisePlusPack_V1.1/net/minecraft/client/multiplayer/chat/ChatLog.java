package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatLog {
   private final LoggedChatEvent[] buffer;
   private int nextId;

   public static Codec<ChatLog> codec(int p_253922_) {
      return Codec.list(LoggedChatEvent.CODEC)
         .comapFlatMap(
            p_274704_ -> {
               int i = p_274704_.size();
               return i > p_253922_
                  ? DataResult.error(() -> "Expected: a buffer of size less than or equal to " + p_253922_ + " but: " + i + " is greater than " + p_253922_)
                  : DataResult.success(new ChatLog(p_253922_, p_274704_));
            },
            ChatLog::loggedChatEvents
         );
   }

   public ChatLog(int p_251403_) {
      this.buffer = new LoggedChatEvent[p_251403_];
   }

   private ChatLog(int p_254212_, List<LoggedChatEvent> p_253624_) {
      this.buffer = p_253624_.toArray(LoggedChatEvent[]::new);
      this.nextId = p_253624_.size();
   }

   private List<LoggedChatEvent> loggedChatEvents() {
      List<LoggedChatEvent> list = new ArrayList<>(this.size());

      for (int i = this.start(); i <= this.end(); i++) {
         list.add(this.lookup(i));
      }

      return list;
   }

   public void push(LoggedChatEvent p_242319_) {
      this.buffer[this.index(this.nextId++)] = p_242319_;
   }

   public @Nullable LoggedChatEvent lookup(int p_239050_) {
      return p_239050_ >= this.start() && p_239050_ <= this.end() ? this.buffer[this.index(p_239050_)] : null;
   }

   private int index(int p_249044_) {
      return p_249044_ % this.buffer.length;
   }

   public int start() {
      return Math.max(this.nextId - this.buffer.length, 0);
   }

   public int end() {
      return this.nextId - 1;
   }

   private int size() {
      return this.end() - this.start() + 1;
   }
}
