CREATE TABLE idempotency_keys (
                                  idempotency_key VARCHAR(255) PRIMARY KEY,
                                  request_path VARCHAR(255) NOT NULL,
                                  response_status INTEGER,
                                  response_body TEXT,
                                  created_at TIMESTAMP NOT NULL DEFAULT now()
);