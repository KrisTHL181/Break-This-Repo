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
import net.minecraft.util.datafix.fixes.DayTimeToClockFix;
import net.minecraft.util.datafix.fixes.DebugProfileLookingAtSplitFix;
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
import net.minecraft.util.datafix.fixes.LevelDatDifficultyFix;
import net.minecraft.util.datafix.fixes.LevelDatToSavedDataPreparationFix;
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
import net.minecraft.util.datafix.fixes.OptionsForceDefaultGraphicsApiFix;
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
import net.minecraft.util.datafix.fixes.PotDecorationsBlockEntityUnflatteningFix;
import net.minecraft.util.datafix.fixes.PotDecorationsComponentUnflatteningFix;
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
import net.minecraft.util.datafix.fixes.RenameNameplateToNameTagFix;
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
import net.minecraft.util.datafix.fixes.StonecutterRecipeRenameFix;
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
import net.minecraft.util.datafix.fixes.VillagerSetVillagerDataFinalized;
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
import net.minecraft.util.datafix.fixes.ZombieVillagerSetVillagerDataFinalized;
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
import net.minecraft.util.datafix.schemas.V4771;
import net.minecraft.util.datafix.schemas.V4881;
import net.minecraft.util.datafix.schemas.V4885;
import net.minecraft.util.datafix.schemas.V4996;
import net.minecraft.util.datafix.schemas.V4996_1;
import net.minecraft.util.datafix.schemas.V4997;
import net.minecraft.util.datafix.schemas.V5000;
import net.minecraft.util.datafix.schemas.V501;
import net.minecraft.util.datafix.schemas.V700;
import net.minecraft.util.datafix.schemas.V701;
import net.minecraft.util.datafix.schemas.V702;
import net.minecraft.util.datafix.schemas.V703;
import net.minecraft.util.datafix.schemas.V704;
import net.minecraft.util.datafix.schemas.V705;
import net.minecraft.util.datafix.schemas.V808;
import net.minecraft.util.datafix.schemas.V99;
import net.minecraft.util.filefix.FileFixerUpper;
import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import net.minecraft.util.filefix.fixes.GeneratedStructuresRenameFileFix;
import net.minecraft.util.filefix.fixes.LegacyStructureFileFix;
import net.minecraft.util.filefix.fixes.LevelDatToSavedDataFileFix;
import net.minecraft.util.filefix.fixes.PlayerStorageFileFix;
import net.minecraft.util.filefix.fixes.ReenableSpectatorsGenerateChunksInHardcoreWorldsFileFix;
import net.minecraft.util.filefix.fixes.RemoveObsoleteFilesFileFix;
import net.minecraft.util.filefix.fixes.ResourcePackLocationFileFix;

public class DataFixers {
   private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
   private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;
   private static final Result DATA_FIXER;
   private static final FileFixerUpper FILE_FIXER;
   public static final int BLENDING_VERSION = 4997;

   private DataFixers() {
   }

   public static DataFixer getDataFixer() {
      return DATA_FIXER.fixer();
   }

   public static FileFixerUpper getFileFixer() {
      return FILE_FIXER;
   }

   public static CompletableFuture<?> optimize(final Set<TypeReference> typesToOptimize) {
      if (typesToOptimize.isEmpty()) {
         return CompletableFuture.completedFuture(null);
      }

      Executor executor = Executors.newSingleThreadExecutor(
         new ThreadFactoryBuilder().setNameFormat("Datafixer Bootstrap #%d").setDaemon(true).setPriority(1).build()
      );
      return DATA_FIXER.optimize(typesToOptimize, executor);
   }

   private static void addFixers(final DataFixerBuilder fixerUpper, final FileFixerUpper.Builder fileFixerUpper) {
      fixerUpper.addSchema(99, V99::new);
      Schema v100 = fixerUpper.addSchema(100, V100::new);
      fixerUpper.addFixer(new EntityEquipmentToArmorAndHandFix(v100));
      Schema v101 = fixerUpper.addSchema(101, SAME);
      fixerUpper.addFixer(new VillagerSetCanPickUpLootFix(v101));
      Schema v102 = fixerUpper.addSchema(102, V102::new);
      fixerUpper.addFixer(new ItemIdFix(v102, true));
      fixerUpper.addFixer(new ItemPotionFix(v102, false));
      Schema v105 = fixerUpper.addSchema(105, SAME);
      fixerUpper.addFixer(new ItemSpawnEggFix(v105, true));
      Schema v106 = fixerUpper.addSchema(106, V106::new);
      fixerUpper.addFixer(new MobSpawnerEntityIdentifiersFix(v106, true));
      Schema v107 = fixerUpper.addSchema(107, V107::new);
      fixerUpper.addFixer(new EntityMinecartIdentifiersFix(v107));
      Schema v108 = fixerUpper.addSchema(108, SAME);
      fixerUpper.addFixer(new EntityStringUuidFix(v108, true));
      Schema v109 = fixerUpper.addSchema(109, SAME);
      fixerUpper.addFixer(new EntityHealthFix(v109, true));
      Schema v110 = fixerUpper.addSchema(110, SAME);
      fixerUpper.addFixer(new EntityHorseSaddleFix(v110, true));
      Schema v111 = fixerUpper.addSchema(111, SAME);
      fixerUpper.addFixer(new EntityPaintingItemFrameDirectionFix(v111, true));
      Schema v113 = fixerUpper.addSchema(113, SAME);
      fixerUpper.addFixer(new EntityRedundantChanceTagsFix(v113, true));
      Schema v135 = fixerUpper.addSchema(135, V135::new);
      fixerUpper.addFixer(new EntityRidingToPassengersFix(v135, true));
      Schema v143 = fixerUpper.addSchema(143, V143::new);
      fixerUpper.addFixer(new EntityTippedArrowFix(v143, true));
      Schema v147 = fixerUpper.addSchema(147, SAME);
      fixerUpper.addFixer(new EntityArmorStandSilentFix(v147, true));
      Schema v165 = fixerUpper.addSchema(165, SAME);
      fixerUpper.addFixer(new SignTextStrictJsonFix(v165));
      fixerUpper.addFixer(new WrittenBookPagesStrictJsonFix(v165));
      Schema v501 = fixerUpper.addSchema(501, V501::new);
      fixerUpper.addFixer(new AddNewChoices(v501, "Add 1.10 entities fix", References.ENTITY));
      Schema v502 = fixerUpper.addSchema(502, SAME);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v502,
            "cooked_fished item renamer",
            item -> Objects.equals(NamespacedSchema.ensureNamespaced(item), "minecraft:cooked_fished") ? "minecraft:cooked_fish" : item
         )
      );
      fixerUpper.addFixer(new EntityZombieVillagerTypeFix(v502, false));
      Schema v505 = fixerUpper.addSchema(505, SAME);
      fixerUpper.addFixer(new OptionsForceVBOFix(v505, false));
      Schema v700 = fixerUpper.addSchema(700, V700::new);
      fixerUpper.addFixer(new EntityElderGuardianSplitFix(v700, true));
      Schema v701 = fixerUpper.addSchema(701, V701::new);
      fixerUpper.addFixer(new EntitySkeletonSplitFix(v701, true));
      Schema v702 = fixerUpper.addSchema(702, V702::new);
      fixerUpper.addFixer(new EntityZombieSplitFix(v702));
      Schema v703 = fixerUpper.addSchema(703, V703::new);
      fixerUpper.addFixer(new EntityHorseSplitFix(v703, true));
      Schema v704 = fixerUpper.addSchema(704, V704::new);
      fixerUpper.addFixer(new BlockEntityIdFix(v704, true));
      Schema v705 = fixerUpper.addSchema(705, V705::new);
      fixerUpper.addFixer(new EntityIdFix(v705, true));
      Schema v804 = fixerUpper.addSchema(804, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemBannerColorFix(v804, true));
      Schema v806 = fixerUpper.addSchema(806, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemWaterPotionFix(v806, false));
      Schema v808 = fixerUpper.addSchema(808, V808::new);
      fixerUpper.addFixer(new AddNewChoices(v808, "added shulker box", References.BLOCK_ENTITY));
      Schema v808_1 = fixerUpper.addSchema(808, 1, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityShulkerColorFix(v808_1, false));
      Schema v813 = fixerUpper.addSchema(813, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemShulkerBoxColorFix(v813, false));
      fixerUpper.addFixer(new BlockEntityShulkerBoxColorFix(v813, false));
      Schema v816 = fixerUpper.addSchema(816, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsLowerCaseLanguageFix(v816, false));
      Schema v820 = fixerUpper.addSchema(820, SAME_NAMESPACED);
      fixerUpper.addFixer(ItemRenameFix.create(v820, "totem item renamer", createRenamer("minecraft:totem", "minecraft:totem_of_undying")));
      Schema v1022 = fixerUpper.addSchema(1022, V1022::new);
      fixerUpper.addFixer(new WriteAndReadFix(v1022, "added shoulder entities to players", References.PLAYER));
      Schema v1125 = fixerUpper.addSchema(1125, V1125::new);
      fixerUpper.addFixer(new ChunkBedBlockEntityInjecterFix(v1125, true));
      fixerUpper.addFixer(new BedItemColorFix(v1125, false));
      Schema v1344 = fixerUpper.addSchema(1344, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsKeyLwjgl3Fix(v1344, false));
      Schema v1446 = fixerUpper.addSchema(1446, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsKeyTranslationFix(v1446, false));
      Schema v1450 = fixerUpper.addSchema(1450, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlockStateStructureTemplateFix(v1450, false));
      Schema v1451 = fixerUpper.addSchema(1451, V1451::new);
      fixerUpper.addFixer(new AddNewChoices(v1451, "AddTrappedChestFix", References.BLOCK_ENTITY));
      Schema v1451_1 = fixerUpper.addSchema(1451, 1, V1451_1::new);
      fixerUpper.addFixer(new ChunkPalettedStorageFix(v1451_1, true));
      Schema v1451_2 = fixerUpper.addSchema(1451, 2, V1451_2::new);
      fixerUpper.addFixer(new BlockEntityBlockStateFix(v1451_2, true));
      Schema v1451_3 = fixerUpper.addSchema(1451, 3, V1451_3::new);
      fixerUpper.addFixer(new EntityBlockStateFix(v1451_3, true));
      fixerUpper.addFixer(new ItemStackMapIdFix(v1451_3, false));
      Schema v1451_4 = fixerUpper.addSchema(1451, 4, V1451_4::new);
      fixerUpper.addFixer(new BlockNameFlatteningFix(v1451_4, true));
      fixerUpper.addFixer(new ItemStackTheFlatteningFix(v1451_4, false));
      Schema v1451_5 = fixerUpper.addSchema(1451, 5, V1451_5::new);
      fixerUpper.addFixer(new RemoveBlockEntityTagFix(v1451_5, true, Set.of("minecraft:noteblock", "minecraft:flower_pot")));
      fixerUpper.addFixer(new ItemStackSpawnEggFix(v1451_5, false, "minecraft:spawn_egg"));
      fixerUpper.addFixer(new EntityWolfColorFix(v1451_5, false));
      fixerUpper.addFixer(new BlockEntityBannerColorFix(v1451_5, false));
      fixerUpper.addFixer(new LevelFlatGeneratorInfoFix(v1451_5, false));
      Schema v1451_6 = fixerUpper.addSchema(1451, 6, V1451_6::new);
      fixerUpper.addFixer(new StatsCounterFix(v1451_6, true));
      fixerUpper.addFixer(new BlockEntityJukeboxFix(v1451_6, false));
      Schema v1451_8 = fixerUpper.addSchema(1451, 7, SAME_NAMESPACED);
      fixerUpper.addFixer(new VillagerTradeFix(v1451_8));
      Schema v1456 = fixerUpper.addSchema(1456, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityItemFrameDirectionFix(v1456, false));
      Schema v1458 = fixerUpper.addSchema(1458, V1458::new);
      fixerUpper.addFixer(new EntityCustomNameToComponentFix(v1458));
      fixerUpper.addFixer(new ItemCustomNameToComponentFix(v1458));
      fixerUpper.addFixer(new BlockEntityCustomNameToComponentFix(v1458));
      Schema v1460 = fixerUpper.addSchema(1460, V1460::new);
      fixerUpper.addFixer(new EntityPaintingMotiveFix(v1460, false));
      Schema v1466 = fixerUpper.addSchema(1466, V1466::new);
      fixerUpper.addFixer(new AddNewChoices(v1466, "Add DUMMY block entity", References.BLOCK_ENTITY));
      fixerUpper.addFixer(new ChunkToProtochunkFix(v1466, true));
      Schema v1470 = fixerUpper.addSchema(1470, V1470::new);
      fixerUpper.addFixer(new AddNewChoices(v1470, "Add 1.13 entities fix", References.ENTITY));
      Schema v1474 = fixerUpper.addSchema(1474, SAME_NAMESPACED);
      fixerUpper.addFixer(new ColorlessShulkerEntityFix(v1474, false));
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1474,
            "Colorless shulker block fixer",
            block -> Objects.equals(NamespacedSchema.ensureNamespaced(block), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : block
         )
      );
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v1474,
            "Colorless shulker item fixer",
            block -> Objects.equals(NamespacedSchema.ensureNamespaced(block), "minecraft:purple_shulker_box") ? "minecraft:shulker_box" : block
         )
      );
      Schema v1475 = fixerUpper.addSchema(1475, SAME_NAMESPACED);
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1475, "Flowing fixer", createRenamer(ImmutableMap.of("minecraft:flowing_water", "minecraft:water", "minecraft:flowing_lava", "minecraft:lava"))
         )
      );
      Schema v1480 = fixerUpper.addSchema(1480, SAME_NAMESPACED);
      fixerUpper.addFixer(BlockRenameFix.create(v1480, "Rename coral blocks", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      fixerUpper.addFixer(ItemRenameFix.create(v1480, "Rename coral items", createRenamer(RenamedCoralFix.RENAMED_IDS)));
      Schema v1481 = fixerUpper.addSchema(1481, V1481::new);
      fixerUpper.addFixer(new AddNewChoices(v1481, "Add conduit", References.BLOCK_ENTITY));
      Schema v1483 = fixerUpper.addSchema(1483, V1483::new);
      fixerUpper.addFixer(new EntityPufferfishRenameFix(v1483, true));
      fixerUpper.addFixer(ItemRenameFix.create(v1483, "Rename pufferfish egg item", createRenamer(EntityPufferfishRenameFix.RENAMED_IDS)));
      Schema v1484 = fixerUpper.addSchema(1484, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v1484,
            "Rename seagrass items",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1484,
            "Rename seagrass blocks",
            createRenamer(ImmutableMap.of("minecraft:sea_grass", "minecraft:seagrass", "minecraft:tall_sea_grass", "minecraft:tall_seagrass"))
         )
      );
      fixerUpper.addFixer(new HeightmapRenamingFix(v1484, false));
      Schema v1486 = fixerUpper.addSchema(1486, V1486::new);
      fixerUpper.addFixer(new EntityCodSalmonFix(v1486, true));
      fixerUpper.addFixer(ItemRenameFix.create(v1486, "Rename cod/salmon egg items", createRenamer(EntityCodSalmonFix.RENAMED_EGG_IDS)));
      Schema v1487 = fixerUpper.addSchema(1487, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v1487,
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
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1487,
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
      Schema v1488 = fixerUpper.addSchema(1488, V1488::new);
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1488, "Rename kelp/kelptop", createRenamer(ImmutableMap.of("minecraft:kelp_top", "minecraft:kelp", "minecraft:kelp", "minecraft:kelp_plant"))
         )
      );
      fixerUpper.addFixer(ItemRenameFix.create(v1488, "Rename kelptop", createRenamer("minecraft:kelp_top", "minecraft:kelp")));
      fixerUpper.addFixer(
         new NamedEntityWriteReadFix(v1488, true, "Command block block entity custom name fix", References.BLOCK_ENTITY, "minecraft:command_block") {
            @Override
            protected <T> Dynamic<T> fix(final Dynamic<T> input) {
               return BlockEntityCustomNameToComponentFix.fixTagCustomName(input);
            }
         }
      );
      fixerUpper.addFixer(
         new DataFix(v1488, false) {
            protected TypeRewriteRule makeRule() {
               Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
               OpticFinder<String> idFinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
               OpticFinder<?> customNameFinder = entityType.findField("CustomName");
               OpticFinder<Pair<String, String>> componentFinder = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
               return this.fixTypeEverywhereTyped(
                  "Command block minecart custom name fix",
                  entityType,
                  input -> {
                     String id = input.getOptional(idFinder).orElse("");
                     return !"minecraft:commandblock_minecart".equals(id)
                        ? input
                        : input.updateTyped(
                           customNameFinder,
                           customName -> customName.update(componentFinder, pair -> pair.mapSecond(LegacyComponentDataFixUtils::createTextComponentJson))
                        );
                  }
               );
            }
         }
      );
      fixerUpper.addFixer(new IglooMetadataRemovalFix(v1488, false));
      Schema v1490 = fixerUpper.addSchema(1490, SAME_NAMESPACED);
      fixerUpper.addFixer(BlockRenameFix.create(v1490, "Rename melon_block", createRenamer("minecraft:melon_block", "minecraft:melon")));
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v1490,
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
      Schema v1492 = fixerUpper.addSchema(1492, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkStructuresTemplateRenameFix(v1492, false));
      Schema v1493 = fileFixerUpper.addSchema(fixerUpper, 1493, SAME_NAMESPACED);
      fileFixerUpper.addFixer(new LegacyStructureFileFix(v1493));
      Schema v1494 = fixerUpper.addSchema(1494, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemStackEnchantmentNamesFix(v1494, false));
      Schema v1496 = fixerUpper.addSchema(1496, SAME_NAMESPACED);
      fixerUpper.addFixer(new LeavesFix(v1496, false));
      Schema v1500 = fixerUpper.addSchema(1500, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlockEntityKeepPacked(v1500, false));
      Schema v1501 = fixerUpper.addSchema(1501, SAME_NAMESPACED);
      fixerUpper.addFixer(new AdvancementsFix(v1501, false));
      Schema v1502 = fixerUpper.addSchema(1502, SAME_NAMESPACED);
      fixerUpper.addFixer(new NamespacedTypeRenameFix(v1502, "Recipes fix", References.RECIPE, createRenamer(RecipesFix.RECIPES)));
      Schema v1506 = fixerUpper.addSchema(1506, SAME_NAMESPACED);
      fixerUpper.addFixer(new LevelDataGeneratorOptionsFix(v1506, false));
      Schema v1510 = fixerUpper.addSchema(1510, V1510::new);
      fixerUpper.addFixer(BlockRenameFix.create(v1510, "Block renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_BLOCKS)));
      fixerUpper.addFixer(ItemRenameFix.create(v1510, "Item renamening fix", createRenamer(EntityTheRenameningFix.RENAMED_ITEMS)));
      fixerUpper.addFixer(new NamespacedTypeRenameFix(v1510, "Recipes renamening fix", References.RECIPE, createRenamer(RecipesRenameningFix.RECIPES)));
      fixerUpper.addFixer(new EntityTheRenameningFix(v1510, true));
      fixerUpper.addFixer(
         new StatsRenameFix(
            v1510,
            "SwimStatsRenameFix",
            ImmutableMap.of("minecraft:swim_one_cm", "minecraft:walk_on_water_one_cm", "minecraft:dive_one_cm", "minecraft:walk_under_water_one_cm")
         )
      );
      Schema v1514 = fixerUpper.addSchema(1514, SAME_NAMESPACED);
      fixerUpper.addFixer(new ScoreboardDisplayNameFix(v1514, "ObjectiveDisplayNameFix", References.OBJECTIVE));
      fixerUpper.addFixer(new ScoreboardDisplayNameFix(v1514, "TeamDisplayNameFix", References.TEAM));
      fixerUpper.addFixer(new ObjectiveRenderTypeFix(v1514));
      Schema v1515 = fixerUpper.addSchema(1515, SAME_NAMESPACED);
      fixerUpper.addFixer(BlockRenameFix.create(v1515, "Rename coral fan blocks", createRenamer(RenamedCoralFansFix.RENAMED_IDS)));
      Schema v1624 = fixerUpper.addSchema(1624, SAME_NAMESPACED);
      fixerUpper.addFixer(new TrappedChestBlockEntityFix(v1624, false));
      Schema v1800 = fixerUpper.addSchema(1800, V1800::new);
      fixerUpper.addFixer(new AddNewChoices(v1800, "Added 1.14 mobs fix", References.ENTITY));
      fixerUpper.addFixer(ItemRenameFix.create(v1800, "Rename dye items", createRenamer(DyeItemRenameFix.RENAMED_IDS)));
      Schema v1801 = fixerUpper.addSchema(1801, V1801::new);
      fixerUpper.addFixer(new AddNewChoices(v1801, "Added Illager Beast", References.ENTITY));
      Schema v1802 = fixerUpper.addSchema(1802, SAME_NAMESPACED);
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v1802,
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
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v1802,
            "Rename sign item & stone slabs",
            createRenamer(ImmutableMap.of("minecraft:stone_slab", "minecraft:smooth_stone_slab", "minecraft:sign", "minecraft:oak_sign"))
         )
      );
      Schema v1803 = fixerUpper.addSchema(1803, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemLoreFix(v1803));
      Schema v1904 = fixerUpper.addSchema(1904, V1904::new);
      fixerUpper.addFixer(new AddNewChoices(v1904, "Added Cats", References.ENTITY));
      fixerUpper.addFixer(new EntityCatSplitFix(v1904, false));
      Schema v1905 = fixerUpper.addSchema(1905, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkStatusFix(v1905, false));
      Schema v1906 = fixerUpper.addSchema(1906, V1906::new);
      fixerUpper.addFixer(new AddNewChoices(v1906, "Add POI Blocks", References.BLOCK_ENTITY));
      Schema v1909 = fixerUpper.addSchema(1909, V1909::new);
      fixerUpper.addFixer(new AddNewChoices(v1909, "Add jigsaw", References.BLOCK_ENTITY));
      Schema v1911 = fixerUpper.addSchema(1911, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkStatusFix2(v1911, false));
      Schema v1914 = fixerUpper.addSchema(1914, SAME_NAMESPACED);
      fixerUpper.addFixer(new WeaponSmithChestLootTableFix(v1914, false));
      Schema v1917 = fixerUpper.addSchema(1917, SAME_NAMESPACED);
      fixerUpper.addFixer(new CatTypeFix(v1917, false));
      Schema v1918 = fixerUpper.addSchema(1918, SAME_NAMESPACED);
      fixerUpper.addFixer(new VillagerDataFix(v1918, "minecraft:villager"));
      fixerUpper.addFixer(new VillagerDataFix(v1918, "minecraft:zombie_villager"));
      Schema v1920 = fixerUpper.addSchema(1920, V1920::new);
      fixerUpper.addFixer(new NewVillageFix(v1920, false));
      fixerUpper.addFixer(new AddNewChoices(v1920, "Add campfire", References.BLOCK_ENTITY));
      Schema v1925 = fixerUpper.addSchema(1925, SAME_NAMESPACED);
      fixerUpper.addFixer(new MapIdFix(v1925));
      Schema v1928 = fixerUpper.addSchema(1928, V1928::new);
      fixerUpper.addFixer(new EntityRavagerRenameFix(v1928, true));
      fixerUpper.addFixer(ItemRenameFix.create(v1928, "Rename ravager egg item", createRenamer(EntityRavagerRenameFix.RENAMED_IDS)));
      Schema v1929 = fixerUpper.addSchema(1929, V1929::new);
      fixerUpper.addFixer(new AddNewChoices(v1929, "Add Wandering Trader and Trader Llama", References.ENTITY));
      Schema v1931 = fixerUpper.addSchema(1931, V1931::new);
      fixerUpper.addFixer(new AddNewChoices(v1931, "Added Fox", References.ENTITY));
      Schema v1936 = fixerUpper.addSchema(1936, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsAddTextBackgroundFix(v1936, false));
      Schema v1946 = fixerUpper.addSchema(1946, SAME_NAMESPACED);
      fixerUpper.addFixer(new ReorganizePoi(v1946, false));
      Schema v1948 = fixerUpper.addSchema(1948, SAME_NAMESPACED);
      fixerUpper.addFixer(new OminousBannerRenameFix(v1948));
      Schema v1953 = fixerUpper.addSchema(1953, SAME_NAMESPACED);
      fixerUpper.addFixer(new OminousBannerBlockEntityRenameFix(v1953, false));
      Schema v1955 = fixerUpper.addSchema(1955, SAME_NAMESPACED);
      fixerUpper.addFixer(new VillagerRebuildLevelAndXpFix(v1955, false));
      fixerUpper.addFixer(new ZombieVillagerRebuildXpFix(v1955, false));
      Schema v1961 = fixerUpper.addSchema(1961, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkLightRemoveFix(v1961, false));
      Schema v1963 = fixerUpper.addSchema(1963, SAME_NAMESPACED);
      fixerUpper.addFixer(new RemoveGolemGossipFix(v1963, false));
      Schema v2100 = fixerUpper.addSchema(2100, V2100::new);
      fixerUpper.addFixer(new AddNewChoices(v2100, "Added Bee and Bee Stinger", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v2100, "Add beehive", References.BLOCK_ENTITY));
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(v2100, "Rename sugar recipe", References.RECIPE, createRenamer("minecraft:sugar", "minecraft:sugar_from_sugar_cane"))
      );
      fixerUpper.addFixer(
         new AdvancementsRenameFix(
            v2100, false, "Rename sugar recipe advancement", createRenamer("minecraft:recipes/misc/sugar", "minecraft:recipes/misc/sugar_from_sugar_cane")
         )
      );
      Schema v2202 = fixerUpper.addSchema(2202, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkBiomeFix(v2202, false));
      Schema v2209 = fixerUpper.addSchema(2209, SAME_NAMESPACED);
      UnaryOperator<String> beehiveRenamer = createRenamer("minecraft:bee_hive", "minecraft:beehive");
      fixerUpper.addFixer(ItemRenameFix.create(v2209, "Rename bee_hive item to beehive", beehiveRenamer));
      fixerUpper.addFixer(new PoiTypeRenameFix(v2209, "Rename bee_hive poi to beehive", beehiveRenamer));
      fixerUpper.addFixer(BlockRenameFix.create(v2209, "Rename bee_hive block to beehive", beehiveRenamer));
      Schema v2211 = fixerUpper.addSchema(2211, SAME_NAMESPACED);
      fixerUpper.addFixer(new StructureReferenceCountFix(v2211, false));
      Schema v2218 = fixerUpper.addSchema(2218, SAME_NAMESPACED);
      fixerUpper.addFixer(new ForcePoiRebuild(v2218, false));
      Schema v2501 = fixerUpper.addSchema(2501, V2501::new);
      fixerUpper.addFixer(new FurnaceRecipeFix(v2501, true));
      Schema v2502 = fixerUpper.addSchema(2502, V2502::new);
      fixerUpper.addFixer(new AddNewChoices(v2502, "Added Hoglin", References.ENTITY));
      Schema v2503 = fixerUpper.addSchema(2503, SAME_NAMESPACED);
      fixerUpper.addFixer(new WallPropertyFix(v2503, false));
      fixerUpper.addFixer(
         new AdvancementsRenameFix(
            v2503, false, "Composter category change", createRenamer("minecraft:recipes/misc/composter", "minecraft:recipes/decorations/composter")
         )
      );
      Schema v2505 = fixerUpper.addSchema(2505, V2505::new);
      fixerUpper.addFixer(new AddNewChoices(v2505, "Added Piglin", References.ENTITY));
      fixerUpper.addFixer(new MemoryExpiryDataFix(v2505, "minecraft:villager"));
      Schema v2508 = fixerUpper.addSchema(2508, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v2508,
            "Renamed fungi items to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v2508,
            "Renamed fungi blocks to fungus",
            createRenamer(ImmutableMap.of("minecraft:warped_fungi", "minecraft:warped_fungus", "minecraft:crimson_fungi", "minecraft:crimson_fungus"))
         )
      );
      Schema v2509 = fixerUpper.addSchema(2509, V2509::new);
      fixerUpper.addFixer(new EntityZombifiedPiglinRenameFix(v2509));
      fixerUpper.addFixer(ItemRenameFix.create(v2509, "Rename zombie pigman egg item", createRenamer(EntityZombifiedPiglinRenameFix.RENAMED_IDS)));
      Schema v2511 = fixerUpper.addSchema(2511, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityProjectileOwnerFix(v2511));
      Schema v2511_1 = fixerUpper.addSchema(2511, 1, V2511_1::new);
      fixerUpper.addFixer(new NamedEntityConvertUncheckedFix(v2511_1, "SplashPotionItemFieldRenameFix", References.ENTITY, "minecraft:potion"));
      Schema v2514 = fixerUpper.addSchema(2514, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityUUIDFix(v2514));
      fixerUpper.addFixer(new BlockEntityUUIDFix(v2514));
      fixerUpper.addFixer(new PlayerUUIDFix(v2514));
      fixerUpper.addFixer(new LevelUUIDFix(v2514));
      fixerUpper.addFixer(new SavedDataUUIDFix(v2514));
      fixerUpper.addFixer(new ItemStackUUIDFix(v2514));
      Schema v2516 = fixerUpper.addSchema(2516, SAME_NAMESPACED);
      fixerUpper.addFixer(new GossipUUIDFix(v2516, "minecraft:villager"));
      fixerUpper.addFixer(new GossipUUIDFix(v2516, "minecraft:zombie_villager"));
      Schema v2518 = fixerUpper.addSchema(2518, SAME_NAMESPACED);
      fixerUpper.addFixer(new JigsawPropertiesFix(v2518, false));
      fixerUpper.addFixer(new JigsawRotationFix(v2518));
      Schema v2519 = fixerUpper.addSchema(2519, V2519::new);
      fixerUpper.addFixer(new AddNewChoices(v2519, "Added Strider", References.ENTITY));
      Schema v2522 = fixerUpper.addSchema(2522, V2522::new);
      fixerUpper.addFixer(new AddNewChoices(v2522, "Added Zoglin", References.ENTITY));
      Schema v2523 = fixerUpper.addSchema(2523, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new AttributesRenameLegacy(
            v2523,
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
      Schema v2527 = fixerUpper.addSchema(2527, SAME_NAMESPACED);
      fixerUpper.addFixer(new BitStorageAlignFix(v2527));
      Schema v2528 = fixerUpper.addSchema(2528, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v2528,
            "Rename soul fire torch and soul fire lantern",
            createRenamer(ImmutableMap.of("minecraft:soul_fire_torch", "minecraft:soul_torch", "minecraft:soul_fire_lantern", "minecraft:soul_lantern"))
         )
      );
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v2528,
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
      Schema v2529 = fixerUpper.addSchema(2529, SAME_NAMESPACED);
      fixerUpper.addFixer(new StriderGravityFix(v2529, false));
      Schema v2531 = fixerUpper.addSchema(2531, SAME_NAMESPACED);
      fixerUpper.addFixer(new RedstoneWireConnectionsFix(v2531));
      Schema v2533 = fixerUpper.addSchema(2533, SAME_NAMESPACED);
      fixerUpper.addFixer(new VillagerFollowRangeFix(v2533));
      Schema v2535 = fixerUpper.addSchema(2535, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityShulkerRotationFix(v2535));
      Schema v2537 = fixerUpper.addSchema(2537, SAME_NAMESPACED);
      fixerUpper.addFixer(new LegacyDimensionIdFix(v2537));
      Schema v2538 = fixerUpper.addSchema(2538, SAME_NAMESPACED);
      fixerUpper.addFixer(new LevelLegacyWorldGenSettingsFix(v2538));
      Schema v2550 = fixerUpper.addSchema(2550, SAME_NAMESPACED);
      fixerUpper.addFixer(new WorldGenSettingsFix(v2550));
      Schema v2551 = fixerUpper.addSchema(2551, V2551::new);
      fixerUpper.addFixer(new WriteAndReadFix(v2551, "add types to WorldGenData", References.WORLD_GEN_SETTINGS));
      Schema v2552 = fixerUpper.addSchema(2552, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(v2552, "Nether biome rename", References.BIOME, createRenamer("minecraft:nether", "minecraft:nether_wastes"))
      );
      Schema v2553 = fixerUpper.addSchema(2553, SAME_NAMESPACED);
      fixerUpper.addFixer(new NamespacedTypeRenameFix(v2553, "Biomes fix", References.BIOME, createRenamer(BiomeFix.BIOMES)));
      Schema v2556 = fixerUpper.addSchema(2556, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsFancyGraphicsToGraphicsModeFix(v2556));
      Schema v2558 = fixerUpper.addSchema(2558, SAME_NAMESPACED);
      fixerUpper.addFixer(new MissingDimensionFix(v2558, false));
      fixerUpper.addFixer(new OptionsRenameFieldFix(v2558, false, "Rename swapHands setting", "key_key.swapHands", "key_key.swapOffhand"));
      Schema v2568 = fixerUpper.addSchema(2568, V2568::new);
      fixerUpper.addFixer(new AddNewChoices(v2568, "Added Piglin Brute", References.ENTITY));
      Schema v2571 = fixerUpper.addSchema(2571, V2571::new);
      fixerUpper.addFixer(new AddNewChoices(v2571, "Added Goat", References.ENTITY));
      Schema v2679 = fixerUpper.addSchema(2679, SAME_NAMESPACED);
      fixerUpper.addFixer(new CauldronRenameFix(v2679, false));
      Schema v2680 = fixerUpper.addSchema(2680, SAME_NAMESPACED);
      fixerUpper.addFixer(ItemRenameFix.create(v2680, "Renamed grass path item to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      fixerUpper.addFixer(BlockRenameFix.create(v2680, "Renamed grass path block to dirt path", createRenamer("minecraft:grass_path", "minecraft:dirt_path")));
      Schema v2684 = fixerUpper.addSchema(2684, V2684::new);
      fixerUpper.addFixer(new AddNewChoices(v2684, "Added Sculk Sensor", References.BLOCK_ENTITY));
      Schema v2686 = fixerUpper.addSchema(2686, V2686::new);
      fixerUpper.addFixer(new AddNewChoices(v2686, "Added Axolotl", References.ENTITY));
      Schema v2688 = fixerUpper.addSchema(2688, V2688::new);
      fixerUpper.addFixer(new AddNewChoices(v2688, "Added Glow Squid", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v2688, "Added Glow Item Frame", References.ENTITY));
      Schema v2690 = fixerUpper.addSchema(2690, SAME_NAMESPACED);
      ImmutableMap<String, String> copperBlocksToRename = ImmutableMap.builder()
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
      fixerUpper.addFixer(ItemRenameFix.create(v2690, "Renamed copper block items to new oxidized terms", createRenamer(copperBlocksToRename)));
      fixerUpper.addFixer(BlockRenameFix.create(v2690, "Renamed copper blocks to new oxidized terms", createRenamer(copperBlocksToRename)));
      Schema v2691 = fixerUpper.addSchema(2691, SAME_NAMESPACED);
      ImmutableMap<String, String> waxedCopperRename = ImmutableMap.builder()
         .put("minecraft:waxed_copper", "minecraft:waxed_copper_block")
         .put("minecraft:oxidized_copper_block", "minecraft:oxidized_copper")
         .put("minecraft:weathered_copper_block", "minecraft:weathered_copper")
         .put("minecraft:exposed_copper_block", "minecraft:exposed_copper")
         .build();
      fixerUpper.addFixer(ItemRenameFix.create(v2691, "Rename copper item suffixes", createRenamer(waxedCopperRename)));
      fixerUpper.addFixer(BlockRenameFix.create(v2691, "Rename copper blocks suffixes", createRenamer(waxedCopperRename)));
      Schema v2693 = fixerUpper.addSchema(2693, SAME_NAMESPACED);
      fixerUpper.addFixer(new AddFlagIfNotPresentFix(v2693, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema v2696 = fixerUpper.addSchema(2696, SAME_NAMESPACED);
      ImmutableMap<String, String> grimstoneBlocksToRename = ImmutableMap.builder()
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
      fixerUpper.addFixer(ItemRenameFix.create(v2696, "Renamed grimstone block items to deepslate", createRenamer(grimstoneBlocksToRename)));
      fixerUpper.addFixer(BlockRenameFix.create(v2696, "Renamed grimstone blocks to deepslate", createRenamer(grimstoneBlocksToRename)));
      Schema v2700 = fixerUpper.addSchema(2700, SAME_NAMESPACED);
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v2700,
            "Renamed cave vines blocks",
            createRenamer(ImmutableMap.of("minecraft:cave_vines_head", "minecraft:cave_vines", "minecraft:cave_vines_body", "minecraft:cave_vines_plant"))
         )
      );
      Schema v2701 = fixerUpper.addSchema(2701, SAME_NAMESPACED);
      fixerUpper.addFixer(new SavedDataFeaturePoolElementFix(v2701));
      Schema v2702 = fixerUpper.addSchema(2702, SAME_NAMESPACED);
      fixerUpper.addFixer(new AbstractArrowPickupFix(v2702));
      Schema v2704 = fixerUpper.addSchema(2704, V2704::new);
      fixerUpper.addFixer(new AddNewChoices(v2704, "Added Goat", References.ENTITY));
      Schema v2707 = fixerUpper.addSchema(2707, V2707::new);
      fixerUpper.addFixer(new AddNewChoices(v2707, "Added Marker", References.ENTITY));
      fixerUpper.addFixer(new AddFlagIfNotPresentFix(v2707, References.WORLD_GEN_SETTINGS, "has_increased_height_already", true));
      Schema v2710 = fixerUpper.addSchema(2710, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new StatsRenameFix(v2710, "Renamed play_one_minute stat to play_time", ImmutableMap.of("minecraft:play_one_minute", "minecraft:play_time"))
      );
      Schema v2717 = fixerUpper.addSchema(2717, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v2717, "Rename azalea_leaves_flowers", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      fixerUpper.addFixer(
         BlockRenameFix.create(
            v2717, "Rename azalea_leaves_flowers items", createRenamer(ImmutableMap.of("minecraft:azalea_leaves_flowers", "minecraft:flowering_azalea_leaves"))
         )
      );
      Schema v2825 = fixerUpper.addSchema(2825, SAME_NAMESPACED);
      fixerUpper.addFixer(new AddFlagIfNotPresentFix(v2825, References.WORLD_GEN_SETTINGS, "has_increased_height_already", false));
      Schema v2831 = fixerUpper.addSchema(2831, V2831::new);
      fixerUpper.addFixer(new SpawnerDataFix(v2831));
      Schema v2832 = fixerUpper.addSchema(2832, V2832::new);
      fixerUpper.addFixer(new WorldGenSettingsHeightAndBiomeFix(v2832));
      fixerUpper.addFixer(new ChunkHeightAndBiomeFix(v2832));
      Schema v2833 = fixerUpper.addSchema(2833, SAME_NAMESPACED);
      fixerUpper.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(v2833));
      Schema v2838 = fixerUpper.addSchema(2838, SAME_NAMESPACED);
      fixerUpper.addFixer(new NamespacedTypeRenameFix(v2838, "Caves and Cliffs biome renames", References.BIOME, createRenamer(CavesAndCliffsRenames.RENAMES)));
      Schema v2841 = fixerUpper.addSchema(2841, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkProtoTickListFix(v2841));
      Schema v2842 = fixerUpper.addSchema(2842, V2842::new);
      fixerUpper.addFixer(new ChunkRenamesFix(v2842));
      Schema v2843 = fixerUpper.addSchema(2843, SAME_NAMESPACED);
      fixerUpper.addFixer(new OverreachingTickFix(v2843));
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(v2843, "Remove Deep Warm Ocean", References.BIOME, createRenamer("minecraft:deep_warm_ocean", "minecraft:warm_ocean"))
      );
      Schema v2846 = fixerUpper.addSchema(2846, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new AdvancementsRenameFix(
            v2846,
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
      Schema v2852 = fixerUpper.addSchema(2852, SAME_NAMESPACED);
      fixerUpper.addFixer(new WorldGenSettingsDisallowOldCustomWorldsFix(v2852));
      Schema v2967 = fixerUpper.addSchema(2967, SAME_NAMESPACED);
      fixerUpper.addFixer(new StructureSettingsFlattenFix(v2967));
      Schema v2970 = fixerUpper.addSchema(2970, SAME_NAMESPACED);
      fixerUpper.addFixer(new StructuresBecomeConfiguredFix(v2970));
      Schema v3076 = fixerUpper.addSchema(3076, V3076::new);
      fixerUpper.addFixer(new AddNewChoices(v3076, "Added Sculk Catalyst", References.BLOCK_ENTITY));
      Schema v3077 = fixerUpper.addSchema(3077, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkDeleteIgnoredLightDataFix(v3077));
      Schema v3078 = fixerUpper.addSchema(3078, V3078::new);
      fixerUpper.addFixer(new AddNewChoices(v3078, "Added Frog", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v3078, "Added Tadpole", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v3078, "Added Sculk Shrieker", References.BLOCK_ENTITY));
      Schema v3081 = fixerUpper.addSchema(3081, V3081::new);
      fixerUpper.addFixer(new AddNewChoices(v3081, "Added Warden", References.ENTITY));
      Schema v3082 = fixerUpper.addSchema(3082, V3082::new);
      fixerUpper.addFixer(new AddNewChoices(v3082, "Added Chest Boat", References.ENTITY));
      Schema v3083 = fixerUpper.addSchema(3083, V3083::new);
      fixerUpper.addFixer(new AddNewChoices(v3083, "Added Allay", References.ENTITY));
      Schema v3084 = fixerUpper.addSchema(3084, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(
            v3084,
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
      Schema v3086 = fixerUpper.addSchema(3086, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new EntityVariantFix(v3086, "Change cat variant type", References.ENTITY, "minecraft:cat", "CatType", Util.make(new Int2ObjectOpenHashMap(), m -> {
            m.defaultReturnValue("minecraft:tabby");
            m.put(0, "minecraft:tabby");
            m.put(1, "minecraft:black");
            m.put(2, "minecraft:red");
            m.put(3, "minecraft:siamese");
            m.put(4, "minecraft:british");
            m.put(5, "minecraft:calico");
            m.put(6, "minecraft:persian");
            m.put(7, "minecraft:ragdoll");
            m.put(8, "minecraft:white");
            m.put(9, "minecraft:jellie");
            m.put(10, "minecraft:all_black");
         })::get)
      );
      ImmutableMap<String, String> catAdvancementMigrationMap = ImmutableMap.builder()
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
      fixerUpper.addFixer(
         new CriteriaRenameFix(
            v3086, "Migrate cat variant advancement", "minecraft:husbandry/complete_catalogue", s -> (String)catAdvancementMigrationMap.getOrDefault(s, s)
         )
      );
      Schema v3087 = fixerUpper.addSchema(3087, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new EntityVariantFix(v3087, "Change frog variant type", References.ENTITY, "minecraft:frog", "Variant", Util.make(new Int2ObjectOpenHashMap(), m -> {
            m.put(0, "minecraft:temperate");
            m.put(1, "minecraft:warm");
            m.put(2, "minecraft:cold");
         })::get)
      );
      Schema v3090 = fixerUpper.addSchema(3090, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new EntityFieldsRenameFix(v3090, "EntityPaintingFieldsRenameFix", "minecraft:painting", Map.of("Motive", "variant", "Facing", "facing"))
      );
      Schema v3093 = fixerUpper.addSchema(3093, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityGoatMissingStateFix(v3093));
      Schema v3094 = fixerUpper.addSchema(3094, SAME_NAMESPACED);
      fixerUpper.addFixer(new GoatHornIdFix(v3094));
      Schema v3097 = fixerUpper.addSchema(3097, SAME_NAMESPACED);
      fixerUpper.addFixer(new FilteredBooksFix(v3097));
      fixerUpper.addFixer(new FilteredSignsFix(v3097));
      Map<String, String> renamedCatCriteria = Map.of("minecraft:british", "minecraft:british_shorthair");
      fixerUpper.addFixer(new VariantRenameFix(v3097, "Rename british shorthair", References.ENTITY, "minecraft:cat", renamedCatCriteria));
      fixerUpper.addFixer(
         new CriteriaRenameFix(
            v3097,
            "Migrate cat variant advancement for british shorthair",
            "minecraft:husbandry/complete_catalogue",
            s -> renamedCatCriteria.getOrDefault(s, s)
         )
      );
      fixerUpper.addFixer(new PoiTypeRemoveFix(v3097, "Remove unpopulated villager PoI types", Set.of("minecraft:unemployed", "minecraft:nitwit")::contains));
      Schema v3108 = fixerUpper.addSchema(3108, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlendingDataRemoveFromNetherEndFix(v3108));
      Schema v3201 = fixerUpper.addSchema(3201, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsProgrammerArtFix(v3201));
      Schema v3202 = fixerUpper.addSchema(3202, V3202::new);
      fixerUpper.addFixer(new AddNewChoices(v3202, "Added Hanging Sign", References.BLOCK_ENTITY));
      Schema v3203 = fixerUpper.addSchema(3203, V3203::new);
      fixerUpper.addFixer(new AddNewChoices(v3203, "Added Camel", References.ENTITY));
      Schema v3204 = fixerUpper.addSchema(3204, V3204::new);
      fixerUpper.addFixer(new AddNewChoices(v3204, "Added Chiseled Bookshelf", References.BLOCK_ENTITY));
      Schema v3209 = fixerUpper.addSchema(3209, SAME_NAMESPACED);
      fixerUpper.addFixer(new ItemStackSpawnEggFix(v3209, false, "minecraft:pig_spawn_egg"));
      Schema v3214 = fixerUpper.addSchema(3214, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsAmbientOcclusionFix(v3214));
      Schema v3319 = fixerUpper.addSchema(3319, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsAccessibilityOnboardFix(v3319));
      Schema v3322 = fixerUpper.addSchema(3322, SAME_NAMESPACED);
      fixerUpper.addFixer(new EffectDurationFix(v3322));
      Schema v3325 = fixerUpper.addSchema(3325, V3325::new);
      fixerUpper.addFixer(new AddNewChoices(v3325, "Added displays", References.ENTITY));
      Schema v3326 = fixerUpper.addSchema(3326, V3326::new);
      fixerUpper.addFixer(new AddNewChoices(v3326, "Added Sniffer", References.ENTITY));
      Schema v3327 = fixerUpper.addSchema(3327, V3327::new);
      fixerUpper.addFixer(new AddNewChoices(v3327, "Archaeology", References.BLOCK_ENTITY));
      Schema v3328 = fixerUpper.addSchema(3328, V3328::new);
      fixerUpper.addFixer(new AddNewChoices(v3328, "Added interaction", References.ENTITY));
      Schema v3438 = fixerUpper.addSchema(3438, V3438::new);
      fixerUpper.addFixer(
         BlockEntityRenameFix.create(
            v3438, "Rename Suspicious Sand to Brushable Block", createRenamer("minecraft:suspicious_sand", "minecraft:brushable_block")
         )
      );
      fixerUpper.addFixer(new EntityBrushableBlockFieldsRenameFix(v3438));
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v3438,
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
      fixerUpper.addFixer(new AddNewChoices(v3438, "Added calibrated sculk sensor", References.BLOCK_ENTITY));
      Schema v3439 = fixerUpper.addSchema(3439, V3439::new);
      fixerUpper.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(v3439, "Updated sign text format for Signs", "minecraft:sign"));
      Schema v3439_1 = fixerUpper.addSchema(3439, 1, V3439_1::new);
      fixerUpper.addFixer(new BlockEntitySignDoubleSidedEditableTextFix(v3439_1, "Updated sign text format for Hanging Signs", "minecraft:hanging_sign"));
      Schema v3440 = fixerUpper.addSchema(3440, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(
            v3440,
            "Replace experimental 1.20 overworld",
            References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
            createRenamer("minecraft:overworld_update_1_20", "minecraft:overworld")
         )
      );
      fixerUpper.addFixer(new FeatureFlagRemoveFix(v3440, "Remove 1.20 feature toggle", Set.of("minecraft:update_1_20")));
      Schema v3447 = fixerUpper.addSchema(3447, SAME_NAMESPACED);
      fixerUpper.addFixer(
         ItemRenameFix.create(
            v3447,
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
                  .collect(Collectors.toMap(Function.identity(), key -> key.replace("_pottery_shard", "_pottery_sherd")))
            )
         )
      );
      Schema v3448 = fixerUpper.addSchema(3448, V3448::new);
      fixerUpper.addFixer(new DecoratedPotFieldRenameFix(v3448));
      Schema v3450 = fixerUpper.addSchema(3450, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new RemapChunkStatusFix(
            v3450,
            "Remove liquid_carvers and heightmap chunk statuses",
            createRenamer(Map.of("minecraft:liquid_carvers", "minecraft:carvers", "minecraft:heightmaps", "minecraft:spawn"))
         )
      );
      Schema v3451 = fixerUpper.addSchema(3451, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkDeleteLightFix(v3451));
      Schema v3459 = fixerUpper.addSchema(3459, SAME_NAMESPACED);
      fixerUpper.addFixer(new LegacyDragonFightFix(v3459));
      Schema v3564 = fixerUpper.addSchema(3564, SAME_NAMESPACED);
      fixerUpper.addFixer(new DropInvalidSignDataFix(v3564, "minecraft:sign"));
      Schema v3564_1 = fixerUpper.addSchema(3564, 1, SAME_NAMESPACED);
      fixerUpper.addFixer(new DropInvalidSignDataFix(v3564_1, "minecraft:hanging_sign"));
      Schema v3565 = fixerUpper.addSchema(3565, SAME_NAMESPACED);
      fixerUpper.addFixer(new RandomSequenceSettingsFix(v3565));
      Schema v3566 = fixerUpper.addSchema(3566, SAME_NAMESPACED);
      fixerUpper.addFixer(new ScoreboardDisplaySlotFix(v3566));
      Schema v3568 = fixerUpper.addSchema(3568, SAME_NAMESPACED);
      fixerUpper.addFixer(new MobEffectIdFix(v3568));
      Schema v3682 = fixerUpper.addSchema(3682, V3682::new);
      fixerUpper.addFixer(new AddNewChoices(v3682, "Added Crafter", References.BLOCK_ENTITY));
      Schema v3683 = fixerUpper.addSchema(3683, V3683::new);
      fixerUpper.addFixer(new PrimedTntBlockStateFixer(v3683));
      Schema v3685 = fixerUpper.addSchema(3685, V3685::new);
      fixerUpper.addFixer(new FixProjectileStoredItem(v3685));
      Schema v3689 = fixerUpper.addSchema(3689, V3689::new);
      fixerUpper.addFixer(new AddNewChoices(v3689, "Added Breeze", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v3689, "Added Trial Spawner", References.BLOCK_ENTITY));
      Schema v3692 = fixerUpper.addSchema(3692, SAME_NAMESPACED);
      UnaryOperator<String> grassRenamer = createRenamer(Map.of("minecraft:grass", "minecraft:short_grass"));
      fixerUpper.addFixer(BlockRenameFix.create(v3692, "Rename grass block to short_grass", grassRenamer));
      fixerUpper.addFixer(ItemRenameFix.create(v3692, "Rename grass item to short_grass", grassRenamer));
      Schema v3799 = fixerUpper.addSchema(3799, V3799::new);
      fixerUpper.addFixer(new AddNewChoices(v3799, "Added Armadillo", References.ENTITY));
      Schema v3800 = fixerUpper.addSchema(3800, SAME_NAMESPACED);
      UnaryOperator<String> scuteRenamer = createRenamer(Map.of("minecraft:scute", "minecraft:turtle_scute"));
      fixerUpper.addFixer(ItemRenameFix.create(v3800, "Rename scute item to turtle_scute", scuteRenamer));
      Schema v3803 = fixerUpper.addSchema(3803, SAME_NAMESPACED);
      fixerUpper.addFixer(new RenameEnchantmentsFix(v3803, "Rename sweeping enchant to sweeping_edge", Map.of("minecraft:sweeping", "minecraft:sweeping_edge")));
      Schema v3807 = fixerUpper.addSchema(3807, V3807::new);
      fixerUpper.addFixer(new AddNewChoices(v3807, "Added Vault", References.BLOCK_ENTITY));
      Schema v3807_1 = fixerUpper.addSchema(3807, 1, SAME_NAMESPACED);
      fixerUpper.addFixer(new MapBannerBlockPosFormatFix(v3807_1));
      Schema v3808 = fixerUpper.addSchema(3808, V3808::new);
      fixerUpper.addFixer(new HorseBodyArmorItemFix(v3808, "minecraft:horse", "ArmorItem", true));
      Schema v3808_1 = fixerUpper.addSchema(3808, 1, V3808_1::new);
      fixerUpper.addFixer(new HorseBodyArmorItemFix(v3808_1, "minecraft:llama", "DecorItem", false));
      Schema v3808_2 = fixerUpper.addSchema(3808, 2, V3808_2::new);
      fixerUpper.addFixer(new HorseBodyArmorItemFix(v3808_2, "minecraft:trader_llama", "DecorItem", false));
      Schema v3809 = fixerUpper.addSchema(3809, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChestedHorsesInventoryZeroIndexingFix(v3809));
      Schema v3812 = fixerUpper.addSchema(3812, SAME_NAMESPACED);
      fixerUpper.addFixer(new FixWolfHealth(v3812));
      Schema v3813 = fixerUpper.addSchema(3813, V3813::new);
      fixerUpper.addFixer(new BlockPosFormatAndRenamesFix(v3813));
      Schema v3814 = fixerUpper.addSchema(3814, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new AttributesRenameLegacy(v3814, "Rename jump strength attribute", createRenamer("minecraft:horse.jump_strength", "minecraft:generic.jump_strength"))
      );
      Schema v3816 = fixerUpper.addSchema(3816, V3816::new);
      fixerUpper.addFixer(new AddNewChoices(v3816, "Added Bogged", References.ENTITY));
      Schema v3818 = fixerUpper.addSchema(3818, V3818::new);
      fixerUpper.addFixer(new BeehiveFieldRenameFix(v3818));
      fixerUpper.addFixer(new EmptyItemInHotbarFix(v3818));
      Schema v3818_1 = fixerUpper.addSchema(3818, 1, SAME_NAMESPACED);
      fixerUpper.addFixer(new BannerPatternFormatFix(v3818_1));
      Schema v3818_2 = fixerUpper.addSchema(3818, 2, SAME_NAMESPACED);
      fixerUpper.addFixer(new TippedArrowPotionToItemFix(v3818_2));
      Schema v3818_3 = fixerUpper.addSchema(3818, 3, V3818_3::new);
      fixerUpper.addFixer(new WriteAndReadFix(v3818_3, "Inject data component types", References.DATA_COMPONENTS));
      Schema v3818_4 = fixerUpper.addSchema(3818, 4, V3818_4::new);
      fixerUpper.addFixer(new ParticleUnflatteningFix(v3818_4));
      Schema v3818_5 = fixerUpper.addSchema(3818, 5, V3818_5::new);
      fixerUpper.addFixer(new ItemStackComponentizationFix(v3818_5));
      Schema v3818_6 = fixerUpper.addSchema(3818, 6, SAME_NAMESPACED);
      fixerUpper.addFixer(new AreaEffectCloudPotionFix(v3818_6));
      Schema v3820 = fixerUpper.addSchema(3820, SAME_NAMESPACED);
      fixerUpper.addFixer(new PlayerHeadBlockProfileFix(v3820));
      fixerUpper.addFixer(new LodestoneCompassComponentFix(v3820));
      Schema v3825 = fixerUpper.addSchema(3825, V3825::new);
      fixerUpper.addFixer(new ItemStackCustomNameToOverrideComponentFix(v3825));
      fixerUpper.addFixer(new BannerEntityCustomNameToOverrideComponentFix(v3825));
      fixerUpper.addFixer(new TrialSpawnerConfigFix(v3825));
      fixerUpper.addFixer(new AddNewChoices(v3825, "Added Ominous Item Spawner", References.ENTITY));
      Schema v3828 = fixerUpper.addSchema(3828, SAME_NAMESPACED);
      fixerUpper.addFixer(new EmptyItemInVillagerTradeFix(v3828));
      Schema v3833 = fixerUpper.addSchema(3833, SAME_NAMESPACED);
      fixerUpper.addFixer(new RemoveEmptyItemInBrushableBlockFix(v3833));
      Schema v3938 = fixerUpper.addSchema(3938, V3938::new);
      fixerUpper.addFixer(new ProjectileStoredWeaponFix(v3938));
      Schema v3939 = fixerUpper.addSchema(3939, SAME_NAMESPACED);
      fixerUpper.addFixer(new FeatureFlagRemoveFix(v3939, "Remove 1.21 feature toggle", Set.of("minecraft:update_1_21")));
      Schema v3943 = fixerUpper.addSchema(3943, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsMenuBlurrinessFix(v3943));
      Schema v3945 = fixerUpper.addSchema(3945, SAME_NAMESPACED);
      fixerUpper.addFixer(new AttributeModifierIdFix(v3945));
      fixerUpper.addFixer(new JukeboxTicksSinceSongStartedFix(v3945));
      Schema v4054 = fixerUpper.addSchema(4054, SAME_NAMESPACED);
      fixerUpper.addFixer(new OminousBannerRarityFix(v4054));
      Schema v4055 = fixerUpper.addSchema(4055, SAME_NAMESPACED);
      fixerUpper.addFixer(new AttributeIdPrefixFix(v4055));
      Schema v4059 = fixerUpper.addSchema(4059, V4059::new);
      fixerUpper.addFixer(new FoodToConsumableFix(v4059));
      Schema v4061 = fixerUpper.addSchema(4061, SAME_NAMESPACED);
      fixerUpper.addFixer(new TrialSpawnerConfigInRegistryFix(v4061));
      Schema v4064 = fixerUpper.addSchema(4064, SAME_NAMESPACED);
      fixerUpper.addFixer(new FireResistantToDamageResistantComponentFix(v4064));
      Schema v4067 = fixerUpper.addSchema(4067, V4067::new);
      fixerUpper.addFixer(new BoatSplitFix(v4067));
      fixerUpper.addFixer(new FeatureFlagRemoveFix(v4067, "Remove Bundle experimental feature flag", Set.of("minecraft:bundle")));
      Schema v4068 = fixerUpper.addSchema(4068, SAME_NAMESPACED);
      fixerUpper.addFixer(new LockComponentPredicateFix(v4068));
      fixerUpper.addFixer(new ContainerBlockEntityLockPredicateFix(v4068));
      Schema v4070 = fixerUpper.addSchema(4070, V4070::new);
      fixerUpper.addFixer(new AddNewChoices(v4070, "Added Pale Oak Boat and Pale Oak Chest Boat", References.ENTITY));
      Schema v4071 = fixerUpper.addSchema(4071, V4071::new);
      fixerUpper.addFixer(new AddNewChoices(v4071, "Added Creaking", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v4071, "Added Creaking Heart", References.BLOCK_ENTITY));
      Schema v4081 = fixerUpper.addSchema(4081, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntitySalmonSizeFix(v4081));
      Schema v4173 = fixerUpper.addSchema(4173, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityFieldsRenameFix(v4173, "Rename TNT Minecart fuse", "minecraft:tnt_minecart", Map.of("TNTFuse", "fuse")));
      Schema v4175 = fixerUpper.addSchema(4175, SAME_NAMESPACED);
      fixerUpper.addFixer(new EquippableAssetRenameFix(v4175));
      fixerUpper.addFixer(new CustomModelDataExpandFix(v4175));
      Schema v4176 = fixerUpper.addSchema(4176, SAME_NAMESPACED);
      fixerUpper.addFixer(new InvalidBlockEntityLockFix(v4176));
      fixerUpper.addFixer(new InvalidLockComponentFix(v4176));
      Schema v4180 = fixerUpper.addSchema(4180, SAME_NAMESPACED);
      fixerUpper.addFixer(new FeatureFlagRemoveFix(v4180, "Remove Winter Drop toggle", Set.of("minecraft:winter_drop")));
      Schema v4181 = fixerUpper.addSchema(4181, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlockEntityFurnaceBurnTimeFix(v4181, "minecraft:furnace"));
      fixerUpper.addFixer(new BlockEntityFurnaceBurnTimeFix(v4181, "minecraft:smoker"));
      fixerUpper.addFixer(new BlockEntityFurnaceBurnTimeFix(v4181, "minecraft:blast_furnace"));
      Schema v4187 = fixerUpper.addSchema(4187, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(
            v4187, "Villager follow range fix undo", "minecraft:villager", "minecraft:follow_range", value -> value == 48.0 ? 16.0 : value
         )
      );
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(v4187, "Bee follow range fix", "minecraft:bee", "minecraft:follow_range", value -> value == 48.0 ? 16.0 : value)
      );
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(v4187, "Allay follow range fix", "minecraft:allay", "minecraft:follow_range", value -> value == 48.0 ? 16.0 : value)
      );
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(v4187, "Llama follow range fix", "minecraft:llama", "minecraft:follow_range", value -> value == 40.0 ? 16.0 : value)
      );
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(
            v4187, "Piglin Brute follow range fix", "minecraft:piglin_brute", "minecraft:follow_range", value -> value == 16.0 ? 12.0 : value
         )
      );
      fixerUpper.addFixer(
         new EntityAttributeBaseValueFix(v4187, "Warden follow range fix", "minecraft:warden", "minecraft:follow_range", value -> value == 16.0 ? 24.0 : value)
      );
      Schema v4290 = fixerUpper.addSchema(4290, V4290::new);
      fixerUpper.addFixer(new UnflattenTextComponentFix(v4290));
      Schema v4291 = fixerUpper.addSchema(4291, SAME_NAMESPACED);
      fixerUpper.addFixer(new LegacyHoverEventFix(v4291));
      fixerUpper.addFixer(new TextComponentStringifiedFlagsFix(v4291));
      Schema v4292 = fixerUpper.addSchema(4292, V4292::new);
      fixerUpper.addFixer(new TextComponentHoverAndClickEventFix(v4292));
      Schema v4293 = fixerUpper.addSchema(4293, SAME_NAMESPACED);
      fixerUpper.addFixer(new DropChancesFormatFix(v4293));
      Schema v4294 = fixerUpper.addSchema(4294, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new BlockPropertyRenameAndFix(
            v4294,
            "CreakingHeartBlockStateFix",
            "minecraft:creaking_heart",
            "active",
            "creaking_heart_state",
            value -> value.equals("true") ? "awake" : "uprooted"
         )
      );
      Schema v4296 = fixerUpper.addSchema(4296, SAME_NAMESPACED);
      fixerUpper.addFixer(new AreaEffectCloudDurationScaleFix(v4296));
      Schema v4297 = fixerUpper.addSchema(4297, SAME_NAMESPACED);
      fixerUpper.addFixer(new ForcedChunkToTicketFix(v4297));
      Schema v4299 = fixerUpper.addSchema(4299, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntitySpawnerItemVariantComponentFix(v4299));
      Schema v4300 = fixerUpper.addSchema(4300, V4300::new);
      fixerUpper.addFixer(new SaddleEquipmentSlotFix(v4300));
      Schema v4301 = fixerUpper.addSchema(4301, V4301::new);
      fixerUpper.addFixer(new EquipmentFormatFix(v4301));
      Schema v4302 = fixerUpper.addSchema(4302, V4302::new);
      fixerUpper.addFixer(new AddNewChoices(v4302, "Added Test and Test Instance Block Entities", References.BLOCK_ENTITY));
      Schema v4303 = fixerUpper.addSchema(4303, SAME_NAMESPACED);
      fixerUpper.addFixer(new EntityFallDistanceFloatToDoubleFix(v4303, References.ENTITY));
      fixerUpper.addFixer(new EntityFallDistanceFloatToDoubleFix(v4303, References.PLAYER));
      Schema v4305 = fixerUpper.addSchema(4305, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlockPropertyRenameAndFix(v4305, "rename test block mode", "minecraft:test_block", "test_block_mode", "mode", a -> a));
      Schema v4306 = fixerUpper.addSchema(4306, V4306::new);
      fixerUpper.addFixer(new ThrownPotionSplitFix(v4306));
      Schema v4307 = fixerUpper.addSchema(4307, V4307::new);
      fixerUpper.addFixer(new TooltipDisplayComponentFix(v4307));
      Schema v4309 = fixerUpper.addSchema(4309, SAME_NAMESPACED);
      fixerUpper.addFixer(new RaidRenamesDataFix(v4309));
      fixerUpper.addFixer(new ChunkTicketUnpackPosFix(v4309));
      Schema v4311 = fixerUpper.addSchema(4311, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new AdvancementsRenameFix(
            v4311, false, "Use lodestone category change", createRenamer("minecraft:nether/use_lodestone", "minecraft:adventure/use_lodestone")
         )
      );
      Schema v4312 = fixerUpper.addSchema(4312, V4312::new);
      fixerUpper.addFixer(new PlayerEquipmentFix(v4312));
      Schema v4314 = fixerUpper.addSchema(4314, SAME_NAMESPACED);
      fixerUpper.addFixer(new InlineBlockPosFormatFix(v4314));
      Schema v4420 = fixerUpper.addSchema(4420, V4420::new);
      fixerUpper.addFixer(new NamedEntityConvertUncheckedFix(v4420, "AreaEffectCloudCustomParticleFix", References.ENTITY, "minecraft:area_effect_cloud"));
      Schema v4421 = fixerUpper.addSchema(4421, V4421::new);
      fixerUpper.addFixer(new AddNewChoices(v4421, "Added Happy Ghast", References.ENTITY));
      Schema v4424 = fixerUpper.addSchema(4424, SAME_NAMESPACED);
      fixerUpper.addFixer(new FeatureFlagRemoveFix(v4424, "Remove Locator Bar experimental feature flag", Set.of("minecraft:locator_bar")));
      fixerUpper.addFixer(new AddFieldFix(v4424, References.PLAYER, "style", field -> field.createString("minecraft:default"), "locator_bar_icon"));
      fixerUpper.addFixer(new AddFieldFix(v4424, References.ENTITY, "style", field -> field.createString("minecraft:default"), "locator_bar_icon"));
      Schema v4531 = fixerUpper.addSchema(4531, V4531::new);
      fixerUpper.addFixer(new AddNewChoices(v4531, "Added Copper Golem", References.ENTITY));
      Schema v4532 = fixerUpper.addSchema(4532, V4532::new);
      fixerUpper.addFixer(new AddNewChoices(v4532, "Added Copper Golem Statue Block Entity", References.BLOCK_ENTITY));
      Schema v4533 = fixerUpper.addSchema(4533, V4533::new);
      fixerUpper.addFixer(new AddNewChoices(v4533, "Added Shelf", References.BLOCK_ENTITY));
      Schema v4535 = fixerUpper.addSchema(4535, SAME_NAMESPACED);
      fixerUpper.addFixer(new CopperGolemWeatherStateFix(v4535));
      Schema v4537 = fixerUpper.addSchema(4537, SAME_NAMESPACED);
      fixerUpper.addFixer(new ChunkDeleteLightFix(v4537));
      Schema v4541 = fixerUpper.addSchema(4541, SAME_NAMESPACED);
      fixerUpper.addFixer(BlockRenameFix.create(v4541, "Rename chain to iron_chain", createRenamer("minecraft:chain", "minecraft:iron_chain")));
      fixerUpper.addFixer(ItemRenameFix.create(v4541, "Rename chain to iron_chain", createRenamer("minecraft:chain", "minecraft:iron_chain")));
      Schema v4543 = fixerUpper.addSchema(4543, V4543::new);
      fixerUpper.addFixer(new AddNewChoices(v4543, "Added Mannequin", References.ENTITY));
      Schema v4544 = fixerUpper.addSchema(4544, SAME_NAMESPACED);
      fixerUpper.addFixer(new LegacyWorldBorderFix(v4544));
      Schema v4548 = fixerUpper.addSchema(4548, SAME_NAMESPACED);
      fixerUpper.addFixer(new WorldSpawnDataFix(v4548));
      fixerUpper.addFixer(new PlayerRespawnDataFix(v4548));
      Schema v4648 = fixerUpper.addSchema(4648, V4648::new);
      fixerUpper.addFixer(new AddNewChoices(v4648, "Added Nautilus and Zombie Nautilus", References.ENTITY));
      Schema v4649 = fixerUpper.addSchema(4649, SAME_NAMESPACED);
      fixerUpper.addFixer(new TridentAnimationFix(v4649));
      Schema v4650 = fixerUpper.addSchema(4650, SAME_NAMESPACED);
      fixerUpper.addFixer(new DebugProfileOverlayReferenceFix(v4650));
      Schema v4651 = fixerUpper.addSchema(4651, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsGraphicsModeSplitFix(v4651, "cutoutLeaves", "false", "true", "true"));
      fixerUpper.addFixer(new OptionsGraphicsModeSplitFix(v4651, "weatherRadius", "5", "10", "10"));
      fixerUpper.addFixer(new OptionsGraphicsModeSplitFix(v4651, "vignette", "false", "true", "true"));
      fixerUpper.addFixer(new OptionsGraphicsModeSplitFix(v4651, "improvedTransparency", "false", "false", "true"));
      fixerUpper.addFixer(new OptionsSetGraphicsPresetToCustomFix(v4651));
      Schema v4656 = fixerUpper.addSchema(4656, V4656::new);
      fixerUpper.addFixer(new AddNewChoices(v4656, "Added Parched and Camel Husk", References.ENTITY));
      Schema v4657 = fixerUpper.addSchema(4657, SAME_NAMESPACED);
      fixerUpper.addFixer(new WorldBorderWarningTimeFix(v4657));
      Schema v4658 = fixerUpper.addSchema(4658, SAME_NAMESPACED);
      fixerUpper.addFixer(new GameRuleRegistryFix(v4658));
      Schema v4661 = fixerUpper.addSchema(4661, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsMusicToastFix(v4661, false));
      Schema v4763 = fixerUpper.addSchema(4763, SAME_NAMESPACED);
      fixerUpper.addFixer(new DebugProfileLookingAtSplitFix(v4763));
      fixerUpper.addFixer(new VillagerSetVillagerDataFinalized(v4763));
      fixerUpper.addFixer(new ZombieVillagerSetVillagerDataFinalized(v4763));
      Schema v4766 = fixerUpper.addSchema(4766, SAME_NAMESPACED);
      fixerUpper.addFixer(new DayTimeToClockFix(v4766));
      Schema v4769 = fixerUpper.addSchema(4769, SAME_NAMESPACED);
      fixerUpper.addFixer(
         new NamespacedTypeRenameFix(v4769, "Rename stonecutter recipes", References.RECIPE, createRenamer(StonecutterRecipeRenameFix.RECIPE_RENAMES))
      );
      fixerUpper.addFixer(
         new AdvancementsRenameFix(v4769, false, "Rename stonecutter recipe advancements", createRenamer(StonecutterRecipeRenameFix.ADVANCEMENT_RENAMES))
      );
      Schema v4771 = fixerUpper.addSchema(4771, V4771::new);
      fixerUpper.addFixer(new LevelDatDifficultyFix(v4771));
      fixerUpper.addFixer(new LevelDatToSavedDataPreparationFix(v4771));
      Schema v4772 = fileFixerUpper.addSchema(fixerUpper, 4772, SAME_NAMESPACED);
      fileFixerUpper.addFixer(new ResourcePackLocationFileFix(v4772));
      fileFixerUpper.addFixer(new DimensionStorageFileFix(v4772));
      fileFixerUpper.addFixer(new PlayerStorageFileFix(v4772));
      fileFixerUpper.addFixer(new LevelDatToSavedDataFileFix(v4772));
      fileFixerUpper.addFixer(new RemoveObsoleteFilesFileFix(v4772));
      Schema v4773 = fileFixerUpper.addSchema(fixerUpper, 4773, SAME_NAMESPACED);
      fileFixerUpper.addFixer(new GeneratedStructuresRenameFileFix(v4773));
      Schema v4881 = fixerUpper.addSchema(4881, V4881::new);
      fixerUpper.addFixer(new AddNewChoices(v4881, "Added Sulfur Cube", References.ENTITY));
      fixerUpper.addFixer(new AddNewChoices(v4881, "Added Potent Sulfur", References.BLOCK_ENTITY));
      Schema v4885 = fixerUpper.addSchema(4885, V4885::new);
      fixerUpper.addFixer(new RemoveBlockEntityTagFix(v4885, Set.of("minecraft:bed")));
      Schema v4888 = fixerUpper.addSchema(4888, SAME_NAMESPACED);
      fixerUpper.addFixer(new RenameNameplateToNameTagFix(v4888));
      Schema v4892 = fixerUpper.addSchema(4892, SAME_NAMESPACED);
      fixerUpper.addFixer(new OptionsForceDefaultGraphicsApiFix(v4892));
      Schema v4899 = fileFixerUpper.addSchema(fixerUpper, 4899, SAME_NAMESPACED);
      fileFixerUpper.addFixer(new ReenableSpectatorsGenerateChunksInHardcoreWorldsFileFix(v4899));
      Schema v4996 = fixerUpper.addSchema(4996, V4996::new);
      fixerUpper.addFixer(new PotDecorationsComponentUnflatteningFix(v4996));
      Schema v4996_1 = fixerUpper.addSchema(4996, 1, V4996_1::new);
      fixerUpper.addFixer(new PotDecorationsBlockEntityUnflatteningFix(v4996_1));
      Schema blendingSchema = fixerUpper.addSchema(4997, SAME_NAMESPACED);
      fixerUpper.addFixer(new BlendingDataFix(blendingSchema));
      Schema v4997 = fixerUpper.addSchema(4997, V4997::new);
      fixerUpper.addFixer(new AddNewChoices(v4997, "Added Poplar Boat and Poplar Chest Boat", References.ENTITY));
      Schema v5000 = fixerUpper.addSchema(5000, V5000::new);
      fixerUpper.addFixer(new AddNewChoices(v5000, "Added Cushion", References.ENTITY));
   }

   private static UnaryOperator<String> createRenamerNoNamespace(final Map<String, String> map) {
      return id -> map.getOrDefault(id, id);
   }

   private static UnaryOperator<String> createRenamer(final Map<String, String> map) {
      return id -> map.getOrDefault(NamespacedSchema.ensureNamespaced(id), id);
   }

   private static UnaryOperator<String> createRenamer(final String from, final String to) {
      return id -> Objects.equals(NamespacedSchema.ensureNamespaced(id), from) ? to : id;
   }

   static {
      int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
      DataFixerBuilder fixerUpper = new DataFixerBuilder(dataVersion);
      FileFixerUpper.Builder fileFixerUpper = new FileFixerUpper.Builder(dataVersion);
      addFixers(fixerUpper, fileFixerUpper);
      DATA_FIXER = fixerUpper.build();
      FILE_FIXER = fileFixerUpper.build(DATA_FIXER);
   }
}
