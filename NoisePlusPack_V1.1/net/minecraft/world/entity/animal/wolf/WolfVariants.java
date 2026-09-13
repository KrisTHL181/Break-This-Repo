package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
   public static final ResourceKey<WolfVariant> PALE = createKey("pale");
   public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
   public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
   public static final ResourceKey<WolfVariant> BLACK = createKey("black");
   public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
   public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
   public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
   public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
   public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
   public static final ResourceKey<WolfVariant> DEFAULT = PALE;

   private static ResourceKey<WolfVariant> createKey(String p_392253_) {
      return ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace(p_392253_));
   }

   private static void register(BootstrapContext<WolfVariant> p_393304_, ResourceKey<WolfVariant> p_395007_, String p_391332_, ResourceKey<Biome> p_395826_) {
      register(p_393304_, p_395007_, p_391332_, highPrioBiome(HolderSet.direct(p_393304_.lookup(Registries.BIOME).getOrThrow(p_395826_))));
   }

   private static void register(BootstrapContext<WolfVariant> p_391328_, ResourceKey<WolfVariant> p_397780_, String p_392447_, TagKey<Biome> p_391941_) {
      register(p_391328_, p_397780_, p_392447_, highPrioBiome(p_391328_.lookup(Registries.BIOME).getOrThrow(p_391941_)));
   }

   private static SpawnPrioritySelectors highPrioBiome(HolderSet<Biome> p_397468_) {
      return SpawnPrioritySelectors.single(new BiomeCheck(p_397468_), 1);
   }

   private static void register(BootstrapContext<WolfVariant> p_395425_, ResourceKey<WolfVariant> p_392059_, String p_392274_, SpawnPrioritySelectors p_392241_) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/wolf/" + p_392274_);
      Identifier identifier1 = Identifier.withDefaultNamespace("entity/wolf/" + p_392274_ + "_tame");
      Identifier identifier2 = Identifier.withDefaultNamespace("entity/wolf/" + p_392274_ + "_angry");
      p_395425_.register(
         p_392059_,
         new WolfVariant(
            new WolfVariant.AssetInfo(
               new ClientAsset.ResourceTexture(identifier), new ClientAsset.ResourceTexture(identifier1), new ClientAsset.ResourceTexture(identifier2)
            ),
            p_392241_
         )
      );
   }

   public static void bootstrap(BootstrapContext<WolfVariant> p_395773_) {
      register(p_395773_, PALE, "wolf", SpawnPrioritySelectors.fallback(0));
      register(p_395773_, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
      register(p_395773_, SNOWY, "wolf_snowy", Biomes.GROVE);
      register(p_395773_, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
      register(p_395773_, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
      register(p_395773_, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
      register(p_395773_, WOODS, "wolf_woods", Biomes.FOREST);
      register(p_395773_, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
      register(p_395773_, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
   }
}
