package cn.navclub.nes4j.bin.function;

/**
 * @param <A> Param one type
 * @param <B> Param two type
 * @param <C> Param three type
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@FunctionalInterface
public interface TCallback<A, B, C> {
    void accept(A a, B b, C c);
}
