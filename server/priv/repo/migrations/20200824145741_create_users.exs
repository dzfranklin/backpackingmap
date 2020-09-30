defmodule Backpackingmap.Repo.Migrations.CreateUsers do
  use Ecto.Migration

  def change do
    create table(:users) do
      add :email, :binary, null: false
      add :password_hash, :binary, null: false
      timestamps()
    end

    create unique_index(:users, [:email])
  end
end
