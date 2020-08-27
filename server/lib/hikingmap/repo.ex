defmodule Hikingmap.Repo do
  use Ecto.Repo,
    otp_app: :hikingmap,
    adapter: Ecto.Adapters.Postgres
end
