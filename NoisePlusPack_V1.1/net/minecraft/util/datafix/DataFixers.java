package net.minecraft.util.datafix;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixerBuilder.Result;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.AbstractArrowPickupFix;
import net.minecraft.util.datafix.fixes.AddFieldFix;
import net.minecraft.util.datafix.fixes.AddFlagIfNotPresentFix;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.AdvancementsFix;
import net.minecraft.util.datafix.fixes.AdvancementsRenameFix;
import net.minecraft.util.datafix.fixes.AreaEffectCloudDurationScaleFix;
import net.minecraft.util.datafix.fixes.AreaEffectCloudPotionFix;
import net.minecraft.util.datafix.fixes.AttributeIdPrefixFix;
import net.minecraft.util.datafix.fixes.AttributeModifierIdFix;
import net.minecraft.util.datafix.fixes.AttributesRenameLegacy;
import net.minecraft.util.datafix.fixes.BannerEntityCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.BannerPatternFormatFix;
import net.minecraft.util.datafix.fixes.BedItemColorFix;
import net.minecraft.util.datafix.fixes.BeehiveFieldRenameFix;
import net.minecraft.util.datafix.fixes.BiomeFix;
import net.minecraft.util.datafix.fixes.BitStorageAlignFix;
import net.minecraft.util.datafix.fixes.BlendingDataFix;
import net.minecraft.util.datafix.fixes.BlendingDataRemoveFromNetherEndFix;
import net.minecraft.util.datafix.fixes.BlockEntityBannerColorFix;
import net.minecraft.util.datafix.fixes.BlockEntityBlockStateFix;
import net.minecraft.util.datafix.fixes.BlockEntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.BlockEntityFurnaceBurnTimeFix;
import net.minecraft.util.datafix.fixes.BlockEntityIdFix;
import net.minecraft.util.datafix.fixes.BlockEntityJukeboxFix;
import net.minecraft.util.datafix.fixes.BlockEntityKeepPacked;
import net.minecraft.util.datafix.fixes.BlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.BlockEntityShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.BlockEntitySignDoubleSidedEditableTextFix;
import net.minecraft.util.datafix.fixes.BlockEntityUUIDFix;
import net.minecraft.util.datafix.fixes.BlockNameFlatteningFix;
import net.minecraft.util.datafix.fixes.BlockPosFormatAndRenamesFix;
import net.minecraft.util.datafix.fixes.BlockPropertyRenameAndFix;
import net.minecraft.util.datafix.fixes.BlockRenameFix;
import net.minecraft.util.datafix.fixes.BlockStateStructureTemplateFix;
import net.minecraft.util.datafix.fixes.BoatSplitFix;
import net.minecraft.util.datafix.fixes.CarvingStepRemoveFix;
import net.minecraft.util.datafix.fixes.CatTypeFix;
import net.minecraft.util.datafix.fixes.CauldronRenameFix;
import net.minecraft.util.datafix.fixes.CavesAndCliffsRenames;
import net.minecraft.util.datafix.fixes.ChestedHorsesInventoryZeroIndexingFix;
import net.minecraft.util.datafix.fixes.ChunkBedBlockEntityInjecterFix;
import net.minecraft.util.datafix.fixes.ChunkBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteIgnoredLightDataFix;
import net.minecraft.util.datafix.fixes.ChunkDeleteLightFix;
import net.minecraft.util.datafix.fixes.ChunkHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.ChunkLightRemoveFix;
import net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix;
import net.minecraft.util.datafix.fixes.ChunkProtoTickListFix;
import net.minecraft.util.datafix.fixes.ChunkRenamesFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix;
import net.minecraft.util.datafix.fixes.ChunkStatusFix2;
import net.minecraft.util.datafix.fixes.ChunkStructuresTemplateRenameFix;
import net.minecraft.util.datafix.fixes.ChunkTicketUnpackPosFix;
import net.minecraft.util.datafix.fixes.ChunkToProtochunkFix;
import net.minecraft.util.datafix.fixes.ColorlessShulkerEntityFix;
import net.minecraft.util.datafix.fixes.ContainerBlockEntityLockPredicateFix;
import net.minecraft.util.datafix.fixes.CopperGolemWeatherStateFix;
import net.minecraft.util.datafix.fixes.CriteriaRenameFix;
import net.minecraft.util.datafix.fixes.CustomModelDataExpandFix;
import net.minecraft.util.datafix.fixes.DebugProfileOverlayReferenceFix;
import net.minecraft.util.datafix.fixes.DecoratedPotFieldRenameFix;
import net.minecraft.util.datafix.fixes.DropChancesFormatFix;
import net.minecraft.util.datafix.fixes.DropInvalidSignDataFix;
import net.minecraft.util.datafix.fixes.DyeItemRenameFix;
import net.minecraft.util.datafix.fixes.EffectDurationFix;
import net.minecraft.util.datafix.fixes.EmptyItemInHotbarFix;
import net.minecraft.util.datafix.fixes.EmptyItemInVillagerTradeFix;
import net.minecraft.util.datafix.fixes.EntityArmorStandSilentFix;
import net.minecraft.util.datafix.fixes.EntityAttributeBaseValueFix;
import net.minecraft.util.datafix.fixes.EntityBlockStateFix;
import net.minecraft.util.datafix.fixes.EntityBrushableBlockFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityCatSplitFix;
import net.minecraft.util.datafix.fixes.EntityCodSalmonFix;
import net.minecraft.util.datafix.fixes.EntityCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.EntityElderGuardianSplitFix;
import net.minecraft.util.datafix.fixes.EntityEquipmentToArmorAndHandFix;
import net.minecraft.util.datafix.fixes.EntityFallDistanceFloatToDoubleFix;
import net.minecraft.util.datafix.fixes.EntityFieldsRenameFix;
import net.minecraft.util.datafix.fixes.EntityGoatMissingStateFix;
import net.minecraft.util.datafix.fixes.EntityHealthFix;
import net.minecraft.util.datafix.fixes.EntityHorseSaddleFix;
import net.minecraft.util.datafix.fixes.EntityHorseSplitFix;
import net.minecraft.util.datafix.fixes.EntityIdFix;
import net.minecraft.util.datafix.fixes.EntityItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityMinecartIdentifiersFix;
import net.minecraft.util.datafix.fixes.EntityPaintingItemFrameDirectionFix;
import net.minecraft.util.datafix.fixes.EntityPaintingMotiveFix;
import net.minecraft.util.datafix.fixes.EntityProjectileOwnerFix;
import net.minecraft.util.datafix.fixes.EntityPufferfishRenameFix;
import net.minecraft.util.datafix.fixes.EntityRavagerRenameFix;
import net.minecraft.util.datafix.fixes.EntityRedundantChanceTagsFix;
import net.minecraft.util.datafix.fixes.EntityRidingToPassengersFix;
import net.minecraft.util.datafix.fixes.EntitySalmonSizeFix;
import net.minecraft.util.datafix.fixes.EntityShulkerColorFix;
import net.minecraft.util.datafix.fixes.EntityShulkerRotationFix;
import net.minecraft.util.datafix.fixes.EntitySkeletonSplitFix;
import net.minecraft.util.datafix.fixes.EntitySpawnerItemVariantComponentFix;
import net.minecraft.util.datafix.fixes.EntityStringUuidFix;
import net.minecraft.util.datafix.fixes.EntityTheRenameningFix;
import net.minecraft.util.datafix.fixes.EntityTippedArrowFix;
import net.minecraft.util.datafix.fixes.EntityUUIDFix;
import net.minecraft.util.datafix.fixes.EntityVariantFix;
import net.minecraft.util.datafix.fixes.EntityWolfColorFix;
import net.minecraft.util.datafix.fixes.EntityZombieSplitFix;
import net.minecraft.util.datafix.fixes.EntityZombieVillagerTypeFix;
import net.minecraft.util.datafix.fixes.EntityZombifiedPiglinRenameFix;
import net.minecraft.util.datafix.fixes.EquipmentFormatFix;
import net.minecraft.util.datafix.fixes.EquippableAssetRenameFix;
import net.minecraft.util.datafix.fixes.FeatureFlagRemoveFix;
import net.minecraft.util.datafix.fixes.FilteredBooksFix;
import net.minecraft.util.datafix.fixes.FilteredSignsFix;
import net.minecraft.util.datafix.fixes.FireResistantToDamageResistantComponentFix;
import net.minecraft.util.datafix.fixes.FixProjectileStoredItem;
import net.minecraft.util.datafix.fixes.FixWolfHealth;
import net.minecraft.util.datafix.fixes.FoodToConsumableFix;
import net.minecraft.util.datafix.fixes.ForcePoiRebuild;
import net.minecraft.util.datafix.fixes.ForcedChunkToTicketFix;
import net.minecraft.util.datafix.fixes.FurnaceRecipeFix;
import net.minecraft.util.datafix.fixes.GameRuleRegistryFix;
import net.minecraft.util.datafix.fixes.GoatHornIdFix;
import net.minecraft.util.datafix.fixes.GossipUUIDFix;
import net.minecraft.util.datafix.fixes.HeightmapRenamingFix;
import net.minecraft.util.datafix.fixes.HorseBodyArmorItemFix;
import net.minecraft.util.datafix.fixes.IglooMetadataRemovalFix;
import net.minecraft.util.datafix.fixes.InlineBlockPosFormatFix;
import net.minecraft.util.datafix.fixes.InvalidBlockEntityLockFix;
import net.minecraft.util.datafix.fixes.InvalidLockComponentFix;
import net.minecraft.util.datafix.fixes.ItemBannerColorFix;
import net.minecraft.util.datafix.fixes.ItemCustomNameToComponentFix;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.util.datafix.fixes.ItemLoreFix;
import net.minecraft.util.datafix.fixes.ItemPotionFix;
import net.minecraft.util.datafix.fixes.ItemRenameFix;
import net.minecraft.util.datafix.fixes.ItemShulkerBoxColorFix;
import net.minecraft.util.datafix.fixes.ItemSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.ItemStackCustomNameToOverrideComponentFix;
import net.minecraft.util.datafix.fixes.ItemStackEnchantmentNamesFix;
import net.minecraft.util.datafix.fixes.ItemStackMapIdFix;
import net.minecraft.util.datafix.fixes.ItemStackSpawnEggFix;
import net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix;
import net.minecraft.util.datafix.fixes.ItemStackUUIDFix;
import net.minecraft.util.datafix.fixes.ItemWaterPotionFix;
import net.minecraft.util.datafix.fixes.JigsawPropertiesFix;
import net.minecraft.util.datafix.fixes.JigsawRotationFix;
import net.minecraft.util.datafix.fixes.JukeboxTicksSinceSongStartedFix;
import net.minecraft.util.datafix.fixes.LeavesFix;
import net.minecraft.util.datafix.fixes.LegacyDimensionIdFix;
import net.minecraft.util.datafix.fixes.LegacyDragonFightFix;
import net.minecraft.util.datafix.fixes.LegacyHoverEventFix;
import net.minecraft.util.datafix.fixes.LegacyWorldBorderFix;
import net.minecraft.util.datafix.fixes.LevelDataGeneratorOptionsFix;
import net.minecraft.util.datafix.fixes.LevelFlatGeneratorInfoFix;
import net.minecraft.util.datafix.fixes.LevelLegacyWorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.LevelUUIDFix;
import net.minecraft.util.datafix.fixes.LockComponentPredicateFix;
import net.minecraft.util.datafix.fixes.LodestoneCompassComponentFix;
import net.minecraft.util.datafix.fixes.MapBannerBlockPosFormatFix;
import net.minecraft.util.datafix.fixes.MapIdFix;
import net.minecraft.util.datafix.fixes.MemoryExpiryDataFix;
import net.minecraft.util.datafix.fixes.MissingDimensionFix;
import net.minecraft.util.datafix.fixes.MobEffectIdFix;
import net.minecraft.util.datafix.fixes.MobSpawnerEntityIdentifiersFix;
import net.minecraft.util.datafix.fixes.NamedEntityConvertUncheckedFix;
import net.minecraft.util.datafix.fixes.NamedEntityWriteReadFix;
import net.minecraft.util.datafix.fixes.NamespacedTypeRenameFix;
import net.minecraft.util.datafix.fixes.NewVillageFix;
import net.minecraft.util.datafix.fixes.ObjectiveRenderTypeFix;
import net.minecraft.util.datafix.fixes.OminousBannerBlockEntityRenameFix;
import net.minecraft.util.datafix.fixes.OminousBannerRarityFix;
import net.minecraft.util.datafix.fixes.OminousBannerRenameFix;
import net.minecraft.util.datafix.fixes.OptionsAccessibilityOnboardFix;
import net.minecraft.util.datafix.fixes.OptionsAddTextBackgroundFix;
import net.minecraft.util.datafix.fixes.OptionsAmbientOcclusionFix;
import net.minecraft.util.datafix.fixes.OptionsFancyGraphicsToGraphicsModeFix;
import net.minecraft.util.datafix.fixes.OptionsForceVBOFix;
import net.minecraft.util.datafix.fixes.OptionsGraphicsModeSplitFix;
import net.minecraft.util.datafix.fixes.OptionsKeyLwjgl3Fix;
import net.minecraft.util.datafix.fixes.OptionsKeyTranslationFix;
import net.minecraft.util.datafix.fixes.OptionsLowerCaseLanguageFix;
import net.minecraft.util.datafix.fixes.OptionsMenuBlurrinessFix;
import net.minecraft.util.datafix.fixes.OptionsMusicToastFix;
import net.minecraft.util.datafix.fixes.OptionsProgrammerArtFix;
import net.minecraft.util.datafix.fixes.OptionsRenameFieldFix;
import net.minecraft.util.datafix.fixes.OptionsSetGraphicsPresetToCustomFix;
import net.minecraft.util.datafix.fixes.OverreachingTickFix;
import net.minecraft.util.datafix.fixes.ParticleUnflatteningFix;
import net.minecraft.util.datafix.fixes.PlayerEquipmentFix;
import net.minecraft.util.datafix.fixes.PlayerHeadBlockProfileFix;
import net.minecraft.util.datafix.fixes.PlayerRespawnDataFix;
import net.minecraft.util.datafix.fixes.PlayerUUIDFix;
import net.minecraft.util.datafix.fixes.PoiTypeRemoveFix;
import net.minecraft.util.datafix.fixes.PoiTypeRenameFix;
import net.minecraft.util.datafix.fixes.PrimedTntBlockStateFixer;
import net.minecraft.util.datafix.fixes.ProjectileStoredWeaponFix;
import net.minecraft.util.datafix.fixes.RaidRenamesDataFix;
import net.minecraft.util.datafix.fixes.RandomSequenceSettingsFix;
import net.minecraft.util.datafix.fixes.RecipesFix;
import net.minecraft.util.datafix.fixes.RecipesRenameningFix;
import net.minecraft.util.datafix.fixes.RedstoneWireConnectionsFix;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.fixes.RemapChunkStatusFix;
import net.minecraft.util.datafix.fixes.RemoveBlockEntityTagFix;
import net.minecraft.util.datafix.fixes.RemoveEmptyItemInBrushableBlockFix;
import net.minecraft.util.datafix.fixes.RemoveGolemGossipFix;
import net.minecraft.util.datafix.fixes.RenameEnchantmentsFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFansFix;
import net.minecraft.util.datafix.fixes.RenamedCoralFix;
import net.minecraft.util.datafix.fixes.ReorganizePoi;
import net.minecraft.util.datafix.fixes.SaddleEquipmentSlotFix;
import net.minecraft.util.datafix.fixes.SavedDataFeaturePoolElementFix;
import net.minecraft.util.datafix.fixes.SavedDataUUIDFix;
import net.minecraft.util.datafix.fixes.ScoreboardDisplayNameFix;
import net.minecraft.util.datafix.fixes.ScoreboardDisplaySlotFix;
import net.minecraft.util.datafix.fixes.SignTextStrictJsonFix;
import net.minecraft.util.datafix.fixes.SpawnerDataFix;
import net.minecraft.util.datafix.fixes.StatsCounterFix;
import net.minecraft.util.datafix.fixes.StatsRenameFix;
import net.minecraft.util.datafix.fixes.StriderGravityFix;
import net.minecraft.util.datafix.fixes.StructureReferenceCountFix;
import net.minecraft.util.datafix.fixes.StructureSettingsFlattenFix;
import net.minecraft.util.datafix.fixes.StructuresBecomeConfiguredFix;
import net.minecraft.util.datafix.fixes.TextComponentHoverAndClickEventFix;
import net.minecraft.util.datafix.fixes.TextComponentStringifiedFlagsFix;
import net.minecraft.util.datafix.fixes.ThrownPotionSplitFix;
import net.minecraft.util.datafix.fixes.TippedArrowPotionToItemFix;
import net.minecraft.util.datafix.fixes.TooltipDisplayComponentFix;
import net.minecraft.util.datafix.fixes.TrappedChestBlockEntityFix;
import net.minecraft.util.datafix.fixes.TrialSpawnerConfigFix;
import net.minecraft.util.datafix.fixes.TrialSpawnerConfigInRegistryFix;
import net.minecraft.util.datafix.fixes.TridentAnimationFix;
import net.minecraft.util.datafix.fixes.UnflattenTextComponentFix;
import net.minecraft.util.datafix.fixes.VariantRenameFix;
import net.minecraft.util.datafix.fixes.VillagerDataFix;
import net.minecraft.util.datafix.fixes.VillagerFollowRangeFix;
import net.minecraft.util.datafix.fixes.VillagerRebuildLevelAndXpFix;
import net.minecraft.util.datafix.fixes.VillagerSetCanPickUpLootFix;
import net.minecraft.util.datafix.fixes.VillagerTradeFix;
import net.minecraft.util.datafix.fixes.WallPropertyFix;
import net.minecraft.util.datafix.fixes.WeaponSmithChestLootTableFix;
import net.minecraft.util.datafix.fixes.WorldBorderWarningTimeFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsDisallowOldCustomWorldsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsFix;
import net.minecraft.util.datafix.fixes.WorldGenSettingsHeightAndBiomeFix;
import net.minecraft.util.datafix.fixes.WorldSpawnDataFix;
import net.minecraft.util.datafix.fixes.WriteAndReadFix;
import net.minecraft.util.datafix.fixes.WrittenBookPagesStrictJsonFix;
import net.minecraft.util.datafix.fixes.ZombieVillagerRebuildXpFix;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V100;
import net.minecraft.util.datafix.schemas.V102;
import net.minecraft.util.datafix.schemas.V1022;
import net.minecraft.util.datafix.schemas.V106;
import net.minecraft.util.datafix.schemas.V107;
import net.minecraft.util.datafix.schemas.V1125;
import net.minecraft.util.datafix.schemas.V135;
import net.minecraft.util.datafix.schemas.V143;
import net.minecraft.util.datafix.schemas.V1451;
import net.minecraft.util.datafix.schemas.V1451_1;
import net.minecraft.util.datafix.schemas.V1451_2;
import net.minecraft.util.datafix.schemas.V1451_3;
import net.minecraft.util.datafix.schemas.V1451_4;
import net.minecraft.util.datafix.schemas.V1451_5;
import net.minecraft.util.datafix.schemas.V1451_6;
import net.minecraft.util.datafix.schemas.V1458;
import net.minecraft.util.datafix.schemas.V1460;
import net.minecraft.util.datafix.schemas.V1466;
import net.minecraft.util.datafix.schemas.V1470;
import net.minecraft.util.datafix.schemas.V1481;
import net.minecraft.util.datafix.schemas.V1483;
import net.minecraft.util.datafix.schemas.V1486;
import net.minecraft.util.datafix.schemas.V1488;
import net.minecraft.util.datafix.schemas.V1510;
import net.minecraft.util.datafix.schemas.V1800;
import net.minecraft.util.datafix.schemas.V1801;
import net.minecraft.util.datafix.schemas.V1904;
import net.minecraft.util.datafix.schemas.V1906;
import net.minecraft.util.datafix.schemas.V1909;
import net.minecraft.util.datafix.schemas.V1920;
import net.minecraft.util.datafix.schemas.V1928;
import net.minecraft.util.datafix.schemas.V1929;
import net.minecraft.util.datafix.schemas.V1931;
import net.minecraft.util.datafix.schemas.V2100;
import net.minecraft.util.datafix.schemas.V2501;
import net.minecraft.util.datafix.schemas.V2502;
import net.minecraft.util.datafix.schemas.V2505;
import net.minecraft.util.datafix.schemas.V2509;
import net.minecraft.util.datafix.schemas.V2511_1;
import net.minecraft.util.datafix.schemas.V2519;
import net.minecraft.util.datafix.schemas.V2522;
import net.minecraft.util.datafix.schemas.V2551;
import net.minecraft.util.datafix.schemas.V2568;
import net.minecraft.util.datafix.schemas.V2571;
import net.minecraft.util.datafix.schemas.V2684;
import net.minecraft.util.datafix.schemas.V2686;
import net.minecraft.util.datafix.schemas.V2688;
import net.minecraft.util.datafix.schemas.V2704;
import net.minecraft.util.datafix.schemas.V2707;
import net.minecraft.util.datafix.schemas.V2831;
import net.minecraft.util.datafix.schemas.V2832;
import net.minecraft.util.datafix.schemas.V2842;
import net.minecraft.util.datafix.schemas.V3076;
import net.minecraft.util.datafix.schemas.V3078;
import net.minecraft.util.datafix.schemas.V3081;
import net.minecraft.util.datafix.schemas.V3082;
import net.minecraft.util.datafix.schemas.V3083;
import net.minecraft.util.datafix.schemas.V3202;
import net.minecraft.util.datafix.schemas.V3203;
import net.minecraft.util.datafix.schemas.V3204;
import net.minecraft.util.datafix.schemas.V3325;
import net.minecraft.util.datafix.schemas.V3326;
import net.minecraft.util.datafix.schemas.V3327;
import net.minecraft.util.datafix.schemas.V3328;
import net.minecraft.util.datafix.schemas.V3438;
import net.minecraft.util.datafix.schemas.V3439;
import net.minecraft.util.datafix.schemas.V3439_1;
import net.minecraft.util.datafix.schemas.V3448;
import net.minecraft.util.datafix.schemas.V3682;
import net.minecraft.util.datafix.schemas.V3683;
import net.minecraft.util.datafix.schemas.V3685;
import net.minecraft.util.datafix.schemas.V3689;
import net.minecraft.util.datafix.schemas.V3799;
import net.minecraft.util.datafix.schemas.V3807;
import net.minecraft.util.datafix.schemas.V3808;
import net.minecraft.util.datafix.schemas.V3808_1;
import net.minecraft.util.datafix.schemas.V3808_2;
import net.minecraft.util.datafix.schemas.V3813;
import net.minecraft.util.datafix.schemas.V3816;
import net.minecraft.util.datafix.schemas.V3818;
import net.minecraft.util.datafix.schemas.V3818_3;
import net.minecraft.util.datafix.schemas.V3818_4;
import net.minecraft.util.datafix.schemas.V3818_5;
import net.minecraft.util.datafix.schemas.V3825;
import net.minecraft.util.datafix.schemas.V3938;
import net.minecraft.util.datafix.schemas.V4059;
import net.minecraft.util.datafix.schemas.V4067;
import net.minecraft.util.datafix.schemas.V4070;
import net.minecraft.util.datafix.schemas.V4071;
import net.minecraft.util.datafix.schemas.V4290;
import net.minecraft.util.datafix.schemas.V4292;
import net.minecraft.util.datafix.schemas.V4300;
import net.minecraft.util.datafix.schemas.V4301;
import net.minecraft.util.datafix.schemas.V4302;
import net.minecraft.util.datafix.schemas.V4306;
import net.minecraft.util.datafix.schemas.V4307;
import net.minecraft.util.datafix.schemas.V4312;
import net.minecraft.util.datafix.schemas.V4420;
import net.minecraft.util.datafix.schemas.V4421;
import net.minecraft.util.datafix.schemas.V4531;
import net.minecraft.util.datafix.schemas.V4532;
import net.minecraft.util.datafix.schemas.V4533;
import net.minecraft.util.datafix.schemas.V4543;
import net.minecraft.util.datafix.schemas.V4648;
import net.minecraft.util.datafix.schemas.V4656;
import net.minecraft.util.datafix.schemas.V501;
import net.minecraft.util.datafix.schemas.V700;
import net.minecraft.util.datafix.schemas.V701;
import net.minecraft.util.datafix.schemas.V702;
import net.minecraft.util.datafix.schemas.V703;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V808;
import net.minecraft.util.datafix.schemas.V99;

public class DataFixers {
   private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
   private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;
   private static final Result DATA_FIXER = createFixerUpper();
   public static final int BLENDING_VERSION = 4295;

   private DataFixers() {
   }

   public static DataFixer getDataFixer() {
      return DATA_FIXER.fixer();
   }

   private static Result createFixerUpper() {
      DataFixerBuilder datafixerbuilder = new DataFixerBuilder(SharedConstants.getCurrentVersion().dataVersion().version());
      addFixers(datafixerbuilder);
      return datafixerbuilder.build();
   }

   public static CompletableFuture<?> optimize(Set<TypeReference> p_344748_) {
      if (p_344748_.isEmpty()) {
         return CompletableFuture.completedFuture(null);
      }

      Executor executor = Executors.newSingleThreadExecutor(
         new ThreadFactoryBuilder().setNameFormat("Datafixer Bootstrap").setDaemon(true).setPriority(1).build()
      );
      return DATA_FIXER.optimize(p_344748_, executor);
   }

   private static void addFixers(DataFixerBuilder p_14514_) {
      p_14514_.addSchema(99, V99::new);
      Schema schema = p_14514_.addSchema(100, V100::new);
      p_14514_.addFixer(new EntityEquipmentToArmorAndHandFix(schema));
      Schema schema1 = p_14514_.addSchema(101, SAME);
      p_14514_.addFixer(new VillagerSetCanPickUpLootFix(schema1));
      Schema schema2 = p_14514_.addSchema(102, V102::new);
      p_14514_.addFixer(new ItemIdFix(schema2, true));
      p_14514_.addFixer(new ItemPotionFix(schema2, false));
      Schema schema3 = p_14514_.addSchema(105, SAME);
      p_14514_.addFixer(new ItemSpawnEggFix(schema3, true));
      Schema schema4 = p_14514_.addSchema(106, V106::new);
      p_14514_.addFixer(new MobSpawnerEntityIdentifiersFix(schema4, true));
      Schema schema5 = p_14514_.addSchema(107, V107::new);
      p_14514_.addFixer(new EntityMinecartIdentifiersFix(schema5));
      Schema schema6 = p_14514_.addSchema(108, SAME);
      p_14514_.addFixer(new EntityStringUuidFix(schema6, true));
      Schema schema7 = p_14514_.addSchema(109, SAME);
      p_14514_.addFixer(new EntityHealthFix(schema7, true));
      Schema schema8 = p_14514_.addSchema(110, SAME);
      p_14514_.addFixer(new EntityHorseSaddleFix(schema8, true));
      Schema schema9 = p_14514_.addSchema(111, SAME);
      p_14514_.addFixer(new EntityPaintingItemFrameDirectionFix(schema9, true));
      Schema schema10 = p_14514_.addSchema(113, SAME);
      p_14514_.addFixer(new EntityRedundantChanceTagsFix(schema10, true));
      Schema schema11 = p_14514_.addSchema(135, V135::new);
      p_14514_.addFixer(new EntityRidingToPassengersFix(schema11, true));
      Schema schema12 = p_14514_.addSchema(143, V143::new);
      p_14514_.addFixer(new EntityTippedArrowFix(schema12, true));
      Schema schema13 = p_14514_.addSchema(147, SAME);
      p_14514_.addFixer(new EntityArmorStandSilentFix(schema13, true));
      Schema schema14 = p_14514_.addSchema(165, SAME);
      p_14514_.addFixer(new SignTextStrictJsonFix(schema14));
      p_14514_.addFixer(new WrittenBookPagesStrictJsonFix(schema14));
      Schema schema15 = p_14514_.addSchema(501, V501::new);
      p_14514_.addFixer(new AddNewChoices(schema15, "Add 1.10 entities fix", References.ENTITY));
      Schema schema16 = p_14514_.addSchema(502, SAME);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema16,
            "cooked_fished item renamer",
            p_14533_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14533_), "minecraft:cooked_fished") ? "minecraft:cooked_fish" : p_14533_
         )
      );
      p_14514_.addFixer(new EntityZombieVillagerTypeFix(schema16, false));
      Schema schema17 = p_14514_.addSchema(505, SAME);
      p_14514_.addFixer(new OptionsForceVBOFix(schema17, false));
      Schema schema18 = p_14514_.addSchema(700, V700::new);
      p_14514_.addFixer(new EntityElderGuardianSplitFix(schema18, true));
      Schema schema19 = p_14514_.addSchema(701, V701::new);
      p_14514_.addFixer(new EntitySkeletonSplitFix(schema19, true));
      Schema schema20 = p_14514_.addSchema(702, V702::new);
      p_14514_.addFixer(new EntityZombieSplitFix(schema20));
      Schema schema21 = p_14514_.addSchema(703, V703::new);
      p_14514_.addFixer(new EntityHorseSplitFix(schema21, true));
      Schema schema22 = p_14514_.addSchema(704, V704::new);
      p_14514_.addFixer(new BlockEntityIdFix(schema22, true));
      Schema schema23 = p_14514_.addSchema(705, V705::new);
      p_14514_.addFixer(new EntityIdFix(schema23, true));
      Schema schema24 = p_14514_.addSchema(804, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemBannerColorFix(schema24, true));
      Schema schema25 = p_14514_.addSchema(806, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemWaterPotionFix(schema25, false));
      Schema schema26 = p_14514_.addSchema(808, V808::new);
      p_14514_.addFixer(new AddNewChoices(schema26, "added shulker box", References.BLOCK_ENTITY));
      Schema schema27 = p_14514_.addSchema(808, 1, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityShulkerColorFix(schema27, false));
      Schema schema28 = p_14514_.addSchema(813, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemShulkerBoxColorFix(schema28, false));
      p_14514_.addFixer(new BlockEntityShulkerBoxColorFix(schema28, false));
      Schema schema29 = p_14514_.addSchema(816, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsLowerCaseLanguageFix(schema29, false));
      Schema schema30 = p_14514_.addSchema(820, SAME_NAMESPACED);
      p_14514_.addFixer(ItemRenameFix.create(schema30, "totem item renamer", createRenamer("minecraft:totem", "minecraft:totem_of_undying")));
      Schema schema31 = p_14514_.addSchema(1022, V1022::new);
      p_14514_.addFixer(new WriteAndReadFix(schema31, "added shoulder entities to players", References.PLAYER));
      Schema schema32 = p_14514_.addSchema(1125, V1125::new);
      p_14514_.addFixer(new ChunkBedBlockEntityInjecterFix(schema32, true));
      p_14514_.addFixer(new BedItemColorFix(schema32, false));
      Schema schema33 = p_14514_.addSchema(1344, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsKeyLwjgl3Fix(schema33, false));
      Schema schema34 = p_14514_.addSchema(1446, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsKeyTranslationFix(schema34, false));
      Schema schema35 = p_14514_.addSchema(1450, SAME_NAMESPACED);
      p_14514_.addFixer(new BlockStateStructureTemplateFix(schema35, false));
      Schema schema36 = p_14514_.addSchema(1451, V1451::new);
      p_14514_.addFixer(new AddNewChoices(schema36, "AddTrappedChestFix", References.BLOCK_ENTITY));
      Schema schema37 = p_14514_.addSchema(1451, 1, V1451_1::new);
      p_14514_.addFixer(new ChunkPalettedStorageFix(schema37, true));
      Schema schema38 = p_14514_.addSchema(1451, 2, V1451_2::new);
      p_14514_.addFixer(new BlockEntityBlockStateFix(schema38, true));
      Schema schema39 = p_14514_.addSchema(1451, 3, V1451_3::new);
      p_14514_.addFixer(new EntityBlockStateFix(schema39, true));
      p_14514_.addFixer(new ItemStackMapIdFix(schema39, false));
      Schema schema40 = p_14514_.addSchema(1451, 4, V1451_4::new);
      p_14514_.addFixer(new BlockNameFlatteningFix(schema40, true));
      p_14514_.addFixer(new ItemStackTheFlatteningFix(schema40, false));
      Schema schema41 = p_14514_.addSchema(1451, 5, V1451_5::new);
      p_14514_.addFixer(new RemoveBlockEntityTagFix(schema41, Set.of("minecraft:noteblock", "minecraft:flower_pot")));
      p_14514_.addFixer(new ItemStackSpawnEggFix(schema41, false, "minecraft:spawn_egg"));
      p_14514_.addFixer(new EntityWolfColorFix(schema41, false));
      p_14514_.addFixer(new BlockEntityBannerColorFix(schema41, false));
      p_14514_.addFixer(new LevelFlatGeneratorInfoFix(schema41, false));
      Schema schema42 = p_14514_.addSchema(1451, 6, V1451_6::new);
      p_14514_.addFixer(new StatsCounterFix(schema42, true));
      p_14514_.addFixer(new BlockEntityJukeboxFix(schema42, false));
      Schema schema43 = p_14514_.addSchema(1451, 7, SAME_NAMESPACED);
      p_14514_.addFixer(new VillagerTradeFix(schema43));
      Schema schema44 = p_14514_.addSchema(1456, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityItemFrameDirectionFix(schema44, false));
      Schema schema45 = p_14514_.addSchema(1458, V1458::new);
      p_14514_.addFixer(new EntityCustomNameToComponentFix(schema45));
      p_14514_.addFixer(new ItemCustomNameToComponentFix(schema45));
      p_14514_.addFixer(new BlockEntityCustomNameToComponentFix(schema45));
      Schema schema46 = p_14514_.addSchema(1460, V1460::new);
      p_14514_.addFixer(new EntityPaintingMotiveFix(schema46, false));
      Schema schema47 = p_14514_.addSchema(1466, V1466::new);
      p_14514_.addFixer(new AddNewChoices(schema47, "Add DUMMY block entity", References.BLOCK_ENTITY));
      p_14514_.addFixer(new ChunkToProtochunkFix(schema47, true));
      Schema schema48 = p_14514_.addSchema(1470, V1470::new);
      p_14514_.addFixer(new AddNewChoices(schema48, "Add 1.13 entities fix", References.ENTITY));
      Schema schema49 = p_14514_.addSchema(1474, SAME_NAMESPACED);
      p_14514_.addFixer(new ColorlessShulkerEntityFix(schema49, false));
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema49,
            "Colorless shulker block fixer",
            p_14531_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14531_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14531_
         )
      );
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema49,
            "Colorless shulker item fixer",
            p_14516_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_14516_), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : p_14516_
         )
      );
      Schema schema50 = p_14514_.addSchema(1475, SAME_NAMESPACED);
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema50, "Flowing fixer", createRenamer(ImmutableMap.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"))
         )
      );
      Schema schema51 = p_14514_.addSchema(1480, SAME_NAMESPACED);
      p_14514_.addFixer(BlockRenameFix.create(schema51, "Rename coral blocks", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      p_14514_.addFixer(ItemRenameFix.create(schema51, "Rename coral items", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      Schema schema52 = p_14514_.addSchema(1481, V1481::new);
      p_14514_.addFixer(new AddNewChoices(schema52, "Add conduit", References.BLOCK_ENTITY));
      Schema schema53 = p_14514_.addSchema(1483, V1483::new);
      p_14514_.addFixer(new EntityPufferfishRenameFix(schema53, true));
      p_14514_.addFixer(ItemRenameFix.create(schema53, "Rename pufferfish egg item", createRenamer(EntityPufferfishRenameFix.RENAMED_IDS)));
      Schema schema54 = p_14514_.addSchema(1484, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema54,
            "Rename seagrass items",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema54,
            "Rename seagrass blocks",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      p_14514_.addFixer(new HeightmapRenamingFix(schema54, false));
      Schema schema55 = p_14514_.addSchema(1486, V1486::new);
      p_14514_.addFixer(new EntityCodSalmonFix(schema55, true));
      p_14514_.addFixer(ItemRenameFix.create(schema55, "Rename cod/salmon egg items", createRenamer(EntityCodSalmonFix.RENAMED_EGG_IDS)));
      Schema schema56 = p_14514_.addSchema(1487, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema56,
            "Rename prismarine_brick(s)_* blocks",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:prismarine_bricks_slab",
                  "minecraft:prismarine_brick_slab",
                  "minecraft:prismarine_bricks_stairs",
                  "minecraft:prismarine_brick_stairs"
               )
            )
         )
      );
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema56,
            "Rename prismarine_brick(s)_* items",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:prismarine_bricks_slab",
                  "minecraft:prismarine_brick_slab",
                  "minecraft:prismarine_bricks_stairs",
                  "minecraft:prismarine_brick_stairs"
               )
            )
         )
      );
      Schema schema57 = p_14514_.addSchema(1488, V1488::new);
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema57, "Rename kelp/kelptop", createRenamer(ImmutableMap.of("minecraft:kelp_top", "minecraft:kelp", "minecraft:kelp", "minecraft:kelp_plant"))
         )
      );
      p_14514_.addFixer(ItemRenameFix.create(schema57, "Rename kelptop", createRenamer("minecraft:kelp_top", "minecraft:kelp")));
      p_14514_.addFixer(
         new NamedEntityWriteReadFix(schema57, true, "Command block block entity custom name fix", References.BLOCK_ENTITY, "minecraft:command_block") {
            @Override
            protected <T> Dynamic<T> fix(Dynamic<T> p_392945_) {
               return BlockEntityCustomNameToComponentFix.fixTagCustomName(p_392945_);
            }
         }
      );
      p_14514_.addFixer(
         new DataFix(schema57, false) {
            protected TypeRewriteRule makeRule() {
               Type<?> type = this.getInputSchema().getType(References.ENTITY);
               OpticFinder<String> opticfinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
               OpticFinder<?> opticfinder1 = type.findField("CustomName");
               OpticFinder<Pair<String, String>> opticfinder2 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
               return this.fixTypeEverywhereTyped(
                  "Command block minecart custom name fix",
                  type,
                  p_392849_ -> {
                     String s = p_392849_.getOptional(opticfinder).orElse("");
                     return !"minecraft:commandblock_minecart".equals(s)
                        ? p_392849_
                        : p_392849_.updateTyped(
                           opticfinder1,
                           p_395102_ -> p_395102_.update(opticfinder2, p_393406_ -> p_393406_.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
                        );
                  }
               );
            }
         }
      );
      p_14514_.addFixer(new IglooMetadataRemovalFix(schema57, false));
      Schema schema58 = p_14514_.addSchema(1490, SAME_NAMESPACED);
      p_14514_.addFixer(BlockRenameFix.create(schema58, "Rename melon_block", createRenamer("minecraft:melon_block", "minecraft:melon")));
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema58,
            "Rename melon_block/melon/speckled_melon",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:melon_block",
                  "minecraft:melon",
                  "minecraft:melon",
                  "minecraft:melon_slice",
                  "minecraft:speckled_melon",
                  "minecraft:glistering_melon_slice"
               )
            )
         )
      );
      Schema schema59 = p_14514_.addSchema(1492, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkStructuresTemplateRenameFix(schema59, false));
      Schema schema60 = p_14514_.addSchema(1494, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemStackEnchantmentNamesFix(schema60, false));
      Schema schema61 = p_14514_.addSchema(1496, SAME_NAMESPACED);
      p_14514_.addFixer(new LeavesFix(schema61, false));
      Schema schema62 = p_14514_.addSchema(1500, SAME_NAMESPACED);
      p_14514_.addFixer(new BlockEntityKeepPacked(schema62, false));
      Schema schema63 = p_14514_.addSchema(1501, SAME_NAMESPACED);
      p_14514_.addFixer(new AdvancementsFix(schema63, false));
      Schema schema64 = p_14514_.addSchema(1502, SAME_NAMESPACED);
      p_14514_.addFixer(new NamespacedTypeRenameFix(schema64, "Recipes fix", References.RECIPE, createRenamer(RecipesFix.RECIPES)));
      Schema schema65 = p_14514_.addSchema(1506, SAME_NAMESPACED);
      p_14514_.addFixer(new LevelDataGeneratorOptionsFix(schema65, false));
      Schema schema66 = p_14514_.addSchema(1510, V1510::new);
      p_14514_.addFixer(BlockRenameFix.create(schema66, "Block renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_BLOCKS)));
      p_14514_.addFixer(ItemRenameFix.create(schema66, "Item renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_ITEMS)));
      p_14514_.addFixer(new NamespacedTypeRenameFix(schema66, "Recipes renamening fix", References.RECIPE, createRenamer(RecipesRenameningFix.RECIPES)));
      p_14514_.addFixer(new EntityTheRenameningFix(schema66, true));
      p_14514_.addFixer(
         new StatsRenameFix(
            schema66,
            "SwimStatsRenameFix",
            ImmutableMap.of("minecraft:swim_one_cm", "minecraft:walk_on_water_one_cm", "minecraft:dive_one_cm", "minecraft:walk_under_water_one_cm")
         )
      );
      Schema schema67 = p_14514_.addSchema(1514, SAME_NAMESPACED);
      p_14514_.addFixer(new ScoreboardDisplayNameFix(schema67, "ObjectiveDisplayNameFix", References.OBJECTIVE));
      p_14514_.addFixer(new ScoreboardDisplayNameFix(schema67, "TeamDisplayNameFix", References.TEAM));
      p_14514_.addFixer(new ObjectiveRenderTypeFix(schema67));
      Schema schema68 = p_14514_.addSchema(1515, SAME_NAMESPACED);
      p_14514_.addFixer(BlockRenameFix.create(schema68, "Rename coral fan blocks", createRenamer(RenamedCoralFansFix.RENAMED_IDS)));
      Schema schema69 = p_14514_.addSchema(1624, SAME_NAMESPACED);
      p_14514_.addFixer(new TrappedChestBlockEntityFix(schema69, false));
      Schema schema70 = p_14514_.addSchema(1800, V1800::new);
      p_14514_.addFixer(new AddNewChoices(schema70, "Added 1.14 mobs fix", References.ENTITY));
      p_14514_.addFixer(ItemRenameFix.create(schema70, "Rename dye items", createRenamer(DyeItemRenameFix.RENAMED_IDS)));
      Schema schema71 = p_14514_.addSchema(1801, V1801::new);
      p_14514_.addFixer(new AddNewChoices(schema71, "Added Illager Beast", References.ENTITY));
      Schema schema72 = p_14514_.addSchema(1802, SAME_NAMESPACED);
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema72,
            "Rename sign blocks & stone slabs",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:stone_slab",
                  "minecraft:smooth_stone_slab",
                  "minecraft:sign",
                  "minecraft:oak_sign",
                  "minecraft:wall_sign",
                  "minecraft:oak_wall_sign"
               )
            )
         )
      );
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema72,
            "Rename sign item & stone slabs",
            createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign"))
         )
      );
      Schema schema73 = p_14514_.addSchema(1803, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemLoreFix(schema73));
      Schema schema74 = p_14514_.addSchema(1904, V1904::new);
      p_14514_.addFixer(new AddNewChoices(schema74, "Added Cats", References.ENTITY));
      p_14514_.addFixer(new EntityCatSplitFix(schema74, false));
      Schema schema75 = p_14514_.addSchema(1905, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkStatusFix(schema75, false));
      Schema schema76 = p_14514_.addSchema(1906, V1906::new);
      p_14514_.addFixer(new AddNewChoices(schema76, "Add POI Blocks", References.BLOCK_ENTITY));
      Schema schema77 = p_14514_.addSchema(1909, V1909::new);
      p_14514_.addFixer(new AddNewChoices(schema77, "Add jigsaw", References.BLOCK_ENTITY));
      Schema schema78 = p_14514_.addSchema(1911, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkStatusFix2(schema78, false));
      Schema schema79 = p_14514_.addSchema(1914, SAME_NAMESPACED);
      p_14514_.addFixer(new WeaponSmithChestLootTableFix(schema79, false));
      Schema schema80 = p_14514_.addSchema(1917, SAME_NAMESPACED);
      p_14514_.addFixer(new CatTypeFix(schema80, false));
      Schema schema81 = p_14514_.addSchema(1918, SAME_NAMESPACED);
      p_14514_.addFixer(new VillagerDataFix(schema81, "minecraft:villager"));
      p_14514_.addFixer(new VillagerDataFix(schema81, "minecraft:zombie_villager"));
      Schema schema82 = p_14514_.addSchema(1920, V1920::new);
      p_14514_.addFixer(new NewVillageFix(schema82, false));
      p_14514_.addFixer(new AddNewChoices(schema82, "Add campfire", References.BLOCK_ENTITY));
      Schema schema83 = p_14514_.addSchema(1925, SAME_NAMESPACED);
      p_14514_.addFixer(new MapIdFix(schema83));
      Schema schema84 = p_14514_.addSchema(1928, V1928::new);
      p_14514_.addFixer(new EntityRavagerRenameFix(schema84, true));
      p_14514_.addFixer(ItemRenameFix.create(schema84, "Rename ravager egg item", createRenamer(EntityRavagerRenameFix.RENAMED_IDS)));
      Schema schema85 = p_14514_.addSchema(1929, V1929::new);
      p_14514_.addFixer(new AddNewChoices(schema85, "Add Wandering Trader and Trader Llama", References.ENTITY));
      Schema schema86 = p_14514_.addSchema(1931, V1931::new);
      p_14514_.addFixer(new AddNewChoices(schema86, "Added Fox", References.ENTITY));
      Schema schema87 = p_14514_.addSchema(1936, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsAddTextBackgroundFix(schema87, false));
      Schema schema88 = p_14514_.addSchema(1946, SAME_NAMESPACED);
      p_14514_.addFixer(new ReorganizePoi(schema88, false));
      Schema schema89 = p_14514_.addSchema(1948, SAME_NAMESPACED);
      p_14514_.addFixer(new OminousBannerRenameFix(schema89));
      Schema schema90 = p_14514_.addSchema(1953, SAME_NAMESPACED);
      p_14514_.addFixer(new OminousBannerBlockEntityRenameFix(schema90, false));
      Schema schema91 = p_14514_.addSchema(1955, SAME_NAMESPACED);
      p_14514_.addFixer(new VillagerRebuildLevelAndXpFix(schema91, false));
      p_14514_.addFixer(new ZombieVillagerRebuildXpFix(schema91, false));
      Schema schema92 = p_14514_.addSchema(1961, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkLightRemoveFix(schema92, false));
      Schema schema93 = p_14514_.addSchema(1963, SAME_NAMESPACED);
      p_14514_.addFixer(new RemoveGolemGossipFix(schema93, false));
      Schema schema94 = p_14514_.addSchema(2100, V2100::new);
      p_14514_.addFixer(new AddNewChoices(schema94, "Added Bee and Bee Stinger", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema94, "Add beehive", References.BLOCK_ENTITY));
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(schema94, "Rename sugar recipe", References.RECIPE, createRenamer("minecraft:sugar", "minecraft:sugar_from_sugar_cane"))
      );
      p_14514_.addFixer(
         new AdvancementsRenameFix(
            schema94, false, "Rename sugar recipe advancement", createRenamer("minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane")
         )
      );
      Schema schema95 = p_14514_.addSchema(2202, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkBiomeFix(schema95, false));
      Schema schema96 = p_14514_.addSchema(2209, SAME_NAMESPACED);
      UnaryOperator<String> unaryoperator = createRenamer("minecraft:bee_hive", "minecraft:beehive");
      p_14514_.addFixer(ItemRenameFix.create(schema96, "Rename bee_hive item to beehive", unaryoperator));
      p_14514_.addFixer(new PoiTypeRenameFix(schema96, "Rename bee_hive poi to beehive", unaryoperator));
      p_14514_.addFixer(BlockRenameFix.create(schema96, "Rename bee_hive block to beehive", unaryoperator));
      Schema schema97 = p_14514_.addSchema(2211, SAME_NAMESPACED);
      p_14514_.addFixer(new StructureReferenceCountFix(schema97, false));
      Schema schema98 = p_14514_.addSchema(2218, SAME_NAMESPACED);
      p_14514_.addFixer(new ForcePoiRebuild(schema98, false));
      Schema schema99 = p_14514_.addSchema(2501, V2501::new);
      p_14514_.addFixer(new FurnaceRecipeFix(schema99, true));
      Schema schema100 = p_14514_.addSchema(2502, V2502::new);
      p_14514_.addFixer(new AddNewChoices(schema100, "Added Hoglin", References.ENTITY));
      Schema schema101 = p_14514_.addSchema(2503, SAME_NAMESPACED);
      p_14514_.addFixer(new WallPropertyFix(schema101, false));
      p_14514_.addFixer(
         new AdvancementsRenameFix(
            schema101, false, "Composter category change", createRenamer("minecraft:recipes/misc/composter", "minecraft:recipes/decorations/composter")
         )
      );
      Schema schema102 = p_14514_.addSchema(2505, V2505::new);
      p_14514_.addFixer(new AddNewChoices(schema102, "Added Piglin", References.ENTITY));
      p_14514_.addFixer(new MemoryExpiryDataFix(schema102, "minecraft:villager"));
      Schema schema103 = p_14514_.addSchema(2508, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema103,
            "Renamed fungi items to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema103,
            "Renamed fungi blocks to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      Schema schema104 = p_14514_.addSchema(2509, V2509::new);
      p_14514_.addFixer(new EntityZombifiedPiglinRenameFix(schema104));
      p_14514_.addFixer(ItemRenameFix.create(schema104, "Rename zombie pigman egg item", createRenamer(EntityZombifiedPiglinRenameFix.RENAMED_IDS)));
      Schema schema105 = p_14514_.addSchema(2511, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityProjectileOwnerFix(schema105));
      Schema schema106 = p_14514_.addSchema(2511, 1, V2511_1::new);
      p_14514_.addFixer(new NamedEntityConvertUncheckedFix(schema106, "SplashPotionItemFieldRenameFix", References.ENTITY, "minecraft:potion"));
      Schema schema107 = p_14514_.addSchema(2514, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityUUIDFix(schema107));
      p_14514_.addFixer(new BlockEntityUUIDFix(schema107));
      p_14514_.addFixer(new PlayerUUIDFix(schema107));
      p_14514_.addFixer(new LevelUUIDFix(schema107));
      p_14514_.addFixer(new SavedDataUUIDFix(schema107));
      p_14514_.addFixer(new ItemStackUUIDFix(schema107));
      Schema schema108 = p_14514_.addSchema(2516, SAME_NAMESPACED);
      p_14514_.addFixer(new GossipUUIDFix(schema108, "minecraft:villager"));
      p_14514_.addFixer(new GossipUUIDFix(schema108, "minecraft:zombie_villager"));
      Schema schema109 = p_14514_.addSchema(2518, SAME_NAMESPACED);
      p_14514_.addFixer(new JigsawPropertiesFix(schema109, false));
      p_14514_.addFixer(new JigsawRotationFix(schema109));
      Schema schema110 = p_14514_.addSchema(2519, V2519::new);
      p_14514_.addFixer(new AddNewChoices(schema110, "Added Strider", References.ENTITY));
      Schema schema111 = p_14514_.addSchema(2522, V2522::new);
      p_14514_.addFixer(new AddNewChoices(schema111, "Added Zoglin", References.ENTITY));
      Schema schema112 = p_14514_.addSchema(2523, SAME_NAMESPACED);
      p_14514_.addFixer(
         new AttributesRenameLegacy(
            schema112,
            "Attribute renames",
            createRenamerNoNamespace(
               ImmutableMap.builder()
                  .put("generic.maxHealth", "minecraft:generic.max_health")
                  .put("Max Health", "minecraft:generic.max_health")
                  .put("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements")
                  .put("Spawn Reinforcements Chance", "minecraft:zombie.spawn_reinforcements")
                  .put("horse.jumpStrength", "minecraft:horse.jump_strength")
                  .put("Jump Strength", "minecraft:horse.jump_strength")
                  .put("generic.followRange", "minecraft:generic.follow_range")
                  .put("Follow Range", "minecraft:generic.follow_range")
                  .put("generic.knockbackResistance", "minecraft:generic.knockback_resistance")
                  .put("Knockback Resistance", "minecraft:generic.knockback_resistance")
                  .put("generic.movementSpeed", "minecraft:generic.movement_speed")
                  .put("Movement Speed", "minecraft:generic.movement_speed")
                  .put("generic.flyingSpeed", "minecraft:generic.flying_speed")
                  .put("Flying Speed", "minecraft:generic.flying_speed")
                  .put("generic.attackDamage", "minecraft:generic.attack_damage")
                  .put("generic.attackKnockback", "minecraft:generic.attack_knockback")
                  .put("generic.attackSpeed", "minecraft:generic.attack_speed")
                  .put("generic.armorToughness", "minecraft:generic.armor_toughness")
                  .build()
            )
         )
      );
      Schema schema113 = p_14514_.addSchema(2527, SAME_NAMESPACED);
      p_14514_.addFixer(new BitStorageAlignFix(schema113));
      Schema schema114 = p_14514_.addSchema(2528, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema114,
            "Rename soul fire torch and soul fire lantern",
            createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))
         )
      );
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema114,
            "Rename soul fire torch and soul fire lantern",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:soul_fire_torch",
                  "minecraft:soul_torch",
                  "minecraft:soul_fire_wall_torch",
                  "minecraft:soul_wall_torch",
                  "minecraft:soul_fire_lantern",
                  "minecraft:soul_lantern"
               )
            )
         )
      );
      Schema schema115 = p_14514_.addSchema(2529, SAME_NAMESPACED);
      p_14514_.addFixer(new StriderGravityFix(schema115, false));
      Schema schema116 = p_14514_.addSchema(2531, SAME_NAMESPACED);
      p_14514_.addFixer(new RedstoneWireConnectionsFix(schema116));
      Schema schema117 = p_14514_.addSchema(2533, SAME_NAMESPACED);
      p_14514_.addFixer(new VillagerFollowRangeFix(schema117));
      Schema schema118 = p_14514_.addSchema(2535, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityShulkerRotationFix(schema118));
      Schema schema119 = p_14514_.addSchema(2537, SAME_NAMESPACED);
      p_14514_.addFixer(new LegacyDimensionIdFix(schema119));
      Schema schema120 = p_14514_.addSchema(2538, SAME_NAMESPACED);
      p_14514_.addFixer(new LevelLegacyWorldGenSettingsFix(schema120));
      Schema schema121 = p_14514_.addSchema(2550, SAME_NAMESPACED);
      p_14514_.addFixer(new WorldGenSettingsFix(schema121));
      Schema schema122 = p_14514_.addSchema(2551, V2551::new);
      p_14514_.addFixer(new WriteAndReadFix(schema122, "add types to WorldGenData", References.WORLD_GEN_SETTINGS));
      Schema schema123 = p_14514_.addSchema(2552, SAME_NAMESPACED);
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(schema123, "Nether biome rename", References.BIOME, createRenamer("minecraft:nether", "minecraft:nether_wastes"))
      );
      Schema schema124 = p_14514_.addSchema(2553, SAME_NAMESPACED);
      p_14514_.addFixer(new NamespacedTypeRenameFix(schema124, "Biomes fix", References.BIOME, createRenamer(BiomeFix.BIOMES)));
      Schema schema125 = p_14514_.addSchema(2556, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsFancyGraphicsToGraphicsModeFix(schema125));
      Schema schema126 = p_14514_.addSchema(2558, SAME_NAMESPACED);
      p_14514_.addFixer(new MissingDimensionFix(schema126, false));
      p_14514_.addFixer(new OptionsRenameFieldFix(schema126, false, "Rename swapHands setting", "key_key.swapHands", "key_key.swapOffhand"));
      Schema schema127 = p_14514_.addSchema(2568, V2568::new);
      p_14514_.addFixer(new AddNewChoices(schema127, "Added Piglin Brute", References.ENTITY));
      Schema schema128 = p_14514_.addSchema(2571, V2571::new);
      p_14514_.addFixer(new AddNewChoices(schema128, "Added Goat", References.ENTITY));
      Schema schema129 = p_14514_.addSchema(2679, SAME_NAMESPACED);
      p_14514_.addFixer(new CauldronRenameFix(schema129, false));
      Schema schema130 = p_14514_.addSchema(2680, SAME_NAMESPACED);
      p_14514_.addFixer(ItemRenameFix.create(schema130, "Renamed grass path item to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      p_14514_.addFixer(BlockRenameFix.create(schema130, "Renamed grass path block to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      Schema schema131 = p_14514_.addSchema(2684, V2684::new);
      p_14514_.addFixer(new AddNewChoices(schema131, "Added Sculk Sensor", References.BLOCK_ENTITY));
      Schema schema132 = p_14514_.addSchema(2686, V2686::new);
      p_14514_.addFixer(new AddNewChoices(schema132, "Added Axolotl", References.ENTITY));
      Schema schema133 = p_14514_.addSchema(2688, V2688::new);
      p_14514_.addFixer(new AddNewChoices(schema133, "Added Glow Squid", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema133, "Added Glow Item Frame", References.ENTITY));
      Schema schema134 = p_14514_.addSchema(2690, SAME_NAMESPACED);
      ImmutableMap<String, String> immutablemap = ImmutableMap.builder()
         .put("minecraft:weathered_copper_block", "minecraft:oxidized_copper_block")
         .put("minecraft:semi_weathered_copper_block", "minecraft:weathered_copper_block")
         .put("minecraft:lightly_weathered_copper_block", "minecraft:exposed_copper_block")
         .put("minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper")
         .put("minecraft:semi_weathered_cut_copper", "minecraft:weathered_cut_copper")
         .put("minecraft:lightly_weathered_cut_copper", "minecraft:exposed_cut_copper")
         .put("minecraft:weathered_cut_copper_stairs", "minecraft:oxidized_cut_copper_stairs")
         .put("minecraft:semi_weathered_cut_copper_stairs", "minecraft:weathered_cut_copper_stairs")
         .put("minecraft:lightly_weathered_cut_copper_stairs", "minecraft:exposed_cut_copper_stairs")
         .put("minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab")
         .put("minecraft:semi_weathered_cut_copper_slab", "minecraft:weathered_cut_copper_slab")
         .put("minecraft:lightly_weathered_cut_copper_slab", "minecraft:exposed_cut_copper_slab")
         .put("minecraft:waxed_semi_weathered_copper", "minecraft:waxed_weathered_copper")
         .put("minecraft:waxed_lightly_weathered_copper", "minecraft:waxed_exposed_copper")
         .put("minecraft:waxed_semi_weathered_cut_copper", "minecraft:waxed_weathered_cut_copper")
         .put("minecraft:waxed_lightly_weathered_cut_copper", "minecraft:waxed_exposed_cut_copper")
         .put("minecraft:waxed_semi_weathered_cut_copper_stairs", "minecraft:waxed_weathered_cut_copper_stairs")
         .put("minecraft:waxed_lightly_weathered_cut_copper_stairs", "minecraft:waxed_exposed_cut_copper_stairs")
         .put("minecraft:waxed_semi_weathered_cut_copper_slab", "minecraft:waxed_weathered_cut_copper_slab")
         .put("minecraft:waxed_lightly_weathered_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab")
         .build();
      p_14514_.addFixer(ItemRenameFix.create(schema134, "Renamed copper block items to new oxidized terms", createRenamer(immutablemap)));
      p_14514_.addFixer(BlockRenameFix.create(schema134, "Renamed copper blocks to new oxidized terms", createRenamer(immutablemap)));
      Schema schema135 = p_14514_.addSchema(2691, SAME_NAMESPACED);
      ImmutableMap<String, String> immutablemap1 = ImmutableMap.builder()
         .put("minecraft:waxed_copper", "minecraft:waxed_copper_block")
         .put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper")
         .put("minecraft:weathered_copper_block", "minecraft:weathered_copper")
         .put("minecraft:exposed_copper_block", "minecraft:exposed_copper")
         .build();
      p_14514_.addFixer(ItemRenameFix.create(schema135, "Rename copper item suffixes", createRenamer(immutablemap1)));
      p_14514_.addFixer(BlockRenameFix.create(schema135, "Rename copper blocks suffixes", createRenamer(immutablemap1)));
      Schema schema136 = p_14514_.addSchema(2693, SAME_NAMESPACED);
      p_14514_.addFixer(new AddFlagIfNotPresentFix(schema136, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema schema137 = p_14514_.addSchema(2696, SAME_NAMESPACED);
      ImmutableMap<String, String> immutablemap2 = ImmutableMap.builder()
         .put("minecraft:grimstone", "minecraft:deepslate")
         .put("minecraft:grimstone_slab", "minecraft:cobbled_deepslate_slab")
         .put("minecraft:grimstone_stairs", "minecraft:cobbled_deepslate_stairs")
         .put("minecraft:grimstone_wall", "minecraft:cobbled_deepslate_wall")
         .put("minecraft:polished_grimstone", "minecraft:polished_deepslate")
         .put("minecraft:polished_grimstone_slab", "minecraft:polished_deepslate_slab")
         .put("minecraft:polished_grimstone_stairs", "minecraft:polished_deepslate_stairs")
         .put("minecraft:polished_grimstone_wall", "minecraft:polished_deepslate_wall")
         .put("minecraft:grimstone_tiles", "minecraft:deepslate_tiles")
         .put("minecraft:grimstone_tile_slab", "minecraft:deepslate_tile_slab")
         .put("minecraft:grimstone_tile_stairs", "minecraft:deepslate_tile_stairs")
         .put("minecraft:grimstone_tile_wall", "minecraft:deepslate_tile_wall")
         .put("minecraft:grimstone_bricks", "minecraft:deepslate_bricks")
         .put("minecraft:grimstone_brick_slab", "minecraft:deepslate_brick_slab")
         .put("minecraft:grimstone_brick_stairs", "minecraft:deepslate_brick_stairs")
         .put("minecraft:grimstone_brick_wall", "minecraft:deepslate_brick_wall")
         .put("minecraft:chiseled_grimstone", "minecraft:chiseled_deepslate")
         .build();
      p_14514_.addFixer(ItemRenameFix.create(schema137, "Renamed grimstone block items to deepslate", createRenamer(immutablemap2)));
      p_14514_.addFixer(BlockRenameFix.create(schema137, "Renamed grimstone blocks to deepslate", createRenamer(immutablemap2)));
      Schema schema138 = p_14514_.addSchema(2700, SAME_NAMESPACED);
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema138,
            "Renamed cave vines blocks",
            createRenamer(ImmutableMap.of("minecraft:cave_vines_head", "minecraft:cave_vines", "minecraft:cave_vines_body", "minecraft:cave_vines_plant"))
         )
      );
      Schema schema139 = p_14514_.addSchema(2701, SAME_NAMESPACED);
      p_14514_.addFixer(new SavedDataFeaturePoolElementFix(schema139));
      Schema schema140 = p_14514_.addSchema(2702, SAME_NAMESPACED);
      p_14514_.addFixer(new AbstractArrowPickupFix(schema140));
      Schema schema141 = p_14514_.addSchema(2704, V2704::new);
      p_14514_.addFixer(new AddNewChoices(schema141, "Added Goat", References.ENTITY));
      Schema schema142 = p_14514_.addSchema(2707, V2707::new);
      p_14514_.addFixer(new AddNewChoices(schema142, "Added Marker", References.ENTITY));
      p_14514_.addFixer(new AddFlagIfNotPresentFix(schema142, References.WORLD_GEN_SETTINGS, "has_increased_height_already", true));
      Schema schema143 = p_14514_.addSchema(2710, SAME_NAMESPACED);
      p_14514_.addFixer(
         new StatsRenameFix(schema143, "Renamed play_one_minute stat to play_time", ImmutableMap.of("minecraft:play_one_minute", "minecraft:play_time"))
      );
      Schema schema144 = p_14514_.addSchema(2717, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema144, "Rename azalea_leaves_flowers", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      p_14514_.addFixer(
         BlockRenameFix.create(
            schema144,
            "Rename azalea_leaves_flowers items",
            createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      Schema schema145 = p_14514_.addSchema(2825, SAME_NAMESPACED);
      p_14514_.addFixer(new AddFlagIfNotPresentFix(schema145, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema schema146 = p_14514_.addSchema(2831, V2831::new);
      p_14514_.addFixer(new SpawnerDataFix(schema146));
      Schema schema147 = p_14514_.addSchema(2832, V2832::new);
      p_14514_.addFixer(new WorldGenSettingsHeightAndBiomeFix(schema147));
      p_14514_.addFixer(new ChunkHeightAndBiomeFix(schema147));
      Schema schema148 = p_14514_.addSchema(2833, SAME_NAMESPACED);
      p_14514_.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema148));
      Schema schema149 = p_14514_.addSchema(2838, SAME_NAMESPACED);
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(schema149, "Caves and Cliffs biome renames", References.BIOME, createRenamer(CavesAndCliffsRenames.RENAMES))
      );
      Schema schema150 = p_14514_.addSchema(2841, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkProtoTickListFix(schema150));
      Schema schema151 = p_14514_.addSchema(2842, V2842::new);
      p_14514_.addFixer(new ChunkRenamesFix(schema151));
      Schema schema152 = p_14514_.addSchema(2843, SAME_NAMESPACED);
      p_14514_.addFixer(new OverreachingTickFix(schema152));
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(schema152, "Remove Deep Warm Ocean", References.BIOME, createRenamer("minecraft:deep_warm_ocean", "minecraft:warm_ocean"))
      );
      Schema schema153 = p_14514_.addSchema(2846, SAME_NAMESPACED);
      p_14514_.addFixer(
         new AdvancementsRenameFix(
            schema153,
            false,
            "Rename some C&C part 2 advancements",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:husbandry/play_jukebox_in_meadows",
                  "minecraft:adventure/play_jukebox_in_meadows",
                  "minecraft:adventure/caves_and_cliff",
                  "minecraft:adventure/fall_from_world_height",
                  "minecraft:adventure/ride_strider_in_overworld_lava",
                  "minecraft:nether/ride_strider_in_overworld_lava"
               )
            )
         )
      );
      Schema schema154 = p_14514_.addSchema(2852, SAME_NAMESPACED);
      p_14514_.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(schema154));
      Schema schema155 = p_14514_.addSchema(2967, SAME_NAMESPACED);
      p_14514_.addFixer(new StructureSettingsFlattenFix(schema155));
      Schema schema156 = p_14514_.addSchema(2970, SAME_NAMESPACED);
      p_14514_.addFixer(new StructuresBecomeConfiguredFix(schema156));
      Schema schema157 = p_14514_.addSchema(3076, V3076::new);
      p_14514_.addFixer(new AddNewChoices(schema157, "Added Sculk Catalyst", References.BLOCK_ENTITY));
      Schema schema158 = p_14514_.addSchema(3077, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkDeleteIgnoredLightDataFix(schema158));
      Schema schema159 = p_14514_.addSchema(3078, V3078::new);
      p_14514_.addFixer(new AddNewChoices(schema159, "Added Frog", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema159, "Added Tadpole", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema159, "Added Sculk Shrieker", References.BLOCK_ENTITY));
      Schema schema160 = p_14514_.addSchema(3081, V3081::new);
      p_14514_.addFixer(new AddNewChoices(schema160, "Added Warden", References.ENTITY));
      Schema schema161 = p_14514_.addSchema(3082, V3082::new);
      p_14514_.addFixer(new AddNewChoices(schema161, "Added Chest Boat", References.ENTITY));
      Schema schema162 = p_14514_.addSchema(3083, V3083::new);
      p_14514_.addFixer(new AddNewChoices(schema162, "Added Allay", References.ENTITY));
      Schema schema163 = p_14514_.addSchema(3084, SAME_NAMESPACED);
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(
            schema163,
            "game_event_renames_3084",
            References.GAME_EVENT_NAME,
            createRenamer(
               ImmutableMap.builder()
                  .put("minecraft:block_press", "minecraft:block_activate")
                  .put("minecraft:block_switch", "minecraft:block_activate")
                  .put("minecraft:block_unpress", "minecraft:block_deactivate")
                  .put("minecraft:block_unswitch", "minecraft:block_deactivate")
                  .put("minecraft:drinking_finish", "minecraft:drink")
                  .put("minecraft:elytra_free_fall", "minecraft:elytra_glide")
                  .put("minecraft:entity_damaged", "minecraft:entity_damage")
                  .put("minecraft:entity_dying", "minecraft:entity_die")
                  .put("minecraft:entity_killed", "minecraft:entity_die")
                  .put("minecraft:mob_interact", "minecraft:entity_interact")
                  .put("minecraft:ravager_roar", "minecraft:entity_roar")
                  .put("minecraft:ring_bell", "minecraft:block_change")
                  .put("minecraft:shulker_close", "minecraft:container_close")
                  .put("minecraft:shulker_open", "minecraft:container_open")
                  .put("minecraft:wolf_shaking", "minecraft:entity_shake")
                  .build()
            )
         )
      );
      Schema schema164 = p_14514_.addSchema(3086, SAME_NAMESPACED);
      p_14514_.addFixer(
         new EntityVariantFix(
            schema164, "Change cat variant type", References.ENTITY, "minecraft:cat", "CatType", Util.make(new Int2ObjectOpenHashMap(), p_216528_ -> {
               p_216528_.defaultReturnValue("minecraft:tabby");
               p_216528_.put(0, "minecraft:tabby");
               p_216528_.put(1, "minecraft:black");
               p_216528_.put(2, "minecraft:red");
               p_216528_.put(3, "minecraft:siamese");
               p_216528_.put(4, "minecraft:british");
               p_216528_.put(5, "minecraft:calico");
               p_216528_.put(6, "minecraft:persian");
               p_216528_.put(7, "minecraft:ragdoll");
               p_216528_.put(8, "minecraft:white");
               p_216528_.put(9, "minecraft:jellie");
               p_216528_.put(10, "minecraft:all_black");
            })::get
         )
      );
      ImmutableMap<String, String> immutablemap3 = ImmutableMap.builder()
         .put("textures/entity/cat/tabby.png", "minecraft:tabby")
         .put("textures/entity/cat/black.png", "minecraft:black")
         .put("textures/entity/cat/red.png", "minecraft:red")
         .put("textures/entity/cat/siamese.png", "minecraft:siamese")
         .put("textures/entity/cat/british_shorthair.png", "minecraft:british")
         .put("textures/entity/cat/calico.png", "minecraft:calico")
         .put("textures/entity/cat/persian.png", "minecraft:persian")
         .put("textures/entity/cat/ragdoll.png", "minecraft:ragdoll")
         .put("textures/entity/cat/white.png", "minecraft:white")
         .put("textures/entity/cat/jellie.png", "minecraft:jellie")
         .put("textures/entity/cat/all_black.png", "minecraft:all_black")
         .build();
      p_14514_.addFixer(
         new CriteriaRenameFix(
            schema164,
            "Migrate cat variant advancement",
            "minecraft:husbandry/complete_catalogue",
            p_216517_ -> (String)immutablemap3.getOrDefault(p_216517_, p_216517_)
         )
      );
      Schema schema165 = p_14514_.addSchema(3087, SAME_NAMESPACED);
      p_14514_.addFixer(
         new EntityVariantFix(
            schema165, "Change frog variant type", References.ENTITY, "minecraft:frog", "Variant", Util.make(new Int2ObjectOpenHashMap(), p_216519_ -> {
               p_216519_.put(0, "minecraft:temperate");
               p_216519_.put(1, "minecraft:warm");
               p_216519_.put(2, "minecraft:cold");
            })::get
         )
      );
      Schema schema166 = p_14514_.addSchema(3090, SAME_NAMESPACED);
      p_14514_.addFixer(
         new EntityFieldsRenameFix(schema166, "EntityPaintingFieldsRenameFix", "minecraft:painting", Map.of("Motive", "variant", "Facing", "facing"))
      );
      Schema schema167 = p_14514_.addSchema(3093, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityGoatMissingStateFix(schema167));
      Schema schema168 = p_14514_.addSchema(3094, SAME_NAMESPACED);
      p_14514_.addFixer(new GoatHornIdFix(schema168));
      Schema schema169 = p_14514_.addSchema(3097, SAME_NAMESPACED);
      p_14514_.addFixer(new FilteredBooksFix(schema169));
      p_14514_.addFixer(new FilteredSignsFix(schema169));
      Map<String, String> map = Map.of("minecraft:british", "minecraft:british_shorthair");
      p_14514_.addFixer(new VariantRenameFix(schema169, "Rename british shorthair", References.ENTITY, "minecraft:cat", map));
      p_14514_.addFixer(
         new CriteriaRenameFix(
            schema169,
            "Migrate cat variant advancement for british shorthair",
            "minecraft:husbandry/complete_catalogue",
            p_216531_ -> map.getOrDefault(p_216531_, p_216531_)
         )
      );
      p_14514_.addFixer(new PoiTypeRemoveFix(schema169, "Remove unpopulated villager PoI types", Set.of("minecraft:unemployed", "minecraft:nitwit")::contains));
      Schema schema170 = p_14514_.addSchema(3108, SAME_NAMESPACED);
      p_14514_.addFixer(new BlendingDataRemoveFromNetherEndFix(schema170));
      Schema schema171 = p_14514_.addSchema(3201, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsProgrammerArtFix(schema171));
      Schema schema172 = p_14514_.addSchema(3202, V3202::new);
      p_14514_.addFixer(new AddNewChoices(schema172, "Added Hanging Sign", References.BLOCK_ENTITY));
      Schema schema173 = p_14514_.addSchema(3203, V3203::new);
      p_14514_.addFixer(new AddNewChoices(schema173, "Added Camel", References.ENTITY));
      Schema schema174 = p_14514_.addSchema(3204, V3204::new);
      p_14514_.addFixer(new AddNewChoices(schema174, "Added Chiseled Bookshelf", References.BLOCK_ENTITY));
      Schema schema175 = p_14514_.addSchema(3209, SAME_NAMESPACED);
      p_14514_.addFixer(new ItemStackSpawnEggFix(schema175, false, "minecraft:pig_spawn_egg"));
      Schema schema176 = p_14514_.addSchema(3214, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsAmbientOcclusionFix(schema176));
      Schema schema177 = p_14514_.addSchema(3319, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsAccessibilityOnboardFix(schema177));
      Schema schema178 = p_14514_.addSchema(3322, SAME_NAMESPACED);
      p_14514_.addFixer(new EffectDurationFix(schema178));
      Schema schema179 = p_14514_.addSchema(3325, V3325::new);
      p_14514_.addFixer(new AddNewChoices(schema179, "Added displays", References.ENTITY));
      Schema schema180 = p_14514_.addSchema(3326, V3326::new);
      p_14514_.addFixer(new AddNewChoices(schema180, "Added Sniffer", References.ENTITY));
      Schema schema181 = p_14514_.addSchema(3327, V3327::new);
      p_14514_.addFixer(new AddNewChoices(schema181, "Archaeology", References.BLOCK_ENTITY));
      Schema schema182 = p_14514_.addSchema(3328, V3328::new);
      p_14514_.addFixer(new AddNewChoices(schema182, "Added interaction", References.ENTITY));
      Schema schema183 = p_14514_.addSchema(3438, V3438::new);
      p_14514_.addFixer(
         BlockEntityRenameFix.create(
            schema183, "Rename Suspicious Sand to Brushable Block", createRenamer("minecraft:suspicious_sand", "minecraft:brushable_block")
         )
      );
      p_14514_.addFixer(new EntityBrushableBlockFieldsRenameFix(schema183));
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema183,
            "Pottery shard renaming",
            createRenamer(
               ImmutableMap.of(
                  "minecraft:pottery_shard_archer",
                  "minecraft:archer_pottery_shard",
                  "minecraft:pottery_shard_prize",
                  "minecraft:prize_pottery_shard",
                  "minecraft:pottery_shard_arms_up",
                  "minecraft:arms_up_pottery_shard",
                  "minecraft:pottery_shard_skull",
                  "minecraft:skull_pottery_shard"
               )
            )
         )
      );
      p_14514_.addFixer(new AddNewChoices(schema183, "Added calibrated sculk sensor", References.BLOCK_ENTITY));
      Schema schema184 = p_14514_.addSchema(3439, V3439::new);
      p_14514_.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(schema184, "Updated sign text format for Signs", "minecraft:sign"));
      Schema schema185 = p_14514_.addSchema(3439, 1, V3439_1::new);
      p_14514_.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(schema185, "Updated sign text format for Hanging Signs", "minecraft:hanging_sign"));
      Schema schema186 = p_14514_.addSchema(3440, SAME_NAMESPACED);
      p_14514_.addFixer(
         new NamespacedTypeRenameFix(
            schema186,
            "Replace experimental 1.20 overworld",
            References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
            createRenamer("minecraft:overworld_update_1_20", "minecraft:overworld")
         )
      );
      p_14514_.addFixer(new FeatureFlagRemoveFix(schema186, "Remove 1.20 feature toggle", Set.of("minecraft:update_1_20")));
      Schema schema187 = p_14514_.addSchema(3447, SAME_NAMESPACED);
      p_14514_.addFixer(
         ItemRenameFix.create(
            schema187,
            "Pottery shard item renaming to Pottery sherd",
            createRenamer(
               Stream.of(
                     "minecraft:angler_pottery_shard",
                     "minecraft:archer_pottery_shard",
                     "minecraft:arms_up_pottery_shard",
                     "minecraft:blade_pottery_shard",
                     "minecraft:brewer_pottery_shard",
                     "minecraft:burn_pottery_shard",
                     "minecraft:danger_pottery_shard",
                     "minecraft:explorer_pottery_shard",
                     "minecraft:friend_pottery_shard",
                     "minecraft:heart_pottery_shard",
                     "minecraft:heartbreak_pottery_shard",
                     "minecraft:howl_pottery_shard",
                     "minecraft:miner_pottery_shard",
                     "minecraft:mourner_pottery_shard",
                     "minecraft:plenty_pottery_shard",
                     "minecraft:prize_pottery_shard",
                     "minecraft:sheaf_pottery_shard",
                     "minecraft:shelter_pottery_shard",
                     "minecraft:skull_pottery_shard",
                     "minecraft:snort_pottery_shard"
                  )
                  .collect(Collectors.toMap(Function.identity(), p_280993_ -> p_280993_.replace("_pottery_shard", "_pottery_sherd")))
            )
         )
      );
      Schema schema188 = p_14514_.addSchema(3448, V3448::new);
      p_14514_.addFixer(new DecoratedPotFieldRenameFix(schema188));
      Schema schema189 = p_14514_.addSchema(3450, SAME_NAMESPACED);
      p_14514_.addFixer(
         new RemapChunkStatusFix(
            schema189,
            "Remove liquid_carvers and heightmap chunk statuses",
            createRenamer(Map.of("minecraft:liquid_carvers", "minecraft:carvers", "minecraft:heightmaps", "minecraft:spawn"))
         )
      );
      Schema schema190 = p_14514_.addSchema(3451, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkDeleteLightFix(schema190));
      Schema schema191 = p_14514_.addSchema(3459, SAME_NAMESPACED);
      p_14514_.addFixer(new LegacyDragonFightFix(schema191));
      Schema schema192 = p_14514_.addSchema(3564, SAME_NAMESPACED);
      p_14514_.addFixer(new DropInvalidSignDataFix(schema192, "minecraft:sign"));
      Schema schema193 = p_14514_.addSchema(3564, 1, SAME_NAMESPACED);
      p_14514_.addFixer(new DropInvalidSignDataFix(schema193, "minecraft:hanging_sign"));
      Schema schema194 = p_14514_.addSchema(3565, SAME_NAMESPACED);
      p_14514_.addFixer(new RandomSequenceSettingsFix(schema194));
      Schema schema195 = p_14514_.addSchema(3566, SAME_NAMESPACED);
      p_14514_.addFixer(new ScoreboardDisplaySlotFix(schema195));
      Schema schema196 = p_14514_.addSchema(3568, SAME_NAMESPACED);
      p_14514_.addFixer(new MobEffectIdFix(schema196));
      Schema schema197 = p_14514_.addSchema(3682, V3682::new);
      p_14514_.addFixer(new AddNewChoices(schema197, "Added Crafter", References.BLOCK_ENTITY));
      Schema schema198 = p_14514_.addSchema(3683, V3683::new);
      p_14514_.addFixer(new PrimedTntBlockStateFixer(schema198));
      Schema schema199 = p_14514_.addSchema(3685, V3685::new);
      p_14514_.addFixer(new FixProjectileStoredItem(schema199));
      Schema schema200 = p_14514_.addSchema(3689, V3689::new);
      p_14514_.addFixer(new AddNewChoices(schema200, "Added Breeze", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema200, "Added Trial Spawner", References.BLOCK_ENTITY));
      Schema schema201 = p_14514_.addSchema(3692, SAME_NAMESPACED);
      UnaryOperator<String> unaryoperator1 = createRenamer(Map.of("minecraft:grass", "minecraft:short_grass"));
      p_14514_.addFixer(BlockRenameFix.create(schema201, "Rename grass block to short_grass", unaryoperator1));
      p_14514_.addFixer(ItemRenameFix.create(schema201, "Rename grass item to short_grass", unaryoperator1));
      Schema schema202 = p_14514_.addSchema(3799, V3799::new);
      p_14514_.addFixer(new AddNewChoices(schema202, "Added Armadillo", References.ENTITY));
      Schema schema203 = p_14514_.addSchema(3800, SAME_NAMESPACED);
      UnaryOperator<String> unaryoperator2 = createRenamer(Map.of("minecraft:scute", "minecraft:turtle_scute"));
      p_14514_.addFixer(ItemRenameFix.create(schema203, "Rename scute item to turtle_scute", unaryoperator2));
      Schema schema204 = p_14514_.addSchema(3803, SAME_NAMESPACED);
      p_14514_.addFixer(
         new RenameEnchantmentsFix(schema204, "Rename sweeping enchant to sweeping_edge", Map.of("minecraft:sweeping", "minecraft:sweeping_edge"))
      );
      Schema schema205 = p_14514_.addSchema(3807, V3807::new);
      p_14514_.addFixer(new AddNewChoices(schema205, "Added Vault", References.BLOCK_ENTITY));
      Schema schema206 = p_14514_.addSchema(3807, 1, SAME_NAMESPACED);
      p_14514_.addFixer(new MapBannerBlockPosFormatFix(schema206));
      Schema schema207 = p_14514_.addSchema(3808, V3808::new);
      p_14514_.addFixer(new HorseBodyArmorItemFix(schema207, "minecraft:horse", "ArmorItem", true));
      Schema schema208 = p_14514_.addSchema(3808, 1, V3808_1::new);
      p_14514_.addFixer(new HorseBodyArmorItemFix(schema208, "minecraft:llama", "DecorItem", false));
      Schema schema209 = p_14514_.addSchema(3808, 2, V3808_2::new);
      p_14514_.addFixer(new HorseBodyArmorItemFix(schema209, "minecraft:trader_llama", "DecorItem", false));
      Schema schema210 = p_14514_.addSchema(3809, SAME_NAMESPACED);
      p_14514_.addFixer(new ChestedHorsesInventoryZeroIndexingFix(schema210));
      Schema schema211 = p_14514_.addSchema(3812, SAME_NAMESPACED);
      p_14514_.addFixer(new FixWolfHealth(schema211));
      Schema schema212 = p_14514_.addSchema(3813, V3813::new);
      p_14514_.addFixer(new BlockPosFormatAndRenamesFix(schema212));
      Schema schema213 = p_14514_.addSchema(3814, SAME_NAMESPACED);
      p_14514_.addFixer(
         new AttributesRenameLegacy(
            schema213, "Rename jump strength attribute", createRenamer("minecraft:horse.jump_strength", "minecraft:generic.jump_strength")
         )
      );
      Schema schema214 = p_14514_.addSchema(3816, V3816::new);
      p_14514_.addFixer(new AddNewChoices(schema214, "Added Bogged", References.ENTITY));
      Schema schema215 = p_14514_.addSchema(3818, V3818::new);
      p_14514_.addFixer(new BeehiveFieldRenameFix(schema215));
      p_14514_.addFixer(new EmptyItemInHotbarFix(schema215));
      Schema schema216 = p_14514_.addSchema(3818, 1, SAME_NAMESPACED);
      p_14514_.addFixer(new BannerPatternFormatFix(schema216));
      Schema schema217 = p_14514_.addSchema(3818, 2, SAME_NAMESPACED);
      p_14514_.addFixer(new TippedArrowPotionToItemFix(schema217));
      Schema schema218 = p_14514_.addSchema(3818, 3, V3818_3::new);
      p_14514_.addFixer(new WriteAndReadFix(schema218, "Inject data component types", References.DATA_COMPONENTS));
      Schema schema219 = p_14514_.addSchema(3818, 4, V3818_4::new);
      p_14514_.addFixer(new ParticleUnflatteningFix(schema219));
      Schema schema220 = p_14514_.addSchema(3818, 5, V3818_5::new);
      p_14514_.addFixer(new ItemStackComponentizationFix(schema220));
      Schema schema221 = p_14514_.addSchema(3818, 6, SAME_NAMESPACED);
      p_14514_.addFixer(new AreaEffectCloudPotionFix(schema221));
      Schema schema222 = p_14514_.addSchema(3820, SAME_NAMESPACED);
      p_14514_.addFixer(new PlayerHeadBlockProfileFix(schema222));
      p_14514_.addFixer(new LodestoneCompassComponentFix(schema222));
      Schema schema223 = p_14514_.addSchema(3825, V3825::new);
      p_14514_.addFixer(new ItemStackCustomNameToOverrideComponentFix(schema223));
      p_14514_.addFixer(new BannerEntityCustomNameToOverrideComponentFix(schema223));
      p_14514_.addFixer(new TrialSpawnerConfigFix(schema223));
      p_14514_.addFixer(new AddNewChoices(schema223, "Added Ominous Item Spawner", References.ENTITY));
      Schema schema224 = p_14514_.addSchema(3828, SAME_NAMESPACED);
      p_14514_.addFixer(new EmptyItemInVillagerTradeFix(schema224));
      Schema schema225 = p_14514_.addSchema(3833, SAME_NAMESPACED);
      p_14514_.addFixer(new RemoveEmptyItemInBrushableBlockFix(schema225));
      Schema schema226 = p_14514_.addSchema(3938, V3938::new);
      p_14514_.addFixer(new ProjectileStoredWeaponFix(schema226));
      Schema schema227 = p_14514_.addSchema(3939, SAME_NAMESPACED);
      p_14514_.addFixer(new FeatureFlagRemoveFix(schema227, "Remove 1.21 feature toggle", Set.of("minecraft:update_1_21")));
      Schema schema228 = p_14514_.addSchema(3943, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsMenuBlurrinessFix(schema228));
      Schema schema229 = p_14514_.addSchema(3945, SAME_NAMESPACED);
      p_14514_.addFixer(new AttributeModifierIdFix(schema229));
      p_14514_.addFixer(new JukeboxTicksSinceSongStartedFix(schema229));
      Schema schema230 = p_14514_.addSchema(4054, SAME_NAMESPACED);
      p_14514_.addFixer(new OminousBannerRarityFix(schema230));
      Schema schema231 = p_14514_.addSchema(4055, SAME_NAMESPACED);
      p_14514_.addFixer(new AttributeIdPrefixFix(schema231));
      Schema schema232 = p_14514_.addSchema(4057, SAME_NAMESPACED);
      p_14514_.addFixer(new CarvingStepRemoveFix(schema232));
      Schema schema233 = p_14514_.addSchema(4059, V4059::new);
      p_14514_.addFixer(new FoodToConsumableFix(schema233));
      Schema schema234 = p_14514_.addSchema(4061, SAME_NAMESPACED);
      p_14514_.addFixer(new TrialSpawnerConfigInRegistryFix(schema234));
      Schema schema235 = p_14514_.addSchema(4064, SAME_NAMESPACED);
      p_14514_.addFixer(new FireResistantToDamageResistantComponentFix(schema235));
      Schema schema236 = p_14514_.addSchema(4067, V4067::new);
      p_14514_.addFixer(new BoatSplitFix(schema236));
      p_14514_.addFixer(new FeatureFlagRemoveFix(schema236, "Remove Bundle experimental feature flag", Set.of("minecraft:bundle")));
      Schema schema237 = p_14514_.addSchema(4068, SAME_NAMESPACED);
      p_14514_.addFixer(new LockComponentPredicateFix(schema237));
      p_14514_.addFixer(new ContainerBlockEntityLockPredicateFix(schema237));
      Schema schema238 = p_14514_.addSchema(4070, V4070::new);
      p_14514_.addFixer(new AddNewChoices(schema238, "Added Pale Oak Boat and Pale Oak Chest Boat", References.ENTITY));
      Schema schema239 = p_14514_.addSchema(4071, V4071::new);
      p_14514_.addFixer(new AddNewChoices(schema239, "Added Creaking", References.ENTITY));
      p_14514_.addFixer(new AddNewChoices(schema239, "Added Creaking Heart", References.BLOCK_ENTITY));
      Schema schema240 = p_14514_.addSchema(4081, SAME_NAMESPACED);
      p_14514_.addFixer(new EntitySalmonSizeFix(schema240));
      Schema schema241 = p_14514_.addSchema(4173, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityFieldsRenameFix(schema241, "Rename TNT Minecart fuse", "minecraft:tnt_minecart", Map.of("TNTFuse", "fuse")));
      Schema schema242 = p_14514_.addSchema(4175, SAME_NAMESPACED);
      p_14514_.addFixer(new EquippableAssetRenameFix(schema242));
      p_14514_.addFixer(new CustomModelDataExpandFix(schema242));
      Schema schema243 = p_14514_.addSchema(4176, SAME_NAMESPACED);
      p_14514_.addFixer(new InvalidBlockEntityLockFix(schema243));
      p_14514_.addFixer(new InvalidLockComponentFix(schema243));
      Schema schema244 = p_14514_.addSchema(4180, SAME_NAMESPACED);
      p_14514_.addFixer(new FeatureFlagRemoveFix(schema244, "Remove Winter Drop toggle", Set.of("minecraft:winter_drop")));
      Schema schema245 = p_14514_.addSchema(4181, SAME_NAMESPACED);
      p_14514_.addFixer(new BlockEntityFurnaceBurnTimeFix(schema245, "minecraft:furnace"));
      p_14514_.addFixer(new BlockEntityFurnaceBurnTimeFix(schema245, "minecraft:smoker"));
      p_14514_.addFixer(new BlockEntityFurnaceBurnTimeFix(schema245, "minecraft:blast_furnace"));
      Schema schema246 = p_14514_.addSchema(4187, SAME_NAMESPACED);
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Villager follow range fix undo", "minecraft:villager", "minecraft:follow_range", p_374906_ -> p_374906_ == 48.0 ? 16.0 : p_374906_
         )
      );
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Bee follow range fix", "minecraft:bee", "minecraft:follow_range", p_374905_ -> p_374905_ == 48.0 ? 16.0 : p_374905_
         )
      );
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Allay follow range fix", "minecraft:allay", "minecraft:follow_range", p_374903_ -> p_374903_ == 48.0 ? 16.0 : p_374903_
         )
      );
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Llama follow range fix", "minecraft:llama", "minecraft:follow_range", p_374907_ -> p_374907_ == 40.0 ? 16.0 : p_374907_
         )
      );
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Piglin Brute follow range fix", "minecraft:piglin_brute", "minecraft:follow_range", p_374904_ -> p_374904_ == 16.0 ? 12.0 : p_374904_
         )
      );
      p_14514_.addFixer(
         new EntityAttributeBaseValueFix(
            schema246, "Warden follow range fix", "minecraft:warden", "minecraft:follow_range", p_374902_ -> p_374902_ == 16.0 ? 24.0 : p_374902_
         )
      );
      Schema schema247 = p_14514_.addSchema(4290, V4290::new);
      p_14514_.addFixer(new UnflattenTextComponentFix(schema247));
      Schema schema248 = p_14514_.addSchema(4291, SAME_NAMESPACED);
      p_14514_.addFixer(new LegacyHoverEventFix(schema248));
      p_14514_.addFixer(new TextComponentStringifiedFlagsFix(schema248));
      Schema schema249 = p_14514_.addSchema(4292, V4292::new);
      p_14514_.addFixer(new TextComponentHoverAndClickEventFix(schema249));
      Schema schema250 = p_14514_.addSchema(4293, SAME_NAMESPACED);
      p_14514_.addFixer(new DropChancesFormatFix(schema250));
      Schema schema251 = p_14514_.addSchema(4294, SAME_NAMESPACED);
      p_14514_.addFixer(
         new BlockPropertyRenameAndFix(
            schema251,
            "CreakingHeartBlockStateFix",
            "minecraft:creaking_heart",
            "active",
            "creaking_heart_state",
            p_390213_ -> p_390213_.equals("true") ? "awake" : "uprooted"
         )
      );
      Schema schema252 = p_14514_.addSchema(4295, SAME_NAMESPACED);
      p_14514_.addFixer(new BlendingDataFix(schema252));
      Schema schema253 = p_14514_.addSchema(4296, SAME_NAMESPACED);
      p_14514_.addFixer(new AreaEffectCloudDurationScaleFix(schema253));
      Schema schema254 = p_14514_.addSchema(4297, SAME_NAMESPACED);
      p_14514_.addFixer(new ForcedChunkToTicketFix(schema254));
      Schema schema255 = p_14514_.addSchema(4299, SAME_NAMESPACED);
      p_14514_.addFixer(new EntitySpawnerItemVariantComponentFix(schema255));
      Schema schema256 = p_14514_.addSchema(4300, V4300::new);
      p_14514_.addFixer(new SaddleEquipmentSlotFix(schema256));
      Schema schema257 = p_14514_.addSchema(4301, V4301::new);
      p_14514_.addFixer(new EquipmentFormatFix(schema257));
      Schema schema258 = p_14514_.addSchema(4302, V4302::new);
      p_14514_.addFixer(new AddNewChoices(schema258, "Added Test and Test Instance Block Entities", References.BLOCK_ENTITY));
      Schema schema259 = p_14514_.addSchema(4303, SAME_NAMESPACED);
      p_14514_.addFixer(new EntityFallDistanceFloatToDoubleFix(schema259, References.ENTITY));
      p_14514_.addFixer(new EntityFallDistanceFloatToDoubleFix(schema259, References.PLAYER));
      Schema schema260 = p_14514_.addSchema(4305, SAME_NAMESPACED);
      p_14514_.addFixer(
         new BlockPropertyRenameAndFix(schema260, "rename test block mode", "minecraft:test_block", "test_block_mode", "mode", p_390214_ -> p_390214_)
      );
      Schema schema261 = p_14514_.addSchema(4306, V4306::new);
      p_14514_.addFixer(new ThrownPotionSplitFix(schema261));
      Schema schema262 = p_14514_.addSchema(4307, V4307::new);
      p_14514_.addFixer(new TooltipDisplayComponentFix(schema262));
      Schema schema263 = p_14514_.addSchema(4309, SAME_NAMESPACED);
      p_14514_.addFixer(new RaidRenamesDataFix(schema263));
      p_14514_.addFixer(new ChunkTicketUnpackPosFix(schema263));
      Schema schema264 = p_14514_.addSchema(4311, SAME_NAMESPACED);
      p_14514_.addFixer(
         new AdvancementsRenameFix(
            schema264, false, "Use lodestone category change", createRenamer("minecraft:nether/use_lodestone", "minecraft:adventure/use_lodestone")
         )
      );
      Schema schema265 = p_14514_.addSchema(4312, V4312::new);
      p_14514_.addFixer(new PlayerEquipmentFix(schema265));
      Schema schema266 = p_14514_.addSchema(4314, SAME_NAMESPACED);
      p_14514_.addFixer(new InlineBlockPosFormatFix(schema266));
      Schema schema267 = p_14514_.addSchema(4420, V4420::new);
      p_14514_.addFixer(new NamedEntityConvertUncheckedFix(schema267, "AreaEffectCloudCustomParticleFix", References.ENTITY, "minecraft:area_effect_cloud"));
      Schema schema268 = p_14514_.addSchema(4421, V4421::new);
      p_14514_.addFixer(new AddNewChoices(schema268, "Added Happy Ghast", References.ENTITY));
      Schema schema269 = p_14514_.addSchema(4424, SAME_NAMESPACED);
      p_14514_.addFixer(new FeatureFlagRemoveFix(schema269, "Remove Locator Bar experimental feature flag", Set.of("minecraft:locator_bar")));
      p_14514_.addFixer(new AddFieldFix(schema269, References.PLAYER, "style", p_421546_ -> p_421546_.createString("minecraft:default"), "locator_bar_icon"));
      p_14514_.addFixer(new AddFieldFix(schema269, References.ENTITY, "style", p_421547_ -> p_421547_.createString("minecraft:default"), "locator_bar_icon"));
      Schema schema270 = p_14514_.addSchema(4531, V4531::new);
      p_14514_.addFixer(new AddNewChoices(schema270, "Added Copper Golem", References.ENTITY));
      Schema schema271 = p_14514_.addSchema(4532, V4532::new);
      p_14514_.addFixer(new AddNewChoices(schema271, "Added Copper Golem Statue Block Entity", References.BLOCK_ENTITY));
      Schema schema272 = p_14514_.addSchema(4533, V4533::new);
      p_14514_.addFixer(new AddNewChoices(schema272, "Added Shelf", References.BLOCK_ENTITY));
      Schema schema273 = p_14514_.addSchema(4535, SAME_NAMESPACED);
      p_14514_.addFixer(new CopperGolemWeatherStateFix(schema273));
      Schema schema274 = p_14514_.addSchema(4537, SAME_NAMESPACED);
      p_14514_.addFixer(new ChunkDeleteLightFix(schema274));
      Schema schema275 = p_14514_.addSchema(4541, SAME_NAMESPACED);
      p_14514_.addFixer(BlockRenameFix.create(schema275, "Rename chain to iron_chain", createRenamer("minecraft:chain", "minecraft:iron_chain")));
      p_14514_.addFixer(ItemRenameFix.create(schema275, "Rename chain to iron_chain", createRenamer("minecraft:chain", "minecraft:iron_chain")));
      Schema schema276 = p_14514_.addSchema(4543, V4543::new);
      p_14514_.addFixer(new AddNewChoices(schema276, "Added Mannequin", References.ENTITY));
      Schema schema277 = p_14514_.addSchema(4544, SAME_NAMESPACED);
      p_14514_.addFixer(new LegacyWorldBorderFix(schema277));
      Schema schema278 = p_14514_.addSchema(4548, SAME_NAMESPACED);
      p_14514_.addFixer(new WorldSpawnDataFix(schema278));
      p_14514_.addFixer(new PlayerRespawnDataFix(schema278));
      Schema schema279 = p_14514_.addSchema(4648, V4648::new);
      p_14514_.addFixer(new AddNewChoices(schema279, "Added Nautilus and Zombie Nautilus", References.ENTITY));
      Schema schema280 = p_14514_.addSchema(4649, SAME_NAMESPACED);
      p_14514_.addFixer(new TridentAnimationFix(schema280));
      Schema schema281 = p_14514_.addSchema(4650, SAME_NAMESPACED);
      p_14514_.addFixer(new DebugProfileOverlayReferenceFix(schema281));
      Schema schema282 = p_14514_.addSchema(4651, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsGraphicsModeSplitFix(schema282, "cutoutLeaves", "false", "true", "true"));
      p_14514_.addFixer(new OptionsGraphicsModeSplitFix(schema282, "weatherRadius", "5", "10", "10"));
      p_14514_.addFixer(new OptionsGraphicsModeSplitFix(schema282, "vignette", "false", "true", "true"));
      p_14514_.addFixer(new OptionsGraphicsModeSplitFix(schema282, "improvedTransparency", "false", "false", "true"));
      p_14514_.addFixer(new OptionsSetGraphicsPresetToCustomFix(schema282));
      Schema schema283 = p_14514_.addSchema(4656, V4656::new);
      p_14514_.addFixer(new AddNewChoices(schema283, "Added Parched and Camel Husk", References.ENTITY));
      Schema schema284 = p_14514_.addSchema(4657, SAME_NAMESPACED);
      p_14514_.addFixer(new WorldBorderWarningTimeFix(schema284));
      Schema schema285 = p_14514_.addSchema(4658, SAME_NAMESPACED);
      p_14514_.addFixer(new GameRuleRegistryFix(schema285));
      Schema schema286 = p_14514_.addSchema(4661, SAME_NAMESPACED);
      p_14514_.addFixer(new OptionsMusicToastFix(schema286, false));
   }

   private static UnaryOperator<String> createRenamerNoNamespace(Map<String, String> p_330646_) {
      return p_216526_ -> p_330646_.getOrDefault(p_216526_, p_216526_);
   }

   private static UnaryOperator<String> createRenamer(Map<String, String> p_14525_) {
      return p_326540_ -> p_14525_.getOrDefault(NamespacedSchema.ensureNamespaced(p_326540_), p_326540_);
   }

   private static UnaryOperator<String> createRenamer(String p_14518_, String p_14519_) {
      return p_326538_ -> Objects.equals(NamespacedSchema.ensureNamespaced(p_326538_), p_14518_) ? p_14519_ : p_326538_;
   }
}
