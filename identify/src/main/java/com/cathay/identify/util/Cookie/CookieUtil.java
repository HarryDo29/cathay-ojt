package com.cathay.identify.util.Cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    // add cookie to a response
    public void addTo(HttpServletResponse response, String name, String value, CookieOption option) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(option.isHttpOnly());
        cookie.setMaxAge(option.getMaxAge());
        cookie.setSecure(option.isSecure());
        cookie.setPath(option.getPath());
        response.addCookie(cookie);
    }
}
