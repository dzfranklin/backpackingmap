# Backpackingmap

To start your Phoenix server:

  * Install [osm2pgsql](https://wiki.openstreetmap.org/wiki/Osm2pgsq), optionally with LuaJit support to increase import
  speed
  * Install dependencies with `mix deps.get`
  * Create and migrate your database with `mix ecto.setup-all`
  * Install Node.js dependencies with `npm install` inside the `assets` directory
  * Start Phoenix endpoint with `mix phx.server`

Now you can visit [`localhost:4000`](http://localhost:4000) from your browser.

You may want to try looking at the database using DataGrip with "Show Geo Viewer" or using
[postgis-editor](https://github.com/danielzfranklin/postgis-editor).

For vector tiles we use the Mapbox Vector Tile format. [mvtview](https://github.com/mapbox/mvtview) is a useful tool for
debugging tile generation.

Ready to run in production? Please [check our deployment guides](https://hexdocs.pm/phoenix/deployment.html).

## Learn more

  * Official website: https://www.phoenixframework.org/
  * Guides: https://hexdocs.pm/phoenix/overview.html
  * Docs: https://hexdocs.pm/phoenix
  * Forum: https://elixirforum.com/c/phoenix-forum
  * Source: https://github.com/phoenixframework/phoenix
