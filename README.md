<p align="center">
  <img src="src/main/resources/logo.png" width="250" alt="CheckYourMods Logo">
</p>

# CheckYourMods 🛡️

**CheckYourMods** is an advanced security and transparency tool for **Minecraft 1.21.1 (NeoForge)** designed for server administrators. It ensures a fair play environment by monitoring client-side modifications and resource packs, providing robust banning and auditing systems.

### 📋 Overview

CheckYourMods provides server administrators with the tools to enforce mod and resource pack policies. It automatically detects unauthorized modifications, tracks player mod usage, and maintains detailed logs and audit trails.

### ✨ Key Features

*   **Smart Mod Verification**: Automatically compares player mods against the server's mod list.
*   **Banning & Warning System**: Automatically bans players using forbidden mods and warns players about suspicious ones.
*   **Intelligent Resource Pack Scan**: Detects suspicious keywords like "xray" or "transparent" in resource pack metadata.
*   **SHA-256 Fingerprinting**: Tracks specific resource packs using unique digital hashes.
*   **Mod Statistics**: Tracks mod usage across the server and provides a "Top 10" most used mods list.
*   **Persistent Auditing**: Maintains detailed audit logs for all security-relevant actions.
*   **Automatic Maintenance**: Periodically cleans up old logs (>30 days) and saves checkpoints.

### 🛠️ Tech Stack

*   **Language**: Java 21
*   **Framework**: NeoForge (Minecraft 1.21.1)
*   **Build System**: Gradle 8.x with `ModDevGradle`
*   **Metadata**: NeoForge `neoforge.mods.toml`

### ⚙️ Requirements

*   **Server**: Minecraft 1.21.1 with NeoForge installed.
*   **Java**: Java 21 or higher.
*   **Development**: Gradle (bundled via `gradlew`).

### 🚀 Setup & Run

1.  **Clone the repository**:
    ```bash
    git clone <repository-url>
    cd checkyourmods-1.21.1
    ```
2.  **Build the project**:
    ```bash
    ./gradlew build
    ```
    The built jar will be in `build/libs/`.
3.  **Run for development**:
    *   **Client**: `./gradlew runClient`
    *   **Server**: `./gradlew runServer`
4.  **Deployment**: Place the generated jar file into your server's `mods` folder.

### 💻 Commands

CheckYourMods uses the `/modcheck` base command:

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/modcheck requiredlist` | All | Shows required mod IDs. |
| `/modcheck bannedlist` | All | Shows banned mod IDs. |
| `/modcheck packslist` | All | Shows watched resource pack hashes. |
| `/modcheck allow <modid>` | All | Player marks a mod as optionally allowed for themselves. |
| `/modcheck dashboard` | Ops (2+) | Displays security overview and stats. |
| `/modcheck stats` | Ops (2+) | Shows top 10 most used mods and violations. |
| `/modcheck bans` | Ops (2+) | Lists all currently banned players. |
| `/modcheck unban <player>` | Ops (2+) | Unbans a specified player. |
| `/modcheck audit` | Super Ops (3+) | Displays the last 20 audit log entries. |
| `/modcheck togglepacks` | Ops (2+) | Toggles strict pack verification. |
| `/modcheck allowpack <hash>`| Ops (2+) | Manually allows a specific pack hash. |

### 🔧 Configuration

The configuration file is located at `serverconfig/checkyourmods-server.toml`.

*   **`required_mod_ids`**: List of mod IDs players *must* have to join.
*   **`banned_mod_ids`**: List of mod IDs that trigger an automatic permanent ban.
*   **`manual_xray_hashes`**: SHA-256 hashes of forbidden resource packs.
*   **`enable_ban_notifications`**: (Default: `true`) Notify admins when a ban occurs.
*   **`enable_mod_warnings`**: (Default: `true`) Warn players about unknown mods.
*   **`enable_stats`**: (Default: `true`) Enable mod usage statistics collection.

### 📂 Project Structure

*   `src/main/java/checkyourmods/main/`: Core logic including networking, commands, and managers.
*   `src/main/resources/`: Assets, translations, and `META-INF/neoforge.mods.toml`.
*   `logs/`: Contains `checkyourmods-log.txt`, `checkyourmods-bans.log`, etc.
*   `data/`: Persistent storage for bans (`checkyourmods_bans.txt`), stats, and audit logs.

### 🧪 Tests
*   **TODO**: Implement unit tests for networking and mod comparison logic.
*   NeoForge `gameTestServer` configuration is available via `./gradlew runGameTestServer`.

### 📄 License

Distributed under the **MIT License**. See `LICENSE.md` for more information.

---
*Created for the Minecraft Server Administration community.*