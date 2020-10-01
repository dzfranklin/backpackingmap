defmodule BackpackingmapWeb.API.V1.RegistrationController do
  use BackpackingmapWeb, :controller

  alias Ecto.Changeset
  alias BackpackingmapWeb.ErrorHelpers

  def create(conn, %{"user" => user_params}) do
    conn
    |> Pow.Plug.create_user(user_params)
    |> case do
      {:ok, _user, conn} ->
        json(conn, %{
          data: %{
            access_token: conn.private[:api_access_token],
            renewal_token: conn.private[:api_renewal_token]
          }
        })

      {:error, changeset, conn} ->
        errors = Changeset.traverse_errors(changeset, &ErrorHelpers.translate_error/1)

        conn
        |> put_status(400)
        |> json(%{error: %{status: 400, message: "Couldn't create user", field_errors: errors}})
    end
  end
end
