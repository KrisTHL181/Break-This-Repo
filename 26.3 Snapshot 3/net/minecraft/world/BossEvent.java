package net.minecraft.world;

import com.mojang.serialization.Codec;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public abstract class BossEvent {
   private final UUID id;
   protected Component name;
   protected float progress;
   protected BossEvent.BossBarColor color;
   protected BossEvent.BossBarOverlay overlay;
   protected boolean darkenScreen;
   protected boolean playBossMusic;
   protected boolean createWorldFog;

   public BossEvent(final UUID id, final Component name, final BossEvent.BossBarColor color, final BossEvent.BossBarOverlay overlay) {
      this.id = id;
      this.name = name;
      this.color = color;
      this.overlay = overlay;
      this.progress = 1.0F;
   }

   public UUID getId() {
      return this.id;
   }

   public Component getName() {
      return this.name;
   }

   public void setName(final Component name) {
      this.name = name;
   }

   public float getProgress() {
      return this.progress;
   }

   public void setProgress(final float progress) {
      this.progress = progress;
   }

   public BossEvent.BossBarColor getColor() {
      return this.color;
   }

   public void setColor(final BossEvent.BossBarColor color) {
      this.color = color;
   }

   public BossEvent.BossBarOverlay getOverlay() {
      return this.overlay;
   }

   public void setOverlay(final BossEvent.BossBarOverlay overlay) {
      this.overlay = overlay;
   }

   public boolean shouldDarkenScreen() {
      return this.darkenScreen;
   }

   public BossEvent setDarkenScreen(final boolean darkenScreen) {
      this.darkenScreen = darkenScreen;
      return this;
   }

   public boolean shouldPlayBossMusic() {
      return this.playBossMusic;
   }

   public BossEvent setPlayBossMusic(final boolean playBossMusic) {
      this.playBossMusic = playBossMusic;
      return this;
   }

   public BossEvent setCreateWorldFog(final boolean createWorldFog) {
      this.createWorldFog = createWorldFog;
      return this;
   }

   public boolean shouldCreateWorldFog() {
      return this.createWorldFog;
   }

   public enum BossBarColor implements StringRepresentable {
      PINK("pink", ChatFormatting.RED),
      BLUE("blue", ChatFormatting.BLUE),
      RED("red", ChatFormatting.DARK_RED),
      GREEN("green", ChatFormatting.GREEN),
      YELLOW("yellow", ChatFormatting.YELLOW),
      PURPLE("purple", ChatFormatting.DARK_BLUE),
      WHITE("white", ChatFormatting.WHITE);

      public static final Codec<BossEvent.BossBarColor> CODEC = StringRepresentable.fromEnum(BossEvent.BossBarColor::values);
      private final String name;
      private final ChatFormatting formatting;

      BossBarColor(final String name, final ChatFormatting formatting) {
         this.name = name;
         this.formatting = formatting;
      }

      public ChatFormatting getFormatting() {
         return this.formatting;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }

   public enum BossBarOverlay implements StringRepresentable {
      PROGRESS("progress"),
      NOTCHED_6("notched_6"),
      NOTCHED_10("notched_10"),
      NOTCHED_12("notched_12"),
      NOTCHED_20("notched_20");

      public static final Codec<BossEvent.BossBarOverlay> CODEC = StringRepresentable.fromEnum(BossEvent.BossBarOverlay::values);
      private final String name;

      BossBarOverlay(final String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
