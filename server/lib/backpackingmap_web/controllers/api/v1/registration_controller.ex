defmodule BackpackingmapWeb.API.V1.RegistrationController do
  use BackpackingmapWeb, :controller
  require Logger

  alias Ecto.Changeset
  alias BackpackingmapWeb.ErrorHelpers

  def create(conn, %{"user" => user_params}) do
    conn
    |> Pow.Plug.create_user(user_params)
    |> case do
      {:ok, %{id: id} = _user, conn} ->
        Logger.info("Registered user #{id}")

        json(conn, %{
          data: %{
            user_id: id,
            access_token: conn.private[:api_access_token],
            renewal_token: conn.private[:api_renewal_token]
          }
        })

      {:error, changeset, conn} ->
        errors = ErrorHelpers.translate_changeset(changeset)
        Logger.warn("Refused to register user because #{inspect(errors)}")

        conn
        |> put_status(200)
        |> json(%{error: %{message: "You couldn't be registered", field_errors: errors}})
    end
  end
end
