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

   public @Nullable Objective getObjective(final @Nullable String name) {
      return (Objective)this.objectivesByName.get(name);
   }

   public Objective addObjective(
      final String name,
      final ObjectiveCriteria criteria,
      final Component displayName,
      final ObjectiveCriteria.RenderType renderType,
      final boolean displayAutoUpdate,
      final @Nullable NumberFormat numberFormat
   ) {
      if (this.objectivesByName.containsKey(name)) {
         throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
      }

      Objective objective = new Objective(this, name, criteria, displayName, renderType, displayAutoUpdate, numberFormat);
      ((List)this.objectivesByCriteria.computeIfAbsent(criteria, k -> Lists.newArrayList())).add(objective);
      this.objectivesByName.put(name, objective);
      this.onObjectiveAdded(objective);
      return objective;
   }

   public final void forAllObjectives(final ObjectiveCriteria criteria, final ScoreHolder name, final Consumer<ScoreAccess> operation) {
      ((List)this.objectivesByCriteria.getOrDefault(criteria, Collections.emptyList()))
         .forEach(o -> operation.accept(this.getOrCreatePlayerScore(name, o, true)));
   }

   private PlayerScores getOrCreatePlayerInfo(final String name) {
      return this.playerScores.computeIfAbsent(name, k -> new PlayerScores());
   }

   public ScoreAccess getOrCreatePlayerScore(final ScoreHolder holder, final Objective objective) {
      return this.getOrCreatePlayerScore(holder, objective, false);
   }

   public ScoreAccess getOrCreatePlayerScore(final ScoreHolder scoreHolder, final Objective objective, final boolean forceWritable) {
      final boolean canModify = forceWritable || !objective.getCriteria().isReadOnly();
      PlayerScores playerScore = this.getOrCreatePlayerInfo(scoreHolder.getScoreboardName());
      final MutableBoolean requiresSync = new MutableBoolean();
      final Score score = playerScore.getOrCreate(objective, newScore -> requiresSync.setTrue());
      return new ScoreAccess() {
         @Override
         public int get() {
            return score.value();
         }

         @Override
         public void set(final int value) {
            if (!canModify) {
               throw new IllegalStateException("Cannot modify read-only score");
            }

            boolean hasChanged = requiresSync.isTrue();
            if (objective.displayAutoUpdate()) {
               Component newDisplay = scoreHolder.getDisplayName();
               if (newDisplay != null && !newDisplay.equals(score.display())) {
                  score.display(newDisplay);
                  hasChanged = true;
               }
            }

            if (value != score.value()) {
               score.value(value);
               hasChanged = true;
            }

            if (hasChanged) {
               this.sendScoreToPlayers();
            }
         }

         @Override
         public @Nullable Component display() {
            return score.display();
         }

         @Override
         public void display(final @Nullable Component display) {
            if (requiresSync.isTrue() || !Objects.equals(display, score.display())) {
               score.display(display);
               this.sendScoreToPlayers();
            }
         }

         @Override
         public void numberFormatOverride(final @Nullable NumberFormat numberFormat) {
            score.numberFormat(numberFormat);
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

         private void setLocked(final boolean locked) {
            score.setLocked(locked);
            if (requiresSync.isTrue()) {
               this.sendScoreToPlayers();
            }

            Scoreboard.this.onScoreLockChanged(scoreHolder, objective);
         }

         private void sendScoreToPlayers() {
            Scoreboard.this.onScoreChanged(scoreHolder, objective, score);
            requiresSync.setFalse();
         }
      };
   }

   public @Nullable ReadOnlyScoreInfo getPlayerScoreInfo(final ScoreHolder name, final Objective objective) {
      PlayerScores playerScore = this.playerScores.get(name.getScoreboardName());
      return playerScore != null ? playerScore.get(objective) : null;
   }

   public Collection<PlayerScoreEntry> listPlayerScores(final Objective objective) {
      List<PlayerScoreEntry> result = new ArrayList<>();
      this.playerScores.forEach((player, scores) -> {
         Score score = scores.get(objective);
         if (score != null) {
            result.add(new PlayerScoreEntry(player, score.value(), score.display(), score.numberFormat()));
         }
      });
      return result;
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

   public void resetAllPlayerScores(final ScoreHolder player) {
      PlayerScores removed = this.playerScores.remove(player.getScoreboardName());
      if (removed != null) {
         this.onPlayerRemoved(player);
      }
   }

   public void resetSinglePlayerScore(final ScoreHolder player, final Objective objective) {
      PlayerScores scores = this.playerScores.get(player.getScoreboardName());
      if (scores != null) {
         boolean hasRemoved = scores.remove(objective);
         if (!scores.hasScores()) {
            PlayerScores removedPlayer = this.playerScores.remove(player.getScoreboardName());
            if (removedPlayer != null) {
               this.onPlayerRemoved(player);
            }
         } else if (hasRemoved) {
            this.onPlayerScoreRemoved(player, objective);
         }
      }
   }

   public Object2IntMap<Objective> listPlayerScores(final ScoreHolder player) {
      PlayerScores scores = this.playerScores.get(player.getScoreboardName());
      return scores != null ? scores.listScores() : Object2IntMaps.emptyMap();
   }

   public void removeObjective(final Objective objective) {
      this.objectivesByName.remove(objective.getName());

      for (DisplaySlot value : DisplaySlot.values()) {
         if (this.getDisplayObjective(value) == objective) {
            this.setDisplayObjective(value, null);
         }
      }

      List<Objective> objectives = (List<Objective>)this.objectivesByCriteria.get(objective.getCriteria());
      if (objectives != null) {
         objectives.remove(objective);
      }

      for (PlayerScores playerScore : this.playerScores.values()) {
         playerScore.remove(objective);
      }

      this.onObjectiveRemoved(objective);
   }

   public void setDisplayObjective(final DisplaySlot slot, final @Nullable Objective objective) {
      this.displayObjectives.put(slot, objective);
   }

   public @Nullable Objective getDisplayObjective(final DisplaySlot slot) {
      return this.displayObjectives.get(slot);
   }

   public @Nullable PlayerTeam getPlayerTeam(final String name) {
      return (PlayerTeam)this.teamsByName.get(name);
   }

   public PlayerTeam addPlayerTeam(final String name) {
      PlayerTeam team = this.getPlayerTeam(name);
      if (team != null) {
         LOGGER.warn("Requested creation of existing team '{}'", name);
         return team;
      } else {
         team = new PlayerTeam(this, name);
         this.teamsByName.put(name, team);
         this.onTeamAdded(team);
         return team;
      }
   }

   public void removePlayerTeam(final PlayerTeam team) {
      this.teamsByName.remove(team.getName());

      for (String player : team.getPlayers()) {
         this.teamsByPlayer.remove(player);
      }

      this.onTeamRemoved(team);
   }

   public boolean addPlayerToTeam(final String player, final PlayerTeam team) {
      if (this.getPlayersTeam(player) != null) {
         this.removePlayerFromTeam(player);
      }

      this.teamsByPlayer.put(player, team);
      return team.getPlayers().add(player);
   }

   public boolean removePlayerFromTeam(final String player) {
      PlayerTeam team = this.getPlayersTeam(player);
      if (team != null) {
         this.removePlayerFromTeam(player, team);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(final String player, final PlayerTeam team) {
      if (this.getPlayersTeam(player) != team) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
      }

      this.teamsByPlayer.remove(player);
      team.getPlayers().remove(player);
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   public @Nullable PlayerTeam getPlayersTeam(final String name) {
      return (PlayerTeam)this.teamsByPlayer.get(name);
   }

   public void onObjectiveAdded(final Objective objective) {
   }

   public void onObjectiveChanged(final Objective objective) {
   }

   public void onObjectiveRemoved(final Objective objective) {
   }

   protected void onScoreChanged(final ScoreHolder owner, final Objective objective, final Score score) {
   }

   protected void onScoreLockChanged(final ScoreHolder owner, final Objective objective) {
   }

   public void onPlayerRemoved(final ScoreHolder player) {
   }

   public void onPlayerScoreRemoved(final ScoreHolder player, final Objective objective) {
   }

   public void onTeamAdded(final PlayerTeam team) {
   }

   public void onTeamChanged(final PlayerTeam team) {
   }

   public void onTeamRemoved(final PlayerTeam team) {
   }

   public void entityRemoved(final Entity entity) {
      if (!(entity instanceof Player) && !entity.isAlive()) {
         this.resetAllPlayerScores(entity);
         this.removePlayerFromTeam(entity.getScoreboardName());
      }
   }

   protected List<Scoreboard.PackedScore> packPlayerScores() {
      return this.playerScores
         .entrySet()
         .stream()
         .flatMap(
            playerEntry -> {
               String player = playerEntry.getKey();
               return playerEntry.getValue()
                  .listRawScores()
                  .entrySet()
                  .stream()
                  .map(entry -> new Scoreboard.PackedScore(player, entry.getKey().getName(), entry.getValue().pack()));
            }
         )
         .toList();
   }

   protected void loadPlayerScore(final Scoreboard.PackedScore score) {
      Objective objective = this.getObjective(score.objective);
      if (objective == null) {
         LOGGER.error("Unknown objective {} for name {}, ignoring", score.objective, score.owner);
      } else {
         this.getOrCreatePlayerInfo(score.owner).setScore(objective, new Score(score.score));
      }
   }

   protected List<PlayerTeam.Packed> packPlayerTeams() {
      return this.getPlayerTeams().stream().map(PlayerTeam::pack).toList();
   }

   protected void loadPlayerTeam(final PlayerTeam.Packed packed) {
      PlayerTeam team = this.addPlayerTeam(packed.name());
      packed.displayName().ifPresent(team::setDisplayName);
      team.setColor(packed.color());
      team.setAllowFriendlyFire(packed.allowFriendlyFire());
      team.setSeeFriendlyInvisibles(packed.seeFriendlyInvisibles());
      team.setPlayerPrefix(packed.memberNamePrefix());
      team.setPlayerSuffix(packed.memberNameSuffix());
      team.setNameTagVisibility(packed.nameTagVisibility());
      team.setDeathMessageVisibility(packed.deathMessageVisibility());
      team.setCollisionRule(packed.collisionRule());

      for (String player : packed.players()) {
         this.addPlayerToTeam(player, team);
      }
   }

   protected List<Objective.Packed> packObjectives() {
      return this.getObjectives().stream().map(Objective::pack).toList();
   }

   protected void loadObjective(final Objective.Packed objective) {
      this.addObjective(
         objective.name(),
         objective.criteria(),
         objective.displayName(),
         objective.renderType(),
         objective.displayAutoUpdate(),
         objective.numberFormat().orElse(null)
      );
   }

   protected Map<DisplaySlot, String> packDisplaySlots() {
      Map<DisplaySlot, String> displaySlots = new EnumMap<>(DisplaySlot.class);

      for (DisplaySlot slot : DisplaySlot.values()) {
         Objective objective = this.getDisplayObjective(slot);
         if (objective != null) {
            displaySlots.put(slot, objective.getName());
         }
      }

      return displaySlots;
   }

   public record PackedScore(String owner, String objective, Score.Packed score) {
      public static final Codec<Scoreboard.PackedScore> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.STRING.fieldOf("Name").forGetter(Scoreboard.PackedScore::owner),
               Codec.STRING.fieldOf("Objective").forGetter(Scoreboard.PackedScore::objective),
               Score.Packed.MAP_CODEC.forGetter(Scoreboard.PackedScore::score)
            )
            .apply(i, Scoreboard.PackedScore::new)
      );
   }
}
