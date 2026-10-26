package com.resqhub.web;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.resqhub.util.InputParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Small request-parsing / rendering helpers shared by all servlets. */
public final class WebUtil {

    public static final String CSRF_ATTR = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebUtil() {
    }

    /** Trimmed parameter, never null. */
    public static String str(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    /** Trimmed parameter or null when blank. */
    public static String opt(HttpServletRequest req, String name) {
        return InputParser.textOrNull(req.getParameter(name));
    }

    public static long reqLong(HttpServletRequest req, String name, String label) {
        return InputParser.parseLong(req.getParameter(name), label);
    }

    public static Long optLong(HttpServletRequest req, String name, String label) {
        String v = opt(req, name);
        return v == null ? null : Long.valueOf(InputParser.parseLong(v, label));
    }

    public static int reqInt(HttpServletRequest req, String name, String label) {
        return InputParser.parseInt(req.getParameter(name), label);
    }

    public static Integer optInt(HttpServletRequest req, String name, String label) {
        String v = opt(req, name);
        return v == null ? null : Integer.valueOf(InputParser.parseInt(v, label));
    }

    public static double reqAmount(HttpServletRequest req, String name, String label) {
        return InputParser.parseAmount(req.getParameter(name), label);
    }

    /** Accepts "yyyy-MM-dd HH:mm" and the HTML "yyyy-MM-ddTHH:mm" form. */
    public static LocalDateTime optDateTime(HttpServletRequest req, String name) {
        String v = opt(req, name);
        if (v == null) {
            return null;
        }
        v = v.replace('T', ' ');
        if (v.length() == 10) {
            v = v + " 00:00";
        }
        return InputParser.parseOptionalDateTime(v);
    }

    /** Parses an enum constant by name; blank -> null; unknown -> IllegalArgumentException. */
    public static <E extends Enum<E>> E optEnum(HttpServletRequest req, String name,
                                                Class<E> type, String label) {
        String v = opt(req, name);
        if (v == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, v);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(label + " has an invalid value");
        }
    }

    public static <E extends Enum<E>> E reqEnum(HttpServletRequest req, String name,
                                                Class<E> type, String label) {
        E value = optEnum(req, name, type, label);
        if (value == null) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value;
    }

    /** "yyyy-MM-dd HH:mm" text for the controllers (HTML datetime-local sends a 'T'); blank -> "". */
    public static String dtText(HttpServletRequest req, String name) {
        String v = opt(req, name);
        if (v == null) {
            return "";
        }
        v = v.replace('T', ' ');
        return v.length() == 10 ? v + " 00:00" : v;
    }

    public static boolean flag(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v != null && (v.equals("on") || v.equals("true") || v.equals("1"));
    }

    /** Dropdown options: [constant name, display label] using getLabel() when present. */
    public static <E extends Enum<E>> List<String[]> options(Class<E> type) {
        List<String[]> out = new ArrayList<>();
        Method labelMethod = null;
        try {
            labelMethod = type.getMethod("getLabel");
        } catch (NoSuchMethodException ignored) {
            // fall back to the constant name
        }
        for (E c : type.getEnumConstants()) {
            String label = c.name();
            if (labelMethod != null) {
                try {
                    label = String.valueOf(labelMethod.invoke(c));
                } catch (ReflectiveOperationException ignored) {
                    // keep constant name
                }
            }
            out.add(new String[] {c.name(), label});
        }
        return out;
    }

    /** Per-session CSRF token, created on first use. */
    public static String csrfToken(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        String token = (String) session.getAttribute(CSRF_ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_ATTR, token);
        }
        return token;
    }

    /**
     * Accepts a "return to" URL only if it points inside this application's
     * /app/ area - prevents open redirects through the hidden _return field.
     */
    public static String safeReturn(HttpServletRequest req, String value) {
        if (value == null || value.length() > 300) {
            return null;
        }
        String prefix = req.getContextPath() + "/app/";
        if (value.startsWith(prefix) && !value.contains("\r") && !value.contains("\n")
                && value.indexOf("//", prefix.length()) < 0) {
            return value;
        }
        return null;
    }

    /** Tiny JSON object writer (string values only) for re-filling forms. */
    public static String toJson(Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String key = e.getKey();
            if (key.startsWith("_") || key.toLowerCase().contains("password")) {
                continue;   // never echo tokens or passwords back
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(key)).append("\":\"")
              .append(escape(e.getValue().length == 0 ? "" : e.getValue()[0])).append('"');
        }
        return sb.append('}').toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
