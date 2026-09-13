package net.minecraft.server.packs.repository;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public interface PackSource {
   UnaryOperator<Component> NO_DECORATION = UnaryOperator.identity();
   PackSource DEFAULT = create(NO_DECORATION, true);
   PackSource BUILT_IN = create(decorateWithSource("pack.source.builtin"), true);
   PackSource FEATURE = create(decorateWithSource("pack.source.feature"), false);
   PackSource WORLD = create(decorateWithSource("pack.source.world"), true);
   PackSource SERVER = create(decorateWithSource("pack.source.server"), true);

   Component decorate(Component var1);

   boolean shouldAddAutomatically();

   static PackSource create(final UnaryOperator<Component> p_251995_, final boolean p_249897_) {
      return new PackSource() {
         @Override
         public Component decorate(Component p_251609_) {
            return p_251995_.apply(p_251609_);
         }

         @Override
         public boolean shouldAddAutomatically() {
            return p_249897_;
         }
      };
   }

   private static UnaryOperator<Component> decorateWithSource(String p_10534_) {
      Component component = Component.translatable(p_10534_);
      return p_10539_ -> Component.translatable("pack.nameAndSource", p_10539_, component).withStyle(ChatFormatting.GRAY);
   }
}
