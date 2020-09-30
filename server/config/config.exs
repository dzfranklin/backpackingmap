# This file is responsible for configuring your application
# and its dependencies with the aid of the Mix.Config module.
#
# This configuration file is loaded before any dependency and
# is restricted to this project.

# General application configuration
use Mix.Config

config :backpackingmap, :certbot,
  error_contacts: ["daniel@danielzfranklin.org"]

config :backpackingmap,
  ecto_repos: [Backpackingmap.Repo]

config :backpackingmap, :pow,
  user: Backpackingmap.Users.User,
  repo: Backpackingmap.Repo,
  web_module: BackpackingmapWeb

# Configures the endpoint
config :backpackingmap, BackpackingmapWeb.Endpoint,
  url: [host: "localhost"],
  secret_key_base: "2fYs29lZ+dptYGCRIEkPIi55ikABOYcIseagrFNgj/CF9TL8J7XVSPukjcGy3YAZ",
  render_errors: [view: BackpackingmapWeb.ErrorView, accepts: ~w(html json), layout: false],
  pubsub_server: Backpackingmap.PubSub,
  live_view: [signing_salt: "Y23eZRzD"]

# Configures Elixir's Logger
config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

# Use Jason for JSON parsing in Phoenix
config :phoenix, :json_library, Jason

# Import environment specific config. This must remain at the bottom
# of this file so it overrides the configuration defined above.
import_config "#{Mix.env()}.exs"
