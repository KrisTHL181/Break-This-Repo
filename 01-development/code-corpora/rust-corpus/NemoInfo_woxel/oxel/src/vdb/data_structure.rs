/// The VDB data structure was originally proposed by Ken Museth in his 2013 paper entitled [VDB: High-Resolution Sparse Volumes with Dynamic Topology](https://www.museth.org/Ken/Publications_files/Museth_TOG13.pdf)
///
use bitflags::bitflags;
use bitvec::vec::BitVec;
use cgmath::Vector3;
use std::{
    collections::HashMap,
    io::{Read, Seek, SeekFrom},
};

use super::VdbValueType;

pub type GlobalCoordinates = Vector3<i32>;
pub type LocalCoordinates = Vector3<u32>;
pub type Offset = usize;

pub trait Node {
    /// LOG2_D of side length -> 3
    ///
    /// LOG2_D = 3 => `512 = 8 * 8 * 8` children
    // TODO: Maybe these should be u32
    const LOG2_D: u64;
    /// LOG2_D * 2 (for 2 arbitrary dimensions)
    const LOG2_DD: u64 = Self::LOG2_D * 2;
    /// Total conceptual LOG2_D node
    const TOTAL_LOG2_D: u64;
    /// Total conceptual LOG2_D of child node
    const CHILD_TOTAL_LOG2_D: u64 = Self::TOTAL_LOG2_D - Self::LOG2_D;
    const DIM: u64 = 1 << Self::LOG2_D;
    /// Total conceptual dimension
    const TOTAL_DIM: u64 = 1 << Self::TOTAL_LOG2_D;
    /// Size of this node (i.e. length of data array)
    const SIZE: usize = 1 << (Self::LOG2_D * 3);
    /// Total conceptual size of node, including child size
    const TOTAL_SIZE: u64 = 1 << (Self::TOTAL_LOG2_D * 3);

    // TODO: Explain naming convention better
    // global -> global coordinates of a node
    // relative -> global coordinates minus node origin
    // child -> local coordinates in the grid of the node (scaling down depending on child size)
    // offset -> index of child in parent data array
    /// Give local coordinates relative to the Node containing `global` position
    fn global_to_relative(global: GlobalCoordinates) -> LocalCoordinates {
        global
            .map(|c| (c & ((1 << Self::TOTAL_LOG2_D) - 1)) as u32)
            .into()
    }

    /// Give local child coordinates from `relative` positon
    fn relative_to_child(local: LocalCoordinates) -> LocalCoordinates {
        local.map(|c| c >> Self::CHILD_TOTAL_LOG2_D)
    }

    /// Give the index for the child node that contains the `global` position ``
    ///
    /// This is conceptually equivalent to calling global_to_relative > relative_to_child > child_to_offset
    /// It is a Rust adaptation of the [original paper](https://www.museth.org/Ken/Publications_files/Museth_TOG13.pdf) (line 46-49)
    fn global_to_offset(global: GlobalCoordinates) -> Offset {
        // Translated from
        // ((( x &(1 << sLog2X ) -1) >> Child :: sLog2X ) << Log2YZ ) +
        // ((( y &(1 << sLog2Y ) -1) >> Child :: sLog2Y ) << Log2Z ) +
        //  (( z &(1 << sLog2Z ) -1) >> Child :: sLog2Z );

        ((((global.x & (1 << Self::TOTAL_LOG2_D) - 1) >> Self::CHILD_TOTAL_LOG2_D)
            << Self::LOG2_DD)
            | (((global.y & (1 << Self::TOTAL_LOG2_D) - 1) >> Self::CHILD_TOTAL_LOG2_D)
                << Self::LOG2_D)
            | ((global.z & (1 << Self::TOTAL_LOG2_D) - 1) >> Self::CHILD_TOTAL_LOG2_D))
            as usize
    }

    /// Give global origin of Node coordinates from `global` point
    fn global_to_node(global: GlobalCoordinates) -> GlobalCoordinates {
        global.map(|c| (c >> Self::TOTAL_LOG2_D) << Self::TOTAL_LOG2_D)
    }

    /// Give relative coordinate from offset
    ///
    /// index in the data field of the node -> relative x, y, z of child inside
    fn offset_to_child(offset: Offset) -> LocalCoordinates {
        (
            offset as u32 >> Self::LOG2_DD,
            (offset as u32 >> Self::LOG2_D) & (Self::DIM as u32 - 1),
            offset as u32 & (Self::DIM as u32 - 1),
        )
            .into()
    }

    fn child_to_offset(local: LocalCoordinates) -> Offset {
        (local.x << Self::LOG2_DD | local.y << Self::LOG2_D | local.z) as usize
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct LeafNode<ValueType, const LOG2_D: u64>
where
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    [(); (1 << (LOG2_D * 3)) as usize]:,
    ValueType: VdbValueType,
{
    pub data: [LeafData<ValueType>; (1 << (LOG2_D * 3)) as usize],
    // IMPORTANT: the mask is encoded inside u64 in the following way
    // [ 63..0, 127..64, .... ]
    // this means it is indexed like
    //  mask[offset / 64] & ( 1 << ( offset % 64 ) )
    //                    OR
    //  mask[offset >> 6] & ( 1 << ( offset & 63 ) )
    pub value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize],
    pub flags: u64,
}

impl<ValueType, const LOG2_D: u64> Node for LeafNode<ValueType, LOG2_D>
where
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    [(); (1 << (LOG2_D * 3)) as usize]:,
    ValueType: VdbValueType,
{
    const LOG2_D: u64 = LOG2_D;
    const TOTAL_LOG2_D: u64 = LOG2_D;
}

impl<ValueType, const LOG2_D: u64> LeafNode<ValueType, LOG2_D>
where
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    [(); (1 << (LOG2_D * 3)) as usize]:,
    ValueType: VdbValueType,
{
    pub fn new() -> Self {
        let data: [LeafData<ValueType>; (1 << (LOG2_D * 3)) as usize] =
            std::array::from_fn(|_| LeafData::Tile(0));
        let value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize] =
            [0; ((1 << (LOG2_D * 3)) / 64) as usize];
        let flags = 0;

        Self {
            data,
            value_mask,
            flags,
        }
    }

    pub fn new_from_header(value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize]) -> Self {
        let data: [LeafData<ValueType>; (1 << (LOG2_D * 3)) as usize] =
            std::array::from_fn(|_| LeafData::Tile(0));
        [0; ((1 << (LOG2_D * 3)) / 64) as usize];
        let flags = 0;

        Self {
            data,
            value_mask,
            flags,
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum LeafData<ValueType> {
    Tile(usize),
    Value(ValueType),
}

#[derive(Debug, Clone, PartialEq)]
pub struct InternalNode<ValueType, ChildType, const LOG2_D: u64>
where
    [(); (1 << (LOG2_D * 3)) as usize]:,
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    ChildType: Node,
{
    pub data: [InternalData<ChildType>; (1 << (LOG2_D * 3)) as usize],
    pub value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize],
    pub child_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize],
    pub origin: [i32; 3],
}

impl<ValueType, ChildType, const LOG2_D: u64> InternalNode<ValueType, ChildType, LOG2_D>
where
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    [(); (1 << (LOG2_D * 3)) as usize]:,
    ValueType: VdbValueType,
    ChildType: Node,
{
    pub fn new(origin: GlobalCoordinates) -> Self {
        let data: [InternalData<ChildType>; (1 << (LOG2_D * 3)) as usize] =
            std::array::from_fn(|_| InternalData::Tile(0));
        let value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize] =
            [0; ((1 << (LOG2_D * 3)) / 64) as usize];
        let child_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize] =
            [0; ((1 << (LOG2_D * 3)) / 64) as usize];
        let origin = origin.into();

        Self {
            data,
            value_mask,
            child_mask,
            origin,
        }
    }

    pub fn new_from_header(
        child_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize],
        value_mask: [u64; ((1 << (LOG2_D * 3)) / 64) as usize],
        origin: [i32; 3],
    ) -> Self {
        let data: [InternalData<ChildType>; (1 << (LOG2_D * 3)) as usize] =
            std::array::from_fn(|_| InternalData::Tile(0));

        Self {
            data,
            value_mask,
            child_mask,
            origin,
        }
    }
}

impl<ValueType, ChildType, const LOG2_D: u64> Node for InternalNode<ValueType, ChildType, LOG2_D>
where
    [(); (1 << (LOG2_D * 3)) as usize]:,
    [(); ((1 << (LOG2_D * 3)) / 64) as usize]:,
    ValueType: VdbValueType,
    ChildType: Node,
{
    const LOG2_D: u64 = LOG2_D;
    const TOTAL_LOG2_D: u64 = LOG2_D + ChildType::TOTAL_LOG2_D;
}

#[derive(Debug, Clone, PartialEq)]
pub enum InternalData<ChildType> {
    Node(Box<ChildType>),
    Tile(u32),
}

#[derive(Debug, Clone, PartialEq)]
pub struct RootNode<ValueType, ChildType: Node>
where
    ValueType: VdbValueType,
{
    // @SPEED: Use a custom hash function
    pub map: HashMap<[i32; 3], RootData<ChildType>>,
    pub background: ValueType,
}

#[derive(Debug, Clone, PartialEq)]
pub enum RootData<ChildType> {
    Node(Box<ChildType>),
    Tile(u32, bool),
}

impl<ValueType, ChildType: Node> RootNode<ValueType, ChildType>
where
    ValueType: VdbValueType,
{
    fn new() -> Self {
        let map = HashMap::new();
        let background = ValueType::zeroed();
        Self { map, background }
    }
}

impl<ValueType, ChildType: Node> RootNode<ValueType, ChildType>
where
    ValueType: VdbValueType,
{
    pub fn root_key_from_coords(global: GlobalCoordinates) -> [i32; 3] {
        // @HACK: these 2 might not actually be equivalent
        // p.map(|c| c & !((1 << ChildType::TOTAL_LOG2_D) - 1)).into()
        ChildType::global_to_node(global).into()
    }
}

#[derive(Debug, Clone, PartialEq)]
pub struct VDB<ValueType, ChildType: Node>
where
    ValueType: VdbValueType,
{
    pub root: RootNode<ValueType, ChildType>,
    pub grid_descriptor: GridDescriptor,
}

#[derive(Debug, Clone, PartialEq)]
pub struct GridDescriptor {
    pub name: String,
    /// If not empty, the name of another grid that shares this grid's tree
    pub instance_parent: String,
    pub grid_type: String,
    /// Location in the stream where the grid data is stored
    pub grid_pos: u64,
    /// Location in the stream where the grid blocks are stored
    pub block_pos: u64,
    /// Location in the stream where the next grid descriptor begins
    pub end_pos: u64,
    pub compression: Compression,
    pub meta_data: Metadata,
}

impl GridDescriptor {
    pub fn seek_to_grid<R: Read + Seek>(&self, reader: &mut R) -> Result<u64, std::io::Error> {
        reader.seek(SeekFrom::Start(self.grid_pos))
    }

    pub(crate) fn seek_to_blocks<R: Read + Seek>(
        &self,
        reader: &mut R,
    ) -> Result<u64, std::io::Error> {
        reader.seek(SeekFrom::Start(self.block_pos))
    }
}

impl<ValueType, ChildType: Node> VDB<ValueType, ChildType>
where
    ValueType: VdbValueType,
{
    pub fn new() -> Self {
        let root = <RootNode<ValueType, ChildType>>::new();
        let grid_descriptor = GridDescriptor {
            name: "Demo".to_string(),
            instance_parent: "What".to_string(),
            grid_type: "Whatt".to_string(),
            grid_pos: 0,
            block_pos: 0,
            end_pos: 0,
            compression: Compression::NONE,
            meta_data: Default::default(),
        };

        Self {
            root,
            grid_descriptor,
        }
    }
}

// HACK:
// This is a bit akward Root is actually just a Node5 tile
// Inner(v, 5) is a Node4 tile
// Inner(v, 4) is a Node3 tile
#[derive(Debug, Clone, PartialEq)]
pub enum VdbEndpoint<ValueType> {
    Offs(usize),
    Leaf(ValueType),
    Innr(u32, u8),
    Root(u32),
    Bkgr(ValueType),
}

#[derive(Debug, Default, Clone, PartialEq)]
pub struct Metadata(pub HashMap<String, MetadataValue>);

impl Metadata {
    pub fn is_half_float(&self) -> bool {
        self.0.get("is_saved_as_half_float") == Some(&MetadataValue::Bool(true))
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum MetadataValue {
    String(String),
    Vec3i(cgmath::Vector3<i32>),
    I32(i32),
    I64(i64),
    Float(f32),
    Bool(bool),
    Unknown { name: String, data: Vec<u8> },
}

bitflags! {
    #[derive(Clone, Copy, Debug, Default, PartialEq, Eq, PartialOrd, Ord, Hash)]
    pub struct Compression: u32 {
        const NONE = 0;
        const ZIP = 0x1;
        const ACTIVE_MASK = 0x2;
        const BLOSC = 0x4;
        const DEFAULT_COMPRESSION = Self::BLOSC.bits() | Self::ACTIVE_MASK.bits();
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum NodeMetaData {
    NoMaskOrInactiveVals,
    NoMaskAndMinusBg,
    NoMaskAndOneInactiveVal,
    MaskAndNoInactiveVals,
    MaskAndOneInactiveVal,
    MaskAndTwoInactiveVals,
    NoMaskAndAllVals,
}

#[derive(Debug)]
pub struct NodeHeader<ValueType> {
    pub child_mask: BitVec<u64>,
    pub value_mask: BitVec<u64>,
    pub data: Vec<ValueType>,
    pub log_2_dim: u64,
}

#[derive(Debug, Default, PartialEq)]
pub struct ArchiveHeader {
    /// The version of the file that was read
    pub file_version: u32,
    /// The version of the library that was used to create the file that was read
    pub library_major: u32,
    pub library_minor: u32,
    /// Unique tag, a random 16-byte (128-bit) value, stored as a string format.
    pub uuid: String,
    /// Flag indicating whether the input stream contains grid offsets and therefore supports partial reading
    pub has_grid_offsets: bool,
    /// Flags indicating whether and how the data stream is compressed
    pub compression: Compression,
    /// the number of grids on the input stream
    pub grid_number: u32,
    /// The metadata for the input stream
    pub meta_data: Metadata,
}

#[cfg(test)]
mod tests {
    use super::*;

    type N3 = LeafNode<u64, 3>;
    type N4 = InternalNode<u64, N3, 4>;
    type N5 = InternalNode<u64, N4, 5>;

    #[test]
    fn global_to_node_test() {
        assert_eq!(
            Vector3::from([-8, 0, 0]),
            N3::global_to_node([-1, 0, 0].into())
        );
        assert_eq!(
            Vector3::from([-256, 2304, 0]),
            N4::global_to_node([-142, 2431, 102].into())
        );
        assert_eq!(
            Vector3::from([-4096, 0, -45056]),
            N5::global_to_node([-1, 0, -42141].into())
        );
    }

    #[test]
    fn n345_mask_size_test() {
        let n345_masks: (usize, usize, usize) = {
            fn size<T>(_: *const T) -> usize {
                std::mem::size_of::<T>()
            }

            let null: *const N3 = std::ptr::null();
            let n3_mask_size = size(unsafe { &raw const (*null).value_mask }) / 8;

            let null: *const N4 = std::ptr::null();
            let n4_mask_size = size(unsafe { &raw const (*null).value_mask }) / 8;

            let null: *const N5 = std::ptr::null();
            let n5_mask_size = size(unsafe { &raw const (*null).value_mask }) / 8;

            (n3_mask_size, n4_mask_size, n5_mask_size)
        };

        assert_eq!(n345_masks, (8, 64, 512));
    }

    #[test]
    fn total_log2_d_test() {
        assert_eq!(8, 1 << N3::TOTAL_LOG2_D);
        assert_eq!(128, 1 << N4::TOTAL_LOG2_D);
        assert_eq!(4096, 1 << N5::TOTAL_LOG2_D);
    }

    #[test]
    fn bit_index_test() {
        assert_eq!(0, N3::global_to_offset([0, 0, 0].into()));
        assert_eq!(83, N3::global_to_offset([1, 2, 3].into()));
        assert_eq!(3382, N4::global_to_offset([121321, 212123, 3121].into()));
        assert_eq!(0, N5::global_to_offset([1, 2, 3].into()));
    }

    #[test]
    fn local_to_offset_test() {
        let tests = vec![[1, 2, 3], [15, 15, 0], [8, 9, 10]];

        for coord in tests {
            let offset = N4::child_to_offset(coord.into());
            let res: [u32; 3] = N4::offset_to_child(offset).into();
            assert_eq!(coord, res);
        }
    }
}
