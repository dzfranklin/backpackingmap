defmodule Backpackingmap.Osm.Features.Route do
  use Ecto.Schema

  @primary_key {:relation_id, :integer, autogenerate: false}
  schema "routes" do
    field :tags, :map
    field :changeset_id, :integer
  end
end
