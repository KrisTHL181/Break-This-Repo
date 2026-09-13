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

   public static void bootstrap(BootstrapContext<PaintingVariant> p_453553_) {
      register(p_453553_, KEBAB, 1, 1);
      register(p_453553_, AZTEC, 1, 1);
      register(p_453553_, ALBAN, 1, 1);
      register(p_453553_, AZTEC2, 1, 1);
      register(p_453553_, BOMB, 1, 1);
      register(p_453553_, PLANT, 1, 1);
      register(p_453553_, WASTELAND, 1, 1);
      register(p_453553_, POOL, 2, 1);
      register(p_453553_, COURBET, 2, 1);
      register(p_453553_, SEA, 2, 1);
      register(p_453553_, SUNSET, 2, 1);
      register(p_453553_, CREEBET, 2, 1);
      register(p_453553_, WANDERER, 1, 2);
      register(p_453553_, GRAHAM, 1, 2);
      register(p_453553_, MATCH, 2, 2);
      register(p_453553_, BUST, 2, 2);
      register(p_453553_, STAGE, 2, 2);
      register(p_453553_, VOID, 2, 2);
      register(p_453553_, SKULL_AND_ROSES, 2, 2);
      register(p_453553_, WITHER, 2, 2, false);
      register(p_453553_, FIGHTERS, 4, 2);
      register(p_453553_, POINTER, 4, 4);
      register(p_453553_, PIGSCENE, 4, 4);
      register(p_453553_, BURNING_SKULL, 4, 4);
      register(p_453553_, SKELETON, 4, 3);
      register(p_453553_, EARTH, 2, 2, false);
      register(p_453553_, WIND, 2, 2, false);
      register(p_453553_, WATER, 2, 2, false);
      register(p_453553_, FIRE, 2, 2, false);
      register(p_453553_, DONKEY_KONG, 4, 3);
      register(p_453553_, BAROQUE, 2, 2);
      register(p_453553_, HUMBLE, 2, 2);
      register(p_453553_, MEDITATIVE, 1, 1);
      register(p_453553_, PRAIRIE_RIDE, 1, 2);
      register(p_453553_, UNPACKED, 4, 4);
      register(p_453553_, BACKYARD, 3, 4);
      register(p_453553_, BOUQUET, 3, 3);
      register(p_453553_, CAVEBIRD, 3, 3);
      register(p_453553_, CHANGING, 4, 2);
      register(p_453553_, COTAN, 3, 3);
      register(p_453553_, ENDBOSS, 3, 3);
      register(p_453553_, FERN, 3, 3);
      register(p_453553_, FINDING, 4, 2);
      register(p_453553_, LOWMIST, 4, 2);
      register(p_453553_, ORB, 4, 4);
      register(p_453553_, OWLEMONS, 3, 3);
      register(p_453553_, PASSAGE, 4, 2);
      register(p_453553_, POND, 3, 4);
      register(p_453553_, SUNFLOWERS, 3, 3);
      register(p_453553_, TIDES, 3, 3);
      register(p_453553_, DENNIS, 3, 3);
   }

   private static void register(BootstrapContext<PaintingVariant> p_451235_, ResourceKey<PaintingVariant> p_453622_, int p_457785_, int p_458756_) {
      register(p_451235_, p_453622_, p_457785_, p_458756_, true);
   }

   private static void register(
      BootstrapContext<PaintingVariant> p_458267_, ResourceKey<PaintingVariant> p_455445_, int p_460332_, int p_458770_, boolean p_452638_
   ) {
      p_458267_.register(
         p_455445_,
         new PaintingVariant(
            p_460332_,
            p_458770_,
            p_455445_.identifier(),
            Optional.of(Component.translatable(p_455445_.identifier().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW)),
            p_452638_
               ? Optional.of(Component.translatable(p_455445_.identifier().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY))
               : Optional.empty()
         )
      );
   }

   private static ResourceKey<PaintingVariant> create(String p_460846_) {
      return ResourceKey.create(Registries.PAINTING_VARIANT, Identifier.withDefaultNamespace(p_460846_));
   }
}
