package com.company.HttpServer;

import java.io.IOException;

/**
 * Created by der-schuessel on 12.10.16.
 */
public interface TestHttpServer {

    /**
     * Create a multithreaded Http Server with a fixed number of threads served from a thread pool.
     * @param numberOfThreads Maximum number of active threads.
     * @return An Instance of a multithreaded Http Server.
     * @throws IOException Throws an Exception if a file cannot be processed correctly.
     */
    TestHttpServer createMultiThreadedServer(int numberOfThreads) throws IOException;

    /**
     * Add a method to the Http Server that allows a client to download a file by giving the absolute path and filename
     * as parameter. If no filename is given, a list of the files and subdirectories string representation should be
     * send as response.
     *
     * Example query: IP_ADDRESS:PORT/URL_PATTERN/?path=/PATH/TO/FILE&filename=FILENAME
     *
     * @param urlPattern
     * @throws IOException
     */
    void addGetFileMethod(String urlPattern) throws IOException;

    /**
     * Start the TestHttpSterver.
     */
    void startServer();
}
