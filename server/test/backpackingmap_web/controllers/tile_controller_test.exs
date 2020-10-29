defmodule BackpackingmapWeb.API.V1.TileControllerTest do
  use BackpackingmapWeb.ConnCase
  alias Backpackingmap.Users.User

  @valid_user %User{email: "foo@example.com", id: 1}

  @os_service_identifier "OS 1.0.0"
  @example_layer_identifier "exampleLayer"
  @example_set_identifier "exampleSet"
  @example_matrix_identifier "exampleMatrix"
  @example_position [row: 42, col: 42]

  @valid_request_params [
    serviceIdentifier: @os_service_identifier,
    layerIdentifier: @example_layer_identifier,
    setIdentifier: @example_set_identifier,
    matrixIdentifier: @example_matrix_identifier,
    position: @example_position
  ]

  setup %{conn: conn} = params do
    conn = if Map.has_key?(params, :auth_as) do
      Pow.Plug.assign_current_user(conn, params.auth_as, [])
    else
      conn
    end

    {:ok, conn: conn}
  end

  @tag auth_as: @valid_user
  test "accepts requests of OS tiles", %{conn: conn} do
    conn =
      conn
      |> put_req_header("accept", "application/json")
      |> post("/api/v1/tile", @valid_request_params)

    assert conn.resp_body == "Simulated response for: %{\"format\" => \"image/png\", \"key\" => \"TEST_API_KEY\", \"layer\" => \"exampleLayer\", \"request\" => \"gettile\", \"service\" => \"wmts\", \"style\" => \"default\", \"tilecol\" => \"42\", \"tilematrix\" => \"exampleMatrix\", \"tilematrixset\" => \"exampleSet\", \"tilerow\" => \"42\", \"version\" => \"1.0.0\"}"
  end

  test "rejects unauthenticated users", %{conn: conn} do
    conn =
      conn
      |> put_req_header("accept", "application/json")
      |> post("/api/v1/tile", @valid_request_params)

    assert conn.status == 401
  end
end
