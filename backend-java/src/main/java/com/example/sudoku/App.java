package com.example.sudoku;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class App {

    static class Puzzle {
        String id;
        String name;
        int[][] grid;
        List<int[]> fixed = new ArrayList<>();
    }

    static Map<String, Puzzle> puzzles = new HashMap<>();
    static Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        loadPuzzles();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "3000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/puzzle", new PuzzleHandler());
        server.createContext("/validate", new ValidateHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(null);
        System.out.println("Starting server on port " + port);
        server.start();
    }

    static void loadPuzzles() throws IOException {
        try (InputStream is = App.class.getResourceAsStream("/puzzles.json")) {
            if (is == null) {
                throw new IOException("puzzles.json not found in resources");
            }
            Type t = new TypeToken<Map<String, Puzzle>>() {}.getType();
            Map<String, Puzzle> map = gson.fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), t);
            if (map == null) {
                throw new IOException("Failed to parse puzzles.json");
            }
            for (Map.Entry<String, Puzzle> e : map.entrySet()) {
                Puzzle p = e.getValue();
                p.id = e.getKey();
                p.fixed = new ArrayList<>();
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        if (p.grid[i][j] != 0) p.fixed.add(new int[]{i, j});
                    }
                }
                puzzles.put(e.getKey(), p);
            }
        }
    }

    static class PuzzleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath(); // e.g. /puzzle/p1
            String[] parts = path.split("/");
            if (parts.length < 3 || parts[2].isEmpty()) {
                send(exchange, 400, gson.toJson(Map.of("error", "missing id")));
                return;
            }
            String id = parts[2];
            Puzzle p = puzzles.get(id);
            if (p == null) {
                send(exchange, 404, gson.toJson(Map.of("error", "not found")));
                return;
            }
            send(exchange, 200, gson.toJson(p));
        }
    }

    static class ValidateHandler implements HttpHandler {
        static class Req {
            Integer row;
            Integer col;
            Integer value;
            String puzzleId;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, gson.toJson(Map.of("valid", false, "reason", "method")));
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Req req = gson.fromJson(body, Req.class);
            if (req == null || req.row == null || req.col == null || req.value == null || req.puzzleId == null) {
                send(exchange, 400, gson.toJson(Map.of("valid", false, "reason", "bad-request")));
                return;
            }
            Puzzle p = puzzles.get(req.puzzleId);
            if (p == null) {
                send(exchange, 400, gson.toJson(Map.of("valid", false, "reason", "puzzle")));
                return;
            }
            for (int[] rc : p.fixed) {
                if (rc[0] == req.row && rc[1] == req.col) {
                    send(exchange, 200, gson.toJson(Map.of("valid", false, "reason", "fixed")));
                    return;
                }
            }
            if (req.value < 1 || req.value > 9) {
                send(exchange, 400, gson.toJson(Map.of("valid", false, "reason", "invalid")));
                return;
            }
            int[][] grid = new int[9][9];
            for (int i = 0; i < 9; i++) {
                System.arraycopy(p.grid[i], 0, grid[i], 0, 9);
            }
            grid[req.row][req.col] = req.value;
            Map<String, Object> conflict = checkConflict(grid, req.row, req.col, req.value);
            if ((Boolean) conflict.get("conflict")) {
                send(exchange, 200, gson.toJson(Map.of("valid", false, "reason", conflict.get("reason"))));
            } else {
                send(exchange, 200, gson.toJson(Map.of("valid", true)));
            }
        }
    }

    // returns Map with keys "conflict":Boolean and optionally "reason":String
    static Map<String, Object> checkConflict(int[][] grid, int r, int c, int val) {
        for (int j = 0; j < 9; j++) {
            if (j != c && grid[r][j] == val) return Map.of("conflict", true, "reason", "row");
        }
        for (int i = 0; i < 9; i++) {
            if (i != r && grid[i][c] == val) return Map.of("conflict", true, "reason", "col");
        }
        int br = (r / 3) * 3;
        int bc = (c / 3) * 3;
        for (int i = br; i < br + 3; i++) {
            for (int j = bc; j < bc + 3; j++) {
                if ((i != r || j != c) && grid[i][j] == val) return Map.of("conflict", true, "reason", "box");
            }
        }
        return Map.of("conflict", false);
    }

    static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    // very small static handler to serve frontend files from classpath /frontend
    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/")) {
                path = "/frontend/index.html";
            } else {
                path = "/frontend" + path;
            }
            InputStream is = App.class.getResourceAsStream(path);
            if (is == null) {
                send(exchange, 404, gson.toJson(Map.of("error", "not found")));
                return;
            }
            byte[] data = is.readAllBytes();
            String contentType = guessContentType(path);
            exSetContentType(exchange, contentType);
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        private String guessContentType(String p) {
            if (p.endsWith(".html")) return "text/html; charset=utf-8";
            if (p.endsWith(".js")) return "application/javascript";
            if (p.endsWith(".css")) return "text/css";
            if (p.endsWith(".json")) return "application/json";
            return "application/octet-stream";
        }

        // helper to set header (keeps send() method unchanged)
        private void exSetContentType(HttpExchange exchange, String contentType) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
        }
    }
}
