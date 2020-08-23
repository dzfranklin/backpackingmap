defmodule Hikingmap.Repo.Migrations.CreateExplorerTiles do
  use Ecto.Migration

  def change do
    create table(:explorer_tiles, primary_key: false) do
      add :row, :integer, primary_key: true, null: false
      add :col, :integer, primary_key: true, null: false
      add :png, :binary, null: false

      timestamps()
    end
  end
end
