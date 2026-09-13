package net.minecraft.world.scores;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum DisplaySlot implements StringRepresentable {
   LIST(0, "list"),
   SIDEBAR(1, "sidebar"),
   BELOW_NAME(2, "below_name"),
   TEAM_BLACK(3, "sidebar.team.black"),
   TEAM_DARK_BLUE(4, "sidebar.team.dark_blue"),
   TEAM_DARK_GREEN(5, "sidebar.team.dark_green"),
   TEAM_DARK_AQUA(6, "sidebar.team.dark_aqua"),
   TEAM_DARK_RED(7, "sidebar.team.dark_red"),
   TEAM_DARK_PURPLE(8, "sidebar.team.dark_purple"),
   TEAM_GOLD(9, "sidebar.team.gold"),
   TEAM_GRAY(10, "sidebar.team.gray"),
   TEAM_DARK_GRAY(11, "sidebar.team.dark_gray"),
   TEAM_BLUE(12, "sidebar.team.blue"),
   TEAM_GREEN(13, "sidebar.team.green"),
   TEAM_AQUA(14, "sidebar.team.aqua"),
   TEAM_RED(15, "sidebar.team.red"),
   TEAM_LIGHT_PURPLE(16, "sidebar.team.light_purple"),
   TEAM_YELLOW(17, "sidebar.team.yellow"),
   TEAM_WHITE(18, "sidebar.team.white");

   public static final StringRepresentable.EnumCodec<DisplaySlot> CODEC = StringRepresentable.fromEnum(DisplaySlot::values);
   public static final IntFunction<DisplaySlot> BY_ID = ByIdMap.continuous(DisplaySlot::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   private final int id;
   private final String name;

   DisplaySlot(final int id, final String name) {
      this.id = id;
      this.name = name;
   }

   public int id() {
      return this.id;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
