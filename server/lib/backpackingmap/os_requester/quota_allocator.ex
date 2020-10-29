defmodule Backpackingmap.OsRequester.QuotaAllocator do
  require Logger
  @type process_item_callback :: (any() -> nil)
  @type reject_item_callback :: (any() -> nil)
  @type milliseconds :: non_neg_integer()

  @type opt :: {:process_item, process_item_callback()} |
               {:reject_item, reject_item_callback()} |
               {:allocation_increment, non_neg_integer()} |
               {:allocation_interval, milliseconds()} |
               {:reset_interval, milliseconds()} |
               {:reject_if_would_wait, milliseconds()} |
               {:name, atom()}

  @doc """
  LIFO queue that rejects new items if the backlog gets too large.

  Every option except name is required.
  """
  @spec start_link([opt()]) :: {:ok, pid()}
  def start_link(opts) do
    name = Keyword.get(opts, :name)
    process_item = Keyword.fetch!(opts, :process_item)
    reject_item = Keyword.fetch!(opts, :reject_item)
    allocation_increment = Keyword.fetch!(opts, :allocation_increment)
    allocation_interval = Keyword.fetch!(opts, :allocation_interval)
    reset_interval = Keyword.fetch!(opts, :reset_interval)
    reject_if_would_wait = Keyword.fetch!(opts, :reject_if_would_wait)

    queue_size = allocation_increment * div(reject_if_would_wait, allocation_interval)

    pid = spawn_link(
      fn ->
        loop(
          %{
            allocated: 0,
            process_item: process_item,
            reject_item: reject_item,
            last_allocated: monotonic_time(),
            allocation_interval: allocation_interval,
            allocation_increment: allocation_increment,
            last_reset: monotonic_time(),
            reset_interval: reset_interval,
            queue_size: queue_size,
            queue: Deque.new(queue_size),
          }
        )
      end
    )

    if not is_nil(name), do: Process.register(pid, name)

    Logger.info("Started #{inspect(__MODULE__)} with opts #{inspect(opts)}")

    {:ok, pid}
  end

  def flush(allocator \\ __MODULE__) do
    send(allocator, {:flush, self()})
    receive do
      :flushed -> :ok
    after
      :timer.minutes(1) -> {:error, :unreachable}
    end
  end

  def enqueue(allocator \\ __MODULE__, item) do
    send(allocator, {:enqueue, item})
    nil
  end

  defp loop(state) do
    now = monotonic_time()

    state
    |> catchup_allocations(now)
    |> catchup_resets(now)
    |> run_allocated()
    |> receive_blocking()
    |> loop()
  end

  defp catchup_allocations(state, now) do
    count = div(now - state.last_allocated, state.allocation_interval)
    if count > 0 do
      %{state | allocated: state.allocated + count * state.allocation_increment, last_allocated: now}
    else
      state
    end
  end

  defp catchup_resets(state, now) do
    if now - state.last_reset > state.reset_interval do
      %{state | allocated: 0, last_reset: now}
    else
      state
    end
  end

  defp run_allocated(%{allocated: allocated} = state) when allocated <= 0, do: state
  defp run_allocated(%{allocated: allocated, queue: queue, process_item: process_item} = state) do
    {item, queue} = Deque.popleft(queue)
    if is_nil(item) do
      state
    else
      spawn(fn -> process_item.(item) end)
      run_allocated(%{state | allocated: allocated - 1, queue: queue})
    end
  end

  defp receive_blocking(%{queue: queue} = state) do
    receive do
      {:flush, caller} ->
        send(caller, :flushed)
        %{state | queue: Deque.new(state.queue_size)}

      {:enqueue, item} ->
        if queue.size == queue.max_size do
          state.reject_item.(item)
          state
        else
          %{state | queue: Deque.append(state.queue, item)}
        end
    after
      state.allocation_interval ->
        state
    end
  end

  defp monotonic_time, do: :erlang.monotonic_time(:millisecond)
end
