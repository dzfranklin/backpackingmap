import Config

config :backpackingmap, :os_api_key,
       System.get_env("OS_API_KEY") || raise "Environment variable OS_API_KEY missing"
