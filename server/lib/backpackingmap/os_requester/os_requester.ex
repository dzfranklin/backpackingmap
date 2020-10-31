defmodule Backpackingmap.OsRequester do
  alias Backpackingmap.OsRequester.{Request, QuotaAllocator}

  @base_url Application.compile_env!(:backpackingmap, :os_raster_api_base_url)
  @ua_header Application.compile_env!(:backpackingmap, :ua_header)

  @seconds_per_minute 60

  # "All our API data (OS OpenData and Premium) are subject to a 600 transactions-per-minute throttle
  # for your live projects."
  # -- <https://osdatahub.os.uk/plans>
  @transactions_per_minute 600

  # Currently we divide the minute fairly
  def transactions_per_second, do: div(@transactions_per_minute, @seconds_per_minute)

  # "API: OS Maps API (WMTS), 1 Transaction: 1 map view (15 raster tiles)"
  # -- <https://osdatahub.os.uk/support/plans#apiTransactions>
  def raster_tiles_per_transaction, do: 15

  @max_millis_enqueued :timer.seconds(5)
  def max_millis_enqueued, do: @max_millis_enqueued

  def child_spec(_params), do: %{
    id: QuotaAllocator,
    start: {
      QuotaAllocator,
      :start_link,
      [
        [
          name: QuotaAllocator,
          process_item: &make_request/1,
          reject_item: &reject_request/1,
          allocation_increment: transactions_per_second() * raster_tiles_per_transaction(),
          allocation_interval: :timer.seconds(1),
          reset_interval: :timer.minutes(1),
          reject_if_would_wait: @max_millis_enqueued
        ]
      ]
    }
  }

  @type params :: Enum.t()
  @type status_code :: integer()
  @type response_body :: term()
  @type request_error :: :timeout |
                         :rejected_backlog_too_large |
                         {:os_server, {status_code(), response_body()}} |
                         {:http, HTTPoison.Error.t()}

  @spec flush_queue :: :ok | {:error, :unreachable}
  def flush_queue do
    QuotaAllocator.flush()
  end

  @spec request_raster_tile(params()) :: {:ok, response_body()} | {:error, request_error()}
  def request_raster_tile(params) do
    received_at = monotonic_time()
    request = %Request{caller: self(), params: params, received_at: received_at}
    QuotaAllocator.enqueue(request)
    receive do
      {request, response} ->
        response
    after
      :timer.minutes(2) ->
        {:error, :timeout}
    end
  end

  defp make_request(%{caller: caller} = request) do
    url = "#{@base_url}?key=#{api_key()}&#{URI.encode_query(request.params)}"

    with {:ok, %{status_code: 200, body: body}} <- HTTPoison.get(url, [@ua_header]) do
      send(caller, {request, {:ok, body}})
    else
      {:ok, %{status_code: status, body: body}} ->
        send(caller, {request, {:error, {:os_server, {status, body}}}})
      {:error, error} ->
        send(caller, {request, {:error, {:http, error}}})
    end
  end

  defp reject_request(%{caller: caller} = request), do:
    send(caller, {request, {:error, :rejected_backlog_too_large}})

  defp api_key, do:
    Application.get_env(:backpackingmap, :os_api_key) || raise "Missing OS API key"

  defp monotonic_time, do: :erlang.monotonic_time(:millisecond)
end
