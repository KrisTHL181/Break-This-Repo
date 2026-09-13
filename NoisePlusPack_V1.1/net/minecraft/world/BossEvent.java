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

   public BossEvent(UUID p_18849_, Component p_18850_, BossEvent.BossBarColor p_18851_, BossEvent.BossBarOverlay p_18852_) {
      this.id = p_18849_;
      this.name = p_18850_;
      this.color = p_18851_;
      this.overlay = p_18852_;
      this.progress = 1.0F;
   }

   public UUID getId() {
      return this.id;
   }

   public Component getName() {
      return this.name;
   }

   public void setName(Component p_18856_) {
      this.name = p_18856_;
   }

   public float getProgress() {
      return this.progress;
   }

   public void setProgress(float p_146639_) {
      this.progress = p_146639_;
   }

   public BossEvent.BossBarColor getColor() {
      return this.color;
   }

   public void setColor(BossEvent.BossBarColor p_18854_) {
      this.color = p_18854_;
   }

   public BossEvent.BossBarOverlay getOverlay() {
      return this.overlay;
   }

   public void setOverlay(BossEvent.BossBarOverlay p_18855_) {
      this.overlay = p_18855_;
   }

   public boolean shouldDarkenScreen() {
      return this.darkenScreen;
   }

   public BossEvent setDarkenScreen(boolean p_18857_) {
      this.darkenScreen = p_18857_;
      return this;
   }

   public boolean shouldPlayBossMusic() {
      return this.playBossMusic;
   }

   public BossEvent setPlayBossMusic(boolean p_18858_) {
      this.playBossMusic = p_18858_;
      return this;
   }

   public BossEvent setCreateWorldFog(boolean p_18859_) {
      this.createWorldFog = p_18859_;
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

      BossBarColor(final String p_18881_, final ChatFormatting p_18882_) {
         this.name = p_18881_;
         this.formatting = p_18882_;
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

      BossBarOverlay(final String p_18901_) {
         this.name = p_18901_;
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
