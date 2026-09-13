package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public record ChatTypeDecoration(String translationKey, List<ChatTypeDecoration.Parameter> parameters, Style style) {
   public static final Codec<ChatTypeDecoration> CODEC = RecordCodecBuilder.create(
      p_308561_ -> p_308561_.group(
            Codec.STRING.fieldOf("translation_key").forGetter(ChatTypeDecoration::translationKey),
            ChatTypeDecoration.Parameter.CODEC.listOf().fieldOf("parameters").forGetter(ChatTypeDecoration::parameters),
            Style.Serializer.CODEC.optionalFieldOf("style", Style.EMPTY).forGetter(ChatTypeDecoration::style)
         )
         .apply(p_308561_, ChatTypeDecoration::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ChatTypeDecoration> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8,
      ChatTypeDecoration::translationKey,
      ChatTypeDecoration.Parameter.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ChatTypeDecoration::parameters,
      Style.Serializer.TRUSTED_STREAM_CODEC,
      ChatTypeDecoration::style,
      ChatTypeDecoration::new
   );

   public static ChatTypeDecoration withSender(String p_239223_) {
      return new ChatTypeDecoration(p_239223_, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY);
   }

   public static ChatTypeDecoration incomingDirectMessage(String p_239425_) {
      Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
      return new ChatTypeDecoration(p_239425_, List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), style);
   }

   public static ChatTypeDecoration outgoingDirectMessage(String p_240772_) {
      Style style = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);
      return new ChatTypeDecoration(p_240772_, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.CONTENT), style);
   }

   public static ChatTypeDecoration teamMessage(String p_239095_) {
      return new ChatTypeDecoration(
         p_239095_, List.of(ChatTypeDecoration.Parameter.TARGET, ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.CONTENT), Style.EMPTY
      );
   }

   public Component decorate(Component p_241301_, ChatType.Bound p_241391_) {
      Object[] aobject = this.resolveParameters(p_241301_, p_241391_);
      return Component.translatable(this.translationKey, aobject).withStyle(this.style);
   }

   private Component[] resolveParameters(Component p_241365_, ChatType.Bound p_241559_) {
      Component[] acomponent = new Component[this.parameters.size()];

      for (int i = 0; i < acomponent.length; i++) {
         ChatTypeDecoration.Parameter chattypedecoration$parameter = this.parameters.get(i);
         acomponent[i] = chattypedecoration$parameter.select(p_241365_, p_241559_);
      }

      return acomponent;
   }

   public enum Parameter implements StringRepresentable {
      SENDER(0, "sender", (p_241238_, p_241239_) -> p_241239_.name()),
      TARGET(1, "target", (p_326059_, p_326060_) -> p_326060_.targetName().orElse(CommonComponents.EMPTY)),
      CONTENT(2, "content", (p_239974_, p_241427_) -> p_239974_);

      private static final IntFunction<ChatTypeDecoration.Parameter> BY_ID = ByIdMap.continuous(
         p_341100_ -> p_341100_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final Codec<ChatTypeDecoration.Parameter> CODEC = StringRepresentable.fromEnum(ChatTypeDecoration.Parameter::values);
      public static final StreamCodec<ByteBuf, ChatTypeDecoration.Parameter> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_341101_ -> p_341101_.id);
      private final int id;
      private final String name;
      private final ChatTypeDecoration.Parameter.Selector selector;

      Parameter(final int p_342713_, final String p_239588_, final ChatTypeDecoration.Parameter.Selector p_239589_) {
         this.id = p_342713_;
         this.name = p_239588_;
         this.selector = p_239589_;
      }

      public Component select(Component p_241369_, ChatType.Bound p_241509_) {
         return this.selector.select(p_241369_, p_241509_);
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public interface Selector {
         Component select(Component var1, ChatType.Bound var2);
      }
   }
}
