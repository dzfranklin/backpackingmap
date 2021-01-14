defmodule Backpackingmap.Osm.SyncTest do
  use ExUnit.Case, async: true
  alias Backpackingmap.Osm.Sync

  test "import_from_scratch" do
    assert {:ok, _} = Sync.import_from_scratch(
             %{
               "id" => "vatican-city",
               "urls" => %{
                 "pbf" => "http://localhost:5917/mock.pbf",
                 "updates" => "http://localhost:5917/mock_region/updates"
               }
             }
           )
  end

  test "osm2pgsql with :create mode (default)" do
    assert {:ok, _} = Sync.osm2pgsql("priv/test_assets/vatican_city.osm.pbf")
  end

  test "osm2pgsql with :append mode" do
    assert {:ok, _} = Sync.osm2pgsql("priv/test_assets/monaco-latest.osm.pbf")
    assert {:ok, _} = Sync.osm2pgsql("priv/test_assets/monaco-updates-779.osc.gz", :append)
  end

  test "download" do
    assert {:ok, path} = Sync.download("http://localhost:5917/mock.pbf")
    assert File.exists?(path)
    assert File.read!(path) == File.read!("priv/test_assets/vatican_city.osm.pbf")
    File.rm!(path)
  end

  test "download handles server error" do
    assert {:error, {:http, 500, "Server error"}} = Sync.download("http://localhost:5917/error/mock.pbf")
  end
end
