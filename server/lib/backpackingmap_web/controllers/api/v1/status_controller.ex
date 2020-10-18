defmodule BackpackingmapWeb.API.V1.StatusController do
  use BackpackingmapWeb, :controller

  def get(conn, _params) do
    json(conn, %{})
  end
end
