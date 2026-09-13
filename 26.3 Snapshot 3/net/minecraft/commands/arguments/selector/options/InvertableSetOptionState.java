package net.minecraft.commands.arguments.selector.options;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;

public class InvertableSetOptionState {
   private InvertableSetOptionState.Limitation state = InvertableSetOptionState.Limitation.NONE;
   private final Set<Identifier> tags = new HashSet<>();

   private boolean canLimitToSingle() {
      return this.state == InvertableSetOptionState.Limitation.NONE;
   }

   private boolean canLimitToMultiple() {
      return this.state != InvertableSetOptionState.Limitation.SINGLE;
   }

   public boolean canParsePositiveElement() {
      return this.canLimitToSingle();
   }

   public boolean canParseNegativeElement() {
      return this.canLimitToMultiple();
   }

   public boolean canParseAnyTag() {
      return this.canLimitToMultiple();
   }

   public boolean canParseTag(final Identifier tag) {
      return this.canParseAnyTag() && !this.tags.contains(tag);
   }

   public boolean canParseAny() {
      return this.state != InvertableSetOptionState.Limitation.SINGLE;
   }

   public boolean canParseElement(final boolean inverted) {
      return inverted ? this.canParseNegativeElement() : this.canParsePositiveElement();
   }

   private void markParsedSingle() {
      this.state = InvertableSetOptionState.Limitation.SINGLE;
   }

   private void markParsedMultiple() {
      this.state = InvertableSetOptionState.Limitation.MULTIPLE;
   }

   public void markParsedPositiveElement() {
      this.markParsedSingle();
   }

   public void markParsedNegativeElement() {
      this.markParsedMultiple();
   }

   public void markParsedTag(final Identifier tag) {
      this.markParsedMultiple();
      this.tags.add(tag);
   }

   public void markParsedElement(final boolean inverted) {
      if (inverted) {
         this.markParsedNegativeElement();
      } else {
         this.markParsedPositiveElement();
      }
   }

   private enum Limitation {
      NONE,
      SINGLE,
      MULTIPLE;
   }
}
