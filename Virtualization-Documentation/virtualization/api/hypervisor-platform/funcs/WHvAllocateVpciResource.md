---
title: WHvAllocateVpciResource
description: Learn about the WHvAllocateVpciResource function that allocates a virtual PCI (VPCI) resource to back a virtual device.
author: pbarbuda
ms.author: pbarbuda
ms.date: 06/08/2026
ms.topic: reference
---

# WHvAllocateVpciResource

Allocates a virtual PCI (VPCI) resource to back a virtual device.

## Syntax

```C
typedef enum WHV_ALLOCATE_VPCI_RESOURCE_FLAGS
{
    WHvAllocateVpciResourceFlagNone = 0x00000000,
    WHvAllocateVpciResourceFlagAllowDirectP2P = 0x00000001
} WHV_ALLOCATE_VPCI_RESOURCE_FLAGS;

// Enables bitwise operators on the WHV_ALLOCATE_VPCI_RESOURCE_FLAGS enumeration.
DEFINE_ENUM_FLAG_OPERATORS(WHV_ALLOCATE_VPCI_RESOURCE_FLAGS);

HRESULT
WINAPI
WHvAllocateVpciResource(
    _In_opt_ const GUID* ProviderId,
    _In_ WHV_ALLOCATE_VPCI_RESOURCE_FLAGS Flags,
    _In_reads_opt_(ResourceDescriptorSizeInBytes) const VOID* ResourceDescriptor,
    _In_ UINT32 ResourceDescriptorSizeInBytes,
    _Out_ HANDLE* VpciResource
    );
```

### Parameters

`ProviderId`

Specifies the identifier of the resource provider. Specify `NULL` to allocate an SR-IOV virtual function, a discretely assignable physical function, or a virtual device that is not backed by any physical resources. If not `NULL`, the GUID identifies a registered resource provider installed on the system that handles the allocation.

`Flags`

Specifies system-defined options for the allocated resource, as a combination of [`WHV_ALLOCATE_VPCI_RESOURCE_FLAGS`](WHvVpciDataTypes.md) values.

`ResourceDescriptor`

Specifies a pointer to a buffer that describes the resources to allocate. If `ProviderId` is `NULL` and this parameter is also `NULL`, the function allocates an empty resource. Otherwise, if `ProviderId` is `NULL`, this parameter must point to a [`WHV_SRIOV_RESOURCE_DESCRIPTOR`](WHvVpciDataTypes.md) structure. If `ProviderId` is not `NULL`, this parameter points to an opaque buffer that is passed through unmodified to the resource provider that handles the allocation request.

`ResourceDescriptorSizeInBytes`

Specifies the size, in bytes, of the `ResourceDescriptor` buffer.

`VpciResource`

Receives a handle that represents the allocated resource.

## Return Value

If the function succeeds, the return value is `S_OK`.

If the underlying virtual PCI stack cannot satisfy the requested physical resource allocation, the function returns `HRESULT_FROM_WIN32(ERROR_NOT_FOUND)`.

## Remarks

The `WHvAllocateVpciResource` function allocates the physical or virtual resource that backs a virtual PCI device. After this function returns, pass the resulting handle to [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md) to create a device that is assigned to a partition.

The caller must close the returned handle with `CloseHandle` when it is no longer needed.

## Requirements

| Requirement | Value |
|---|---|
| Minimum supported Windows | Windows 10, version 20H2 (x64); Windows 11, version 24H2, build 26100.3915 (Arm64) |
| Header | WinHvPlatform.h |
| Library | WinHvPlatform.lib |
| DLL | WinHvPlatform.dll |
| Architecture | x64, Arm64 |

## See also

- [`WHvCreateVpciDevice`](WHvCreateVpciDevice.md)
- [`WHvDeleteVpciDevice`](WHvDeleteVpciDevice.md)
- [Virtual PCI Data Types](WHvVpciDataTypes.md)
- [Windows Hypervisor Platform API Definitions](../hypervisor-platform.md)
