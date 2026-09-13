#ifndef NET_MINECRAFT_WORLD_ENTITY__Entity_H__
#define NET_MINECRAFT_WORLD_ENTITY__Entity_H__

#include <cstdint>

class Level;
class Player;
class EntityPos;
class Material;
class ItemEntity;
class ItemInstance;
class CompoundTag;

#include "EntityRendererId.h"
#include "../phys/AABB.h"
#include "../../SharedConstants.h"
#include "../../util/Mth.h"
#include "../../util/Random.h"
// 在现有 include 之后添加：
#include "../../util/WorldCoordinate.h"

class SynchedEntityData;

class Entity
{
public:
    static int entityCounter;
    static const int TOTAL_AIR_SUPPLY = 15 * SharedConstants::TicksPerSecond;

    Entity(Level* level);
    virtual ~Entity();

    void _init();
    virtual void reset();

    int hashCode();
    bool operator==(Entity& rhs);

    virtual void setLevel(Level* level);
    
    virtual void remove();

    virtual void setPos(double x, double y, double z);
    virtual void move(double xa, double ya, double za);
    virtual void moveTo(double x, double y, double z, float yRot, float xRot);
    virtual void lerpTo(double x, double y, double z, float yRot, float xRot, int steps);
    virtual void lerpMotion(double xd, double yd, double zd);
    virtual void moveRelative(float xa, float za, float speed);

    virtual void turn(float xo, float yo);
    virtual void interpolateTurn(float xo, float yo);

    virtual void tick();
    virtual void baseTick();

    virtual bool intersects(float x0, float y0, float z0, float x1, float y1, float z1);
    virtual bool isFree(float xa, float ya, float za, float grow);
    virtual bool isFree(float xa, float ya, float za);
    virtual bool isInWall();
    virtual bool isInWater();
    virtual bool isInLava();
    virtual bool isUnderLiquid(const Material* material);

    virtual void makeStuckInWeb();

    virtual float getHeadHeight();
    virtual float getShadowHeightOffs();

    virtual float getBrightness(float a);

    float distanceTo(Entity* e);
    float distanceTo(float x2, float y2, float z2);
    float distanceToSqr(float x2, float y2, float z2);
    float distanceToSqr(Entity* e);

    virtual bool interactPreventDefault();
    virtual bool interact(Player* player);
    virtual void playerTouch(Player* player);

    virtual void push(Entity* e);
    virtual void push(float xa, float ya, float za);
    
    virtual bool isPickable();
    virtual bool isPushable();
    virtual bool isShootable();

    virtual bool isSneaking();

    virtual bool isAlive();
    virtual bool isOnFire();

    virtual bool isPlayer();
    virtual bool isCreativeModeAllowed();

    virtual bool shouldRender(Vec3& c);
    virtual bool shouldRenderAtSqrDistance(float distance);

    virtual bool hurt(Entity* source, int damage);
    virtual void animateHurt();

    virtual void handleEntityEvent(char eventId) {}

    virtual float getPickRadius();

    virtual ItemEntity* spawnAtLocation(int resource, int count);
    virtual ItemEntity* spawnAtLocation(int resource, int count, float yOffs);
    virtual ItemEntity* spawnAtLocation(ItemInstance* itemInstance, float yOffs);

    virtual void awardKillScore(Entity* victim, int score);

    virtual void setEquippedSlot(int slot, int item, int auxValue);

    virtual bool save(CompoundTag* entityTag);
    virtual void saveWithoutId(CompoundTag* entityTag);
    virtual bool load(CompoundTag* entityTag);
    virtual SynchedEntityData* getEntityData();
    virtual const SynchedEntityData* getEntityData() const;

    __inline bool isEntityType(int type) { return getEntityTypeId() == type; }
    virtual int getEntityTypeId() const = 0;
    virtual int getCreatureBaseType() const { return 0; }
    virtual EntityRendererId queryEntityRenderer() { return ER_DEFAULT_RENDERER; }

    virtual bool isMob() { return false; }
    virtual bool isItemEntity();
    virtual bool isHangingEntity();

    virtual int getAuxData();
    virtual void storeAbsolutePosition(const BigWorldCoordinate& bx, 
                                    const BigWorldCoordinate& by, 
                                    const BigWorldCoordinate& bz) {
    this->x = bx.convert_to<double>();
    this->y = by.convert_to<double>();
    this->z = bz.convert_to<double>();
}
virtual BigWorldCoordinate getBigAbsX() const { return BigWorldCoordinate(this->x); }
virtual BigWorldCoordinate getBigAbsY() const { return BigWorldCoordinate(this->y); }
virtual BigWorldCoordinate getBigAbsZ() const { return BigWorldCoordinate(this->z); }
// ====== Big 速度 (真无限精度) ======
    BigWorldCoordinate m_bigVx{0};
    BigWorldCoordinate m_bigVy{0};
    BigWorldCoordinate m_bigVz{0};

    /// 统一的速度设置入口 — 所有改 velocity 的地方都应走此函数
    virtual void setVelocity(const BigWorldCoordinate& vx,
                             const BigWorldCoordinate& vy,
                             const BigWorldCoordinate& vz) {
        m_bigVx = vx;
        m_bigVy = vy;
        m_bigVz = vz;
        this->xd = vx.convert_to<double>();
        this->yd = vy.convert_to<double>();
        this->zd = vz.convert_to<double>();
    }

    /// 设置 X 分速度 (链式清零常用)
    virtual void setVelocityX(const BigWorldCoordinate& vx) {
        m_bigVx = vx;
        this->xd = vx.convert_to<double>();
    }

    /// 设置 Y 分速度
    virtual void setVelocityY(const BigWorldCoordinate& vy) {
        m_bigVy = vy;
        this->yd = vy.convert_to<double>();
    }

    /// 设置 Z 分速度
    virtual void setVelocityZ(const BigWorldCoordinate& vz) {
        m_bigVz = vz;
        this->zd = vz.convert_to<double>();
    }

    /// 乘以摩擦系数 (Big 精确)
    void scaleVelocity(double friction) {
        BigWorldCoordinate bf(friction);
        setVelocity(m_bigVx * bf, m_bigVy * bf, m_bigVz * bf);
    }
    virtual bool isLocalPlayer() const { return false; }

protected:
    virtual void setRot(float yRot, float xRot);
    virtual void setSize(float w, float h);
    virtual void setPos(EntityPos* pos);
    virtual void resetPos(bool clearMore);
    virtual void outOfWorld();

    virtual void checkFallDamage(float ya, bool onGround);
    virtual void causeFallDamage(float fallDamage2);
    virtual void markHurt();

    virtual void burn(int dmg);
    virtual void lavaHurt();

    virtual void readAdditionalSaveData(CompoundTag* tag) = 0;
    virtual void addAdditonalSaveData(CompoundTag* tag) = 0;

    virtual void playStepSound(int64_t xt, int yt, int64_t zt, int t);

public:
    double x, y, z;

    int64_t xChunk, yChunk, zChunk;   // 区块索引（单位：区块），改为 int64_t

    int entityId;

    float viewScale;

    Level* level;
    double xo, yo, zo;
    double xd, yd, zd;
    float yRot, xRot;
    float yRotO, xRotO;

    AABB bb;

    float heightOffset;

    float bbWidth;
    float bbHeight;

    float walkDistO;
    float walkDist;

    double xOld, yOld, zOld;
    float ySlideOffset;
    float footSize;
    float pushthrough;

    int tickCount;
    int invulnerableTime;
    int airSupply;
    int onFire;
    int flameTime;

    EntityRendererId entityRendererId;

    float fallDistance;
    bool blocksBuilding;
    bool inChunk;

    bool onGround;
    bool horizontalCollision, verticalCollision;
    bool collision;
    bool hurtMarked;

    bool slide;
    bool removed;
    bool noPhysics;
    bool canRemove;
    bool invisible;
    bool reallyRemoveIfPlayer;

protected:
    virtual void updatePositionFromBB();

    static Random sharedRandom;
virtual double getLocalFrameOriginX() const { return 0.0; }
    virtual double getLocalFrameOriginY() const { return 0.0; }
    virtual double getLocalFrameOriginZ() const { return 0.0; }

/// Big 格式的 local frame origin — LocalPlayer 覆写返回 m_origin 的 Big 值
virtual BigWorldCoordinate getLocalFrameOriginBigX() const { return BigWorldCoordinate(0.0); }
virtual BigWorldCoordinate getLocalFrameOriginBigY() const { return BigWorldCoordinate(0.0); }
virtual BigWorldCoordinate getLocalFrameOriginBigZ() const { return BigWorldCoordinate(0.0); }

    int airCapacity;
    bool makeStepSound;
    bool wasInWater;
    bool fireImmune;
    static const int DATA_AIR_SUPPLY_ID = 1;

private:
    bool firstTick;
    int nextStep;
    
    bool isStuckInWeb;
};

#endif
