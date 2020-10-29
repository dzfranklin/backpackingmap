defmodule BackpackingmapWeb.API.V1.TileController do
  use BackpackingmapWeb, :controller
  alias Backpackingmap.OsRequester
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
        } = params
      ) do

    response = OsRequester.request_raster_tile(
      %{
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
      }
    )

    with {:ok, png} <- response do
      conn
      |> put_resp_content_type("image/png")
      |> send_resp(:ok, png)
    else
      {:error, error} ->
        Timber.w("Got error #{error} while trying to serve request with params #{params}")
        json(
          conn,
          %{
            error: %{
              message: "Error requesting OS tile"
            }
          }
        )
    end
  end
end
