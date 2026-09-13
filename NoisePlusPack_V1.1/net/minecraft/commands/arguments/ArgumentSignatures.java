package net.minecraft.commands.arguments;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignableCommand;
import org.jspecify.annotations.Nullable;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> entries) {
   public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
   private static final int MAX_ARGUMENT_COUNT = 8;
   private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

   public ArgumentSignatures(FriendlyByteBuf p_231052_) {
      this(p_231052_.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), ArgumentSignatures.Entry::new));
   }

   public void write(FriendlyByteBuf p_231062_) {
      p_231062_.writeCollection(this.entries, (p_241214_, p_241215_) -> p_241215_.write(p_241214_));
   }

   public static ArgumentSignatures signCommand(SignableCommand<?> p_251621_, ArgumentSignatures.Signer p_248653_) {
      List<ArgumentSignatures.Entry> list = p_251621_.arguments().stream().map(p_247962_ -> {
         MessageSignature messagesignature = p_248653_.sign(p_247962_.value());
         return messagesignature != null ? new ArgumentSignatures.Entry(p_247962_.name(), messagesignature) : null;
      }).filter(Objects::nonNull).toList();
      return new ArgumentSignatures(list);
   }

   public record Entry(String name, MessageSignature signature) {
      public Entry(FriendlyByteBuf p_241305_) {
         this(p_241305_.readUtf(16), MessageSignature.read(p_241305_));
      }

      public void write(FriendlyByteBuf p_241403_) {
         p_241403_.writeUtf(this.name, 16);
         MessageSignature.write(p_241403_, this.signature);
      }
   }

   @FunctionalInterface
   public interface Signer {
      @Nullable MessageSignature sign(String var1);
   }
}
