# Esper Managed Configuration & Automated Device Group Movement

## 📌 Overview

This repository is a **reference project and solution accelerator** that demonstrates how to:

- Use Esper Managed Configurations to inject device metadata into Android applications
- Collect runtime attributes (IP address, OS, hardware) on the device
- Send device data to a backend service
- Automatically move devices between Esper Device Groups using Esper Public APIs
- Trigger Blueprint convergence without manual intervention

The project is tenant-agnostic, customer-neutral, and GitHub-friendly.

---

## 🎯 Who Should Use This

- Android developers integrating Esper Managed Configurations
- Backend engineers automating device lifecycle workflows
- Solution architects designing Esper-based deployments
- Partners and customers looking for a practical reference

---

## 🧠 Key Concepts

- Managed Configuration → authoritative device identifiers
- Backend-first decision making → logic stays off-device
- IP tallying & mapping → handled in backend
- Device Groups → desired state
- Blueprint convergence → configuration enforcement

---

## 🗺️ Architecture

```text
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
```

## 🚀 Quick Start
	1. Clone the repository
	2. Add `restrictions.xml` to your Android app
	3. Configure Managed Config in an Esper Blueprint
	4. Provision a device
	5. Run the backend service

---

## 🛠️ Android App Setup

Create `res/xml/restrictions.xml`:

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
```
## 🧩 Managed Configuration (Blueprint)

```
{
  "imei1": "${esper.imei1}",
  "imei2": "${esper.imei2}",
  "serialNumber": "${esper.serialNumber}",
  "macAddress": "${esper.macAddress}",
  "uuid": "${esper.uuid}",
  "deviceName": "${esper.deviceName}"
}
```
Note on IP Address Handling
Managed Configuration is intended for static identifiers (UUID, serial number, MAC, IMEI).
IP addresses are dynamic and may change after provisioning, so the application must always use Android networking APIs to fetch the current IP at runtime and report it to the backend.
Even if an IP were included in Managed Config, runtime validation would still be required.

## 🌐 Backend → Esper API Flow

IP mapping and policy logic must be handled on the backend.

### Get Device

```
GET https://{tenant}-api.esper.cloud/api/device/v0/devices/{device_uuid}/
Authorization: Bearer {access_token}
```
### Move Device

```
PATCH https://{tenant}-api.esper.cloud/api/enterprise/{enterprise_id}/devicegroup/{group_id}/?action=add
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "device_ids": ["{device_uuid}"]
}
```
## 🔄 Manual Blueprint Convergence (When Group Is Not Linked)

If the **target Device Group does NOT have a Blueprint linked**, moving a device to that group **will not automatically trigger Blueprint convergence**.

In this scenario, the backend must **explicitly trigger convergence** after the group move.

---

### When Is This Required?

- Target device group has **no Blueprint linked**
- Device is moved to the group via Esper API
- Desired configuration must still be applied immediately

---

### Manual Converge API Call

Trigger Blueprint convergence explicitly from the backend:

```bash
POST https://{tenant}-api.esper.cloud/api/v2/converge
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "device_id": "{device_id}",
  "converge_with_provision_option": false,
  "schedule_type": "IMMEDIATE",
  "schedule_args": {}
}
```
## 📎 Disclaimer

This is a reference implementation.
Always validate against the latest Esper API documentation.
