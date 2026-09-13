package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

public class TrimMaterials {
   public static final ResourceKey<TrimMaterial> QUARTZ = registryKey("quartz");
   public static final ResourceKey<TrimMaterial> IRON = registryKey("iron");
   public static final ResourceKey<TrimMaterial> NETHERITE = registryKey("netherite");
   public static final ResourceKey<TrimMaterial> REDSTONE = registryKey("redstone");
   public static final ResourceKey<TrimMaterial> COPPER = registryKey("copper");
   public static final ResourceKey<TrimMaterial> GOLD = registryKey("gold");
   public static final ResourceKey<TrimMaterial> EMERALD = registryKey("emerald");
   public static final ResourceKey<TrimMaterial> DIAMOND = registryKey("diamond");
   public static final ResourceKey<TrimMaterial> LAPIS = registryKey("lapis");
   public static final ResourceKey<TrimMaterial> AMETHYST = registryKey("amethyst");
   public static final ResourceKey<TrimMaterial> RESIN = registryKey("resin");

   public static void bootstrap(final BootstrapContext<TrimMaterial> context) {
      register(context, QUARTZ, Style.EMPTY.withColor(14931140), TrimMaterials.Palette.QUARTZ);
      register(context, IRON, Style.EMPTY.withColor(15527148), TrimMaterials.Palette.IRON);
      register(context, NETHERITE, Style.EMPTY.withColor(6445145), TrimMaterials.Palette.NETHERITE);
      register(context, REDSTONE, Style.EMPTY.withColor(9901575), TrimMaterials.Palette.REDSTONE);
      register(context, COPPER, Style.EMPTY.withColor(11823181), TrimMaterials.Palette.COPPER);
      register(context, GOLD, Style.EMPTY.withColor(14594349), TrimMaterials.Palette.GOLD);
      register(context, EMERALD, Style.EMPTY.withColor(1155126), TrimMaterials.Palette.EMERALD);
      register(context, DIAMOND, Style.EMPTY.withColor(7269586), TrimMaterials.Palette.DIAMOND);
      register(context, LAPIS, Style.EMPTY.withColor(4288151), TrimMaterials.Palette.LAPIS);
      register(context, AMETHYST, Style.EMPTY.withColor(10116294), TrimMaterials.Palette.AMETHYST);
      register(context, RESIN, Style.EMPTY.withColor(16545810), TrimMaterials.Palette.RESIN);
   }

   private static void register(
      final BootstrapContext<TrimMaterial> context,
      final ResourceKey<TrimMaterial> registryKey,
      final Style hoverTextStyle,
      final TrimMaterials.Palette palette
   ) {
      Component description = Component.translatable(Util.makeDescriptionId("trim_material", registryKey.identifier())).withStyle(hoverTextStyle);
      context.register(registryKey, new TrimMaterial(palette.id(), description));
   }

   private static ResourceKey<TrimMaterial> registryKey(final String id) {
      return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace(id));
   }

   public enum Palette {
      QUARTZ("quartz"),
      IRON("iron"),
      IRON_DARKER("iron_darker"),
      NETHERITE("netherite"),
      NETHERITE_DARKER("netherite_darker"),
      REDSTONE("redstone"),
      COPPER("copper"),
      COPPER_DARKER("copper_darker"),
      GOLD("gold"),
      GOLD_DARKER("gold_darker"),
      EMERALD("emerald"),
      DIAMOND("diamond"),
      DIAMOND_DARKER("diamond_darker"),
      LAPIS("lapis"),
      AMETHYST("amethyst"),
      RESIN("resin");

      private final String suffix;
      private final Identifier id;

      Palette(final String name) {
         this.suffix = name;
         this.id = Identifier.withDefaultNamespace("trim/" + name);
      }

      public String suffix() {
         return this.suffix;
      }

      public Identifier id() {
         return this.id;
      }
   }
}
