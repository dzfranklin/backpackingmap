defmodule Backpackingmap.Os do
  alias Backpackingmap.{Repo, Os.Auth, Users.User}
  import Ecto.Query

  def get_auth_for_user(%User{os_username: nil}), do: {:error, :nonexistent}
  def get_auth_for_user(%User{os_username: username}) do
    Auth
    |> where([a], a.username == ^username)
    |> Repo.one()
    |> nillable_to_tuple()
  end

  defp nillable_to_tuple(nil), do: {:error, :nonexistent}
  defp nillable_to_tuple(non_nil), do: {:ok, non_nil}
end
