# SunMc

A high-performance Practice/Duels plugin for **Paper 1.21.1**.

## Features

- ⚔️ **Duels** — 1v1 challenges with kit selection and arena rotation
- 🏟️ **Arenas** — Create, duplicate, and auto-restore arenas after each match
- 🎒 **Kits** — Full kit editor GUI with loadout saving per player
- 💥 **FFA** — Free-for-all zones with live kill tracking
- 👥 **Parties** — Party system with party vs party and split-queue support
- 🏆 **Leaderboards** — Holographic leaderboards for kills, wins, and KDR
- 📊 **Scoreboard** — Live per-player scoreboard with match info

## Requirements

| Requirement | Version |
|-------------|---------|
| Server software | Paper |
| Minecraft | 1.21.1 |
| Java | 21+ |

## Building

### Option A — GitHub Actions (automatic)

Push to `main` or `master` and the workflow builds the jar for you.  
Download it from the **Actions** tab → latest run → **Artifacts**.

To create a release, push a version tag:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Option B — Build locally

Requires JDK 21 and Maven 3.8+.

```bash
git clone <your-repo-url>
cd SunMc
mvn clean package
# jar will be at target/SunMc-1.0.0.jar
```

## Installation

1. Drop `SunMc-1.0.0.jar` into your server's `plugins/` folder.
2. Start or restart the server.
3. Edit `plugins/SunMc/config.yml` as needed.
4. Run `/sunmc reload` to apply config changes without restarting.

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/arena <create\|delete\|list\|duplicate\|setregen\|setboundary>` | Manage arenas | `sunmc.admin` |
| `/kit <create\|delete\|list\|edit>` | Manage kits | `sunmc.admin` |
| `/duel <player>` | Challenge a player | `sunmc.use` |
| `/ffa <join\|leave\|list>` | FFA zones | `sunmc.use` |
| `/party <create\|invite\|leave\|split\|fight\|vs>` | Party management | `sunmc.use` |
| `/leaderboard <name>` | Spawn a leaderboard hologram | `sunmc.admin` |
| `/sunmc <reload\|stats>` | Plugin management | `sunmc.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `sunmc.admin` | Full admin access | OP |
| `sunmc.use` | Use practice features | Everyone |
