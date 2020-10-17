defmodule BackpackingmapWeb.API.V1.SessionController do
  use BackpackingmapWeb, :controller
  require Logger
  alias BackpackingmapWeb.APIAuth
  alias Backpackingmap.Users

  def create(conn, %{"user" => user_params}) do
    conn
    |> Pow.Plug.authenticate_user(user_params)
    |> case do
         {:ok, conn} ->
           %{id: id} = Pow.Plug.current_user(conn)

           json(
             conn,
             %{
               data: %{
                 user_id: id,
                 access_token: conn.private[:api_access_token],
                 renewal_token: conn.private[:api_renewal_token]
               }
             }
           )

         {:error, conn} ->
           conn
           |> json(
                %{
                  error: %{
                    message: "Incorrect email or password"
                  }
                }
              )
       end
  end

  def renew(conn, _params) do
    config = Pow.Plug.fetch_config(conn)

    conn
    |> APIAuth.Plug.renew(config)
    |> case do
         {conn, nil} ->
           Logger.warn("Refusing to renew due to incorrect authentication token")

           conn
           |> json(
                %{
                  error: %{
                    message: "Incorrect authentication token"
                  }
                }
              )

         {conn, user} ->
           Logger.info("Renewed authentication token for user #{user.id}")

           json(
             conn,
             %{
               data: %{
                 user_id: user.id,
                 access_token: conn.private[:api_access_token],
                 renewal_token: conn.private[:api_renewal_token]
               }
             }
           )
       end
  end

  def delete(conn, _params) do
    conn
    |> Pow.Plug.delete()
    |> json(%{data: %{}})
  end
end
