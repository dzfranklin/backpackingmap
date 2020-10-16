defmodule BackpackingmapWeb.API.V1.TileController do
  use BackpackingmapWeb, :controller
  alias Backpackingmap.Os
  require Logger

  defp get_api_key, do:
    Application.get_env(:backpackingmap, :os_api_key)

  def post(
        conn,
        %{
          "serviceIdentifier" => "OS 1.0.0",
          "layerIdentifier" => layerIdentifier,
          "setIdentifier" => setIdentifier,
          "matrixIdentifier" => matrixIdentifier,
          "position" => %{
            "row" => row,
            "col" => col
          }
        }
      ) do

    {:ok, %{status_code: 200, body: png}} =
      "https://api.os.uk/maps/raster/v1/wmts?"
      |> Kernel.<>(URI.encode_query(
        [
          key: get_api_key(),
          service: "wmts",
          request: "gettile",
          version: "1.0.0",
          style: "default",
          layer: layerIdentifier,
          tilematrixset: setIdentifier,
          tilematrix: matrixIdentifier,
          format: "image/png",
          tilerow: row,
          tilecol: col
        ]
      ))
      |> HTTPoison.get()

    Logger.info("Sent tile")

    conn
    |> put_resp_content_type("image/png")
    |> send_resp(:ok, png)
  end
end
