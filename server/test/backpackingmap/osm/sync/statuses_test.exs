defmodule Backpackingmap.Osm.Sync.StatusesTest do
  use Backpackingmap.DataCase
  alias Backpackingmap.Repo
  alias Backpackingmap.Osm.Sync.Statuses
  alias Statuses.Status

  @region_name "imaginary_region"
  @region_index %{"id" => @region_name}

  def status_fixture do
    Status.changeset(%{
      region: @region_name,
      success?: true,
      osm2pgsql_output: "osm2pgsql_output",
      append?: true,
      exported_at: DateTime.utc_now(),
      sequence_number: 1
    })
  end

  test "most_recent_success_for/1 returns nil if none present" do
    assert is_nil(Statuses.most_recent_success_for(@region_index))
  end

  test "most_recent_success_for doesn't return failed" do
    status_fixture()
    |> Status.changeset(%{success?: false})
    |> Repo.insert!()

    assert Statuses.most_recent_success_for(@region_index) == nil
  end

  test "most_recent_success_for returns the most recent present if multiple" do
    status_fixture()
    |> Repo.insert!()

    status_fixture()
    |> Status.changeset(%{sequence_number: 2})
    |> Repo.insert!()

    status_fixture()
    |> Status.changeset(%{sequence_number: 3})
    |> Repo.insert!()

    response = Statuses.most_recent_success_for(@region_index)
    assert response.sequence_number == 3
  end
end
