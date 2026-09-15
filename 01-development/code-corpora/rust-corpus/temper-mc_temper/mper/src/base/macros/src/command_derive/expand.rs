use quote::{format_ident, quote};
use syn::{Data, DataEnum, DataStruct, DeriveInput, Ident, LitStr, Result as SynResult};

use super::attrs::{command_kind, variant_attrs, CommandKind, PrefixAttrs, VariantPrefix};
use super::fields::{CommandFields, FieldParse};

pub fn expand(input: DeriveInput) -> SynResult<proc_macro2::TokenStream> {
    let ident = input.ident;
    let command_kind = command_kind(&input.attrs)?;

    match input.data {
        Data::Enum(data_enum) => expand_enum(&ident, command_kind, data_enum),
        Data::Struct(data_struct) => expand_struct(&ident, command_kind, data_struct),
        Data::Union(_) => Err(syn::Error::new(
            ident.span(),
            "Command can only be derived for enums or structs",
        )),
    }
}

fn expand_enum(
    ident: &Ident,
    command_kind: CommandKind,
    data_enum: DataEnum,
) -> SynResult<proc_macro2::TokenStream> {
    let mut parse_arms = Vec::new();
    let mut segment_entries = Vec::new();
    let mut greedy_assertions = Vec::new();
    let mut suggestion_registrations = Vec::new();

    for variant in data_enum.variants {
        let variant_ident = variant.ident;
        let variant_attrs = variant_attrs(&variant.attrs)?;
        let fields = CommandFields::from_fields(variant.fields)?;

        match variant_attrs.prefix.as_ref() {
            Some(VariantPrefix::Subcommand(prefix)) => {
                let ty = fields.single_unnamed_type()?;
                let permission_parse = permission_parse(variant_attrs.permission.as_ref());
                let literal_parse = literal_parse(prefix);
                parse_arms.push(quote! {
                    {
                        let __checkpoint = __reader.checkpoint();
                        let __result = (|| -> Result<Self, ::temper_command_infra::ParseError> {
                            #permission_parse
                            #literal_parse
                            let __subcommand = <#ty as ::temper_command_infra::SubcommandSpec>::parse_reader_with_permissions(__reader, __can_use)?;
                            Ok(Self::#variant_ident(__subcommand))
                        })();

                        match __result {
                            Ok(__command) => return Ok(__command),
                            Err(__err) => {
                                __best_error = Some(match __best_error.take() {
                                    Some(__best) => __best.farthest(__err),
                                    None => __err,
                                });
                                __reader.rewind(__checkpoint);
                            }
                        }
                    }
                });

                segment_entries.push(subcommand_segment_entries(
                    prefix,
                    variant_attrs.permission.as_ref(),
                    ty,
                ));
            }
            _ => {
                let field_parse = FieldParse::new(fields.fields())?;
                greedy_assertions.extend(field_parse.greedy_assertions.clone());
                suggestion_registrations.extend(field_parse.suggestion_registrations.clone());
                let constructor = constructor(ident, &variant_ident, &fields, &field_parse);
                let raw_bindings = &field_parse.raw_bindings;
                let permission_parse = permission_parse(variant_attrs.permission.as_ref());
                let prefix_parse = prefix_parse(variant_attrs.prefix.as_ref());
                let variant_segment_entries = variant_segment_entries(
                    variant_attrs.prefix.as_ref(),
                    variant_attrs.permission.as_ref(),
                    &field_parse.segments,
                );

                parse_arms.push(quote! {
                    {
                        let __checkpoint = __reader.checkpoint();
                        let __result = (|| -> Result<Self, ::temper_command_infra::ParseError> {
                            #permission_parse
                            #prefix_parse
                            #(#raw_bindings)*
                            __reader.expect_end()?;
                            Ok(#constructor)
                        })();

                        match __result {
                            Ok(__command) => return Ok(__command),
                            Err(__err) => {
                                __best_error = Some(match __best_error.take() {
                                    Some(__best) => __best.farthest(__err),
                                    None => __err,
                                });
                                __reader.rewind(__checkpoint);
                            }
                        }
                    }
                });

                segment_entries.push(variant_segment_entries);
            }
        }
    }

    let parse_body = quote! {
        let mut __best_error: Option<::temper_command_infra::ParseError> = None;

        #(#parse_arms)*

        Err(__best_error.unwrap_or_else(|| {
            ::temper_command_infra::ParseError::expected(__reader.cursor(), "command variant")
        }))
    };

    Ok(expand_impl(
        ident,
        command_kind,
        parse_body,
        segment_entries,
        greedy_assertions,
        suggestion_registrations,
    ))
}

fn expand_struct(
    ident: &Ident,
    command_kind: CommandKind,
    data_struct: DataStruct,
) -> SynResult<proc_macro2::TokenStream> {
    let fields = CommandFields::from_fields(data_struct.fields)?;
    let field_parse = FieldParse::new(fields.fields())?;
    let raw_bindings = &field_parse.raw_bindings;
    let constructor = struct_constructor(&fields, &field_parse);

    let parse_body = quote! {
        #(#raw_bindings)*
        __reader.expect_end()?;
        Ok(#constructor)
    };

    let segments = &field_parse.segments;
    let segment_entries = vec![quote! {
        vec![vec![#(#segments),*]]
    }];

    Ok(expand_impl(
        ident,
        command_kind,
        parse_body,
        segment_entries,
        field_parse.greedy_assertions,
        field_parse.suggestion_registrations,
    ))
}

fn expand_impl(
    ident: &Ident,
    command_kind: CommandKind,
    parse_body: proc_macro2::TokenStream,
    segment_entries: Vec<proc_macro2::TokenStream>,
    greedy_assertions: Vec<proc_macro2::TokenStream>,
    suggestion_registrations: Vec<proc_macro2::TokenStream>,
) -> proc_macro2::TokenStream {
    let segment_builder = quote! {
        let mut __segments = Vec::new();
        #(
            __segments.extend(#segment_entries);
        )*
        __segments
    };

    match command_kind {
        CommandKind::Root(command_attrs) => {
            let command_name = command_attrs.name;
            let aliases = command_attrs.aliases;
            let command_permission_parse = permission_parse(command_attrs.permission.as_ref());
            let permission_fn = permission_fn(command_attrs.permission.as_ref());
            let registration = expand_registration(ident, &suggestion_registrations);

            quote! {
                #(#greedy_assertions)*

                impl ::temper_command_infra::CommandSpec for #ident {
                    const NAME: &'static str = #command_name;

                    fn parse_reader(
                        __reader: &mut ::temper_command_infra::CommandReader<'_>,
                    ) -> Result<Self, ::temper_command_infra::ParseError> {
                        let __can_use = |_| true;
                        Self::parse_reader_with_permissions(__reader, &__can_use)
                    }

                    fn parse_reader_with_permissions(
                        __reader: &mut ::temper_command_infra::CommandReader<'_>,
                        __can_use: &dyn Fn(::temper_command_infra::Permissions) -> bool,
                    ) -> Result<Self, ::temper_command_infra::ParseError> {
                        #command_permission_parse
                        #parse_body
                    }

                    fn aliases() -> &'static [&'static str] {
                        &[#(#aliases),*]
                    }

                    #permission_fn

                    fn paths() -> Vec<::temper_command_infra::CommandPath> {
                        #segment_builder
                            .into_iter()
                            .map(|__segments| {
                                ::temper_command_infra::CommandPath::new(#command_name, __segments)
                            })
                            .collect()
                    }
                }

                #registration
            }
        }
        CommandKind::Subcommand(subcommand_attrs) => {
            let permission_parse = permission_parse(subcommand_attrs.permission.as_ref());
            let subcommand_segments = subcommand_segments_with_permission(
                subcommand_attrs.permission.as_ref(),
                segment_builder,
            );
            let suggestion_registration =
                expand_suggestion_registration(ident, &suggestion_registrations);

            quote! {
                #(#greedy_assertions)*

                impl ::temper_command_infra::SubcommandSpec for #ident {
                    fn parse_reader(
                        __reader: &mut ::temper_command_infra::CommandReader<'_>,
                    ) -> Result<Self, ::temper_command_infra::ParseError> {
                        let __can_use = |_| true;
                        Self::parse_reader_with_permissions(__reader, &__can_use)
                    }

                    fn parse_reader_with_permissions(
                        __reader: &mut ::temper_command_infra::CommandReader<'_>,
                        __can_use: &dyn Fn(::temper_command_infra::Permissions) -> bool,
                    ) -> Result<Self, ::temper_command_infra::ParseError> {
                        #permission_parse
                        #parse_body
                    }

                    fn segments() -> Vec<Vec<::temper_command_infra::CommandPathSegment>> {
                        #subcommand_segments
                    }
                }

                #suggestion_registration
            }
        }
    }
}

fn expand_registration(
    ident: &Ident,
    suggestion_registrations: &[proc_macro2::TokenStream],
) -> proc_macro2::TokenStream {
    let register_fn = format_ident!("__{}_register_command", ident);
    let register_system_fn = format_ident!("__{}_register_command_system", ident);
    let suggestion_registration = expand_suggestion_registration(ident, suggestion_registrations);

    quote! {
        #[::temper_command_infra::ctor::ctor(unsafe)]
        #[allow(non_snake_case)]
        #[doc(hidden)]
        fn #register_fn() {
            ::temper_command_infra::register_static_command(
                ::temper_command_infra::RegisteredCommand::of::<#ident>(),
            );
        }

        #[::temper_command_infra::ctor::ctor(unsafe)]
        #[allow(non_snake_case)]
        #[doc(hidden)]
        fn #register_system_fn() {
            ::temper_command_infra::add_system(
                ::temper_command_infra::dispatch_command::<#ident>,
            );
        }

        #suggestion_registration
    }
}

fn expand_suggestion_registration(
    ident: &Ident,
    suggestion_registrations: &[proc_macro2::TokenStream],
) -> proc_macro2::TokenStream {
    let register_suggestions_fn = format_ident!("__{}_register_command_arg_suggestions", ident);

    quote! {
        #[::temper_command_infra::ctor::ctor(unsafe)]
        #[allow(non_snake_case)]
        #[doc(hidden)]
        fn #register_suggestions_fn() {
            #(#suggestion_registrations)*
        }
    }
}

fn constructor(
    ident: &Ident,
    variant_ident: &Ident,
    fields: &CommandFields,
    field_parse: &FieldParse,
) -> proc_macro2::TokenStream {
    match fields {
        CommandFields::Unnamed(_) => {
            let values = &field_parse.tuple_values;
            quote! {
                #ident::#variant_ident(#(#values),*)
            }
        }
        CommandFields::Named(_) => {
            let values = &field_parse.named_values;
            quote! {
                #ident::#variant_ident { #(#values),* }
            }
        }
        CommandFields::Unit => quote! {
            #ident::#variant_ident
        },
    }
}

fn struct_constructor(
    fields: &CommandFields,
    field_parse: &FieldParse,
) -> proc_macro2::TokenStream {
    match fields {
        CommandFields::Unnamed(_) => {
            let values = &field_parse.tuple_values;
            quote! {
                Self(#(#values),*)
            }
        }
        CommandFields::Named(_) => {
            let values = &field_parse.named_values;
            quote! {
                Self { #(#values),* }
            }
        }
        CommandFields::Unit => quote! {
            Self
        },
    }
}

fn prefix_parse(prefix: Option<&VariantPrefix>) -> proc_macro2::TokenStream {
    match prefix {
        Some(VariantPrefix::Literal(prefix)) => literal_parse(prefix),
        Some(VariantPrefix::Subcommand(_)) | None => quote! {},
    }
}

fn variant_segment_entries(
    prefix: Option<&VariantPrefix>,
    permission: Option<&syn::Path>,
    segments: &[proc_macro2::TokenStream],
) -> proc_macro2::TokenStream {
    match prefix {
        Some(VariantPrefix::Literal(prefix)) => {
            let literal_segments = literal_segments(prefix, permission);
            let trailing_segments = quote! {
                vec![#(#segments),*]
            };

            quote! {
                vec![
                    #({
                        let mut __segments = vec![#literal_segments];
                        __segments.extend(#trailing_segments);
                        __segments
                    }),*
                ]
            }
        }
        Some(VariantPrefix::Subcommand(_)) | None => {
            quote! {
                vec![vec![#(#segments),*]]
            }
        }
    }
}

fn subcommand_segment_entries(
    prefix: &PrefixAttrs,
    permission: Option<&syn::Path>,
    ty: &syn::Type,
) -> proc_macro2::TokenStream {
    let literal_segments = literal_segments(prefix, permission);

    quote! {
        {
            let __subcommand_paths = <#ty as ::temper_command_infra::SubcommandSpec>::segments();
            let mut __paths = Vec::new();

            #(
                __paths.extend(__subcommand_paths.iter().cloned().map(|mut __segments| {
                    let mut __path = vec![#literal_segments];
                    __path.append(&mut __segments);
                    __path
                }));
            )*

            __paths
        }
    }
}

fn literal_segments(
    prefix: &PrefixAttrs,
    permission: Option<&syn::Path>,
) -> Vec<proc_macro2::TokenStream> {
    std::iter::once(&prefix.name)
        .chain(prefix.aliases.iter())
        .map(|literal| literal_segment(literal, permission))
        .collect()
}

fn literal_segment(literal: &LitStr, permission: Option<&syn::Path>) -> proc_macro2::TokenStream {
    let segment = quote! {
        ::temper_command_infra::CommandPathSegment::literal(#literal)
    };

    match permission {
        Some(permission) => quote! {
            #segment.with_permission(#permission)
        },
        None => segment,
    }
}

fn permission_parse(permission: Option<&syn::Path>) -> proc_macro2::TokenStream {
    match permission {
        Some(permission) => quote! {
            if !__can_use(#permission) {
                return Err(::temper_command_infra::ParseError::new(
                    __reader.cursor(),
                    "permission",
                    "you do not have permission to use this command path",
                ));
            }
        },
        None => quote! {},
    }
}

fn permission_fn(permission: Option<&syn::Path>) -> proc_macro2::TokenStream {
    match permission {
        Some(permission) => quote! {
            fn permission() -> Option<::temper_command_infra::Permissions> {
                Some(#permission)
            }
        },
        None => quote! {},
    }
}

fn subcommand_segments_with_permission(
    permission: Option<&syn::Path>,
    segment_builder: proc_macro2::TokenStream,
) -> proc_macro2::TokenStream {
    match permission {
        Some(permission) => quote! {
            let mut __segments = #segment_builder;
            for __path in &mut __segments {
                if let Some(__first) = __path.first_mut() {
                    *__first = __first.clone().with_permission(#permission);
                }
            }
            __segments
        },
        None => segment_builder,
    }
}

fn literal_parse(prefix: &PrefixAttrs) -> proc_macro2::TokenStream {
    let literal = &prefix.name;
    let aliases = &prefix.aliases;

    quote! {
        let __literal_cursor = __reader.cursor();
        let __actual_literal = __reader.read_word_span()?;
        if __actual_literal != #literal #(&& __actual_literal != #aliases)* {
            return Err(::temper_command_infra::ParseError::new(
                __literal_cursor,
                #literal,
                format!("expected literal {}", #literal),
            ));
        }
    }
}
