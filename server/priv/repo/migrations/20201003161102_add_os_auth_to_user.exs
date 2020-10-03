defmodule Backpackingmap.Repo.Migrations.AddOsAuthToUser do
  use Ecto.Migration

  def change do
    alter table(:users) do
      add :os_username, references(:os_auths, column: :username, type: :binary)
    end
  end
end
