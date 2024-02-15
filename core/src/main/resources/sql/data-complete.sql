INSERT INTO qos_profiles (name, description, status, target_min_upstream_rate, max_upstream_rate, max_upstream_burst_rate,
                          target_min_downstream_rate, max_downstream_rate, max_downstream_burst_rate, min_duration, max_duration, priority,
                          packet_delay_budget, jitter, packet_error_loss_rate)
VALUES ('QOS_E', 'The QOS profile E', 'ACTIVE', 'null', '{"value": 20, "unit": "Mbps"}',
        '{"value": 30, "unit": "Mbps"}', '{"value": 40, "unit": "Mbps"}', '{"value": 50, "unit": "Mbps"}', '{"value": 60, "unit": "Mbps"}',
        '{"value": 5, "unit": "Minutes"}', '{"value": 60, "unit": "Minutes"}', 20, '{"value": 100, "unit": "Milliseconds"}',
        '{"value": 5, "unit": "Milliseconds"}', 3),

       ('QOS_S', 'The QOS profile S', 'ACTIVE', '{"value": 15, "unit": "Mbps"}', '{"value": 25, "unit": "Mbps"}',
        '{"value": 35, "unit": "Mbps"}', '{"value": 45, "unit": "Mbps"}', '{"value": 55, "unit": "Mbps"}', '{"value": 65, "unit": "Mbps"}',
        '{"value": 6, "unit": "Minutes"}', '{"value": 70, "unit": "Minutes"}', 25, '{"value": 120, "unit": "Milliseconds"}',
        '{"value": 6, "unit": "Milliseconds"}', 4),

       ('QOS_M', 'The QOS profile M', 'ACTIVE', '{"value": 18, "unit": "Mbps"}', '{"value": 28, "unit": "Mbps"}',
        '{"value": 38, "unit": "Mbps"}', '{"value": 48, "unit": "Mbps"}', '{"value": 58, "unit": "Mbps"}', '{"value": 68, "unit": "Mbps"}',
        '{"value": 7, "unit": "Minutes"}', '{"value": 80, "unit": "Minutes"}', 30, '{"value": 140, "unit": "Milliseconds"}',
        '{"value": 7, "unit": "Milliseconds"}', 5),

       ('QOS_L', 'The QOS profile L', 'ACTIVE', '{"value": 22, "unit": "Mbps"}', '{"value": 32, "unit": "Mbps"}',
        '{"value": 42, "unit": "Mbps"}', '{"value": 52, "unit": "Mbps"}', '{"value": 62, "unit": "Mbps"}', '{"value": 72, "unit": "Mbps"}',
        '{"value": 8, "unit": "Minutes"}', '{"value": 90, "unit": "Minutes"}', 35, '{"value": 160, "unit": "Milliseconds"}',
        '{"value": 8, "unit": "Milliseconds"}', 6);
