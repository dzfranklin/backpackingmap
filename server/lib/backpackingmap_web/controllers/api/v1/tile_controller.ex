defmodule BackpackingmapWeb.API.V1.TileController do
  use BackpackingmapWeb, :controller
  alias Backpackingmap.Os

  def get(conn, %{"type" => "explorer", "col" => col, "row" => row}) do
    col = String.to_integer(col)
    row = String.to_integer(row)

    {:ok, os_auth} = Os.get_auth_for_user(conn.assigns.current_user)
    {:ok, png} = Os.ExplorerTile.get(%{row: row, col: col}, os_auth)

    conn
    |> put_resp_content_type("image/png")
    |> send_resp(:ok, png)
  end
end
