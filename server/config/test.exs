use Mix.Config

# Server in test/support/mock_os_raster_server.ex
config :backpackingmap, :os_raster_api_base_url, "http://localhost:5916/wmts"
config :backpackingmap, :os_api_key, "TEST_API_KEY"

# Configure your database
#
# The MIX_TEST_PARTITION environment variable can be used
# to provide built-in test partitioning in CI environment.
# Run `mix help test` for more information.
config :backpackingmap,
       Backpackingmap.Repo,
       username: "postgres",
       password: "postgres",
       database: "backpackingmap_test#{System.get_env("MIX_TEST_PARTITION")}",
       hostname: "localhost",
       pool: Ecto.Adapters.SQL.Sandbox

config :backpackingmap,
       Backpackingmap.Osm.Repo,
       username: "postgres",
       password: "postgres",
       database: "backpackingmap_osm_test#{System.get_env("MIX_TEST_PARTITION")}",
       hostname: "localhost",
       pool: Ecto.Adapters.SQL.Sandbox

# We don't run a server during test. If one is required,
# you can enable the server option below.
config :backpackingmap,
       BackpackingmapWeb.Endpoint,
       http: [
         port: 4002
       ],
       https: [
         port: 5442
       ],
       server: false

config :backpackingmap,
       :certbot,
       db_folder: "priv/test_site_encrypt",
       directory_url: {
         :internal,
         port: 5002
       }

# Print only warnings and errors during test
config :logger, level: :warn
