use syn::parse::{Parse, ParseStream};
use syn::{
    Attribute, Expr, ExprArray, ExprLit, Ident, Lit, LitStr, Path, Result as SynResult, Token,
};

pub enum CommandKind {
    Root(CommandAttrs),
    Subcommand(SubcommandAttrs),
}

pub struct CommandAttrs {
    pub name: LitStr,
    pub aliases: Vec<LitStr>,
    pub permission: Option<Path>,
}

pub struct SubcommandAttrs {
    pub permission: Option<Path>,
}

pub enum VariantPrefix {
    Literal(PrefixAttrs),
    Subcommand(PrefixAttrs),
}

pub struct PrefixAttrs {
    pub name: LitStr,
    pub aliases: Vec<LitStr>,
}

pub struct VariantAttrs {
    pub prefix: Option<VariantPrefix>,
    pub permission: Option<Path>,
}

pub fn command_kind(attrs: &[Attribute]) -> SynResult<CommandKind> {
    for attr in attrs {
        if !attr.path().is_ident("command") {
            continue;
        }

        if let Ok(name) = attr.parse_args::<LitStr>() {
            return Ok(CommandKind::Root(CommandAttrs {
                name,
                aliases: Vec::new(),
                permission: None,
            }));
        }

        let mut name = None;
        let mut aliases = Vec::new();
        let mut permission = None;
        let mut subcommand = false;

        attr.parse_nested_meta(|meta| {
            if meta.path.is_ident("name") {
                name = Some(meta.value()?.parse::<LitStr>()?);
                Ok(())
            } else if meta.path.is_ident("aliases") {
                aliases = parse_aliases(meta.value()?.parse::<ExprArray>()?)?;
                Ok(())
            } else if meta.path.is_ident("permission") {
                permission = Some(meta.value()?.parse::<Path>()?);
                Ok(())
            } else if meta.path.is_ident("subcommand") {
                subcommand = true;
                Ok(())
            } else {
                Err(meta.error("unsupported command option"))
            }
        })?;

        return match (name, subcommand) {
            (Some(name), false) => Ok(CommandKind::Root(CommandAttrs {
                name,
                aliases,
                permission,
            })),
            (None, true) => Ok(CommandKind::Subcommand(SubcommandAttrs { permission })),
            (Some(_), true) => Err(syn::Error::new_spanned(
                attr,
                "command cannot be both named and a subcommand",
            )),
            (None, false) => Err(syn::Error::new_spanned(
                attr,
                "expected #[command(\"name\")], #[command(name = \"name\")], or #[command(subcommand)]",
            )),
        };
    }

    Err(syn::Error::new(
        proc_macro2::Span::call_site(),
        "missing #[command(...)] attribute",
    ))
}

pub fn variant_attrs(attrs: &[Attribute]) -> SynResult<VariantAttrs> {
    let mut prefix = None;
    let mut permission = None;

    for attr in attrs {
        let next = if attr.path().is_ident("literal") {
            Some(VariantPrefix::Literal(attr.parse_args::<PrefixAttrs>()?))
        } else if attr.path().is_ident("subcommand") {
            Some(VariantPrefix::Subcommand(attr.parse_args::<PrefixAttrs>()?))
        } else {
            None
        };

        if let Some(next) = next {
            if prefix.is_some() {
                return Err(syn::Error::new_spanned(
                    attr,
                    "command variants can only have one #[literal(...)] or #[subcommand(...)] attribute",
                ));
            }

            prefix = Some(next);
            continue;
        }

        if attr.path().is_ident("permission") {
            if permission.is_some() {
                return Err(syn::Error::new_spanned(
                    attr,
                    "command variants can only have one #[permission(...)] attribute",
                ));
            }

            permission = Some(attr.parse_args::<Path>()?);
        }
    }

    Ok(VariantAttrs { prefix, permission })
}

pub fn permission_attr(attrs: &[Attribute]) -> SynResult<Option<Path>> {
    let mut permission = None;

    for attr in attrs {
        if !attr.path().is_ident("permission") {
            continue;
        }

        if permission.is_some() {
            return Err(syn::Error::new_spanned(
                attr,
                "fields can only have one #[permission(...)] attribute",
            ));
        }

        permission = Some(attr.parse_args::<Path>()?);
    }

    Ok(permission)
}

fn parse_aliases(aliases: ExprArray) -> SynResult<Vec<LitStr>> {
    aliases
        .elems
        .into_iter()
        .map(|expr| match expr {
            Expr::Lit(ExprLit {
                lit: Lit::Str(alias),
                ..
            }) => Ok(alias),
            _ => Err(syn::Error::new_spanned(
                expr,
                "aliases must be string literals",
            )),
        })
        .collect()
}

impl Parse for PrefixAttrs {
    fn parse(input: ParseStream<'_>) -> SynResult<Self> {
        let name = input.parse::<LitStr>()?;
        let mut aliases = Vec::new();

        while input.peek(Token![,]) {
            input.parse::<Token![,]>()?;

            if input.is_empty() {
                break;
            }

            let option = input.parse::<Ident>()?;
            if option != "aliases" {
                return Err(syn::Error::new_spanned(
                    option,
                    "unsupported literal/subcommand option",
                ));
            }

            input.parse::<Token![=]>()?;
            aliases = parse_aliases(input.parse::<ExprArray>()?)?;
        }

        Ok(Self { name, aliases })
    }
}
