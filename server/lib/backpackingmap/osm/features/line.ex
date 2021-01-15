defmodule Backpackingmap.Osm.Features.Line do
  use Ecto.Schema

  @primary_key {:way_id, :integer, autogenerate: false}
  schema "lines" do
    field :tags, :map
    field :changeset_id, :integer
  end
end
