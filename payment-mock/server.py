
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
from urllib.parse import urlsplit
import json
import time
import uuid


payments = {}
payments_lock = Lock()
retry_attempts = {}

class PaymentHandler(BaseHTTPRequestHandler):

    def send_json(self, status, data):

        body = json.dumps(data).encode("utf-8")

        try:
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))

            request_id = self.headers.get("X-Request-ID")

            if request_id:
                self.send_header("X-Request-ID", request_id)

            self.end_headers()
            self.wfile.write(body)

        except (BrokenPipeError, ConnectionResetError):
            print("Client disconnected", flush=True)

    def do_GET(self):

        path = urlsplit(self.path).path
        prefix = "/api/payments/"

        if not path.startswith(prefix):
            self.send_json(404, {"error": "Not found"})
            return

        payment_id = path[len(prefix):]

        if not payment_id:
            self.send_json(404, {"error": "Not found"})
            return

        print(
            f"GET payment={payment_id}, "
            f"requestId={self.headers.get('X-Request-ID')}",
            flush=True
        )
        print(
            "TRACE CONTEXT:",
            self.headers.get("traceparent"),
            flush=True
        )

        # 517-dars: transient failure simulation

        if payment_id == "FLAKY":

            request_id = self.headers.get("X-Request-ID")

            if not request_id:
                self.send_json(400, {
                    "error": "X-Request-ID required"
                })
                return

            key = (request_id, payment_id)

            with payments_lock:

                # Lab memory'ni cheksiz oshirmaslik
                if key not in retry_attempts and len(retry_attempts) >= 1024:
                    retry_attempts.pop(next(iter(retry_attempts)))

                attempt = retry_attempts.get(key, 0) + 1
                retry_attempts[key] = attempt

                if attempt >= 3:
                    retry_attempts.pop(key, None)

            print(
                f"FLAKY attempt={attempt}, "
                f"requestId={request_id}",
                flush=True
            )

            if attempt < 3:

                self.send_json(503, {
                    "error": "Temporary payment failure"
                })

            else:

                self.send_json(200, {
                    "paymentId": payment_id,
                    "status": "PAID"
                })

            return


        # 516-dars: failure simulation

        if payment_id == "SLOW":
            time.sleep(4)

        if payment_id == "DELAYED":
            time.sleep(1)

        if payment_id == "EMPTY":
            self.send_response(204)
            self.end_headers()
            return

        failures = {
            "BAD-REQUEST": (400, "Invalid payment request"),
            "UNAUTHORIZED": (401, "Invalid service credential"),
            "MISSING": (404, "Payment not found"),
            "RATE-LIMIT": (429, "Too many requests"),
            "ERROR": (500, "Internal payment error"),
            "UNAVAILABLE": (503, "Payment service unavailable")
        }

        if payment_id in failures:
            status, message = failures[payment_id]

            self.send_json(status, {
                "error": message
            })

            return


        with payments_lock:
            result = payments.get(payment_id)

        # Oldingi GET mock contractini saqlaymiz
        if result is None:
            result = {
                "paymentId": payment_id,
                "status": "PAID"
            }

        self.send_json(200, result)

    def do_POST(self):

        if urlsplit(self.path).path != "/api/payments":
            self.send_json(404, {"error": "Not found"})
            return

        if self.headers.get_content_type() != "application/json":
            self.send_json(415, {"error": "JSON required"})
            return

        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_json(400, {"error": "Invalid length"})
            return

        if length <= 0 or length > 10000:
            self.send_json(400, {"error": "Invalid body size"})
            return

        try:
            body = json.loads(self.rfile.read(length))

            order_id = body["orderId"]
            amount = body["amount"]

            if not isinstance(order_id, str) or not order_id.strip():
                raise ValueError("Invalid orderId")

            if type(amount) is not int or amount <= 0:
                raise ValueError("Invalid amount")

        except (ValueError, KeyError, TypeError):
            self.send_json(400, {"error": "Invalid request"})
            return

        payment_id = "PAY-" + uuid.uuid4().hex[:12]

        result = {
            "paymentId": payment_id,
            "status": "PENDING"
        }

        with payments_lock:
            payments[payment_id] = result

        print(
            f"POST payment={payment_id}, "
            f"orderId={order_id}, amount={amount}, "
            f"requestId={self.headers.get('X-Request-ID')}",
            flush=True
        )

        self.send_json(201, result)


server = ThreadingHTTPServer(
    ("0.0.0.0", 8090),
    PaymentHandler
)

print("Payment mock started on port 8090", flush=True)

server.serve_forever()
