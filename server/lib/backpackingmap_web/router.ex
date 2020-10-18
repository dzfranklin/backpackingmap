defmodule BackpackingmapWeb.Router do
  use BackpackingmapWeb, :router
  use Pow.Phoenix.Router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, {BackpackingmapWeb.LayoutView, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :protected do
    plug Pow.Plug.RequireAuthenticated,
         error_handler: Pow.Phoenix.PlugErrorHandler
  end

  pipeline :api do
    plug :accepts, ["json"]
    plug BackpackingmapWeb.APIAuth.Plug, otp_app: :backpackingmap
  end

  pipeline :api_protected do
    plug Pow.Plug.RequireAuthenticated, error_handler: BackpackingmapWeb.APIAuthErrorHandler
  end

  scope "/" do
    pipe_through :browser
    pow_routes()
  end

  scope "/", BackpackingmapWeb do
    pipe_through :browser

    live "/", PageLive, :index
  end

  scope "/", BackpackingmapWeb do
    pipe_through [:browser, :protected]

    # Add your protected routes here
  end

  scope "/api/v1", BackpackingmapWeb.API.V1, as: :api_v1 do
    pipe_through [:api, :api_protected]

    post "/tile", TileController, :post
  end

  scope "/api/v1", BackpackingmapWeb.API.V1, as: :v1_api do
    pipe_through :api

    post "/registration", RegistrationController, :create
    post "/session", SessionController, :create
    delete "/session", SessionController, :delete
    post "/session/renew", SessionController, :renew
  end

  # Enables LiveDashboard only for development
  #
  # If you want to use the LiveDashboard in production, you should put
  # it behind authentication and allow only admins to access it.
  # If your application does not have an admins-only section yet,
  # you can use Plug.BasicAuth to set up some basic authentication
  # as long as you are also using SSL (which you should anyway).
  if Mix.env() in [:dev, :test] do
    import Phoenix.LiveDashboard.Router

    scope "/" do
      pipe_through :browser
      live_dashboard "/dashboard", metrics: BackpackingmapWeb.Telemetry
    end
  end
end
