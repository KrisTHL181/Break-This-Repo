#include "Entity.h"
#include "EntityPos.h"
#include "../level/Level.h"
#include "../level/tile/LiquidTile.h"
#include "item/ItemEntity.h"
#include "../item/ItemInstance.h"
#include "../../nbt/CompoundTag.h"
#include "../../util/PerfTimer.h"
#include "../../util/WorldCoordinate.h"

int Entity::entityCounter = 0;
Random Entity::sharedRandom(getEpochTimeS());

Entity::Entity(Level* level)
:   level(level),
    viewScale(1.0f),
    blocksBuilding(false),
    onGround(false),
    wasInWater(false),
    collision(false),
    hurtMarked(false),
    slide(true),
    isStuckInWeb(false),
    removed(false),
    reallyRemoveIfPlayer(false),
    canRemove(true),
    noPhysics(false),
    firstTick(true),

    bbWidth(0.6f),
    bbHeight(1.8f),
    heightOffset(0 / 16.0f),
    bb(0,0,0,0,0,0),

    ySlideOffset(0),
    fallDistance(0),
    footSize(0),
    invulnerableTime(0),
    pushthrough(0),
    airCapacity(TOTAL_AIR_SUPPLY),
    airSupply(TOTAL_AIR_SUPPLY),

    xOld(0), yOld(0), zOld(0),
    horizontalCollision(false), verticalCollision(false),

    x(0), y(0), z(0),
    xo(0), yo(0), zo(0), xd(0), yd(0), zd(0),
    xRot(0), yRot(0),
    xRotO(0), yRotO(0),

    xChunk(0), yChunk(0), zChunk(0),
    inChunk(false),

    fireImmune(false),
    onFire(0),
    flameTime(1),
    walkDist(0), walkDistO(0),
    tickCount(0),
    entityRendererId(ER_DEFAULT_RENDERER),
    nextStep(1),
    makeStepSound(true),
    invisible(false)
{
    _init();
    entityId = ++entityCounter;
    setPos(0, 0, 0);
}

Entity::~Entity() {}

SynchedEntityData* Entity::getEntityData() { return NULL; }
const SynchedEntityData* Entity::getEntityData() const { return NULL; }

bool Entity::isInWall(){
    int64_t xt = static_cast<int64_t>(
        getBigAbsX().convert_to<BigWorldCoordinate_Integer>());
    int64_t yt = Mth::floor64(y + getHeadHeight());
    int64_t zt = static_cast<int64_t>(
        getBigAbsZ().convert_to<BigWorldCoordinate_Integer>());
    return level->isSolidBlockingTile(xt, yt, zt);
}

void Entity::resetPos(bool clearMore) {
    if (level == NULL) return;
    while (y > 0) {
        setPos(x, y, z);
        if (level->getCubes(this, bb).size() == 0) break;
        y += 1;
    }
    setVelocity(BigWorldCoordinate(0.0), BigWorldCoordinate(0.0), BigWorldCoordinate(0.0));
    xRot = 0;
}

bool Entity::isInWater() {
	int64_t bx = static_cast<int64_t>(getBigAbsX().convert_to<BigWorldCoordinate_Integer>());
int64_t bz = static_cast<int64_t>(getBigAbsZ().convert_to<BigWorldCoordinate_Integer>());
return level->checkAndHandleWater(
    AABB((double)bx - bbWidth/2, bb.y0, (double)bz - bbWidth/2,
         (double)bx + bbWidth/2, bb.y1, (double)bz + bbWidth/2),
    Material::water, this);
}

bool Entity::isInLava() {
    int64_t bx = static_cast<int64_t>(getBigAbsX().convert_to<BigWorldCoordinate_Integer>());
int64_t bz = static_cast<int64_t>(getBigAbsZ().convert_to<BigWorldCoordinate_Integer>());
return level->containsMaterial(
    AABB((double)bx - bbWidth/2, bb.y0, (double)bz - bbWidth/2,
         (double)bx + bbWidth/2, bb.y1, (double)bz + bbWidth/2),
    Material::lava);
}

bool Entity::isFree(float xa, float ya, float za, float grow) {
    AABB box = bb.grow(grow, grow, grow).cloneMove(xa, ya, za);
    const std::vector<AABB>& aABBs = level->getCubes(this, box);
    if (aABBs.size() > 0) return false;
    if (level->containsAnyLiquid(box)) return false;
    return true;
}

bool Entity::isFree(float xa, float ya, float za) {
    AABB box = bb.cloneMove(xa, ya, za);
    const std::vector<AABB>& aABBs = level->getCubes(this, box);
    if (aABBs.size() > 0) return false;
    if (level->containsAnyLiquid(box)) return false;
    return true;
}

// src/world/entity/Entity.cpp
// 加在 Entity::move() 之前

void Entity::updatePositionFromBB(){
	double frameOx = getLocalFrameOriginX();
	double frameOz = getLocalFrameOriginZ();

	if(frameOx != 0.0 || frameOz != 0.0){
		BigWorldCoordinate bigOx = getLocalFrameOriginBigX();
		BigWorldCoordinate bigOz = getLocalFrameOriginBigZ();

		BigWorldCoordinate bx0 = BigWorldCoordinate(bb.x0) + bigOx;
		BigWorldCoordinate bx1 = BigWorldCoordinate(bb.x1) + bigOx;
		BigWorldCoordinate bxc = (bx0 + bx1) / BigWorldCoordinate(2.0);

		BigWorldCoordinate bz0 = BigWorldCoordinate(bb.z0) + bigOz;
		BigWorldCoordinate bz1 = BigWorldCoordinate(bb.z1) + bigOz;
		BigWorldCoordinate bzc = (bz0 + bz1) / BigWorldCoordinate(2.0);

		BigWorldCoordinate byc = BigWorldCoordinate(bb.y0 + heightOffset - ySlideOffset);
		storeAbsolutePosition(bxc, byc, bzc);
	} else {
		BigWorldCoordinate bxc = BigWorldCoordinate((bb.x0 + bb.x1) / 2.0);
		BigWorldCoordinate byc = BigWorldCoordinate(bb.y0 + heightOffset - ySlideOffset);
		BigWorldCoordinate bzc = BigWorldCoordinate((bb.z0 + bb.z1) / 2.0);
		storeAbsolutePosition(bxc, byc, bzc);
	}
}

// src/world/entity/Entity.cpp
// 替换整个函数：

void Entity::move(double xa, double ya, double za){
	if(noPhysics){
		bb.move(xa, ya, za);
		BigWorldCoordinate bxc = BigWorldCoordinate((bb.x0 + bb.x1) / 2.0);
		BigWorldCoordinate byc = BigWorldCoordinate(bb.y0 + heightOffset - ySlideOffset);
		BigWorldCoordinate bzc = BigWorldCoordinate((bb.z0 + bb.z1) / 2.0);
		storeAbsolutePosition(bxc, byc, bzc);
		return;
	}

	TIMER_PUSH("move");

	double xo_ = x;
	double zo_ = z;

	// ====== Big + double 双版本 origin ======
	BigWorldCoordinate bigOx = getLocalFrameOriginBigX();
	BigWorldCoordinate bigOy = getLocalFrameOriginBigY();
	BigWorldCoordinate bigOz = getLocalFrameOriginBigZ();
	double ox = bigOx.convert_to<double>();
	double oy = bigOy.convert_to<double>();
	double oz = bigOz.convert_to<double>();

	bool useLocal = !bigOx.is_zero() || !bigOy.is_zero() || !bigOz.is_zero();

    // bb 转到 local 空间
    if(useLocal){
		// ====== 从 Big 绝对位置直接重建 local bb ======
		BigWorldCoordinate bigAbsX = getBigAbsX();
		BigWorldCoordinate bigAbsY = getBigAbsY();
		BigWorldCoordinate bigAbsZ = getBigAbsZ();

		BigWorldCoordinate bigLocalX = bigAbsX - bigOx;
		BigWorldCoordinate bigLocalY = bigAbsY - bigOy;
		BigWorldCoordinate bigLocalZ = bigAbsZ - bigOz;

		double localCX = bigLocalX.convert_to<double>();
		double localCY = bigLocalY.convert_to<double>();
		double localCZ = bigLocalZ.convert_to<double>();

		double bw = bbWidth / 2.0;
		double bh = bbHeight;
		double yBase = localCY - heightOffset;  // 步高偏移由碰撞检测自然处理。

		bb.set(localCX - bw, yBase, localCZ - bw,
		       localCX + bw, yBase + bh, localCZ + bw);

		static BigWorldCoordinate lastBigOx = bigOx;
	if(!(bigOx == lastBigOx)){
		ySlideOffset = 0.0;
		lastBigOx = bigOx;

		// 不再用 bb.move(-ox, -oy, -oz) — Big 重建已覆盖
	}
	}

    if (isStuckInWeb) {
        isStuckInWeb = false;
        xa *= .25;
        ya *= .05;
        za *= .25;
        xd = 0.0;
        yd = 0.0;
        zd = 0.0;
    }

    double xaOrg = xa;
    double yaOrg = ya;
    double zaOrg = za;

    AABB bbOrg = bb;

    bool sneaking = onGround && isSneaking();

    // ── sneaking 逻辑：getCubes 需要传绝对坐标 AABB ──
    if (sneaking) {
        float d = 0.05f;
        AABB absBB_ = bb; if (useLocal) absBB_.move(ox, oy, oz);
        while (xa != 0 && level->getCubes(this, absBB_.cloneMove(xa, -1.0, 0)).empty()) {
            if (xa < d && xa >= -d) xa = 0;
            else if (xa > 0) xa -= d;
            else xa += d;
            xaOrg = xa;
        }
        AABB absBB_2 = bb; if (useLocal) absBB_2.move(ox, oy, oz);
        while (za != 0 && level->getCubes(this, absBB_2.cloneMove(0, -1.0, za)).empty()) {
            if (za < d && za >= -d) za = 0;
            else if (za > 0) za -= d;
            else za += d;
            zaOrg = za;
        }
        AABB absBB_3 = bb; if (useLocal) absBB_3.move(ox, oy, oz);
        while (xa != 0 && za != 0 && level->getCubes(this, absBB_3.cloneMove(xa, -1.0, za)).empty()) {
            if (xa < d && xa >= -d) xa = 0;
            else if (xa > 0) xa -= d;
            else xa += d;
            if (za < d && za >= -d) za = 0;
            else if (za > 0) za -= d;
            else za += d;
            xaOrg = xa;
            zaOrg = za;
        }
    }

    // getCubes：构造绝对坐标 AABB 来查询
    // 用 Big 速度合成展开量，避免 double xa 在远距离精度丢失
double bigXa = m_bigVx.convert_to<double>(); // 速度本身量级 ~0.1, double 保存精度够
// 但入口 xa 是传参，改为用 Big 速度:
double useXa = (m_bigVx != BigWorldCoordinate(0.0)) ? m_bigVx.convert_to<double>() : xa;
double useYa = (m_bigVy != BigWorldCoordinate(0.0)) ? m_bigVy.convert_to<double>() : ya;
double useZa = (m_bigVz != BigWorldCoordinate(0.0)) ? m_bigVz.convert_to<double>() : za;
AABB absExpand = bb.expand(useXa, useYa, useZa);
    if (useLocal) absExpand.move(ox, oy, oz);
    std::vector<AABB>& aABBs = level->getCubes(this, absExpand);

    // 返回的碰撞盒 shift 到 local
    if (useLocal) {
        for (unsigned int i = 0; i < aABBs.size(); i++)
            aABBs[i].move(-ox, -oy, -oz);
    }

    // ── 碰撞（local 空间） ──
    for (unsigned int i = 0; i < aABBs.size(); i++)
        ya = aABBs[i].clipYCollide(bb, ya);
    bb.move(0, ya, 0);

    if (!slide && yaOrg != ya) { xa = ya = za = 0; }

    bool og = onGround || (yaOrg != ya && yaOrg < 0);

    for (unsigned int i = 0; i < aABBs.size(); i++)
        xa = aABBs[i].clipXCollide(bb, xa);
    bb.move(xa, 0, 0);

    if (!slide && xaOrg != xa) { xa = ya = za = 0; }

    for (unsigned int i = 0; i < aABBs.size(); i++)
        za = aABBs[i].clipZCollide(bb, za);
    bb.move(0, 0, za);

    if (!slide && zaOrg != za) { xa = ya = za = 0; }

    // ── step-up（同 local + 绝对 getCubes） ──
    if (footSize > 0 && og && (ySlideOffset < 0.05f) && ((xaOrg != xa) || (zaOrg != za))) {
        double xaN = xa, yaN = ya, zaN = za;
        xa = xaOrg; ya = footSize; za = zaOrg;
        AABB normal = bb;
        bb.set(bbOrg);

        AABB absExpand2 = bb.expand(xa, ya, za);
        if (useLocal) absExpand2.move(ox, oy, oz);
        std::vector<AABB>& aABBs2 = level->getCubes(this, absExpand2);
        if (useLocal) {
            for (unsigned int i = 0; i < aABBs2.size(); i++)
                aABBs2[i].move(-ox, -oy, -oz);
        }

        for (unsigned int i = 0; i < aABBs2.size(); i++)
            ya = aABBs2[i].clipYCollide(bb, ya);
        bb.move(0, ya, 0);
        if (!slide && yaOrg != ya) { xa = ya = za = 0; }

        for (unsigned int i = 0; i < aABBs2.size(); i++)
            xa = aABBs2[i].clipXCollide(bb, xa);
        bb.move(xa, 0, 0);
        if (!slide && xaOrg != xa) { xa = ya = za = 0; }

        for (unsigned int i = 0; i < aABBs2.size(); i++)
            za = aABBs2[i].clipZCollide(bb, za);
        bb.move(0, 0, za);
        if (!slide && zaOrg != za) { xa = ya = za = 0; }

        if (xaN * xaN + zaN * zaN >= xa * xa + za * za) {
            xa = xaN; ya = yaN; za = zaN;
            bb.set(normal);
        } else {
            ySlideOffset += 0.5f;
        }
    }

    // ═══════════════════════════════════════════════════
    // 转回绝对空间 + Big 精度位置合成
    // ═══════════════════════════════════════════════════
    if(useLocal){
	// Big 合成绝对位置 (纯 Big 算术, 50 位精度)
	BigWorldCoordinate bigOx = getLocalFrameOriginBigX();
	BigWorldCoordinate bigOy = getLocalFrameOriginBigY();
	BigWorldCoordinate bigOz = getLocalFrameOriginBigZ();

	BigWorldCoordinate bx0 = BigWorldCoordinate(bb.x0) + bigOx;
	BigWorldCoordinate bx1 = BigWorldCoordinate(bb.x1) + bigOx;
	BigWorldCoordinate bxc = (bx0 + bx1) / BigWorldCoordinate(2.0);

	BigWorldCoordinate bz0 = BigWorldCoordinate(bb.z0) + bigOz;
	BigWorldCoordinate bz1 = BigWorldCoordinate(bb.z1) + bigOz;
	BigWorldCoordinate bzc = (bz0 + bz1) / BigWorldCoordinate(2.0);

	BigWorldCoordinate byc = BigWorldCoordinate(bb.y0) + bigOy 
		                       + BigWorldCoordinate(heightOffset - ySlideOffset);

	storeAbsolutePosition(bxc, byc, bzc);  // ← 🔧 就这一行

	// useLocal 分支末尾：
double ox_d = bigOx.convert_to<double>();
double oy_d = bigOy.convert_to<double>();
double oz_d = bigOz.convert_to<double>();
bb.move(ox_d, oy_d, oz_d);
} else {
	BigWorldCoordinate bxc = BigWorldCoordinate((bb.x0 + bb.x1) / 2.0);
	BigWorldCoordinate byc = BigWorldCoordinate(bb.y0 + heightOffset - ySlideOffset);
	BigWorldCoordinate bzc = BigWorldCoordinate((bb.z0 + bb.z1) / 2.0);
	storeAbsolutePosition(bxc, byc, bzc);
	}
    
    TIMER_POP_PUSH("rest");

    horizontalCollision = (xaOrg != xa) || (zaOrg != za);
    verticalCollision   = (yaOrg != ya);
    onGround            = yaOrg != ya && yaOrg < 0;
    collision           = horizontalCollision || verticalCollision;
    checkFallDamage((float)ya, onGround);

    if(xaOrg != xa) setVelocity(BigWorldCoordinate(0.0), m_bigVy, m_bigVz);
if(yaOrg != ya) setVelocity(m_bigVx, BigWorldCoordinate(0.0), m_bigVz);
if(zaOrg != za) setVelocity(m_bigVx, m_bigVy, BigWorldCoordinate(0.0));
	
    double xm = x - xo_;
    double zm = z - zo_;

    if (makeStepSound && !sneaking) {
        walkDist += (float)Mth::sqrt(xm * xm + zm * zm) * 0.6f;
        int64_t xt = Mth::floor64(x);
        int64_t yt = Mth::floor64(y - 0.2f - this->heightOffset);
        int64_t zt = Mth::floor64(z);
        int t = level->getTile(xt, yt, zt);
        if (t == 0) {
            int under = level->getTile(xt, yt - 1, zt);
            if (Tile::fence->id == under || Tile::fenceGate->id == under) t = under;
        }
        if (walkDist > nextStep && t > 0) {
            nextStep = ((int)walkDist) + 1;
            playStepSound(xt, yt, zt, t);
        }
    }

    // 用 BigWorldCoordinate_Integer 精确 floor XZ 坐标
BigWorldCoordinate_Integer bx0 = static_cast<BigWorldCoordinate_Integer>(
    getBigAbsX().convert_to<BigWorldCoordinate_Integer>()) - 1;
BigWorldCoordinate_Integer bx1 = bx0 + 2;
BigWorldCoordinate_Integer bz0 = static_cast<BigWorldCoordinate_Integer>(
    getBigAbsZ().convert_to<BigWorldCoordinate_Integer>()) - 1;
BigWorldCoordinate_Integer bz1 = bz0 + 2;
int64_t x0 = static_cast<int64_t>(bx0);
int64_t x1 = static_cast<int64_t>(bx1);
int64_t z0 = static_cast<int64_t>(bz0);
int64_t z1 = static_cast<int64_t>(bz1);
int64_t y0 = Mth::floor64(bb.y0);
int64_t y1 = Mth::floor64(bb.y1);

if(level->hasChunksAt(x0, y0, z0, x1, y1, z1)){
        for (int64_t tx = x0; tx <= x1; tx++)
            for (int64_t ty = y0; ty <= y1; ty++)
                for (int64_t tz = z0; tz <= z1; tz++) {
                    int t = level->getTile(tx, ty, tz);
                    if (t > 0) Tile::tiles[t]->entityInside(level, (int)tx, (int)ty, (int)tz, this);
                }
    }

    ySlideOffset *= 0.4f;

    bool water = this->isInWater();
    if (level->containsFireTile(bb)) {
        burn(1);
        if (!water) {
            onFire++;
            if (onFire == 0) onFire = 20 * 15;
        }
    } else {
        if (onFire <= 0) onFire = -flameTime;
    }

    if (water && onFire > 0) onFire = -flameTime;

    TIMER_POP();
}

void Entity::makeStuckInWeb() {
    isStuckInWeb = true;
    fallDistance = 0;
}

bool Entity::isUnderLiquid(const Material* material) {
    double yp = y + getHeadHeight();
    int64_t xt = Mth::floor64(x);
    int64_t yt = Mth::floor64(yp);
    int64_t zt = Mth::floor64(z);
    int t = level->getTile(xt, yt, zt);
    if (t != 0 && Tile::tiles[t]->material == material) {
        float hh = LiquidTile::getHeight(level->getData(xt, yt, zt)) - 1 / 9.0f;
        float h = (float)(yt + 1 - hh);
        return yp < h;
    }
    return false;
}

void Entity::setPos(EntityPos* pos) {
    if (pos->move) setPos(pos->x, pos->y, pos->z);
    else setPos(x, y, z);
    if (pos->rot) setRot(pos->yRot, pos->xRot);
    else setRot(yRot, xRot);
}

void Entity::setPos(double x, double y, double z) {
    this->x = x; this->y = y; this->z = z;
    float w = bbWidth / 2;
    float h = bbHeight;
    bb.set(x - w, y - heightOffset + ySlideOffset, z - w,
           x + w, y - heightOffset + ySlideOffset + h, z + w);
}

float Entity::getBrightness(float a) {
    int64_t xTile = Mth::floor64(x);
    float hh = (bb.y1 - bb.y0) * 0.66f;
    int64_t yTile = Mth::floor64(y - this->heightOffset + hh);
    int64_t zTile = Mth::floor64(z);
    if (level->hasChunksAt(Mth::floor64(bb.x0), Mth::floor64(bb.y0), Mth::floor64(bb.z0),
                           Mth::floor64(bb.x1), Mth::floor64(bb.y1), Mth::floor64(bb.z1))) {
        return level->getBrightness(xTile, yTile, zTile);
    }
    return 0;
}

bool Entity::operator==(Entity& rhs) { return entityId == rhs.entityId; }
int Entity::hashCode() { return entityId; }
void Entity::remove() { removed = true; }
void Entity::setSize(float w, float h) { bbWidth = w; bbHeight = h; }
void Entity::setRot(float yRot, float xRot) { this->yRot = yRotO = yRot; this->xRot = xRotO = xRot; }

void Entity::turn(float xo, float yo) {
    float xRotOld = xRot, yRotOld = yRot;
    yRot += xo * 0.15f;
    xRot -= yo * 0.15f;
    if (xRot < -90) xRot = -90;
    if (xRot > 90) xRot = 90;
    xRotO += xRot - xRotOld;
    yRotO += yRot - yRotOld;
}

void Entity::interpolateTurn(float xo, float yo) {
    yRot += xo * 0.15f;
    xRot -= yo * 0.15f;
    if (xRot < -90) xRot = -90;
    if (xRot > 90) xRot = 90;
}

void Entity::tick() { baseTick(); }

void Entity::baseTick() {
    TIMER_PUSH("entityBaseTick");
    tickCount++;
    walkDistO = walkDist;
    xo = x; yo = y; zo = z;
    xRotO = xRot; yRotO = yRot;
    if (isInWater()) {
        if (!wasInWater && !firstTick) {
            float speed = (float)sqrt(xd * xd * 0.2 + yd * yd + zd * zd * 0.2) * 0.2f;
            if (speed > 1) speed = 1;
            level->playSound(this, "random.splash", speed, 1 + (sharedRandom.nextFloat() - sharedRandom.nextFloat()) * 0.4f);
            float yt = (float)floor(bb.y0);
            for (int i = 0; i < 1 + bbWidth * 20; i++) {
                float xo = (sharedRandom.nextFloat() * 2 - 1) * bbWidth;
                float zo = (sharedRandom.nextFloat() * 2 - 1) * bbWidth;
                level->addParticle(PARTICLETYPE(bubble), (float)x + xo, yt + 1, (float)z + zo,
                                   (float)xd, (float)yd - sharedRandom.nextFloat() * 0.2f, (float)zd);
            }
        }
        fallDistance = 0;
        wasInWater = true;
        onFire = 0;
    } else {
        wasInWater = false;
    }
    if (level->isClientSide) {
        onFire = 0;
    } else {
        if (onFire > 0) {
            if (fireImmune) {
                onFire -= 4;
                if (onFire < 0) onFire = 0;
            } else {
                if (onFire % 20 == 0) hurt(NULL, 1);
                onFire--;
            }
        }
    }
    if (isInLava()) lavaHurt();
    if (y < -64) outOfWorld();
    firstTick = false;
    TIMER_POP();
}

void Entity::outOfWorld() { remove(); }

void Entity::checkFallDamage(float ya, bool onGround) {
    if (onGround) {
        if (fallDistance > 0) {
            if(isMob()) {
                int64_t xt = Mth::floor64(x);
                int64_t yt = Mth::floor64(y - 0.2f - heightOffset);
                int64_t zt = Mth::floor64(z);
                int t = level->getTile(xt, yt, zt);
                if (t == 0 && level->getTile(xt, yt-1, zt) == Tile::fence->id) t = level->getTile(xt, yt-1, zt);
                if (t > 0) Tile::tiles[t]->fallOn(level, (int)xt, (int)yt, (int)zt, this, fallDistance);
            }
            causeFallDamage(fallDistance);
            fallDistance = 0;
        }
    } else {
        if (ya < 0) fallDistance -= ya;
    }
}

void Entity::causeFallDamage(float fallDamage2) {}
float Entity::getHeadHeight() { return 0; }

void Entity::moveRelative(float xa, float za, float speed) {
    float dist = sqrt(xa*xa + za*za);
    if (dist < 0.01f) return;
    if (dist < 1) dist = 1;
    dist = speed / dist;
    xa *= dist; za *= dist;
    float sin_ = (float)sin(yRot * Mth::PI / 180);
    float cos_ = (float)cos(yRot * Mth::PI / 180);
    BigWorldCoordinate bxa(xa), bza(za);
BigWorldCoordinate bcos(cos_), bsin(sin_);
setVelocityX(m_bigVx + bxa * bcos - bza * bsin);
setVelocityZ(m_bigVz + bza * bcos + bxa * bsin);
}

void Entity::setLevel(Level* level) { this->level = level; }

void Entity::moveTo(double x, double y, double z, float yRot, float xRot) {
    this->xOld = this->xo = this->x = x;
    this->yOld = this->yo = this->y = y + heightOffset;
    this->zOld = this->zo = this->z = z;
    this->yRot = this->yRotO = yRot;
    this->xRot = this->xRotO = xRot;
    this->setPos(this->x, this->y, this->z);
}

float Entity::distanceTo(Entity* e) {
    double dx = x - e->x, dy = y - e->y, dz = z - e->z;
    return (float)sqrt(dx*dx + dy*dy + dz*dz);
}
float Entity::distanceTo(float x2, float y2, float z2) {
    double dx = x - x2, dy = y - y2, dz = z - z2;
    return (float)sqrt(dx*dx + dy*dy + dz*dz);
}
float Entity::distanceToSqr(float x2, float y2, float z2) {
    double dx = x - x2, dy = y - y2, dz = z - z2;
    return (float)(dx*dx + dy*dy + dz*dz);
}
float Entity::distanceToSqr(Entity* e) {
    double dx = x - e->x, dy = y - e->y, dz = z - e->z;
    return (float)(dx*dx + dy*dy + dz*dz);
}

void Entity::playerTouch(Player* player) {}

void Entity::push(Entity* e) {
    double xa = e->x - x;
    double za = e->z - z;
    double dd = Mth::absMax((float)xa, (float)za);
    if (dd >= 0.01f) {
        dd = sqrt(dd);
        xa /= dd; za /= dd;
        double pow = 1.0 / dd;
        if (pow > 1) pow = 1;
        xa *= pow; za *= pow;
        xa *= 0.05; za *= 0.05;
        xa *= 1 - pushthrough; za *= 1 - pushthrough;
        this->push((float)-xa, 0, (float)-za);
        e->push((float)xa, 0, (float)za);
    }
}
void Entity::push(float xa, float ya, float za){
    BigWorldCoordinate bxa(xa);
    BigWorldCoordinate bya(ya);
    BigWorldCoordinate bza(za);
    setVelocity(m_bigVx + bxa, m_bigVy + bya, m_bigVz + bza);
}
void Entity::markHurt() { hurtMarked = true; }
bool Entity::hurt(Entity* source, int damage) { markHurt(); return false; }
void Entity::reset() { _init(); }
void Entity::_init() {
    xo = xOld = x;
    yo = yOld = y;
    zo = zOld = z;
	// 改：
m_bigVx = BigWorldCoordinate(0.0);
m_bigVy = BigWorldCoordinate(0.0);
m_bigVz = BigWorldCoordinate(0.0);
this->xd = this->yd = this->zd = 0.0;
    xRotO = xRot; yRotO = yRot;
    onFire = 0; removed = false; fallDistance = 0;
}
bool Entity::intersects(float x0, float y0, float z0, float x1, float y1, float z1) {
    return bb.intersects(x0, y0, z0, x1, y1, z1);
}
bool Entity::isPickable() { return false; }
bool Entity::isPushable() { return false; }
bool Entity::isShootable() { return false; }
void Entity::awardKillScore(Entity* victim, int score) {}
bool Entity::shouldRender(Vec3& c) {
    if (invisible) return false;
    double dx = x - c.x, dy = y - c.y, dz = z - c.z;
    double dist = dx*dx + dy*dy + dz*dz;
    return shouldRenderAtSqrDistance((float)dist);
}
bool Entity::shouldRenderAtSqrDistance(float distance) {
    float size = bb.getSize();
    size *= 64.0f * viewScale;
    return distance < size * size;
}
bool Entity::isCreativeModeAllowed() { return false; }
float Entity::getShadowHeightOffs() { return bbHeight / 2; }
bool Entity::isAlive() { return !removed; }
bool Entity::interact(Player* player) { return false; }
void Entity::lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    setPos(x, y, z);
    setRot(yRot, xRot);
}
float Entity::getPickRadius() { return 0.1f; }
void Entity::lerpMotion(double xd, double yd, double zd){
    setVelocity(BigWorldCoordinate(xd), BigWorldCoordinate(yd), BigWorldCoordinate(zd));
}
void Entity::animateHurt() {}
void Entity::setEquippedSlot(int slot, int item, int auxValue) {}
bool Entity::isSneaking() { return false; }
bool Entity::isPlayer() { return false; }
void Entity::lavaHurt() {
    if (!fireImmune) { hurt(NULL, 4); onFire = 30 * SharedConstants::TicksPerSecond; }
}
void Entity::burn(int dmg) { if (!fireImmune) hurt(NULL, dmg); }

bool Entity::save(CompoundTag* entityTag) {
    int id = getEntityTypeId();
    if (removed || id == 0) return false;
    entityTag->putInt("id", id);
    saveWithoutId(entityTag);
    return true;
}

void Entity::saveWithoutId(CompoundTag* entityTag) {
    ListTag* posList = new ListTag();
    posList->addDouble(x);
    posList->addDouble(y);
    posList->addDouble(z);
    entityTag->put("Pos", posList);

    ListTag* motionList = new ListTag();
    motionList->addDouble(xd);
    motionList->addDouble(yd);
    motionList->addDouble(zd);
    entityTag->put("Motion", motionList);

    entityTag->put("Rotation", ListTagFloatAdder(yRot)(xRot).tag);
    entityTag->putFloat("FallDistance", fallDistance);
    entityTag->putShort("Fire", (short)onFire);
    entityTag->putShort("Air", (short)airSupply);
    entityTag->putBoolean("OnGround", onGround);
    addAdditonalSaveData(entityTag);
}

bool Entity::load(CompoundTag* tag) {
    ListTag* pos = tag->getList("Pos");
    ListTag* motion = tag->getList("Motion");
    ListTag* rotation = tag->getList("Rotation");
    setPos(0, 0, 0);

    double lxd = motion->getDouble(0);
double lyd = motion->getDouble(1);
double lzd = motion->getDouble(2);
if(Mth::abs(lxd) > 10.0) lxd = 0;
if(Mth::abs(lyd) > 10.0) lyd = 0;
if(Mth::abs(lzd) > 10.0) lzd = 0;
setVelocity(BigWorldCoordinate(lxd), BigWorldCoordinate(lyd), BigWorldCoordinate(lzd));
	
    double xx = pos->getDouble(0);
    double yy = pos->getDouble(1);
    double zz = pos->getDouble(2);

    xo = xOld = x = xx;
    yo = yOld = y = yy;
    zo = zOld = z = zz;

    yRotO = yRot = fmod(rotation->getFloat(0), 360.0f);
    xRotO = xRot = fmod(rotation->getFloat(1), 360.0f);

    fallDistance = tag->getFloat("FallDistance");
    onFire = tag->getShort("Fire");
    airSupply = tag->getShort("Air");
    onGround = tag->getBoolean("OnGround");

    setPos(x, y, z);
    readAdditionalSaveData(tag);
    return (tag->errorState == 0);
}

ItemEntity* Entity::spawnAtLocation(int resource, int count) {
    return spawnAtLocation(resource, count, 0);
}
ItemEntity* Entity::spawnAtLocation(int resource, int count, float yOffs) {
    return spawnAtLocation(new ItemInstance(resource, count, 0), yOffs);
}
ItemEntity* Entity::spawnAtLocation(ItemInstance* itemInstance, float yOffs) {
    ItemEntity* ie = new ItemEntity(level, (float)x, (float)(y + yOffs), (float)z, *itemInstance);
    delete itemInstance;
    ie->throwTime = 10;
    level->addEntity(ie);
    return ie;
}
bool Entity::isOnFire() { return onFire > 0; }
bool Entity::interactPreventDefault() { return false; }
bool Entity::isItemEntity() { return false; }
bool Entity::isHangingEntity() { return false; }
int Entity::getAuxData() { return 0; }
void Entity::playStepSound(int64_t xt, int yt, int64_t zt, int t) {
    const Tile::SoundType* soundType = Tile::tiles[t]->soundType;
    if (level->getTile(xt, yt+1, zt) == Tile::topSnow->id) {
        soundType = Tile::topSnow->soundType;
        level->playSound(this, soundType->getStepSound(), soundType->getVolume() * 0.25f, soundType->getPitch());
    } else if (!Tile::tiles[t]->material->isLiquid()) {
        level->playSound(this, soundType->getStepSound(), soundType->getVolume() * 0.25f, soundType->getPitch());
    }
}
