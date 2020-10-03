defmodule Backpackingmap.Os.ExplorerTile do
  use Ecto.Schema
  import Ecto.Changeset
  require Logger
  alias Backpackingmap.Os.{Auth, Client}
  alias Backpackingmap.Repo

  @primary_key false
  schema "explorer_tiles" do
    field :col, :integer, primary_key: true
    field :row, :integer, primary_key: true
    field :png, :binary

    timestamps()
  end

  @doc """
  get({row, col}, %Auth{})
  """
  def get(loc, %Auth{} = auth) do
    with {:ok, png} <- fetch_stored_tile(loc, auth) do
      {:ok, png}
    else
      {:error, :not_stored} -> fetch_from_os_and_store(loc, auth)
    end
  end

  defp fetch_stored_tile({row, col}, auth) do
    if Auth.allowed_leisure_tiles?(auth) do
      case Repo.get_by(__MODULE__, %{row: row, col: col}) do
        %__MODULE__{png: png} -> {:ok, png}
        nil -> {:error, :not_stored}
      end
    else
      {:error, :not_authorized}
    end
  end

  defp fetch_from_os_and_store({row, col} = loc, auth) do
    {:ok, png} = fetch_from_os(loc, auth)

    insertion_changeset(%{png: png, row: row, col: col})
    |> Repo.insert!()

    {:ok, png}
  end

  def fetch_from_os(loc, auth) do
    with {:ok, {_headers, body}} <- Client.request(:get, tile_url(loc, auth)) do
      {:ok, body}
    else
      {:error, {403, headers, body}} ->
        Logger.warn("403 unauthorized fetching Explorer tile. headers: #{inspect(headers)}, body: #{body}")
        {:error, :unauthorized}

      {:error, {204, _headers, _body}} ->
        Logger.warn("204 nonexistent fetching Explorer tile.")

      {:error, error} ->
        Logger.error("Unknown error fetching an Explorer tile: #{inspect(error)}")
        {:error, :unknown}
    end
  end

  defp tile_url(loc, %Auth{leisure_token: url}) do
    url <> "?" <> URI.encode_query(tile_params(loc))
  end

  # NOTE: The web interface presents these layers under leisure:
  # Miniscale:
  #   Open access, we should download our own copy
  #   See <https://osdatahub.os.uk/downloads/open/MiniScale>
  # Regional view:
  #   Open access, we should download
  #   See <https://osdatahub.os.uk/downloads/open/250kScaleColourRaster>
  # Landranger:
  #   Paid, included with subscription
  #   See <https://www.ordnancesurvey.co.uk/business-government/products/50k-raster>
  #   %{
  #     "LAYER" => "1:50 000 Scale Colour Raster (5 and 10 metres per pixel)",
  #     "TILEMATRIXSET" => "1:50 000 Scale Colour Raster (5 and 10 metres per pixel)",
  #     "TILEMATRIX" => "50KR"
  #   }
  # Explorer:
  #   Paid, included
  #   See <https://www.ordnancesurvey.co.uk/business-government/products/25k-raster>

  defp tile_params({row, col}) do
    # From <https://osmaps.ordnancesurvey.co.uk/bundles/app/js/osmaps.js?version=f85e874aa>
    # this.mapLayers.push(new OpenLayers.Layer.WMTS({
    #   name: '25k',
    #   url: ServerData.OpenSpaceAPI,
    #   format: 'image/png',
    #   layer: '1:25 000 Scale Colour Raster (2.5 and 4 metres per pixel)',
    #   matrixSet: '1:25 000 Scale Colour Raster (2.5 and 4 metres per pixel)',
    #   matrixIds: [
    #     {
    #       identifier: '25K',
    #       tileWidth: 200,
    #       tileHeight: 200
    #     }
    #   ],
    #   style: 'default',
    #   serverResolutions: [
    #     2.5
    #   ],
    #   maxResolution: 4
    # }));
    #
    # That is put on a map with the projection EPSG:27700
    #
    # tileFullExtent: { bottom: 0,​​​ centerLonLat: {lon: 350000, lat: 650000}, left: 0, right: 700000, top: 1300000 }
    # tileOrigin: Object { lon: 0, lat: 1300000 },
    # tileSize: Object { w: 200, h: 200 },
    # units: "m"
    %{
      "SERVICE" => "WMTS",
      "REQUEST" => "GetTile",
      "VERSION" => "1.0.0",
      "STYLE" => "default",
      "LAYER" => "1:25 000 Scale Colour Raster (2.5 and 4 metres per pixel)",
      "TILEMATRIXSET" => "1:25 000 Scale Colour Raster (2.5 and 4 metres per pixel)",
      "TILEMATRIX" => "25K",
      "FORMAT" => "image/png",
      "TILEROW" => row,
      "TILECOL" => col
    }
  end

  def insertion_changeset(explorer_tile \\ %__MODULE__{}, attrs) do
    explorer_tile
    |> cast(attrs, [:row, :col, :png])
    |> validate_required([:row, :col, :png])
  end
end
