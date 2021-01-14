defmodule Backpackingmap.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    children = [
      # Start the OS quota allocator
      Backpackingmap.OsRequester,
      # Start the Ecto repository
      Backpackingmap.Repo,
      Backpackingmap.Osm.Repo,
      # Start the Telemetry supervisor
      BackpackingmapWeb.Telemetry,
      # Start the PubSub system
      {Phoenix.PubSub, name: Backpackingmap.PubSub},
      # Start the Endpoint (http/https)
      {SiteEncrypt.Phoenix, BackpackingmapWeb.Endpoint},
      Pow.Store.Backend.MnesiaCache
      # Start a worker by calling: Backpackingmap.Worker.start_link(arg)
      # {Backpackingmap.Worker, arg}
    ] ++ env_specific_children(Application.get_env(:backpackingmap, :env))

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Backpackingmap.Supervisor]
    Supervisor.start_link(children, opts)
  end

  defp env_specific_children(:test), do: [MockOsRasterServer, MockOsmPbfServer]

  defp env_specific_children(_), do: []

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  def config_change(changed, _new, removed) do
    BackpackingmapWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
