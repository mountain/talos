package org.talos.util.seq;

import org.talos.util.ISeq;

public class Seq<T> implements ISeq<T> {

    public static final <S> ISeq<S> empty() {
        return new Seq<S>(null, null);
    }

    public static final <S> ISeq<S> cons(S head, ISeq<S> tail) {
        return new Seq<S>(head, tail);
    }

    public static final <S> S head(ISeq<S> seq) {
        return seq.head();
    }

    public static final <S> ISeq<S> tail(ISeq<S> seq) {
        return seq.tail();
    }

    public static final <S> int count(ISeq<S> seq) {
        return seq.count();
    }

    protected T head;
    protected ISeq<T> tail;

    protected Seq(T head, ISeq<T> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public int count() {
        return 1 + tail.count();
    }

    @Override
    public boolean equiv(ISeq<T> another) {
        return head.equals(another.head()) && tail.equals(another.tail());
    }

    @Override
    public ISeq<T> cons(T e) {
        return new Seq<T>(head, tail.cons(e));
    }

    @Override
    public T head() {
        return head;
    }

    @Override
    public ISeq<T> tail() {
        return tail;
    }

}
