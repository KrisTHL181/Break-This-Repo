use bitcode::{Decode, Encode};
use std::collections::HashMap;
use type_hash::TypeHash;

pub mod player;
#[cfg(test)]
mod tests;

#[derive(Debug, Clone, Copy, Eq, Hash, PartialEq, Encode, Decode, TypeHash)]
pub enum Permissions {
    ALL,

    StopServer,
    Teleport,
    Kill,
    Ban,
    Kick,
    Op,
    DeOp,
}

#[derive(Debug, Clone, Copy, Eq, PartialEq, Encode, Decode, TypeHash)]
pub enum Access {
    Allow,
    Deny,
}

pub type PermissionSet = HashMap<Permissions, Access>;
