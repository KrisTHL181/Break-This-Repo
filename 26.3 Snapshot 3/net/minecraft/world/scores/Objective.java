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
      final Scoreboard scoreboard,
      final String name,
      final ObjectiveCriteria criteria,
      final Component displayName,
      final ObjectiveCriteria.RenderType renderType,
      final boolean displayAutoUpdate,
      final @Nullable NumberFormat numberFormat
   ) {
      this.scoreboard = scoreboard;
      this.name = name;
      this.criteria = criteria;
      this.displayName = displayName;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.renderType = renderType;
      this.displayAutoUpdate = displayAutoUpdate;
      this.numberFormat = numberFormat;
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

   public NumberFormat numberFormatOrDefault(final NumberFormat _default) {
      return Objects.requireNonNullElse(this.numberFormat, _default);
   }

   private Component createFormattedDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(
         this.displayName.copy().withStyle(s -> s.withHoverEvent(new HoverEvent.ShowText(Component.literal(this.name))))
      );
   }

   public Component getFormattedDisplayName() {
      return this.formattedDisplayName;
   }

   public void setDisplayName(final Component name) {
      this.displayName = name;
      this.formattedDisplayName = this.createFormattedDisplayName();
      this.scoreboard.onObjectiveChanged(this);
   }

   public ObjectiveCriteria.RenderType getRenderType() {
      return this.renderType;
   }

   public void setRenderType(final ObjectiveCriteria.RenderType renderType) {
      this.renderType = renderType;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setDisplayAutoUpdate(final boolean displayAutoUpdate) {
      this.displayAutoUpdate = displayAutoUpdate;
      this.scoreboard.onObjectiveChanged(this);
   }

   public void setNumberFormat(final @Nullable NumberFormat numberFormat) {
      this.numberFormat = numberFormat;
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
         i -> i.group(
               Codec.STRING.fieldOf("Name").forGetter(Objective.Packed::name),
               ObjectiveCriteria.CODEC.optionalFieldOf("CriteriaName", ObjectiveCriteria.DUMMY).forGetter(Objective.Packed::criteria),
               ComponentSerialization.CODEC.fieldOf("DisplayName").forGetter(Objective.Packed::displayName),
               ObjectiveCriteria.RenderType.CODEC.optionalFieldOf("RenderType", ObjectiveCriteria.RenderType.INTEGER).forGetter(Objective.Packed::renderType),
               Codec.BOOL.optionalFieldOf("display_auto_update", false).forGetter(Objective.Packed::displayAutoUpdate),
               NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Objective.Packed::numberFormat)
            )
            .apply(i, Objective.Packed::new)
      );
   }
}
