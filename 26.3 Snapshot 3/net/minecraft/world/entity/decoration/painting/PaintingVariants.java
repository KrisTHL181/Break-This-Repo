package net.minecraft.world.entity.decoration.painting;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PaintingVariants {
   public static final ResourceKey<PaintingVariant> KEBAB = create("kebab");
   public static final ResourceKey<PaintingVariant> AZTEC = create("aztec");
   public static final ResourceKey<PaintingVariant> ALBAN = create("alban");
   public static final ResourceKey<PaintingVariant> AZTEC2 = create("aztec2");
   public static final ResourceKey<PaintingVariant> BOMB = create("bomb");
   public static final ResourceKey<PaintingVariant> PLANT = create("plant");
   public static final ResourceKey<PaintingVariant> WASTELAND = create("wasteland");
   public static final ResourceKey<PaintingVariant> POOL = create("pool");
   public static final ResourceKey<PaintingVariant> COURBET = create("courbet");
   public static final ResourceKey<PaintingVariant> SEA = create("sea");
   public static final ResourceKey<PaintingVariant> SUNSET = create("sunset");
   public static final ResourceKey<PaintingVariant> CREEBET = create("creebet");
   public static final ResourceKey<PaintingVariant> WANDERER = create("wanderer");
   public static final ResourceKey<PaintingVariant> GRAHAM = create("graham");
   public static final ResourceKey<PaintingVariant> MATCH = create("match");
   public static final ResourceKey<PaintingVariant> BUST = create("bust");
   public static final ResourceKey<PaintingVariant> STAGE = create("stage");
   public static final ResourceKey<PaintingVariant> VOID = create("void");
   public static final ResourceKey<PaintingVariant> SKULL_AND_ROSES = create("skull_and_roses");
   public static final ResourceKey<PaintingVariant> WITHER = create("wither");
   public static final ResourceKey<PaintingVariant> FIGHTERS = create("fighters");
   public static final ResourceKey<PaintingVariant> POINTER = create("pointer");
   public static final ResourceKey<PaintingVariant> PIGSCENE = create("pigscene");
   public static final ResourceKey<PaintingVariant> BURNING_SKULL = create("burning_skull");
   public static final ResourceKey<PaintingVariant> SKELETON = create("skeleton");
   public static final ResourceKey<PaintingVariant> DONKEY_KONG = create("donkey_kong");
   public static final ResourceKey<PaintingVariant> EARTH = create("earth");
   public static final ResourceKey<PaintingVariant> WIND = create("wind");
   public static final ResourceKey<PaintingVariant> WATER = create("water");
   public static final ResourceKey<PaintingVariant> FIRE = create("fire");
   public static final ResourceKey<PaintingVariant> BAROQUE = create("baroque");
   public static final ResourceKey<PaintingVariant> HUMBLE = create("humble");
   public static final ResourceKey<PaintingVariant> MEDITATIVE = create("meditative");
   public static final ResourceKey<PaintingVariant> PRAIRIE_RIDE = create("prairie_ride");
   public static final ResourceKey<PaintingVariant> UNPACKED = create("unpacked");
   public static final ResourceKey<PaintingVariant> BACKYARD = create("backyard");
   public static final ResourceKey<PaintingVariant> BOUQUET = create("bouquet");
   public static final ResourceKey<PaintingVariant> CAVEBIRD = create("cavebird");
   public static final ResourceKey<PaintingVariant> CHANGING = create("changing");
   public static final ResourceKey<PaintingVariant> COTAN = create("cotan");
   public static final ResourceKey<PaintingVariant> ENDBOSS = create("endboss");
   public static final ResourceKey<PaintingVariant> FERN = create("fern");
   public static final ResourceKey<PaintingVariant> FINDING = create("finding");
   public static final ResourceKey<PaintingVariant> LOWMIST = create("lowmist");
   public static final ResourceKey<PaintingVariant> ORB = create("orb");
   public static final ResourceKey<PaintingVariant> OWLEMONS = create("owlemons");
   public static final ResourceKey<PaintingVariant> PASSAGE = create("passage");
   public static final ResourceKey<PaintingVariant> POND = create("pond");
   public static final ResourceKey<PaintingVariant> SUNFLOWERS = create("sunflowers");
   public static final ResourceKey<PaintingVariant> TIDES = create("tides");
   public static final ResourceKey<PaintingVariant> DENNIS = create("dennis");

   public static void bootstrap(final BootstrapContext<PaintingVariant> context) {
      register(context, KEBAB, 1, 1);
      register(context, AZTEC, 1, 1);
      register(context, ALBAN, 1, 1);
      register(context, AZTEC2, 1, 1);
      register(context, BOMB, 1, 1);
      register(context, PLANT, 1, 1);
      register(context, WASTELAND, 1, 1);
      register(context, POOL, 2, 1);
      register(context, COURBET, 2, 1);
      register(context, SEA, 2, 1);
      register(context, SUNSET, 2, 1);
      register(context, CREEBET, 2, 1);
      register(context, WANDERER, 1, 2);
      register(context, GRAHAM, 1, 2);
      register(context, MATCH, 2, 2);
      register(context, BUST, 2, 2);
      register(context, STAGE, 2, 2);
      register(context, VOID, 2, 2);
      register(context, SKULL_AND_ROSES, 2, 2);
      register(context, WITHER, 2, 2, false);
      register(context, FIGHTERS, 4, 2);
      register(context, POINTER, 4, 4);
      register(context, PIGSCENE, 4, 4);
      register(context, BURNING_SKULL, 4, 4);
      register(context, SKELETON, 4, 3);
      register(context, EARTH, 2, 2, false);
      register(context, WIND, 2, 2, false);
      register(context, WATER, 2, 2, false);
      register(context, FIRE, 2, 2, false);
      register(context, DONKEY_KONG, 4, 3);
      register(context, BAROQUE, 2, 2);
      register(context, HUMBLE, 2, 2);
      register(context, MEDITATIVE, 1, 1);
      register(context, PRAIRIE_RIDE, 1, 2);
      register(context, UNPACKED, 4, 4);
      register(context, BACKYARD, 3, 4);
      register(context, BOUQUET, 3, 3);
      register(context, CAVEBIRD, 3, 3);
      register(context, CHANGING, 4, 2);
      register(context, COTAN, 3, 3);
      register(context, ENDBOSS, 3, 3);
      register(context, FERN, 3, 3);
      register(context, FINDING, 4, 2);
      register(context, LOWMIST, 4, 2);
      register(context, ORB, 4, 4);
      register(context, OWLEMONS, 3, 3);
      register(context, PASSAGE, 4, 2);
      register(context, POND, 3, 4);
      register(context, SUNFLOWERS, 3, 3);
      register(context, TIDES, 3, 3);
      register(context, DENNIS, 3, 3);
   }

   private static void register(final BootstrapContext<PaintingVariant> context, final ResourceKey<PaintingVariant> id, final int width, final int height) {
      register(context, id, width, height, true);
   }

   private static void register(
      final BootstrapContext<PaintingVariant> context, final ResourceKey<PaintingVariant> id, final int width, final int height, final boolean hasAuthor
   ) {
      context.register(
         id,
         new PaintingVariant(
            width,
            height,
            id.identifier(),
            Optional.of(Component.translatable(id.identifier().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW)),
            hasAuthor
               ? Optional.of(Component.translatable(id.identifier().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY))
               : Optional.empty()
         )
      );
   }

   private static ResourceKey<PaintingVariant> create(final String name) {
      return ResourceKey.create(Registries.PAINTING_VARIANT, Identifier.withDefaultNamespace(name));
   }
}
