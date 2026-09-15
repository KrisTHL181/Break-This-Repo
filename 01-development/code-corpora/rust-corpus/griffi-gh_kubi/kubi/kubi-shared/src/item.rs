use std::num::NonZeroU8;
use num_enum::TryFromPrimitive;
use serde::{Serialize, Deserialize};
use strum::EnumIter;
use crate::block::Block;

#[derive(Clone, Copy)]
pub enum ItemUsage {
  AsBlock(Block)
}

pub struct ItemDescriptor {
  pub name: &'static str,
  pub usage: Option<ItemUsage>,
  pub stack_size: NonZeroU8,
}

#[derive(Serialize, Deserialize, Clone, Copy, PartialEq, Eq, Hash, Debug, EnumIter, TryFromPrimitive)]
#[repr(u8)]
pub enum Item {
  TestItem,
}

impl Item {
  #[inline]
  pub const fn descriptor(self) -> ItemDescriptor {
    match self {
      Self::TestItem => ItemDescriptor {
        name: "Test Item",
        usage: None,
        stack_size: nz::u8!(32),
      },
    }
  }
}

#[derive(Clone, Copy)]
pub struct ItemCollection(Option<(Item, NonZeroU8)>);

impl ItemCollection {
  /// Create a new item collection with `amount` of `item`
  ///
  /// If `amount` is 0, the slot will be empty, and the item will be ignored
  pub const fn new(item: Item, amount: u8) -> Self {
    if amount == 0 {
      return Self::new_empty()
    }
    // SAFETY: `amount` guaranteed to be non-zero
    let amount = unsafe { NonZeroU8::new_unchecked(amount) };
    Self::new_nonzero(item, amount)
  }

  /// Create a new item collection with `amount` of `item`
  pub const fn new_nonzero(item: Item, amount: NonZeroU8) -> Self {
    Self(Some((item, amount)))
  }

  /// Create a new item collection with a single item
  pub const fn new_single(item: Item) -> Self {
    Self(Some((item, nz::u8!(1))))
  }

  /// Create a new empty item collection
  pub const fn new_empty() -> Self {
    Self(None)
  }

  /// Set the amount of items in the slot to `amount`\
  ///
  /// If `amount` is 0, the slot will be emptied\
  /// If slot is empty, this will do nothing, even if `amount` is non-zero
  pub const fn with_amount(&self, amount: u8) -> Self {
    if amount == 0 {
      return Self::new_empty()
    }
    // SAFETY: `amount` guaranteed to be non-zero
    let amount = unsafe { NonZeroU8::new_unchecked(amount) };
    self.with_amount_nonzero(amount)
  }

  /// Set the amount of items in the slot to `amount`
  ///
  /// If slot is empty, this will do nothing
  pub const fn with_amount_nonzero(&self, amount: NonZeroU8) -> Self {
    Self(match self.0 {
      Some((item, _)) => Some((item, amount)),
      None => None,
    })
  }

  /// Check if the slot is empty (contains no items)
  pub const fn is_empty(&self) -> bool {
    self.0.is_none()
  }

  /// Check if the slot is full (contains the maximum stack size)
  pub const fn is_full(&self) -> bool {
    match self.0 {
      Some((item, amount)) => {
        amount.get() >= item.descriptor().stack_size.get()
      },
      None => false,
    }
  }

  /// Get the item in the slot
  ///
  /// If the slot is empty, returns None
  pub const fn item(&self) -> Option<Item> {
    match self.0 {
      Some((item, _)) => Some(item),
      None => None,
    }
  }

  /// Get the amount of items in the slot
  ///
  /// If the slot is empty, returns 0
  pub const fn amount(&self) -> u8 {
    match self.0 {
      Some((_, amount)) => amount.get(),
      None => 0,
    }
  }

  /// Get the amount of items in the slot
  ///
  /// If the slot is empty, returns None
  pub const fn amount_nonzero(&self) -> Option<NonZeroU8> {
    match self.0 {
      Some((_, amount)) => Some(amount),
      None => None,
    }
  }

  /// Add items from another slot, copying them\
  /// Returns the leftover items (items that could not be added)
  pub fn add(&mut self, from: &Self) -> Self {
    // If there are no items to add, return
    let Some((add_item, add_count)) = from.0 else {
      return Self::new_empty()
    };
    let item_stack_size = add_item.descriptor().stack_size;

    // Add items to the slot
    let (this_slot, leftovers) = match self.0 {
      None => (
        (
          add_item,
          add_count.min(item_stack_size)
        ),
        match add_count > item_stack_size {
          true => Self::new_nonzero(
            add_item,
            NonZeroU8::new(
              add_count.get() - item_stack_size.get()
            ).unwrap(),
          ),
          false => Self::new_empty()
        }
      ),
      Some((cur_item, cur_count)) if cur_item == add_item => {
        let total_count = cur_count.checked_add(add_count.get()).unwrap();
        (
          (
            cur_item,
            total_count.min(item_stack_size),
          ),
          match total_count > item_stack_size {
            true => Self::new_nonzero(
              add_item,
              NonZeroU8::new(
                total_count.get() - item_stack_size.get()
              ).unwrap()
            ),
            false => Self::new_empty()
          }
        )
      },
      // If items are different, do not add anything, everything is leftovers
      _ => return *from,
    };

    self.0 = Some(this_slot);
    leftovers
  }

  /// Move as much as possible items from another slot, removing them
  ///
  /// This may not be possible if the slot is full or contains a different item
  pub fn move_all(&mut self, to: &mut Self) {
    let leftovers = to.add(self);
    *self = leftovers;
  }

  /// Move up to `amount` items from another slot, removing them
  ///
  /// If `amount` is 0, nothing will be moved
  pub fn move_up_to(&mut self, to: &mut Self, limit: u8) {

    if self.is_empty() { return }
    // SAFETY: slot is guaranteed to be non-empty
    let amount = unsafe { self.amount_nonzero().unwrap_unchecked() } ;

    if limit == 0 { return }
    // SAFETY: `limit` guaranteed to be non-zero
    let limit = unsafe { NonZeroU8::new_unchecked(limit) };

    let amount_with_limit = amount.min(limit);
    let self_with_limit = self.with_amount_nonzero(amount_with_limit);

    let mut leftovers = to.add(&self_with_limit);

    // Compensate for the amount of items that were not moved
    let amount_difference = amount.get() - amount_with_limit.get();
    if amount_difference > 0 {
      let correct_item = self.item().unwrap();
      let correct_amount = leftovers.amount() + amount_difference;
      leftovers = Self::new(correct_item, correct_amount);
    }

    *self = leftovers;
  }

  /// Try to move a single item from another slot, removing it
  ///
  /// This may not be possible if the slot is full or contains a different item
  pub fn move_single(&mut self, to: &mut Self) {
    self.move_up_to(to, 1);
  }
}
