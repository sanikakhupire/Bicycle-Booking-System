package com.sanika.bicyclebookingapp.util;

/**
 * A generic wrapper that holds a value with its loading status.
 * Used to communicate results from Repository to ViewModel to UI.
 *
 * @param <T> The type of data held by this Resource.
 */
public class Resource<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    public final Status status;
    public final T data;
    public final String message;

    private Resource(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String message, T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    public static <T> Resource<T> loading(T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError()   { return status == Status.ERROR; }
    public boolean isLoading() { return status == Status.LOADING; }
}
