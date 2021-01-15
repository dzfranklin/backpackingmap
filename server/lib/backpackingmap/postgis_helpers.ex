defmodule Backpackingmap.PostgisHelpers do
  defmacro st_as_geo_json(row) do
    quote do
      fragment("ST_AsGeoJSON(?)", unquote(row))
    end
  end

  @doc """
    Creates a rectangular Polygon in Web Mercator (SRID:3857) using the XYZ tile system.
    <https://postgis.net/docs/ST_TileEnvelope.html>
  """
  defmacro st_tile_envelope(zoom, x, y) do
    quote do
      fragment("ST_TileEnvelope(?, ?, ?)", unquote(zoom), unquote(x), unquote(y))
    end
  end

  defmacro st_transform(geom, srid) do
    quote do
      fragment("ST_Transform(?, ?)", unquote(geom), unquote(srid))
    end
  end

  @doc """
  <https://postgis.net/docs/geometry_overlaps.html>
  """
  defmacro geometry_overlaps?(a, b) do
    quote do
      fragment("? && ?", unquote(a), unquote(b))
    end
  end

  @doc """
  bytea ST_AsMVT(anyelement row, text name, integer extent, text geom_name)
  <https://postgis.net/docs/ST_AsMVT.html>
  """
  defmacro st_as_mvt(row, name, extent, geom_name) do
    quote do
      fragment(
        "ST_AsMVT(?, ?, ?, ?)",
        unquote(row),
        unquote(name),
        unquote(extent),
        unquote(geom_name)
      )
    end
  end

  @doc """
  geometry ST_AsMVTGeom(geometry geom, box2d bounds, integer extent=4096, integer buffer=256, boolean clip_geom=true)
  <https://postgis.net/docs/ST_AsMVT.html>
  """
  defmacro st_as_mvt_geom(
             geom,
             bounds,
             extent \\ 4096,
             buffer \\ 256,
             clip? \\ true
           ) do
    quote do
      fragment(
        "ST_AsMVTGeom(?, ?, ?, ?, ?)",
        unquote(geom),
        unquote(bounds),
        unquote(extent),
        unquote(buffer),
        unquote(clip?)
      )
    end
  end
end
