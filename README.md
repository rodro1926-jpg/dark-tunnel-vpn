# Dark Tunnel VPN — Production Android Client

[![Android Build CI](https://github.com/your-username/dark-tunnel-vpn/actions/workflows/android.yml/badge.svg)](https://github.com/your-username/dark-tunnel-vpn/actions/workflows/android.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024%2B%29-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-black.svg)](LICENSE)

A high-performance, dark-themed Android VPN client and network tunneling application supporting SSH, SSH over SSL/TLS (Stunnel), SSH with custom HTTP Request Payloads, Xray VMess, Xray VLESS, Trojan, Shadowsocks, SOCKS5, and DNSTT / SlowDNS protocols.

---

## Screenshots & Interface

```
+-------------------------------------------------------------+
|                     DARK TUNNEL VPN                         |
|                 SECURE NETWORK PROTOCOL                     |
|                                                             |
|   [ Selected Server: Bangladesh Premium (BD-01) ]           |
|                                                             |
|                     /-------------\                         |
|                    /   (( o ))     \                        |
|                   |    CONNECTED    |                       |
|                    \   (TAP STOP)  /                        |
|                     \-------------/                         |
|                                                             |
|   [ DOWNLOAD: 1.2 MB/s ]       [ UPLOAD: 340 KB/s ]         |
|   [ DURATION: 00:14:28 ]       [ PING: 42 ms ]              |
|   [ PUBLIC IP: 104.28.19.12 ]                               |
|                                                             |
|   > LIVE TUNNEL LOGS: [INFO] Packet routing active          |
|                                                             |
|    [Tunnel]   [Configs]   [Payload]   [Tools]   [Settings]  |
+-------------------------------------------------------------+
```

---

## Core Feature Overview

### 1. Main Dashboard
- **Cyber Dark Interface:** Neon cyan, emerald green, and deep obsidian dark design engineered for maximum readability.
- **Large Action Button:** Multi-state glowing pulse animation reflecting live states: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`, and `ERROR`.
- **Live Metrics:** Real-time download/upload speed meters (`KB/s`, `MB/s`), session data counters, duration timer, and server round-trip latency.
- **Public IP Inspector:** Detects external public IP address and matches against local subnet hunts.
- **Live Logs Console:** Real-time terminal output with severity filters (`INFO`, `WARN`, `ERROR`, `SUCCESS`).

### 2. Supported Tunnel Protocols
- **SSH Direct:** RFC 4253 TCP socket identification and tunnel forwarder.
- **SSH over SSL/TLS:** Full TLS handshake wrapper with custom SNI (Server Name Indication).
- **SSH + Custom HTTP Payload:** Connects via proxy or direct IP, formats and injects customized HTTP headers (`CONNECT`, `GET`, `Host`, `X-Online-Host`), parses HTTP 200 response, and upgrades to an encrypted SSH stream.
- **DNSTT / SlowDNS:** DNS tunnel routing UDP queries via selected DNS resolvers.
- **Xray VMess:** VMess protocol implementation with WebSocket framing and TLS support.
- **Xray VLESS:** Lightweight VLESS protocol framing with UUID identification.
- **Trojan:** Trojan client protocol with SHA-224 password authentication.
- **Shadowsocks:** Stream and AEAD cipher support.
- **SOCKS5 Proxy:** RFC 1928 client authentication and tunneling.
- **Hysteria / Custom UDP:** UDP socket tunnel forwarding.

### 3. Configuration Management & Provider Mode
- **Import / Export:** Supports `vmess://`, `vless://`, `trojan://`, `ss://`, `ssh://`, JSON, and `.conf` files.
- **Provider Encryption:** Locks sensitive server details using AES-256-GCM encryption with optional password protection into `.dark` packages.
- **Validation:** Automatic checking of hosts, port ranges (1–65535), and expiration dates prior to initiating connections.

### 4. HTTP Payload Generator
- **Injection Methods:** Normal, Front Inject, Back Inject, Split, Double Split.
- **HTTP Verbs:** `CONNECT`, `GET`, `POST`, `HEAD`, `PUT`, `OPTIONS`, `TRACE`, `PATCH`.
- **Dynamic Macros:** `[host]`, `[port]`, `[host_port]`, `[protocol]`, `[cr]`, `[lf]`, `[crlf]`, `[split]`, `[ua]`.
- **Pre-configured Templates:** Cloudflare WebSocket, Direct HTTP CONNECT, Front Split.

### 5. Network Diagnostic Tools
- **Host / SNI Checker:** Direct or proxy-routed HTTP/HTTPS requests with status code inspection.
- **IP Hunter:** Subnet watcher that alerts when your cellular/Wi-Fi IP matches target ISP prefixes (e.g. `10.`, `100.`, `172.`).
- **Ping Tester:** TCP socket ping measuring min/max/average latency.
- **DNS Tools:** Resolve domain names via local resolvers or Google DNS-over-HTTPS (`https://dns.google/resolve`).

### 6. Per-App VPN Filter
- Select individual apps to **Route** or **Bypass** from the VPN using Android `VpnService.Builder`.

---

## Project Structure

```
.
├── .github/
│   └── workflows/
│       └── android.yml            # Automated CI/CD: unit tests, APK build, release publishing
├── app/
│   ├── build.gradle.kts           # Module-level Gradle configuration & signingConfigs
│   ├── proguard-rules.pro         # Proguard rules for R8 optimization
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # VpnService, permissions, activity registration
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt # VpnService.prepare() permission flow & entry point
│       │   │   ├── data/           # Room Database, DAOs, AppPreferences, DefaultServers
│       │   │   ├── model/          # VpnConfig, PayloadModel, VpnState, NetworkToolModels
│       │   │   ├── service/        # DarkTunnelVpnService, VpnController, AppLogger
│       │   │   ├── tunnel/         # TunnelEngine, PayloadFormatter, ConfigParser, Tools
│       │   │   └── ui/             # Jetpack Compose UI (Screens, Components, Theme)
│       │   └── res/                # Drawables, mipmaps, launcher icons, values
│       └── test/                   # JUnit & Robolectric automated unit tests
├── build.gradle.kts               # Root Gradle configuration
├── gradle/
│   └── libs.versions.toml         # Version catalog
├── settings.gradle.kts            # Project settings
├── .gitignore                     # Git ignore rules for Android & secrets
└── README.md
```

---

## Building the APK Locally

### Prerequisites
- JDK 17 or higher
- Android SDK with Platform 36 installed (or compatible minimum API 24+)

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
gradle :app:testDebugUnitTest
```

---

## GitHub Actions & Release Automation

The included workflow (`.github/workflows/android.yml`) automatically builds your application on push or pull request.

### Setting Up GitHub Secrets for Release Signing
To generate a signed production APK, add the following secrets in **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Note |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded upload keystore (`base64 -w 0 upload.jks`) | `MIIKVQIBAzCC...` |
| `STORE_PASSWORD` | Keystore password | Your keystore password |
| `KEY_ALIAS` | Key alias name | `upload` or your alias |
| `KEY_PASSWORD` | Key password | Your key alias password |

### Publishing a Release
When you push a Git tag starting with `v` (e.g. `git tag v1.0.0 && git push origin v1.0.0`), the workflow will:
1. Compile and test the application.
2. Build the signed release APK (`app-release.apk`).
3. Create a GitHub Release with the APK attached for direct download.

---

## Deploying Your Own Tunnel Servers

*Notice: Connect only to servers you own, operate, or are authorized to use.*

### 1. SSH Server Setup (Ubuntu / Debian)
```bash
sudo apt update && sudo apt install openssh-server -y
sudo useradd -m -s /bin/false tunnel_user
echo "tunnel_user:YourSecurePassword" | sudo chpasswd
```

### 2. Stunnel (SSH over SSL/TLS)
```bash
sudo apt install stunnel4 -y
cat << 'EOF' | sudo tee /etc/stunnel/stunnel.conf
pid = /var/run/stunnel.pid
cert = /etc/letsencrypt/live/yourdomain.com/fullchain.pem
key = /etc/letsencrypt/live/yourdomain.com/privkey.pem

[ssh-tls]
accept = 443
connect = 127.0.0.1:22
EOF
sudo systemctl restart stunnel4
```

### 3. Xray-Core (VMess / VLESS / Trojan)
Install official Xray:
```bash
bash -c "$(curl -L https://github.com/XTLS/Xray-install/raw/main/install-release.sh)"
```
Configure your incoming port and UUID in `/usr/local/etc/xray/config.json`, then restart:
```bash
sudo systemctl restart xray
```

---

## Publishing to GitHub

To push your repository to GitHub:
1. In the AI Studio interface, use the **Settings menu > Push to GitHub** (or connect your GitHub repository).
2. Or initialize and push via standard Git:
   ```bash
   git init
   git add .
   git commit -m "feat: initial Dark Tunnel VPN publication"
   git remote add origin https://github.com/<your-username>/<your-repo-name>.git
   git branch -M main
   git push -u origin main
   ```
3. Create a release tag to trigger automated APK building:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
