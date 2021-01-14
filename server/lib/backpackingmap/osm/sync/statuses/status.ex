defmodule Backpackingmap.Osm.Sync.Statuses.Status do
  use Ecto.Schema
  import Ecto.Changeset

  schema "osm_sync_status" do
    field :region, :binary
    field :success?, :boolean
    field :osm2pgsql_output, :binary
    field :append?, :boolean
    field :exported_at, :utc_datetime
    field :sequence_number, :integer
    timestamps()
  end

  def changeset(status_or_changeset \\ %__MODULE__{}, attrs) do
    status_or_changeset
    |> cast(attrs, [:region, :success?, :osm2pgsql_output, :append?, :exported_at, :sequence_number])
    |> validate_required([:region, :success?, :osm2pgsql_output, :append?, :exported_at, :sequence_number])
  end
end
