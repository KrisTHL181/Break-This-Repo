use core::fmt;
use std::{fmt::Write, mem::size_of};

use bytes::{self, BufMut, BytesMut};
use cgmath::num_traits::ToBytes;
use half::f16;

use super::{InternalData, LeafData, RootData, VdbValueType, N3, N4, N5, VDB345};

pub fn write_vdb<T: VdbValueType>(
    b: &mut BytesMut,
    vdb: &VDB345<T>,
    mat: [[f64; 4]; 4],
) -> fmt::Result {
    // Magic number
    b.write_str(" BDV")?;
    b.put_bytes(0, 4);

    // File version
    b.put_u32_le(224);

    // Library Version
    b.put_u32_le(8);
    b.put_u32_le(1);

    // No grid offsets
    b.put_u8(0);

    // Temporary UUID
    b.write_str("d2b59639-ac2f-4047-9c50-9648f951180c")?;

    // Metadata
    b.put_u32_le(0);

    // # of grids
    b.put_u32_le(1);

    write_grid(b, vdb, mat)?;

    Ok(())
}

fn write_grid<T: VdbValueType>(
    b: &mut BytesMut,
    vdb: &VDB345<T>,
    mat: [[f64; 4]; 4],
) -> fmt::Result {
    // Grid name
    // @HACK: Actually use the grid descriptor here
    write_len_based_str(b, "woxel")?;

    // Grid Type
    // @HACK: This should be related to the generic T type not just a random float
    write_len_based_str(b, "Tree_float_5_4_3")?;

    // No instance parent
    b.put_u32_le(0);

    // Grid descriptor stream position
    b.put_u64((b.len() + (size_of::<u64>() * 3)) as u64);
    b.put_u64(0);
    b.put_u64(0);

    // No compression
    b.put_u32_le(0);

    write_metadata(b)?;
    write_transform(b, mat)?;
    write_tree(b, vdb)?;

    Ok(())
}

fn write_tree<T: VdbValueType>(b: &mut BytesMut, vdb: &VDB345<T>) -> fmt::Result {
    // Magic
    b.put_u32_le(1);

    // Root node background value
    b.put_u32_le(vdb.root.background.copy_bytes_to_u32());

    // Number of top level tiles
    b.put_u32_le(0);

    // Number of 5 nodes
    b.put_u32_le(vdb.root.map.len() as u32);

    // Iterate node 5s
    for (_, node5_data) in &vdb.root.map {
        let RootData::Node(node5) = node5_data else {
            continue;
        };
        write_node5_header(b, node5);

        for node4_data in &node5.data {
            let InternalData::Node(node4) = node4_data else {
                continue;
            };

            write_node4_header(b, node4);
            for node3_data in &node4.data {
                let InternalData::Node(node3) = node3_data else {
                    continue;
                };

                write_node3_header(b, node3);
            }
        }
    }

    for (_, node5_data) in &vdb.root.map {
        let RootData::Node(node5) = node5_data else {
            continue;
        };

        for node4_data in &node5.data {
            let InternalData::Node(node4) = node4_data else {
                continue;
            };

            for node3_data in &node4.data {
                let InternalData::Node(node3) = node3_data else {
                    continue;
                };

                write_node3_data(b, node3);
            }
        }
    }

    Ok(())
}

fn write_node5_header<T: VdbValueType>(b: &mut BytesMut, node5: &Box<N5<T>>) {
    b.put_i32(node5.origin[0]);
    b.put_i32(node5.origin[1]);
    b.put_i32(node5.origin[2]);

    for word in node5.child_mask {
        b.put_u64(word);
    }

    for word in node5.value_mask {
        b.put_u64(word);
    }

    // Write uncompressed node values, 6 = no compression
    b.put_u8(6);

    // Write values of the tiles
    for i in 0..(node5.value_mask.len() * 64) {
        let value = &node5.data[i];
        if let InternalData::Tile(value) = value {
            b.put_u32_le(value.copy_bytes_to_u32());
        }
    }
}

fn write_node4_header<T: VdbValueType>(b: &mut BytesMut, node4: &Box<N4<T>>) {
    for word in node4.child_mask {
        b.put_u64(word);
    }

    for word in node4.value_mask {
        b.put_u64(word);
    }

    // Write uncompressed node values, 6 = no compression
    b.put_u8(6);

    // Write values of the tiles
    for i in 0..(node4.value_mask.len() * 64) {
        let value = &node4.data[i];
        if let InternalData::Tile(value) = value {
            b.put_u32_le(value.copy_bytes_to_u32());
        }
    }
}

fn write_node3_header<T: VdbValueType>(b: &mut BytesMut, node3: &Box<N3<T>>) {
    for word in node3.value_mask {
        b.put_u64(word);
    }
}

fn write_node3_data<T: VdbValueType>(b: &mut BytesMut, node3: &Box<N3<T>>) {
    for word in node3.value_mask {
        b.put_u64(word);
    }
    // No compression
    b.put_u8(6);

    for data in &node3.data {
        match data {
            // @HACK: figure out the offsets size and shape and purpose =)))
            LeafData::Tile(_) => b.put_u32_le(0),
            LeafData::Value(val) => b.put_u32_le(val.copy_bytes_to_u32()),
        }
    }
}

fn write_transform(b: &mut BytesMut, mat: [[f64; 4]; 4]) -> fmt::Result {
    write_len_based_str(b, "Affine Map")?;

    for j in 0..=3 {
        for i in 0..=2 {
            b.put_f64(mat[j][i]);
        }
        b.put_f64((j == 3) as u8 as f64);
    }

    Ok(())
}

fn write_metadata(b: &mut BytesMut) -> fmt::Result {
    b.put_u32_le(4);

    write_meta_string(b, "class", "unknown")?;
    write_meta_string(b, "file_compression", "none")?;
    write_meta_bool(b, "is_saved_as_half_float", false)?;
    write_meta_string(b, "name", "woxel")?;

    Ok(())
}

fn write_meta_string(b: &mut BytesMut, name: &str, string: &str) -> fmt::Result {
    write_len_based_str(b, name)?;
    write_len_based_str(b, "string")?;
    write_len_based_str(b, string)?;

    Ok(())
}

fn write_meta_bool(b: &mut BytesMut, name: &str, v: bool) -> fmt::Result {
    write_len_based_str(b, name)?;
    write_len_based_str(b, "bool")?;
    b.put_u32_le(1);
    b.put_u8(v as u8);

    Ok(())
}

fn write_len_based_str(b: &mut BytesMut, s: &str) -> fmt::Result {
    b.put_u32_le(s.len() as u32);
    b.write_str(s)?;

    Ok(())
}

pub trait CopyBytesToU32 {
    fn copy_bytes_to_u32(&self) -> u32;
}

impl CopyBytesToU32 for f16 {
    fn copy_bytes_to_u32(&self) -> u32 {
        let bytes = self.to_le_bytes();
        u32::from_le_bytes([0, 0, bytes[0], bytes[1]])
    }
}

impl CopyBytesToU32 for f32 {
    fn copy_bytes_to_u32(&self) -> u32 {
        u32::from_be_bytes(self.to_be_bytes())
    }
}

impl CopyBytesToU32 for u8 {
    fn copy_bytes_to_u32(&self) -> u32 {
        let byte = self.to_le_bytes();
        u32::from_le_bytes([0, 0, 0, byte[0]])
    }
}

impl CopyBytesToU32 for u16 {
    fn copy_bytes_to_u32(&self) -> u32 {
        let byte = self.to_le_bytes();
        u32::from_le_bytes([0, 0, byte[0], byte[1]])
    }
}

impl CopyBytesToU32 for u32 {
    fn copy_bytes_to_u32(&self) -> u32 {
        *self
    }
}

impl CopyBytesToU32 for u64 {
    fn copy_bytes_to_u32(&self) -> u32 {
        let bytes = self.to_le_bytes();
        u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])
    }
}

impl CopyBytesToU32 for u128 {
    fn copy_bytes_to_u32(&self) -> u32 {
        // HACK: This loses data, but we should never write a vdb file with u128 data
        let bytes = (*self as u64).to_le_bytes();
        u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])
    }
}

#[cfg(test)]
mod tests {
    use std::{fs, io::Write, thread};

    use super::*;

    #[test]
    fn test_vdb_write_wrapper() {
        let builder = thread::Builder::new()
            .name("set_voxel_test".into())
            .stack_size(80 * 1024 * 1024); // @HACK to increase stack size of this test
        let handler = builder.spawn(|| test_vdb_write()).unwrap();
        handler.join().unwrap_or_else(|_| panic!("Test Failed"));
    }

    fn test_vdb_write() {
        let mut vdb = <VDB345<f32>>::new();
        let points = [[0, 0, 0], [123, 78, 3], [34, 123, 46], [102, 79, 28]];
        for (i, &point) in points.iter().enumerate() {
            vdb.set_voxel(point.into(), i as f32);
        }

        let mut b: BytesMut = BytesMut::new();
        let mat = [
            [1., 0., 0., 0.],
            [0., 1., 0., 0.],
            [0., 0., 1., 0.],
            [0., 0., 0., 1.],
        ];
        let _ = write_vdb(&mut b, &vdb, mat);

        println!("{:#x}", b);

        let mut file = fs::File::create("assets/test.vdb").unwrap();
        let res = file.write_all(&b);
        match res {
            Err(_) => assert!(false),
            Ok(()) => {}
        }
    }
}
