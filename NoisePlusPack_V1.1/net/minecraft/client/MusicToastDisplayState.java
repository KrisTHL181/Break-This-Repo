package net.minecraft.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum MusicToastDisplayState implements StringRepresentable {
   NEVER("never", "options.musicToast.never"),
   PAUSE("pause", "options.musicToast.pauseMenu"),
   PAUSE_AND_TOAST("pause_and_toast", "options.musicToast.pauseMenuAndToast");

   public static final Codec<MusicToastDisplayState> CODEC = StringRepresentable.fromEnum(MusicToastDisplayState::values);
   private final String name;
   private final Component text;
   private final Component tooltip;

   MusicToastDisplayState(final String p_453125_, final String p_459953_) {
      this.name = p_453125_;
      this.text = Component.translatable(p_459953_);
      this.tooltip = Component.translatable(p_459953_ + ".tooltip");
   }

   public Component text() {
      return this.text;
   }

   public Component tooltip() {
      return this.tooltip;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean renderInPauseScreen() {
      return this != NEVER;
   }

   public boolean renderToast() {
      return this == PAUSE_AND_TOAST;
   }
}
