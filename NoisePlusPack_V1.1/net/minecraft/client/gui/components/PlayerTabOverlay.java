package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class PlayerTabOverlay {
   private static final Identifier PING_UNKNOWN_SPRITE = Identifier.withDefaultNamespace("icon/ping_unknown");
   private static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("icon/ping_1");
   private static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("icon/ping_2");
   private static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("icon/ping_3");
   private static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("icon/ping_4");
   private static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("icon/ping_5");
   private static final Identifier HEART_CONTAINER_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/container_blinking");
   private static final Identifier HEART_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/container");
   private static final Identifier HEART_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/full_blinking");
   private static final Identifier HEART_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/half_blinking");
   private static final Identifier HEART_ABSORBING_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking");
   private static final Identifier HEART_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
   private static final Identifier HEART_ABSORBING_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking");
   private static final Identifier HEART_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/half");
   private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(p_357670_ -> -p_357670_.getTabListOrder())
      .thenComparingInt(p_253306_ -> p_253306_.getGameMode() == GameType.SPECTATOR ? 1 : 0)
      .thenComparing(p_269613_ -> Optionull.mapOrDefault(p_269613_.getTeam(), PlayerTeam::getName, ""))
      .thenComparing(p_420708_ -> p_420708_.getProfile().name(), String::compareToIgnoreCase);
   public static final int MAX_ROWS_PER_COL = 20;
   private final Minecraft minecraft;
   private final Gui gui;
   private @Nullable Component footer;
   private @Nullable Component header;
   private boolean visible;
   private final Map<UUID, PlayerTabOverlay.HealthState> healthStates = new Object2ObjectOpenHashMap();

   public PlayerTabOverlay(Minecraft p_94527_, Gui p_94528_) {
      this.minecraft = p_94527_;
      this.gui = p_94528_;
   }

   public Component getNameForDisplay(PlayerInfo p_94550_) {
      return p_94550_.getTabListDisplayName() != null
         ? this.decorateName(p_94550_, p_94550_.getTabListDisplayName().copy())
         : this.decorateName(p_94550_, PlayerTeam.formatNameForTeam(p_94550_.getTeam(), Component.literal(p_94550_.getProfile().name())));
   }

   private Component decorateName(PlayerInfo p_94552_, MutableComponent p_94553_) {
      return p_94552_.getGameMode() == GameType.SPECTATOR ? p_94553_.withStyle(ChatFormatting.ITALIC) : p_94553_;
   }

   public void setVisible(boolean p_94557_) {
      if (this.visible != p_94557_) {
         this.healthStates.clear();
         this.visible = p_94557_;
         if (p_94557_) {
            Component component = ComponentUtils.formatList(this.getPlayerInfos(), Component.literal(", "), this::getNameForDisplay);
            this.minecraft.getNarrator().saySystemNow(Component.translatable("multiplayer.player.list.narration", component));
         }
      }
   }

   private List<PlayerInfo> getPlayerInfos() {
      return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
   }

   public void render(GuiGraphics p_281484_, int p_283602_, Scoreboard p_282338_, @Nullable Objective p_282369_) {
      List<PlayerInfo> list = this.getPlayerInfos();
      List<PlayerTabOverlay.ScoreDisplayEntry> list1 = new ArrayList<>(list.size());
      int i = this.minecraft.font.width(" ");
      int j = 0;
      int k = 0;

      for (PlayerInfo playerinfo : list) {
         Component component = this.getNameForDisplay(playerinfo);
         j = Math.max(j, this.minecraft.font.width(component));
         int l = 0;
         Component component1 = null;
         int i1 = 0;
         if (p_282369_ != null) {
            ScoreHolder scoreholder = ScoreHolder.fromGameProfile(playerinfo.getProfile());
            ReadOnlyScoreInfo readonlyscoreinfo = p_282338_.getPlayerScoreInfo(scoreholder, p_282369_);
            if (readonlyscoreinfo != null) {
               l = readonlyscoreinfo.value();
            }

            if (p_282369_.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
               NumberFormat numberformat = p_282369_.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
               component1 = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, numberformat);
               i1 = this.minecraft.font.width(component1);
               k = Math.max(k, i1 > 0 ? i + i1 : 0);
            }
         }

         list1.add(new PlayerTabOverlay.ScoreDisplayEntry(component, l, component1, i1));
      }

      if (!this.healthStates.isEmpty()) {
         Set<UUID> set = list.stream().map(p_420707_ -> p_420707_.getProfile().id()).collect(Collectors.toSet());
         this.healthStates.keySet().removeIf(p_248583_ -> !set.contains(p_248583_));
      }

      int j2 = list.size();
      int k2 = j2;

      int l2;
      for (l2 = 1; k2 > 20; k2 = (j2 + l2 - 1) / l2) {
         l2++;
      }

      boolean flag1 = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
      int i3;
      if (p_282369_ != null) {
         if (p_282369_.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            i3 = 90;
         } else {
            i3 = k;
         }
      } else {
         i3 = 0;
      }

      int j3 = Math.min(l2 * ((flag1 ? 9 : 0) + j + i3 + 13), p_283602_ - 50) / l2;
      int k3 = p_283602_ / 2 - (j3 * l2 + (l2 - 1) * 5) / 2;
      int l3 = 10;
      int i4 = j3 * l2 + (l2 - 1) * 5;
      List<FormattedCharSequence> list2 = null;
      if (this.header != null) {
         list2 = this.minecraft.font.split(this.header, p_283602_ - 50);

         for (FormattedCharSequence formattedcharsequence : list2) {
            i4 = Math.max(i4, this.minecraft.font.width(formattedcharsequence));
         }
      }

      List<FormattedCharSequence> list3 = null;
      if (this.footer != null) {
         list3 = this.minecraft.font.split(this.footer, p_283602_ - 50);

         for (FormattedCharSequence formattedcharsequence1 : list3) {
            i4 = Math.max(i4, this.minecraft.font.width(formattedcharsequence1));
         }
      }

      if (list2 != null) {
         p_281484_.fill(p_283602_ / 2 - i4 / 2 - 1, l3 - 1, p_283602_ / 2 + i4 / 2 + 1, l3 + list2.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence formattedcharsequence2 : list2) {
            int j1 = this.minecraft.font.width(formattedcharsequence2);
            p_281484_.drawString(this.minecraft.font, formattedcharsequence2, p_283602_ / 2 - j1 / 2, l3, -1);
            l3 += 9;
         }

         l3++;
      }

      p_281484_.fill(p_283602_ / 2 - i4 / 2 - 1, l3 - 1, p_283602_ / 2 + i4 / 2 + 1, l3 + k2 * 9, Integer.MIN_VALUE);
      int j4 = this.minecraft.options.getBackgroundColor(553648127);

      for (int k4 = 0; k4 < j2; k4++) {
         int l4 = k4 / k2;
         int k1 = k4 % k2;
         int l1 = k3 + l4 * j3 + l4 * 5;
         int i2 = l3 + k1 * 9;
         p_281484_.fill(l1, i2, l1 + j3, i2 + 8, j4);
         if (k4 < list.size()) {
            PlayerInfo playerinfo1 = list.get(k4);
            PlayerTabOverlay.ScoreDisplayEntry playertaboverlay$scoredisplayentry = list1.get(k4);
            GameProfile gameprofile = playerinfo1.getProfile();
            if (flag1) {
               Player player = this.minecraft.level.getPlayerByUUID(gameprofile.id());
               boolean flag = player != null && AvatarRenderer.isPlayerUpsideDown(player);
               PlayerFaceRenderer.draw(p_281484_, playerinfo1.getSkin().body().texturePath(), l1, i2, 8, playerinfo1.showHat(), flag, -1);
               l1 += 9;
            }

            p_281484_.drawString(
               this.minecraft.font, playertaboverlay$scoredisplayentry.name, l1, i2, playerinfo1.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1
            );
            if (p_282369_ != null && playerinfo1.getGameMode() != GameType.SPECTATOR) {
               int j5 = l1 + j + 1;
               int k5 = j5 + i3;
               if (k5 - j5 > 5) {
                  this.renderTablistScore(p_282369_, i2, playertaboverlay$scoredisplayentry, j5, k5, gameprofile.id(), p_281484_);
               }
            }

            this.renderPingIcon(p_281484_, j3, l1 - (flag1 ? 9 : 0), i2, playerinfo1);
         }
      }

      if (list3 != null) {
         l3 += k2 * 9 + 1;
         p_281484_.fill(p_283602_ / 2 - i4 / 2 - 1, l3 - 1, p_283602_ / 2 + i4 / 2 + 1, l3 + list3.size() * 9, Integer.MIN_VALUE);

         for (FormattedCharSequence formattedcharsequence3 : list3) {
            int i5 = this.minecraft.font.width(formattedcharsequence3);
            p_281484_.drawString(this.minecraft.font, formattedcharsequence3, p_283602_ / 2 - i5 / 2, l3, -1);
            l3 += 9;
         }
      }
   }

   protected void renderPingIcon(GuiGraphics p_283286_, int p_281809_, int p_282801_, int p_282223_, PlayerInfo p_282986_) {
      Identifier identifier;
      if (p_282986_.getLatency() < 0) {
         identifier = PING_UNKNOWN_SPRITE;
      } else if (p_282986_.getLatency() < 150) {
         identifier = PING_5_SPRITE;
      } else if (p_282986_.getLatency() < 300) {
         identifier = PING_4_SPRITE;
      } else if (p_282986_.getLatency() < 600) {
         identifier = PING_3_SPRITE;
      } else if (p_282986_.getLatency() < 1000) {
         identifier = PING_2_SPRITE;
      } else {
         identifier = PING_1_SPRITE;
      }

      p_283286_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, p_282801_ + p_281809_ - 11, p_282223_, 10, 8);
   }

   private void renderTablistScore(
      Objective p_283381_, int p_282557_, PlayerTabOverlay.ScoreDisplayEntry p_312058_, int p_283533_, int p_281254_, UUID p_283099_, GuiGraphics p_282280_
   ) {
      if (p_283381_.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
         this.renderTablistHearts(p_282557_, p_283533_, p_281254_, p_283099_, p_282280_, p_312058_.score);
      } else if (p_312058_.formattedScore != null) {
         p_282280_.drawString(this.minecraft.font, p_312058_.formattedScore, p_281254_ - p_312058_.scoreWidth, p_282557_, -1);
      }
   }

   private void renderTablistHearts(int p_282904_, int p_283173_, int p_282149_, UUID p_283348_, GuiGraphics p_281723_, int p_281354_) {
      PlayerTabOverlay.HealthState playertaboverlay$healthstate = this.healthStates
         .computeIfAbsent(p_283348_, p_249546_ -> new PlayerTabOverlay.HealthState(p_281354_));
      playertaboverlay$healthstate.update(p_281354_, this.gui.getGuiTicks());
      int i = Mth.positiveCeilDiv(Math.max(p_281354_, playertaboverlay$healthstate.displayedValue()), 2);
      int j = Math.max(p_281354_, Math.max(playertaboverlay$healthstate.displayedValue(), 20)) / 2;
      boolean flag = playertaboverlay$healthstate.isBlinking(this.gui.getGuiTicks());
      if (i > 0) {
         int k = Mth.floor(Math.min((float)(p_282149_ - p_283173_ - 4) / j, 9.0F));
         if (k <= 3) {
            float f1 = Mth.clamp(p_281354_ / 20.0F, 0.0F, 1.0F);
            int j1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
            float f = p_281354_ / 2.0F;
            Component component = Component.translatable("multiplayer.player.list.hp", f);
            Component component1;
            if (p_282149_ - this.minecraft.font.width(component) >= p_283173_) {
               component1 = component;
            } else {
               component1 = Component.literal(Float.toString(f));
            }

            p_281723_.drawString(
               this.minecraft.font, component1, (p_282149_ + p_283173_ - this.minecraft.font.width(component1)) / 2, p_282904_, ARGB.opaque(j1)
            );
         } else {
            Identifier identifier = flag ? HEART_CONTAINER_BLINKING_SPRITE : HEART_CONTAINER_SPRITE;

            for (int l = i; l < j; l++) {
               p_281723_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, p_283173_ + l * k, p_282904_, 9, 9);
            }

            for (int i1 = 0; i1 < i; i1++) {
               p_281723_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, p_283173_ + i1 * k, p_282904_, 9, 9);
               if (flag) {
                  if (i1 * 2 + 1 < playertaboverlay$healthstate.displayedValue()) {
                     p_281723_.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL_BLINKING_SPRITE, p_283173_ + i1 * k, p_282904_, 9, 9);
                  }

                  if (i1 * 2 + 1 == playertaboverlay$healthstate.displayedValue()) {
                     p_281723_.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF_BLINKING_SPRITE, p_283173_ + i1 * k, p_282904_, 9, 9);
                  }
               }

               if (i1 * 2 + 1 < p_281354_) {
                  p_281723_.blitSprite(
                     RenderPipelines.GUI_TEXTURED, i1 >= 10 ? HEART_ABSORBING_FULL_BLINKING_SPRITE : HEART_FULL_SPRITE, p_283173_ + i1 * k, p_282904_, 9, 9
                  );
               }

               if (i1 * 2 + 1 == p_281354_) {
                  p_281723_.blitSprite(
                     RenderPipelines.GUI_TEXTURED, i1 >= 10 ? HEART_ABSORBING_HALF_BLINKING_SPRITE : HEART_HALF_SPRITE, p_283173_ + i1 * k, p_282904_, 9, 9
                  );
               }
            }
         }
      }
   }

   public void setFooter(@Nullable Component p_94555_) {
      this.footer = p_94555_;
   }

   public void setHeader(@Nullable Component p_94559_) {
      this.header = p_94559_;
   }

   public void reset() {
      this.header = null;
      this.footer = null;
   }

   @OnlyIn(Dist.CLIENT)
   static class HealthState {
      private static final long DISPLAY_UPDATE_DELAY = 20L;
      private static final long DECREASE_BLINK_DURATION = 20L;
      private static final long INCREASE_BLINK_DURATION = 10L;
      private int lastValue;
      private int displayedValue;
      private long lastUpdateTick;
      private long blinkUntilTick;

      public HealthState(int p_250562_) {
         this.displayedValue = p_250562_;
         this.lastValue = p_250562_;
      }

      public void update(int p_251066_, long p_251460_) {
         if (p_251066_ != this.lastValue) {
            long i = p_251066_ < this.lastValue ? 20L : 10L;
            this.blinkUntilTick = p_251460_ + i;
            this.lastValue = p_251066_;
            this.lastUpdateTick = p_251460_;
         }

         if (p_251460_ - this.lastUpdateTick > 20L) {
            this.displayedValue = p_251066_;
         }
      }

      public int displayedValue() {
         return this.displayedValue;
      }

      public boolean isBlinking(long p_251847_) {
         return this.blinkUntilTick > p_251847_ && (this.blinkUntilTick - p_251847_) % 6L >= 3L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record ScoreDisplayEntry(Component name, int score, @Nullable Component formattedScore, int scoreWidth) {
   }
}
