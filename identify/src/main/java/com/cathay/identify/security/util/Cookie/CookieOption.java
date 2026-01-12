package com.cathay.identify.security.util.Cookie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CookieOption {
    private int maxAge;

    private boolean httpOnly = false;

    private boolean secure = false;

    private String path = "/";
}
