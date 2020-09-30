defmodule Backpackingmap.Repo.Migrations.CreateOsAuths do
  use Ecto.Migration

  def change do
    create table(:os_auths, primary_key: false) do
      add :username, :binary, primary_key: true
      add :refresh_ident, :binary
      add :refresh_token, :binary
      add :expiry, :naive_datetime
      add :leisure_token, :binary

      timestamps()
    end
  end
end
