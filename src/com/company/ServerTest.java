package com.company;

import com.company.HttpServer.TestHttpServer;
import com.company.HttpServer.TestHttpServerImpl;

import java.io.IOException;

public class ServerTest {

    private static final int NUMBER_OF_THREADS = 10;

    public static void main(String[] args) {
        TestHttpServer server = new TestHttpServerImpl();
        try {
            server.createMultiThreadedServer(NUMBER_OF_THREADS);
            server.addGetFileMethod("/file");
            server.startServer();
            System.out.println("Server started");
        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }
}
