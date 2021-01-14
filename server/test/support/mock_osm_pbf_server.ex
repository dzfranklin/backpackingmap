defmodule MockOsmPbfServer do
  use Plug.Router
  alias Plug.Conn
  require Logger

  plug :match
  plug :dispatch

  @port 5917

  def assets_dir, do: "priv/test_assets"

  def child_spec(_), do: %{
    id: __MODULE__,
    start: {__MODULE__, :start_link, [[]]}
  }

  def start_link(_) do
    Logger.info("Starting #{__MODULE__} on port #{@port}")

    children = [
      {
        Plug.Cowboy,
        [
          scheme: :http,
          plug: __MODULE__,
          options: [
            port: @port
          ]
        ]
      }
    ]

    Supervisor.start_link(children, [strategy: :one_for_one])
  end

  get "/mock.pbf" do
    Conn.send_file(conn, 200, assets_dir() <> "/vatican_city.osm.pbf")
  end

  get "/mock.pbf.md5" do
    Conn.send_file(conn, 200, assets_dir() <> "/vatican_city.osm.pbf.md5")
  end

  get "/error/mock.pbf" do
    Conn.send_resp(conn, 500, "Server error")
  end

  get "/mock_region/updates/state.txt" do
    Conn.send_resp(
      conn,
      200,
      # a snapshot of <https://download.geofabrik.de/europe/great-britain-updates/state.txt>
      "# original OSM minutely replication sequence number 4264784\ntimestamp=2020-11-01T21\\:42\\:02Z\nsequenceNumber=2780\n"
    )
  end
end
