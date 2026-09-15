use std::{
    collections::HashMap,
    io::{Read, Seek, SeekFrom},
    string::FromUtf8Error,
};

use bitvec::prelude::*;
use blosc_src::blosc_cbuffer_sizes;
use bytemuck::{bytes_of_mut, cast_slice_mut, Zeroable};
use byteorder::{LittleEndian, ReadBytesExt};
use cgmath::Vector3;
use half::f16;
use log::{trace, warn};

use crate::vdb::{
    transform::Map, ArchiveHeader, Compression, GridDescriptor, InternalData, LeafData, Metadata,
    MetadataValue, Node, NodeHeader, NodeMetaData, Root345, RootData, RootNode, N3, N4, N5, VDB345,
};

use super::VdbValueType;

type Result<T> = std::result::Result<T, ErrorKind>;

const OPENVDB_MIN_SUPPORTED_VERSION: u32 = OPENVDB_FILE_VERSION_BOOST_UUID;

const OPENVDB_FILE_VERSION_PER_GRID_COMPRESSION: u32 = 223;
pub const OPENVDB_FILE_VERSION_SELECTIVE_COMPRESSION: u32 = 220;
pub const OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION: u32 = 222;
pub const OPENVDB_FILE_VERSION_BOOST_UUID: u32 = 218;

#[derive(Debug, thiserror::Error)]
pub enum ErrorKind {
    #[error("Magic bytes mismatched")]
    MagicMismatch,
    #[error("Unsupported VDB file version {0}")]
    UnsupportedVErsion(u32),
    #[error("IoError")]
    IoError(#[from] std::io::Error),
    #[error("Utf8Error")]
    Utf8Error(#[from] FromUtf8Error),
    #[error("Invalid compression {0}")]
    InvalidCompression(u32),
    #[error("Invaild grid name {0}")]
    InvalidGridName(String),
    #[error("Invalid node metadata entry (u8)")]
    InvalidNodeMetadata(u8),
    #[error("Unsupported Blosc format")]
    UnsupportedBloscFormat,
    #[error("Invalid Blsoc data")]
    InvalidBloscData,
    #[error("Unexpected Mask length")]
    UnexpectedMaskLength,
}

pub struct VdbReader<R: Read + Seek> {
    reader: R,
    pub header: ArchiveHeader,
    pub grid_descriptors: HashMap<String, GridDescriptor>,
}

impl<R: Read + Seek> VdbReader<R> {
    pub fn new(mut reader: R) -> Result<Self> {
        let magic = reader.read_u64::<LittleEndian>()?;
        if magic != 0x56444220 {
            return Err(ErrorKind::MagicMismatch);
        }

        let file_version = reader.read_u32::<LittleEndian>()?;
        if file_version < OPENVDB_MIN_SUPPORTED_VERSION {
            return Err(ErrorKind::UnsupportedVErsion(file_version));
        }

        let library_major = reader.read_u32::<LittleEndian>()?;
        let library_minor = reader.read_u32::<LittleEndian>()?;

        let has_grid_offsets = reader.read_u8()? != 0;

        let mut compression = if file_version < OPENVDB_FILE_VERSION_PER_GRID_COMPRESSION {
            // Prior to the introduction of Blosc, ZLIB was the default compression scheme.
            Compression::ZIP | Compression::ACTIVE_MASK
        } else {
            // From version 223 on, compression information is stored per grid
            Compression::DEFAULT_COMPRESSION
        };

        if (OPENVDB_FILE_VERSION_SELECTIVE_COMPRESSION..OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION)
            .contains(&file_version)
        {
            let is_compressed = reader.read_u8()? == 1;
            if is_compressed {
                compression = Compression::ZIP;
            } else {
                compression = Compression::NONE;
            }
        }

        let uuid = read_string(&mut reader, 36)?;

        let meta_data = Self::read_metadata(&mut reader)?;

        let grid_number = reader.read_u32::<LittleEndian>()?;

        let header = ArchiveHeader {
            file_version,
            library_major,
            library_minor,
            uuid,
            has_grid_offsets,
            compression,
            grid_number,
            meta_data,
        };

        let grid_descriptors = Self::read_grid_descriptors(&mut reader, &header)?;

        Ok(Self {
            reader,
            header,
            grid_descriptors,
        })
    }

    pub fn read_vdb345_grid<T: VdbValueType>(&mut self, name: &str) -> Result<VDB345<T>> {
        let grid_descriptor = self.grid_descriptors.get(name).cloned();
        let grid_descriptor =
            grid_descriptor.ok_or_else(|| ErrorKind::InvalidGridName(name.to_owned()))?;
        grid_descriptor.seek_to_grid(&mut self.reader)?;

        if self.header.file_version >= OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION {
            let _: Compression = self.reader.read_u32::<LittleEndian>()?.try_into()?;
        }
        let _ = Self::read_metadata(&mut self.reader)?;

        // @TODO: Make a Grid struct to store the descriptors and the VDB
        let _transform = Self::read_transform(&mut self.reader)?;

        let mut vdb = self.read_tree_topology::<T>(&grid_descriptor)?;
        self.read_tree_data::<T>(&grid_descriptor, &mut vdb)?;

        Ok(vdb)
    }

    fn read_transform(reader: &mut R) -> Result<Map> {
        let transform_name = read_len_string(reader)?;

        Ok(match transform_name.as_str() {
            "UniformScaleMap" => Map::UniformScaleMap {
                scale_values: read_vec3d(reader)?,
                voxel_size: read_vec3d(reader)?,
                scale_values_inverse: read_vec3d(reader)?,
                inv_scale_sqr: read_vec3d(reader)?,
                inv_twice_scale: read_vec3d(reader)?,
            },
            "UniformScaleTranslateMap" | "ScaleTranslateMap" => Map::ScaleTranslateMap {
                translation: read_vec3d(reader)?,
                scale_values: read_vec3d(reader)?,
                voxel_size: read_vec3d(reader)?,
                scale_values_inverse: read_vec3d(reader)?,
                inv_scale_sqr: read_vec3d(reader)?,
                inv_twice_scale: read_vec3d(reader)?,
            },
            _ => panic!("Not suppored transform type {transform_name}"),
        })
    }

    fn read_grid_descriptors(
        reader: &mut R,
        header: &ArchiveHeader,
    ) -> Result<HashMap<String, GridDescriptor>> {
        // Guaranteed by minimum file version
        assert!(header.has_grid_offsets);

        let mut grid_descriptors = HashMap::new();

        for _ in 0..header.grid_number {
            let name = read_len_string(reader)?;
            let grid_type = read_len_string(reader)?;
            let instance_parent = read_len_string(reader)?;

            let grid_pos = reader.read_u64::<LittleEndian>()?;
            let block_pos = reader.read_u64::<LittleEndian>()?;
            let end_pos = reader.read_u64::<LittleEndian>()?;

            let mut grid_descriptor = GridDescriptor {
                name: name.clone(),
                grid_type,
                instance_parent,
                grid_pos,
                block_pos,
                end_pos,
                compression: header.compression,
                meta_data: Default::default(),
            };

            if header.file_version >= OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION {
                grid_descriptor.compression = reader.read_u32::<LittleEndian>()?.try_into()?;
            }

            grid_descriptor.meta_data = Self::read_metadata(reader)?;

            assert!(
                grid_descriptors
                    .insert(name.clone(), grid_descriptor)
                    .is_none(),
                "Gird {name} already exists"
            );

            reader.seek(SeekFrom::Start(end_pos))?;
        }

        Ok(grid_descriptors)
    }

    fn read_metadata(reader: &mut R) -> Result<Metadata> {
        let meta_data_number = reader.read_u32::<LittleEndian>()?;
        let mut meta_data = Metadata::default();

        for _ in 0..meta_data_number {
            let name = read_len_string(reader)?;
            let meta_type = read_len_string(reader)?;

            let meta_len = reader.read_u32::<LittleEndian>()? as usize;
            meta_data.0.insert(
                name,
                match meta_type.as_str() {
                    "string" => MetadataValue::String(read_string(reader, meta_len)?),
                    "bool" => {
                        let val = reader.read_u8()?;
                        MetadataValue::Bool(val == 1)
                    }
                    "int32" => {
                        let val = reader.read_i32::<LittleEndian>()?;
                        MetadataValue::I32(val)
                    }
                    "int64" => {
                        let val = reader.read_i64::<LittleEndian>()?;
                        MetadataValue::I64(val)
                    }
                    "float" => {
                        let val = reader.read_f32::<LittleEndian>()?;
                        MetadataValue::Float(val)
                    }
                    "vec3i" => {
                        let val = read_vec3i(reader)?;
                        MetadataValue::Vec3i(val)
                    }
                    name => {
                        let mut data = vec![0u8; meta_len];
                        reader.read_exact(&mut data)?;

                        warn!("Unknown metadata value {}", name);

                        MetadataValue::Unknown {
                            name: name.to_owned(),
                            data,
                        }
                    }
                },
            );
        }

        trace!("Metadata:");
        for (name, value) in &meta_data.0 {
            trace!("{name} {value:?}");
        }

        Ok(meta_data)
    }

    fn read_tree_topology<T: VdbValueType>(
        &mut self,
        grid_descriptor: &GridDescriptor,
    ) -> Result<VDB345<T>> {
        let buffer_count = self.reader.read_u32::<LittleEndian>()?;
        if buffer_count != 1 {
            todo!("Multi-buffer trees not implemented");
        }

        // @TODO: What is the logic here for half_float? this takes up 4 bytes?
        let rn_bytes = self.reader.read_u32::<LittleEndian>()?;
        let root_node_background = T::from_4_le_bytes(rn_bytes.to_le_bytes());

        let number_of_tiles = self.reader.read_u32::<LittleEndian>()?;
        let number_of_node5s = self.reader.read_u32::<LittleEndian>()?;

        let mut node5_entries = vec![];

        // Iterate Node5 tiles
        for _ in 0..number_of_tiles {
            let origin = read_vec3i(&mut self.reader)?;
            let root_key = <Root345<T>>::root_key_from_coords(origin);

            let value_bytes = self.reader.read_u32::<LittleEndian>()?;
            let value = u32::from_4_le_bytes(value_bytes.to_le_bytes());
            let active = self.reader.read_u8()? == 1;

            let node5_tile = RootData::Tile::<N5<T>>(value, active);
            node5_entries.push((root_key, node5_tile));
        }

        // Iterate Node5 Children
        for _ in 0..number_of_node5s {
            let origin = read_vec3i(&mut self.reader)?;
            let root_key = <Root345<T>>::root_key_from_coords(origin);

            let node_5_header = self.read_internal_node_header::<T, N5<T>>(&grid_descriptor)?;

            let mut node_5 = <N5<T>>::new_from_header(
                try_from_bitvec(node_5_header.child_mask.clone())?,
                try_from_bitvec(node_5_header.value_mask.clone())?,
                origin.into(),
            );

            for idx in node_5_header.child_mask.iter_ones() {
                let node_4_header = self.read_internal_node_header::<T, N4<T>>(&grid_descriptor)?;
                let mut node_4 = <N4<T>>::new_from_header(
                    try_from_bitvec(node_4_header.child_mask.clone())?,
                    try_from_bitvec(node_4_header.value_mask.clone())?,
                    // @TODO: Actually compute coords
                    [0; 3],
                );

                for idx in node_4_header.child_mask.iter_ones() {
                    let mut value_mask = bitvec![u64, Lsb0; 0; <N3<T>>::SIZE];
                    self.reader
                        .read_u64_into::<LittleEndian>(value_mask.as_raw_mut_slice())?;

                    node_4.data[idx] = InternalData::Node(Box::new(<N3<T>>::new_from_header(
                        try_from_bitvec(value_mask)?,
                    )));
                }

                node_5.data[idx] = InternalData::Node(Box::new(node_4));
            }

            let root_data = RootData::Node(Box::new(node_5));
            node5_entries.push((root_key, root_data));
        }

        let root = RootNode {
            map: HashMap::from_iter(node5_entries),
            background: root_node_background,
        };

        Ok(VDB345 {
            root,
            grid_descriptor: grid_descriptor.clone(),
        })
    }

    fn read_internal_node_header<T: VdbValueType, N: Node>(
        &mut self,
        grid_descriptor: &GridDescriptor,
    ) -> Result<NodeHeader<T>> {
        let mut child_mask = bitvec![u64, Lsb0; 0; N::SIZE];
        let mut value_mask = bitvec![u64, Lsb0; 0; N::SIZE];
        self.reader
            .read_u64_into::<LittleEndian>(child_mask.as_raw_mut_slice())?;
        self.reader
            .read_u64_into::<LittleEndian>(value_mask.as_raw_mut_slice())?;

        let size = if self.header.file_version < OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION {
            child_mask.count_zeros()
        } else {
            N::SIZE
        };

        let data = self.read_compressed(grid_descriptor, size, value_mask.as_bitslice())?;

        Ok(NodeHeader {
            child_mask,
            value_mask,
            data,
            log_2_dim: N::LOG2_D,
        })
    }

    fn read_compressed<T: VdbValueType>(
        &mut self,
        grid_descriptor: &GridDescriptor,
        size: usize,
        value_mask: &BitSlice<u64>,
    ) -> Result<Vec<T>> {
        let mut meta_data: NodeMetaData = NodeMetaData::NoMaskAndAllVals;
        if self.header.file_version >= OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION {
            meta_data = self.reader.read_u8()?.try_into()?;
        }

        let mut inactive_val0 = T::zeroed();
        let mut inactive_val1 = T::zeroed();
        match meta_data {
            NodeMetaData::MaskAndOneInactiveVal | NodeMetaData::NoMaskAndOneInactiveVal => {
                self.reader.read_exact(bytes_of_mut(&mut inactive_val0))?;
            }
            NodeMetaData::MaskAndTwoInactiveVals => {
                self.reader.read_exact(bytes_of_mut(&mut inactive_val0))?;
                self.reader.read_exact(bytes_of_mut(&mut inactive_val1))?;
            }
            _ => {}
        }

        let mut selection_mask = bitvec![u64, Lsb0; 0; size];

        if meta_data == NodeMetaData::MaskAndNoInactiveVals
            || meta_data == NodeMetaData::MaskAndOneInactiveVal
            || meta_data == NodeMetaData::MaskAndTwoInactiveVals
        {
            self.reader
                .read_u64_into::<LittleEndian>(selection_mask.as_raw_mut_slice())?;
        }

        let count = if grid_descriptor
            .compression
            .contains(Compression::ACTIVE_MASK)
            && meta_data != NodeMetaData::NoMaskAndAllVals
            && self.header.file_version >= OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION
        {
            value_mask.count_ones()
        } else {
            size
        };

        let expected = std::any::TypeId::of::<T>();
        let f16_type = std::any::TypeId::of::<f16>();
        let f32_type = std::any::TypeId::of::<f32>();

        let real = if grid_descriptor.meta_data.is_half_float() {
            f16_type
        } else {
            f32_type
        };

        let data: Vec<T> = match (expected, real) {
            (e, r) if e == r => self.read_compressed_data(grid_descriptor, count)?,
            (e, r) if e == f16_type && r == f32_type => {
                let data = self.read_compressed_data::<f32>(grid_descriptor, count)?;
                bytemuck::cast_vec(data.into_iter().map(f16::from_f32).collect::<Vec<_>>())
            }
            (e, r) if e == f32_type && r == f16_type => {
                let data = self.read_compressed_data::<f16>(grid_descriptor, count)?;
                bytemuck::cast_vec(data.into_iter().map(f16::to_f32).collect::<Vec<f32>>())
            }
            (_, r) if r == f16_type => {
                let data = self.read_compressed_data::<f16>(grid_descriptor, count)?;
                bytemuck::cast_vec(
                    data.into_iter()
                        .map(|c| T::from_f16_bites(c))
                        .collect::<Vec<_>>(),
                )
            }
            (_, r) if r == f32_type => {
                let data = self.read_compressed_data::<f32>(grid_descriptor, count)?;
                bytemuck::cast_vec(
                    data.into_iter()
                        .map(|c| T::from_f32_bites(c))
                        .collect::<Vec<_>>(),
                )
            }
            _ => unreachable!(),
        };

        Ok(
            if grid_descriptor
                .compression
                .contains(Compression::ACTIVE_MASK)
                && data.len() != size
            {
                trace!("Expanding active maska data {} to {}", data.len(), size);

                let mut expanded = vec![T::zeroed(); size];
                let mut read_idx = 0;
                for dest_idx in 0..size {
                    expanded[dest_idx] = if value_mask[dest_idx] {
                        let v = data[read_idx];
                        read_idx += 1;
                        v
                    } else if selection_mask[dest_idx] {
                        inactive_val1
                    } else {
                        inactive_val0
                    }
                }
                expanded
            } else {
                data
            },
        )
    }

    fn read_compressed_data<T: VdbValueType>(
        &mut self,
        grid_descriptor: &GridDescriptor,
        count: usize,
    ) -> Result<Vec<T>> {
        Ok(match grid_descriptor.compression {
            c if c.contains(Compression::BLOSC) => {
                let num_compressed_bytes = self.reader.read_i64::<LittleEndian>()?;
                let compressed_count = num_compressed_bytes / std::mem::size_of::<T>() as i64;

                trace!("Reading blosc data, {} bytes", num_compressed_bytes);
                if num_compressed_bytes <= 0 {
                    let mut data = vec![T::zeroed(); (-compressed_count) as usize];
                    self.reader.read_exact(cast_slice_mut(&mut data))?;
                    assert_eq!(-compressed_count as usize, count);
                    data
                } else {
                    let mut blosc_data = vec![0u8; num_compressed_bytes as usize];
                    self.reader.read_exact(&mut blosc_data)?;
                    if count > 0 {
                        let mut nbytes: usize = 0;
                        let mut cbytes: usize = 0;
                        let mut blocksize: usize = 0;
                        unsafe {
                            blosc_cbuffer_sizes(
                                blosc_data.as_ptr().cast(),
                                &mut nbytes,
                                &mut cbytes,
                                &mut blocksize,
                            )
                        };
                        if nbytes == 0 {
                            return Err(ErrorKind::UnsupportedBloscFormat);
                        }
                        let dest_size = nbytes / std::mem::size_of::<T>();
                        let mut dest: Vec<T> = vec![Zeroable::zeroed(); dest_size];
                        let error = unsafe {
                            blosc_src::blosc_decompress_ctx(
                                blosc_data.as_ptr().cast(),
                                dest.as_mut_ptr().cast(),
                                nbytes,
                                1,
                            )
                        };
                        if error < 1 {
                            return Err(ErrorKind::InvalidBloscData);
                        }
                        dest
                    } else {
                        trace!(
                            "Skipping blosc decompression because of a {}-count read",
                            count
                        );
                        vec![T::zeroed(); 0]
                    }
                }
            }
            c if c.contains(Compression::ZIP) => {
                let num_zipped_bytes = self.reader.read_i64::<LittleEndian>()?;
                let compressed_count = num_zipped_bytes / std::mem::size_of::<T>() as i64;

                trace!("Reading zipped data, {} bytes", num_zipped_bytes);
                if num_zipped_bytes <= 0 {
                    let mut data = vec![T::zeroed(); (-compressed_count) as usize];
                    self.reader.read_exact(cast_slice_mut(&mut data))?;
                    data
                } else {
                    let mut zipped_data = vec![0u8; num_zipped_bytes as usize];
                    self.reader.read_exact(&mut zipped_data)?;

                    let mut zip_reader = flate2::read::ZlibDecoder::new(zipped_data.as_slice());
                    let mut data = vec![T::zeroed(); count];
                    zip_reader.read_exact(cast_slice_mut(&mut data))?;
                    data
                }
            }
            _ => {
                trace!("Reading uncompressed data, {} elements", count);

                let mut data = vec![T::zeroed(); count];
                self.reader.read_exact(cast_slice_mut(&mut data))?;
                data
            }
        })
    }

    fn read_tree_data<T: VdbValueType>(
        &mut self,
        grid_descriptor: &GridDescriptor,
        vdb: &mut VDB345<T>,
    ) -> Result<()> {
        grid_descriptor.seek_to_blocks(&mut self.reader)?;

        for node_5 in vdb.root.map.values_mut() {
            let RootData::Node(node_5) = node_5 else {
                continue;
            };

            for node_4 in node_5.data.iter_mut() {
                let InternalData::Node(node_4) = node_4 else {
                    continue;
                };

                for node_3 in node_4.data.iter_mut() {
                    let InternalData::Node(node_3) = node_3 else {
                        continue;
                    };

                    let mut value_mask = bitvec![u64, Lsb0; 0; <N3<T>>::SIZE];
                    self.reader
                        .read_u64_into::<LittleEndian>(value_mask.as_raw_mut_slice())?;

                    if self.header.file_version < OPENVDB_FILE_VERSION_NODE_MASK_COMPRESSION {
                        let _origin = read_vec3i(&mut self.reader)?;
                        let num_buffers = self.reader.read_u8()?;
                        assert_eq!(num_buffers, 1);
                    }

                    let data: Vec<T> = self.read_compressed(
                        &grid_descriptor,
                        <N3<T>>::SIZE,
                        value_mask.as_bitslice(),
                    )?;

                    for idx in 0..data.len() {
                        // HACK:
                        // It looks like the initial value mask given in the previous Node4 section is the correct one,
                        // I am not sure that the actual data of the voxel is being read properly (it is probably not)
                        // I think using the previous value mask just gives the correct topology but probably the value of the voxels
                        // is not the one that was intended, I need to reinvestigate how the file encoding is done
                        if (node_3.value_mask[idx >> 6] & (1 << (idx & 63))) != 0 {
                            node_3.data[idx] = LeafData::Value(data[idx]);
                        }
                    }
                }
            }
        }

        Ok(())
    }
}

pub trait From4LeBytes {
    fn from_4_le_bytes(array: [u8; 4]) -> Self;

    fn from_f16_bites(f: f16) -> Self
    where
        Self: Sized,
    {
        let [b1, b0] = f.to_le_bytes();

        Self::from_4_le_bytes([0, 0, b0, b1])
    }

    fn from_f32_bites(f: f32) -> Self
    where
        Self: Sized,
    {
        let bytes = f.to_le_bytes();

        Self::from_4_le_bytes(bytes)
    }
}
fn read_len_string<R: Read + Seek>(reader: &mut R) -> Result<String> {
    let len = reader.read_u32::<LittleEndian>()? as usize;
    read_string(reader, len)
}

fn read_string<R: Read + Seek>(reader: &mut R, len: usize) -> Result<String> {
    let mut buf = vec![0; len];
    reader.read_exact(&mut buf)?;
    let string = String::from_utf8(buf.to_vec())?;
    Ok(string)
}

fn read_vec3i<R: Read + Seek>(reader: &mut R) -> Result<Vector3<i32>> {
    let x = reader.read_i32::<LittleEndian>()?;
    let y = reader.read_i32::<LittleEndian>()?;
    let z = reader.read_i32::<LittleEndian>()?;

    Ok(Vector3 { x, y, z })
}

fn read_vec3d<R: Read + Seek>(reader: &mut R) -> Result<Vector3<f64>> {
    let x = reader.read_f64::<LittleEndian>()?;
    let y = reader.read_f64::<LittleEndian>()?;
    let z = reader.read_f64::<LittleEndian>()?;

    Ok(Vector3 { x, y, z })
}

impl TryFrom<u8> for NodeMetaData {
    type Error = ErrorKind;

    fn try_from(v: u8) -> Result<NodeMetaData> {
        Ok(match v {
            0 => Self::NoMaskOrInactiveVals,
            1 => Self::NoMaskAndMinusBg,
            2 => Self::NoMaskAndOneInactiveVal,
            3 => Self::MaskAndNoInactiveVals,
            4 => Self::MaskAndOneInactiveVal,
            5 => Self::MaskAndTwoInactiveVals,
            6 => Self::NoMaskAndAllVals,
            _ => return Err(ErrorKind::InvalidNodeMetadata(v)),
        })
    }
}

trait TryConvertFromBitVec<const LOG2_D: u64> {
    type Error;
    fn try_convert(value: BitVec<u64>) -> std::result::Result<Self, Self::Error>
    where
        Self: Sized;
}

impl<const LOG2_D: u64> TryConvertFromBitVec<LOG2_D> for [u64; (1 << (LOG2_D * 3)) / 64] {
    type Error = ErrorKind;

    fn try_convert(mask: BitVec<u64>) -> Result<Self> {
        let slice: &[u64] = mask.as_raw_slice();
        match TryInto::<[u64; (1 << (LOG2_D * 3)) / 64]>::try_into(slice) {
            Err(_) => Err(ErrorKind::UnexpectedMaskLength),
            Ok(array) => Ok(array),
        }
    }
}

fn try_from_bitvec<const LOG2_D: u64>(
    mask: BitVec<u64>,
) -> Result<[u64; ((1 << (LOG2_D * 3)) / 64) as usize]> {
    let slice: &[u64] = mask.as_raw_slice();
    match TryInto::<[u64; ((1 << (LOG2_D * 3)) / 64) as usize]>::try_into(slice) {
        Err(_) => Err(ErrorKind::UnexpectedMaskLength),
        Ok(array) => Ok(array),
    }
}

#[cfg(test)]
mod tests {
    use std::{io::BufReader, thread};

    use super::*;

    #[test]
    fn test_read_utahteapot() {
        let builder = thread::Builder::new()
            .name("set_voxel_test".into())
            .stack_size(80 * 1024 * 1024); // @HACK to increase stack size of this test
        let handler = builder.spawn(|| test_read_vdb("utahteapot")).unwrap();
        handler.join().unwrap_or_else(|_| panic!("Test Failed"));
    }

    #[test]
    fn test_read_cube() {
        let builder = thread::Builder::new()
            .name("set_voxel_test".into())
            .stack_size(80 * 1024 * 1024); // @HACK to increase stack size of this test
        let handler = builder.spawn(|| test_read_vdb("cube")).unwrap();
        handler.join().unwrap_or_else(|_| panic!("Test Failed"));
    }

    #[test]
    fn test_read_icosahedron() {
        let builder = thread::Builder::new()
            .name("set_voxel_test".into())
            .stack_size(80 * 1024 * 1024); // @HACK to increase stack size of this test
        let handler = builder.spawn(|| test_read_vdb("icosahedron")).unwrap();
        handler.join().unwrap_or_else(|_| panic!("Test Failed"));
    }

    fn test_read_vdb(name: &'static str) {
        let f = std::fs::File::open(format!("assets/{name}.vdb")).unwrap();
        let b = BufReader::new(f);

        let mut vdb_reader = VdbReader::new(b).unwrap();
        dbg!(&vdb_reader.header);
        dbg!(&vdb_reader.grid_descriptors);

        let grid_name = format!("ls_{name}");

        let vdb = vdb_reader.read_vdb345_grid::<u32>(&grid_name).unwrap();

        let mut count_voxels = 0;
        for (root_key, root_child) in &vdb.root.map {
            let RootData::Node(node5) = root_child else {
                continue;
            };
            println!("root_key: {root_key:?}");
            for node5_child in &node5.data {
                let InternalData::Node(node4) = node5_child else {
                    continue;
                };
                for node4_child in &node4.data {
                    let InternalData::Node(node3) = node4_child else {
                        continue;
                    };
                    for node3_child in &node3.data {
                        let LeafData::Value(_) = node3_child else {
                            continue;
                        };
                        count_voxels += 1;
                    }
                }
            }
        }

        assert_eq!(
            *vdb_reader
                .grid_descriptors
                .get(&grid_name)
                .unwrap()
                .meta_data
                .0
                .get("file_voxel_count")
                .unwrap(),
            MetadataValue::I64(count_voxels)
        );
    }
}

impl From4LeBytes for f16 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        f16::from_le_bytes([array[0], array[1]])
    }
}

impl From4LeBytes for f32 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        f32::from_le_bytes(array)
    }
}

impl From4LeBytes for u8 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        u8::from_le_bytes([array[0]])
    }
}

impl From4LeBytes for u16 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        u16::from_le_bytes([array[0], array[1]])
    }
}

impl From4LeBytes for u32 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        u32::from_le_bytes(array)
    }
}

impl From4LeBytes for u64 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        u32::from_le_bytes(array) as u64
    }
}

impl From4LeBytes for u128 {
    fn from_4_le_bytes(array: [u8; 4]) -> Self {
        u32::from_le_bytes(array) as u128
    }
}

impl TryFrom<u32> for Compression {
    type Error = ErrorKind;

    fn try_from(v: u32) -> Result<Compression> {
        Self::from_bits(v).ok_or(ErrorKind::InvalidCompression(v))
    }
}
