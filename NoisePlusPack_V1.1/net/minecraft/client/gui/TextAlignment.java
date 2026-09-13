package net.minecraft.client.gui;

import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum TextAlignment {
   LEFT {
      @Override
      public int calculateLeft(int p_458518_, int p_450821_) {
         return p_458518_;
      }

      @Override
      public int calculateLeft(int p_455143_, Font p_459662_, FormattedCharSequence p_460836_) {
         return p_455143_;
      }
   },
   CENTER {
      @Override
      public int calculateLeft(int p_460766_, int p_455738_) {
         return p_460766_ - p_455738_ / 2;
      }
   },
   RIGHT {
      @Override
      public int calculateLeft(int p_451696_, int p_453531_) {
         return p_451696_ - p_453531_;
      }
   };

   public abstract int calculateLeft(int var1, int var2);

   public int calculateLeft(int p_457845_, Font p_453473_, FormattedCharSequence p_457592_) {
      return this.calculateLeft(p_457845_, p_453473_.width(p_457592_));
   }
}
