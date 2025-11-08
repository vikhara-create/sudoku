package com.example.sudoku;

import org.junit.jupiter.api.Test;

import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @Test
    public void testPuzzleEndpoint() throws IOException, InterruptedException {
        // start server in a thread
        new Thread(() -> {
            try { App.main(new String[]{}); } catch (Exception e) { e.printStackTrace(); }
        }).start();
        // wait briefly for server start
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:3000/puzzle/p1")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("sample-easy"));
        // POST validate fixed cell
        HttpRequest post = HttpRequest.newBuilder().uri(URI.create("http://localhost:3000/validate"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"row\":0,\"col\":0,\"value\":5,\"puzzleId\":\"p1\"}"))
                .header("Content-Type","application/json").build();
        HttpResponse<String> resp2 = client.send(post, HttpResponse.BodyHandlers.ofString());
        //assertTrue(resp2.body().contains(""valid":false"));
        assertTrue(resp2.body().contains("{\"valid\":false"));

    }
}
