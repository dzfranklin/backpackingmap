defmodule Backpackingmap.OsRequester.Request do
  @enforce_keys [:caller, :params, :received_at]

  @type monotonic_milliseconds :: integer()

  @type t :: %__MODULE__{caller: pid(), params: Enum.t(), received_at: monotonic_milliseconds()}

  defstruct caller: nil, params: nil, received_at: nil
end
