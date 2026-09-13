#ifndef NET_MINECRAFT_WORLD_LEVEL_STORAGE__RegionFile_H__
#define NET_MINECRAFT_WORLD_LEVEL_STORAGE__RegionFile_H__

#include <map>
#include <string>
#include <cstdint>
#include <cstdio>
#include "../../../raknet/BitStream.h"

class RegionFile {
public:
    static const int CHUNKS_PER_REGION = 32;   // 每个区域 32×32 区块
    static const int SECTOR_SIZE = 4096;       // 扇区大小 4KB

    RegionFile(const std::string& filePath);
    ~RegionFile();

    bool open();
    void close();

    bool readChunk(int localX, int localZ, RakNet::BitStream** outData);
    bool writeChunk(int localX, int localZ, RakNet::BitStream& data);

private:
    struct ChunkInfo {
        uint32_t sectorStart;   // 起始扇区号（0 表示未使用）
        uint32_t sizeBytes;     // 数据字节数（不含长度头）
    };

    bool loadOffsetTable();
    bool saveOffsetTable();
    uint32_t allocateSectors(uint32_t sectorsNeeded);
    void freeSectors(uint32_t startSector, uint32_t sectorCount);

    std::string m_filePath;
    FILE* m_file;
    ChunkInfo m_chunks[CHUNKS_PER_REGION][CHUNKS_PER_REGION];
    std::map<uint32_t, bool> m_freeSectors; // true = 空闲
};

#endif
