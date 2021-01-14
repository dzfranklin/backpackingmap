defmodule Backpackingmap.Osm.Sync do
  require Logger
  alias Backpackingmap.Osm.Sync.Statuses

  @ua_header Application.compile_env!(:backpackingmap, :ua_header)

  @db_config Application.get_env(:backpackingmap, Backpackingmap.Osm.Repo)
  @db_hostname Keyword.fetch!(@db_config, :hostname)
  @db_database Keyword.fetch!(@db_config, :database)
  @db_username Keyword.fetch!(@db_config, :username)
  @db_password Keyword.fetch!(@db_config, :password)

  defp osm2pgsql_config, do:
    Path.join(Application.app_dir(:backpackingmap, "priv"), "/osm2pgsql_config.lua")

  def make_latest(region) do
    case region_geofabrik_index(region) do
      {:ok, index} ->
        latest = Statuses.most_recent_success_for(index)
        if is_nil(latest) do
          import_from_scratch(index)
        else
          import_changed(index, latest)
        end
      :error -> {:error, :unknown_region}
    end
  end

  def import_from_scratch(region_index) do
    pbf_url = region_index["urls"]["pbf"]

    # We get the state before and after we get the checksum to ensure that both refer to the same item.
    # Otherwise if we request when they are uploading new files we could get inconsistencies.

    state = latest_remote_state(region_index)

    {:ok, %{status_code: 200, body: checksum_file}} = HTTPoison.get(
      pbf_url <> ".md5",
      [@ua_header],
      follow_redirect: true
    )
    [checksum, _] = String.split(checksum_file, "  ")

    state_2 = latest_remote_state(region_index)

    if state != state_2 do
      raise "Inconsistent state. Likely new remote files are being uploaded right now. state: #{
        inspect(state)
      }, state2: #{inspect(state_2)}"
    end

    {:ok, path} = download(pbf_url)

    file_md5 = streaming_md5(path)
    if file_md5 != checksum do
      raise "File does not match checksum. Expected: #{checksum}, actual: #{file_md5}"
    end

    result = osm2pgsql(path, :create)

    Statuses.record(region_index, false, state, result)
  end

  def import_changed(_, _), do:
    raise "TODO"

  def latest_remote_state(region_index) do
    state_url = region_index["urls"]["updates"] <> "/state.txt"
    {:ok, %{status_code: 200, body: state}} = HTTPoison.get(state_url, [@ua_header], follow_redirect: true)

    [_header, timestamp_line, sequence_number_line] =
      state
      |> String.trim("\n")
      |> String.split("\n")
    ["timestamp", timestamp] = String.split(timestamp_line, "=")
    ["sequenceNumber", sequence_number] = String.split(sequence_number_line, "=")

    # 0 offset means the DateTime was correctly interpreted as UTC
    {:ok, timestamp, 0} =
      timestamp
      |> String.replace("\\:", ":")
      |> DateTime.from_iso8601()

    sequence_number = String.to_integer(sequence_number)

    {timestamp, sequence_number}
  end

  def region_geofabrik_index(region_name), do:
    Path.join(Application.app_dir(:backpackingmap, "priv"), "/geofabrik_index.json")
    |> File.read!()
    |> Jason.decode!()
    |> Map.fetch!("features")
    |> Enum.map(&Map.fetch!(&1, "properties"))
    |> Map.new(&{Map.fetch!(&1, "id"), &1})
    |> Map.fetch(region_name)

  @spec osm2pgsql(binary(), :create | :append) :: {:ok, binary(), :error, binary()}
  def osm2pgsql(path, mode \\ :create) do
    # See <https://osm2pgsql.org/doc/manual.html>
    # and <https://pavie.info/2020/03/09/openstreetmap-data-processing-osm2pgsql-flex/>

    # NOTE: We may want to run this on a separate spot instance b/c/o high memory usage

    mode = case mode do
      :create -> "--create"
      :append -> "--append"
      _ -> raise "Invalid mode: #{inspect(mode)}"
    end

    Logger.info("Beginning PBF import")

    # Consider that doesn't guarantee it doesn't duplicate ids b/c items can get split up

    {messages, status} = System.cmd(
      "osm2pgsql",
      [
        mode,
        "--slim",
        "--output=flex",
        "--extra-attributes",
        "--style=#{osm2pgsql_config()}",
        "--database=#{@db_database}",
        "--user=#{@db_username}",
        "--host=#{@db_hostname}",
        path
      ],
      stderr_to_stdout: true,
      env: [{"PGPASSWORD", @db_password}]
    )

    if status == 0 do
      Logger.info("PBF import succeeded: #{messages}")
      {:ok, messages}
    else
      Logger.error("PBF import failed: #{messages}")
      {:error, messages}
    end
  end

  @doc """
    Caller is responsible for cleaning up temp file.
  """
  def download(url) do
    {:ok, %{id: ref}} = HTTPoison.get(url, [@ua_header], stream_to: self(), max_body_length: :infinity)

    {fd, path} = Temp.open!(suffix: ".pbf")
    result = receive_loop(ref, fd)

    with {:ok, status} <- result do
      if success_code?(status) do
        {:ok, path}
      else
        # Return a binary instead of a path
        body = File.read!(path)
        # Clean up after ourselves
        File.rm!(path)
        {:error, {:http, status, body}}
      end
    else
      {:error, error} -> {:error, error}
    end
  end

  defp receive_loop(ref, fd, status \\ nil, length_received \\ 0, total_length \\ nil) do
    receive do
      %HTTPoison.AsyncRedirect{id: ^ref} ->
        receive_loop(ref, fd, status, length_received, total_length)
      %HTTPoison.AsyncStatus{id: ^ref, code: code} ->
        receive_loop(ref, fd, code, length_received, total_length)
      %HTTPoison.AsyncHeaders{id: ^ref, headers: headers} ->
        total_length = get_content_length(headers)
        receive_loop(ref, fd, status, length_received, total_length)
      %HTTPoison.AsyncChunk{chunk: chunk, id: ^ref} ->
        IO.binwrite(fd, chunk)
        length_received = length_received + byte_size(chunk)
        if !is_nil(total_length) do
          percent =
            (length_received / total_length)
            |> Kernel.*(1.0e5)
            |> round()
            |> Kernel./(1.0e3)
          Logger.debug("Wrote #{percent}% of download #{inspect(ref)}")
        else
          Logger.debug("Wrote #{length_received} bytes of download #{inspect(ref)}")
        end
        receive_loop(ref, fd, status, length_received, total_length)
      %HTTPoison.AsyncEnd{id: ^ref} ->
        File.close(fd)
        {:ok, status}
    after
      :timer.minutes(5) -> {:error, :receive_loop_timeout}
    end
  end

  defp get_content_length(headers) do
    headers
    |> Enum.find_value(
         fn
           {"Content-Length", length} -> String.to_integer(length)
           _ -> nil
         end
       )
  end

  defp success_code?(status), do:
    200 <= status and status < 300

  defp streaming_md5(file) do
    # Based on <http://www.cursingthedarkness.com/2015/04/how-to-get-hash-of-file-in-exilir.html>
    File.stream!(file, [], 2048)
    |> Enum.reduce(
         :crypto.hash_init(:md5),
         fn (line, acc) -> :crypto.hash_update(acc, line) end
       )
    |> :crypto.hash_final()
    |> Base.encode16(case: :lower)
  end
end
