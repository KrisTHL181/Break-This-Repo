package net.minecraft.client.input;

import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record CharacterEvent(int codepoint, @InputWithModifiers.Modifiers int modifiers) {
   public String codepointAsString() {
      return Character.toString(this.codepoint);
   }

   public boolean isAllowedChatCharacter() {
      return StringUtil.isAllowedChatCharacter(this.codepoint);
   }
}
