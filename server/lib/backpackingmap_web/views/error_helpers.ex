defmodule BackpackingmapWeb.ErrorHelpers do
  @moduledoc """
  Conveniences for translating and building error messages.
  """

  use Phoenix.HTML

  def translate_changeset(%Ecto.Changeset{} = changeset) do
    Ecto.Changeset.traverse_errors(changeset, &translate_error/1)
    |> Enum.map(
         fn {field, errors} ->
           field = Atom.to_string(field)
           {field, "#{titlecase(field)} #{join_list(errors)}."}
         end
       )
    |> Map.new()
  end

  defp titlecase(s) do
    first =
      String.at(s, 0)
      |> String.upcase()

    rest = String.slice(s, 1..-1)

    "#{first}#{rest}"
  end

  defp join_list([s]), do: s
  defp join_list([s1, s2]), do: "#{s1} and #{s2}"
  defp join_list(list) do
    last = Enum.at(list, -1)
    rest = Enum.slice(list, 0..-2)
    "#{Enum.join(rest, ", ")}, and #{last}"
  end

  @doc """
  Generates tag for inlined form input errors.
  """
  def error_tag(form, field) do
    Enum.map(
      Keyword.get_values(form.errors, field),
      fn error ->
        content_tag(
          :span,
          translate_error(error),
          class: "invalid-feedback",
          phx_feedback_for: input_id(form, field)
        )
      end
    )
  end

  @doc """
  Translates an error message using gettext.
  """
  def translate_error({msg, opts}) do
    # When using gettext, we typically pass the strings we want
    # to translate as a static argument:
    #
    #     # Translate "is invalid" in the "errors" domain
    #     dgettext("errors", "is invalid")
    #
    #     # Translate the number of files with plural rules
    #     dngettext("errors", "1 file", "%{count} files", count)
    #
    # Because the error messages we show in our forms and APIs
    # are defined inside Ecto, we need to translate them dynamically.
    # This requires us to call the Gettext module passing our gettext
    # backend as first argument.
    #
    # Note we use the "errors" domain, which means translations
    # should be written to the errors.po file. The :count option is
    # set by Ecto and indicates we should also apply plural rules.
    if count = opts[:count] do
      Gettext.dngettext(BackpackingmapWeb.Gettext, "errors", msg, msg, count, opts)
    else
      Gettext.dgettext(BackpackingmapWeb.Gettext, "errors", msg, opts)
    end
  end
end
