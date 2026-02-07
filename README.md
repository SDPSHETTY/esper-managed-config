Esper Managed Configuration & Automated Device Group Movement
📌 Overview

This repository is a reference project and solution accelerator that demonstrates how to:
	•	Use Esper Managed Configurations to inject device metadata into Android applications
	•	Collect runtime attributes (IP address, OS, hardware) on the device
	•	Send device data to a backend service
	•	Automatically move devices between Esper Device Groups using Esper Public APIs
	•	Trigger Blueprint convergence without manual intervention

The project is tenant-agnostic, customer-neutral, and designed to be GitHub-friendly for reuse and learning.

⸻

🎯 Who Should Use This
	•	Android developers integrating Esper Managed Configurations
	•	Backend engineers automating device lifecycle workflows
	•	Solution architects designing Esper-based deployments
	•	Partners and customers looking for a practical reference

⸻

🧠 Key Concepts
	•	Managed Configuration → authoritative device identifiers injected at provisioning time
	•	Backend-first decision making → business logic stays off-device
	•	IP tallying & mapping → handled centrally in the backend
	•	Device Groups → represent desired operational state
	•	Blueprint convergence → enforces configuration consistency

  🗺️ Architecture
  ┌────────────┐
│  Blueprint │
└─────┬──────┘
      │ Managed Config
┌─────▼──────┐
│ Android App│
│  (Device)  │
└─────┬──────┘
      │ Device Metadata
┌─────▼──────┐
│   Backend  │
│  Service   │
└─────┬──────┘
      │ Esper APIs
┌─────▼──────┐
│ Esper Cloud│
│ Device Mgmt│
└────────────┘

Flow:
	1.	Device is provisioned using an Esper Blueprint
	2.	Managed Config injects device identifiers
	3.	App sends runtime metadata to backend
	4.	Backend evaluates rules
	5.	Device is moved to the target group
	6.	Blueprint converges automatically (if linked)

⸻

🚀 Quick Start
	1.	Clone this repository
	2.	Add restrictions.xml to your Android app
	3.	Configure Managed Config in an Esper Blueprint
	4.	Provision a device
	5.	Run the backend service
	6.	Observe automatic device group movement

⸻

🛠️ Android App Setup

Managed Configuration Placeholders

#Create res/xml/restrictions.xml in your Android app:

```xml
<restrictions xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <entry android:key="serialNumber" tools:ignore="ValidRestrictions" />
    <entry android:key="uuid" tools:ignore="ValidRestrictions" />
    <entry android:key="imei1" tools:ignore="ValidRestrictions" />
    <entry android:key="imei2" tools:ignore="ValidRestrictions" />
    <entry android:key="deviceName" tools:ignore="ValidRestrictions" />
    <entry android:key="macAddress" tools:ignore="ValidRestrictions" />

</restrictions>

Use Android’s RestrictionsManager API to retrieve these values at runtime.

🧩 Blueprint Configuration

#Attach the following Managed Configuration JSON to your application inside the Esper Blueprint:

### JSON (Managed Config / Provisioning)
```md
```json
{
  "imei1": "${esper.imei1}",
  "imei2": "${esper.imei2}",
  "serialNumber": "${esper.serialNumber}",
  "macAddress": "${esper.macAddress}",
  "uuid": "${esper.uuid}",
  "deviceName": "${esper.deviceName}"
}

📲 Device Provisioning (Example)

#Replace all placeholders with values from your Esper tenant.

{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "<DPC_PACKAGE>/<DPC_ADMIN_RECEIVER>",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": "<APK_CHECKSUM>",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "<DPC_APK_URL>",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "EID": "<ENTERPRISE_ID>",
    "E": "<TENANT_NAME>",
    "B": "<BLUEPRINT_ID>",
    "AT": "<ACCESS_TOKEN>"
  },
  "android.app.extra.PROVISIONING_LOCALE": "en_US"
}

#🌐 Backend → Esper API Flow

#Design note
#IP address tallying, IP-to-site mapping, geo resolution, and policy logic are intentionally handled on the backend, not on the device.

#Fetch Device Details

GET https://{tenant}-api.esper.cloud/api/device/v0/devices/{device_uuid}/
Authorization: Bearer {access_token}

#Resolve Target Group

GET https://{tenant}-api.esper.cloud/api/enterprise/{enterprise_id}/devicegroup/?name={group_name}
Authorization: Bearer {access_token}

#Move Device to Target Group

PATCH https://{tenant}-api.esper.cloud/api/enterprise/{enterprise_id}/devicegroup/{group_id}/?action=add
Content-Type: application/json
Authorization: Bearer {access_token}

{
  "device_ids": ["{device_uuid}"]
}

If a Blueprint is linked to the group, convergence occurs automatically.

📁 Repository Structure
.
├── android-app/
│   ├── README.md
│   └── res/xml/restrictions.xml
├── backend/
│   ├── esper_client.py
│   ├── device_ingest.py
│   ├── group_logic.py
│   └── config.py
├── docs/
│   ├── architecture.md
│   └── api-flow.md
├── .env.example
├── LICENSE
└── README.md

⚠️ Error Handling & Safety
	•	Verify the current group before moving a device
	•	Ensure idempotent updates
	•	Retry safely on transient (5xx) errors
	•	Log Esper request IDs for traceability

⸻

🔐 Security Best Practices
	•	Never commit access tokens
	•	Use environment variables for secrets
	•	Apply least-privilege API scopes
	•	Rotate tokens regularly

  📎 Disclaimer

This repository is a reference implementation, not an official SDK.
Always validate behavior against the latest Esper API documentation before production use.
