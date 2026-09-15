//
// Copyright 2024 Signal Messenger, LLC.
// SPDX-License-Identifier: AGPL-3.0-only
//
use std::net::IpAddr;
use std::sync::Arc;

use clap::Parser;
use const_str::ip_addr;
use libsignal_net::infra::certs::RootCertificates;
use libsignal_net::infra::dns::custom_resolver::CustomDnsResolver;
use libsignal_net::infra::dns::dns_lookup::{DnsLookup, DnsLookupRequest};
use libsignal_net::infra::host::Host;
use libsignal_net_infra::dns::dns_transport_doh::DohTransportConnectorFactory;
use libsignal_net_infra::route::{
    HttpRouteFragment, HttpVersion, HttpsTlsRoute, TcpRoute, TlsRoute, TlsRouteFragment,
};
use libsignal_net_infra::timeouts::DNS_LATER_RESPONSE_GRACE_PERIOD;
use libsignal_net_infra::utils::no_network_change_events;
use libsignal_net_infra::{Alpn, OverrideNagleAlgorithm};
use nonzero_ext::nonzero;
use tokio::time::Instant;

#[derive(Parser, Debug)]
struct Args {
    /// domain name to resolve
    #[arg(long)]
    domain: String,
}

#[tokio::main]
async fn main() {
    env_logger::Builder::new()
        .filter_level(log::LevelFilter::Debug)
        .parse_default_env()
        .init();

    let args = Args::parse();
    const HOST_IP: IpAddr = ip_addr!("1.1.1.1");

    let host: Arc<str> = HOST_IP.to_string().into();
    let target = HttpsTlsRoute {
        fragment: HttpRouteFragment {
            host_header: host.clone(),
            path_prefix: "".into(),
            http_version: Some(HttpVersion::Http2),
            front_name: None,
        },
        inner: TlsRoute {
            fragment: TlsRouteFragment {
                root_certs: RootCertificates::Native,
                sni: Host::Domain(host.clone()),
                alpn: Some(Alpn::Http2),
                min_protocol_version: None,
            },
            inner: TcpRoute {
                address: HOST_IP,
                port: nonzero!(443u16),
                override_nagle_algorithm: OverrideNagleAlgorithm::UseSystemDefault,
            },
        },
    };
    let custom_resolver = CustomDnsResolver::new(
        vec![target],
        DohTransportConnectorFactory,
        &no_network_change_events(),
        DNS_LATER_RESPONSE_GRACE_PERIOD,
    );

    let lookup_request = DnsLookupRequest {
        hostname: Arc::from(args.domain.as_str()),
        ipv6_enabled: true,
    };

    // first time making a DNS query
    let started_at = Instant::now();
    let result = custom_resolver.dns_lookup(lookup_request.clone()).await;
    log::info!(
        "DNS lookup result for [{}] after {:?}: {:?}",
        args.domain,
        started_at.elapsed(),
        result
    );

    // second time retrieving from cache
    let started_at = Instant::now();
    let result = custom_resolver.dns_lookup(lookup_request.clone()).await;
    log::info!(
        "DNS lookup result for [{}] after {:?}: {:?}",
        args.domain,
        started_at.elapsed(),
        result
    );
}
