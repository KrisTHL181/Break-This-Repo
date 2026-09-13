package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum DyeColor implements StringRepresentable {
   WHITE(0, "white", 16383998, MapColor.SNOW, 15790320, 16777215),
   ORANGE(1, "orange", 16351261, MapColor.COLOR_ORANGE, 15435844, 16738335),
   MAGENTA(2, "magenta", 13061821, MapColor.COLOR_MAGENTA, 12801229, 16711935),
   LIGHT_BLUE(3, "light_blue", 3847130, MapColor.COLOR_LIGHT_BLUE, 6719955, 10141901),
   YELLOW(4, "yellow", 16701501, MapColor.COLOR_YELLOW, 14602026, 16776960),
   LIME(5, "lime", 8439583, MapColor.COLOR_LIGHT_GREEN, 4312372, 12582656),
   PINK(6, "pink", 15961002, MapColor.COLOR_PINK, 14188952, 16738740),
   GRAY(7, "gray", 4673362, MapColor.COLOR_GRAY, 4408131, 8421504),
   LIGHT_GRAY(8, "light_gray", 10329495, MapColor.COLOR_LIGHT_GRAY, 11250603, 13882323),
   CYAN(9, "cyan", 1481884, MapColor.COLOR_CYAN, 2651799, 65535),
   PURPLE(10, "purple", 8991416, MapColor.COLOR_PURPLE, 8073150, 10494192),
   BLUE(11, "blue", 3949738, MapColor.COLOR_BLUE, 2437522, 255),
   BROWN(12, "brown", 8606770, MapColor.COLOR_BROWN, 5320730, 9127187),
   GREEN(13, "green", 6192150, MapColor.COLOR_GREEN, 3887386, 65280),
   RED(14, "red", 11546150, MapColor.COLOR_RED, 11743532, 16711680),
   BLACK(15, "black", 1908001, MapColor.COLOR_BLACK, 1973019, 0);

   private static final IntFunction<DyeColor> BY_ID = ByIdMap.continuous(DyeColor::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   private static final Int2ObjectOpenHashMap<DyeColor> BY_FIREWORK_COLOR = new Int2ObjectOpenHashMap(
      Arrays.stream(values()).collect(Collectors.toMap(p_41064_ -> p_41064_.fireworkColor, p_41056_ -> (DyeColor)p_41056_))
   );
   public static final StringRepresentable.EnumCodec<DyeColor> CODEC = StringRepresentable.fromEnum(DyeColor::values);
   public static final StreamCodec<ByteBuf, DyeColor> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DyeColor::getId);
   @Deprecated
   public static final Codec<DyeColor> LEGACY_ID_CODEC = Codec.BYTE.xmap(DyeColor::byId, p_390804_ -> (byte)p_390804_.id);
   private final int id;
   private final String name;
   private final MapColor mapColor;
   private final int textureDiffuseColor;
   private final int fireworkColor;
   private final int textColor;

   DyeColor(final int p_41046_, final String p_41047_, final int p_41048_, final MapColor p_285297_, final int p_41050_, final int p_41051_) {
      this.id = p_41046_;
      this.name = p_41047_;
      this.mapColor = p_285297_;
      this.textColor = ARGB.opaque(p_41051_);
      this.textureDiffuseColor = ARGB.opaque(p_41048_);
      this.fireworkColor = p_41050_;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public int getTextureDiffuseColor() {
      return this.textureDiffuseColor;
   }

   public MapColor getMapColor() {
      return this.mapColor;
   }

   public int getFireworkColor() {
      return this.fireworkColor;
   }

   public int getTextColor() {
      return this.textColor;
   }

   public static DyeColor byId(int p_41054_) {
      return BY_ID.apply(p_41054_);
   }

   @Contract("_,!null->!null;_,null->_")
   public static @Nullable DyeColor byName(String p_41058_, @Nullable DyeColor p_41059_) {
      DyeColor dyecolor = CODEC.byName(p_41058_);
      return dyecolor != null ? dyecolor : p_41059_;
   }

   public static @Nullable DyeColor byFireworkColor(int p_41062_) {
      return (DyeColor)BY_FIREWORK_COLOR.get(p_41062_);
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static DyeColor getMixedColor(ServerLevel p_377753_, DyeColor p_378336_, DyeColor p_376650_) {
      CraftingInput craftinginput = makeCraftColorInput(p_378336_, p_376650_);
      return p_377753_.recipeAccess()
         .getRecipeFor(RecipeType.CRAFTING, craftinginput, p_377753_)
         .map(p_449791_ -> p_449791_.value().assemble(craftinginput, p_377753_.registryAccess()))
         .map(ItemStack::getItem)
         .filter(DyeItem.class::isInstance)
         .map(DyeItem.class::cast)
         .map(DyeItem::getDyeColor)
         .orElseGet(() -> p_377753_.random.nextBoolean() ? p_378336_ : p_376650_);
   }

   private static CraftingInput makeCraftColorInput(DyeColor p_376875_, DyeColor p_378606_) {
      return CraftingInput.of(2, 1, List.of(new ItemStack(DyeItem.byColor(p_376875_)), new ItemStack(DyeItem.byColor(p_378606_))));
   }
}
