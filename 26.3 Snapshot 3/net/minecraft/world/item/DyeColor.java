package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum DyeColor implements StringRepresentable {
   WHITE(0, "white", 16383998, MapColor.SNOW, MapColor.TERRACOTTA_WHITE, 15790320, 16777215),
   ORANGE(1, "orange", 16351261, MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_ORANGE, 15435844, 16738335),
   MAGENTA(2, "magenta", 13061821, MapColor.COLOR_MAGENTA, MapColor.TERRACOTTA_MAGENTA, 12801229, 16711935),
   LIGHT_BLUE(3, "light_blue", 3847130, MapColor.COLOR_LIGHT_BLUE, MapColor.TERRACOTTA_LIGHT_BLUE, 6719955, 10141901),
   YELLOW(4, "yellow", 16701501, MapColor.COLOR_YELLOW, MapColor.TERRACOTTA_YELLOW, 14602026, 16776960),
   LIME(5, "lime", 8439583, MapColor.COLOR_LIGHT_GREEN, MapColor.TERRACOTTA_LIGHT_GREEN, 4312372, 12582656),
   PINK(6, "pink", 15961002, MapColor.COLOR_PINK, MapColor.TERRACOTTA_PINK, 14188952, 16738740),
   GRAY(7, "gray", 4673362, MapColor.COLOR_GRAY, MapColor.TERRACOTTA_GRAY, 4408131, 8421504),
   LIGHT_GRAY(8, "light_gray", 10329495, MapColor.COLOR_LIGHT_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY, 11250603, 13882323),
   CYAN(9, "cyan", 1481884, MapColor.COLOR_CYAN, MapColor.TERRACOTTA_CYAN, 2651799, 65535),
   PURPLE(10, "purple", 8991416, MapColor.COLOR_PURPLE, MapColor.TERRACOTTA_PURPLE, 8073150, 10494192),
   BLUE(11, "blue", 3949738, MapColor.COLOR_BLUE, MapColor.TERRACOTTA_BLUE, 2437522, 255),
   BROWN(12, "brown", 8606770, MapColor.COLOR_BROWN, MapColor.TERRACOTTA_BROWN, 5320730, 9127187),
   GREEN(13, "green", 6192150, MapColor.COLOR_GREEN, MapColor.TERRACOTTA_GREEN, 3887386, 65280),
   RED(14, "red", 11546150, MapColor.COLOR_RED, MapColor.TERRACOTTA_RED, 11743532, 16711680),
   BLACK(15, "black", 1908001, MapColor.COLOR_BLACK, MapColor.TERRACOTTA_BLACK, 1973019, 0);

   public static final List<DyeColor> VALUES = List.of(values());
   private static final IntFunction<DyeColor> BY_ID = ByIdMap.continuous(DyeColor::getId, VALUES.toArray(DyeColor[]::new), ByIdMap.OutOfBoundsStrategy.ZERO);
   private static final Int2ObjectOpenHashMap<DyeColor> BY_FIREWORK_COLOR = new Int2ObjectOpenHashMap(
      VALUES.stream().collect(Collectors.toMap(v -> v.fireworkColor, v -> (DyeColor)v))
   );
   public static final StringRepresentable.EnumCodec<DyeColor> CODEC = StringRepresentable.fromEnum(DyeColor::values);
   public static final StreamCodec<ByteBuf, DyeColor> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DyeColor::getId);
   @Deprecated
   public static final Codec<DyeColor> LEGACY_ID_CODEC = Codec.BYTE.xmap(DyeColor::byId, color -> (byte)color.id);
   private final int id;
   private final String name;
   private final MapColor mapColor;
   private final MapColor terracottaColor;
   private final int textureDiffuseColor;
   private final int fireworkColor;
   private final int textColor;

   DyeColor(
      final int id,
      final String name,
      final int textureDiffuseColor,
      final MapColor mapColor,
      final MapColor terracottaColor,
      final int fireworkColor,
      final int textColor
   ) {
      this.id = id;
      this.name = name;
      this.mapColor = mapColor;
      this.terracottaColor = terracottaColor;
      this.textColor = ARGB.opaque(textColor);
      this.textureDiffuseColor = ARGB.opaque(textureDiffuseColor);
      this.fireworkColor = fireworkColor;
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

   public MapColor getTerracottaColor() {
      return this.terracottaColor;
   }

   public int getFireworkColor() {
      return this.fireworkColor;
   }

   public int getTextColor() {
      return this.textColor;
   }

   public static DyeColor byId(final int id) {
      return BY_ID.apply(id);
   }

   @Contract("_,!null->!null;_,null->_")
   public static @Nullable DyeColor byName(final String name, final @Nullable DyeColor def) {
      DyeColor result = CODEC.byName(name);
      return result != null ? result : def;
   }

   public static @Nullable DyeColor byFireworkColor(final int color) {
      return (DyeColor)BY_FIREWORK_COLOR.get(color);
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static DyeColor getMixedColor(final ServerLevel level, final DyeColor dyeColor1, final DyeColor dyeColor2) {
      DyeColor mixedColor = findColorMixInRecipes(level, dyeColor1, dyeColor2);
      if (mixedColor != null) {
         return mixedColor;
      } else {
         return level.getRandom().nextBoolean() ? dyeColor1 : dyeColor2;
      }
   }

   private static @Nullable DyeColor findColorMixInRecipes(final ServerLevel level, final DyeColor dyeColor1, final DyeColor dyeColor2) {
      DataComponentLookup<Item> itemComponents = level.registryAccess().lookupOrThrow(Registries.ITEM).componentLookup();
      Collection<Holder<Item>> dye1Items = itemComponents.findAll(DataComponents.DYE, dyeColor1);
      if (dye1Items.isEmpty()) {
         return null;
      }

      Collection<Holder<Item>> dye2Items = itemComponents.findAll(DataComponents.DYE, dyeColor2);
      if (dye2Items.isEmpty()) {
         return null;
      }

      for (Holder<Item> dye1Item : dye1Items) {
         for (Holder<Item> dye2Item : dye2Items) {
            CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(dye1Item), new ItemStack(dye2Item)));
            Optional<RecipeHolder<CraftingRecipe>> foundRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (foundRecipe.isPresent()) {
               ItemStack craftingResult = foundRecipe.get().value().assemble(input);
               DyeColor craftedDyeColor = craftingResult.get(DataComponents.DYE);
               if (craftedDyeColor != null) {
                  return craftedDyeColor;
               }
            }
         }
      }

      return null;
   }
}
