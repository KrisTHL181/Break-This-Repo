#include "RegionFile.h"
#include "../../../platform/log.h"
#include <cstring>
#include <cerrno>

RegionFile::RegionFile(const std::string& filePath)
    : m_filePath(filePath), m_file(nullptr) {
    memset(m_chunks, 0, sizeof(m_chunks));
}

RegionFile::~RegionFile() {
    close();
}

bool RegionFile::open() {
    close();
    m_file = fopen(m_filePath.c_str(), "r+b");
    if (!m_file) {
        // 文件不存在，创建新文件
        m_file = fopen(m_filePath.c_str(), "w+b");
        if (!m_file) {
            LOGE("Failed to create region file: %s", m_filePath.c_str());
            return false;
        }
        // 初始化偏移表（全0）
        memset(m_chunks, 0, sizeof(m_chunks));
        saveOffsetTable();
        // 扇区 0 和 1 被偏移表占用（32×32×4 = 4096 字节，正好 1 扇区，但为对齐保留 2 扇区）
        m_freeSectors[0] = false;
        m_freeSectors[1] = false;
        return true;
    }

    // 读取已有文件
    if (!loadOffsetTable()) {
        LOGE("Failed to read offset table from %s", m_filePath.c_str());
        return false;
    }

    // 扫描所有已用扇区，构建空闲扇区表
    fseek(m_file, 0, SEEK_END);
    long fileSize = ftell(m_file);
    uint32_t totalSectors = (fileSize + SECTOR_SIZE - 1) / SECTOR_SIZE;
    for (uint32_t i = 0; i < totalSectors; i++) {
        m_freeSectors[i] = true;
    }
    // 偏移表占用的扇区
    m_freeSectors[0] = false;
    m_freeSectors[1] = false;
    // 遍历所有区块，标记已用扇区
    for (int x = 0; x < CHUNKS_PER_REGION; x++) {
        for (int z = 0; z < CHUNKS_PER_REGION; z++) {
            ChunkInfo& info = m_chunks[x][z];
            if (info.sectorStart == 0) continue;
            uint32_t sectorCount = (info.sizeBytes + sizeof(uint32_t) + SECTOR_SIZE - 1) / SECTOR_SIZE;
            for (uint32_t i = 0; i < sectorCount; i++) {
                m_freeSectors[info.sectorStart + i] = false;
            }
        }
    }
    return true;
}

void RegionFile::close() {
    if (m_file) {
        fclose(m_file);
        m_file = nullptr;
    }
}

bool RegionFile::loadOffsetTable() {
    fseek(m_file, 0, SEEK_SET);
    uint32_t table[CHUNKS_PER_REGION * CHUNKS_PER_REGION];
    if (fread(table, sizeof(uint32_t), CHUNKS_PER_REGION * CHUNKS_PER_REGION, m_file) != CHUNKS_PER_REGION * CHUNKS_PER_REGION) {
        return false;
    }
    for (int x = 0; x < CHUNKS_PER_REGION; x++) {
        for (int z = 0; z < CHUNKS_PER_REGION; z++) {
            uint32_t entry = table[x + z * CHUNKS_PER_REGION];
            m_chunks[x][z].sectorStart = entry >> 8;
            m_chunks[x][z].sizeBytes = 0; // 大小在读取数据时更新
        }
    }
    return true;
}

bool RegionFile::saveOffsetTable() {
    fseek(m_file, 0, SEEK_SET);
    uint32_t table[CHUNKS_PER_REGION * CHUNKS_PER_REGION];
    memset(table, 0, sizeof(table));
    for (int x = 0; x < CHUNKS_PER_REGION; x++) {
        for (int z = 0; z < CHUNKS_PER_REGION; z++) {
            ChunkInfo& info = m_chunks[x][z];
            if (info.sectorStart == 0) continue;
            uint32_t sectorCount = (info.sizeBytes + sizeof(uint32_t) + SECTOR_SIZE - 1) / SECTOR_SIZE;
            table[x + z * CHUNKS_PER_REGION] = (info.sectorStart << 8) | (sectorCount & 0xFF);
        }
    }
    fseek(m_file, 0, SEEK_SET);
    return fwrite(table, sizeof(uint32_t), CHUNKS_PER_REGION * CHUNKS_PER_REGION, m_file) == CHUNKS_PER_REGION * CHUNKS_PER_REGION;
}

bool RegionFile::readChunk(int localX, int localZ, RakNet::BitStream** outData) {
    if (localX < 0 || localX >= CHUNKS_PER_REGION || localZ < 0 || localZ >= CHUNKS_PER_REGION) {
        return false;
    }
    ChunkInfo& info = m_chunks[localX][localZ];
    if (info.sectorStart == 0) {
        return false; // 区块不存在
    }

    fseek(m_file, info.sectorStart * SECTOR_SIZE, SEEK_SET);
    uint32_t totalSize;
    if (fread(&totalSize, sizeof(uint32_t), 1, m_file) != 1) {
        return false;
    }
    if (totalSize < sizeof(uint32_t) || totalSize > SECTOR_SIZE * 255) {
        LOGE("Invalid chunk size: %u", totalSize);
        return false;
    }
    uint32_t dataSize = totalSize - sizeof(uint32_t);
    unsigned char* buffer = new unsigned char[dataSize];
    if (fread(buffer, 1, dataSize, m_file) != dataSize) {
        delete[] buffer;
        return false;
    }
    *outData = new RakNet::BitStream(buffer, dataSize, false);
    info.sizeBytes = dataSize; // 更新缓存大小
    return true;
}

bool RegionFile::writeChunk(int localX, int localZ, RakNet::BitStream& data) {
    if (localX < 0 || localX >= CHUNKS_PER_REGION || localZ < 0 || localZ >= CHUNKS_PER_REGION) {
        return false;
    }

    uint32_t dataSize = data.GetNumberOfBytesUsed();
    uint32_t totalSize = dataSize + sizeof(uint32_t);
    uint32_t sectorsNeeded = (totalSize + SECTOR_SIZE - 1) / SECTOR_SIZE;

    ChunkInfo& info = m_chunks[localX][localZ];
    // 如果已有空间，先释放
    if (info.sectorStart != 0) {
        uint32_t oldSectors = (info.sizeBytes + sizeof(uint32_t) + SECTOR_SIZE - 1) / SECTOR_SIZE;
        freeSectors(info.sectorStart, oldSectors);
    }

    // 分配新扇区
    uint32_t newSector = allocateSectors(sectorsNeeded);
    if (newSector == 0) {
        LOGE("Failed to allocate %u sectors for chunk (%d,%d)", sectorsNeeded, localX, localZ);
        return false;
    }

    info.sectorStart = newSector;
    info.sizeBytes = dataSize;

    // 写入数据
    fseek(m_file, newSector * SECTOR_SIZE, SEEK_SET);
    if (fwrite(&totalSize, sizeof(uint32_t), 1, m_file) != 1) {
        return false;
    }
    if (fwrite(data.GetData(), 1, dataSize, m_file) != dataSize) {
        return false;
    }
    // 填充剩余扇区（可选）
    uint32_t written = sizeof(uint32_t) + dataSize;
    uint32_t padding = sectorsNeeded * SECTOR_SIZE - written;
    if (padding > 0) {
        unsigned char zero[4096] = {0};
        fwrite(zero, 1, padding, m_file);
    }
    fflush(m_file);

    // 更新偏移表
    return saveOffsetTable();
}

uint32_t RegionFile::allocateSectors(uint32_t sectorsNeeded) {
    // 寻找连续空闲扇区
    uint32_t start = 2; // 从扇区 2 开始（0,1 被偏移表占用）
    uint32_t consecutive = 0;
    while (true) {
        if (m_freeSectors.find(start + consecutive) == m_freeSectors.end() || m_freeSectors[start + consecutive]) {
            consecutive++;
            if (consecutive == sectorsNeeded) {
                // 找到，标记为已用
                for (uint32_t i = 0; i < sectorsNeeded; i++) {
                    m_freeSectors[start + i] = false;
                }
                return start;
            }
        } else {
            // 不连续，跳到下一个扇区
            start = start + consecutive + 1;
            consecutive = 0;
        }
        // 防止无限循环（实际不会，因为文件可以扩展）
        if (start > 1000000) break;
    }

    // 没有足够连续空间，扩展文件
    fseek(m_file, 0, SEEK_END);
    long fileSize = ftell(m_file);
    start = (fileSize + SECTOR_SIZE - 1) / SECTOR_SIZE;
    for (uint32_t i = 0; i < sectorsNeeded; i++) {
        m_freeSectors[start + i] = false;
    }
    return start;
}

void RegionFile::freeSectors(uint32_t startSector, uint32_t sectorCount) {
    for (uint32_t i = 0; i < sectorCount; i++) {
        m_freeSectors[startSector + i] = true;
    }
}
