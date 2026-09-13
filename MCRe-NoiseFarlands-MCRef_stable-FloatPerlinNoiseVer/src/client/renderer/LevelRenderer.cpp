#include "LevelRenderer.h"

#include "DirtyChunkSorter.h"
#include "DistanceChunkSorter.h"
#include "Chunk.h"
#include "TileRenderer.h"
#include "../Minecraft.h"
#include "../../util/Mth.h"
#include "../../world/entity/player/Player.h"
#include "../../world/level/tile/LevelEvent.h"
#include "../../world/level/tile/LeafTile.h"
#include "../../client/particle/ParticleEngine.h"
#include "../../client/particle/ParticleInclude.h"
#include "../sound/SoundEngine.h"
#include "culling/Culler.h"
#include "entity/EntityRenderDispatcher.h"
#include "../model/HumanoidModel.h"

#include "GameRenderer.h"
#include "../../AppPlatform.h"
#include "../../util/PerfTimer.h"
#include "Textures.h"
#include "tileentity/TileEntityRenderDispatcher.h"//
#include "../particle/BreakingItemParticle.h"

#include "../../client/player/LocalPlayer.h"

// ---------- 新增：偏移/区块缓存/地形源 头文件 ----------
#include "../../world/level/chunk/ChunkCache.h"
#include "../../world/level/levelgen/RandomLevelSource.h"
#include "../../world/level/levelgen/TheEndLevelSource.h"
// ----------------------------------------------------
#include "../../util/DebugLog.h"  // 确保在文件顶部包含，如果还没有的话
#include <cmath>

#ifdef GFX_SMALLER_CHUNKS
/* static */ const int LevelRenderer::CHUNK_SIZE = 8;
#else
/* static */ const int LevelRenderer::CHUNK_SIZE = 16;
#endif

// 静态实体数组（不依赖任何对象成员，存放于 .data 段）
static Entity* s_safeEntities[8192];
static int s_safeEntityCount = 0;

LevelRenderer::LevelRenderer( Minecraft* mc)
:	mc(mc),
	textures(mc->textures),
	level(NULL),
	cullStep(0),

	chunkLists(0),
	xChunks(0), yChunks(0), zChunks(0),

	chunks(NULL),
	sortedChunks(NULL),

	xMinChunk(0), yMinChunk(0), zMinChunk(0),
	xMaxChunk(0), yMaxChunk(0), zMaxChunk(0),

	lastViewDistance(-1),

	noEntityRenderFrames(2),
	totalEntities(0),
	renderedEntities(0),
	culledEntities(0),

	occlusionCheck(true),
	totalChunks(0), offscreenChunks(0), renderedChunks(0), occludedChunks(0), emptyChunks(0),

	chunkFixOffs(0),
	xOld(-9999), yOld(-9999), zOld(-9999),

	ticks(0),
	skyList(0), starList(0), darkList(0),
	tileRenderer(NULL),
lastCullCamX(-9999), lastCullCamY(-9999), lastCullCamZ(-9999),
lastCullYRot(-9999), lastCullXRot(-9999),
cullCacheValid(false), cullSkipTimer(0),
	destroyProgress(0),
    m_starsGenerated(false)
{
#ifdef OPENGL_ES
	int maxChunksWidth = 2 * LEVEL_WIDTH / CHUNK_SIZE + 1;
	numListsOrBuffers = maxChunksWidth * maxChunksWidth * (128/CHUNK_SIZE) * 3;
	chunkBuffers = new GLuint[numListsOrBuffers];
	glGenBuffers2(numListsOrBuffers, chunkBuffers);
	LOGI("numBuffers: %d\n", numListsOrBuffers);
	//for (int i = 0; i < numListsOrBuffers; ++i) printf("bufId %d: %d\t", i, chunkBuffers[i]);

	// ... 原有初始化 ...
    // 在构造函数末尾，原来 generateSky() 调用的地方
generateSky();
//generateStars();
#else
	int maxChunksWidth = 1024 / CHUNK_SIZE;
	numListsOrBuffers = maxChunksWidth * maxChunksWidth * maxChunksWidth * 3;
	chunkLists = glGenLists(numListsOrBuffers);
#endif
	m_sunTexture   = textures->loadTexture("environment/sun.png");
	m_moonTexture = textures->loadTexture("environment/moon.png");
m_starsTexture = textures->loadTexture("environment/stars.png");
}

LevelRenderer::~LevelRenderer()
{
	delete tileRenderer;
	tileRenderer = NULL;

	deleteChunks();

#ifdef OPENGL_ES
	glDeleteBuffers(numListsOrBuffers, chunkBuffers);
	if (m_skyChunk.vboId != (GLuint)-1) glDeleteBuffers(1, &m_skyChunk.vboId);
if (m_skyChunk2.vboId != (GLuint)-1) glDeleteBuffers(1, &m_skyChunk2.vboId);
if (m_starsChunk.vboId != (GLuint)-1) glDeleteBuffers(1, &m_starsChunk.vboId);
	delete[] chunkBuffers;
#else
	glDeleteLists(numListsOrBuffers, chunkLists);
#endif
}

// 🧊 惰性生成星星 —— 确保 GL 上下文已就绪
void LevelRenderer::ensureStarsGenerated(){
    if(m_starsGenerated) return;
    
    // 只在有 GL 上下文时生成（renderSky 中调用，此时上下文保证有效）
    generateStars();
}

void LevelRenderer::generateSky() {
    Tesselator& t = Tesselator::instance;

    // -------- 上半天空 (y = 16) --------
    t.begin();
    const int s = 64;
    const int d = (256 / s) + 2;
    for (int xx = -s * d; xx <= s * d; xx += s) {
        for (int zz = -s * d; zz <= s * d; zz += s) {
            t.vertexUV((float)xx,        16.0f, (float)zz,        0.0f, 0.0f);
            t.vertexUV((float)(xx + s),  16.0f, (float)zz,        0.0f, 0.0f);
            t.vertexUV((float)(xx + s),  16.0f, (float)(zz + s),  0.0f, 0.0f);
            t.vertexUV((float)xx,        16.0f, (float)(zz + s),  0.0f, 0.0f);
        }
    }
    t.draw();
    m_skyChunk = t.end(true, 0);   // 保存上半天空 VBO

    // -------- 下半天空 (y = -16) --------
    t.begin();
    for (int xx = -s * d; xx <= s * d; xx += s) {
        for (int zz = -s * d; zz <= s * d; zz += s) {
            t.vertexUV((float)(xx + s), -16.0f, (float)zz,        0.0f, 0.0f);
            t.vertexUV((float)xx,       -16.0f, (float)zz,        0.0f, 0.0f);
            t.vertexUV((float)xx,       -16.0f, (float)(zz + s),  0.0f, 0.0f);
            t.vertexUV((float)(xx + s), -16.0f, (float)(zz + s),  0.0f, 0.0f);
        }
    }
    t.draw();
    m_skyChunk2 = t.end(true, 0);  // 保存下半天空 VBO
}

void LevelRenderer::generateStars() {
    if (m_starsGenerated) return;

    Tesselator& t = Tesselator::instance;
    Random random(10842L);          // MCP 原版种子
    t.begin();

    for (int i = 0; i < 1500; ++i) {
        double x = random.nextFloat() * 2.0 - 1.0;
        double y = random.nextFloat() * 2.0 - 1.0;
        double z = random.nextFloat() * 2.0 - 1.0;
        double d = x * x + y * y + z * z;

        if (d < 1.0 && d > 0.01) {
            double scale = 1.0 / Mth::sqrt(d);
            double dx = x * scale;
            double dy = y * scale;
            double dz = z * scale;

            double size = 0.25 + random.nextFloat() * 0.25;

            double atan2 = Mth::atan2(dx, dz);
            double sinA = Mth::sin(atan2);
            double cosA = Mth::cos(atan2);

            double atan22 = Mth::atan2(Mth::sqrt(dx * dx + dz * dz), dy);
            double sinB = Mth::sin(atan22);
            double cosB = Mth::cos(atan22);

            double spin = random.nextDouble() * Mth::PI * 2.0;
            double sinS = Mth::sin(spin);
            double cosS = Mth::cos(spin);

            for (int j = 0; j < 4; ++j) {
                double xOff = ((j & 2) - 1) * size;
                double yOff = (((j + 1) & 2) - 1) * size;

                double xRot = xOff * cosS - yOff * sinS;
                double yRot = yOff * cosS + xOff * sinS;

                float fx = (float)(100.0 * dx + (xRot * cosA - yRot * sinA * sinB));
                float fy = (float)(100.0 * dy + yRot * cosB);
                float fz = (float)(100.0 * dz + (xRot * sinA + yRot * cosA * sinB));

                t.vertex(fx, fy, fz);
            }
        }
    }

    t.draw();
    m_starsChunk = t.end(true, 0);
    m_starsGenerated = true;
}

void LevelRenderer::renderSun(float a) {
    textures->loadAndBindTexture("environment/sun.png");
    Tesselator& t = Tesselator::instance;
    float size = 30.0f; // 原版
    t.begin();
    t.vertexUV(-size, 100.0f, -size, 0.0f, 0.0f);
    t.vertexUV( size, 100.0f, -size, 1.0f, 0.0f);
    t.vertexUV( size, 100.0f,  size, 1.0f, 1.0f);
    t.vertexUV(-size, 100.0f,  size, 0.0f, 1.0f);
    t.draw();
}

void LevelRenderer::renderMoon(float a) {
    textures->loadAndBindTexture("environment/moon.png");
    Tesselator& t = Tesselator::instance;
    float size = 20.0f; // 原版
    t.begin();
    t.vertexUV(-size, -100.0f,  size, 1.0f, 1.0f);
    t.vertexUV( size, -100.0f,  size, 0.0f, 1.0f);
    t.vertexUV( size, -100.0f, -size, 0.0f, 0.0f);
    t.vertexUV(-size, -100.0f, -size, 1.0f, 0.0f);
    t.draw();
}

void LevelRenderer::setLevel( Level* level )
{
	if (this->level != NULL) {
		this->level->removeListener(this);
	}

	xOld = -9999;
	yOld = -9999;
	zOld = -9999;

	EntityRenderDispatcher::getInstance()->setLevel(level);
	EntityRenderDispatcher::getInstance()->setMinecraft(mc);
	this->level = level;

	delete tileRenderer;
	tileRenderer = new TileRenderer(level);

	if (level != NULL) {
		level->addListener(this);
		allChanged();
// 填充安全数组并打印日志
s_safeEntityCount = 0;
const EntityList& allEntities = level->getAllEntities();
int totalCount = (int)allEntities.size();
DLOG_C("setLevel: getAllEntities returned %d entities", totalCount);
int maxCount = totalCount;
if (maxCount > 8192) maxCount = 8192;
for (int i = 0; i < maxCount; ++i) {
    if (allEntities[i] && !allEntities[i]->removed) {
        s_safeEntities[s_safeEntityCount++] = allEntities[i];
        DLOG_C("   add to safe list: id=%d, pos=(%.2f,%.2f,%.2f)", 
               allEntities[i]->entityId, allEntities[i]->x, allEntities[i]->y, allEntities[i]->z);
    }
}
// 确保 cameraTargetPlayer 在列表中
if (mc->cameraTargetPlayer) {
    bool found = false;
    for (int i = 0; i < s_safeEntityCount; ++i)
        if (s_safeEntities[i] == mc->cameraTargetPlayer) { found = true; break; }
    if (!found && s_safeEntityCount < 8192) {
        s_safeEntities[s_safeEntityCount++] = mc->cameraTargetPlayer;
        DLOG_C("   manually add cameraTarget: id=%d", mc->cameraTargetPlayer->entityId);
    }
}
DLOG_C("setLevel: safe list count = %d", s_safeEntityCount);
	}
}

void LevelRenderer::allChanged()
{
	deleteChunks();

	bool fancy = mc->options.getBooleanValue(OPTIONS_FANCY_GRAPHICS);

	Tile::leaves->setFancy(fancy);
	Tile::leaves_carried->setFancy(fancy);
	lastViewDistance = mc->options.getIntValue(OPTIONS_VIEW_DISTANCE);

	int dist = (512 >> 3) << (3 - lastViewDistance);
	if (lastViewDistance <= 2 && mc->isPowerVR())
		dist = (int)((float)dist * 0.8f);
	LOGI("last: %d, power: %d\n", lastViewDistance, mc->isPowerVR());

	#if defined(RPI)
		dist *= 0.6f;
	#endif

	if (dist > 400) dist = 400;
	/*
	* if (Minecraft.FLYBY_MODE) { dist = 512 - CHUNK_SIZE * 2; }
	*/
	xChunks = (dist / LevelRenderer::CHUNK_SIZE) + 1;
	yChunks = (128 /  LevelRenderer::CHUNK_SIZE);
	zChunks = (dist / LevelRenderer::CHUNK_SIZE) + 1;
	chunksLength = xChunks * yChunks * zChunks;
	LOGI("chunksLength: %d. Distance: %d\n", chunksLength, dist);

	chunks = new Chunk*[chunksLength];
	sortedChunks = new Chunk*[chunksLength];

	int id = 0;
	int count = 0;

	xMinChunk = 0;
	yMinChunk = 0;
	zMinChunk = 0;
	xMaxChunk = xChunks;
	yMaxChunk = yChunks;
	zMaxChunk = zChunks;
	dirtyChunks.clear();
	//renderableTileEntities.clear();

	for (int x = 0; x < xChunks; x++) {
		for (int y = 0; y < yChunks; y++) {
			for (int z = 0; z < zChunks; z++) {
				const int c = getLinearCoord(x, y, z);
				Chunk* chunk = new Chunk(level, x * CHUNK_SIZE, y * CHUNK_SIZE, z * CHUNK_SIZE, CHUNK_SIZE, chunkLists + id, &chunkBuffers[id]);

				if (occlusionCheck) {
					chunk->occlusion_id = 0;//occlusionCheckIds.get(count);
				}
				chunk->occlusion_querying = false;
				chunk->occlusion_visible = true;
				chunk->visible = true;
				chunk->id = count++;
				chunk->setDirty();

				chunks[c] = chunk;
				sortedChunks[c] = chunk;
				dirtyChunks.push_back(chunk);

				id += 3;
			}
		}
	}

	if (level != NULL) {
		Entity* player = mc->cameraTargetPlayer;
		if (player != NULL) {
			this->resortChunks(Mth::floor(player->x), Mth::floor(player->y), Mth::floor(player->z));
			DistanceChunkSorter distanceSorter(player);
			std::sort(sortedChunks, sortedChunks + chunksLength, distanceSorter);
		}
	}
	noEntityRenderFrames = 2;
}

void LevelRenderer::deleteChunks()
{
	for (int z = 0; z < zChunks; ++z)
	for (int y = 0; y < yChunks; ++y)
	for (int x = 0; x < xChunks; ++x) {
		int c = getLinearCoord(x, y, z);
		delete chunks[c];
	}

	delete[] chunks;
	chunks = NULL;

	delete[] sortedChunks;
	sortedChunks = NULL;
}

void LevelRenderer::resortChunks(int64_t xc, int64_t yc, int64_t zc) {
    xc -= CHUNK_SIZE / 2;
    zc -= CHUNK_SIZE / 2;
    xMinChunk = INT64_MAX; yMinChunk = INT64_MAX;
    zMinChunk = INT64_MAX;
    xMaxChunk = INT64_MIN; yMaxChunk = INT64_MIN;
    zMaxChunk = INT64_MIN;

    int dirty = 0;

    int64_t s2 = xChunks * CHUNK_SIZE;
    int64_t s1 = s2 / 2;

    for (int x = 0; x < xChunks; x++) {
        int64_t xx = x * CHUNK_SIZE;
        // 🔥 X 轴：用 safe_mod 替代原来的取模
        int64_t xOff = Mth::safe_mod(xx + s1 - xc, s2);  // 结果 [0, s2-1]
        xx = xc - s1 + xOff;

        if (xx < xMinChunk) xMinChunk = xx;
        if (xx > xMaxChunk) xMaxChunk = xx;

        for (int z = 0; z < zChunks; z++) {
            int64_t zz = z * CHUNK_SIZE;
            // 🔥 Z 轴：统一成与 X 轴一致的 safe_mod 逻辑
            // 原代码用除法 + 条件调整，容易在边界溢出
            int64_t zOff = Mth::safe_mod(zz + s1 - zc, s2);
            zz = zc - s1 + zOff;

            if (zz < zMinChunk) zMinChunk = zz;
            if (zz > zMaxChunk) zMaxChunk = zz;

            for (int y = 0; y < yChunks; y++) {
                int64_t yy = y * CHUNK_SIZE;
                if (yy < yMinChunk) yMinChunk = yy;
                if (yy > yMaxChunk) yMaxChunk = yy;

                Chunk* chunk = chunks[(z * yChunks + y) * xChunks + x];
                bool wasDirty = chunk->isDirty();
                chunk->setPos(xx, yy, zz);
                if (!wasDirty && chunk->isDirty()) {
                    dirtyChunks.push_back(chunk);
                    ++dirty;
                }
            }
        }
    }
}

int LevelRenderer::render(Mob* player, int layer, float alpha)
{
    if (mc->options.getIntValue(OPTIONS_VIEW_DISTANCE) != lastViewDistance) {
        allChanged();
    }

    
    for (int i = 0; i < 10; i++) {
        chunkFixOffs = (chunkFixOffs + 1) % chunksLength;
        Chunk* c = chunks[chunkFixOffs];
        if (c->isDirty() && std::find(dirtyChunks.begin(), dirtyChunks.end(), c) == dirtyChunks.end()) {
            dirtyChunks.push_back(c);
        }
    }

    if (layer == 0) {
        totalChunks = 0;
        offscreenChunks = 0;
        occludedChunks = 0;
        renderedChunks = 0;
        emptyChunks = 0;
    }

    double xOff = player->xOld + (player->x - player->xOld) * alpha;
double yOff = player->yOld + (player->y - player->yOld) * alpha;
double zOff = player->zOld + (player->z - player->zOld) * alpha;

    float xd = player->x - xOld;
    float yd = player->y - yOld;
    float zd = player->z - zOld;
    if (xd * xd + yd * yd + zd * zd > 4 * 4) {
        xOld = player->x;
        yOld = player->y;
        zOld = player->z;
resortChunks(Mth::floor64(player->x), Mth::floor64(player->y), Mth::floor64(player->z));
        DistanceChunkSorter distanceSorter(player);
        std::sort(sortedChunks, sortedChunks + chunksLength, distanceSorter);
    }

    int count = 0;
    if (occlusionCheck && !mc->options.getBooleanValue(OPTIONS_ANAGLYPH_3D) && layer == 0) {
    int from = 0;
    int to = 16;
    for (int i = from; i < to; i++) {
        sortedChunks[i]->occlusion_visible = true;
    }

    count += renderChunks(from, to, layer, alpha);

    do {
        from = to;
        to = to * 2;
        if (to > chunksLength) to = chunksLength;

        glDisable2(GL_TEXTURE_2D);
        glDisable2(GL_LIGHTING);
        glDisable2(GL_ALPHA_TEST);
        glDisable2(GL_FOG);

        glColorMask(false, false, false, false);
        glDepthMask(false);
        glPushMatrix2();
        // ★ 改为 double
        double xo = 0.0, yo = 0.0, zo = 0.0;
        for (int i = from; i < to; i++) {
            if (sortedChunks[i]->isEmpty()) {
                sortedChunks[i]->visible = false;
                continue;
            }
            if (!sortedChunks[i]->visible) {
                sortedChunks[i]->occlusion_visible = true;
            }

            if (sortedChunks[i]->visible && !sortedChunks[i]->occlusion_querying) {
                float dist = Mth::sqrt(sortedChunks[i]->distanceToSqr(player));
                int frequency = (int)(1 + dist / 128);
                if (ticks % frequency == i % frequency) {
                    Chunk* chunk = sortedChunks[i];
                    // ★ 使用 double 计算目标位置
                    double xt = (double)chunk->x - xOff;
                    double yt = (double)chunk->y - yOff;
                    double zt = (double)chunk->z - zOff;
                    // 相对偏移量（double 精度，差值很小）
                    double xdd = xt - xo;
                    double ydd = yt - yo;
                    double zdd = zt - zo;
                    if (xdd != 0.0 || ydd != 0.0 || zdd != 0.0) {
                        glTranslatef2((float)xdd, (float)ydd, (float)zdd);
                        xo += xdd;
                        yo += ydd;
                        zo += zdd;
                    }
                    sortedChunks[i]->renderBB();
                    sortedChunks[i]->occlusion_querying = true;
                }
            }
        }
        glPopMatrix2();
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glEnable2(GL_TEXTURE_2D);
        glEnable2(GL_ALPHA_TEST);
        glEnable2(GL_FOG);

        count += renderChunks(from, to, layer, alpha);

    } while (to < chunksLength);
	} else {
        TIMER_POP_PUSH("render");
        count += renderChunks(0, chunksLength, layer, alpha);
    }

    
    return count;
}

void LevelRenderer::renderDebug(const AABB& b, float a) const {
	float x0 = b.x0;
	float x1 = b.x1;
	float y0 = b.y0;
	float y1 = b.y1;
	float z0 = b.z0;
	float z1 = b.z1;
	float u0 = 0, v0 = 0;
	float u1 = 1, v1 = 1;

	glEnable2(GL_BLEND);
	glBlendFunc2(GL_DST_COLOR, GL_SRC_COLOR);
	glDisable2(GL_TEXTURE_2D);
	glColor4f2(1, 1, 1, 1);

	textures->loadAndBindTexture("terrain.png");

	Tesselator& t = Tesselator::instance;
	t.begin();
	t.color(255, 255, 255, 255);

	t.offset(((Mob*)mc->player)->getPos(a).negated());

	// up
	t.vertexUV(x0, y0, z1, u0, v1);
	t.vertexUV(x0, y0, z0, u0, v0);
	t.vertexUV(x1, y0, z0, u1, v0);
	t.vertexUV(x1, y0, z1, u1, v1);

	// down
	t.vertexUV(x1, y1, z1, u1, v1);
	t.vertexUV(x1, y1, z0, u1, v0);
	t.vertexUV(x0, y1, z0, u0, v0);
	t.vertexUV(x0, y1, z1, u0, v1);

	// north
	t.vertexUV(x0, y1, z0, u1, v0);
	t.vertexUV(x1, y1, z0, u0, v0);
	t.vertexUV(x1, y0, z0, u0, v1);
	t.vertexUV(x0, y0, z0, u1, v1);

	// south
	t.vertexUV(x0, y1, z1, u0, v0);
	t.vertexUV(x0, y0, z1, u0, v1);
	t.vertexUV(x1, y0, z1, u1, v1);
	t.vertexUV(x1, y1, z1, u1, v0);

	// west
	t.vertexUV(x0, y1, z1, u1, v0);
	t.vertexUV(x0, y1, z0, u0, v0);
	t.vertexUV(x0, y0, z0, u0, v1);
	t.vertexUV(x0, y0, z1, u1, v1);

	// east
	t.vertexUV(x1, y0, z1, u0, v1);
	t.vertexUV(x1, y0, z0, u1, v1);
	t.vertexUV(x1, y1, z0, u1, v0);
	t.vertexUV(x1, y1, z1, u0, v0);

	t.offset(0, 0, 0);
	t.draw();

	glEnable2(GL_TEXTURE_2D);
	glDisable2(GL_BLEND);
}

void LevelRenderer::render(const AABB& b) const
{
	Tesselator& t = Tesselator::instance;

	glColor4f2(1, 1, 1, 1);

	textures->loadAndBindTexture("terrain.png");

	//t.begin();
	t.color(255, 255, 255, 255);

	t.offset(((Mob*)mc->player)->getPos(0).negated());

	t.begin(GL_LINE_STRIP);
	t.vertex(b.x0, b.y0, b.z0);
	t.vertex(b.x1, b.y0, b.z0);
	t.vertex(b.x1, b.y0, b.z1);
	t.vertex(b.x0, b.y0, b.z1);
	t.vertex(b.x0, b.y0, b.z0);
	t.draw();

	t.begin(GL_LINE_STRIP);
	t.vertex(b.x0, b.y1, b.z0);
	t.vertex(b.x1, b.y1, b.z0);
	t.vertex(b.x1, b.y1, b.z1);
	t.vertex(b.x0, b.y1, b.z1);
	t.vertex(b.x0, b.y1, b.z0);
	t.draw();

	t.begin(GL_LINES);
	t.vertex(b.x0, b.y0, b.z0);
	t.vertex(b.x0, b.y1, b.z0);
	t.vertex(b.x1, b.y0, b.z0);
	t.vertex(b.x1, b.y1, b.z0);
	t.vertex(b.x1, b.y0, b.z1);
	t.vertex(b.x1, b.y1, b.z1);
	t.vertex(b.x0, b.y0, b.z1);
	t.vertex(b.x0, b.y1, b.z1);

	t.offset(0, 0, 0);
	t.draw();
}

//void LevelRenderer::checkQueryResults( int from, int to )
//{
//	for (int i = from; i < to; i++) {
//		if (sortedChunks[i]->occlusion_querying) {
//			// I wanna do a fast occusion culler here.
//		}
//	}
//}

int LevelRenderer::renderChunks(int from, int to, int layer, float alpha)
{
    _renderChunks.clear();
    int count = 0;
    Mob* player = mc->cameraTargetPlayer;

    for (int i = from; i < to; i++) {
        if (layer == 0) {
            totalChunks++;
            if (sortedChunks[i]->empty[layer]) emptyChunks++;
            else if (!sortedChunks[i]->visible) offscreenChunks++;
            else if (occlusionCheck && !sortedChunks[i]->occlusion_visible) occludedChunks++;
            else renderedChunks++;
        }

        if (!sortedChunks[i]->empty[layer] && sortedChunks[i]->visible && sortedChunks[i]->occlusion_visible) {
            if (layer != 0 && player) {
                float dSqr = sortedChunks[i]->distanceToSqr(player);
                if (dSqr > 160.0f * 160.0f) continue;
            }

            int list = sortedChunks[i]->getList(layer);
            if (list >= 0) {
                _renderChunks.push_back(sortedChunks[i]);
                count++;
            }
        }
    }

    // 相机双精度位置
    double xOff = player ? (player->xOld + (player->x - player->xOld) * alpha) : 0.0;
    double yOff = player ? (player->yOld + (player->y - player->yOld) * alpha) : 0.0;
    double zOff = player ? (player->zOld + (player->z - player->zOld) * alpha) : 0.0;

    bool stripeFix = mc->options.getBooleanValue(OPTIONS_STRIPE_REPAIR);

    if (stripeFix) {
    const int64_t ORIGIN_STEP = 1LL << 24; // 16,777,216
    // 计算整数原点（使用 int64_t，避免 double 转换）
    int64_t originX = ((int64_t)xOff / ORIGIN_STEP) * ORIGIN_STEP;
    int64_t originZ = ((int64_t)zOff / ORIGIN_STEP) * ORIGIN_STEP;
    // 相机相对偏移仍然用 double（因为 xOff 本身是 double）
    double camRelX = xOff - (double)originX;
    double camRelZ = zOff - (double)originZ;

    for (unsigned int i = 0; i < _renderChunks.size(); ++i) {
        Chunk* chunk = _renderChunks[i];
        // 关键：整数减法，差值范围小
        int64_t deltaX = chunk->x - originX;
        int64_t deltaZ = chunk->z - originZ;
        // 再将差值转为 double，减去相机相对偏移
        double targetX = (double)deltaX - camRelX;
        double targetY = -yOff;
        double targetZ = (double)deltaZ - camRelZ;

        glPushMatrix2();
        glTranslatef2((float)targetX, (float)targetY, (float)targetZ);
        // ... 渲染
		#ifdef USE_VBO
            RenderChunk& rc = chunk->getRenderChunk(layer);
            if (rc.vertexCount > 0) {
                renderChunkVBO(rc);
            }
#else
            int listId = chunk->getList(layer);
            if (listId >= 0) {
                glCallList(listId);
            }
#endif
            glPopMatrix2();
        }
		renderSameAsLast(layer, alpha);
        return count;
	}
        
    // ========== 非条纹修复模式（原有逻辑，通常不会启用）==========
    // 保留原实现，但一般 stripeFix 为 true，所以这里可简略
	if (!stripeFix) {
        double xOff2 = player ? (player->xOld + (player->x - player->xOld) * alpha) : 0.0;
        double yOff2 = player ? (player->yOld + (player->y - player->yOld) * alpha) : 0.0;
        double zOff2 = player ? (player->zOld + (player->z - player->zOld) * alpha) : 0.0;
        for (unsigned int i = 0; i < _renderChunks.size(); ++i) {
             Chunk* chunk = _renderChunks[i];
             double targetX = (double)chunk->x - xOff2;
             double targetY = 0;
             double targetZ = (double)chunk->z - zOff2;
             glPushMatrix2();
             glTranslatef2((float)targetX, (float)targetY, (float)targetZ);
#ifdef USE_VBO
             RenderChunk& rc = chunk->getRenderChunk(layer);
             if (rc.vertexCount > 0) renderChunkVBO(rc);
#else
             int listId = chunk->getList(layer);
             if (listId >= 0) glCallList(listId);
#endif
             glPopMatrix2();
        }
        renderSameAsLast(layer, alpha);
        return count;
	}
}

void LevelRenderer::renderChunkVBO(const RenderChunk& rc) {
    if (rc.vertexCount == 0) return;

    glEnableClientState2(GL_VERTEX_ARRAY);
    glEnableClientState2(GL_COLOR_ARRAY);
    glEnableClientState2(GL_TEXTURE_COORD_ARRAY);

    const int Stride = VertexSizeBytes;

    glBindBuffer2(GL_ARRAY_BUFFER, rc.vboId);
    glVertexPointer2(3, GL_FLOAT, Stride, 0);
    glTexCoordPointer2(2, GL_FLOAT, Stride, (GLvoid*)(3 * 4));
    glColorPointer2(4, GL_UNSIGNED_BYTE, Stride, (GLvoid*)(5 * 4));
    glDrawArrays2(GL_TRIANGLES, 0, rc.vertexCount);

    glDisableClientState2(GL_VERTEX_ARRAY);
    glDisableClientState2(GL_COLOR_ARRAY);
    glDisableClientState2(GL_TEXTURE_COORD_ARRAY);
}

void LevelRenderer::renderSameAsLast(int layer, float alpha) {
    renderList.render();
}

void LevelRenderer::tick()
{
	ticks++;
}

bool LevelRenderer::updateDirtyChunks( Mob* player, bool force )
{
	
	bool slow = false;

	if (slow) {
		DirtyChunkSorter dirtySorter(player);
		std::sort(dirtyChunks.begin(), dirtyChunks.end(), dirtySorter);
		int64_t s = dirtyChunks.size() - 1;
		int64_t amount = dirtyChunks.size();
		for (int i = 0; i < amount; i++) {
			Chunk* chunk = dirtyChunks[s-i];
			if (!force) {
				if (chunk->distanceToSqr(player) > 16 * 16) {
					if (chunk->visible) {
						if (i >= MAX_VISIBLE_REBUILDS_PER_FRAME) return false;
					} else {
						if (i >= MAX_INVISIBLE_REBUILDS_PER_FRAME) return false;
					}
				}
			} else {
				if (!chunk->visible) continue;
			}
			chunk->rebuild();

			dirtyChunks.erase( std::find(dirtyChunks.begin(), dirtyChunks.end(), chunk) ); // @q: s-i?
			chunk->setClean();
		}

		return dirtyChunks.size() == 0;
	} else {
		const int64_t count = 3;

		DirtyChunkSorter dirtyChunkSorter(player);
		Chunk* toAdd[count] = {NULL};
		std::vector<Chunk*>* nearChunks = NULL;

		int64_t pendingChunkSize = dirtyChunks.size();
		int64_t pendingChunkRemoved = 0;

		for (int64_t i = 0; i < pendingChunkSize; i++) {
			Chunk* chunk = dirtyChunks[i];

			if (!force) {
				if (chunk->distanceToSqr(player) > 1024.0f) {
					int index;

					// is this chunk in the closest <count>?
					for (index = 0; index < count; index++) {
						if (toAdd[index] != NULL && dirtyChunkSorter(toAdd[index], chunk) == false) {
							break;
						}
					}

					index--;

					if (index > 0) {
						int64_t x = index;
						while (--x != 0) {
							toAdd[x - 1] = toAdd[x];
						}
						toAdd[index] = chunk;
					}

					continue;
				}
			} else if (!chunk->visible) {
				continue;
			}

			// chunk is very close -- always render

			if (nearChunks == NULL) {
				nearChunks = new std::vector<Chunk*>();
			}

			pendingChunkRemoved++;
			nearChunks->push_back(chunk);
			dirtyChunks[i] = NULL;
		}

		// if there are nearby chunks that need to be prepared for
		// rendering, sort them and then process them
		static const float MaxFrameTime = 1.0f / 100.0f;
		Stopwatch chunkWatch;
		chunkWatch.start();

		if (nearChunks != NULL) {
			if (nearChunks->size() > 1) {
				std::sort(nearChunks->begin(), nearChunks->end(), dirtyChunkSorter);
			}

			for (int64_t i = nearChunks->size() - 1; i >= 0; i--) {
				Chunk* chunk = (*nearChunks)[i];
				chunk->rebuild();
				chunk->setClean();
			}
			delete nearChunks;
		}

		// render the nearest <count> chunks (farther than 1024 units away)
		int64_t secondaryRemoved = 0;

		for (int64_t i = count - 1; i >= 0; i--) {
			Chunk* chunk = toAdd[i];
			if (chunk != NULL) {

				float ttt = chunkWatch.stopContinue();
				if (ttt >= MaxFrameTime) {
					//LOGI("Too much work, I quit2!\n");
					break;
				}

				if (!chunk->visible && i != count - 1) {
					// escape early if chunks aren't ready
					toAdd[i] = NULL;
					toAdd[0] = NULL;
					break;
				}
				toAdd[i]->rebuild();
				toAdd[i]->setClean();
				secondaryRemoved++;
			}
		}

		// compact by removing nulls
		int64_t cursor = 0;
		int64_t target = 0;
		int64_t arraySize = dirtyChunks.size();
		while (cursor != arraySize) {
			Chunk* chunk = dirtyChunks[cursor];
			if (chunk != NULL) {
				bool remove = false;
                for (int i = 0; i < count && !remove; i++)
                    if (chunk == toAdd[i]) {
                        remove = true;
                    }

                if (!remove) {
				//if (chunk == toAdd[0] || chunk == toAdd[1] || chunk == toAdd[2]) {
				//	; // this chunk was rendered and should be removed
				//} else {
					if (target != cursor) {
						dirtyChunks[target] = chunk;
					}
					target++;
				}
			}
			cursor++;
		}

		// trim
		if (cursor > target)
			dirtyChunks.erase(dirtyChunks.begin() + target, dirtyChunks.end());

		return pendingChunkSize == (pendingChunkRemoved + secondaryRemoved);
	}
	
}

void LevelRenderer::renderHit( Player* player, const HitResult& h, int mode, /*ItemInstance*/void* inventoryItem, float a )
{
	if (mode == 0) {
		if (destroyProgress > 0) {
			Tesselator& t = Tesselator::instance;
			glEnable2(GL_BLEND);
			glBlendFunc2(GL_DST_COLOR, GL_SRC_COLOR);

			textures->loadAndBindTexture("terrain.png");
			glPushMatrix2();

			int tileId = level->getTile(h.x, h.y, h.z);
			Tile* tile = tileId > 0 ? Tile::tiles[tileId] : NULL;
			//glDisable2(GL_ALPHA_TEST);

			glPolygonOffset(-3.0f, -3.0f);
			glEnable2(GL_POLYGON_OFFSET_FILL);
			t.begin();
			t.color(1.0f, 1.0f, 1.0f, 0.5f);
			t.noColor();
			float xo = player->xOld + (player->x - player->xOld) * a;
			float yo = player->yOld + (player->y - player->yOld) * a;
			float zo = player->zOld + (player->z - player->zOld) * a;

			t.offset(-xo, -yo, -zo);
			//t.noColor();

			if (tile == NULL) tile = Tile::rock;
			const int progress = (int) (destroyProgress * 10);
			tileRenderer->tesselateInWorld(tile, h.x, h.y, h.z, 15 * 16 + progress);

			t.draw();
			t.offset(0, 0, 0);
			glPolygonOffset(0.0f, 0.0f);
			glDisable2(GL_POLYGON_OFFSET_FILL);
			//glDisable2(GL_ALPHA_TEST);
			glDisable2(GL_BLEND);

			glDepthMask(true);
			glPopMatrix2();
		}
	}
	//else if (inventoryItem != NULL) {
	//          glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	//          float br = ((float) (util.Mth::sin(System.currentTimeMillis() / 100.0f)) * 0.2f + 0.8f);
	//          glColor4f2(br, br, br, ((float) (util.Mth::sin(System.currentTimeMillis() / 200.0f)) * 0.2f + 0.5f));

	//          int id = textures.loadTexture("terrain.png");
	//          glBindTexture2(GL_TEXTURE_2D, id);
	//          int x = h.x;
	//          int y = h.y;
	//          int z = h.z;
	//          if (h.f == 0) y--;
	//          if (h.f == 1) y++;
	//          if (h.f == 2) z--;
	//          if (h.f == 3) z++;
	//          if (h.f == 4) x--;
	//          if (h.f == 5) x++;
	//          /*
	//           * t.begin(); t.noColor(); Tile.tiles[tileType].tesselate(level, x,
	//           * y, z, t); t.end();
	//           */
	//      }
}

void LevelRenderer::renderHitOutline( Player* player, const HitResult& h, int mode, /*ItemInstance*/void* inventoryItem, float a )
{
	if (mode == 0 && h.type == TILE) {
		glEnable2(GL_BLEND);
		glBlendFunc2(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glColor4f2(0, 0, 0, 0.4f);
		glLineWidth(1.0f);
		glDisable2(GL_TEXTURE_2D);
		glDepthMask(false);
		float ss = 0.002f;
		int tileId = level->getTile(h.x, h.y, h.z);

		if (tileId > 0) {
			Tile::tiles[tileId]->updateShape(level, h.x, h.y, h.z);
			float xo = player->xOld + (player->x - player->xOld) * a;
			float yo = player->yOld + (player->y - player->yOld) * a;
			float zo = player->zOld + (player->z - player->zOld) * a;
			render(Tile::tiles[tileId]->getTileAABB(level, h.x, h.y, h.z).grow(ss, ss, ss).cloneMove(-xo, -yo, -zo));
		}
		glDepthMask(true);
		glEnable2(GL_TEXTURE_2D);
		glDisable2(GL_BLEND);
	}
}

void LevelRenderer::setDirty( int x0, int y0, int z0, int x1, int y1, int z1 )
{   
	
	int _x0 = Mth::intFloorDiv(x0, CHUNK_SIZE);
	int _y0 = Mth::intFloorDiv(y0, CHUNK_SIZE);
	int _z0 = Mth::intFloorDiv(z0, CHUNK_SIZE);
	int _x1 = Mth::intFloorDiv(x1, CHUNK_SIZE);
	int _y1 = Mth::intFloorDiv(y1, CHUNK_SIZE);
	int _z1 = Mth::intFloorDiv(z1, CHUNK_SIZE);

	for (int x = _x0; x <= _x1; x++) {
		int xx = x % xChunks;
		if (xx < 0) xx += xChunks;
		for (int y = _y0; y <= _y1; y++) {
			int yy = y % yChunks;
			if (yy < 0) yy += yChunks;
			for (int z = _z0; z <= _z1; z++) {
				int zz = z % zChunks;
				if (zz < 0) zz += zChunks;

				int p = ((zz) * yChunks + (yy)) * xChunks + (xx);
				Chunk* chunk = chunks[p];
				if (!chunk->isDirty()) {
					dirtyChunks.push_back(chunk);
					chunk->setDirty();
				}
			}
		}
	}
	
}

void LevelRenderer::tileChanged( int x, int y, int z)
{
	setDirty(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1);
}


void LevelRenderer::setTilesDirty( int x0, int y0, int z0, int x1, int y1, int z1 )
{
	setDirty(x0 - 1, y0 - 1, z0 - 1, x1 + 1, y1 + 1, z1 + 1);
}


void LevelRenderer::cull(Culler* culler, float a) {
    // 缓存快速路径：摄像机没怎么动，直接跳过
    Mob* player = mc->cameraTargetPlayer;
    if (player) {
        double xOff = player->xOld + (player->x - player->xOld) * a;
        double yOff = player->yOld + (player->y - player->yOld) * a;
        double zOff = player->zOld + (player->z - player->zOld) * a;
        float yRot = player->yRot;
        float xRot = player->xRot;

        if (cullCacheValid &&
            fabs(xOff - lastCullCamX) < 2.0 &&
            fabs(yOff - lastCullCamY) < 2.0 &&
            fabs(zOff - lastCullCamZ) < 2.0 &&
            fabs(yRot - lastCullYRot) < 5.0f &&
            fabs(xRot - lastCullXRot) < 5.0f) {
            return;   // 缓存命中，完全跳过
        }

        // 缓存不命中，更新缓存（但不立即执行，需经过跳帧判断）
        lastCullCamX = xOff; lastCullCamY = yOff; lastCullCamZ = zOff;
        lastCullYRot = yRot; lastCullXRot = xRot;
    }

    // 跳帧：每两帧只执行一次真实剔除
    cullSkipTimer++;
    if (cullSkipTimer & 1) return;   // 奇数帧跳过

    // 真正执行视锥体剔除
    for (int i = 0; i < chunksLength; i++) {
        if (!chunks[i]->isEmpty()) {
            if (!chunks[i]->visible || ((i + cullStep) & 15) == 0) {
                chunks[i]->cull(culler);
            }
        }
    }
    cullStep++;
    cullCacheValid = true;   // 标记缓存有效
}

void LevelRenderer::skyColorChanged()
{
	for (int i = 0; i < chunksLength; i++) {
		if (chunks[i]->skyLit) {
			if (!chunks[i]->isDirty()) {
				dirtyChunks.push_back(chunks[i]);
				chunks[i]->setDirty();
			}
		}
	}
}

bool entityRenderPredicate(const Entity* a, const Entity* b) {
	return a->entityRendererId < b->entityRendererId;
}
void LevelRenderer::renderEntities(Vec3 cam, Culler* culler, float a) {
    if (!mc) return;
    Mob* player = mc->cameraTargetPlayer;
    if (!player) return;

    // 准备渲染调度器
    EntityRenderDispatcher* disp = EntityRenderDispatcher::getInstance();
    TileEntityRenderDispatcher* tileDisp = TileEntityRenderDispatcher::getInstance();
    disp->prepare(level, mc->font, player, &mc->options, a);
    tileDisp->prepare(level, textures, mc->font, player, a);

    // 设置相机偏移（不再叠加 worldOff，实体坐标已是世界坐标）
    double xOff, yOff, zOff;
    if (mc->options.getBooleanValue(OPTIONS_STRIPE_REPAIR)) {
        xOff = player->xOld + (player->x - player->xOld) * a;
        yOff = player->yOld + (player->y - player->yOld) * a;
        zOff = player->zOld + (player->z - player->zOld) * a;
    } else {
        xOff = yOff = zOff = 0.0;
    }
    disp->xOff = xOff;    disp->yOff = yOff;    disp->zOff = zOff;
    tileDisp->xOff = xOff; tileDisp->yOff = yOff; tileDisp->zOff = zOff;

    glEnableClientState2(GL_VERTEX_ARRAY);
    glEnableClientState2(GL_TEXTURE_COORD_ARRAY);

    // ★ 核心：从区块动态查找实体（完全不依赖被破坏的全局列表）
    AABB queryBox(player->x - 64, player->y - 64, player->z - 64,
                  player->x + 64, player->y + 64, player->z + 64);
    EntityList& nearby = level->getEntities(NULL, queryBox);

    for (size_t i = 0; i < nearby.size(); ++i) {
        Entity* entity = nearby[i];
        if (!entity || entity->removed) continue;

        bool thirdPerson = mc->options.getBooleanValue(OPTIONS_THIRD_PERSON_VIEW);
        if (entity == player && !thirdPerson) continue;   // 第一人称下隐藏自己

        Vec3 renderPos(entity->x, entity->y, entity->z);
        if (!entity->shouldRender(renderPos)) continue;
        if (!culler->isVisible(entity->bb)) continue;

        disp->render(entity, a);
    }

    // 方块实体（箱子、告示牌）照常
    for (unsigned int i = 0; i < level->tileEntities.size(); i++) {
        tileDisp->render(level->tileEntities[i], a);
    }

    glDisableClientState2(GL_VERTEX_ARRAY);
    glDisableClientState2(GL_TEXTURE_COORD_ARRAY);
}

std::string LevelRenderer::gatherStats1() {
	std::stringstream ss;
	ss << "C: " << renderedChunks << "/" << totalChunks << ". F: " << offscreenChunks << ", O: " << occludedChunks << ", E: " << emptyChunks << "\n";
    return ss.str();
}

//
//    /*public*/ std::string gatherStats2() {
//        return "E: " + renderedEntities + "/" + totalEntities + ". B: " + culledEntities + ", I: " + ((totalEntities - culledEntities) - renderedEntities);
//    }
//
//    int[] toRender = new int[50000];
//    IntBuffer resultBuffer = MemoryTracker.createIntBuffer(64);

// ==================== 完整 renderSky ====================
void LevelRenderer::renderSky(float a){
    if (mc->level->dimension->foggy) {
        // 末地：黑色天空 + 无天体
        bool isEnd = dynamic_cast<TheEndLevelSource*>(mc->level->getChunkSource()) != nullptr;
        if (isEnd) {
            // 只画纯黑天空底，不画太阳/月亮/星星
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
            glDepthMask(false);
            drawArrayVT(m_skyChunk.vboId, m_skyChunk.vertexCount);
            glDepthMask(true);
            glEnable(GL_TEXTURE_2D);
        }
        return;
	}
    
    Vec3 skyColor = level->getSkyColor(mc->cameraTargetPlayer, a);
    float r = (float)skyColor.x;
    float g = (float)skyColor.y;
    float b = (float)skyColor.z;

    if(mc->options.getBooleanValue(OPTIONS_ANAGLYPH_3D)){
        float rr = (r * 30.0f + g * 59.0f + b * 11.0f) / 100.0f;
        float gg = (r * 30.0f + g * 70.0f) / 100.0f;
        float bb = (r * 30.0f + b * 70.0f) / 100.0f;
        r = rr;
        g = gg;
        b = bb;
    }

    // ========== 上层天空穹顶 ==========
    glDisable(GL_TEXTURE_2D);
    glColor4f(r, g, b, 1.0f);
    glDepthMask(false);
    glEnable(GL_FOG);
    glColor4f(r, g, b, 1.0f);
    if(m_skyChunk.vboId != (GLuint)-1 && m_skyChunk.vertexCount > 0){
        drawArrayVT(m_skyChunk.vboId, m_skyChunk.vertexCount);
    }
    glDisable(GL_FOG);
    glDisable(GL_ALPHA_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // ========== 日出/日落光晕 ==========
    renderSunriseSunset(a);

    // ==================== renderSky 中的星星部分 ====================

    // ========== 太阳 / 月亮 ==========
    glEnable(GL_TEXTURE_2D);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    glPushMatrix();
    float rainStrength = 1.0f;
    glColor4f(1, 1, 1, rainStrength);
    float sunAngleDeg = level->getCelestialAngle(a) * (180.0f / Mth::PI);
    glRotatef(sunAngleDeg, 1.0f, 0.0f, 0.0f);
    
    renderSun(a);
    renderMoon(a);

    // 🧊 惰性初始化星星
    ensureStarsGenerated();

    float starBr = level->getStarBrightness(a) * rainStrength;
    
    if(starBr > 0.0f && m_starsChunk.vertexCount > 0){
        
        // ========== 第一遍：加法混合，让亮星在天空中叠加辉光 ==========
        // 🧊 必须禁用纹理！星星 VBO 没有 UV 数据，用纹理采样会取到月亮的黑边
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);  // 加法混合：星越亮，叠加越强
        
        glColor4f(starBr, starBr, starBr, starBr);
        drawArrayVT(m_starsChunk.vboId, m_starsChunk.vertexCount);
        
        // ========== 第二遍：标准 alpha 混合，让星星在半透明天空上可见 ==========
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(starBr, starBr, starBr, starBr * 0.7f);
        drawArrayVT(m_starsChunk.vboId, m_starsChunk.vertexCount);
        
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
    }

    glColor4f(1, 1, 1, 1);
    glDisable(GL_BLEND);
    glEnable(GL_ALPHA_TEST);
    glEnable(GL_FOG);
    glEnable(GL_DEPTH_TEST);
    glPopMatrix();

    // ========== 下层天空穹顶（地平线以下） ==========
    if(mc->level->dimension->foggy){
        glColor4f(r * 0.2f + 0.04f, g * 0.2f + 0.04f, b * 0.6f + 0.1f, 1.0f);
    } else {
        glColor4f(r, g, b, 1.0f);
    }
    glDisable(GL_TEXTURE_2D);
    if(m_skyChunk2.vboId != (GLuint)-1 && m_skyChunk2.vertexCount > 0){
        drawArrayVT(m_skyChunk2.vboId, m_skyChunk2.vertexCount);
    }
    glEnable(GL_TEXTURE_2D);
    glDepthMask(true);
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);
    glEnable(GL_ALPHA_TEST);
    glEnable(GL_FOG);
    glPopMatrix();
}

void LevelRenderer::renderSunriseSunset(float a) {
    float angle = level->getCelestialAngle(a);
    float time = level->getTimeOfDay(a);
    // 模拟 MCP 的 calcSunriseSunsetColors
    float alpha = 0.0f;
    float rr = 0, gg = 0, bb = 0;
    // 日出/日落大约在 angle 接近 0 或 180 度时，这里根据余弦判断
    float cosAngle = Mth::cos(angle);
    if (cosAngle > -0.05f && cosAngle < 0.25f) {
        alpha = (cosAngle + 0.05f) / 0.3f;
        alpha = 1.0f - alpha;
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;
        if (angle < Mth::PI) { // 上升（日出）
            rr = 1.0f; gg = 0.6f; bb = 0.2f;
        } else {
            rr = 0.8f; gg = 0.3f; bb = 0.6f;
        }
    }
    if (alpha <= 0.0f) return;

    float colors[4] = { rr, gg, bb, alpha };
    if (mc->options.getBooleanValue(OPTIONS_ANAGLYPH_3D)) {
        float tmp = (30*colors[0] + 59*colors[1] + 11*colors[2]) / 100.0f;
        colors[1] = (30*colors[0] + 70*colors[1]) / 100.0f;
        colors[2] = (30*colors[0] + 70*colors[2]) / 100.0f;
        colors[0] = tmp;
    }

    glDisable(GL_TEXTURE_2D);
    glShadeModel(GL_SMOOTH);
    glPushMatrix();
    glRotatef(90, 1, 0, 0);
    glRotatef(angle > Mth::PI ? 180.0f : 0.0f, 0, 0, 1);

    Tesselator& t = Tesselator::instance;
    t.begin(GL_TRIANGLE_FAN);
    t.color(colors[0], colors[1], colors[2], colors[3]);
    t.vertex(0.0f, 100.0f, 0.0f);
    t.color(colors[0], colors[1], colors[2], 0.0f);
    for (int i = 0; i <= 16; ++i) {
        float f12 = (i * Mth::PI * 2.0f) / 16.0f;
        float sin = Mth::sin(f12);
        float cos = Mth::cos(f12);
        t.vertex(120.0f * sin, 120.0f * cos, -cos * 40.0f * colors[3]);
    }
    t.draw();
    glPopMatrix();
    glShadeModel(GL_FLAT);
    glEnable(GL_TEXTURE_2D);
}

void LevelRenderer::renderClouds(float alpha){
    // 🧊 确保深度测试启用（防御性修复）
    glEnable(GL_DEPTH_TEST);
    // 🧊 云不写入深度缓冲（半透明物体标准做法）
    glDepthMask(false);
    
    glEnable2(GL_TEXTURE_2D);
    glDisable(GL_CULL_FACE);
    float yOffs = (float)(mc->player->yOld + (mc->player->y - mc->player->yOld) * alpha);
    int s = 32;
    int d = 256 / s;
    Tesselator& t = Tesselator::instance;

    textures->loadAndBindTexture("environment/clouds.png");

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    Vec3 cc = level->getCloudColor(alpha);
    float cr = (float)cc.x;
    float cg = (float)cc.y;
    float cb = (float)cc.z;

    float scale = 1 / 2048.0f;

    float time = (ticks + alpha);
    float xo = mc->player->xo + (mc->player->x - mc->player->xo) * alpha + time * 0.03f;
    float zo = mc->player->zo + (mc->player->z - mc->player->zo) * alpha;
    int xOffs = Mth::floor(xo / 2048);
    int zOffs = Mth::floor(zo / 2048);
    xo -= xOffs * 2048;
    zo -= zOffs * 2048;

    // ... 云几何生成代码保持不变 ...
	float yy = /*level.dimension.getCloudHeight()*/ 128 - yOffs + 0.33f;//mc->player->y + 1;
	float uo = (float) (xo * scale);
	float vo = (float) (zo * scale);
	t.begin();

	t.color(cr, cg, cb, 0.8f);
	for (int xx = -s * d; xx < +s * d; xx += s) {
		for (int zz = -s * d; zz < +s * d; zz += s) {
			t.vertexUV((float)xx, yy, (float)zz + s, xx * scale + uo, (zz + s) * scale + vo);
			t.vertexUV((float)xx + s, yy, (float)zz + s, (xx + s) * scale + uo, (zz + s) * scale + vo);
			t.vertexUV((float)xx + s, yy, (float)zz, (xx + s) * scale + uo, zz * scale + vo);
			t.vertexUV((float)xx, yy, (float)zz, xx * scale + uo, zz * scale + vo);
		}
	}
	t.endOverrideAndDraw();
	glColor4f(1, 1, 1, 1.0f);
	glDisable(GL_BLEND);
	glDepthMask(true);
    glDisable(GL_BLEND);
    glEnable(GL_CULL_FACE);
}

void LevelRenderer::playSound(const std::string& name, float x, float y, float z, float volume, float pitch) {
	// @todo: deny sounds here if sound is off (rather than waiting 'til SoundEngine)
	float dd = 16;

    if (volume > 1) dd *= volume;
    if (mc->cameraTargetPlayer->distanceToSqr(x, y, z) < dd * dd) {
        mc->soundEngine->play(name, x, y, z, volume, pitch);
    }
}

void LevelRenderer::addParticle(const std::string& name, float x, float y, float z, float xa, float ya, float za, int data) {

	float xd = mc->cameraTargetPlayer->x - x;
    float yd = mc->cameraTargetPlayer->y - y;
    float zd = mc->cameraTargetPlayer->z - z;
	float distanceSquared = xd * xd + yd * yd + zd * zd;

	//Particle* p = NULL;
	//if (name == "hugeexplosion") p = new HugeExplosionSeedParticle(level, x, y, z, xa, ya, za);
	//else if (name == "largeexplode") p = new HugeExplosionParticle(textures, level, x, y, z, xa, ya, za);

	//if (p) {
	//	if (distanceSquared < 32 * 32) {
	//		mc->particleEngine->add(p);
	//	} else { delete p; }
	//	return;
	//}

    const float particleDistance = 16;
    if (distanceSquared > particleDistance * particleDistance) return;

	//static Stopwatch sw;
	//sw.start();

    if (name == "bubble") mc->particleEngine->add(new BubbleParticle(level, x, y, z, xa, ya, za));
	else if (name == "crit") mc->particleEngine->add(new CritParticle2(level, x, y, z, xa, ya, za));
	else if (name == "smoke") mc->particleEngine->add(new SmokeParticle(level, x, y, z, xa, ya, za));
    //else if (name == "note") mc->particleEngine->add(new NoteParticle(level, x, y, z, xa, ya, za));
    else if (name == "explode") mc->particleEngine->add(new ExplodeParticle(level, x, y, z, xa, ya, za));
    else if (name == "flame") mc->particleEngine->add(new FlameParticle(level, x, y, z, xa, ya, za));
    else if (name == "lava") mc->particleEngine->add(new LavaParticle(level, x, y, z));
    //else if (name == "splash") mc->particleEngine->add(new SplashParticle(level, x, y, z, xa, ya, za));
	else if (name == "largesmoke") mc->particleEngine->add(new SmokeParticle(level, x, y, z, xa, ya, za, 2.5f));
    else if (name == "reddust") mc->particleEngine->add(new RedDustParticle(level, x, y, z, xa, ya, za));
	else if (name == "iconcrack") mc->particleEngine->add(new BreakingItemParticle(level, x, y, z, xa, ya, za, Item::items[data]));
	else if (name == "snowballpoof") mc->particleEngine->add(new BreakingItemParticle(level, x, y, z, Item::snowBall));
    //else if (name == "snowballpoof") mc->particleEngine->add(new BreakingItemParticle(level, x, y, z, Item::snowBall));
    //else if (name == "slime") mc->particleEngine->add(new BreakingItemParticle(level, x, y, z, Item::slimeBall));
    //else if (name == "heart") mc->particleEngine->add(new HeartParticle(level, x, y, z, xa, ya, za));

	//sw.stop();
	//sw.printEvery(50, "add-particle-string");
}

/*
void LevelRenderer::addParticle(ParticleType::Id name, float x, float y, float z, float xa, float ya, float za, int data) {
	float xd = mc->cameraTargetPlayer->x - x;
	float yd = mc->cameraTargetPlayer->y - y;
	float zd = mc->cameraTargetPlayer->z - z;

	const float particleDistance = 16;
	if (xd * xd + yd * yd + zd * zd > particleDistance * particleDistance) return;

	//static Stopwatch sw;
	//sw.start();

	//Particle* p = NULL;

	if (name == ParticleType::bubble)		mc->particleEngine->add( new BubbleParticle(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::crit)		mc->particleEngine->add(new CritParticle2(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::smoke)		mc->particleEngine->add(new SmokeParticle(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::explode)		mc->particleEngine->add( new ExplodeParticle(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::flame)		mc->particleEngine->add( new FlameParticle(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::lava)		mc->particleEngine->add( new LavaParticle(level, x, y, z) );
	else if (name == ParticleType::largesmoke)	mc->particleEngine->add( new SmokeParticle(level, x, y, z, xa, ya, za, 2.5f) );
	else if (name == ParticleType::reddust)		mc->particleEngine->add( new RedDustParticle(level, x, y, z, xa, ya, za) );
	else if (name == ParticleType::iconcrack)	mc->particleEngine->add( new BreakingItemParticle(level, x, y, z, xa, ya, za, Item::items[data]) );

	//switch (name) {
	//	case ParticleType::bubble:		p = new BubbleParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::crit:		p = new CritParticle2(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::smoke:		p = new SmokeParticle(level, x, y, z, xa, ya, za); break;
	//	//case ParticleType::note: p = new NoteParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::explode:		p = new ExplodeParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::flame:		p = new FlameParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::lava:		p = new LavaParticle(level, x, y, z); break;
	//	//case ParticleType::splash: p = new SplashParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::largesmoke:	p = new SmokeParticle(level, x, y, z, xa, ya, za, 2.5f); break;
	//	case ParticleType::reddust:		p = new RedDustParticle(level, x, y, z, xa, ya, za); break;
	//	case ParticleType::iconcrack:	p = new BreakingItemParticle(level, x, y, z, xa, ya, za, Item::items[data]); break;
	//	//case ParticleType::snowballpoof: p = new BreakingItemParticle(level, x, y, z, Item::snowBall); break;
	//	//case ParticleType::slime: p = new BreakingItemParticle(level, x, y, z, Item::slimeBall); break;
	//	//case ParticleType::heart: p = new HeartParticle(level, x, y, z, xa, ya, za); break;
	//	default:
	//		LOGW("Couldn't find particle of type: %d\n", name);
	//		break;
	//}
	//if (p) {
	//	mc->particleEngine->add(p);
	//}

	//sw.stop();
	//sw.printEvery(50, "add-particle-enum");
}
*/

void LevelRenderer::renderHitSelect( Player* player, const HitResult& h, int mode, /*ItemInstance*/void* inventoryItem, float a )
{
	//if (h.type == TILE) LOGI("type: %s @ (%d, %d, %d)\n", Tile::tiles[level->getTile(h.x, h.y, h.z)]->getDescriptionId().c_str(), h.x, h.y, h.z);

	if (mode == 0) {

		Tesselator& t = Tesselator::instance;
		glEnable2(GL_BLEND);
		glDisable2(GL_TEXTURE_2D);
		glBlendFunc2(GL_SRC_ALPHA, GL_ONE);
		glBlendFunc2(GL_DST_COLOR, GL_SRC_COLOR);
		glEnable2(GL_DEPTH_TEST);

		textures->loadAndBindTexture("terrain.png");
		
		int tileId = level->getTile(h.x, h.y, h.z);
		Tile* tile = tileId > 0 ? Tile::tiles[tileId] : NULL;
		glDisable2(GL_ALPHA_TEST);

		//LOGI("block: %d - %d (%s)\n", tileId, level->getData(h.x, h.y, h.z), tile==NULL?"null" : tile->getDescriptionId().c_str() );

		const float br = 0.65f;
		glColor4f2(br * 1.0f, br * 1.0f, br * 1.0f, br * 1.0f);
		glPushMatrix2();

		//glPolygonOffset(-.3f, -.3f);
		glPolygonOffset(-1.f, -1.f); //Implementation dependent units
		glEnable2(GL_POLYGON_OFFSET_FILL);
		float xo = player->xOld + (player->x - player->xOld) * a;
		float yo = player->yOld + (player->y - player->yOld) * a;
		float zo = player->zOld + (player->z - player->zOld) * a;

		t.begin();
		t.offset(-xo, -yo, -zo);
		t.noColor();

		if (tile == NULL) tile = Tile::rock;
		tileRenderer->tesselateInWorld(tile, h.x, h.y, h.z);

		t.draw();
		t.offset(0, 0, 0);
		glPolygonOffset(0.0f, 0.0f);

		glDisable2(GL_POLYGON_OFFSET_FILL);
		glEnable2(GL_TEXTURE_2D);

		glDepthMask(true);
		glPopMatrix2();

		glEnable2(GL_ALPHA_TEST);
		glDisable2(GL_BLEND);
	}
}

void LevelRenderer::onGraphicsReset(){
    // 重建天空几何
    if(m_skyChunk.vboId != (GLuint)-1){
        glDeleteBuffers(1, &m_skyChunk.vboId);
    }
    if(m_skyChunk2.vboId != (GLuint)-1){
        glDeleteBuffers(1, &m_skyChunk2.vboId);
    }
    if(m_starsChunk.vboId != (GLuint)-1 && m_starsGenerated){
        glDeleteBuffers(1, &m_starsChunk.vboId);
    }
    m_starsGenerated = false;
    generateSky();
	// Get new buffers
#ifdef OPENGL_ES
	glGenBuffers2(numListsOrBuffers, chunkBuffers);
#else
	chunkLists = glGenLists(numListsOrBuffers);
#endif

	// Rebuild
	allChanged();
}

void LevelRenderer::entityAdded(Entity* entity) {
    // 安全起见，暂时清空，避免任何可能的副作用
}

void LevelRenderer::entityRemoved(Entity* entity) {
    // 同上
}

int _t_keepPic = -1;

void LevelRenderer::takePicture( TripodCamera* cam, Entity* entity )
{
	// Push old values
	Mob* oldCameraEntity = mc->cameraTargetPlayer;
	bool hideGui = mc->options.getBooleanValue(OPTIONS_HIDEGUI);
	bool thirdPerson = mc->options.getBooleanValue(OPTIONS_THIRD_PERSON_VIEW);

	// @huge @attn: This is highly illegal, super temp!
	mc->cameraTargetPlayer = (Mob*)cam;
	mc->options.set(OPTIONS_HIDEGUI, true);
	mc->options.set(OPTIONS_THIRD_PERSON_VIEW, false);

	mc->gameRenderer->renderLevel(0);

	// Pop values back
	mc->cameraTargetPlayer = oldCameraEntity;
	mc->options.set(OPTIONS_HIDEGUI, hideGui);
	mc->options.set(OPTIONS_THIRD_PERSON_VIEW, thirdPerson);

	_t_keepPic = -1;

	// Save image
	static char filename[256];
	sprintf(filename, "%s/games/com.mojang/img_%.4d.jpg", mc->externalStoragePath.c_str(), getTimeMs());

	mc->platform()->saveScreenshot(filename, mc->width, mc->height);
}

void LevelRenderer::levelEvent(Player* player, int type, int x, int y, int z, int data) {
	switch (type) {    
	case LevelEvent::SOUND_OPEN_DOOR:
        if (Mth::random() < 0.5f) {
            level->playSound(x + 0.5f, y + 0.5f, z + 0.5f, "random.door_open", 1, level->random.nextFloat() * 0.1f + 0.9f);
        } else {
            level->playSound(x + 0.5f, y + 0.5f, z + 0.5f, "random.door_close", 1, level->random.nextFloat() * 0.1f + 0.9f);
        }
        break;
	}
}



