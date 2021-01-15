defmodule Backpackingmap.Osm.Features.Polygon do
  use Ecto.Schema

  @primary_key {:area_id, :integer, autogenerate: false}
  schema "polygons" do
    field :tags, :map
    field :area, :float
    field :changeset_id, :integer
  end
end
