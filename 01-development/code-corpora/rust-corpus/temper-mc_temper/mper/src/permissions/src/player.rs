use crate::{Access, PermissionSet, Permissions};
use bevy_ecs::prelude::Component;
use bitcode::{Decode, Encode};
use std::collections::HashMap;
use type_hash::TypeHash;

/// Component representing a player's permissions.
#[derive(Component, Clone, Debug, Encode, Decode, TypeHash)]
pub struct PlayerPermission {
    pub permissions: PermissionSet,
}

impl Default for PlayerPermission {
    fn default() -> Self {
        Self::new()
    }
}

impl PlayerPermission {
    pub fn new() -> Self {
        PlayerPermission {
            permissions: HashMap::new(),
        }
    }

    /// Adds a permission to the player's individual permissions.
    pub fn set_permission(&mut self, permission: Permissions, access: Access) {
        self.permissions.insert(permission, access);
    }

    /// Removes a permission from the player's individual permissions.
    pub fn remove_permission(&mut self, permission: &Permissions) {
        self.permissions.remove(permission);
    }

    /// Checks if the player has a specific permission
    pub fn can(&self, permission: Permissions) -> bool {
        let mut result = None;

        // Get ALL first so more specific permissions can override it
        if let Some(value) = self.permissions.get(&Permissions::ALL)
            && matches!(value, Access::Allow)
        {
            result = Some(value);
        }

        if let Some(value) = self.permissions.get(&permission) {
            result = Some(value);
        }

        matches!(result, Some(Access::Allow))
    }
}
