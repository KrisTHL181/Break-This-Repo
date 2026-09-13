package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public class MapItem extends Item {
   public static final int IMAGE_WIDTH = 128;
   public static final int IMAGE_HEIGHT = 128;

   public MapItem(final Item.Properties properties) {
      super(properties);
   }

   public static ItemStack create(
      final ServerLevel level, final int originX, final int originZ, final byte scale, final boolean trackPosition, final boolean unlimitedTracking
   ) {
      ItemStack map = new ItemStack(Items.FILLED_MAP);
      MapId newId = createNewSavedData(level, originX, originZ, scale, trackPosition, unlimitedTracking, level.dimension());
      map.set(DataComponents.MAP_ID, newId);
      return map;
   }

   public static @Nullable MapItemSavedData getSavedData(final @Nullable MapId id, final Level level) {
      return id == null ? null : level.getMapData(id);
   }

   public static @Nullable MapItemSavedData getSavedData(final ItemStack itemStack, final Level level) {
      MapId id = itemStack.get(DataComponents.MAP_ID);
      return getSavedData(id, level);
   }

   private static MapId createNewSavedData(
      final ServerLevel level,
      final int xSpawn,
      final int zSpawn,
      final int scale,
      final boolean trackingPosition,
      final boolean unlimitedTracking,
      final ResourceKey<Level> dimension
   ) {
      MapItemSavedData newData = MapItemSavedData.createFresh(xSpawn, zSpawn, (byte)scale, trackingPosition, unlimitedTracking, dimension);
      MapId id = level.getFreeMapId();
      level.setMapData(id, newData);
      return id;
   }

   public void update(final Level level, final Entity player, final MapItemSavedData data) {
      if (level.dimension() == data.dimension && player instanceof Player player2) {
         int scale = 1 << data.scale;
         int centerX = data.centerX;
         int centerZ = data.centerZ;
         int playerImgX = Mth.floor(player.getX() - centerX) / scale + 64;
         int playerImgY = Mth.floor(player.getZ() - centerZ) / scale + 64;
         int radius = 128 / scale;
         if (level.dimensionType().hasCeiling()) {
            radius /= 2;
         }

         MapItemSavedData.HoldingPlayer holdingPlayer = data.getHoldingPlayer(player2);
         holdingPlayer.step++;
         BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
         boolean foundConsecutiveChanges = false;

         for (int imgX = playerImgX - radius + 1; imgX < playerImgX + radius; imgX++) {
            if ((imgX & 15) == (holdingPlayer.step & 15) || foundConsecutiveChanges) {
               foundConsecutiveChanges = false;
               double previousAverageAreaHeight = 0.0;

               for (int imgY = playerImgY - radius - 1; imgY < playerImgY + radius; imgY++) {
                  if (imgX >= 0 && imgY >= -1 && imgX < 128 && imgY < 128) {
                     int distanceToPlayerSqr = Mth.square(imgX - playerImgX) + Mth.square(imgY - playerImgY);
                     boolean ditherBlack = distanceToPlayerSqr > (radius - 2) * (radius - 2);
                     int averagingAreaMinX = (centerX / scale + imgX - 64) * scale;
                     int averagingAreaMinZ = (centerZ / scale + imgY - 64) * scale;
                     Multiset<MapColor> colorCount = LinkedHashMultiset.create();
                     LevelChunk chunk = level.getChunk(SectionPos.blockToSectionCoord(averagingAreaMinX), SectionPos.blockToSectionCoord(averagingAreaMinZ));
                     if (!chunk.isEmpty()) {
                        int waterDepth = 0;
                        double averageAreaHeight = 0.0;
                        if (level.dimensionType().hasCeiling()) {
                           int ceilingNoise = averagingAreaMinX + averagingAreaMinZ * 231871;
                           ceilingNoise = ceilingNoise * ceilingNoise * 31287121 + ceilingNoise * 11;
                           if ((ceilingNoise >> 20 & 1) == 0) {
                              colorCount.add(Blocks.DIRT.defaultBlockState().getMapColor(level, BlockPos.ZERO), 10);
                           } else {
                              colorCount.add(Blocks.STONE.defaultBlockState().getMapColor(level, BlockPos.ZERO), 100);
                           }

                           averageAreaHeight = 100.0;
                        } else {
                           for (int averagingAreaDeltaX = 0; averagingAreaDeltaX < scale; averagingAreaDeltaX++) {
                              for (int averagingAreaDeltaZ = 0; averagingAreaDeltaZ < scale; averagingAreaDeltaZ++) {
                                 blockPos.set(averagingAreaMinX + averagingAreaDeltaX, 0, averagingAreaMinZ + averagingAreaDeltaZ);
                                 int columnY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockPos.getX(), blockPos.getZ()) + 1;
                                 BlockState state;
                                 if (columnY <= level.getMinY()) {
                                    state = Blocks.BEDROCK.defaultBlockState();
                                 } else {
                                    do {
                                       blockPos.setY(--columnY);
                                       state = chunk.getBlockState(blockPos);
                                    } while (state.getMapColor(level, blockPos) == MapColor.NONE && columnY > level.getMinY());

                                    if (columnY > level.getMinY() && !state.getFluidState().isEmpty()) {
                                       int solidY = columnY - 1;
                                       belowPos.set(blockPos);

                                       BlockState belowBlock;
                                       do {
                                          belowPos.setY(solidY--);
                                          belowBlock = chunk.getBlockState(belowPos);
                                          waterDepth++;
                                       } while (solidY > level.getMinY() && !belowBlock.getFluidState().isEmpty());

                                       state = this.getCorrectStateForFluidBlock(level, state, blockPos);
                                    }
                                 }

                                 data.checkBanners(level, blockPos.getX(), blockPos.getZ());
                                 averageAreaHeight += (double)columnY / (scale * scale);
                                 colorCount.add(state.getMapColor(level, blockPos));
                              }
                           }
                        }

                        waterDepth /= scale * scale;
                        MapColor color = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst(colorCount), MapColor.NONE);
                        MapColor.Brightness brightness;
                        if (color == MapColor.WATER) {
                           double diff = waterDepth * 0.1 + (imgX + imgY & 1) * 0.2;
                           if (diff < 0.5) {
                              brightness = MapColor.Brightness.HIGH;
                           } else if (diff > 0.9) {
                              brightness = MapColor.Brightness.LOW;
                           } else {
                              brightness = MapColor.Brightness.NORMAL;
                           }
                        } else {
                           double diff = (averageAreaHeight - previousAverageAreaHeight) * 4.0 / (scale + 4) + ((imgX + imgY & 1) - 0.5) * 0.4;
                           if (diff > 0.6) {
                              brightness = MapColor.Brightness.HIGH;
                           } else if (diff < -0.6) {
                              brightness = MapColor.Brightness.LOW;
                           } else {
                              brightness = MapColor.Brightness.NORMAL;
                           }
                        }

                        previousAverageAreaHeight = averageAreaHeight;
                        if (imgY >= 0 && distanceToPlayerSqr < radius * radius && (!ditherBlack || (imgX + imgY & 1) != 0)) {
                           foundConsecutiveChanges |= data.updateColor(imgX, imgY, color.getPackedId(brightness));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState getCorrectStateForFluidBlock(final Level level, final BlockState state, final BlockPos pos) {
      FluidState fluidState = state.getFluidState();
      return !fluidState.isEmpty() && !state.isFaceSturdy(level, pos, Direction.UP) ? fluidState.createLegacyBlock() : state;
   }

   private static boolean isBiomeWatery(final boolean[] isBiomeWatery, final int x, final int z) {
      return isBiomeWatery[z * 128 + x];
   }

   public static void renderBiomePreviewMap(final ServerLevel level, final ItemStack mapItemStack) {
      MapItemSavedData data = getSavedData(mapItemStack, level);
      if (data != null) {
         if (level.dimension() == data.dimension) {
            int scale = 1 << data.scale;
            int centerX = data.centerX;
            int centerZ = data.centerZ;
            boolean[] isBiomeWatery = new boolean[16384];
            int unscaledStartX = centerX / scale - 64;
            int unscaledStartZ = centerZ / scale - 64;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int row = 0; row < 128; row++) {
               for (int column = 0; column < 128; column++) {
                  Holder<Biome> biome = level.getBiome(pos.set((unscaledStartX + column) * scale, 0, (unscaledStartZ + row) * scale));
                  isBiomeWatery[row * 128 + column] = biome.is(BiomeTags.WATER_ON_MAP_OUTLINES);
               }
            }

            for (int mx = 1; mx < 127; mx++) {
               for (int mz = 1; mz < 127; mz++) {
                  int waterCount = 0;

                  for (int dx = -1; dx < 2; dx++) {
                     for (int dz = -1; dz < 2; dz++) {
                        if ((dx != 0 || dz != 0) && isBiomeWatery(isBiomeWatery, mx + dx, mz + dz)) {
                           waterCount++;
                        }
                     }
                  }

                  MapColor.Brightness brightness = MapColor.Brightness.LOWEST;
                  MapColor newColor = MapColor.NONE;
                  if (isBiomeWatery(isBiomeWatery, mx, mz)) {
                     newColor = MapColor.COLOR_ORANGE;
                     if (waterCount > 7 && mz % 2 == 0) {
                        switch ((mx + (int)(Mth.sin(mz + 0.0F) * 7.0F)) / 8 % 5) {
                           case 0:
                           case 4:
                              brightness = MapColor.Brightness.LOW;
                              break;
                           case 1:
                           case 3:
                              brightness = MapColor.Brightness.NORMAL;
                              break;
                           case 2:
                              brightness = MapColor.Brightness.HIGH;
                        }
                     } else if (waterCount > 7) {
                        newColor = MapColor.NONE;
                     } else if (waterCount > 5) {
                        brightness = MapColor.Brightness.NORMAL;
                     } else if (waterCount > 3) {
                        brightness = MapColor.Brightness.LOW;
                     } else if (waterCount > 1) {
                        brightness = MapColor.Brightness.LOW;
                     }
                  } else if (waterCount > 0) {
                     newColor = MapColor.COLOR_BROWN;
                     if (waterCount > 3) {
                        brightness = MapColor.Brightness.NORMAL;
                     } else {
                        brightness = MapColor.Brightness.LOWEST;
                     }
                  }

                  if (newColor != MapColor.NONE) {
                     data.setColor(mx, mz, newColor.getPackedId(brightness));
                  }
               }
            }
         }
      }
   }

   @Override
   public void inventoryTick(final ItemStack itemStack, final ServerLevel level, final Entity owner, final @Nullable EquipmentSlot slot) {
      MapItemSavedData data = getSavedData(itemStack, level);
      if (data != null) {
         if (owner instanceof Player player) {
            data.tickCarriedBy(player, itemStack, null);
         }

         if (!data.locked && slot != null && slot.getType() == EquipmentSlot.Type.HAND) {
            this.update(level, owner, data);
         }
      }
   }

   @Override
   public void onCraftedPostProcess(final ItemStack itemStack, final Level level) {
      MapPostProcessing postProcessing = itemStack.remove(DataComponents.MAP_POST_PROCESSING);
      if (postProcessing != null) {
         if (level instanceof ServerLevel serverLevel) {
            switch (postProcessing) {
               case LOCK:
                  lockMap(itemStack, serverLevel);
                  break;
               case SCALE:
                  scaleMap(itemStack, serverLevel);
            }
         }
      }
   }

   private static void scaleMap(final ItemStack itemStack, final ServerLevel level) {
      MapItemSavedData original = getSavedData(itemStack, level);
      if (original != null) {
         MapId id = level.getFreeMapId();
         level.setMapData(id, original.scaled());
         itemStack.set(DataComponents.MAP_ID, id);
      }
   }

   private static void lockMap(final ItemStack map, final ServerLevel level) {
      MapItemSavedData mapData = getSavedData(map, level);
      if (mapData != null) {
         MapId id = level.getFreeMapId();
         MapItemSavedData newData = mapData.locked();
         level.setMapData(id, newData);
         map.set(DataComponents.MAP_ID, id);
      }
   }

   @Override
   public InteractionResult useOn(final UseOnContext context) {
      BlockState clicked = context.getLevel().getBlockState(context.getClickedPos());
      if (clicked.is(BlockTags.BANNERS)) {
         if (!context.getLevel().isClientSide()) {
            MapItemSavedData data = getSavedData(context.getItemInHand(), context.getLevel());
            if (data != null && !data.toggleBanner(context.getLevel(), context.getClickedPos())) {
               return InteractionResult.FAIL;
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(context);
      }
   }
}
