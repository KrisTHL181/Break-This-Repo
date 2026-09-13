package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jspecify.annotations.Nullable;

public class Objective {
   private final Scoreboard scoreboard;
   private final String name;
   private final ObjectiveCriteria criteria;
   private Component displayName;
   private Component formattedDisplayName;
   private ObjectiveCriteria.RenderType renderType;
   private boolean displayAutoUpdate;
   private @Nullable NumberFormat numberFormat;

   public Objective(
      Scoreboard p_83308_,
      String p_83309_,
      ObjectiveCriteria p_83310_,
      Component p_83311_,
      ObjectiveCriteria.RenderType p_83312_,
      boolean p_311052_,
      @Nullable NumberFormat p_309864_
   ) {
      this.scoreboard = p_83308_;
      this.name = p_83309_;
      this.criteria = p_83310_;
      this.displayName = p_83311_;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.renderType = p_83312_;
      this.displayAutoUpdate = p_311052_;
      this.numberFormat = p_309864_;
   }

   public Objective.Packed pack() {
      return new Objective.Packed(this.name, this.criteria, this.displayName, this.renderType, this.displayAutoUpdate, Optional.ofNullable(this.numberFormat));
   }

   public Scoreboard getScoreboard() {
      return this.scoreboard;
   }

   public String getName() {
      return this.name;
   }

   public ObjectiveCriteria getCriteria() {
      return this.criteria;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public boolean displayAutoUpdate() {
      return this.displayAutoUpdate;
   }

   public @Nullable NumberFormat numberFormat() {
      return this.numberFormat;
   }

   public NumberFormat numberFormatOrDefault(NumberFormat p_309891_) {
      return Objects.requireNonNullElse(this.numberFormat, p_309891_);
   }

   private Component createFormattedDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(
         this.displayName.copy().withStyle(p_391141_ -> p_391141_.withHoverEvent(new HoverEvent.ShowText(Component.literal(this.name))))
      );
   }

   public Component getFormattedDisplayName() {
      return this.formattedDisplayName;
   }

   public void setDisplayName(Component p_83317_) {
      this.displayName = p_83317_;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.scoreboard.onObjectiveChanged(this);
   }

   public ObjectiveCriteria.RenderType getRenderType() {
      return this.renderType;
   }

   public void setRenderType(ObjectiveCriteria.RenderType p_83315_) {
      this.renderType = p_83315_;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setDisplayAutoUpdate(boolean p_309636_) {
      this.displayAutoUpdate = p_309636_;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setNumberFormat(@Nullable NumberFormat p_311380_) {
      this.numberFormat = p_311380_;
      this.scoreboard.onObjectiveChanged(this);
   }

   public record Packed(
      String name,
      ObjectiveCriteria criteria,
      Component displayName,
      ObjectiveCriteria.RenderType renderType,
      boolean displayAutoUpdate,
      Optional<NumberFormat> numberFormat
   ) {
      public static final Codec<Objective.Packed> CODEC = RecordCodecBuilder.create(
         p_393081_ -> p_393081_.group(
               Codec.STRING.fieldOf("Name").forGetter(Objective.Packed::name),
               ObjectiveCriteria.CODEC.optionalFieldOf("CriteriaName", ObjectiveCriteria.DUMMY).forGetter(Objective.Packed::criteria),
               ComponentSerialization.CODEC.fieldOf("DisplayName").forGetter(Objective.Packed::displayName),
               ObjectiveCriteria.RenderType.CODEC.optionalFieldOf("RenderType", ObjectiveCriteria.RenderType.INTEGER).forGetter(Objective.Packed::renderType),
               Codec.BOOL.optionalFieldOf("display_auto_update", false).forGetter(Objective.Packed::displayAutoUpdate),
               NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Objective.Packed::numberFormat)
            )
            .apply(p_393081_, Objective.Packed::new)
      );
   }
}
