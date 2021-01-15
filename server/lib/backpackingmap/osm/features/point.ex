defmodule Backpackingmap.Osm.Features.Point do
  use Ecto.Schema

  @primary_key {:node_id, :integer, autogenerate: false}
  schema "points" do
    field :tags, :map
    field :changeset_id, :integer
  end
end
