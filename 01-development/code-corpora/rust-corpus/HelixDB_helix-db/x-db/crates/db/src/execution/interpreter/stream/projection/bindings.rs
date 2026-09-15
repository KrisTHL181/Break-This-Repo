//! Binding projection contracts.

use super::*;

impl<'db> ExecutionContext<'db> {
    #[cfg(test)]
    pub(in crate::execution::interpreter::stream::projection) async fn binding_projection(
        &self,
        row: &ExecutionRow,
        projection: &ir::BindingProjectionPlan,
    ) -> Result<Option<(String, DbPropertyValue)>> {
        let mut resolver = eval::RowValueResolver::new(self);
        self.binding_projection_with_resolver(row, projection, &mut resolver)
            .await
    }

    pub(in crate::execution::interpreter::stream::projection) async fn binding_projection_with_resolver(
        &self,
        row: &ExecutionRow,
        projection: &ir::BindingProjectionPlan,
        resolver: &mut eval::RowValueResolver<'_, 'db>,
    ) -> Result<Option<(String, DbPropertyValue)>> {
        match projection {
            ir::BindingProjectionPlan::Property {
                target,
                source,
                alias,
            } => {
                let Some((element, virtual_properties)) = self.binding_target(row, target) else {
                    return Ok(None);
                };
                let row =
                    ExecutionRow::current_with_virtual_properties(element, virtual_properties);
                Ok(resolver
                    .row_property(&row, source)
                    .await?
                    .map(|value| (alias.as_ref().to_string(), value)))
            }
            ir::BindingProjectionPlan::Coalesce { refs, alias } => {
                for value_ref in refs.as_ref() {
                    let Some((element, virtual_properties)) =
                        self.binding_target(row, &value_ref.target)
                    else {
                        continue;
                    };
                    let row =
                        ExecutionRow::current_with_virtual_properties(element, virtual_properties);
                    if let Some(value) = resolver.row_property(&row, &value_ref.source).await?
                        && !matches!(value, DbPropertyValue::Null)
                    {
                        return Ok(Some((alias.as_ref().to_string(), value)));
                    }
                }
                Ok(None)
            }
        }
    }

    pub(in crate::execution::interpreter::stream::projection) fn binding_target(
        &self,
        row: &ExecutionRow,
        target: &ir::BindingTargetPlan,
    ) -> Option<(ElementRef, RowVirtualProperties)> {
        match target {
            ir::BindingTargetPlan::Current => row
                .current
                .clone()
                .map(|element| (element, row.virtual_properties.clone())),
            ir::BindingTargetPlan::Binding(name) => {
                row.bindings.get(name).cloned().map(|element| {
                    let virtual_properties = row
                        .binding_virtual_properties
                        .get(name)
                        .cloned()
                        .unwrap_or_else(RowVirtualProperties::empty);
                    (element, virtual_properties)
                })
            }
        }
    }
}
