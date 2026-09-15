//! Schema-derived ignored-field collection for migrated policy documents.

use std::cell::RefCell;
use std::fmt;
use std::rc::Rc;

use serde::de::value::StringDeserializer;
use serde::de::{
    self, DeserializeSeed, Deserializer, EnumAccess, MapAccess, SeqAccess, VariantAccess, Visitor,
};
use serde::Deserialize;

/// Deserialize through the production [`crate::policy::Policy`] model and
/// return every path that Serde ignored as an unknown field.
pub(crate) fn collect(value: serde_yaml::Value) -> Result<Vec<String>, serde_yaml::Error> {
    let ignored = Rc::new(RefCell::new(Vec::new()));
    crate::policy::Policy::deserialize(TrackingDeserializer::new(
        value,
        Path::default(),
        Rc::clone(&ignored),
    ))?;

    let mut paths = ignored.borrow().clone();
    paths.sort();
    paths.dedup();
    Ok(paths)
}

#[derive(Clone, Default)]
struct Path(String);

impl Path {
    fn field(&self, field: &str) -> Self {
        if self.0.is_empty() {
            Self(field.to_string())
        } else {
            Self(format!("{}.{field}", self.0))
        }
    }

    fn index(&self, index: usize) -> Self {
        Self(format!("{}[{index}]", self.0))
    }
}

struct TrackingDeserializer<D> {
    delegate: D,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
}

impl<D> TrackingDeserializer<D> {
    fn new(delegate: D, path: Path, ignored: Rc<RefCell<Vec<String>>>) -> Self {
        Self {
            delegate,
            path,
            ignored,
        }
    }
}

macro_rules! deserialize_with_tracking {
    ($method:ident) => {
        fn $method<V>(self, visitor: V) -> Result<V::Value, Self::Error>
        where
            V: Visitor<'de>,
        {
            self.delegate.$method(TrackingVisitor::new(
                visitor,
                self.path,
                self.ignored,
            ))
        }
    };
    ($method:ident, $($arg:ident: $ty:ty),+ $(,)?) => {
        fn $method<V>(self, $($arg: $ty,)+ visitor: V) -> Result<V::Value, Self::Error>
        where
            V: Visitor<'de>,
        {
            self.delegate.$method($($arg,)+ TrackingVisitor::new(
                visitor,
                self.path,
                self.ignored,
            ))
        }
    };
}

impl<'de, D> Deserializer<'de> for TrackingDeserializer<D>
where
    D: Deserializer<'de>,
{
    type Error = D::Error;

    deserialize_with_tracking!(deserialize_any);
    deserialize_with_tracking!(deserialize_bool);
    deserialize_with_tracking!(deserialize_i8);
    deserialize_with_tracking!(deserialize_i16);
    deserialize_with_tracking!(deserialize_i32);
    deserialize_with_tracking!(deserialize_i64);
    deserialize_with_tracking!(deserialize_i128);
    deserialize_with_tracking!(deserialize_u8);
    deserialize_with_tracking!(deserialize_u16);
    deserialize_with_tracking!(deserialize_u32);
    deserialize_with_tracking!(deserialize_u64);
    deserialize_with_tracking!(deserialize_u128);
    deserialize_with_tracking!(deserialize_f32);
    deserialize_with_tracking!(deserialize_f64);
    deserialize_with_tracking!(deserialize_char);
    deserialize_with_tracking!(deserialize_str);
    deserialize_with_tracking!(deserialize_string);
    deserialize_with_tracking!(deserialize_bytes);
    deserialize_with_tracking!(deserialize_byte_buf);
    deserialize_with_tracking!(deserialize_option);
    deserialize_with_tracking!(deserialize_unit);
    deserialize_with_tracking!(deserialize_unit_struct, name: &'static str);
    deserialize_with_tracking!(deserialize_newtype_struct, name: &'static str);
    deserialize_with_tracking!(deserialize_seq);
    deserialize_with_tracking!(deserialize_tuple, len: usize);
    deserialize_with_tracking!(deserialize_tuple_struct, name: &'static str, len: usize);
    deserialize_with_tracking!(deserialize_map);
    deserialize_with_tracking!(
        deserialize_struct,
        name: &'static str,
        fields: &'static [&'static str],
    );
    deserialize_with_tracking!(
        deserialize_enum,
        name: &'static str,
        variants: &'static [&'static str],
    );
    deserialize_with_tracking!(deserialize_identifier);

    fn deserialize_ignored_any<V>(self, visitor: V) -> Result<V::Value, Self::Error>
    where
        V: Visitor<'de>,
    {
        if !self.path.0.is_empty() {
            self.ignored.borrow_mut().push(self.path.0);
        }
        self.delegate.deserialize_ignored_any(visitor)
    }

    fn is_human_readable(&self) -> bool {
        self.delegate.is_human_readable()
    }
}

struct TrackingVisitor<V> {
    delegate: V,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
}

impl<V> TrackingVisitor<V> {
    fn new(delegate: V, path: Path, ignored: Rc<RefCell<Vec<String>>>) -> Self {
        Self {
            delegate,
            path,
            ignored,
        }
    }
}

macro_rules! forward_visit_value {
    ($method:ident, $ty:ty) => {
        fn $method<E>(self, value: $ty) -> Result<Self::Value, E>
        where
            E: de::Error,
        {
            self.delegate.$method(value)
        }
    };
}

impl<'de, V> Visitor<'de> for TrackingVisitor<V>
where
    V: Visitor<'de>,
{
    type Value = V::Value;

    fn expecting(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        self.delegate.expecting(formatter)
    }

    forward_visit_value!(visit_bool, bool);
    forward_visit_value!(visit_i8, i8);
    forward_visit_value!(visit_i16, i16);
    forward_visit_value!(visit_i32, i32);
    forward_visit_value!(visit_i64, i64);
    forward_visit_value!(visit_i128, i128);
    forward_visit_value!(visit_u8, u8);
    forward_visit_value!(visit_u16, u16);
    forward_visit_value!(visit_u32, u32);
    forward_visit_value!(visit_u64, u64);
    forward_visit_value!(visit_u128, u128);
    forward_visit_value!(visit_f32, f32);
    forward_visit_value!(visit_f64, f64);
    forward_visit_value!(visit_char, char);
    forward_visit_value!(visit_string, String);
    forward_visit_value!(visit_byte_buf, Vec<u8>);

    fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_str(value)
    }

    fn visit_borrowed_str<E>(self, value: &'de str) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_borrowed_str(value)
    }

    fn visit_bytes<E>(self, value: &[u8]) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_bytes(value)
    }

    fn visit_borrowed_bytes<E>(self, value: &'de [u8]) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_borrowed_bytes(value)
    }

    fn visit_none<E>(self) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_none()
    }

    fn visit_some<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
    where
        D: Deserializer<'de>,
    {
        self.delegate.visit_some(TrackingDeserializer::new(
            deserializer,
            self.path,
            self.ignored,
        ))
    }

    fn visit_unit<E>(self) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        self.delegate.visit_unit()
    }

    fn visit_newtype_struct<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
    where
        D: Deserializer<'de>,
    {
        self.delegate
            .visit_newtype_struct(TrackingDeserializer::new(
                deserializer,
                self.path,
                self.ignored,
            ))
    }

    fn visit_seq<A>(self, sequence: A) -> Result<Self::Value, A::Error>
    where
        A: SeqAccess<'de>,
    {
        self.delegate.visit_seq(TrackingSeqAccess {
            delegate: sequence,
            path: self.path,
            ignored: self.ignored,
            next_index: 0,
        })
    }

    fn visit_map<A>(self, map: A) -> Result<Self::Value, A::Error>
    where
        A: MapAccess<'de>,
    {
        self.delegate.visit_map(TrackingMapAccess {
            delegate: map,
            path: self.path,
            ignored: self.ignored,
            next_key: None,
        })
    }

    fn visit_enum<A>(self, data: A) -> Result<Self::Value, A::Error>
    where
        A: EnumAccess<'de>,
    {
        self.delegate.visit_enum(TrackingEnumAccess {
            delegate: data,
            path: self.path,
            ignored: self.ignored,
        })
    }
}

struct TrackingSeed<S> {
    delegate: S,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
}

impl<'de, S> DeserializeSeed<'de> for TrackingSeed<S>
where
    S: DeserializeSeed<'de>,
{
    type Value = S::Value;

    fn deserialize<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
    where
        D: Deserializer<'de>,
    {
        self.delegate.deserialize(TrackingDeserializer::new(
            deserializer,
            self.path,
            self.ignored,
        ))
    }
}

struct TrackingSeqAccess<A> {
    delegate: A,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
    next_index: usize,
}

impl<'de, A> SeqAccess<'de> for TrackingSeqAccess<A>
where
    A: SeqAccess<'de>,
{
    type Error = A::Error;

    fn next_element_seed<T>(&mut self, seed: T) -> Result<Option<T::Value>, Self::Error>
    where
        T: DeserializeSeed<'de>,
    {
        let result = self.delegate.next_element_seed(TrackingSeed {
            delegate: seed,
            path: self.path.index(self.next_index),
            ignored: Rc::clone(&self.ignored),
        })?;
        if result.is_some() {
            self.next_index += 1;
        }
        Ok(result)
    }

    fn size_hint(&self) -> Option<usize> {
        self.delegate.size_hint()
    }
}

struct TrackingMapAccess<A> {
    delegate: A,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
    next_key: Option<String>,
}

impl<'de, A> MapAccess<'de> for TrackingMapAccess<A>
where
    A: MapAccess<'de>,
{
    type Error = A::Error;

    fn next_key_seed<K>(&mut self, seed: K) -> Result<Option<K::Value>, Self::Error>
    where
        K: DeserializeSeed<'de>,
    {
        let Some(key) = self.delegate.next_key::<String>()? else {
            return Ok(None);
        };
        self.next_key = Some(key.clone());
        seed.deserialize(StringDeserializer::<Self::Error>::new(key))
            .map(Some)
    }

    fn next_value_seed<V>(&mut self, seed: V) -> Result<V::Value, Self::Error>
    where
        V: DeserializeSeed<'de>,
    {
        let key = self.next_key.take().unwrap_or_else(|| "?".to_string());
        self.delegate.next_value_seed(TrackingSeed {
            delegate: seed,
            path: self.path.field(&key),
            ignored: Rc::clone(&self.ignored),
        })
    }

    fn size_hint(&self) -> Option<usize> {
        self.delegate.size_hint()
    }
}

struct TrackingEnumAccess<A> {
    delegate: A,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
}

impl<'de, A> EnumAccess<'de> for TrackingEnumAccess<A>
where
    A: EnumAccess<'de>,
{
    type Error = A::Error;
    type Variant = TrackingVariantAccess<A::Variant>;

    fn variant_seed<V>(self, seed: V) -> Result<(V::Value, Self::Variant), Self::Error>
    where
        V: DeserializeSeed<'de>,
    {
        let (value, variant) = self.delegate.variant_seed(seed)?;
        Ok((
            value,
            TrackingVariantAccess {
                delegate: variant,
                path: self.path,
                ignored: self.ignored,
            },
        ))
    }
}

struct TrackingVariantAccess<A> {
    delegate: A,
    path: Path,
    ignored: Rc<RefCell<Vec<String>>>,
}

impl<'de, A> VariantAccess<'de> for TrackingVariantAccess<A>
where
    A: VariantAccess<'de>,
{
    type Error = A::Error;

    fn unit_variant(self) -> Result<(), Self::Error> {
        self.delegate.unit_variant()
    }

    fn newtype_variant_seed<T>(self, seed: T) -> Result<T::Value, Self::Error>
    where
        T: DeserializeSeed<'de>,
    {
        self.delegate.newtype_variant_seed(TrackingSeed {
            delegate: seed,
            path: self.path,
            ignored: self.ignored,
        })
    }

    fn tuple_variant<V>(self, len: usize, visitor: V) -> Result<V::Value, Self::Error>
    where
        V: Visitor<'de>,
    {
        self.delegate
            .tuple_variant(len, TrackingVisitor::new(visitor, self.path, self.ignored))
    }

    fn struct_variant<V>(
        self,
        fields: &'static [&'static str],
        visitor: V,
    ) -> Result<V::Value, Self::Error>
    where
        V: Visitor<'de>,
    {
        self.delegate.struct_variant(
            fields,
            TrackingVisitor::new(visitor, self.path, self.ignored),
        )
    }
}
