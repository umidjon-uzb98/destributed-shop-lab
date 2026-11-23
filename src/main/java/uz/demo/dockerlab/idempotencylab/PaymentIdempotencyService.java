
package uz.demo.dockerlab.idempotencylab;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentIdempotencyService {

    private final JdbcTemplate jdbc;

    public PaymentIdempotencyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public PaymentReply pay(
            String idempotencyKey,
            PaymentRequest request
    ) {

        validate(idempotencyKey, request);

        String fingerprint = fingerprint(request);

        // 1. Birinchi request key'ni egallashga urinadi
        int inserted = jdbc.update("""
                INSERT INTO idem_request_539 (
                    idempotency_key,
                    fingerprint,
                    expires_at
                )
                VALUES (
                    ?,
                    ?,
                    now() + interval '24 hours'
                )
                ON CONFLICT (idempotency_key) DO NOTHING
                """,
                idempotencyKey,
                fingerprint
        );

        // 2. Key oldin mavjud bo'lsa
        if (inserted == 0) {

            SavedReply saved = jdbc.queryForObject("""
                    SELECT
                        fingerprint,
                        http_status,
                        response_body::text AS body
                    FROM idem_request_539
                    WHERE idempotency_key = ?
                    """,
                    (rs, rowNum) -> new SavedReply(
                            rs.getString("fingerprint"),
                            (Integer) rs.getObject("http_status"),
                            rs.getString("body")
                    ),
                    idempotencyKey
            );

            if (saved == null) {
                throw new IllegalStateException(
                        "Idempotency record not found"
                );
            }

            // Bitta key, boshqa request
            if (!saved.fingerprint().equals(fingerprint)) {

                return new PaymentReply(
                        409,
                        """
                        {"error":"KEY_REUSED_WITH_DIFFERENT_REQUEST"}
                        """.trim(),
                        false
                );
            }

            // Oddiy holatda boshqa transaction commit
            // qilgandan keyin tayyor response mavjud bo'ladi
            if (saved.httpStatus() == null
                    || saved.body() == null) {

                return new PaymentReply(
                        409,
                        """
                        {"error":"REQUEST_STILL_PROCESSING"}
                        """.trim(),
                        false
                );
            }

            // Payment qayta bajarilmaydi
            return new PaymentReply(
                    saved.httpStatus(),
                    saved.body(),
                    true
            );
        }

        // 3. Yangi key — atomik debit
        List<Long> balances = jdbc.query("""
                UPDATE idem_wallet_539
                SET balance_uzs = balance_uzs - ?
                WHERE id = ?
                  AND balance_uzs >= ?
                RETURNING balance_uzs
                """,
                (rs, rowNum) -> rs.getLong("balance_uzs"),
                request.amountUzs(),
                request.walletId(),
                request.amountUzs()
        );

        // 4. Pul yetarli bo'lmasa, declined result saqlanadi
        if (balances.isEmpty()) {

            String body = """
                    {"status":"DECLINED",
                     "reason":"INSUFFICIENT_FUNDS"}
                    """.trim();

            saveResult(idempotencyKey, 409, body);

            return new PaymentReply(
                    409,
                    body,
                    false
            );
        }

        // 5. Payment tarixiga bitta row yozamiz
        UUID paymentId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO idem_payment_539 (
                    payment_id,
                    idempotency_key,
                    wallet_id,
                    amount_uzs
                )
                VALUES (?, ?, ?, ?)
                """,
                paymentId,
                idempotencyKey,
                request.walletId(),
                request.amountUzs()
        );

        // 6. Saqlanadigan HTTP response
        String body = """
                {"paymentId":"%s",
                 "status":"SUCCEEDED",
                 "walletId":%d,
                 "amountUzs":%d,
                 "balanceAfterUzs":%d}
                """.formatted(
                paymentId,
                request.walletId(),
                request.amountUzs(),
                balances.getFirst()
        ).trim();

        saveResult(idempotencyKey, 201, body);

        return new PaymentReply(
                201,
                body,
                false
        );
    }

    private void saveResult(
            String key,
            int httpStatus,
            String body
    ) {

        jdbc.update("""
                UPDATE idem_request_539
                SET
                    http_status = ?,
                    response_body = CAST(? AS jsonb)
                WHERE idempotency_key = ?
                """,
                httpStatus,
                body,
                key
        );
    }

    private void validate(
            String key,
            PaymentRequest request
    ) {

        if (key == null ||
                !key.matches("[A-Za-z0-9_-]{8,80}")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key"
            );
        }

        if (request == null ||
                request.walletId() == null ||
                request.walletId() != 1L ||
                request.amountUzs() == null ||
                request.amountUzs() <= 0 ||
                request.amountUzs() > 1_000_000L) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid wallet or amount"
            );
        }
    }

    private String fingerprint(PaymentRequest request) {

        String canonical = """
                POST|/api/idempotency-lab/payments|%d|%d
                """.formatted(
                request.walletId(),
                request.amountUzs()
        ).trim();

        try {

            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonical.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    ex
            );
        }
    }

    @Transactional
    public WalletState reset() {

        // Faqat lokal lab. Parallel payment paytida ishlatilmaydi.
        jdbc.update("DELETE FROM idem_payment_539");
        jdbc.update("DELETE FROM idem_request_539");

        jdbc.update("""
                INSERT INTO idem_wallet_539 (
                    id,
                    balance_uzs
                )
                VALUES (1, 1000000)
                ON CONFLICT (id)
                DO UPDATE SET balance_uzs = 1000000
                """);

        return state();
    }

    public WalletState state() {

        Long balance = jdbc.queryForObject("""
                SELECT balance_uzs
                FROM idem_wallet_539
                WHERE id = 1
                """,
                Long.class
        );

        Long paymentCount = jdbc.queryForObject(
                "SELECT count(*) FROM idem_payment_539",
                Long.class
        );

        Long keyCount = jdbc.queryForObject(
                "SELECT count(*) FROM idem_request_539",
                Long.class
        );

        return new WalletState(
                balance,
                paymentCount,
                keyCount
        );
    }

    public record PaymentReply(
            int httpStatus,
            String body,
            boolean replayed
    ) {}

    private record SavedReply(
            String fingerprint,
            Integer httpStatus,
            String body
    ) {}

    public record WalletState(
            Long balanceUzs,
            Long paymentCount,
            Long idempotencyRecordCount
    ) {}
}
