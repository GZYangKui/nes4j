package cn.navclub.nes4j.bin.function;

@FunctionalInterface
public interface TCallback<A, B, C> {
    void accept(A a, B b, C c);
}
