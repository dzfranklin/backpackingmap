defmodule Backpackingmap.OsRequesterTest do
  alias Backpackingmap.OsRequester
  require Logger
  use ExUnit.Case, async: false

  @tile_params %{"__dummy" => "tile1"}
  @tile2_params %{"__dummy" => "tile2"}
  @max_per_second OsRequester.transactions_per_second() * OsRequester.raster_tiles_per_transaction()
  @max_millis_enqueued OsRequester.max_millis_enqueued()
  @max_seconds_enqueued div(@max_millis_enqueued, :timer.seconds(1))

  setup do
    OsRequester.flush_queue()
  end

  test "fulfills requests" do
    {:ok, tile} = OsRequester.request_raster_tile(@tile_params)
  end

  test "fulfills concurrent requests correctly" do
    {:ok, tile1_sample} = OsRequester.request_raster_tile(@tile_params)
    :timer.sleep(10)
    {:ok, tile2_sample} = OsRequester.request_raster_tile(@tile2_params)
    :timer.sleep(10)

    assert tile1_sample != tile2_sample

    pairs =
      0..(div(@max_per_second, 2) - 2)
      |> Enum.map(
           fn _ ->
             task1 = Task.async(
               fn ->
                 {:ok, tile1} = OsRequester.request_raster_tile(@tile_params)
                 tile1
               end
             )

             task2 = Task.async(
               fn ->
                 {:ok, tile2} = OsRequester.request_raster_tile(@tile2_params)
                 tile2
               end
             )

             [task1, task2]
           end
         )
      |> Enum.map(fn pair -> Enum.map(pair, &Task.await/1) end)

    for [tile1, tile2] <- pairs do
      assert tile1 == tile1_sample
      assert tile2 == tile2_sample
    end
  end

  @tag timeout: @max_millis_enqueued + :timer.seconds(1)
  test "handles massively over the limit" do
    {micros, responses} = :timer.tc(
      fn ->
        1..(@max_per_second * 120)
        |> Enum.map(fn _ -> Task.async(&request_default_tile/0) end)
        |> Enum.map(&Task.await/1)
      end
    )

    millis = microseconds_to_milliseconds(micros)
    seconds = millis / :timer.seconds(1)

    succeeded =
      responses
      |> Enum.filter(&success?/1)
      |> length()

    failed =
      responses
      |> Enum.filter(&failure?/1)
      |> length()

    # should enqueue some for the future
    assert succeeded > @max_per_second

    # but most of the requests we made should fail
    assert failed > succeeded

    # and it should have taken more than a second to request all, as some were enqueued
    assert seconds > 1

    # and we should have hit the cap for max wait time
    assert_in_delta(millis, OsRequester.max_millis_enqueued, 1_000)
  end

  @tag timeout: @max_millis_enqueued + :timer.seconds(1)
  test "all of max allowed to wait succeed" do
    successes =
      1..(@max_per_second * @max_seconds_enqueued)
      |> Enum.map(fn _ -> Task.async(&request_default_tile/0) end)
      |> Enum.map(&Task.await/1)
      |> Enum.filter(&success?/1)
      |> length()

    assert successes == @max_per_second * @max_seconds_enqueued
  end

  @tag timeout: @max_millis_enqueued + :timer.seconds(2)
  test "all over max allowed to wait fail" do
      failures =
        1..(@max_per_second * @max_seconds_enqueued + 100)
        |> Enum.map(fn _ -> Task.async(&request_default_tile/0) end)
        |> Enum.map(&Task.await/1)
        |> Enum.filter(&failure?/1)
        |> length()

      assert failures = 100
  end

  test "adds little overhead" do
    :timer.sleep(:timer.seconds(1)) # ensure we have quota

    base_url = Application.get_env(:backpackingmap, :os_raster_api_base_url) || raise "Missing env key"
    request_time_micros =
      :timer.tc(
        fn ->
          0..1_000
          |> Enum.map(fn n -> HTTPoison.get("#{base_url}?n=#{n}") end)
          |> Enum.map(fn {:ok, _} -> nil end) # ensure succeeded
        end
      )
      |> elem(0)
      |> Kernel./(1_000)

    micros =
      :timer.tc(
        fn ->
          0..(@max_per_second - 1)
          |> Enum.map(fn _ -> Task.async(&request_default_tile/0) end)
          |> Enum.map(&Task.await/1)
          |> Enum.map(fn {:ok, _} -> nil end) # ensure succeeded
        end
      )
      |> elem(0)

    overhead = microseconds_to_milliseconds(micros - request_time_micros)

    assert overhead < 40
  end

  test "handles API errors" do
    response = OsRequester.request_raster_tile(%{"simulate_failure_with_status_code" => 500})
    assert {:error, {:os_server, {500, "Simulated failure"}}} == response
  end

  #  test "doesn't request downloads if busy" do
  #    download_successes =
  #      0..(@max_per_second * 10_000_000)
  #      |> Enum.map(fn _ -> Task.async(&OsRequester.request_raster_tile_for_download(@tile_params)) end)
  #      |> Enum.map(&Task.await/1)
  #      |> Enum.filter(&success?/1)
  #      |> length()
  #
  #    normal_successes =
  #      0..(@max_per_second * 10_000_000)
  #      |> Enum.map(fn _ -> Task.async(&OsRequester.request_raster_tile(@tile_params)) end)
  #      |> Enum.map(&Task.await/1)
  #      |> Enum.filter(&success?/1)
  #      |> length()
  #
  #    assert normal_successes > download_successes
  #    assert normal_successes > @max_per_second
  #    assert download_successes
  #  end

  def success?({:ok, _}), do: true
  def success?({:error, _}), do: false
  def failure?({:ok, _}), do: false
  def failure?({:error, _}), do: true

  def request_default_tile, do:
    OsRequester.request_raster_tile(@tile_params)

  def microseconds_to_milliseconds(micros) when is_integer(micros), do: div(micros, 1_000)
  def microseconds_to_milliseconds(micros), do: micros / 1_000.0
end
