#include "RenderList.h"
#include "gles.h"
#include "RenderChunk.h"
#include "Tesselator.h"

RenderList::RenderList()
    : inited(false), rendered(false), m_useRelativeTranslation(false),
      m_camX(0.0), m_camY(0.0), m_camZ(0.0)
{
    lists = new int[MAX_NUM_OBJECTS];
    rlists = new RenderChunk[MAX_NUM_OBJECTS];
    for (int i = 0; i < MAX_NUM_OBJECTS; ++i)
        rlists[i].vboId = -1;
}

RenderList::~RenderList() {
    delete[] lists;
    delete[] rlists;
}

void RenderList::init(double xOff, double yOff, double zOff) {
    inited = true;
    listIndex = 0;
    this->xOff = xOff;          // xOff 改为 double 成员（原本是 float？检查一下：RenderList 有 float xOff, yOff, zOff 成员。需要同步改为 double）
    this->yOff = yOff;
    this->zOff = zOff;
    m_camX = xOff;
    m_camY = yOff;
    m_camZ = zOff;
}

void RenderList::add(int list) {
    lists[listIndex] = list;
    if (listIndex == MAX_NUM_OBJECTS) render();
}

void RenderList::addR(const RenderChunk& chunk) {
    rlists[listIndex] = chunk;
}

void RenderList::render() {
    if (!inited) return;
    if (!rendered) {
        bufferLimit = listIndex;
        listIndex = 0;
        rendered = true;
    }
    if (listIndex < bufferLimit) {
        glPushMatrix2();
        if (!m_useRelativeTranslation) {
            glTranslatef2((float)-xOff, (float)-yOff, (float)-zOff);
        }
#ifndef USE_VBO
        glCallLists(bufferLimit, GL_UNSIGNED_INT, lists);
#else
        renderChunks();
#endif
        glPopMatrix2();
    }
}

void RenderList::renderChunks() {
    glEnableClientState2(GL_VERTEX_ARRAY);
    glEnableClientState2(GL_COLOR_ARRAY);
    glEnableClientState2(GL_TEXTURE_COORD_ARRAY);

    const int Stride = VertexSizeBytes;

    for (int i = 0; i < bufferLimit; ++i) {
        RenderChunk& rc = rlists[i];

        glPushMatrix2();

        if (m_useRelativeTranslation) {
            // 相对平移：区块世界坐标减去相机位置
            double transX = (double)rc.baseX - m_camX;
            double transY = (double)rc.baseY - m_camY;
            double transZ = (double)rc.baseZ - m_camZ;
            glTranslatef2((float)transX, (float)transY, (float)transZ);
        } else {
            // 传统模式：直接平移区块到世界位置（顶点是局部坐标）
            glTranslatef2(rc.pos.x, rc.pos.y, rc.pos.z);
        }

        glBindBuffer2(GL_ARRAY_BUFFER, rc.vboId);
        glVertexPointer2(3, GL_FLOAT, Stride, 0);
        glTexCoordPointer2(2, GL_FLOAT, Stride, (GLvoid*)(3 * 4));
        glColorPointer2(4, GL_UNSIGNED_BYTE, Stride, (GLvoid*)(5 * 4));
        glDrawArrays2(GL_TRIANGLES, 0, rc.vertexCount);

        glPopMatrix2();
    }

    glDisableClientState2(GL_VERTEX_ARRAY);
    glDisableClientState2(GL_COLOR_ARRAY);
    glDisableClientState2(GL_TEXTURE_COORD_ARRAY);
}

void RenderList::clear() {
    inited = false;
    rendered = false;
}
