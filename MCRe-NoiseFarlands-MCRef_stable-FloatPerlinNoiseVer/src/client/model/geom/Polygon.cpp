#include "Polygon.h"
#include "../../renderer/Tesselator.h"
#include "../../../world/phys/Vec3.h"

PolygonQuad::PolygonQuad(VertexPT* v0, VertexPT* v1, VertexPT* v2, VertexPT* v3)
:	_flipNormal(false)
{
	vertices[0] = *v0;
	vertices[1] = *v1;
	vertices[2] = *v2;
	vertices[3] = *v3;
}

PolygonQuad::PolygonQuad(	VertexPT* v0, VertexPT* v1, VertexPT* v2, VertexPT* v3,
				int uu0, int vv0, int uu1, int vv1, float texW, float texH)
: 	_flipNormal(false)
{
	const float us = -0.002f / texW;
	const float vs = -0.002f / texH;
	vertices[0] = v0->remap(uu1 / texW - us, vv0 / texH + vs);
	vertices[1] = v1->remap(uu0 / texW + us, vv0 / texH + vs);
	vertices[2] = v2->remap(uu0 / texW + us, vv1 / texH - vs);
	vertices[3] = v3->remap(uu1 / texW - us, vv1 / texH - vs);
}

PolygonQuad::PolygonQuad(	VertexPT* v0, VertexPT* v1, VertexPT* v2, VertexPT* v3,
					float uu0, float vv0, float uu1, float vv1)
:	_flipNormal(false)
{
	vertices[0] = v0->remap(uu1, vv0);
	vertices[1] = v1->remap(uu0, vv0);
	vertices[2] = v2->remap(uu0, vv1);
	vertices[3] = v3->remap(uu1, vv1);
}

void PolygonQuad::mirror() {
	for (int i = 0; i < VERTEX_COUNT / 2; ++i) {
		const int j = VERTEX_COUNT - i - 1;
		VertexPT tmp = vertices[i];
		vertices[i] = vertices[j];
		vertices[j] = tmp;
	}
}

void PolygonQuad::render(Tesselator& t, float scale, int vboId /* = -1 */) {
	for (int i = 0; i < 4; i++) {
		VertexPT& v = vertices[i];
		t.vertexUV(v.pos.x * scale, v.pos.y * scale, v.pos.z * scale, v.u, v.v);
	}
}

PolygonQuad* PolygonQuad::flipNormal() {
	_flipNormal = true;
	return this;
}
