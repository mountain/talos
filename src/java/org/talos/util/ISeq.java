package org.talos.util;

public interface ISeq<T> {

    public int count();

    public boolean equiv(ISeq<T> another);

    public ISeq<T> cons(T e);

    public T head();

    public ISeq<T> tail();

}