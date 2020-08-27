defmodule Hikingmap.Os.Auth do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query
  require Logger
  alias Hikingmap.Repo
  alias Hikingmap.Os.Client

  @primary_key false
  schema "os_auths" do
    field :username, :binary, primary_key: true, null: false
    field :expiry, :naive_datetime
    field :leisure_token, :binary
    field :refresh_ident, :binary
    field :refresh_token, :binary

    timestamps()
  end

  def allowed_leisure_tiles?(%__MODULE__{username: username} = auth) do
    {expiry, token} =
      __MODULE__
      |> where([a], a.username == ^username)
      |> select([a], {a.expiry, a.leisure_token})
      |> Repo.one!()

    cond do
      token == nil ->
        Logger.info("User not allowed leisure tiles because token is nil: #{inspect(auth)}")
        false

      NaiveDateTime.utc_now() > expiry ->
        Logger.info("Refreshing auth to check if user allowed leisure tiles: #{inspect(auth)}")
        :ok = refresh(auth)
        allowed_leisure_tiles?(auth)

      true ->
        true
    end
  end

  def report_unauthorized(%__MODULE__{} = auth) do
    Logger.warn("Got error unauthorized when attempting to communicate with OS: #{inspect(auth)}")

    auth
    |> clear_authorization_changeset()
    |> Repo.update!()
  end

  def create(%{username: username, password: password}) do
    {:ok, {headers, body}} =
      Client.request(:post, "https://osmaps.ordnancesurvey.co.uk/user/login.json", {
        "application/x-www-form-urlencoded; charset=UTF-8",
        URI.encode_query([
          {"username", username},
          {"password", password},
          {"createSession", "true"}
        ])
      })

    decode_login_response(headers, body)
  end

  def refresh(%__MODULE__{} = auth) do
    {:ok, attrs} = get_refreshed(auth)

    auth
    |> refresh_changeset(attrs)
    |> Repo.update!()

    :ok
  end

  defp get_refreshed(auth) do
    {:ok, {headers, body}} =
      Client.request(:get, "https://osmaps.ordnancesurvey.co.uk/user/check_login.json", auth)

    attrs = decode_login_response(headers, body)

    {:ok, attrs}
  end

  defp decode_login_response(headers, body) do
    {ident, token} = get_refresher(headers)
    %{"Expiry" => expiry, "tokens" => %{"leisure" => leisure_token}} = Jason.decode!(body)
    expiry = parse_expiry(expiry)

    %{
      refresh_ident: ident,
      refresh_token: token,
      expiry: expiry,
      leisure_token: leisure_token
    }
  end

  defp get_refresher(headers) do
    %{profile_mark: ident, remember_me: token} =
      headers
      |> Enum.filter(fn
        {"set-cookie", _} -> true
        _ -> false
      end)
      |> Enum.reduce(%{}, fn {_, cookie}, acc ->
        [_, name, value] = Regex.run(~r/^([^=]+)=(.*?);/, cookie)

        case name do
          "PROFILEMARK" -> Map.put(acc, :profile_mark, value)
          "REMEMBER_ME" -> Map.put(acc, :remember_me, value)
          _ -> acc
        end
      end)

    {ident, token}
  end

  defp parse_expiry(string) do
    {:ok, datetime} = NaiveDateTime.from_iso8601(string)
    datetime
  end

  def insertion_changeset(auth \\ %__MODULE__{}, attrs) do
    auth
    |> cast(attrs, [:username, :refresh_ident, :refresh_token, :expiry, :leisure_token])
    |> validate_required([:username, :refresh_ident, :refresh_token, :expiry, :leisure_token])
  end

  def refresh_changeset(auth, attrs) do
    auth
    |> cast(attrs, [:refresh_ident, :refresh_token, :expiry, :leisure_token])
    |> validate_required([:username, :refresh_ident, :refresh_token, :expiry, :leisure_token])
  end

  def clear_authorization_changeset(auth) do
    attrs = %{
      refresh_ident: nil,
      refresh_token: nil,
      expiry: nil,
      leisure_token: nil
    }

    auth
    |> cast(attrs, [:refresh_ident, :refresh_token, :expiry, :leisure_token])
  end
end
