defmodule MockOsRasterServer do
  use Plug.Router
  alias Plug.Conn
  require Logger

  plug :match
  plug :dispatch

  @port 5916

  def child_spec(_), do: %{
    id: __MODULE__,
    start: {__MODULE__, :start_link, [%{}]}
  }

  def start_link(_params) do
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

  get "/wmts" do
    conn = fetch_query_params(conn)
    simulate_fail = conn.params["simulate_failure_with_status_code"]
    if not is_nil(simulate_fail) do
      Conn.send_resp(conn, String.to_integer(simulate_fail), "Simulated failure")
    else
      Conn.send_resp(conn, 200, "Simulated response for: #{inspect(conn.params)}")
    end
  end
end
