package net.minecraft.server.packs.repository;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.InclusiveRange;

public enum PackCompatibility {
   TOO_OLD("old"),
   TOO_NEW("new"),
   UNKNOWN("unknown"),
   COMPATIBLE("compatible");

   public static final int UNKNOWN_VERSION = Integer.MAX_VALUE;
   private final Component description;
   private final Component confirmation;

   PackCompatibility(final String p_10488_) {
      this.description = Component.translatable("pack.incompatible." + p_10488_).withStyle(ChatFormatting.GRAY);
      this.confirmation = Component.translatable("pack.incompatible.confirm." + p_10488_);
   }

   public boolean isCompatible() {
      return this == COMPATIBLE;
   }

   public static PackCompatibility forVersion(InclusiveRange<PackFormat> p_300208_, PackFormat p_425079_) {
      if (p_300208_.minInclusive().major() == Integer.MAX_VALUE) {
         return UNKNOWN;
      } else if (p_300208_.maxInclusive().compareTo(p_425079_) < 0) {
         return TOO_OLD;
      } else {
         return p_425079_.compareTo(p_300208_.minInclusive()) < 0 ? TOO_NEW : COMPATIBLE;
      }
   }

   public Component getDescription() {
      return this.description;
   }

   public Component getConfirmation() {
      return this.confirmation;
   }
}
