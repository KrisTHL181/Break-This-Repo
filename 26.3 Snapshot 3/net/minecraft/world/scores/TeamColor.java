package net.minecraft.world.scores;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

public enum TeamColor implements StringRepresentable {
   BLACK(0, "black", TextColor.BLACK, DisplaySlot.TEAM_BLACK),
   DARK_BLUE(1, "dark_blue", TextColor.DARK_BLUE, DisplaySlot.TEAM_DARK_BLUE),
   DARK_GREEN(2, "dark_green", TextColor.DARK_GREEN, DisplaySlot.TEAM_DARK_GREEN),
   DARK_AQUA(3, "dark_aqua", TextColor.DARK_AQUA, DisplaySlot.TEAM_DARK_AQUA),
   DARK_RED(4, "dark_red", TextColor.DARK_RED, DisplaySlot.TEAM_DARK_RED),
   DARK_PURPLE(5, "dark_purple", TextColor.DARK_PURPLE, DisplaySlot.TEAM_DARK_PURPLE),
   GOLD(6, "gold", TextColor.GOLD, DisplaySlot.TEAM_GOLD),
   GRAY(7, "gray", TextColor.GRAY, DisplaySlot.TEAM_GRAY),
   DARK_GRAY(8, "dark_gray", TextColor.DARK_GRAY, DisplaySlot.TEAM_DARK_GRAY),
   BLUE(9, "blue", TextColor.BLUE, DisplaySlot.TEAM_BLUE),
   GREEN(10, "green", TextColor.GREEN, DisplaySlot.TEAM_GREEN),
   AQUA(11, "aqua", TextColor.AQUA, DisplaySlot.TEAM_AQUA),
   RED(12, "red", TextColor.RED, DisplaySlot.TEAM_RED),
   LIGHT_PURPLE(13, "light_purple", TextColor.LIGHT_PURPLE, DisplaySlot.TEAM_LIGHT_PURPLE),
   YELLOW(14, "yellow", TextColor.YELLOW, DisplaySlot.TEAM_YELLOW),
   WHITE(15, "white", TextColor.WHITE, DisplaySlot.TEAM_WHITE);

   public static final List<TeamColor> VALUES = List.of(values());
   public static final StringRepresentable.EnumCodec<TeamColor> CODEC = StringRepresentable.fromEnum(TeamColor::values);
   private static final IntFunction<TeamColor> BY_ID = ByIdMap.continuous(v -> v.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, TeamColor> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, v -> v.id);
   private final int id;
   private final String name;
   private final TextColor format;
   private final DisplaySlot displaySlot;

   TeamColor(final int id, final String name, final TextColor format, final DisplaySlot displaySlot) {
      this.id = id;
      this.name = name;
      this.format = format;
      this.displaySlot = displaySlot;
   }

   public static @Nullable TeamColor byName(final String name) {
      return CODEC.byName(name);
   }

   public TextColor textColor() {
      return this.format;
   }

   public DisplaySlot displaySlot() {
      return this.displaySlot;
   }

   public int rgb() {
      return this.format.getValue();
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
