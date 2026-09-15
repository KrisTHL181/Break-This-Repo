use glam::Vec3;
use shipyard::{AllStoragesView, IntoIter, NonSendSync, Unique, UniqueView, UniqueViewMut, View};
use kubi_shared::chunk::CHUNK_SIZE;
use crate::{
  camera::Camera,
  prefabs::GpuPrefabs,
  world::{ChunkMeshStorage, ChunkStorage},
};
use super::{camera_uniform::CameraUniformBuffer, depth::DepthTexture, RenderCtx, Renderer};

mod pipeline;
mod vertex;
pub use vertex::ChunkVertex;

#[derive(Unique)]
pub struct WorldRenderState {
  pub pipeline: wgpu::RenderPipeline,
  pub pipeline_trans: wgpu::RenderPipeline,
  pub trans_bundle: Option<wgpu::RenderBundle>,
}

pub fn init_world_render_state(storages: AllStoragesView) {
  let (pipeline, pipeline_trans) = storages.run(pipeline::init_world_pipeline);
  storages.add_unique(WorldRenderState {
    pipeline, pipeline_trans,
    trans_bundle: None,
  })
}

pub fn draw_world(
  ctx: &mut RenderCtx,
  mut state: UniqueViewMut<WorldRenderState>,
  renderer: UniqueView<Renderer>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
  depth: UniqueView<DepthTexture>,
  textures: UniqueView<GpuPrefabs>,
  camera: View<Camera>,
  chunks: UniqueView<ChunkStorage>,
  meshes: NonSendSync<UniqueView<ChunkMeshStorage>>,
  //settings: UniqueView<GameSettings>,
) {
  let camera = camera.iter().next().expect("No cameras in the scene");

  let mut render_pass = ctx.encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
    label: Some("rpass_draw_world"),
    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
      view: ctx.surface_view,
      resolve_target: None,
      ops: wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      },
    })],
    depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
      view: &depth.depth_view,
      depth_ops: Some(wgpu::Operations {
        load: wgpu::LoadOp::Clear(1.0),
        store: wgpu::StoreOp::Store,
      }),
      stencil_ops: None,
    }),
    ..Default::default()
  });

  render_pass.set_pipeline(&state.pipeline);
  render_pass.set_bind_group(0, &textures.block_diffuse_bind_group, &[]);
  render_pass.set_bind_group(1, &camera_ubo.camera_bind_group, &[]);

  let mut trans_bundle_used = false;
  let mut trans_bundle = renderer.device().create_render_bundle_encoder(&wgpu::RenderBundleEncoderDescriptor {
    label: Some("trans_bundle_encoder"),
    color_formats: &[Some(renderer.surface_config().format)],
    depth_stencil: Some(wgpu::RenderBundleDepthStencil {
      format: depth.depth_texture.format(),
      depth_read_only: true,
      stencil_read_only: true,
    }),
    sample_count: 1,
    multiview: None,
  });

  trans_bundle.set_pipeline(&state.pipeline_trans);
  trans_bundle.set_bind_group(0, &textures.block_diffuse_bind_group, &[]);
  trans_bundle.set_bind_group(1, &camera_ubo.camera_bind_group, &[]);

  for (&position, chunk) in &chunks.chunks {
    if let Some(key) = chunk.mesh_index {
      let mesh = meshes.get(key).expect("Mesh index pointing to nothing");
      let world_position = position.as_vec3() * CHUNK_SIZE as f32;

      //Skip if mesh is empty
      if mesh.main.index.size() == 0 && mesh.trans.index.size() == 0 {
        continue
      }

      //Frustum culling
      let minp = world_position;
      let maxp = world_position + Vec3::splat(CHUNK_SIZE as f32);
      if !camera.frustum.is_box_visible(minp, maxp) {
        continue
      }

      //Draw chunk mesh
      if mesh.main.index_len > 0 {
        render_pass.set_index_buffer(mesh.main.index.slice(..), wgpu::IndexFormat::Uint32);
        render_pass.set_vertex_buffer(0, mesh.main.vertex.slice(..));
        render_pass.draw_indexed(0..mesh.main.index_len, 0, 0..1);
      }

      //Draw transparent chunk mesh
      if mesh.trans.index_len > 0 {
        trans_bundle_used = true;
        trans_bundle.set_index_buffer(mesh.trans.index.slice(..), wgpu::IndexFormat::Uint32);
        trans_bundle.set_vertex_buffer(0, mesh.trans.vertex.slice(..));
        trans_bundle.draw_indexed(0..mesh.trans.index_len, 0, 0..1);
      }
    }
  }

  drop(render_pass);

  if trans_bundle_used {
    let bundle = trans_bundle.finish(&wgpu::RenderBundleDescriptor {
      label: Some("trans_bundle"),
    });
    state.trans_bundle = Some(bundle);
  } else {
    state.trans_bundle = None;
  }
}

pub fn rpass_submit_trans_bundle(
  ctx: &mut RenderCtx,
  state: UniqueView<WorldRenderState>,
  depth: UniqueView<DepthTexture>,
) {
  let Some(bundle) = state.trans_bundle.as_ref() else {
    return
  };
  let mut rpass = ctx.encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
    label: Some("rpass_submit_trans_bundle"),
    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
      view: ctx.surface_view,
      resolve_target: None,
      ops: wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      },
    })],
    depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
      view: &depth.depth_view,
      depth_ops: Some(wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      }),
      stencil_ops: None,
    }),
    ..Default::default()
  });
  rpass.execute_bundles(Some(bundle));
}
