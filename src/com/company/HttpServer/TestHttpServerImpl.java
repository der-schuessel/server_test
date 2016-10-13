package com.company.HttpServer;

import com.company.RequestedFile;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by der-schuessel on 12.10.16.
 */
public class TestHttpServerImpl implements TestHttpServer {

    public static final String CONTENT_LENGTH = "Content-Length: ";
    public static final String ETAG = "Etag";
    public static final String CONNECTION = "Connection";
    public static final String KEEP_ALIVE = "Keep-Alive";
    public static final String PATH = "path";
    public static final String FILENAME = "filename";
    public static final String IF_NONE_MATCH = "If-None-Match";
    public static final int PORT = 8080;
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String IF_MATCH = "If-Match";
    public static final int PRECONDITION_FAILED = 412;
    public static final int NOT_MODIFIED = 304;
    public static final int OK = 200;
    HttpServer server;

    @Override
    public TestHttpServer createMultiThreadedServer(int numberOfThreads) throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        server.setExecutor(executor);
        return this;
    }

    @Override
    public void addGetFileMethod(String urlPattern) throws IOException {

        // Create Context for serving files by using the given url pattern and defining a HttpHandler for all request
        // of the url pattern
        HttpContext context = server.createContext(urlPattern, httpExchange -> {
            Map<String, String> params = queryToParameterMap(httpExchange.getRequestURI().getQuery());
            System.out.println("Query: " + httpExchange.getRequestURI().getQuery());
            // Check whether client specified the mandatory path parameter
            if (!params.containsKey(PATH)) {
                throw new IOException("Query must contain well formed path parameter");
            }
            // If client only specified a directory respond with listing of files and subdirectories
            if (params.get(FILENAME).isEmpty()) {
                respondWithFileList(httpExchange, params);

                //  If both path and filename is specified by client respond with requested file (taking clients
                // requested behaviour according to request header into account).
            } else {
                respondWithFileIfNecessary(httpExchange, params);
            }
            System.out.println("Served request with origin: " + httpExchange.getRemoteAddress());
        });

        System.out.println("Context Path: " + context.getPath());
    }

    private void respondWithFileIfNecessary(HttpExchange httpExchange, Map<String, String> params) throws IOException {
        File file = new File(params.get(PATH), params.get(FILENAME));
        RequestedFile requestedFile = new RequestedFile(file);
        Headers requestHeaders = httpExchange.getRequestHeaders();

        // Check whether client uses If-None-Match
        if (requestHeaders.containsKey(IF_NONE_MATCH)) {
            String etag = requestHeaders.getFirst(IF_NONE_MATCH);
            String actualEtag = requestedFile.getETag();

            // Only respond with file if the Request ETag does not match the ETag of the requested file
            if (etag.equals(actualEtag)) {
                Headers headers = httpExchange.getResponseHeaders();
                headers.add(ETAG, actualEtag);
                addKeepAlive(headers, requestHeaders);
                httpExchange.sendResponseHeaders(NOT_MODIFIED, file.length());
                System.out.println("File already in Cache: " + NOT_MODIFIED);

                // In case the Etag differs respond with actual requested file
            } else {
                respondWithNewFile(httpExchange, file);
            }
        }

        // Check whether client uses If-Modified-Since
        else if (requestHeaders.containsKey(IF_MODIFIED_SINCE)) {
            String lastModifiedTag = requestHeaders.getFirst(IF_MODIFIED_SINCE);
            // if the file has been modified since the given date of the request, respond with new file
            if (requestedFile.isFileNewerThanTag(lastModifiedTag)) {
                respondWithNewFile(httpExchange, file);
                // if the file hasn't been modified since the given date, respond with 304
            } else {
                httpExchange.sendResponseHeaders(NOT_MODIFIED, file.length());
                System.out.println("File not modified since given date: " + NOT_MODIFIED);
            }

            // Check whether client uses If-Match
        } else if (requestHeaders.containsKey(IF_MATCH)) {
            List<String> etags = requestHeaders.get(IF_MATCH);
            String actualEtag = requestedFile.getETag();

            // If "*" is given as attribute in the If-Match header field or the file's ETag is not in the list
            // of the If-Match header field respnod with 412 precondition failed
            if (etags.contains("*") || !etags.contains(actualEtag)) {
                httpExchange.sendResponseHeaders(PRECONDITION_FAILED, file.length());

                // If the files ETag equals one of the requested ETags respond with the requested file
            } else {
                respondWithNewFile(httpExchange, file);
            }
        }

        // If client doesn't use any of the above, respond with actual requested file
        else {
            respondWithNewFile(httpExchange, file);
        }
    }

    /**
     * Send a list of the files and subdirectories string representation as response.
     *
     * @param httpExchange Http Handler tp process the request.
     * @param params       Map with parameter identifier as key and the according value obtained from the URL.
     * @throws IOException Throws an Exception when the name of a file or subdirectory cannot be determined (for
     *                     instance if permission is denied or the given file doesn't exist).
     */
    private void respondWithFileList(HttpExchange httpExchange, Map<String, String> params) throws IOException {
        String fileList = Files.list(Paths.get(params.get(PATH))) // Get list of files and subdirectories of given path
                .map(file -> file.getFileName().toString()) // map names of files and subdirectories to String
                .collect(Collectors.joining("\n")); // join the names to a line separated joined string

        httpExchange.sendResponseHeaders(OK, fileList.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(fileList.getBytes());
        os.close();
    }

    /**
     * Read the keep-alive capabilities of the requesting client and adjust the kee-alive behaviour of the connection
     * accordingly.
     *
     * @param headers        List of response header field to add kee-alive fields if necessary.
     * @param requestHeaders List of given request header fields used to determine keep-alive capabilities.
     */
    private void addKeepAlive(Headers headers, Headers requestHeaders) {
        if (requestHeaders.containsKey(CONNECTION)) {
            headers.add(CONNECTION, requestHeaders.getFirst(CONNECTION));
        }
        if (requestHeaders.containsKey(KEEP_ALIVE)) {
            for (String attribute : requestHeaders.get(KEEP_ALIVE)) {
                headers.add(KEEP_ALIVE, attribute);
            }
        }
    }

    /**
     * Send the requested file as response body while taking requested keep alive behaviour into account.
     *
     * @param httpExchange Handler that processes the request.
     * @param file         Requested local file.
     * @throws IOException Throws exception whenever the length of a requested cannot be determined (for instance if
     *                     permission is denied or the given file doesn't exist).
     */
    private void respondWithNewFile(HttpExchange httpExchange, File file) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        System.out.println(CONTENT_LENGTH + String.valueOf(file.length()));
        headers.add(CONTENT_LENGTH, String.valueOf(file.length()));
        headers.add(ETAG, String.valueOf(new RequestedFile(file).hashCode()));
        addKeepAlive(headers, httpExchange.getRequestHeaders());

        httpExchange.sendResponseHeaders(OK, file.length());
        OutputStream os = httpExchange.getResponseBody();
        Files.copy(file.toPath(), os);
        os.close();
    }

    @Override
    public void startServer() {
        server.start();
    }

    /**
     * Copied (with minor edits) from
     * http://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
     * returns the url parameters in a map
     *
     * @param query
     * @return map
     */
    public static Map<String, String> queryToParameterMap(String query) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            } else {
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
