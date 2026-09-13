package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;

public class PlayerTeam extends Team {
   private static final int BIT_FRIENDLY_FIRE = 0;
   private static final int BIT_SEE_INVISIBLES = 1;
   private final Scoreboard scoreboard;
   private final String name;
   private final Set<String> players = Sets.newHashSet();
   private Component displayName;
   private Component playerPrefix = CommonComponents.EMPTY;
   private Component playerSuffix = CommonComponents.EMPTY;
   private boolean allowFriendlyFire = true;
   private boolean seeFriendlyInvisibles = true;
   private Team.Visibility nameTagVisibility = Team.Visibility.ALWAYS;
   private Team.Visibility deathMessageVisibility = Team.Visibility.ALWAYS;
   private ChatFormatting color = ChatFormatting.RESET;
   private Team.CollisionRule collisionRule = Team.CollisionRule.ALWAYS;
   private final Style displayNameStyle;

   public PlayerTeam(Scoreboard p_83340_, String p_83341_) {
      this.scoreboard = p_83340_;
      this.name = p_83341_;
      this.displayName = Component.literal(p_83341_);
      this.displayNameStyle = Style.EMPTY.withInsertion(p_83341_).withHoverEvent(new HoverEvent.ShowText(Component.literal(p_83341_)));
   }

   public PlayerTeam.Packed pack() {
      return new PlayerTeam.Packed(
         this.name,
         Optional.of(this.displayName),
         this.color != ChatFormatting.RESET ? Optional.of(this.color) : Optional.empty(),
         this.allowFriendlyFire,
         this.seeFriendlyInvisibles,
         this.playerPrefix,
         this.playerSuffix,
         this.nameTagVisibility,
         this.deathMessageVisibility,
         this.collisionRule,
         List.copyOf(this.players)
      );
   }

   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public MutableComponent getFormattedDisplayName() {
      MutableComponent mutablecomponent = ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle(this.displayNameStyle));
      ChatFormatting chatformatting = this.getColor();
      if (chatformatting != ChatFormatting.RESET) {
         mutablecomponent.withStyle(chatformatting);
      }

      return mutablecomponent;
   }

   public void setDisplayName(Component p_83354_) {
      if (p_83354_ == null) {
         throw new IllegalArgumentException("Name cannot be null");
      }

      this.displayName = p_83354_;
      this.scoreboard.onTeamChanged(this);
   }

   public void setPlayerPrefix(@Nullable Component p_83361_) {
      this.playerPrefix = p_83361_ == null ? CommonComponents.EMPTY : p_83361_;
      this.scoreboard.onTeamChanged(this);
   }

   public Component getPlayerPrefix() {
      return this.playerPrefix;
   }

   public void setPlayerSuffix(@Nullable Component p_83366_) {
      this.playerSuffix = p_83366_ == null ? CommonComponents.EMPTY : p_83366_;
      this.scoreboard.onTeamChanged(this);
   }

   public Component getPlayerSuffix() {
      return this.playerSuffix;
   }

   @Override
   public Collection<String> getPlayers() {
      return this.players;
   }

   @Override
   public MutableComponent getFormattedName(Component p_83369_) {
      MutableComponent mutablecomponent = Component.empty().append(this.playerPrefix).append(p_83369_).append(this.playerSuffix);
      ChatFormatting chatformatting = this.getColor();
      if (chatformatting != ChatFormatting.RESET) {
         mutablecomponent.withStyle(chatformatting);
      }

      return mutablecomponent;
   }

   public static MutableComponent formatNameForTeam(@Nullable Team p_83349_, Component p_83350_) {
      return p_83349_ == null ? p_83350_.copy() : p_83349_.getFormattedName(p_83350_);
   }

   @Override
   public boolean isAllowFriendlyFire() {
      return this.allowFriendlyFire;
   }

   public void setAllowFriendlyFire(boolean p_83356_) {
      this.allowFriendlyFire = p_83356_;
      this.scoreboard.onTeamChanged(this);
   }

   @Override
   public boolean canSeeFriendlyInvisibles() {
      return this.seeFriendlyInvisibles;
   }

   public void setSeeFriendlyInvisibles(boolean p_83363_) {
      this.seeFriendlyInvisibles = p_83363_;
      this.scoreboard.onTeamChanged(this);
   }

   @Override
   public Team.Visibility getNameTagVisibility() {
      return this.nameTagVisibility;
   }

   @Override
   public Team.Visibility getDeathMessageVisibility() {
      return this.deathMessageVisibility;
   }

   public void setNameTagVisibility(Team.Visibility p_83347_) {
      this.nameTagVisibility = p_83347_;
      this.scoreboard.onTeamChanged(this);
   }

   public void setDeathMessageVisibility(Team.Visibility p_83359_) {
      this.deathMessageVisibility = p_83359_;
      this.scoreboard.onTeamChanged(this);
   }

   @Override
   public Team.CollisionRule getCollisionRule() {
      return this.collisionRule;
   }

   public void setCollisionRule(Team.CollisionRule p_83345_) {
      this.collisionRule = p_83345_;
      this.scoreboard.onTeamChanged(this);
   }

   public int packOptions() {
      int i = 0;
      if (this.isAllowFriendlyFire()) {
         i |= 1;
      }

      if (this.canSeeFriendlyInvisibles()) {
         i |= 2;
      }

      return i;
   }

   public void unpackOptions(int p_83343_) {
      this.setAllowFriendlyFire((p_83343_ & 1) > 0);
      this.setSeeFriendlyInvisibles((p_83343_ & 2) > 0);
   }

   public void setColor(ChatFormatting p_83352_) {
      this.color = p_83352_;
      this.scoreboard.onTeamChanged(this);
   }

   @Override
   public ChatFormatting getColor() {
      return this.color;
   }

   public record Packed(
      String name,
      Optional<Component> displayName,
      Optional<ChatFormatting> color,
      boolean allowFriendlyFire,
      boolean seeFriendlyInvisibles,
      Component memberNamePrefix,
      Component memberNameSuffix,
      Team.Visibility nameTagVisibility,
      Team.Visibility deathMessageVisibility,
      Team.CollisionRule collisionRule,
      List<String> players
   ) {
      public static final Codec<PlayerTeam.Packed> CODEC = RecordCodecBuilder.create(
         p_391724_ -> p_391724_.group(
               Codec.STRING.fieldOf("Name").forGetter(PlayerTeam.Packed::name),
               ComponentSerialization.CODEC.optionalFieldOf("DisplayName").forGetter(PlayerTeam.Packed::displayName),
               ChatFormatting.COLOR_CODEC.optionalFieldOf("TeamColor").forGetter(PlayerTeam.Packed::color),
               Codec.BOOL.optionalFieldOf("AllowFriendlyFire", true).forGetter(PlayerTeam.Packed::allowFriendlyFire),
               Codec.BOOL.optionalFieldOf("SeeFriendlyInvisibles", true).forGetter(PlayerTeam.Packed::seeFriendlyInvisibles),
               ComponentSerialization.CODEC.optionalFieldOf("MemberNamePrefix", CommonComponents.EMPTY).forGetter(PlayerTeam.Packed::memberNamePrefix),
               ComponentSerialization.CODEC.optionalFieldOf("MemberNameSuffix", CommonComponents.EMPTY).forGetter(PlayerTeam.Packed::memberNameSuffix),
               Team.Visibility.CODEC.optionalFieldOf("NameTagVisibility", Team.Visibility.ALWAYS).forGetter(PlayerTeam.Packed::nameTagVisibility),
               Team.Visibility.CODEC.optionalFieldOf("DeathMessageVisibility", Team.Visibility.ALWAYS).forGetter(PlayerTeam.Packed::deathMessageVisibility),
               Team.CollisionRule.CODEC.optionalFieldOf("CollisionRule", Team.CollisionRule.ALWAYS).forGetter(PlayerTeam.Packed::collisionRule),
               Codec.STRING.listOf().optionalFieldOf("Players", List.of()).forGetter(PlayerTeam.Packed::players)
            )
            .apply(p_391724_, PlayerTeam.Packed::new)
      );
   }
}
