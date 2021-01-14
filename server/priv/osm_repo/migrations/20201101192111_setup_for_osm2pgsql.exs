defmodule Backpackingmap.Osm.Repo.Migrations.SetupForOsm2pgsql do
  use Ecto.Migration

  def up do
    execute("CREATE EXTENSION postgis")
    execute("CREATE EXTENSION hstore")
  end

  def down do
    execute("DROP EXTENSION postgis")
    execute("DROP EXTENSION hstore")
  end
end
