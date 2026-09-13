package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class ScoreboardSaveData extends SavedData {
   public static final SavedDataType<ScoreboardSaveData> TYPE = new SavedDataType<>(
      "scoreboard",
      ScoreboardSaveData::new,
      ScoreboardSaveData.Packed.CODEC.xmap(ScoreboardSaveData::new, ScoreboardSaveData::getData),
      DataFixTypes.SAVED_DATA_SCOREBOARD
   );
   private ScoreboardSaveData.Packed data;

   private ScoreboardSaveData() {
      this(ScoreboardSaveData.Packed.EMPTY);
   }

   public ScoreboardSaveData(ScoreboardSaveData.Packed p_452993_) {
      this.data = p_452993_;
   }

   public ScoreboardSaveData.Packed getData() {
      return this.data;
   }

   public void setData(ScoreboardSaveData.Packed p_453477_) {
      if (!p_453477_.equals(this.data)) {
         this.data = p_453477_;
         this.setDirty();
      }
   }

   public record Packed(
      List<Objective.Packed> objectives, List<Scoreboard.PackedScore> scores, Map<DisplaySlot, String> displaySlots, List<PlayerTeam.Packed> teams
   ) {
      public static final ScoreboardSaveData.Packed EMPTY = new ScoreboardSaveData.Packed(List.of(), List.of(), Map.of(), List.of());
      public static final Codec<ScoreboardSaveData.Packed> CODEC = RecordCodecBuilder.create(
         p_396099_ -> p_396099_.group(
               Objective.Packed.CODEC.listOf().optionalFieldOf("Objectives", List.of()).forGetter(ScoreboardSaveData.Packed::objectives),
               Scoreboard.PackedScore.CODEC.listOf().optionalFieldOf("PlayerScores", List.of()).forGetter(ScoreboardSaveData.Packed::scores),
               Codec.unboundedMap(DisplaySlot.CODEC, Codec.STRING).optionalFieldOf("DisplaySlots", Map.of()).forGetter(ScoreboardSaveData.Packed::displaySlots),
               PlayerTeam.Packed.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(ScoreboardSaveData.Packed::teams)
            )
            .apply(p_396099_, ScoreboardSaveData.Packed::new)
      );
   }
}
