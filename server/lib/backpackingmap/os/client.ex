defmodule Backpackingmap.Os.Client do
  alias Backpackingmap.Os.Auth
  require Logger

  def request(method, url) do
    :httpc.request(method, {to_charlist(url), basic_headers()}, [], [])
    |> handle_response()
  end

  def request(method, url, %Auth{} = auth) do
    :httpc.request(method, {to_charlist(url), headers(auth)}, [], [])
    |> handle_response()
  end

  def request(method, url, {content_type, body}) do
    :httpc.request(
      method,
      {to_charlist(url), basic_headers(), to_charlist(content_type), body},
      [],
      []
    )
    |> handle_response()
  end

  def request(method, url, %Auth{} = auth, {content_type, body}) do
    :httpc.request(
      method,
      {to_charlist(url), headers(auth), to_charlist(content_type), body},
      [],
      []
    )
    |> handle_response(auth)
  end

  defp handle_response(resp, auth \\ nil)

  defp handle_response({:ok, {{_, 200, _}, headers, body}}, _auth) do
    headers = parse_response_headers(headers)
    body = to_string(body)

    {:ok, {headers, body}}
  end

  defp handle_response({:ok, {{_, 403, _}, headers, body}}, auth) do
    headers = parse_response_headers(headers)
    body = to_string(body)

    if auth do
      Auth.report_unauthorized(auth)
    else
      Logger.warn("403 for unauthenticated user. Headers: #{inspect(headers)}, body: #{body}")
    end

    {:error, {403, headers, body}}
  end

  defp handle_response({:ok, {{_, status_code, _}, headers, body}}, auth) do
    headers = parse_response_headers(headers)
    body = to_string(body)

    Logger.warn(
      "Status code #{status_code} in response for auth #{inspect(auth)}, headers: #{
        inspect(headers)
      }, body: #{body}"
    )

    {:error, {status_code, headers, body}}
  end

  defp parse_response_headers(headers),
       do: Enum.map(headers, fn {name, value} -> {to_string(name), to_string(value)} end)

  defp headers(auth), do: Enum.concat([basic_headers(), auth_headers(auth)])

  defp auth_headers(%{refresh: {ident, token}}) do
    [
      {'Cookie', 'PROFILEMARK=#{to_charlist(ident)}'},
      {'Cookie', 'REMEMBER_ME=#{to_charlist(token)}'}
    ]
  end

  defp basic_headers do
    [
      {
        'User-Agent',
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36'
      },
      {'Accept', '*/*'},
      {'Accept-Language', 'en-US,en;q=0.5'},
      {'Referer', 'https://osmaps.ordnancesurvey.co.uk/'},
      {'X-Requested-With', 'XMLHttpRequest'},
      {'Origin', 'https://osmaps.ordnancesurvey.co.uk'},
      {'TE', 'Trailers'},
      {'Cookie', generate_consent_cookie()}
    ]
  end

  defp generate_consent_cookie do
    # From <https://ordnancesurvey.co.uk/site-elements/js/cookie-control/cookie-control.min.js>
    ~s(CookieControl={"necessaryCookies":["TS*","BIGip*","prod_tomcat*","Contensis*","CookieControl","cookieMessage","__cfduid","frontend","JSESSIONID","varnish/*","PHPSESSID","AUXSESSION*","PROFILEMARK"],"optionalCookies":{"performance":"legitimate interest","functionality":"legitimate interest"},"initialState":{"type":"notify"},"statement":{"shown":true,"updated":"06/09/2019"},"consentDate":#{
      generate_consent_cookie_consent_date()
    },"consentExpiry":90,"interactedWith":false,"user":"#{generate_consent_cookie_user_id()}"})
    |> String.to_charlist()
  end

  defp generate_consent_cookie_consent_date do
    # consentDate: Date.now(),
    DateTime.utc_now()
    |> DateTime.to_unix()
  end

  defp generate_consent_cookie_user_id do
    # a = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
    a =
      for <<c :: binary - size(1) <- "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz">>,
          do: c

    # NOTE: e and t are set to undefined
    # function (e, t) {

    # var o, c, i = a, n = [ ];
    i = a

    # NOTE: Never executed because of param values
    # if (t = t || i.length, e) for (o = 0; o < e; o++) n[o] = i[0 | Math.random() * t];

    # else for
    Enum.map(
      0..(36 - 1),
      fn o ->
        # (n[8] = n[13] = n[18] = n[23] = '-', n[14] = '4', o = 0; o < 36; o++)
        #    n[o] || (
        if o == 8 or o == 13 or o == 18 or o == 23 do
          "-"
        else
          #    c = 0 | 16 * Math.random(),
          # NOTE: bitwise or with 0 is a Javascript trick to convert a float to an integer by rounding down
          c = floor(16 * :rand.uniform())
          #    n[o] = i[
          #      19 == o ?
          out =
            if o == 19 do
              #  3 & c | 8 :
              Bitwise.bor(Bitwise.band(3, c), 8)
            else
              #  c]
              c
            end

          Enum.at(i, out)
        end

        #    );
      end
    )
    |> Enum.join()

    # return n.join('')
    # };
  end
end
