defmodule Backpackingmap.Users.User do
  use Ecto.Schema
  use Pow.Ecto.Schema
  import Pow.Ecto.Schema.Changeset, only: [new_password_changeset: 3]

  schema "users" do
    pow_user_fields()
    timestamps()
  end

  def changeset(user_or_changeset, attrs) do
    user_or_changeset
    |> pow_user_id_field_changeset(attrs)
    |> pow_current_password_changeset(attrs)
    |> new_password_changeset(attrs, @pow_config)
  end
end
