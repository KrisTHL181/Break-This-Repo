//! Expression evaluation contracts.

use super::*;

impl<'db> ExecutionContext<'db> {
    pub(in crate::execution::interpreter) async fn eval_expr(
        &self,
        row: &ExecutionRow,
        expr: &Expr,
    ) -> Result<DbPropertyValue> {
        let mut resolver = RowValueResolver::new(self);
        self.eval_expr_with_resolver(row, expr, &mut resolver).await
    }

    pub(in crate::execution::interpreter::stream) async fn eval_expr_with_resolver(
        &self,
        row: &ExecutionRow,
        expr: &Expr,
        resolver: &mut RowValueResolver<'_, 'db>,
    ) -> Result<DbPropertyValue> {
        match expr {
            Expr::Property(property) => {
                let property = ir::NonEmptyString::new(property.clone()).ok_or_else(|| {
                    HelixDbError::Query("expression property name must not be empty".to_string())
                })?;
                Ok(resolver
                    .row_property(row, &property)
                    .await?
                    .unwrap_or(DbPropertyValue::Null))
            }
            Expr::Id => row
                .current
                .as_ref()
                .map(|element| DbPropertyValue::I64(element.id().try_into().unwrap_or(i64::MAX)))
                .ok_or_else(|| HelixDbError::Query("id expression has no current element".into())),
            Expr::Timestamp => Ok(DbPropertyValue::I64(chrono::Utc::now().timestamp_millis())),
            Expr::DateTimeNow => Ok(DbPropertyValue::DateTime(
                chrono::Utc::now().timestamp_millis(),
            )),
            Expr::Constant(value) => Ok(super::super::values::ast_to_db_value(value.clone())),
            Expr::Param(name) => {
                let name = ir::NonEmptyString::new(name.clone()).ok_or_else(|| {
                    HelixDbError::Query("expression parameter name must not be empty".to_string())
                })?;
                self.param_value(&name)
            }
            Expr::Add { left, right } => self.numeric_binary_values(
                Box::pin(self.eval_expr_with_resolver(row, left, resolver)).await?,
                Box::pin(self.eval_expr_with_resolver(row, right, resolver)).await?,
                |l, r| l + r,
            ),
            Expr::Sub { left, right } => self.numeric_binary_values(
                Box::pin(self.eval_expr_with_resolver(row, left, resolver)).await?,
                Box::pin(self.eval_expr_with_resolver(row, right, resolver)).await?,
                |l, r| l - r,
            ),
            Expr::Mul { left, right } => self.numeric_binary_values(
                Box::pin(self.eval_expr_with_resolver(row, left, resolver)).await?,
                Box::pin(self.eval_expr_with_resolver(row, right, resolver)).await?,
                |l, r| l * r,
            ),
            Expr::Div { left, right } => self.numeric_binary_values(
                Box::pin(self.eval_expr_with_resolver(row, left, resolver)).await?,
                Box::pin(self.eval_expr_with_resolver(row, right, resolver)).await?,
                |l, r| l / r,
            ),
            Expr::Mod { left, right } => {
                let left = Box::pin(self.eval_expr_with_resolver(row, left, resolver))
                    .await?
                    .as_i64()
                    .ok_or_else(|| {
                        HelixDbError::Query("mod left expression must be i64".to_string())
                    })?;
                let right = Box::pin(self.eval_expr_with_resolver(row, right, resolver))
                    .await?
                    .as_i64()
                    .ok_or_else(|| {
                        HelixDbError::Query("mod right expression must be i64".to_string())
                    })?;
                Ok(DbPropertyValue::I64(left % right))
            }
            Expr::Neg { expr } => {
                let value = Box::pin(self.eval_expr_with_resolver(row, expr, resolver)).await?;
                if let Some(value) = value.as_i64() {
                    Ok(DbPropertyValue::I64(-value))
                } else if let Some(value) = value.as_f64() {
                    Ok(DbPropertyValue::F64(-value))
                } else {
                    Err(HelixDbError::Query(
                        "neg expression must be numeric".to_string(),
                    ))
                }
            }
            Expr::Case {
                when_then,
                else_expr,
            } => {
                for branch in when_then {
                    if Box::pin(self.eval_predicate_with_resolver(row, &branch.when, resolver))
                        .await?
                    {
                        return Box::pin(self.eval_expr_with_resolver(row, &branch.then, resolver))
                            .await;
                    }
                }
                match else_expr {
                    Some(expr) => Box::pin(self.eval_expr_with_resolver(row, expr, resolver)).await,
                    None => Ok(DbPropertyValue::Null),
                }
            }
        }
    }
}
