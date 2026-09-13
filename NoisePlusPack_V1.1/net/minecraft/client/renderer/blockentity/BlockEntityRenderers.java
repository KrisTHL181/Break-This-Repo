package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderers {
   private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = Maps.newHashMap();

   private static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
      BlockEntityType<? extends T> p_173591_, BlockEntityRendererProvider<T, S> p_173592_
   ) {
      PROVIDERS.put(p_173591_, p_173592_);
   }

   public static Map<BlockEntityType<?>, BlockEntityRenderer<?, ?>> createEntityRenderers(BlockEntityRendererProvider.Context p_173599_) {
      Builder<BlockEntityType<?>, BlockEntityRenderer<?, ?>> builder = ImmutableMap.builder();
      PROVIDERS.forEach(
         (p_448212_, p_448213_) -> {
            try {
               builder.put(p_448212_, p_448213_.create(p_173599_));
            } catch (Exception exception) {
               throw new IllegalStateException(
                  "Failed to create model for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)p_448212_), exception
               );
            }
         }
      );
      return builder.build();
   }

   static {
      register(BlockEntityType.SIGN, SignRenderer::new);
      register(BlockEntityType.HANGING_SIGN, HangingSignRenderer::new);
      register(BlockEntityType.MOB_SPAWNER, SpawnerRenderer::new);
      register(BlockEntityType.PISTON, p_420903_ -> new PistonHeadRenderer());
      register(BlockEntityType.CHEST, ChestRenderer::new);
      register(BlockEntityType.ENDER_CHEST, ChestRenderer::new);
      register(BlockEntityType.TRAPPED_CHEST, ChestRenderer::new);
      register(BlockEntityType.ENCHANTING_TABLE, EnchantTableRenderer::new);
      register(BlockEntityType.LECTERN, LecternRenderer::new);
      register(BlockEntityType.END_PORTAL, p_420902_ -> new TheEndPortalRenderer());
      register(BlockEntityType.END_GATEWAY, p_420905_ -> new TheEndGatewayRenderer());
      register(BlockEntityType.BEACON, p_420906_ -> new BeaconRenderer());
      register(BlockEntityType.SKULL, SkullBlockRenderer::new);
      register(BlockEntityType.BANNER, BannerRenderer::new);
      register(BlockEntityType.STRUCTURE_BLOCK, p_420904_ -> new BlockEntityWithBoundingBoxRenderer());
      register(BlockEntityType.TEST_INSTANCE_BLOCK, p_448209_ -> new TestInstanceRenderer());
      register(BlockEntityType.SHULKER_BOX, ShulkerBoxRenderer::new);
      register(BlockEntityType.BED, BedRenderer::new);
      register(BlockEntityType.CONDUIT, ConduitRenderer::new);
      register(BlockEntityType.BELL, BellRenderer::new);
      register(BlockEntityType.CAMPFIRE, CampfireRenderer::new);
      register(BlockEntityType.BRUSHABLE_BLOCK, BrushableBlockRenderer::new);
      register(BlockEntityType.DECORATED_POT, DecoratedPotRenderer::new);
      register(BlockEntityType.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
      register(BlockEntityType.VAULT, VaultRenderer::new);
      register(BlockEntityType.COPPER_GOLEM_STATUE, CopperGolemStatueBlockRenderer::new);
      register(BlockEntityType.SHELF, ShelfRenderer::new);
   }
}
