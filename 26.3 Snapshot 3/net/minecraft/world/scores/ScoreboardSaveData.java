package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ScoreboardSaveData extends SavedData {
   public static final SavedDataType<ScoreboardSaveData> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("scoreboard"),
      ScoreboardSaveData::new,
      ScoreboardSaveData.Packed.CODEC.xmap(ScoreboardSaveData::new, ScoreboardSaveData::getData),
      DataFixTypes.SAVED_DATA_SCOREBOARD
   );
   private ScoreboardSaveData.Packed data;

   private ScoreboardSaveData() {
      this(ScoreboardSaveData.Packed.EMPTY);
   }

   public ScoreboardSaveData(final ScoreboardSaveData.Packed data) {
      this.data = data;
   }

   public ScoreboardSaveData.Packed getData() {
      return this.data;
   }

   public void setData(final ScoreboardSaveData.Packed data) {
      if (!data.equals(this.data)) {
         this.data = data;
         this.setDirty();
      }
   }

   public record Packed(
      List<Objective.Packed> objectives, List<Scoreboard.PackedScore> scores, Map<DisplaySlot, String> displaySlots, List<PlayerTeam.Packed> teams
   ) {
      public static final ScoreboardSaveData.Packed EMPTY = new ScoreboardSaveData.Packed(List.of(), List.of(), Map.of(), List.of());
      public static final Codec<ScoreboardSaveData.Packed> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Objective.Packed.CODEC.listOf().optionalFieldOf("Objectives", List.of()).forGetter(ScoreboardSaveData.Packed::objectives),
               Scoreboard.PackedScore.CODEC.listOf().optionalFieldOf("PlayerScores", List.of()).forGetter(ScoreboardSaveData.Packed::scores),
               Codec.unboundedMap(DisplaySlot.CODEC, Codec.STRING).optionalFieldOf("DisplaySlots", Map.of()).forGetter(ScoreboardSaveData.Packed::displaySlots),
               PlayerTeam.Packed.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(ScoreboardSaveData.Packed::teams)
            )
            .apply(i, ScoreboardSaveData.Packed::new)
      );
   }
}
