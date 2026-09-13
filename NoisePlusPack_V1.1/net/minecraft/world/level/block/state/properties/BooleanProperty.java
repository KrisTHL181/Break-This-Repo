package net.minecraft.world.level.block.state.properties;

import java.util.List;
import java.util.Optional;

public final class BooleanProperty extends Property<Boolean> {
   private static final List<Boolean> VALUES = List.of(true, false);
   private static final int TRUE_INDEX = 0;
   private static final int FALSE_INDEX = 1;

   private BooleanProperty(String p_61459_) {
      super(p_61459_, Boolean.class);
   }

   @Override
   public List<Boolean> getPossibleValues() {
      return VALUES;
   }

   public static BooleanProperty create(String p_61466_) {
      return new BooleanProperty(p_61466_);
   }

   @Override
   public Optional<Boolean> getValue(String p_61469_) {
      return switch (p_61469_) {
         case "true" -> Optional.of(true);
         case "false" -> Optional.of(false);
         default -> Optional.empty();
      };
   }

   public String getName(Boolean p_61462_) {
      return p_61462_.toString();
   }

   public int getInternalIndex(Boolean p_361970_) {
      return p_361970_ ? 0 : 1;
   }
}
