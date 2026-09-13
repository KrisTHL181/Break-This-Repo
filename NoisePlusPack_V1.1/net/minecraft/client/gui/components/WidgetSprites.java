package net.minecraft.client.gui.components;

import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WidgetSprites(Identifier enabled, Identifier disabled, Identifier enabledFocused, Identifier disabledFocused) {
   public WidgetSprites(Identifier p_451229_) {
      this(p_451229_, p_451229_, p_451229_, p_451229_);
   }

   public WidgetSprites(Identifier p_453278_, Identifier p_452761_) {
      this(p_453278_, p_453278_, p_452761_, p_452761_);
   }

   public WidgetSprites(Identifier p_458301_, Identifier p_451399_, Identifier p_450840_) {
      this(p_458301_, p_451399_, p_450840_, p_451399_);
   }

   public Identifier get(boolean p_299771_, boolean p_299716_) {
      if (p_299771_) {
         return p_299716_ ? this.enabledFocused : this.enabled;
      } else {
         return p_299716_ ? this.disabledFocused : this.disabled;
      }
   }
}
