#ifndef _MINECRAFT_NETWORK_CLIENTSIDENETWORKHANDLER_H_
#define _MINECRAFT_NETWORK_CLIENTSIDENETWORKHANDLER_H_

#include "NetEventCallback.h"
#include "../raknet/RakNetTypes.h"
#include "../world/level/LevelConstants.h"

#include <vector>
#include <map>          // 替换 unordered_map
#include <queue>

class Minecraft;
class Level;
class IRakNetInstance;

struct SBufferedBlockUpdate
{
    int x, z;
    unsigned char y;
    unsigned char blockId;
    unsigned char blockData;
    bool setData;
};
typedef std::vector<SBufferedBlockUpdate> BlockUpdateList;

class ClientSideNetworkHandler : public NetEventCallback
{
public:
    ClientSideNetworkHandler(Minecraft* minecraft, IRakNetInstance* raknetInstance);
    virtual ~ClientSideNetworkHandler();

    virtual void levelGenerated(Level* level);

    virtual void onConnect(const RakNet::RakNetGUID& hostGuid);
    virtual void onUnableToConnect();
    virtual void onDisconnect(const RakNet::RakNetGUID& guid);

    // ... 所有 handle 声明保持不变 ...
    virtual void handle(const RakNet::RakNetGUID& source, LoginStatusPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, StartGamePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, MessagePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SetTimePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AddItemEntityPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AddPaintingPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, TakeItemEntityPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AddEntityPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AddMobPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AddPlayerPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, RemoveEntityPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, RemovePlayerPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, MovePlayerPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, MoveEntityPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, UpdateBlockPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ExplodePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, LevelEventPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, TileEventPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, EntityEventPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ChunkDataPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, PlayerEquipmentPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, PlayerArmorEquipmentPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, InteractPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SetEntityDataPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SetEntityMotionPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SetHealthPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SetSpawnPositionPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AnimatePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, UseItemPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, HurtArmorPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, RespawnPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ContainerOpenPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ContainerClosePacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ContainerSetContentPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ContainerSetSlotPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ContainerSetDataPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, ChatPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, AdventureSettingsPacket* packet);
    virtual void handle(const RakNet::RakNetGUID& source, SignUpdatePacket* packet);

private:
    void requestNextChunk();
    void arrangeRequestChunkOrder();
    bool isChunkLoaded(int x, int z);
    void clearChunksLoaded();

    Minecraft* minecraft;
    Level* level;
    IRakNetInstance* raknetInstance;
    RakNet::RakPeerInterface* rakPeer;
    RakNet::RakNetGUID serverGuid;

    BlockUpdateList bufferedBlockUpdates;

    // 动态区块管理（使用 map 避免哈希问题）
    std::map<std::pair<int,int>, bool> loadedChunks;   // 已加载区块
    std::queue<std::pair<int,int>> pendingChunks;      // 待请求的区块队列
    bool initialChunksLoaded;          // 是否已发送 ReadyPacket
    int totalInitialChunks;            // 初始请求的区块总数
    int loadedInitialChunks;           // 已加载的初始区块数
};

#endif
