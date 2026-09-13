package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Scoreboard {
   public static final String HIDDEN_SCORE_PREFIX = "#";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2ObjectMap<String, Objective> objectivesByName = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Reference2ObjectMap<ObjectiveCriteria, List<Objective>> objectivesByCriteria = new Reference2ObjectOpenHashMap();
   private final Map<String, PlayerScores> playerScores = new Object2ObjectOpenHashMap(16, 0.5F);
   private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
   private final Object2ObjectMap<String, PlayerTeam> teamsByName = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<String, PlayerTeam> teamsByPlayer = new Object2ObjectOpenHashMap();

   public @Nullable Objective getObjective(@Nullable String p_83478_) {
      return (Objective)this.objectivesByName.get(p_83478_);
   }

   public Objective addObjective(
      String p_83437_,
      ObjectiveCriteria p_83438_,
      Component p_83439_,
      ObjectiveCriteria.RenderType p_83440_,
      boolean p_311367_,
      @Nullable NumberFormat p_311959_
   ) {
      if (this.objectivesByName.containsKey(p_83437_)) {
         throw new IllegalArgumentException("An objective with the name '" + p_83437_ + "' already exists!");
      }

      Objective objective = new Objective(this, p_83437_, p_83438_, p_83439_, p_83440_, p_311367_, p_311959_);
      ((List)this.objectivesByCriteria.computeIfAbsent(p_83438_, p_310953_ -> Lists.newArrayList())).add(objective);
      this.objectivesByName.put(p_83437_, objective);
      this.onObjectiveAdded(objective);
      return objective;
   }

   public final void forAllObjectives(ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
      ((List)this.objectivesByCriteria.getOrDefault(p_83428_, Collections.emptyList()))
         .forEach(p_309370_ -> p_83430_.accept(this.getOrCreatePlayerScore(p_310719_, p_309370_, true)));
   }

   private PlayerScores getOrCreatePlayerInfo(String p_311117_) {
      return this.playerScores.computeIfAbsent(p_311117_, p_309376_ -> new PlayerScores());
   }

   public ScoreAccess getOrCreatePlayerScore(ScoreHolder p_309688_, Objective p_83473_) {
      return this.getOrCreatePlayerScore(p_309688_, p_83473_, false);
   }

   public ScoreAccess getOrCreatePlayerScore(final ScoreHolder p_310827_, final Objective p_312875_, boolean p_310024_) {
      final boolean flag = p_310024_ || !p_312875_.getCriteria().isReadOnly();
      PlayerScores playerscores = this.getOrCreatePlayerInfo(p_310827_.getScoreboardName());
      final MutableBoolean mutableboolean = new MutableBoolean();
      final Score score = playerscores.getOrCreate(p_312875_, p_309375_ -> mutableboolean.setTrue());
      return new ScoreAccess() {
         @Override
         public int get() {
            return score.value();
         }

         @Override
         public void set(int p_312858_) {
            if (!flag) {
               throw new IllegalStateException("Cannot modify read-only score");
            }

            boolean flag1 = mutableboolean.isTrue();
            if (p_312875_.displayAutoUpdate()) {
               Component component = p_310827_.getDisplayName();
               if (component != null && !component.equals(score.display())) {
                  score.display(component);
                  flag1 = true;
               }
            }

            if (p_312858_ != score.value()) {
               score.value(p_312858_);
               flag1 = true;
            }

            if (flag1) {
               this.sendScoreToPlayers();
            }
         }

         @Override
         public @Nullable Component display() {
            return score.display();
         }

         @Override
         public void display(@Nullable Component p_309551_) {
            if (mutableboolean.isTrue() || !Objects.equals(p_309551_, score.display())) {
               score.display(p_309551_);
               this.sendScoreToPlayers();
            }
         }

         @Override
         public void numberFormatOverride(@Nullable NumberFormat p_312257_) {
            score.numberFormat(p_312257_);
            this.sendScoreToPlayers();
         }

         @Override
         public boolean locked() {
            return score.isLocked();
         }

         @Override
         public void unlock() {
            this.setLocked(false);
         }

         @Override
         public void lock() {
            this.setLocked(true);
         }

         private void setLocked(boolean p_311228_) {
            score.setLocked(p_311228_);
            if (mutableboolean.isTrue()) {
               this.sendScoreToPlayers();
            }

            Scoreboard.this.onScoreLockChanged(p_310827_, p_312875_);
         }

         private void sendScoreToPlayers() {
            Scoreboard.this.onScoreChanged(p_310827_, p_312875_, score);
            mutableboolean.setFalse();
         }
      };
   }

   public @Nullable ReadOnlyScoreInfo getPlayerScoreInfo(ScoreHolder p_309394_, Objective p_310266_) {
      PlayerScores playerscores = this.playerScores.get(p_309394_.getScoreboardName());
      return playerscores != null ? playerscores.get(p_310266_) : null;
   }

   public Collection<PlayerScoreEntry> listPlayerScores(Objective p_312530_) {
      List<PlayerScoreEntry> list = new ArrayList<>();
      this.playerScores.forEach((p_309362_, p_309363_) -> {
         Score score = p_309363_.get(p_312530_);
         if (score != null) {
            list.add(new PlayerScoreEntry(p_309362_, score.value(), score.display(), score.numberFormat()));
         }
      });
      return list;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<ScoreHolder> getTrackedPlayers() {
      return this.playerScores.keySet().stream().map(ScoreHolder::forNameOnly).toList();
   }

   public void resetAllPlayerScores(ScoreHolder p_311535_) {
      PlayerScores playerscores = this.playerScores.remove(p_311535_.getScoreboardName());
      if (playerscores != null) {
         this.onPlayerRemoved(p_311535_);
      }
   }

   public void resetSinglePlayerScore(ScoreHolder p_312886_, Objective p_311508_) {
      PlayerScores playerscores = this.playerScores.get(p_312886_.getScoreboardName());
      if (playerscores != null) {
         boolean flag = playerscores.remove(p_311508_);
         if (!playerscores.hasScores()) {
            PlayerScores playerscores1 = this.playerScores.remove(p_312886_.getScoreboardName());
            if (playerscores1 != null) {
               this.onPlayerRemoved(p_312886_);
            }
         } else if (flag) {
            this.onPlayerScoreRemoved(p_312886_, p_311508_);
         }
      }
   }

   public Object2IntMap<Objective> listPlayerScores(ScoreHolder p_312742_) {
      PlayerScores playerscores = this.playerScores.get(p_312742_.getScoreboardName());
      return playerscores != null ? playerscores.listScores() : Object2IntMaps.emptyMap();
   }

   public void removeObjective(Objective p_83503_) {
      this.objectivesByName.remove(p_83503_.getName());

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == p_83503_) {
            this.setDisplayObjective(displayslot, null);
         }
      }

      List<Objective> list = (List<Objective>)this.objectivesByCriteria.get(p_83503_.getCriteria());
      if (list != null) {
         list.remove(p_83503_);
      }

      for (PlayerScores playerscores : this.playerScores.values()) {
         playerscores.remove(p_83503_);
      }

      this.onObjectiveRemoved(p_83503_);
   }

   public void setDisplayObjective(DisplaySlot p_297926_, @Nullable Objective p_83419_) {
      this.displayObjectives.put(p_297926_, p_83419_);
   }

   public @Nullable Objective getDisplayObjective(DisplaySlot p_297931_) {
      return this.displayObjectives.get(p_297931_);
   }

   public @Nullable PlayerTeam getPlayerTeam(String p_83490_) {
      return (PlayerTeam)this.teamsByName.get(p_83490_);
   }

   public PlayerTeam addPlayerTeam(String p_83493_) {
      PlayerTeam playerteam = this.getPlayerTeam(p_83493_);
      if (playerteam != null) {
         LOGGER.warn("Requested creation of existing team '{}'", p_83493_);
         return playerteam;
      } else {
         playerteam = new PlayerTeam(this, p_83493_);
         this.teamsByName.put(p_83493_, playerteam);
         this.onTeamAdded(playerteam);
         return playerteam;
      }
   }

   public void removePlayerTeam(PlayerTeam p_83476_) {
      this.teamsByName.remove(p_83476_.getName());

      for (String s : p_83476_.getPlayers()) {
         this.teamsByPlayer.remove(s);
      }

      this.onTeamRemoved(p_83476_);
   }

   public boolean addPlayerToTeam(String p_83434_, PlayerTeam p_83435_) {
      if (this.getPlayersTeam(p_83434_) != null) {
         this.removePlayerFromTeam(p_83434_);
      }

      this.teamsByPlayer.put(p_83434_, p_83435_);
      return p_83435_.getPlayers().add(p_83434_);
   }

   public boolean removePlayerFromTeam(String p_83496_) {
      PlayerTeam playerteam = this.getPlayersTeam(p_83496_);
      if (playerteam != null) {
         this.removePlayerFromTeam(p_83496_, playerteam);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(String p_83464_, PlayerTeam p_83465_) {
      if (this.getPlayersTeam(p_83464_) != p_83465_) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + p_83465_.getName() + "'.");
      }

      this.teamsByPlayer.remove(p_83464_);
      p_83465_.getPlayers().remove(p_83464_);
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   public @Nullable PlayerTeam getPlayersTeam(String p_83501_) {
      return (PlayerTeam)this.teamsByPlayer.get(p_83501_);
   }

   public void onObjectiveAdded(Objective p_83422_) {
   }

   public void onObjectiveChanged(Objective p_83455_) {
   }

   public void onObjectiveRemoved(Objective p_83467_) {
   }

   protected void onScoreChanged(ScoreHolder p_312923_, Objective p_311972_, Score p_83424_) {
   }

   protected void onScoreLockChanged(ScoreHolder p_311114_, Objective p_309936_) {
   }

   public void onPlayerRemoved(ScoreHolder p_312272_) {
   }

   public void onPlayerScoreRemoved(ScoreHolder p_311030_, Objective p_83433_) {
   }

   public void onTeamAdded(PlayerTeam p_83423_) {
   }

   public void onTeamChanged(PlayerTeam p_83456_) {
   }

   public void onTeamRemoved(PlayerTeam p_83468_) {
   }

   public void entityRemoved(Entity p_83421_) {
      if (!(p_83421_ instanceof Player) && !p_83421_.isAlive()) {
         this.resetAllPlayerScores(p_83421_);
         this.removePlayerFromTeam(p_83421_.getScoreboardName());
      }
   }

   protected List<Scoreboard.PackedScore> packPlayerScores() {
      return this.playerScores
         .entrySet()
         .stream()
         .flatMap(
            p_391148_ -> {
               String s = p_391148_.getKey();
               return p_391148_.getValue()
                  .listRawScores()
                  .entrySet()
                  .stream()
                  .map(p_450135_ -> new Scoreboard.PackedScore(s, p_450135_.getKey().getName(), p_450135_.getValue().pack()));
            }
         )
         .toList();
   }

   protected void loadPlayerScore(Scoreboard.PackedScore p_392188_) {
      Objective objective = this.getObjective(p_392188_.objective);
      if (objective == null) {
         LOGGER.error("Unknown objective {} for name {}, ignoring", p_392188_.objective, p_392188_.owner);
      } else {
         this.getOrCreatePlayerInfo(p_392188_.owner).setScore(objective, new Score(p_392188_.score));
      }
   }

   protected List<PlayerTeam.Packed> packPlayerTeams() {
      return this.getPlayerTeams().stream().map(PlayerTeam::pack).toList();
   }

   protected void loadPlayerTeam(PlayerTeam.Packed p_394336_) {
      PlayerTeam playerteam = this.addPlayerTeam(p_394336_.name());
      p_394336_.displayName().ifPresent(playerteam::setDisplayName);
      p_394336_.color().ifPresent(playerteam::setColor);
      playerteam.setAllowFriendlyFire(p_394336_.allowFriendlyFire());
      playerteam.setSeeFriendlyInvisibles(p_394336_.seeFriendlyInvisibles());
      playerteam.setPlayerPrefix(p_394336_.memberNamePrefix());
      playerteam.setPlayerSuffix(p_394336_.memberNameSuffix());
      playerteam.setNameTagVisibility(p_394336_.nameTagVisibility());
      playerteam.setDeathMessageVisibility(p_394336_.deathMessageVisibility());
      playerteam.setCollisionRule(p_394336_.collisionRule());

      for (String s : p_394336_.players()) {
         this.addPlayerToTeam(s, playerteam);
      }
   }

   protected List<Objective.Packed> packObjectives() {
      return this.getObjectives().stream().map(Objective::pack).toList();
   }

   protected void loadObjective(Objective.Packed p_397493_) {
      this.addObjective(
         p_397493_.name(),
         p_397493_.criteria(),
         p_397493_.displayName(),
         p_397493_.renderType(),
         p_397493_.displayAutoUpdate(),
         p_397493_.numberFormat().orElse(null)
      );
   }

   protected Map<DisplaySlot, String> packDisplaySlots() {
      Map<DisplaySlot, String> map = new EnumMap<>(DisplaySlot.class);

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         Objective objective = this.getDisplayObjective(displayslot);
         if (objective != null) {
            map.put(displayslot, objective.getName());
         }
      }

      return map;
   }

   public record PackedScore(String owner, String objective, Score.Packed score) {
      public static final Codec<Scoreboard.PackedScore> CODEC = RecordCodecBuilder.create(
         p_450136_ -> p_450136_.group(
               Codec.STRING.fieldOf("Name").forGetter(Scoreboard.PackedScore::owner),
               Codec.STRING.fieldOf("Objective").forGetter(Scoreboard.PackedScore::objective),
               Score.Packed.MAP_CODEC.forGetter(Scoreboard.PackedScore::score)
            )
            .apply(p_450136_, Scoreboard.PackedScore::new)
      );
   }
}
