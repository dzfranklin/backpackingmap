defmodule Backpackingmap.Osm.Repo do
  use Ecto.Repo,
    otp_app: :backpackingmap,
    adapter: Ecto.Adapters.Postgres
end
