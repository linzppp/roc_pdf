package org.roc.practice.pdf.common.result;

import lombok.Getter;

@Getter
public class R<T> {

    private final int code;
    private final String message;
    private final T data;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "ok", data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> notFound(String message) {
        return new R<>(404, message, null);
    }
}
