defmodule Backpackingmap.OsRequester.QuotaAllocatorTest do
  use ExUnit.Case, async: true
  alias Backpackingmap.OsRequester.QuotaAllocator

  test "flushes" do
    me = self()
    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn n -> send(me, {:success, n}) end,
      reject_item: fn n -> send(me, {:failure, n}) end,
      allocation_increment: 1,
      allocation_interval: 100,
      reset_interval: :timer.minutes(1),
      reject_if_would_wait: :timer.minutes(1)
    )

    # Create a backlog
    Enum.map(0..1000, fn n -> QuotaAllocator.enqueue(subject, n) end)

    # Clear the backlog
    QuotaAllocator.flush(subject)

    # Ensure that there is no backlog by verifying a new item is processed within an allocation_interval
    QuotaAllocator.enqueue(subject, 42_000)
    assert_receive {:success, 42_000}, 150
  end

  test "can be initialized" do
    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn _ -> nil end,
      reject_item: fn _ -> nil end,
      allocation_increment: 1,
      allocation_interval: :timer.seconds(1),
      reset_interval: :timer.minutes(1),
      reject_if_would_wait: :timer.minutes(1)
    )

    :timer.sleep(1)

    assert Process.alive?(subject)
  end

  test "processes immediately if well under quota" do
    {:ok, items_agent} = Agent.start_link(fn -> %{} end)

    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn {n, enqueued_at} ->
                       spawn(
                         fn ->
                           Agent.update(items_agent, Map, :put, [n, {enqueued_at, monotonic_time()}])
                         end
                       )
      end,
      reject_item: fn _ -> nil end,
      allocation_increment: 100,
      allocation_interval: 1,
      reset_interval: :timer.seconds(1),
      reject_if_would_wait: :timer.seconds(10)
    )

    :timer.sleep(5)

    for n <- 0..50 do
      QuotaAllocator.enqueue(subject, {n, monotonic_time()})
    end

    :timer.sleep(5)

    items = Agent.get(items_agent, fn item -> item end)

    for n <- 0..50 do
      item = items[n]
      assert not is_nil(item)

      {enqueued_at, dequeued_at} = item
      assert_equal_within(enqueued_at, dequeued_at, 1)
    end
  end

  test "processes at the correct times" do
    {:ok, items_agent} = Agent.start_link(fn -> %{} end)

    start_time = monotonic_time()
    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn {n, enqueued_at} ->
                       received_at = monotonic_time()
                       spawn(
                         fn ->
                           Agent.update(items_agent, Map, :put, [n, {enqueued_at, received_at}])
                         end
                       )
      end,
      reject_item: fn _ -> nil end,
      allocation_increment: 2,
      allocation_interval: 100,
      reset_interval: :timer.minutes(1),
      reject_if_would_wait: 400,
    )

    for n <- 0..500 do
      QuotaAllocator.enqueue(subject, {n, monotonic_time()})
    end

    :timer.sleep(500)

    items = Agent.get(items_agent, fn items -> items end)

    # First increment
    {item_in, item_out} = items[0]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 100, item_out, 10)
    {item_in, item_out} = items[1]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 100, item_out, 10)

    # Second increment
    {item_in, item_out} = items[2]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 200, item_out, 10)
    {item_in, item_out} = items[3]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 200, item_out, 10)

    # Third increment
    {item_in, item_out} = items[4]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 300, item_out, 10)
    {item_in, item_out} = items[5]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 300, item_out, 10)

    # Fourth increment
    {item_in, item_out} = items[6]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 400, item_out, 10)
    {item_in, item_out} = items[7]
    assert_equal_within(start_time, item_in, 1)
    assert_equal_within(start_time + 400, item_out, 10)
  end

  test "sheds excess backlog correctly" do
    {:ok, items_agent} = Agent.start_link(fn -> MapSet.new() end)

    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn n ->
                       spawn(
                         fn ->
                           Agent.update(items_agent, MapSet, :put, [n])
                         end
                       )
      end,
      reject_item: fn _ -> nil end,
      allocation_increment: 100,
      allocation_interval: div(:timer.seconds(1), 10),
      reset_interval: :timer.minutes(1),
      reject_if_would_wait: :timer.seconds(1),
    )

    for n <- 0..10_000 do
      QuotaAllocator.enqueue(subject, n)
    end

    :timer.sleep(5)

    items = Agent.get(items_agent, fn items -> items end)

    assert MapSet.difference(items, MapSet.new(0..1_000)) == MapSet.new([])
  end

  test "accumulates quota within a refresh interval" do
    {:ok, items_agent} = Agent.start_link(fn -> MapSet.new() end)

    {:ok, subject} = QuotaAllocator.start_link(
      process_item: fn n ->
                       spawn(
                         fn ->
                           Agent.update(items_agent, MapSet, :put, [n])
                         end
                       )
      end,
      reject_item: fn _ -> nil end,
      allocation_increment: 100,
      allocation_interval: 100,
      reset_interval: 3000,
      reject_if_would_wait: :timer.seconds(1),
    )

    :timer.sleep(200)

    for n <- 0..1_000 do
      QuotaAllocator.enqueue(subject, n)
    end

    :timer.sleep(10)

    items = Agent.get(items_agent, fn items -> items end)

    assert MapSet.size(items) == 200
  end

  defp assert_equal_within(a, b, max_diff) do
    diff = abs(a - b)
    try do
      assert diff <= max_diff
    catch
      _, _ ->
        IO.puts(:stderr, "assert_equal_within with max_diff `#{inspect(max_diff)}` failed:")
        IO.puts(:stderr, "a: #{inspect(a)}")
        IO.puts(:stderr, "b: #{inspect(b)}")
        assert diff <= max_diff
    end
  end

  defp monotonic_time, do:
    :erlang.monotonic_time(:millisecond)
end
