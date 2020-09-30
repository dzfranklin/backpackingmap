defmodule Backpackingmap.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    children = [
      # Start the Ecto repository
      Backpackingmap.Repo,
      # Start the Telemetry supervisor
      BackpackingmapWeb.Telemetry,
      # Start the PubSub system
      {Phoenix.PubSub, name: Backpackingmap.PubSub},
      # Start the Endpoint (http/https)
      {SiteEncrypt.Phoenix, BackpackingmapWeb.Endpoint}
      # Start a worker by calling: Backpackingmap.Worker.start_link(arg)
      # {Backpackingmap.Worker, arg}
    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Backpackingmap.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  def config_change(changed, _new, removed) do
    BackpackingmapWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
