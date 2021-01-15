defmodule Backpackingmap.Osm.Features.Boundary do
  use Ecto.Schema

  @primary_key {:relation_id, :integer, autogenerate: false}
  schema "boundaries" do
    field :tags, :map
    field :changeset_id, :integer
  end
end
