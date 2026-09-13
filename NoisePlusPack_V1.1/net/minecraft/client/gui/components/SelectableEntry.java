package net.minecraft.client.gui.components;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SelectableEntry {
   default boolean mouseOverIcon(int p_451881_, int p_459041_, int p_458504_) {
      return p_451881_ >= 0 && p_451881_ < p_458504_ && p_459041_ >= 0 && p_459041_ < p_458504_;
   }

   default boolean mouseOverLeftHalf(int p_451648_, int p_456147_, int p_458745_) {
      return p_451648_ >= 0 && p_451648_ < p_458745_ / 2 && p_456147_ >= 0 && p_456147_ < p_458745_;
   }

   default boolean mouseOverRightHalf(int p_456349_, int p_460165_, int p_453760_) {
      return p_456349_ >= p_453760_ / 2 && p_456349_ < p_453760_ && p_460165_ >= 0 && p_460165_ < p_453760_;
   }

   default boolean mouseOverTopRightQuarter(int p_452927_, int p_454428_, int p_455672_) {
      return p_452927_ >= p_455672_ / 2 && p_452927_ < p_455672_ && p_454428_ >= 0 && p_454428_ < p_455672_ / 2;
   }

   default boolean mouseOverBottomRightQuarter(int p_456199_, int p_451715_, int p_454254_) {
      return p_456199_ >= p_454254_ / 2 && p_456199_ < p_454254_ && p_451715_ >= p_454254_ / 2 && p_451715_ < p_454254_;
   }

   default boolean mouseOverTopLeftQuarter(int p_455728_, int p_450842_, int p_458880_) {
      return p_455728_ >= 0 && p_455728_ < p_458880_ / 2 && p_450842_ >= 0 && p_450842_ < p_458880_ / 2;
   }

   default boolean mouseOverBottomLeftQuarter(int p_454997_, int p_457092_, int p_451792_) {
      return p_454997_ >= 0 && p_454997_ < p_451792_ / 2 && p_457092_ >= p_451792_ / 2 && p_457092_ < p_451792_;
   }
}
