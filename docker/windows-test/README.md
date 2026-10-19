# Windows VM Hardware Test Environment

This directory provides an automated environment using [dockurr/windows](https://github.com/dockur/windows) to run native Windows tests against physical USB hardware attached to the host (via KVM + QEMU USB passthrough).

---

## Prerequisites
- Host Linux with KVM enabled (`/dev/kvm`).
- Docker / Docker Compose.
- Connected Aquacomputer OCTO (`0c70:f011`).

---

## How to Run

1. **Start the Windows VM:**
   ```bash
   cd docker/windows-test
   docker compose up -d
   ```

2. **Monitor the Boot / Web Console:**
   Open [http://localhost:8006](http://localhost:8006) in your browser to see the live Windows desktop / console.

3. **Run the Tests:**
   Inside Windows (via Web console, RDP, or terminal), run:
   ```cmd
   C:\workspace\docker\windows-test\run-test.bat
   ```
   Or manually:
   ```cmd
   cd C:\workspace
   gradlew.bat :engine:engine-prod:mingwX64Test -Ptest.hardware=true --info
   ```

4. **Shut down the VM when done:**
   ```bash
   docker compose down
   ```
