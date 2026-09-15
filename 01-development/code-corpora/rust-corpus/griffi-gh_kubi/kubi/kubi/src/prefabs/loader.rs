use glam::UVec2;
use strum::IntoEnumIterator;
use rayon::prelude::*;
use wgpu::util::{DeviceExt, TextureDataOrder};
use std::{io::BufReader, path::{Path, PathBuf}};
use crate::{filesystem::AssetManager, prefabs::ModelVertex, rendering::{BufferPair, Renderer}};
use super::AssetPaths;

pub fn load_texture2darray_prefab<T: AssetPaths + IntoEnumIterator>(
  renderer: &Renderer,
  assman: &AssetManager,
  directory: PathBuf,
) -> wgpu::Texture {
  log::info!("started loading {}", directory.as_os_str().to_str().unwrap());

  //Load raw images
  let tex_files: Vec<&'static str> = T::iter().map(|x| x.file_name()).collect();
  let raw_images: Vec<(Vec<u8>, UVec2)> = tex_files.par_iter().map(|&file_name| {
    log::info!("loading texture {}", file_name);

    //Get path to the image and open the file
    let reader = {
      let path = directory.join(file_name);
      BufReader::new(assman.open_asset(&path).expect("Failed to open texture file"))
    };

    //Parse image data
    let (image_data, dimensions) = {
      let image = image::load(
        reader,
        image::ImageFormat::Png
      ).unwrap().to_rgba8();
      let dimensions = image.dimensions();
      (image.into_raw(), dimensions)
    };
    (image_data, UVec2::from(dimensions))
  }).collect();

  assert!(!raw_images.is_empty(), "no images loaded");
  //TODO: check same size

  log::info!("done loading texture files, uploading to the gpu");

  let size = raw_images[0].1;
  let layers = raw_images.len() as u32;

  //Concat data into a single vec
  let mut data = Vec::with_capacity((size.x * size.y * layers * 4) as usize);
  for (layer_data, _) in raw_images {
    data.extend_from_slice(&layer_data);
  }

  //Upload images to the GPU
  let label = format!("texture2darray_prefab_{}", directory.as_os_str().to_str().unwrap());
  let desc = &wgpu::TextureDescriptor {
    label: Some(&label),
    size: wgpu::Extent3d {
      width: size.x,
      height: size.y,
      depth_or_array_layers: layers,
    },
    dimension: wgpu::TextureDimension::D2,
    format: wgpu::TextureFormat::Rgba8UnormSrgb,
    usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
    mip_level_count: 1,
    sample_count: 1,
    view_formats: &[],
  };

  renderer.device().create_texture_with_data(
    renderer.queue(),
    desc,
    TextureDataOrder::MipMajor,
    &data
  )
}

pub fn load_texture2d_prefab(
  renderer: &Renderer,
  assman: &AssetManager,
  path: &Path,
) -> wgpu::Texture {
  log::info!("loading texture2d: {path:?}");

  let image = image::load(
    BufReader::new(assman.open_asset(path).expect("Failed to open texture file")),
    image::ImageFormat::Png
  ).unwrap().to_rgba8();
  let size = image.dimensions();
  let data = image.into_raw();

  let label = format!("texture2d_prefab_{}", path.file_name().unwrap().to_str().unwrap());
  let desc = wgpu::TextureDescriptor {
    label: Some(&label),
    size: wgpu::Extent3d {
      width: size.0,
      height: size.1,
      depth_or_array_layers: 1,
    },
    dimension: wgpu::TextureDimension::D2,
    format: wgpu::TextureFormat::Rgba8UnormSrgb,
    usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
    mip_level_count: 1,
    sample_count: 1,
    view_formats: &[],
  };

  renderer.device().create_texture_with_data(
    renderer.queue(),
    &desc,
    TextureDataOrder::MipMajor,
    &data
  )
}

pub fn load_obj_prefab(
  renderer: &Renderer,
  assman: &AssetManager,
  path: &Path,
) -> BufferPair {
  log::info!("loading obj prefab: {path:?}");

  let mut reader = BufReader::new(
    assman.open_asset(path).expect("Failed to open texture file")
  );

  let (model, _) = tobj::load_obj_buf(
    &mut reader,
    &tobj::GPU_LOAD_OPTIONS,
    |_| unimplemented!()
  ).unwrap();

  assert_eq!(model.len(), 1, "only single model supported at the moment, sowwy :3");
  let mesh = &model[0].mesh;
  debug_assert!(mesh.normal_indices.is_empty() && mesh.texcoord_indices.is_empty(), "forgor single_index");

  let tex_coords = bytemuck::cast_slice::<f32, [f32; 2]>(&mesh.texcoords);
  let positions = bytemuck::cast_slice::<f32, [f32; 3]>(&mesh.positions);
  let normals = bytemuck::cast_slice::<f32, [f32; 3]>(&mesh.normals);

  let vertex_buffer: Vec<_> = (0..positions.len()).map(|i| {
    ModelVertex {
      tex_coords: [tex_coords[i][0], 1. - tex_coords[i][1]],
      position: positions[i],
      _padding: 0,
      normal: normals[i],
    }
  }).collect();

  let vertex_buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
    label: Some("obj_vertex_buffer"),
    contents: bytemuck::cast_slice(&vertex_buffer),
    usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::VERTEX,
  });

  let index_buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
    label: Some("obj_index_buffer"),
    contents: bytemuck::cast_slice(&mesh.indices),
    usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::INDEX,
  });

  BufferPair {
    vertex: vertex_buffer,
    vertex_len: positions.len() as u32,
    index: index_buffer,
    index_len: mesh.indices.len() as u32,
  }
}
