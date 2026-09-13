package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private static final int DEFAULT_MAX = 100;
   private final Identifier id;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;

   public CustomBossEvent(Identifier p_452936_, Component p_136262_) {
      super(p_136262_, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.id = p_452936_;
      this.setProgress(0.0F);
   }

   public Identifier getTextId() {
      return this.id;
   }

   @Override
   public void addPlayer(ServerPlayer p_136267_) {
      super.addPlayer(p_136267_);
      this.players.add(p_136267_.getUUID());
   }

   public void addOfflinePlayer(UUID p_136271_) {
      this.players.add(p_136271_);
   }

   @Override
   public void removePlayer(ServerPlayer p_136281_) {
      super.removePlayer(p_136281_);
      this.players.remove(p_136281_.getUUID());
   }

   @Override
   public void removeAllPlayers() {
      super.removeAllPlayers();
      this.players.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMax() {
      return this.max;
   }

   public void setValue(int p_136265_) {
      this.value = p_136265_;
      this.setProgress(Mth.clamp((float)p_136265_ / this.max, 0.0F, 1.0F));
   }

   public void setMax(int p_136279_) {
      this.max = p_136279_;
      this.setProgress(Mth.clamp((float)this.value / p_136279_, 0.0F, 1.0F));
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName())
         .withStyle(
            p_448863_ -> p_448863_.withColor(this.getColor().getFormatting())
               .withHoverEvent(new HoverEvent.ShowText(Component.literal(this.getTextId().toString())))
               .withInsertion(this.getTextId().toString())
         );
   }

   public boolean setPlayers(Collection<ServerPlayer> p_136269_) {
      Set<UUID> set = Sets.newHashSet();
      Set<ServerPlayer> set1 = Sets.newHashSet();

      for (UUID uuid : this.players) {
         boolean flag = false;

         for (ServerPlayer serverplayer : p_136269_) {
            if (serverplayer.getUUID().equals(uuid)) {
               flag = true;
               break;
            }
         }

         if (!flag) {
            set.add(uuid);
         }
      }

      for (ServerPlayer serverplayer1 : p_136269_) {
         boolean flag1 = false;

         for (UUID uuid2 : this.players) {
            if (serverplayer1.getUUID().equals(uuid2)) {
               flag1 = true;
               break;
            }
         }

         if (!flag1) {
            set1.add(serverplayer1);
         }
      }

      for (UUID uuid1 : set) {
         for (ServerPlayer serverplayer3 : this.getPlayers()) {
            if (serverplayer3.getUUID().equals(uuid1)) {
               this.removePlayer(serverplayer3);
               break;
            }
         }

         this.players.remove(uuid1);
      }

      for (ServerPlayer serverplayer2 : set1) {
         this.addPlayer(serverplayer2);
      }

      return !set.isEmpty() || !set1.isEmpty();
   }

   public static CustomBossEvent load(Identifier p_453719_, CustomBossEvent.Packed p_392681_) {
      CustomBossEvent custombossevent = new CustomBossEvent(p_453719_, p_392681_.name);
      custombossevent.setVisible(p_392681_.visible);
      custombossevent.setValue(p_392681_.value);
      custombossevent.setMax(p_392681_.max);
      custombossevent.setColor(p_392681_.color);
      custombossevent.setOverlay(p_392681_.overlay);
      custombossevent.setDarkenScreen(p_392681_.darkenScreen);
      custombossevent.setPlayBossMusic(p_392681_.playBossMusic);
      custombossevent.setCreateWorldFog(p_392681_.createWorldFog);
      p_392681_.players.forEach(custombossevent::addOfflinePlayer);
      return custombossevent;
   }

   public CustomBossEvent.Packed pack() {
      return new CustomBossEvent.Packed(
         this.getName(),
         this.isVisible(),
         this.getValue(),
         this.getMax(),
         this.getColor(),
         this.getOverlay(),
         this.shouldDarkenScreen(),
         this.shouldPlayBossMusic(),
         this.shouldCreateWorldFog(),
         Set.copyOf(this.players)
      );
   }

   public void onPlayerConnect(ServerPlayer p_136284_) {
      if (this.players.contains(p_136284_.getUUID())) {
         this.addPlayer(p_136284_);
      }
   }

   public void onPlayerDisconnect(ServerPlayer p_136287_) {
      super.removePlayer(p_136287_);
   }

   public record Packed(
      Component name,
      boolean visible,
      int value,
      int max,
      BossEvent.BossBarColor color,
      BossEvent.BossBarOverlay overlay,
      boolean darkenScreen,
      boolean playBossMusic,
      boolean createWorldFog,
      Set<UUID> players
   ) {
      public static final Codec<CustomBossEvent.Packed> CODEC = RecordCodecBuilder.create(
         p_397740_ -> p_397740_.group(
               ComponentSerialization.CODEC.fieldOf("Name").forGetter(CustomBossEvent.Packed::name),
               Codec.BOOL.optionalFieldOf("Visible", false).forGetter(CustomBossEvent.Packed::visible),
               Codec.INT.optionalFieldOf("Value", 0).forGetter(CustomBossEvent.Packed::value),
               Codec.INT.optionalFieldOf("Max", 100).forGetter(CustomBossEvent.Packed::max),
               BossEvent.BossBarColor.CODEC.optionalFieldOf("Color", BossEvent.BossBarColor.WHITE).forGetter(CustomBossEvent.Packed::color),
               BossEvent.BossBarOverlay.CODEC.optionalFieldOf("Overlay", BossEvent.BossBarOverlay.PROGRESS).forGetter(CustomBossEvent.Packed::overlay),
               Codec.BOOL.optionalFieldOf("DarkenScreen", false).forGetter(CustomBossEvent.Packed::darkenScreen),
               Codec.BOOL.optionalFieldOf("PlayBossMusic", false).forGetter(CustomBossEvent.Packed::playBossMusic),
               Codec.BOOL.optionalFieldOf("CreateWorldFog", false).forGetter(CustomBossEvent.Packed::createWorldFog),
               UUIDUtil.CODEC_SET.optionalFieldOf("Players", Set.of()).forGetter(CustomBossEvent.Packed::players)
            )
            .apply(p_397740_, CustomBossEvent.Packed::new)
      );
   }
}
