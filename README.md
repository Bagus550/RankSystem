# RankSystem

A fully configurable rank system for Paper servers. Define ranks, prefixes, permissions, and rank-up requirements in a single `config.yml`, with gradient and animated prefixes, automatic rank-up by playtime and XP, and ready-made integration for TAB and EssentialsX Chat.

## Features

- **Everything in `config.yml`:** rank ID, display name, prefix, weight, inherited ranks, and permissions
- **Permission inheritance:** ranks inherit from other ranks and can be chained (`owner` → `moderator` → `elite` → ...). Revoke an inherited permission by prefixing it with `-`
- **Styled prefixes with MiniMessage:** colors, hex colors, gradients, and animated gradients
- **Automatic rank-up** by playtime and earned XP, with a title, sound, particles, a chat message, and an optional server-wide announcement
- **`/progress`:** players can check how close they are to the next rank
- **TAB support** through a PlaceholderAPI expansion (`%ranksystem_prefix%`)
- **EssentialsX Chat support** through a Vault Chat/Permission provider
- **Optional built-in chat format** and tab list name
- **Multiple languages:** English and Indonesian included, add your own by copying a file

## Requirements

- Paper 26.2 and Java 25
- Optional: [Vault](https://www.spigotmc.org/resources/vault.34315/) (prefix in EssentialsX Chat)
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (prefix in TAB)

Folia is not supported.

## Installation

1. Download the jar and put it in your server's `plugins/` folder.
2. Start the server. The plugin creates `plugins/RankSystem/config.yml` and the `lang/` folder.
3. Edit `config.yml` to your liking and run `/rank reload`.
4. Give a player a rank with `/rank set <player> <rank>`.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/rank set <player> <rank>` | Set a player's rank | `ranks.admin` |
| `/rank remove <player>` | Reset a player to the default rank | `ranks.admin` |
| `/rank info <player>` | Show a player's rank | `ranks.admin` |
| `/rank list` | List all ranks, highest weight first | `ranks.admin` |
| `/rank reload` | Reload `config.yml` and the language file | `ranks.admin` |
| `/rank preview <rank>` | Preview the rank-up title, sound, and particles | `ranks.admin` |
| `/progress` (alias `/rankprogress`) | Show progress toward the next rank | everyone |

## Configuration

### Ranks

```yaml
default-rank: citizen

ranks:
  citizen:
    display-name: "Citizen"
    prefix: "<gray>[Citizen]</gray>"
    weight: 1
    inherits: []
    permissions:
      - essentials.sethome
      - essentials.home

  wanderer:
    display-name: "Wanderer"
    prefix: "<green>[Wanderer]</green>"
    weight: 5
    inherits: [citizen]
    requirements:            # optional: makes this rank reachable automatically
      playtime-hours: 2
      exp: 300
    permissions:
      - essentials.sethome.multiple.wanderer

  moderator:
    display-name: "Moderator"
    prefix: "<gradient:#ff512f:#dd2476><bold>[MOD]</bold></gradient>"
    weight: 50
    inherits: [wanderer]
    permissions:
      - essentials.fly
      - "-essentials.home"   # "-" revokes a permission, even an inherited one
```

| Key | Description |
|---|---|
| `display-name` | Name shown in plugin messages |
| `prefix` | Text in front of the player's name (MiniMessage) |
| `weight` | Rank level. Higher is better. Used for ordering, automatic rank-up, and announcements |
| `inherits` | Ranks whose permissions are inherited |
| `permissions` | Permissions granted by this rank. Prefix with `-` to revoke |
| `requirements` | Optional rank-up requirements (see below) |

Rules:

- A rank's own permissions always override inherited ones.
- Circular inheritance is ignored.
- Removing a rank from the config makes players who hold it fall back to the default rank.
- Bukkit does not treat `*` as a wildcard. Use plugin-specific wildcards such as `essentials.*`.

### Prefix styling

Prefixes use [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Close every tag you open, and do not use legacy codes like `&a`.

```yaml
prefix: "<green>[VIP]</green>"                                          # named color
prefix: "<#ff8800>[VIP]</#ff8800>"                                      # hex color
prefix: "<gold><bold>[Veteran]</bold></gold>"                           # bold
prefix: "<gradient:#4facfe:#00f2fe><bold>[MOD]</bold></gradient>"       # gradient
prefix: "<rainbow>[OWNER]</rainbow>"                                    # rainbow
prefix: "<gradient:#f7971e:#ffd200:{phase}>[Owner]</gradient>"          # animated
```

Put `{phase}` at the end of a `<gradient>` or `<rainbow>` tag to animate it. Control the speed with `animation.period-ms`. In chat the prefix is a snapshot taken when the message is sent, so animation shows in places that refresh, such as the TAB nametag and tab list.

### Automatic rank-up

Add `requirements` to a rank to make it reachable automatically:

```yaml
requirements:
  playtime-hours: 10   # total hours played (decimals allowed)
  exp: 2000            # total XP earned (not the level)
```

Global settings:

```yaml
auto-rankup:
  enabled: true
  check-interval-seconds: 60
  broadcast-enabled: true
  broadcast-min-weight: 15   # only ranks with weight >= this are announced server-wide
  title:
    enabled: true
    fade-in-ms: 500
    stay-ms: 3500
    fade-out-ms: 1000
  sound:
    enabled: true
    key: "minecraft:ui.toast.challenge_complete"
    volume: 1.0
    pitch: 1.0
  particles:
    enabled: true
```

How it works:

- Online players are checked every `check-interval-seconds`.
- A player must meet **all** requirements of a rank. The highest-weight rank they qualify for is applied.
- Players only move **up**. Staff ranks without `requirements` are never touched.
- Playtime comes from Minecraft's built-in statistic, so existing playtime counts (AFK time included).
- XP is counted from **XP earned** after the plugin was installed, so spending XP on enchantments does not lower progress.
- If an admin demotes a player with `/rank set`, they rank up again at the next check while they still meet the requirements.

### Language

Messages live in `plugins/RankSystem/lang/`. Choose a language in `config.yml`:

```yaml
language: en   # en or id
```

To add a language, copy `lang/en.yml` to `lang/<code>.yml`, translate it, set `language: <code>`, and run `/rank reload`. Keys missing from a language file fall back to English.

### Chat and tab list

```yaml
chat:
  enabled: false
  format: "<prefix> <gray><player></gray><dark_gray> » </dark_gray><white><message></white>"

tablist:
  enabled: true
```

Set `chat.enabled: false` if another plugin (for example EssentialsX Chat) formats chat. Set `tablist.enabled: false` if TAB manages the tab list.

## Integrations

### TAB (PlaceholderAPI)

With PlaceholderAPI installed, RankSystem registers these placeholders:

| Placeholder | Result |
|---|---|
| `%ranksystem_prefix%` | Prefix in `&` color format (works in TAB) |
| `%ranksystem_prefix_mm%` | Prefix in raw MiniMessage format |
| `%ranksystem_rank%` | Rank display name |
| `%ranksystem_rank_id%` | Rank ID |
| `%ranksystem_weight%` | Rank weight |

In TAB's `groups.yml`:

```yaml
_DEFAULT_:
  tabprefix: "%ranksystem_prefix% "
  tagprefix: "%ranksystem_prefix% "
```

To sort the tab list by rank, in TAB's `config.yml`:

```yaml
scoreboard-teams:
  sorting-types:
    - "PLACEHOLDER_HIGH_TO_LOW:%ranksystem_weight%"
    - "PLACEHOLDER_A_TO_Z:%player%"
```

If you use animated prefixes, refresh the placeholder more often in TAB's `config.yml`:

```yaml
placeholder-refresh-intervals:
  "%ranksystem_prefix%": 200
```

Remove any old per-group entries in TAB's `groups.yml` that match your rank IDs, otherwise they override the placeholder.

### EssentialsX Chat (Vault)

With Vault installed, RankSystem registers itself as a Chat and Permission provider, so `{PREFIX}` in EssentialsX Chat shows the rank prefix.

1. In RankSystem's `config.yml`, set `chat.enabled: false`.
2. In EssentialsX's `config.yml`, use `{USERNAME}` or `{NICKNAME}` instead of `{DISPLAYNAME}`:

```yaml
format: '{PREFIX}{USERNAME}&7: &f{MESSAGE}'
```

`{DISPLAYNAME}` already includes the prefix by default, which causes a double prefix like `[Mod] [Mod] Name`.

## Data files

| File | Content |
|---|---|
| `plugins/RankSystem/data.yml` | Which player holds which rank |
| `plugins/RankSystem/progress.yml` | XP earned per player, used for rank-up |

## Notes and limitations

- Permissions are applied with Bukkit permission attachments. This is fine for small and medium servers, but RankSystem is not a replacement for LuckPerms. Do not run it together with another plugin that also provides Vault Chat/Permission.
- Operators bypass permission checks. Test rank permissions with a non-OP account.
- Existing `config.yml` and `lang/` files are never overwritten by an update. Delete them to regenerate the defaults, or copy new keys over manually.

## Building from source

Requires Java 25.

```
./gradlew build
```

The jar is created in `build/libs/`.

## License

Released under the MIT License. See the [LICENSE](LICENSE) file for details.

Contributions are welcome. Feel free to open an issue or a pull request.