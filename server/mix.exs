defmodule Backpackingmap.MixProject do
  use Mix.Project

  def project do
    [
      app: :backpackingmap,
      version: "0.1.0",
      elixir: "~> 1.7",
      elixirc_paths: elixirc_paths(Mix.env()),
      compilers: [:phoenix, :gettext] ++ Mix.compilers(),
      start_permanent: Mix.env() == :prod,
      aliases: aliases(),
      deps: deps()
    ]
  end

  # Configuration for the OTP application.
  #
  # Type `mix help compile.app` for more information.
  def application do
    [
      mod: {Backpackingmap.Application, []},
      extra_applications: [:logger, :runtime_tools, :mnesia]
    ]
  end

  # Specifies which paths to compile per environment.
  defp elixirc_paths(:test), do: ["lib", "test/support"]
  defp elixirc_paths(_), do: ["lib"]

  # Specifies your project dependencies.
  #
  # Type `mix help deps` for examples and options.
  defp deps do
    [
      {:phoenix, "~> 1.5.3"},
      {:phoenix_ecto, "~> 4.1"},
      {:ecto_sql, "~> 3.4"},
      {:postgrex, ">= 0.0.0"},
      {:phoenix_live_view, "~> 0.13.0"},
      {:floki, ">= 0.0.0", only: :test},
      {:phoenix_html, "~> 2.11"},
      {:phoenix_live_reload, "~> 1.2", only: :dev},
      {:phoenix_live_dashboard, "~> 0.2.0"},
      {:telemetry_metrics, "~> 0.4"},
      {:telemetry_poller, "~> 0.4"},
      {:gettext, "~> 0.11"},
      {:jason, "~> 1.0"},
      {:plug_cowboy, "~> 2.0"},
      {:coord, "~> 0.1"},
      {:pow, "~> 1.0.20"},
      {:site_encrypt, "~> 0.3.0"},
      {:httpoison, "~> 1.6"},
      {:deque, "~> 1.0"},
      {:tracer, git: "https://github.com/martinmaillard/tracer.git", commit: "beaf6e8"},
      {:temp, "~> 0.4"}
    ]
  end

  # Aliases are shortcuts or tasks specific to the current project.
  # For example, to install project dependencies and perform other setup tasks, run:
  #
  #     $ mix setup
  #
  # See the documentation for `Mix` for more info on aliases.
  defp aliases do
    [
      setup: [
        "deps.get",
        "ecto.setup",
        fn _ -> Mix.Task.reenable("ecto.setup") end,
        "ecto.setup -r Backpackingmap.Osm.Repo",
        "cmd npm install --prefix assets"
      ],
      "ecto.migrate-all": [
        "ecto.migrate -r Backpackingmap.Repo",
        fn _ -> Mix.Task.reenable("ecto.migrate") end,
        "ecto.migrate -r Backpackingmap.Osm.Repo",
      ],
      "ecto.create-all": [
        "ecto.create -r Backpackingmap.Repo",
        fn _ -> Mix.Task.reenable("ecto.create") end,
        "ecto.create -r Backpackingmap.Osm.Repo",
      ],
      "ecto.setup-all": [
        "ecto.create-all",
        "ecto.migrate-all",
        "run priv/repo/seeds.exs"
      ],
      "ecto.reset-all": [
        "ecto.drop",
        fn _ -> Mix.Task.reenable("ecto.drop") end,
        "ecto.drop -r Backpackingmap.Osm.Repo",
        "ecto.setup-all",
      ],
      test: [
        "ecto.create --quiet",
        fn _ -> Mix.Task.reenable("ecto.create") end,
        "ecto.create --quiet -r Backpackingmap.Osm.Repo",
        "ecto.migrate --quiet",
        fn _ -> Mix.Task.reenable("ecto.migrate") end,
        "ecto.migrate --quiet -r Backpackingmap.Osm.Repo",
        "test"
      ]
    ]
  end
end
