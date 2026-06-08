# CheckYourMods - Erweiterte Mod-Sicherheit

## 🎯 Implementierte Features

### 1. **Required Mods System** ✅
- Admins können erforderliche Mods konfigurieren
- Spieler ohne erforderliche Mods werden gekickt
- Detailliertes Logging ohne In-Game Ankündigung an andere Spieler
- **Config:** `required_mod_ids` in der Serverkonfiguration

### 2. **Ban System** ✅
- Automatisches Banning bei verbotenen Mods
- Persistent Storage von Ban-Daten
- Spieler können mit `/modcheck unban <Spieler>` entbannt werden
- Ban-Verlauf wird detailliert geloggt
- **Config:** `banned_mod_ids` in der Serverkonfiguration

### 3. **Ban-Benachrichtigungen** ✅
- Admins werden benachrichtigt, wenn jemand gebannt wird
- Benachrichtigungen zeigen bis zu 3 der verbotenen Mods
- Permission-basiert konfigurierbar
- **Config:** 
  - `enable_ban_notifications` (true/false)
  - `ban_notification_permission` (0-4, Standard: 2=Ops)

### 4. **Player Warning System** ✅
- Spieler erhalten Warnungen beim Join für verdächtige Mods
- Klickbare Buttons zum optionalen Erlauben von Mods
- `/modcheck allow <modid>` - Spieler können Mods selbst erlauben
- Warnungen werden persistent gespeichert
- **Config:** `enable_mod_warnings` (true/false)

### 5. **Mod-Statistiken** ✅
- Automatische Erfassung von Mod-Nutzungsdaten
- Top 10 Most Used Mods anzeigen
- Violation-Counter für verbotene Mods
- **Command:** `/modcheck stats`
- **Config:** `enable_stats` (true/false)

### 6. **Erweiterte Logging Funktionen** ✅
Separate Log-Dateien für:
- `checkyourmods-log.txt` - Allgemeine Logs
- `checkyourmods-bans.log` - Ban-Logs
- `checkyourmods-warnings.log` - Warnungs-Logs
- `checkyourmods-violations.log` - Verstöße
- `checkyourmods_audit.log` - Audit-Trail (alle Aktionen mit Details)

### 7. **Audit-Log System** ✅
Vollständige Verfolgung aller sicherheitsrelevanten Aktionen:
- Player Bans/Unbans
- Warnings
- Allowed Mods
- Blocked/Detected Mods
- Pack Alerts
- Alle mit Timestamp, Spielername und UUID

### 8. **Automatische Wartung** ✅
- Automatisches Löschen alter Logs (>30 Tage)
- Periodisch: Alle Daten in Prüfpunkt speichern
- Alle Minute: Wartungscheck durchführen
- Status in `data/maintenance_checkpoint.txt`

### 9. **Admin Dashboard** ✅
**Command:** `/modcheck dashboard`
Zeigt übersichtlich:
- Anzahl gebannter Spieler
- Anzahl verfolgter Spieler
- Anzahl erkannter Mods
- Aktuelle Konfiguration
- Top 3 Most Used Mods

### 10. **Erweiterte Commands** ✅
```
/modcheck modlist                  - Erlaubte Mods anzeigen
/modcheck requiredlist             - Erforderliche Mods anzeigen
/modcheck bannedlist               - Verbotene Mods anzeigen
/modcheck packslist                - Beobachtete Ressourcen-Packs anzeigen
/modcheck dashboard                - Admin-Dashboard (Ops+)
/modcheck bans                     - Liste gebannter Spieler (Ops+)
/modcheck unban <Spieler>          - Spieler entbannen (Ops+)
/modcheck stats                    - Mod-Statistiken (Ops+)
/modcheck audit                    - Audit-Log (Super-Ops+)
/modcheck allow <modid>            - Mod als optional erlauben (jedem Spieler)
```

---

## 📋 Konfiguration

### server-config\.toml Beispiel:
```toml
[Detection_Logic_Explanation]
    allowed_mod_ids = ["optifine", "oculus", "sodium", "iris"]
    required_mod_ids = ["mymod"]
    banned_mod_ids = ["huzuni", "aristois", "schematica"]

[Resource_Pack_Detection]
    manual_xray_hashes = ["hash1", "hash2"]

[Ban_System]
    enable_ban_notifications = true
    ban_notification_permission = "2"

[Features]
    enable_mod_warnings = true
    enable_stats = true
```

---

## 📂 Dateistruktur

```
logs/
├── checkyourmods-log.txt
├── checkyourmods-bans.log
├── checkyourmods-warnings.log
└── checkyourmods-violations.log

data/
├── checkyourmods_bans.txt           - Ban-Datenbank
├── checkyourmods_player_data.txt    - Spieler-Warndaten
├── checkyourmods_stats.txt          - Statistiken
├── checkyourmods_audit.log          - Audit-Trail
└── maintenance_checkpoint.txt        - Status
```

---

## 🔒 Sicherheitsfeatures

✅ Persistent Ban-System - Bans überstehen Server-Neustarts
✅ UUID-basierte Tracking - Bans folgen Spielern über Name-Änderungen
✅ Detailliertes Audit-Log - Alle Aktionen werden protokolliert
✅ Permission-basierte Benachrichtigungen - Nur Admins sehen sensitive Info
✅ Auto-Cleanup - Alte Logs werden automatisch gelöscht
✅ Verschlüsselte Datenspeicherung (zukünftige Version)

---

## 🚀 Best Practices

1. **Konfiguration vor Server-Start setzen** - Laden Sie Ihre Mod-Listen bevor Spieler joinen
2. **Regelmäßig Audits prüfen** - `/modcheck audit` nutzen um Sicherheit zu überwachen
3. **Statistiken nutzen** - `/modcheck stats` zeigt verdächtige Mod-Trends
4. **Bans dokumentieren** - Das Audit-Log speichert automatisch alle Infos
5. **Logs archivieren** - Nach 30 Tagen werden Logs automatisch gelöscht

---

## 📊 Datenverwaltung

**Ban-Daten werden automatisch gespeichert in:**
```
data/checkyourmods_bans.txt
```
Format: `BANNED: PlayerName | UUID | Mod1,Mod2,Mod3 | Reason | [Timestamp]`

**Spieler-Warnungen werden gespeichert in:**
```
data/checkyourmods_player_data.txt
```
Format: `PLAYER: UUID | PlayerName | WarnedMod1,WarnedMod2 | OptionalMod1,OptionalMod2 | [Timestamp]`

---

## 🐛 Troubleshooting

**Problem:** Logs werden nicht erstellt
- **Lösung:** Prüfen Sie dass der Server Schreibrechte im `logs/` Verzeichnis hat

**Problem:** Bans funktionieren nicht
- **Lösung:** Stellen Sie sicher dass `banned_mod_ids` in der Config gesetzt sind

**Problem:** Warnungen werden nicht angezeigt
- **Lösung:** Aktivieren Sie `enable_mod_warnings` und stellen Sie sicher dass die Mods nicht in `allowed_mod_ids` sind

---

## 📞 Support

Für weitere Informationen siehe die Logs:
- `logs/checkyourmods-log.txt` - Hauptlog
- `data/checkyourmods_audit.log` - Audit-Trail

