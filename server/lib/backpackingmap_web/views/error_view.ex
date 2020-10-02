defmodule BackpackingmapWeb.ErrorView do
  use BackpackingmapWeb, :view

  def render("500.json", assigns) do
    Jason.encode!(
      %{
        message: "Internal server error",
        _reason: inspect(assigns.reason),
        _stack: inspect(assigns.stack)
      }
    )
  end

  # If you want to customize a particular status code
  # for a certain format, you may uncomment below.
  # def render("500.html", _assigns) do
  #   "Internal Server Error"
  # end

  # By default, Phoenix returns the status message from
  # the template name. For example, "404.html" becomes
  # "Not Found".
  def template_not_found(template, _assigns) do
    Phoenix.Controller.status_message_from_template(template)
  end
end
