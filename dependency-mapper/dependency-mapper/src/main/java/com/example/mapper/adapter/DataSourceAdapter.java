package com.example.mapper.adapter;

import java.io.IOException;

public interface DataSourceAdapter<T> {
    T fetch() throws IOException, InterruptedException;
}
