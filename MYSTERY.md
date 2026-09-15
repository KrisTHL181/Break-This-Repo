# 🔐 Classified Repository Control Map

> [!WARNING]
> The following diagram was recovered from the repository after the incident.

```mermaid
flowchart TD
    A["Visitor opens repository"] --> B{"Trust the contents?"}
    B -->|"Yes"| C["Critical mistake"]
    B -->|"No"| D["Open a PR anyway"]
    C --> E["Repository becomes more broken"]
    D --> E
    E --> F["Cat promoted to maintainer"]
    F --> A

    style C fill:#bf8700,color:#ffffff
    style E fill:#cf222e,color:#ffffff
    style F fill:#8250df,color:#ffffff
```

<details>
<summary><strong>View classified incident log</strong></summary>

```text
[00:00:01] Repository initialized
[00:00:02] First PR detected
[00:00:03] Human control lost
[00:00:04] Cat promoted to administrator
[00:00:05] Documentation no longer trustworthy
```

![Current administrator](./cat.jpeg)

</details>
