defmodule Backpackingmap.Osm.Sync.Statuses do
  alias Backpackingmap.Repo
  alias Backpackingmap.Osm.Sync
  alias Backpackingmap.Osm.Sync.Statuses.Status
  import Ecto.Query

  def record(region_index, append?, state, osm2pgsql_result) do
    {success_status, osm2pgsql_output} = osm2pgsql_result
    {exported_at, sequence_number} = state

    success? = case success_status do
      :ok -> true
      :error -> false
    end

    Status.changeset(
      %{
        region: region_index["id"],
        success?: success?,
        osm2pgsql_output: osm2pgsql_output,
        append?: append?,
        exported_at: exported_at,
        sequence_number: sequence_number
      }
    )
    |> Repo.insert()
  end

  def most_recent_success_for(region_index) do
    region = Map.fetch!(region_index, "id")

    Status
    |> where([s], s.region == ^region)
    |> where([s], s.success?)
    |> order_by({:desc, :sequence_number})
    |> limit(1)
    |> Repo.one()
  end
end
