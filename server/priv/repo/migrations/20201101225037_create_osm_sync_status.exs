defmodule Backpackingmap.Repo.Migrations.CreateOsmSyncStatus do
  use Ecto.Migration

  def change do
    create table(:osm_sync_status) do
      add :region, :binary, null: false
      add :success?, :boolean, null: false
      add :osm2pgsql_output, :binary, null: false
      add :append?, :boolean, null: false
      add :exported_at, :utc_datetime, null: false
      add :sequence_number, :integer, null: false
      timestamps()
    end
  end
end
