# GijinkakunProfile Plugin

GijinkakunProfile is a Bukkit/Spigot plugin that provides detailed player profile information. It integrates with LuckPerms for user group data and stores player statistics in YAML files.

## Features

- Display player profiles with detailed statistics.
- Asynchronously fetches user data from LuckPerms.
- Saves player statistics to YAML files on player quit.
- Command for retrieving player profile information.
- Tab completion for player names in commands.

## Commands

### `/pf <playerNameOrUUID>`

Displays the profile information of the specified player.

- **Usage:** `/pf <playerNameOrUUID>`
- **Permission:** This command can only be run by players.

## Permissions

No specific permissions are required for this plugin. It uses the LuckPerms API to retrieve group information.

## Configuration

The plugin generates a default configuration file on first run. Colors for messages and labels can be customized in the configuration file (`config.yml`).

### Configuration Options

- `colors.label`: Color for labels in profile messages.
- `colors.value`: Color for values in profile messages.
- `colors.message`: Color for general messages.

### Default Configuration

colors:
  label: "#208799"
  value: "#3fc19c"
  message: "#95aaab"

## Installation

1. Download the plugin JAR file and place it in your server's `plugins` directory.
2. Start your server to generate the default configuration file.
3. Modify the configuration file (`config.yml`) if necessary.
4. Restart the server to apply any configuration changes.

## Building the Plugin

If you want to build the plugin from source, follow these steps:

1. Clone the repository.
2. Ensure you have Maven installed.
3. Run `mvn clean install` to build the plugin.
4. The compiled JAR file will be located in the `target` directory.

## Usage

1. Join the server and use the `/pf <playerNameOrUUID>` command to view player profiles.
2. Player data is automatically saved to YAML files upon quitting the server.

## Event Listeners

- **PlayerQuitEvent:** Saves player data to a YAML file when a player quits the server.

## Dependencies

- **LuckPerms:** The plugin requires the LuckPerms API for fetching user group data.

## Troubleshooting

- Ensure that LuckPerms is installed and properly configured on your server.
- Verify that the `profiles` directory is created in the plugin's data folder.
- Check the server console for any error messages related to the plugin.

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes.

## Contact

For any questions or support, feel free to open an issue on the GitHub repository.
