defmodule HikingmapWeb.APIAuth.Plug do
  @moduledoc """
  Usage:

  ```
  $ curl -X POST -d "user[email]=test@example.com&user[password]=secret1234&user[password_confirmation]=secret1234" http://localhost:4000/api/v1/registration
  {"data":{"renewal_token":"RENEW_TOKEN","access_token":"AUTH_TOKEN"}}

  $ curl -X POST -d "user[email]=test@example.com&user[password]=secret1234" http://localhost:4000/api/v1/session
  {"data":{"renewal_token":"RENEW_TOKEN","access_token":"AUTH_TOKEN"}}

  $ curl -X DELETE -H "Authorization: AUTH_TOKEN" http://localhost:4000/api/v1/session
  {"data":{}}

  $ curl -X POST -H "Authorization: RENEW_TOKEN" http://localhost:4000/api/v1/session/renew
  {"data":{"renewal_token":"RENEW_TOKEN","access_token":"AUTH_TOKEN"}}
  ```
  """
  use Pow.Plug.Base

  alias Plug.Conn
  alias Pow.{Config, Plug, Store.CredentialsCache}
  alias PowPersistentSession.Store.PersistentSessionCache

  @doc """
  Fetches the user from access token.
  """
  @impl true
  def fetch(conn, config) do
    with {:ok, signed_token} <- fetch_access_token(conn),
         {:ok, token} <- verify_token(conn, signed_token, config),
         {user, _meta} <- CredentialsCache.get(store_config(config), token) do
      {conn, user}
    else
      _ -> {conn, nil}
    end
  end

  @doc """
  Creates an access and renewal token for the user.

  The tokens are added to the `conn.private` as `:api_access_token` and
  `:api_renewal_token`. The renewal token is stored in the access token
  metadata and vice versa.
  """
  @impl true
  def create(conn, user, config) do
    store_config = store_config(config)
    access_token = Pow.UUID.generate()
    renewal_token = Pow.UUID.generate()

    conn =
      conn
      |> Conn.put_private(:api_access_token, sign_token(conn, access_token, config))
      |> Conn.put_private(:api_renewal_token, sign_token(conn, renewal_token, config))

    CredentialsCache.put(store_config, access_token, {user, [renewal_token: renewal_token]})

    PersistentSessionCache.put(
      store_config,
      renewal_token,
      {[id: user.id], [access_token: access_token]}
    )

    {conn, user}
  end

  @doc """
  Delete the access token from the cache. Does not invalidate renewal token.

  The renewal token is deleted by fetching it from the access token metadata.
  """
  def delete(conn, config) do
    store_config = store_config(config)

    with {:ok, signed_token} <- fetch_access_token(conn),
         {:ok, token} <- verify_token(conn, signed_token, config),
         {_user, meta} <- CredentialsCache.get(store_config, token) do
      PersistentSessionCache.delete(store_config, meta[:renewal_token])
      CredentialsCache.delete(store_config, token)
    else
      _ -> :ok
    end

    conn
  end

  @doc """
  Creates new tokens using the renewal token.

  The access token, if any, will be deleted by fetching it from the renewal
  token metadata. The renewal token will be deleted from the store after the
  it has been fetched.
  """
  def renew(conn, config) do
    store_config = store_config(config)

    with {:ok, signed_token} <- fetch_access_token(conn),
         {:ok, token} <- verify_token(conn, signed_token, config),
         {clauses, metadata} <- PersistentSessionCache.get(store_config, token) do
      CredentialsCache.delete(store_config, metadata[:access_token])
      PersistentSessionCache.delete(store_config, token)

      load_and_create_session(conn, {clauses, metadata}, config)
    else
      _any -> {conn, nil}
    end
  end

  defp load_and_create_session(conn, {clauses, _metadata}, config) do
    case Pow.Operations.get_by(clauses, config) do
      nil -> {conn, nil}
      user -> create(conn, user, config)
    end
  end

  defp sign_token(conn, token, config) do
    Plug.sign_token(conn, signing_salt(), token, config)
  end

  defp signing_salt(), do: Atom.to_string(__MODULE__)

  defp fetch_access_token(conn) do
    case Conn.get_req_header(conn, "authorization") do
      [token | _rest] -> {:ok, token}
      _any -> :error
    end
  end

  defp verify_token(conn, token, config),
    do: Plug.verify_token(conn, signing_salt(), token, config)

  defp store_config(config) do
    backend = Config.get(config, :cache_store_backend, Pow.Store.Backend.EtsCache)

    [backend: backend]
  end
end
