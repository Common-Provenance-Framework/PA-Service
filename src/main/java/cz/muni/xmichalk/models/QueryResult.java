package cz.muni.xmichalk.models;

public class QueryResult<T> {
    public String jwt;
    public T result;

    public QueryResult() {
    }

    public QueryResult(T result, String jwt) {
        this.jwt = jwt;
        this.result = result;
    }
}