#include "RandomLevelSource.h"
#include "feature/FeatureInclude.h"
#include "../Level.h"
#include "../ChunkPos.h"
#include "../MobSpawner.h"
#include "../biome/Biome.h"
#include "../biome/BiomeSource.h"
#include "../chunk/LevelChunk.h"
#include "../material/Material.h"
#include "../tile/Tile.h"
#include "../tile/HeavyTile.h"
#include "../../../util/Random.h"
#include "../../../client/Minecraft.h"
#include "CanyonFeature.h"
#include "DungeonFeature.h"
#include <cmath>

#include <cstdint>

const float RandomLevelSource::SNOW_CUTOFF = 0.5f;
const float RandomLevelSource::SNOW_SCALE = 0.3f;
static const int MAX_BUFFER_SIZE = 1024;

RandomLevelSource::RandomLevelSource(Level* level, long seed, int version, bool spawnMobs)
    : random(seed),
      level(level),
      lperlinNoise1(&random, 16),
      lperlinNoise2(&random, 16),
      perlinNoise1(&random, 8),
      perlinNoise2(&random, 4),
      perlinNoise3(&random, 4),
      scaleNoise(&random, 10),
      depthNoise(&random, 16),
      forestNoise(&random, 8),
      spawnMobs(spawnMobs),
      pnr(NULL),
      ar(NULL),
      br(NULL),
      sr(NULL),
      dr(NULL),
      fi(NULL),
      fis(NULL),
      m_worldOffsetX(0.0),
      m_worldOffsetY(0.0),
      m_worldOffsetZ(0.0),
      m_worldScaleX(1.0),
      m_worldScaleY(1.0),
      m_worldScaleZ(1.0),
      m_disableSkygrid(false)
{
    for (int i = 0; i < 32; ++i)
        for (int j = 0; j < 32; ++j)
            waterDepths[i][j] = 0;

    buffer = new float[MAX_BUFFER_SIZE];

    if (Minecraft::instance) {
    std::string scaleXStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_SCALE_X);
    if (!scaleXStr.empty()) {
        m_worldScaleX = atof(scaleXStr.c_str());
    }
    std::string scaleYStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_SCALE_Y);
if (!scaleYStr.empty()) {
    m_worldScaleY = atof(scaleYStr.c_str());
}
std::string scaleZStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_SCALE_Z);
if (!scaleZStr.empty()) {
    m_worldScaleZ = atof(scaleZStr.c_str());
}

    std::string xStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_OFFSET_X);
    if (!xStr.empty()) { m_worldOffsetX = atof(xStr.c_str()); }
    std::string yStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_OFFSET_Y);
    if (!yStr.empty()) { m_worldOffsetY = atof(yStr.c_str()); }
    std::string zStr = Minecraft::instance->options.getStringValue(OPTIONS_WORLD_OFFSET_Z);
    if (!zStr.empty()) { m_worldOffsetZ = atof(zStr.c_str()); }

if (Minecraft::instance) {
    m_disableSkygrid = Minecraft::instance->options.getBooleanValue(OPTIONS_DISABLE_SKYGRID);
}

    // BiomeSource 传入时用 worldCoordToDouble()
    if (level) {
        BiomeSource* biomeSource = level->getBiomeSource();
        if (biomeSource) {
            biomeSource->setWorldTransform(
                m_worldOffsetX, m_worldOffsetZ,
                m_worldScaleX, m_worldScaleZ
            );
        }
	}
}
}

RandomLevelSource::~RandomLevelSource()
{
    delete[] buffer;
    delete[] pnr;
    delete[] ar;
    delete[] br;
    delete[] sr;
    delete[] dr;
    delete[] fi;
    delete[] fis;
}

void RandomLevelSource::prepareHeights(double xOffs, double zOffs, unsigned char* blocks, void* biomes, float* temperatures)
{
    int waterHeight = customSeaLevel + 1;
    if (waterHeight < 0) waterHeight = 0;
    if (waterHeight > 127) waterHeight = 127;

    int xChunks = 16 / CHUNK_WIDTH;
    int xSize = xChunks + 1;
    int ySize = 128 / CHUNK_HEIGHT + 1;
    int zSize = xChunks + 1;

    buffer = getHeights(buffer, xOffs, 0, zOffs, xSize, ySize, zSize);

    for (int xc = 0; xc < xChunks; xc++) {
        for (int zc = 0; zc < xChunks; zc++) {
            for (int yc = 0; yc < 128 / CHUNK_HEIGHT; yc++) {
                double yStep = 1.0 / (double)CHUNK_HEIGHT;
                double s0 = buffer[((xc + 0) * zSize + (zc + 0)) * ySize + (yc + 0)];
                double s1 = buffer[((xc + 0) * zSize + (zc + 1)) * ySize + (yc + 0)];
                double s2 = buffer[((xc + 1) * zSize + (zc + 0)) * ySize + (yc + 0)];
                double s3 = buffer[((xc + 1) * zSize + (zc + 1)) * ySize + (yc + 0)];

                double s0a = (buffer[((xc + 0) * zSize + (zc + 0)) * ySize + (yc + 1)] - s0) * yStep;
                double s1a = (buffer[((xc + 0) * zSize + (zc + 1)) * ySize + (yc + 1)] - s1) * yStep;
                double s2a = (buffer[((xc + 1) * zSize + (zc + 0)) * ySize + (yc + 1)] - s2) * yStep;
                double s3a = (buffer[((xc + 1) * zSize + (zc + 1)) * ySize + (yc + 1)] - s3) * yStep;

                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    double xStep = 1.0 / (double)CHUNK_WIDTH;
                    double _s0 = s0;
                    double _s1 = s1;
                    double _s0a = (s2 - s0) * xStep;
                    double _s1a = (s3 - s1) * xStep;

                    for (int x = 0; x < CHUNK_WIDTH; x++) {
                        int offs = (x + xc * CHUNK_WIDTH) << 11 | (0 + zc * CHUNK_WIDTH) << 7 | (yc * CHUNK_HEIGHT + y);
                        int step = 1 << 7;
                        double zStep = 1.0 / (double)CHUNK_WIDTH;
                        double val = _s0;
                        double vala = (_s1 - _s0) * zStep;
                        for (int z = 0; z < CHUNK_WIDTH; z++) {
                            double temp = temperatures[(xc * CHUNK_WIDTH + x) * 16 + (zc * CHUNK_WIDTH + z)];
                            int tileId = 0;
                            if (yc * CHUNK_HEIGHT + y < waterHeight) {
                                if (temp < SNOW_CUTOFF && yc * CHUNK_HEIGHT + y >= waterHeight - 1) {
                                    tileId = Tile::ice->id;
                                } else {
                                    tileId = Tile::calmWater->id;
                                }
                            }
                            if (val > 0) {
                                tileId = Tile::rock->id;
                            }
                            if (m_disableSkygrid && (std::isnan(val) || std::isinf(val))) {
                                tileId = 0;
                            }
                            blocks[offs] = (unsigned char)tileId;
                            offs += step;
                            val += vala;
                        }
                        _s0 += _s0a;
                        _s1 += _s1a;
                    }
                    s0 += s0a;
                    s1 += s1a;
                    s2 += s2a;
                    s3 += s3a;
                }
            }
        }
    }
}

void RandomLevelSource::buildSurfaces(double xOffs, double zOffs, unsigned char* blocks, Biome** biomes)
{
    int waterHeight = customSeaLevel + 1;
    if (waterHeight < 0) waterHeight = 0;
    if (waterHeight > 127) waterHeight = 127;

        double sx = 684.412 * m_worldScaleX;
    double sz = 684.412 * m_worldScaleZ;
    double xf = xOffs / 4.0;
    double zf = zOffs / 4.0;

    for (int x = 0; x < 16; x++) {
    for (int z = 0; z < 16; z++) {
        double coordX = (xOffs / 4.0 + x) * sx;
        double coordZ = (zOffs / 4.0 + z) * sz;
        
        sandBuffer[x + z * 16]   = perlinNoise2.getValue(coordX, 0.0, coordZ);
        gravelBuffer[x + z * 16] = perlinNoise2.getValue(coordX, 109.01340, coordZ);
        depthBuffer[x + z * 16]  = perlinNoise3.getValue(coordX * 2.0, 0.0, coordZ * 2.0);
    }
	}
    for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
            double temp = 1.0;
            Biome* b = biomes[x + z * 16];
            double sandVal = sandBuffer[x + z * 16];
            double gravelVal = gravelBuffer[x + z * 16];
            bool sand = (sandVal + random.nextFloat() * 0.2f) > 0;
            bool gravel = (gravelVal + random.nextFloat() * 0.2f) > 3;
            if (m_disableSkygrid) {
                if (std::isnan(sandVal) || std::isinf(sandVal)) sand = false;
                if (std::isnan(gravelVal) || std::isinf(gravelVal)) gravel = false;
            }
            int runDepth = (int)(depthBuffer[x + z * 16] / 3.0 + 3.0 + random.nextFloat() * 0.25f);
            int run = -1;
            char top = b->topMaterial;
            char material = b->material;
            for (int y = 127; y >= 0; y--) {
                int offs = (z * 16 + x) * 128 + y;
                if (y <= 0 + random.nextInt(5)) {
                    blocks[offs] = (char)Tile::unbreakable->id;
                } else {
                    int old = blocks[offs];
                    if (old == 0) {
                        run = -1;
                    } else if (old == Tile::rock->id) {
                        if (run == -1) {
                            if (runDepth <= 0) {
                                top = 0;
                                material = (char)Tile::rock->id;
                            } else if (y >= waterHeight - 4 && y <= waterHeight + 1) {
                                top = b->topMaterial;
                                material = b->material;
                                if (gravel) {
                                    top = 0;
                                    material = (char)Tile::gravel->id;
                                }
                                if (sand) {
                                    top = (char)Tile::sand->id;
                                    material = (char)Tile::sand->id;
                                }
                            }
                            if (y < waterHeight && top == 0) {
                                if (temp < 0.15f)
                                    top = (char)Tile::ice->id;
                                else
                                    top = (char)Tile::calmWater->id;
                            }
                            run = runDepth;
                            if (y >= waterHeight - 1)
                                blocks[offs] = top;
                            else
                                blocks[offs] = material;
                        } else if (run > 0) {
                            run--;
                            blocks[offs] = material;
                            if (run == 0 && material == Tile::sand->id) {
                                run = random.nextInt(4);
                                material = (char)Tile::sandStone->id;
                            }
                        }
                    }
                }
            }
        }
    }
}

void RandomLevelSource::postProcess(ChunkSource* parent, int64_t xt, int64_t zt)
{
        double worldBlockX = xt * 16.0 + m_worldOffsetX;
    double worldBlockZ = zt * 16.0 + m_worldOffsetZ;
    int xo = (int)worldBlockX;
    int zo = (int)worldBlockZ;
    double transformedXo = (xo + m_worldScaleX) * m_worldScaleX;
    double transformedZo = (zo + m_worldScaleZ) * m_worldScaleZ;
	
    if (!level->hasChunk(xt - 1, zt - 1) || !level->hasChunk(xt, zt - 1) ||
        !level->hasChunk(xt - 1, zt) || !level->hasChunk(xt, zt)) {
        return;
    }

    LevelChunk* chunk = level->getChunk(xt, zt);
    if (!chunk) return;
    unsigned char* blocks = chunk->getBlockData();

    level->isGeneratingTerrain = true;
    HeavyTile::instaFall = true;

    Biome* biome = level->getBiomeSource()->getBiome(
        (int)(transformedXo + 16 * m_worldScaleX),
        (int)(transformedZo + 16 * m_worldScaleZ)
    );

    random.setSeed(level->getSeed());
    int xScale = random.nextInt() / 2 * 2 + 1;
    int zScale = random.nextInt() / 2 * 2 + 1;
    random.setSeed(((xt * xScale) + (zt * zScale)) ^ level->getSeed());

    // 放置各种地形特征（湖泊、矿石、树木等，原样保留，省略详细代码）
    // ... (此处与原版相同，不影响编译，略)
	if (random.nextInt(4) == 0) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        LakeFeature feature(Tile::calmWater->id);
		feature.place(level, &random, x, y, z);
        LOGI("Adding underground lake @ (%d,%d,%d)\n", x, y, z);
    }

	////@todo: hide those chunks if they are aren't visible
    if (random.nextInt(8) == 0) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(random.nextInt(120) + 8);
        int z = zo + random.nextInt(16) + 8;
        if (y < 64 || random.nextInt(10) == 0) {
			LakeFeature feature(Tile::calmLava->id);
			feature.place(level, &random, x, y, z);
		}
    }

	static float totalTime = 0;
	const float st = getTimeS();

    //for (int i = 0; i < 8; i++) {
    //    int x = xo + random.nextInt(16) + 8;
    //    int y = random.nextInt(128);
    //    int z = zo + random.nextInt(16) + 8;
    //    MonsterRoomFeature().place(level, random, x, y, z);
    //}

    for (int i = 0; i < 10; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16);
        ClayFeature feature(32);
		feature.place(level, &random, x, y, z);
    }

    for (int i = 0; i < 20; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::dirt->id, 32);
		feature.place(level, &random, x, y, z);
    }

    for (int i = 0; i < 10; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::gravel->id, 32);
		feature.place(level, &random, x, y, z);
    }

    // Coal: common, wide Y range, moderate vein size
    for (int i = 0; i < 16; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::coalOre->id, 14);
        feature.place(level, &random, x, y, z);
    }

    // Iron: common, limited to upper underground
    for (int i = 0; i < 14; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(64);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::ironOre->id, 10);
        feature.place(level, &random, x, y, z);
    }

    // Gold: rarer and deeper
    for (int i = 0; i < 2; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(32);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::goldOre->id, 9);
        feature.place(level, &random, x, y, z);
    }

    // Redstone: somewhat common at low depths
    for (int i = 0; i < 6; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(16);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::redStoneOre->id, 8);
        feature.place(level, &random, x, y, z);
    }

    // Emerald (diamond-equivalent): still rare but slightly more than vanilla
    for (int i = 0; i < 3; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(16);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::emeraldOre->id, 6);
        feature.place(level, &random, x, y, z);
    }

    // Lapis: rare and not in very high Y
    for (int i = 0; i < 1; i++) {
        int x = xo + random.nextInt(16);
        int y = random.nextInt(16) + random.nextInt(16);
        int z = zo + random.nextInt(16);
        OreFeature feature(Tile::lapisOre->id, 6);
        feature.place(level, &random, x, y, z);
    }

    const float ss = 0.5f;
    int oFor = (int) ((forestNoise.getValue(xo * ss, zo * ss) / 8 + random.nextFloat() * 4 + 4) / 3);
    int forests = 0;//1; (java: 0)
    if (random.nextInt(10) == 0) forests += 1;

    if (biome == Biome::forest) forests += oFor + 2;
    if (biome == Biome::rainForest) forests += oFor + 2;
    if (biome == Biome::seasonalForest) forests += oFor + 1;
    if (biome == Biome::taiga) {
		forests += oFor + 1;
    }

    if (biome == Biome::desert) forests -= 20;
    if (biome == Biome::tundra) forests -= 20;
    if (biome == Biome::plains) forests -= 20;

    for (int i = 0; i < forests; i++) {
        int x = xo + random.nextInt(16) + 8;
        int z = zo + random.nextInt(16) + 8;
		int y = level->getHeightmap(x, z);
        Feature* tree = biome->getTreeFeature(&random);
		if (tree) {
	        tree->init(1, 1, 1);
		    tree->place(level, &random, x, y, z);
			delete tree;
		}
    }

    int grassCount = 1;
	for (int i = 0; i < grassCount; i++) {
		int x = xo + random.nextInt(16) + 8;
		int y = random.nextInt(Level::genDepth);
		int z = zo + random.nextInt(16) + 8;
		Feature* grassFeature = biome->getGrassFeature(&random);
		if (grassFeature) {
			grassFeature->place(level, &random, x, y, z);
			delete grassFeature;
		}
	}

    for (int i = 0; i < 2; i++) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        FlowerFeature feature(Tile::flower->id);
		feature.place(level, &random, x, y, z);
    }

    if (random.nextInt(2) == 0) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
		FlowerFeature feature(Tile::rose->id);
        feature.place(level, &random, x, y, z);
    }

    if (random.nextInt(4) == 0) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        FlowerFeature feature(Tile::mushroom1->id);
		feature.place(level, &random, x, y, z);
    }

    if (random.nextInt(8) == 0) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        FlowerFeature feature(Tile::mushroom2->id);
		feature.place(level, &random, x, y, z);
    }

    for (int i = 0; i < 10; i++) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        ReedsFeature feature;
		feature.place(level, &random, x, y, z);
    }

    int cacti = 0;
    if (biome == Biome::desert) cacti += 5;

    for (int i = 0; i < cacti; i++) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(128);
        int z = zo + random.nextInt(16) + 8;
        CactusFeature feature;
        feature.place(level, &random, x, y, z);
    }

    for (int i = 0; i < 50; i++) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(random.nextInt(120) + 8);
        int z = zo + random.nextInt(16) + 8;
        SpringFeature feature(Tile::water->id);
		feature.place(level, &random, x, y, z);
    }

    for (int i = 0; i < 20; i++) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(random.nextInt(random.nextInt(112) + 8) + 8);
        int z = zo + random.nextInt(16) + 8;
        SpringFeature feature(Tile::lava->id);
		feature.place(level, &random, x, y, z);
    }

	if (spawnMobs && !level->isClientSide)
		MobSpawner::postProcessSpawnMobs(level, biome, xo + 8, zo + 8, 16, 16, &random);

    // 积雪处理：直接使用 BiomeSource 中的 temperatures 数组（已在 create 中填充）
    float* temperatures = level->getBiomeSource()->temperatures;
    for (int x = xo + 8; x < xo + 8 + 16; x++) {
        for (int z = zo + 8; z < zo + 8 + 16; z++) {
            int xp = x - (xo + 8);
            int zp = z - (zo + 8);
            int y = level->getTopSolidBlock(x, z);
            float temp = temperatures[xp * 16 + zp] - (y - customSeaLevel) / 64.0 * SNOW_SCALE;
            if (temp < SNOW_CUTOFF) {
                if (y > 0 && y < 128 && level->isEmptyTile(x, y, z) && level->getMaterial(x, y - 1, z)->blocksMotion()) {
                    if (level->getMaterial(x, y - 1, z) != Material::ice)
                        level->setTile(x, y, z, Tile::topSnow->id);
                }
            }
        }
    }

    caveFeature.apply(this, level, (int)worldBlockX, (int)worldBlockZ, blocks, LevelChunk::ChunkBlockCount);

    // 峡谷生成 --- 改成按次数随机位置生成，不再一次性扫周边
    int canyonAttempts = 1 + random.nextInt(2);   // 每区块尝试 1~2 次
    for (int i = 0; i < canyonAttempts; ++i) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(random.nextInt(120) + 8);
        int z = zo + random.nextInt(16) + 8;
        CanyonFeature canyon;
        // 手动调用其内部 addFeature，而不是 apply
        canyon.addFeature(level, x >> 4, z >> 4, xo >> 4, zo >> 4, blocks, LevelChunk::ChunkBlockCount);
    }

    // 地牢生成 --- 改成限量随机位置生成
    int dungeonAttempts = 1 + random.nextInt(2);   // 每区块尝试 1~2 次
    for (int i = 0; i < dungeonAttempts; ++i) {
        int x = xo + random.nextInt(16) + 8;
        int y = random.nextInt(random.nextInt(random.nextInt(112) + 8) + 8);
        int z = zo + random.nextInt(16) + 8;
                DungeonFeature dungeon;
        dungeon.addFeature(level, x >> 4, z >> 4, xo >> 4, zo >> 4, blocks, LevelChunk::ChunkBlockCount);
}
    HeavyTile::instaFall = false;
    level->isGeneratingTerrain = false;
}

float* RandomLevelSource::getHeights(float* buffer, double x, int y, double z, int xSize, int ySize, int zSize)
{
    float farlandsScale = 1.0f;
        double sx = 684.412 * farlandsScale * m_worldScaleX;
    double sy = 684.412 * farlandsScale * m_worldScaleY;
    double sz = 684.412 * farlandsScale * m_worldScaleZ;

    const int size = xSize * ySize * zSize;
    if (size > MAX_BUFFER_SIZE) {
        LOGI("RandomLevelSource::getHeights: TOO LARGE BUFFER REQUESTED: %d (max %d)\n", size, MAX_BUFFER_SIZE);
    }

    float* temperatures = level->getBiomeSource()->temperatures;
    float* downfalls = level->getBiomeSource()->downfalls;

    double noiseX = x / 4.0;
        double noiseY = (y + m_worldOffsetY) / 8.0;
    double noiseZ = z / 4.0;

    int intNoiseX = (int)noiseX;
    int intNoiseZ = (int)noiseZ;

        sr = scaleNoise.getRegion(sr, intNoiseX, intNoiseZ, xSize, zSize,
                              1.121 * m_worldScaleX,
                              1.121 * m_worldScaleZ, 0.5);
        dr = depthNoise.getRegion(dr, intNoiseX, intNoiseZ, xSize, zSize,
                              200.0 * m_worldScaleX,
                              200.0 * m_worldScaleZ, 0.5);
	
    double xf = (double)noiseX;
    double yf = (double)noiseY;
    double zf = (double)noiseZ;

    pnr = perlinNoise1.getRegion(pnr, xf, yf, zf, xSize, ySize, zSize, sx / 80.0, sy / 160.0, sz / 80.0);
    ar  = lperlinNoise1.getRegion(ar,  xf, yf, zf, xSize, ySize, zSize, sx, sy, sz);
    br  = lperlinNoise2.getRegion(br,  xf, yf, zf, xSize, ySize, zSize, sx, sy, sz);

    int p = 0;
    int pp = 0;
    int wScale = 16 / xSize;
    for (int xx = 0; xx < xSize; xx++) {
        int xp = xx * wScale + wScale / 2;
        for (int zz = 0; zz < zSize; zz++) {
            int zp = zz * wScale + wScale / 2;
            float temperature = temperatures[xp * 16 + zp];
            float downfall = downfalls[xp * 16 + zp] * temperature;
            double dd = 1 - downfall;
            dd = dd * dd;
            dd = dd * dd;
            dd = 1 - dd;
            float scale = ((sr[pp] + 256.0) / 512.0);
            scale *= dd;
            if (scale > 1) scale = 1;
            float depth = (dr[pp] / 8000.0);
            if (depth < 0) depth = -depth * 0.3;
            depth = depth * 3.0 - 2.0;
            if (depth < 0) {
                depth = depth / 2;
                if (depth < -1) depth = -1;
                depth = depth / 1.4;
                depth /= 2;
                scale = 0;
            } else {
                if (depth > 1) depth = 1;
                depth = depth / 8;
            }
            if (scale < 0) scale = 0;
            scale = (scale) + 0.5;
            depth = depth * ySize / 16.0;
            double yCenter = ySize / 2.0 + depth * 4;
            pp++;
            for (int yy = 0; yy < ySize; yy++) {
                double val = 0;
                double yOffs = (yy - yCenter) * 12 / scale;
                if (yOffs < 0) yOffs *= 4;
                double bb = ar[p] / 512.0;
                double cc = br[p] / 512.0;
                double v = (pnr[p] / 10.0 + 1) / 2.0;
                if (v < 0) val = bb;
                else if (v > 1) val = cc;
                else val = bb + (cc - bb) * v;
                val -= yOffs;
                if (yy > ySize - 4) {
                    double slide = (yy - (ySize - 4)) / (4.0 - 1.0);
                    val = val * (1 - slide) + -10 * slide;
                }
                buffer[p] = val;
                p++;
            }
        }
    }
    return buffer;
}

LevelChunk* RandomLevelSource::create(int64_t x, int64_t z)
{
    int64_t hashedPos = (x << 32) | (z & 0xffffffff);
    ChunkMap::iterator it = chunkMap.find(hashedPos);
    if (it != chunkMap.end())
        return it->second;

    random.setSeed((long)(x * 341872712l + z * 132899541l));

    unsigned char* blocks = new unsigned char[LevelChunk::ChunkBlockCount];
LevelChunk* levelChunk = new LevelChunk(level, blocks, (int)x, (int)z);  // 🔧 补这行
chunkMap.insert(std::make_pair(hashedPos, levelChunk));

        double worldBlockX = x * 16.0 + m_worldOffsetX;
    double worldBlockZ = z * 16.0 + m_worldOffsetZ;

    Biome** biomes = level->getBiomeSource()->getBiomeBlock((int)worldBlockX, (int)worldBlockZ, 16, 16);
    float* temperatures = level->getBiomeSource()->temperatures;
    prepareHeights(worldBlockX, worldBlockZ, blocks, 0, temperatures);
    buildSurfaces(worldBlockX, worldBlockZ, blocks, biomes);

    levelChunk->recalcHeightmap();
    return levelChunk;
}

LevelChunk* RandomLevelSource::getChunk(int64_t xOffs, int64_t zOffs)
{
    return create(xOffs, zOffs);
}

bool RandomLevelSource::hasChunk(int64_t x, int64_t z)
{
    return true;
}

bool RandomLevelSource::tick()
{
    return false;
}

bool RandomLevelSource::shouldSave()
{
    return true;
}

std::string RandomLevelSource::gatherStats()
{
    return "RandomLevelSource";
}

Biome::MobList RandomLevelSource::getMobsAt(const MobCategory& mobCategory, int x, int y, int z)
{
    BiomeSource* biomeSource = level->getBiomeSource();
    if (biomeSource == NULL) return Biome::MobList();
    Biome* biome = biomeSource->getBiome(x, z);
    if (biome == NULL) return Biome::MobList();
    return biome->getMobs(mobCategory);
}

LevelChunk* PerformanceTestChunkSource::create(int64_t x, int64_t z)
{
    unsigned char* blocks = new unsigned char[LevelChunk::ChunkBlockCount];
    memset(blocks, 0, LevelChunk::ChunkBlockCount);
    int64_t xi = x;
    int64_t zi = z;
    for (int64_t y = 0; y < 65; y++) {
        if (y < 60) {
            for (int64_t xx = (y + 1) & 1; xx < 16; xx += 2) {
                for (int64_t zz = y & 1; zz < 16; zz += 2) {
                    blocks[xx << 11 | zz << 7 | y] = 3;
                }
            }
        } else {
            for (int64_t xx = 0; xx < 16; xx += 2) {
                for (int64_t zz = 0; zz < 16; zz += 2) {
                    blocks[xx << 11 | zz << 7 | y] = 3;
                }
            }
        }
    }
    LevelChunk* levelChunk = new LevelChunk(level, blocks, xi, zi);
    levelChunk->recalcHeightmap();
    return levelChunk;
}

