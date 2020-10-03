defmodule Backpackingmap.Os.Client do
  alias Backpackingmap.Os.Auth
  require Logger

  def request(method, url) do
    HTTPoison.request(method, url, "", basic_headers())
    |> handle_response()
  end

  def request(method, url, %Auth{} = auth) do
    HTTPoison.request(method, url, "", headers(auth))
    |> handle_response()
  end

  def request(method, url, {content_type, body}) do
    HTTPoison.request(
      method,
      url,
      body,
      [content_type_header(content_type) | basic_headers()]
    )
    |> handle_response()
  end

  def request(method, url, %Auth{} = auth, {content_type, body}) do
    HTTPoison.request(
      method,
      url,
      body,
      [content_type_header(content_type) | headers(auth)]
    )
    |> handle_response(auth)
  end

  defp content_type_header(content_type), do: {"Content-Type", content_type}

  defp handle_response(resp, auth \\ nil)

  defp handle_response({:ok, %{status_code: 200, headers: headers, body: body}}, _auth) do
    {:ok, {headers, body}}
  end

  defp handle_response({:ok, %{status_code: 403, headers: headers, body: body}}, auth) do
    if auth do
      Auth.report_unauthorized(auth)
    else
      Logger.warn("403 for unauthenticated user. Headers: #{inspect(headers)}, body: #{body}")
    end

    {:error, {403, headers, body}}
  end

  defp handle_response({:ok, %{status_code: status_code, headers: headers, body: body}}, auth) do
    Logger.warn(
      "Status code #{status_code} in response for auth #{inspect(auth)}, headers: #{
        inspect(headers)
      }, body: #{body}"
    )

    {:error, {status_code, headers, body}}
  end

  defp headers(auth), do: Enum.concat([basic_headers(), auth_headers(auth)])

  defp auth_headers(%{refresh_ident: ident, refresh_token: token}) do
    [
      {"Cookie", "PROFILEMARK=#{ident}"},
      {"Cookie", "REMEMBER_ME=#{token}"}
    ]
  end

  defp basic_headers do
    [
      {
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36"
      },
      {"Accept", "*/*"},
      {"Accept-Language", "en-US,en;q=0.5"},
      {"Referer", "https://osmaps.ordnancesurvey.co.uk/"},
      {"X-Requested-With", "XMLHttpRequest"},
      {"Origin", "https://osmaps.ordnancesurvey.co.uk"},
      {"TE", "Trailers"},
      {"Cookie", generate_consent_cookie()}
    ]
  end

  defp generate_consent_cookie do
    # From <https://ordnancesurvey.co.uk/site-elements/js/cookie-control/cookie-control.min.js>
    ~s(CookieControl={"necessaryCookies":["TS*","BIGip*","prod_tomcat*","Contensis*","CookieControl","cookieMessage","__cfduid","frontend","JSESSIONID","varnish/*","PHPSESSID","AUXSESSION*","PROFILEMARK"],"optionalCookies":{"performance":"legitimate interest","functionality":"legitimate interest"},"initialState":{"type":"notify"},"statement":{"shown":true,"updated":"06/09/2019"},"consentDate":#{
      generate_consent_cookie_consent_date()
    },"consentExpiry":90,"interactedWith":false,"user":"#{generate_consent_cookie_user_id()}"})
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
