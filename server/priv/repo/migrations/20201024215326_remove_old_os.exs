defmodule Backpackingmap.Repo.Migrations.RemoveOldOs do
  use Ecto.Migration

  def change do
    alter table(:users) do
      remove :os_username
    end
    drop table(:explorer_tiles)
    drop table(:os_auths)
  end
end
