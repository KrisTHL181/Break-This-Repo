use std::fmt::Debug;

use bytemuck::Pod;
pub trait VdbValueType =
    From4LeBytes + Pod + Copy + CopyBytesToU32 + Debug + PartialOrd + Bounded + PartialEq;

mod data_structure;
use cgmath::Bounded;
pub use data_structure::*;

mod vdb345;
pub use vdb345::*;

mod write;
pub use write::*;

mod read;
pub use read::*;

mod transform;
