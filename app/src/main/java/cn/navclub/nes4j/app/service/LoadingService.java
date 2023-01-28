package cn.navclub.nes4j.app.service;

public interface LoadingService<T> {
    /**
     * Execute async load data
     *
     * @param params Execute params
     */
    T execute(Object... params);

    /**
     * Data load success call this function
     *
     * @param data Target data
     */
    void onSuccess(T data);

    /**
     * When load data occur error call this function
     *
     * @param throwable Error detail
     */
    default void onError(Throwable throwable) {

    }
}
