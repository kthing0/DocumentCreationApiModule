import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

public class CrptApi {
    private final Lock lock = new ReentrantLock();
    private final long timeLimitMillis;
    private long lastResetTimeMillis;
    private int requestCount;
    private final int requestLimit;
    private final ObjectMapper objectMapper;
    private final String apiUrl;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeLimitMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.lastResetTimeMillis = System.currentTimeMillis();
        this.requestCount = 0;
        this.objectMapper = new ObjectMapper();
        this.apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    }

    public void createDocument(CrptDocument document, String signature) throws IOException {
        try {
            lock.lock();
            resetIfNecessary();

            if (requestCount < requestLimit) {
                sendHttpPostRequest(document, signature);
                requestCount++;
            } else {
                long currentTimeMillis = System.currentTimeMillis();
                long elapsedTime = currentTimeMillis - lastResetTimeMillis;
                long remainingTime = timeLimitMillis - elapsedTime;

                if (remainingTime > 0) {
                    TimeUnit.MILLISECONDS.sleep(remainingTime);
                    createDocument(document, signature);
                } else {
                    resetIfNecessary();
                    createDocument(document, signature);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    private void resetIfNecessary() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastResetTimeMillis > timeLimitMillis) {
            lastResetTimeMillis = currentTimeMillis;
            requestCount = 0;
        }
    }

    private void sendHttpPostRequest(CrptDocument document, String signature) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        String jsonInputString = objectMapper.writeValueAsString(document);

        con.getOutputStream().write(jsonInputString.getBytes());

        /*
        int responseCode = con.getResponseCode();
        System.out.println("HTTP Response Code: " + responseCode);   // For debugging purposes

         */

        con.disconnect();
    }

    @Data
    @AllArgsConstructor
    @Getter
    public static class CrptDocument {
        private final Description description;
        private final String doc_id;
        private final String doc_status;
        private final String doc_type;
        private final boolean importRequest;
        private final String owner_inn;
        private final String participant_inn;
        private final String producer_inn;
        private final String production_date;
        private final String production_type;
        private final List<Product> products;
        private final String reg_date;
        private final String reg_number;
    }

    @Getter
    @AllArgsConstructor
    public static class Description {
        private final String participantInn;
    }

    @Data
    @AllArgsConstructor
    @Getter
    public static class Product {
        private final String certificate_document;
        private final String certificate_document_date;
        private final String certificate_document_number;
        private final String owner_inn;
        private final String producer_inn;
        private final String production_date;
        private final String tnved_code;
        private final String uit_code;
        private final String uitu_code;
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

        CrptApi.Description description = new CrptApi.Description("string");
        List<CrptApi.Product> products = List.of(new CrptApi.Product(
                "string", "2020-01-23", "string",
                "string", "string", "2020-01-23",
                "string", "string", "string"
        ));

        CrptApi.CrptDocument crptDocument = new CrptApi.CrptDocument(
                description, "string", "string", "LP_INTRODUCE_GOODS",
                true, "string", "string", "string",
                "2020-01-23", "string", products, "2020-01-23", "string"
        );

        String signature = "sample_signature";
        try {
            crptApi.createDocument(crptDocument, signature);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
