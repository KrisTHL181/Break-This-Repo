use crate::BLOCK_MAPPINGS;
use temper_block_data::{BlockUpdates, BrokenBlocks, PlacedBlocks, PlacementContext};
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_world::World;

/// Macro to autogenerate the `BlockBehavior` trait and associated VTable structs.
///
/// This macro simply exists to make adding methods to blocks easier. It should NOT be used anywhere except in this file, and should also only be used once.
/// See below this macro for where to add functions.
///
/// The syntax for this macro is as follows: `fn <name>([mut]; <arguments>) [-> <return type>; <default return value>]`
/// - `name`: The name of the method
/// - `mut`: Optional, whether the function takes a mutable reference to the block or not
/// - `arguments`: Any additional arguments to the method
/// - `return type`: Optional, what the function returns
/// - `default return value`: Optional, required if return type is set, the default expression to use for blocks that do not implement this method.
///
/// TODO: Later on, add support for methods like BlockStateId::<function_name>(base_id) -> (BlockStateId, <other args>). Would be useful for methods like
///     get_placement_state().
macro_rules! block_behavior_trait {
    ($(fn $name:ident($($mut_meta:ident)?; $($argument:ident: $ty:ty),*) $(-> $ret:ty; $default:expr)?),* $(,)?) => {
        macro_rules! ptr_ret_ty {
            (mut) => {
                u32
            };
            () => {
                ()
            };
            (mut $retb:ty) => {
                (u32, $retb)
            };
            ($retb:ty) => {
                $retb
            };
        }

        macro_rules! id_ret_decode {
            (mut, $in_data:expr) => {
                ($in_data, ())
            };
            (, $in_data:expr) => {
                ((), ())
            };
            (mut $retb:ty, $in_data:expr) => {
                $in_data
            };
            ($retb:ty, $in_data:expr) => {
                ((), $in_data)
            };
        }

        macro_rules! lambda_ret_ty {
            (mut; $data:expr;) => {
                $data
                    .try_into()
                    .unwrap_or_else(|_| panic!("Failed to convert block data back into id"))
            };
            () => {
                ()
            };
            (mut; $data:expr; $retb:ty; $ret_val:expr) => {
                (
                    $data
                        .try_into()
                        .unwrap_or_else(|_| panic!("Failed to convert block data back into id")),
                    $ret_val,
                )
            };
            ($retb:ty; $ret_val:expr) => {
                $ret_val
            };
        }

        pub trait BlockBehavior:
            TryInto<u32, Error = ()> + TryFrom<u32, Error = ()> + Clone + std::fmt::Debug
        {
            $(
                fn $name(&$($mut_meta)? self, $($argument: $ty),*) $(-> $ret)? { $($default)? }
            )*
        }

        #[allow(dead_code)]
        pub trait BlockDispatch {
            fn try_cast<T: BlockBehavior>(&self) -> Option<T>;

            $(
                fn $name(&$($mut_meta)? self, $($argument: $ty),*) $(-> $ret)?;
            )*
        }

        pub struct BlockBehaviorTable {
            $(
                $name: fn(id: u32, $($argument: $ty),*) -> ptr_ret_ty!{$($mut_meta)? $($ret)?}
            ),*
        }

        impl BlockBehaviorTable {
            pub const fn from<T: BlockBehavior>() -> Self {
                Self {
                    $(
                        $name: |id, $($argument),*| {
                            let $($mut_meta)? data = T::try_from(id).unwrap_or_else(|_| panic!("Failed to convert id to data"));
                            let _ret = data.$name($($argument),*);
                            lambda_ret_ty!($($mut_meta; data;)? $($ret; _ret)?)
                        }
                    ),*
                }
            }
        }

        pub struct StateBehaviorTable {
            block: &'static BlockBehaviorTable,
            id: u32,
        }

        impl StateBehaviorTable {
            pub const fn spin_off(block: &'static BlockBehaviorTable, id: u32) -> Self {
                Self { block, id }
            }

            $(
                pub fn $name(&self, $($argument: $ty),*) -> ptr_ret_ty!{$($mut_meta)? $($ret)?} {
                    (self.block.$name)(self.id, $($argument),*)
                }
            )*
        }

        macro_rules! update_self {
            (mut, $s:expr, $new_id:expr) => {
                *$s = Self::new($new_id);
            };
            (, $s:expr, $new_id:expr) => {

            };
        }

        impl BlockDispatch for BlockStateId {
            fn try_cast<T: BlockBehavior>(&self) -> Option<T> {
                T::try_from(self.raw()).ok()
            }

            $(
                fn $name(& $($mut_meta)? self, $($argument: $ty),*) $(-> $ret)? {
                    let (_new_id, _ret) = id_ret_decode!{$($mut_meta)? $($ret)?, BLOCK_MAPPINGS[self.raw() as usize].$name($($argument),*)};
                    update_self!{$($mut_meta)?, self, _new_id}
                    _ret
                }
            )*
        }
    };
}

// This is where methods are defined for blocks. See the macro above for the syntax.
//
// This is the only place where the `block_behavior_trait!` macro should be used.
block_behavior_trait!(
    fn get_placement_state(mut; _context: PlacementContext) -> PlacedBlocks; PlacedBlocks::default(),
    fn interact(mut; _world: &World, _pos: BlockPos) -> BlockUpdates; BlockUpdates::default(),
    fn try_break(; _world: &World, _pos: BlockPos) -> BrokenBlocks; BrokenBlocks::default(),

    fn is_interactable(;) -> bool; false,
    fn is_solid(;) -> bool; true, // Used for fence and pane connections

    fn can_be_replaced(; _context: PlacementContext) -> bool; false,
    fn update(mut; _world: &World, _pos: BlockPos) -> bool; false, // The boolean returned is whether to emit more adjacent updates from this block
);
