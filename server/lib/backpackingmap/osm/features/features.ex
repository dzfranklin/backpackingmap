defmodule Backpackingmap.Osm.Features do
  alias Backpackingmap.Osm
  alias Osm.Features.{Boundary, Line, Point, Polygon, Route}
  import Ecto.Query
  import Backpackingmap.PostgisHelpers

  @all_features [Boundary, Line, Point, Polygon, Route]
  @wgs84_srid 4326

  @doc """
  Sample in England: x = 507, y = 328
  """
  @spec get_tile(integer(), integer()) :: iolist()
  def get_tile(x, y) do
    @all_features
    |> Enum.map(&select_feature_in_tile(&1, x, y))
    |> Enum.map(&Task.async(fn -> Osm.Repo.one(&1) end))
    |> Enum.map(&Task.await/1)
  end

  defmacro tile_bounds(x, y) do
    quote do
      st_tile_envelope(10, unquote(x), unquote(y))
      |> st_transform(@wgs84_srid)
    end
  end

  defmacro geom_as_mvt_geom(x, y) do
    quote do
      st_as_mvt_geom(fragment("geom"), tile_bounds(unquote(x), unquote(y)))
    end
  end

  def select_feature_in_tile(Boundary, x, y) do
    Boundary
    |> where_in_tile(x, y)
    |> select(
         [p],
         %{
           relation_id: p.relation_id,
           tags: p.tags,
           changeset_id: p.changeset_id,
           geom: geom_as_mvt_geom(^x, ^y)
         }
       )
    |> as_mvt("boundaries")
  end

  def select_feature_in_tile(Line, x, y) do
    Line
    |> where_in_tile(x, y)
    |> select(
         [p],
         %{
           way_id: p.way_id,
           tags: p.tags,
           geom: geom_as_mvt_geom(^x, ^y)
         }
       )
    |> as_mvt("lines")
  end

  def select_feature_in_tile(Point, x, y) do
    Point
    |> where_in_tile(x, y)
    |> select(
         [p],
         %{
           node_id: p.node_id,
           tags: p.tags,
           changeset_id: p.changeset_id,
           geom: geom_as_mvt_geom(^x, ^y)
         }
       )
    |> as_mvt("points")
  end

  def select_feature_in_tile(Polygon, x, y) do
    Polygon
    |> where_in_tile(x, y)
    |> select(
         [p],
         %{
           area_id: p.area_id,
           tags: p.tags,
           area: p.area,
           changeset_id: p.changeset_id,
           geom: geom_as_mvt_geom(^x, ^y)
         }
       )
    |> as_mvt("polygons")
  end

  def select_feature_in_tile(Route, x, y) do
    Route
    |> where_in_tile(x, y)
    |> select(
         [p],
         %{
           relation_id: p.relation_id,
           tags: p.tags,
           changeset_id: p.changeset_id,
           geom: geom_as_mvt_geom(^x, ^y)
         }
       )
    |> as_mvt("routes")
  end

  defp as_mvt(data_query, name) do
    from(subquery(data_query))
    |> select([d], st_as_mvt(d, ^name, 4096, "geom"))
  end

  defp where_in_tile(query, x, y), do:
    where(query, [r], geometry_overlaps?(r.geom, tile_bounds(^x, ^y)))
end
